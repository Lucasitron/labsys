<script lang="ts">
	import { goto, invalidateAll } from '$app/navigation';
	import type { PageProps } from './$types';
	import type { Cliente, Encomenda, KanbanStatus, OrdemOrigem } from '$lib/types/vendas';
	import { listClientes } from '$lib/api/vendas/clientes';
	import { createEncomenda, moverKanban } from '$lib/api/vendas/encomendas';
	import { ApiError } from '$lib/api/client';
	import { toUserMessage } from '$lib/utils/errors';
	import { toasts } from '$lib/stores/toast';
	import { formatNumber } from '$lib/utils/format';
	import { formatDateBR, formatMoneyBRL } from '$lib/utils/vendas-format';
	import PageHeader from '$lib/components/ui/PageHeader.svelte';
	import SearchInput from '$lib/components/ui/SearchInput.svelte';
	import Select from '$lib/components/ui/Select.svelte';
	import KanbanBoard, {
		type KanbanCard,
		type KanbanColumn
	} from '$lib/components/ui/KanbanBoard.svelte';
	import TypeBadge from '$lib/components/ui/TypeBadge.svelte';
	import EmptyState from '$lib/components/ui/EmptyState.svelte';
	import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte';
	import Icon from '$lib/components/ui/Icon.svelte';
	import Modal from '$lib/components/ui/Modal.svelte';

	let { data }: PageProps = $props();

	const params = $derived(data.params);
	const resultado = $derived(data.resultado);
	const erro = $derived(data.error);

	const encomendas = $derived<Encomenda[]>(resultado?.encomendas ?? []);
	const contagens = $derived<Record<string, number>>(resultado?.counts ?? {});

	const carregando = $derived(resultado === null && erro === null);
	const comErro = $derived(erro !== null && resultado === null);

	const filtrado = $derived(
		params.status_kanban !== '' || params.search !== '' || params.clienteId !== ''
	);

	const COLUNAS: { id: KanbanStatus; slug: string; tone: 'muted' | 'brand' | 'warn' | 'success' }[] =
		[
			{ id: 'Fila', slug: 'fila', tone: 'muted' },
			{ id: 'Produção', slug: 'producao', tone: 'brand' },
			{ id: 'Acabamento', slug: 'acabamento', tone: 'warn' },
			{ id: 'Pronto', slug: 'pronto', tone: 'success' },
			{ id: 'Entregue', slug: 'entregue', tone: 'muted' }
		];

	function numero(valor: unknown): number | null {
		const n = Number(valor);
		return Number.isFinite(n) ? n : null;
	}

	function fmtContagem(valor: number | null): string {
		return valor === null ? '—' : formatNumber(valor);
	}

	// KPIs servidos pelo backend (nunca derivados no client).
	const kpiProducao = $derived(numero(contagens['Produção']));
	const kpiProntas = $derived(numero(contagens['Pronto']));
	const kpiAtrasadas = $derived(numero(contagens['Atrasadas'] ?? contagens['Atrasada']));
	const kpiEntreguesMes = $derived(
		numero(contagens['Entregues no mês'] ?? contagens['Entregues/mês'] ?? contagens['Entregues'])
	);

	const porId = $derived(new Map(encomendas.map((e) => [e.id, e])));

	// Coluna derivada do estado servido (statusKanban); contagem servida com fallback local.
	const colunas = $derived<KanbanColumn[]>(
		COLUNAS.map((col) => {
			const daColuna = encomendas.filter((e) => e.statusKanban === col.id);
			const servida = numero(contagens[col.id]);
			return {
				id: col.id,
				label: col.id,
				tone: col.tone,
				count: servida ?? daColuna.length,
				cards: daColuna.map(
					(e): KanbanCard => ({
						id: e.id,
						title: e.codigo,
						description: e.cliente.nome,
						meta: formatMoneyBRL(e.valorFinal)
					})
				)
			};
		})
	);

	function dataBR(iso: string): string {
		if (!iso || Number.isNaN(new Date(iso).getTime())) return '—';
		return formatDateBR(iso);
	}

	function estaAtrasada(enc: Encomenda): boolean {
		if (enc.statusKanban === 'Entregue') return false;
		const tempo = new Date(enc.previsao).getTime();
		if (Number.isNaN(tempo)) return false;
		const hoje = new Date();
		hoje.setHours(0, 0, 0, 0);
		return tempo < hoje.getTime();
	}

	function origemTone(origem: OrdemOrigem): 'brand' | 'success' | 'warn' | 'muted' {
		if (origem === 'Orçamento') return 'brand';
		if (origem === 'Venda direta') return 'success';
		if (origem === 'Marketplace') return 'warn';
		return 'muted';
	}

	function destinosDe(enc: Encomenda): KanbanStatus[] {
		return COLUNAS.map((c) => c.id).filter((id) => id !== enc.statusKanban);
	}

	interface Query {
		[key: string]: string | undefined;
	}

	function navegar(overrides: Query): void {
		const base: Query = {
			status_kanban: params.status_kanban || undefined,
			search: params.search || undefined,
			clienteId: params.clienteId || undefined
		};
		const merged = { ...base, ...overrides };
		const url = new URLSearchParams();
		for (const [chave, valor] of Object.entries(merged)) {
			if (valor !== undefined && valor !== '') url.append(chave, valor);
		}
		const qs = url.toString();
		void goto(`/vendas/encomendas${qs ? `?${qs}` : ''}`);
	}

	function onSearch(termo: string): void {
		navegar({ search: termo || undefined });
	}

	function onEtapa(valor: string): void {
		navegar({ status_kanban: valor || undefined });
	}

	function onClienteId(valor: string): void {
		navegar({ clienteId: valor.trim() || undefined });
	}

	function limparFiltros(): void {
		navegar({ status_kanban: undefined, search: undefined, clienteId: undefined });
	}

	function tentarNovamente(): void {
		void goto(`/vendas/encomendas${window.location.search}`, { invalidateAll: true });
	}

	// ---- Mover no Kanban (PUT /api/vendas/encomendas/{id}/kanban 🔴) ----

	let popoverMover = $state<string | null>(null);
	let movendoId = $state<string | null>(null);

	async function mover(enc: Encomenda, destino: KanbanStatus): Promise<void> {
		popoverMover = null;
		if (movendoId) return;
		movendoId = enc.id;
		try {
			await moverKanban(enc.id, { statusKanban: destino });
			toasts.success(`Encomenda ${enc.codigo} movida para ${destino}.`);
			await invalidateAll();
		} catch (err) {
			if (err instanceof ApiError && err.status === 409) {
				toasts.warn('Conflito de versão — atualize a tela.');
			} else {
				toasts.danger(toUserMessage(err).message);
			}
		} finally {
			movendoId = null;
		}
	}

	// ---- Modal nova encomenda (venda direta; nasce na Fila 🔴) ----

	const ORIGENS: OrdemOrigem[] = ['Orçamento', 'Venda direta', 'Marketplace'];

	let modalAberto = $state(false);
	let clienteSel = $state<{ id: string; nome: string } | null>(null);
	let buscaCliente = $state('');
	let resultadosCliente = $state<Cliente[]>([]);
	let buscandoCliente = $state(false);
	let erroBuscaCliente = $state<string | null>(null);
	let origem = $state<OrdemOrigem>('Venda direta');
	let valorStr = $state('');
	let previsao = $state('');
	let produtos = $state('');
	let erros = $state<Record<string, string>>({});
	let ocupado = $state(false);

	let timerBusca: ReturnType<typeof setTimeout> | null = null;
	let ctrlBusca: AbortController | null = null;

	function abrirModal(): void {
		clienteSel = null;
		buscaCliente = '';
		resultadosCliente = [];
		buscandoCliente = false;
		erroBuscaCliente = null;
		origem = 'Venda direta';
		valorStr = '';
		previsao = '';
		produtos = '';
		erros = {};
		modalAberto = true;
	}

	function onBuscarCliente(termo: string): void {
		buscaCliente = termo;
		if (timerBusca) clearTimeout(timerBusca);
		if (!termo.trim()) {
			resultadosCliente = [];
			buscandoCliente = false;
			erroBuscaCliente = null;
			ctrlBusca?.abort();
			return;
		}
		buscandoCliente = true;
		erroBuscaCliente = null;
		const atual = termo.trim();
		timerBusca = setTimeout(() => void executarBuscaCliente(atual), 300);
	}

	async function executarBuscaCliente(termo: string): Promise<void> {
		ctrlBusca?.abort();
		const ctrl = new AbortController();
		ctrlBusca = ctrl;
		const sinal = ctrl.signal;
		try {
			const res = await listClientes({ search: termo, page: 1, pageSize: 8 }, (input, init) =>
				fetch(input, { ...init, signal: sinal })
			);
			if (!sinal.aborted) {
				resultadosCliente = res.clientes;
				buscandoCliente = false;
			}
		} catch (err) {
			if (sinal.aborted) return;
			buscandoCliente = false;
			erroBuscaCliente = err instanceof Error ? err.message : 'Erro ao buscar clientes.';
		}
	}

	function selecionarCliente(c: Cliente): void {
		clienteSel = { id: c.id, nome: c.nome };
		resultadosCliente = [];
		buscaCliente = '';
		if (erros['cliente']) erros = { ...erros, cliente: '' };
	}

	function parseValor(texto: string): number | null {
		const n = Number(texto.replace(/\./g, '').replace(',', '.'));
		return Number.isFinite(n) && n > 0 ? n : null;
	}

	async function salvarEncomenda(): Promise<void> {
		if (ocupado) return;
		const novos: Record<string, string> = {};
		if (!clienteSel) novos['cliente'] = 'Selecione o cliente.';
		const valor = parseValor(valorStr);
		if (valor === null) novos['valor'] = 'Informe um valor maior que zero.';
		if (previsao && Number.isNaN(new Date(previsao).getTime()))
			novos['previsao'] = 'Informe uma data válida.';
		erros = novos;
		if (Object.keys(novos).length > 0 || !clienteSel || valor === null) return;
		ocupado = true;
		try {
			const enc = await createEncomenda({
				clienteId: clienteSel.id,
				origem,
				valorFinal: valor,
				...(previsao ? { previsao } : {}),
				...(produtos.trim() ? { observacoes: produtos.trim() } : {})
			});
			toasts.success(`Encomenda ${enc.codigo} criada na Fila.`);
			modalAberto = false;
			await invalidateAll();
		} catch (err) {
			toasts.danger(toUserMessage(err).message);
		} finally {
			ocupado = false;
		}
	}

	const inputCls =
		'w-full rounded-md border border-border bg-elevated px-3 py-2.5 text-sm text-ink placeholder:text-muted/60 focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/30 transition';
	const inputErroCls = 'border-danger/60 focus:border-danger focus:ring-danger/30';
	const labelCls = 'mb-1 block text-xs font-medium text-muted';
</script>

<svelte:head>
	<title>Encomendas — Vendas — FabLab</title>
</svelte:head>

<div class="space-y-4">
	<PageHeader title="Encomendas" subtitle="Acompanhe a produção do laboratório no Kanban.">
		{#snippet children()}
			<button
				type="button"
				onclick={abrirModal}
				class="inline-flex items-center gap-1.5 rounded-md bg-brand px-3.5 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi"
			>
				<Icon name="plus" class="h-4 w-4" /> Registrar venda
			</button>
		{/snippet}
	</PageHeader>

	{#if comErro}
		<ErrorBanner
			message="Não foi possível carregar as encomendas"
			hint="Verifique sua conexão e tente novamente. Se persistir, contate o suporte."
			onRetry={tentarNovamente}
			testid="encomendas-retry"
		/>
	{:else if carregando}
		<div
			data-testid="kanban-skeleton"
			aria-hidden="true"
			class="grid auto-cols-[minmax(16rem,1fr)] grid-flow-col gap-4 overflow-hidden pb-2"
		>
			{#each [1, 2, 3, 4, 5] as i (i)}
				<div class="h-64 animate-pulse rounded-xl border border-border bg-surface"></div>
			{/each}
		</div>
	{:else}
		<!-- KPIs servidos pelo backend (nunca derivados no client) -->
		<section class="grid grid-cols-2 gap-4 lg:grid-cols-4" aria-label="Indicadores">
			<div class="rounded-xl border border-border bg-elevated p-4">
				<p class="text-xs text-muted">Em produção</p>
				<p class="mt-1 font-mono text-2xl font-semibold tabnums text-ink">
					{fmtContagem(kpiProducao)}
				</p>
				<p class="mt-0.5 text-xs text-muted">na etapa de produção</p>
			</div>
			<div class="rounded-xl border border-border bg-elevated p-4">
				<p class="text-xs text-muted">Prontas p/ entrega</p>
				<p class="mt-1 font-mono text-2xl font-semibold tabnums text-ink">
					{fmtContagem(kpiProntas)}
				</p>
				<p class="mt-0.5 text-xs text-muted">aguardando retirada</p>
			</div>
			<div class="rounded-xl border border-border bg-elevated p-4">
				<p class="text-xs text-muted">Atrasadas</p>
				<p class="mt-1 font-mono text-2xl font-semibold tabnums text-ink">
					{fmtContagem(kpiAtrasadas)}
				</p>
				<p class="mt-0.5 text-xs text-muted">passaram da previsão</p>
			</div>
			<div class="rounded-xl border border-border bg-elevated p-4">
				<p class="text-xs text-muted">Entregues no mês</p>
				<p class="mt-1 font-mono text-2xl font-semibold tabnums text-ink">
					{fmtContagem(kpiEntreguesMes)}
				</p>
				<p class="mt-0.5 text-xs text-muted">concluídas</p>
			</div>
		</section>

		<div class="flex flex-wrap items-end gap-2">
			<div class="min-w-56 flex-1">
				<SearchInput
					value={params.search}
					onSearch={onSearch}
					placeholder="Buscar por código ou cliente…"
					delay={300}
					label="Buscar encomendas"
				/>
			</div>
			<div class="w-48">
				<label for="enc-etapa" class="mb-1 block text-xs font-medium text-muted">Etapa</label>
				<select
					id="enc-etapa"
					value={params.status_kanban}
					onchange={(e) => onEtapa((e.currentTarget as HTMLSelectElement).value)}
					class="w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm text-ink focus:border-brand focus:outline-none focus:ring-1 focus:ring-brand/40"
				>
					<option value="">Todas as etapas</option>
					{#each COLUNAS as col (col.id)}
						<option value={col.id}>{col.id}</option>
					{/each}
				</select>
			</div>
			<div class="w-48">
				<label for="enc-cliente" class="mb-1 block text-xs font-medium text-muted">Cliente</label>
				<input
					id="enc-cliente"
					type="text"
					value={params.clienteId}
					onchange={(e) => onClienteId((e.currentTarget as HTMLInputElement).value)}
					placeholder="ID do cliente…"
					class="w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm text-ink placeholder:text-muted/70 focus:border-brand focus:outline-none focus:ring-1 focus:ring-brand/40"
				/>
			</div>
			{#if filtrado}
				<button
					type="button"
					onclick={limparFiltros}
					class="rounded-md border border-border bg-elevated px-3 py-2 text-sm font-medium text-ink transition hover:border-brand/50 hover:text-brandhi"
				>
					Limpar filtros
				</button>
			{/if}
		</div>

		{#if encomendas.length === 0}
			<div class="rounded-xl border border-border bg-surface">
				{#if filtrado}
					<EmptyState
						icon="search"
						title="Nenhuma encomenda encontrada"
						description="Nenhuma encomenda combina com os filtros aplicados."
					>
						{#snippet children()}
							<button
								type="button"
								onclick={limparFiltros}
								class="rounded-md border border-border bg-elevated px-3 py-2 text-sm font-medium text-ink transition hover:border-brand/50 hover:text-brandhi"
							>
								Limpar filtros
							</button>
						{/snippet}
					</EmptyState>
				{:else}
					<EmptyState
						icon="box"
						title="Nenhuma encomenda em produção"
						description="Registre a primeira venda para iniciar o Kanban do laboratório."
					>
						{#snippet children()}
							<button
								type="button"
								onclick={abrirModal}
								class="inline-flex items-center gap-1.5 rounded-md bg-brand px-3.5 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi"
							>
								<Icon name="plus" class="h-4 w-4" /> Registrar venda
							</button>
						{/snippet}
					</EmptyState>
				{/if}
			</div>
		{:else}
			<div class="sr-only">
				{#each COLUNAS as col (col.id)}
					<span data-testid="kanban-col-{col.slug}">Coluna {col.id}</span>
				{/each}
			</div>
			<KanbanBoard columns={colunas}>
				{#snippet children({ card })}
					{@const enc = porId.get(card.id)}
					{#if enc}
						{@const atrasada = estaAtrasada(enc)}
						<div data-testid="en-card" class="w-full space-y-1.5">
							<a
								href={`/vendas/encomendas/${enc.id}`}
								aria-label={`Ver encomenda ${enc.codigo}`}
								class="font-mono text-xs font-semibold text-brandhi hover:text-brand"
							>
								{enc.codigo}
							</a>
							<p class="text-xs text-muted">
								Previsão
								<span class={atrasada ? 'font-medium text-danger' : 'text-muted'}>
									{dataBR(enc.previsao)}{atrasada ? ' · atrasada' : ''}
								</span>
							</p>
							<div class="flex flex-wrap items-center gap-1.5">
								<TypeBadge tone={origemTone(enc.origem)} label={enc.origem} />
								<span class="text-[11px] text-muted">
									{formatNumber(enc.itensCount)}
									{enc.itensCount === 1 ? 'item' : 'itens'}
								</span>
							</div>
							<div class="relative flex items-center gap-1.5">
								<button
									type="button"
									data-testid="move-en{enc.id}"
									aria-label={`Mover encomenda ${enc.codigo}`}
									aria-expanded={popoverMover === enc.id}
									aria-haspopup="menu"
									disabled={movendoId === enc.id}
									onclick={() => (popoverMover = popoverMover === enc.id ? null : enc.id)}
									class="rounded-md border border-border bg-surface px-2.5 py-1 text-xs font-medium text-ink transition hover:border-brand/50 hover:text-brandhi disabled:opacity-50"
								>
									{movendoId === enc.id ? 'Movendo…' : 'Mover'}
								</button>
								{#if popoverMover === enc.id}
									<div
										role="menu"
										aria-label={`Mover ${enc.codigo} para`}
										class="absolute top-full left-0 z-30 mt-1 min-w-40 rounded-md border border-border bg-surface p-1 shadow-2xl shadow-black/50"
									>
										{#each destinosDe(enc) as destino (destino)}
											<button
												type="button"
												role="menuitem"
												onclick={() => void mover(enc, destino)}
												class="block w-full rounded px-2.5 py-1.5 text-left text-xs text-ink transition hover:bg-elevated hover:text-brandhi"
											>
												Mover para {destino}
											</button>
										{/each}
									</div>
								{/if}
							</div>
						</div>
					{/if}
				{/snippet}
			</KanbanBoard>
		{/if}
	{/if}
</div>

<Modal
	open={modalAberto}
	title="Registrar venda"
	subtitle="A encomenda nasce na Fila do Kanban."
	onClose={() => (modalAberto = false)}
	width="md"
>
	{#snippet children()}
		<div data-testid="modal-nova-encomenda" class="space-y-4">
			<div>
				<label for="novaenc-cliente" class={labelCls}>
					Cliente <span class="text-danger">*</span>
				</label>
				{#if clienteSel}
					<div class="flex items-center justify-between gap-2 rounded-md border border-border bg-elevated/50 px-3 py-2.5">
						<span class="truncate text-sm font-medium text-ink">{clienteSel.nome}</span>
						<button
							type="button"
							onclick={() => (clienteSel = null)}
							aria-label="Remover cliente selecionado"
							class="shrink-0 rounded px-1.5 py-0.5 text-xs text-muted transition hover:text-danger"
						>
							Remover
						</button>
					</div>
				{:else}
					<input
						id="novaenc-cliente"
						type="search"
						value={buscaCliente}
						oninput={(e) => onBuscarCliente((e.currentTarget as HTMLInputElement).value)}
						placeholder="Buscar cliente por nome ou documento…"
						disabled={ocupado}
						aria-invalid={erros['cliente'] ? 'true' : undefined}
						class="{inputCls} {erros['cliente'] ? inputErroCls : ''}"
					/>
					{#if buscandoCliente}
						<p class="mt-1 text-xs text-muted">Buscando clientes…</p>
					{:else if erroBuscaCliente}
						<p role="alert" class="mt-1 text-xs text-danger">{erroBuscaCliente}</p>
					{:else if buscaCliente.trim() && resultadosCliente.length > 0}
						<ul class="mt-1 overflow-hidden rounded-md border border-border" aria-label="Clientes encontrados">
							{#each resultadosCliente as c (c.id)}
								<li>
									<button
										type="button"
										onclick={() => selecionarCliente(c)}
										class="block w-full px-3 py-2 text-left transition hover:bg-elevated"
									>
										<span class="block truncate text-sm font-medium text-ink">{c.nome}</span>
										<span class="block truncate text-xs text-muted">{c.email}</span>
									</button>
								</li>
							{/each}
						</ul>
					{:else if buscaCliente.trim()}
						<p class="mt-1 text-xs text-muted">Nenhum cliente encontrado.</p>
					{/if}
				{/if}
				{#if erros['cliente']}
					<p role="alert" class="mt-1 text-xs text-danger">{erros['cliente']}</p>
				{/if}
			</div>

			<Select
				id="novaenc-origem"
				label="Origem"
				options={ORIGENS.map((o) => ({ id: o, label: o }))}
				value={origem}
				onChange={(v) => (origem = v as OrdemOrigem)}
				disabled={ocupado}
			/>

			<div class="grid gap-4 sm:grid-cols-2">
				<div>
					<label for="novaenc-valor" class={labelCls}>
						Valor total (R$) <span class="text-danger">*</span>
					</label>
					<input
						id="novaenc-valor"
						type="text"
						inputmode="decimal"
						bind:value={valorStr}
						placeholder="0,00"
						disabled={ocupado}
						aria-invalid={erros['valor'] ? 'true' : undefined}
						class="{inputCls} {erros['valor'] ? inputErroCls : ''}"
					/>
					{#if erros['valor']}
						<p role="alert" class="mt-1 text-xs text-danger">{erros['valor']}</p>
					{/if}
				</div>
				<div>
					<label for="novaenc-previsao" class={labelCls}>Previsão de entrega</label>
					<input
						id="novaenc-previsao"
						type="date"
						bind:value={previsao}
						disabled={ocupado}
						aria-invalid={erros['previsao'] ? 'true' : undefined}
						class="{inputCls} {erros['previsao'] ? inputErroCls : ''}"
					/>
					{#if erros['previsao']}
						<p role="alert" class="mt-1 text-xs text-danger">{erros['previsao']}</p>
					{/if}
				</div>
			</div>

			<div>
				<label for="novaenc-produtos" class={labelCls}>Produtos</label>
				<textarea
					id="novaenc-produtos"
					bind:value={produtos}
					rows={3}
					placeholder="Descreva os produtos ou serviços da encomenda"
					disabled={ocupado}
					class="{inputCls} resize-y"
				></textarea>
			</div>
		</div>
	{/snippet}
	{#snippet footer()}
		<button
			type="button"
			onclick={() => (modalAberto = false)}
			disabled={ocupado}
			class="rounded-md border border-border bg-surface px-4 py-2 text-sm font-medium text-ink transition hover:bg-elevated disabled:opacity-50"
		>
			Cancelar
		</button>
		<button
			type="button"
			onclick={() => void salvarEncomenda()}
			disabled={ocupado}
			class="rounded-md bg-brand px-4 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi disabled:opacity-50"
		>
			{ocupado ? 'Salvando…' : 'Criar encomenda'}
		</button>
	{/snippet}
</Modal>
