<script lang="ts">
	import { goto, invalidateAll } from '$app/navigation';
	import type { PageProps } from './$types';
	import type { Encomenda, Plataforma, RegistroMarketplace } from '$lib/types/vendas';
	import { registrarVenda } from '$lib/api/vendas/marketplace';
	import { listEncomendas } from '$lib/api/vendas/encomendas';
	import { ApiError } from '$lib/api/client';
	import { toUserMessage } from '$lib/utils/errors';
	import { toasts } from '$lib/stores/toast';
	import { formatNumber } from '$lib/utils/format';
	import { formatDateBR, formatMoneyBRL } from '$lib/utils/vendas-format';
	import { plataformaMeta } from '$lib/utils/vendas-status';
	import PageHeader from '$lib/components/ui/PageHeader.svelte';
	import StatusBadge from '$lib/components/ui/StatusBadge.svelte';
	import TableSkeleton from '$lib/components/ui/TableSkeleton.svelte';
	import EmptyState from '$lib/components/ui/EmptyState.svelte';
	import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte';
	import Icon from '$lib/components/ui/Icon.svelte';
	import Modal from '$lib/components/ui/Modal.svelte';
	import MoneyInput from '$lib/components/ui/MoneyInput.svelte';

	let { data }: PageProps = $props();

	const params = $derived(data.params);
	const resultado = $derived(data.resultado);
	const erro = $derived(data.error);

	const registros = $derived<RegistroMarketplace[]>(resultado?.registros ?? []);
	// Totais servidos pelo backend — nunca recalculados no client (R-7).
	const totais = $derived(resultado?.totais ?? null);

	const carregando = $derived(resultado === null && erro === null);
	const comErro = $derived(erro !== null && resultado === null);

	const ABAS = [
		{ id: 'todos', label: 'Todos' },
		{ id: 'ml', label: 'Mercado Livre' },
		{ id: 'shopee', label: 'Shopee' },
		{ id: 'elo7', label: 'Elo7' }
	];

	function moeda(valor: number | null | undefined): string {
		return typeof valor === 'number' && Number.isFinite(valor) ? formatMoneyBRL(valor) : '—';
	}

	function dataBR(iso: string): string {
		if (!iso || Number.isNaN(new Date(iso).getTime())) return '—';
		return formatDateBR(iso);
	}

	interface Query {
		[key: string]: string | number | undefined;
	}

	function navegar(overrides: Query): void {
		const base: Query = {
			tab: params.tab === 'todos' ? undefined : params.tab,
			page: params.page,
			pageSize: params.pageSize === 10 ? undefined : params.pageSize
		};
		const merged = { ...base, ...overrides };
		const url = new URLSearchParams();
		for (const [chave, valor] of Object.entries(merged)) {
			if (valor !== undefined && valor !== '') url.append(chave, String(valor));
		}
		const qs = url.toString();
		void goto(`/vendas/marketplace${qs ? `?${qs}` : ''}`);
	}

	function trocarAba(id: string): void {
		navegar({ tab: id === 'todos' ? undefined : id, page: 1 });
	}

	function limparFiltros(): void {
		navegar({ tab: undefined, page: 1 });
	}

	function onPage(page: number): void {
		navegar({ page });
	}

	function onPageSize(size: number): void {
		navegar({ pageSize: size === 10 ? undefined : size, page: 1 });
	}

	function tentarNovamente(): void {
		void goto(`/vendas/marketplace${window.location.search}`, { invalidateAll: true });
	}

	// ---- Modal Registrar venda externa (POST /api/vendas/marketplace 🔴) ----

	const PLATAFORMAS: Plataforma[] = ['Mercado Livre', 'Shopee', 'Elo7'];

	let modalAberto = $state(false);
	let plataforma = $state<Plataforma | ''>('');
	let codigoExterno = $state('');
	let encSel = $state<Encomenda | null>(null);
	let buscaEnc = $state('');
	let resultadosEnc = $state<Encomenda[]>([]);
	let buscandoEnc = $state(false);
	let erroBuscaEnc = $state<string | null>(null);
	let dataVenda = $state('');
	let taxa: number | null = $state(null);
	let erros = $state<Record<string, string>>({});
	let ocupado = $state(false);

	let timerBusca: ReturnType<typeof setTimeout> | null = null;
	let ctrlBusca: AbortController | null = null;

	// Líquido exibido só como preview no modal — nunca persistido nem totalizado no client.
	const liquidoPreview = $derived(
		encSel !== null && taxa !== null ? encSel.valorFinal - taxa : null
	);

	function abrirModal(): void {
		plataforma = '';
		codigoExterno = '';
		encSel = null;
		buscaEnc = '';
		resultadosEnc = [];
		buscandoEnc = false;
		erroBuscaEnc = null;
		dataVenda = '';
		taxa = null;
		erros = {};
		modalAberto = true;
	}

	function fecharModal(): void {
		if (ocupado) return;
		ctrlBusca?.abort();
		modalAberto = false;
	}

	function onBuscarEnc(termo: string): void {
		buscaEnc = termo;
		if (timerBusca) clearTimeout(timerBusca);
		if (!termo.trim()) {
			resultadosEnc = [];
			buscandoEnc = false;
			erroBuscaEnc = null;
			ctrlBusca?.abort();
			return;
		}
		buscandoEnc = true;
		erroBuscaEnc = null;
		const atual = termo.trim();
		timerBusca = setTimeout(() => void executarBuscaEnc(atual), 300);
	}

	async function executarBuscaEnc(termo: string): Promise<void> {
		ctrlBusca?.abort();
		const ctrl = new AbortController();
		ctrlBusca = ctrl;
		const sinal = ctrl.signal;
		try {
			const res = await listEncomendas({ search: termo }, (input, init) =>
				fetch(input, { ...init, signal: sinal })
			);
			if (!sinal.aborted) {
				resultadosEnc = res.encomendas.slice(0, 8);
				buscandoEnc = false;
			}
		} catch (err) {
			if (sinal.aborted) return;
			buscandoEnc = false;
			erroBuscaEnc = toUserMessage(err).message;
		}
	}

	function selecionarEnc(enc: Encomenda): void {
		encSel = enc;
		resultadosEnc = [];
		buscaEnc = '';
		if (erros['encomenda']) erros = { ...erros, encomenda: '' };
	}

	async function salvarVenda(): Promise<void> {
		if (ocupado) return;
		const novos: Record<string, string> = {};
		if (!plataforma) novos['plataforma'] = 'Selecione a plataforma.';
		if (!codigoExterno.trim()) novos['codigoExterno'] = 'Informe o código externo.';
		if (!encSel) novos['encomenda'] = 'Vincule uma encomenda.';
		if (dataVenda && Number.isNaN(new Date(dataVenda).getTime()))
			novos['dataVenda'] = 'Informe uma data válida.';
		if (taxa === null || !Number.isFinite(taxa) || taxa < 0)
			novos['taxa'] = 'Informe a taxa em reais (valor, nunca percentual).';
		if (encSel && taxa !== null && taxa > encSel.valorFinal)
			novos['taxa'] = 'Taxa não pode exceder o valor da encomenda.';
		erros = novos;
		if (Object.keys(novos).length > 0 || !encSel || !plataforma) return;
		ocupado = true;
		try {
			await registrarVenda({
				encomendaId: encSel.id,
				plataforma,
				codigoExterno: codigoExterno.trim(),
				dataVenda: dataVenda || new Date().toISOString().slice(0, 10),
				valorTaxa: taxa as number
			});
			toasts.success('Venda externa registrada.');
			modalAberto = false;
			await invalidateAll();
		} catch (err) {
			if (err instanceof ApiError && err.status === 403) {
				toasts.warn('Sem permissão para esta ação.');
			} else {
				toasts.danger(toUserMessage(err).message);
			}
		} finally {
			ocupado = false;
		}
	}

	const inputCls =
		'w-full rounded-md border border-border bg-elevated px-3 py-2.5 text-sm text-ink placeholder:text-muted/60 focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/30 transition';
	const inputErroCls = 'border-danger/60 focus:border-danger focus:ring-danger/30';
	const labelCls = 'mb-1 block text-xs font-medium text-muted';

	const temProxima = $derived(registros.length >= params.pageSize);
</script>

<svelte:head>
	<title>Marketplace — Vendas — FabLab</title>
</svelte:head>

<div class="space-y-4">
	<PageHeader
		title="Marketplace"
		subtitle="Vendas externas em Mercado Livre, Shopee e Elo7 com taxas e líquido."
	>
		{#snippet children()}
			<button
				type="button"
				onclick={abrirModal}
				data-testid="mp-registrar"
				aria-label="Registrar venda externa"
				class="inline-flex items-center gap-1.5 rounded-md bg-brand px-3.5 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi"
			>
				<Icon name="plus" class="h-4 w-4" /> Registrar venda externa
			</button>
		{/snippet}
	</PageHeader>

	{#if comErro}
		<ErrorBanner
			message="Não foi possível carregar as vendas de marketplace"
			hint="Verifique sua conexão e tente novamente. Se persistir, contate o suporte."
			onRetry={tentarNovamente}
			testid="mp-retry"
		/>
	{:else if carregando}
		<div class="grid grid-cols-2 gap-4 lg:grid-cols-4" aria-hidden="true">
			{#each [1, 2, 3, 4] as i (i)}
				<div class="h-24 animate-pulse rounded-xl border border-border bg-elevated"></div>
			{/each}
		</div>
		<div class="h-10 animate-pulse rounded-lg bg-elevated"></div>
		<div class="rounded-xl border border-border bg-surface p-4">
			<TableSkeleton rows={6} columns={6} />
		</div>
	{:else}
		<!-- KPIs servidos pelo backend (nunca derivados no client) -->
		<section class="grid grid-cols-2 gap-4 lg:grid-cols-4" aria-label="Indicadores">
			<div class="rounded-xl border border-border bg-elevated p-4">
				<p class="text-xs text-muted">Bruto</p>
				<p class="mt-1 font-mono text-2xl font-semibold tabnums text-ink">
					{moeda(totais?.bruto)}
				</p>
				<p class="mt-0.5 text-xs text-muted">valor das encomendas</p>
			</div>
			<div class="rounded-xl border border-border bg-elevated p-4">
				<p class="text-xs text-muted">Taxas</p>
				<p class="mt-1 font-mono text-2xl font-semibold tabnums text-ink">
					{moeda(totais?.taxas)}
				</p>
				<p class="mt-0.5 text-xs text-muted">cobrança das plataformas</p>
			</div>
			<div class="rounded-xl border border-border bg-elevated p-4">
				<p class="text-xs text-muted">Líquido</p>
				<p
					data-testid="mp-liquido"
					class="mt-1 font-mono text-2xl font-semibold tabnums text-ink"
				>
					{moeda(totais?.liquido)}
				</p>
				<p class="mt-0.5 text-xs text-muted">bruto menos taxas</p>
			</div>
			<div class="rounded-xl border border-border bg-elevated p-4">
				<p class="text-xs text-muted">Vendas no período</p>
				<p class="mt-1 font-mono text-2xl font-semibold tabnums text-ink">
					{formatNumber(registros.length)}
				</p>
				<p class="mt-0.5 text-xs text-muted">registros listados</p>
			</div>
		</section>

		<div
			role="tablist"
			aria-label="Filtrar vendas por plataforma"
			class="flex items-center gap-1 overflow-x-auto border-b border-border px-1"
		>
			{#each ABAS as aba (aba.id)}
				{@const ativa = params.tab === aba.id}
				<button
					type="button"
					role="tab"
					aria-selected={ativa}
					data-testid="mp-tab-{aba.id}"
					onclick={() => trocarAba(aba.id)}
					class="inline-flex shrink-0 items-center gap-1.5 border-b-2 px-3 py-2 text-sm font-medium transition {ativa
						? 'border-brand text-brand'
						: 'border-transparent text-muted hover:text-ink'}"
				>
					{aba.label}
				</button>
			{/each}
		</div>

		{#if registros.length === 0}
			<div class="rounded-xl border border-border bg-surface">
				<EmptyState
					icon="tag"
					title={params.tab === 'todos'
						? 'Nenhuma venda externa registrada'
						: 'Nenhuma venda nesta plataforma'}
					description={params.tab === 'todos'
						? 'Registre a primeira venda externa para acompanhar bruto, taxas e líquido.'
						: 'Nenhum registro para esta plataforma no período.'}
				>
					{#snippet children()}
						<div class="flex flex-wrap items-center justify-center gap-2">
							{#if params.tab !== 'todos'}
								<button
									type="button"
									onclick={limparFiltros}
									class="rounded-md border border-border bg-elevated px-3 py-2 text-sm font-medium text-ink transition hover:border-brand/50 hover:text-brandhi"
								>
									Limpar filtros
								</button>
							{/if}
							<button
								type="button"
								onclick={abrirModal}
								class="inline-flex items-center gap-1.5 rounded-md bg-brand px-3.5 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi"
							>
								<Icon name="plus" class="h-4 w-4" /> Registrar venda externa
							</button>
						</div>
					{/snippet}
				</EmptyState>
			</div>
		{:else}
			<div class="overflow-hidden rounded-xl border border-border bg-surface">
				<div class="overflow-x-auto">
					<table class="w-full min-w-[880px] text-sm">
						<thead>
							<tr
								class="border-b border-border bg-elevated/50 text-left text-[11px] uppercase tracking-wide text-muted"
							>
								<th class="px-4 py-2.5 font-medium">Plataforma</th>
								<th class="px-4 py-2.5 font-medium">Código</th>
								<th class="px-4 py-2.5 font-medium">Encomenda</th>
								<th class="px-4 py-2.5 font-medium">Cliente</th>
								<th class="px-4 py-2.5 font-medium">Data</th>
								<th class="px-4 py-2.5 text-right font-medium">Bruto</th>
								<th class="px-4 py-2.5 text-right font-medium">Taxa</th>
								<th class="px-4 py-2.5 text-right font-medium">Líquido</th>
							</tr>
						</thead>
						<tbody>
							{#each registros as registro (registro.id)}
								{@const meta = plataformaMeta(registro.plataforma)}
								<tr
									class="border-b border-border transition last:border-0 hover:bg-elevated/40"
								>
									<td class="px-4 py-3">
										<StatusBadge label={meta.label} color={meta.color} />
									</td>
									<td class="px-4 py-3 font-mono text-xs text-muted">{registro.codigoExterno}</td>
									<td class="px-4 py-3">
										<a
											href="/vendas/encomendas?search={encodeURIComponent(
												registro.encomenda.codigo
											)}"
											aria-label="Ver encomenda {registro.encomenda.codigo}"
											class="font-mono text-xs text-brandhi transition hover:text-brand"
										>
											{registro.encomenda.codigo || '—'}
										</a>
										<span class="ml-1.5 font-mono text-xs text-muted tabnums"
											>{moeda(registro.encomenda.valor)}</span
										>
									</td>
									<td class="max-w-44 truncate px-4 py-3 text-ink">{registro.clienteNome}</td>
									<td class="px-4 py-3 font-mono text-xs text-muted tabnums">
										{dataBR(registro.dataVenda)}
									</td>
									<td class="px-4 py-3 text-right font-mono text-sm tabnums text-ink">
										{moeda(registro.valorBruto)}
									</td>
									<td class="px-4 py-3 text-right font-mono text-sm tabnums text-ink">
										{moeda(registro.valorTaxa)}
									</td>
									<td
										class="px-4 py-3 text-right font-mono text-sm font-semibold tabnums text-ink"
									>
										{moeda(registro.valorLiquido)}
									</td>
								</tr>
							{/each}
						</tbody>
					</table>
				</div>
				<!-- Card de totais do servido (exibição direta, sem recalcular) -->
				<div
					class="flex flex-wrap items-center gap-x-6 gap-y-1 border-t border-border bg-elevated/40 px-4 py-3"
					aria-label="Totais do período"
				>
					<p class="text-xs text-muted">
						Bruto <span class="ml-1 font-mono text-sm font-semibold text-ink"
							>{moeda(totais?.bruto)}</span
						>
					</p>
					<p class="text-xs text-muted">
						Taxas <span class="ml-1 font-mono text-sm font-semibold text-ink"
							>{moeda(totais?.taxas)}</span
						>
					</p>
					<p class="text-xs text-muted">
						Líquido <span class="ml-1 font-mono text-sm font-semibold text-ink"
							>{moeda(totais?.liquido)}</span
						>
					</p>
				</div>
				<div class="border-t border-border px-4 py-3">
					<div class="flex flex-wrap items-center justify-between gap-2">
						<p class="text-xs text-muted">página {params.page} · {registros.length} vendas</p>
						<div class="flex items-center gap-2">
							<label class="flex items-center gap-1.5 text-xs text-muted">
								<span>Exibir</span>
								<select
									value={params.pageSize}
									onchange={(e) =>
										onPageSize(Number((e.currentTarget as HTMLSelectElement).value))}
									aria-label="Itens por página"
									class="rounded-md border border-border bg-surface px-1.5 py-1 text-xs text-ink focus:border-brand focus:outline-none"
								>
									{#each [10, 25, 50, 100] as size (size)}
										<option value={size}>{size}</option>
									{/each}
								</select>
							</label>
							<div class="flex items-center gap-1">
								<button
									type="button"
									onclick={() => onPage(params.page - 1)}
									disabled={params.page <= 1}
									aria-label="Página anterior"
									class="inline-flex h-8 w-8 items-center justify-center rounded-lg border border-border bg-surface text-muted transition-colors hover:border-brand/50 hover:text-brandhi disabled:cursor-not-allowed disabled:opacity-40"
								>
									<Icon name="arrow-left" class="h-4 w-4" />
								</button>
								<button
									type="button"
									onclick={() => onPage(params.page + 1)}
									disabled={!temProxima}
									aria-label="Próxima página"
									class="inline-flex h-8 w-8 items-center justify-center rounded-lg border border-border bg-surface text-muted transition-colors hover:border-brand/50 hover:text-brandhi disabled:cursor-not-allowed disabled:opacity-40"
								>
									<Icon name="arrow-right" class="h-4 w-4" />
								</button>
							</div>
						</div>
					</div>
				</div>
			</div>
		{/if}
	{/if}
</div>

<Modal
	open={modalAberto}
	title="Registrar venda externa"
	subtitle="Vincule a encomenda e informe a taxa cobrada pela plataforma, em reais."
	onClose={fecharModal}
>
	{#snippet children()}
		<div data-testid="modal-registrar-venda" class="space-y-4">
			<div>
				<label for="mp-plataforma" class={labelCls}
					>Plataforma <span class="text-danger">*</span></label
				>
				<select
					id="mp-plataforma"
					value={plataforma}
					onchange={(e) => {
						plataforma = (e.currentTarget as HTMLSelectElement).value as Plataforma | '';
						if (erros['plataforma']) erros = { ...erros, plataforma: '' };
					}}
					aria-invalid={!!erros['plataforma']}
					class="{inputCls} {erros['plataforma'] ? inputErroCls : ''}"
				>
					<option value="" disabled>Selecione…</option>
					{#each PLATAFORMAS as p (p)}
						<option value={p}>{p}</option>
					{/each}
				</select>
				{#if erros['plataforma']}
					<p role="alert" class="mt-1 text-xs text-danger">{erros['plataforma']}</p>
				{/if}
			</div>

			<div>
				<label for="mp-codigo" class={labelCls}>Código externo <span class="text-danger">*</span></label
				>
				<input
					id="mp-codigo"
					type="text"
					value={codigoExterno}
					oninput={(e) => {
						codigoExterno = (e.currentTarget as HTMLInputElement).value;
						if (erros['codigoExterno']) erros = { ...erros, codigoExterno: '' };
					}}
					placeholder="ML-000000"
					aria-invalid={!!erros['codigoExterno']}
					class="{inputCls} font-mono {erros['codigoExterno'] ? inputErroCls : ''}"
				/>
				{#if erros['codigoExterno']}
					<p role="alert" class="mt-1 text-xs text-danger">{erros['codigoExterno']}</p>
				{/if}
			</div>

			<div>
				<span id="mp-enc-label" class={labelCls}
					>Encomenda vinculada <span class="text-danger">*</span></span
				>
				{#if encSel}
					<div class="flex items-center justify-between gap-2 rounded-md border border-border bg-elevated px-3 py-2.5">
						<div class="min-w-0">
							<p class="font-mono text-xs text-ink">{encSel.codigo}</p>
							<p class="truncate text-xs text-muted">
								{encSel.cliente.nome} · {formatMoneyBRL(encSel.valorFinal)}
							</p>
						</div>
						<button
							type="button"
							onclick={() => (encSel = null)}
							aria-label="Remover encomenda vinculada"
							class="rounded-md p-1 text-muted transition hover:bg-border/50 hover:text-ink"
						>
							<Icon name="x-mark" class="h-4 w-4" />
						</button>
					</div>
				{:else}
					<input
						type="text"
						value={buscaEnc}
						oninput={(e) => onBuscarEnc((e.currentTarget as HTMLInputElement).value)}
						placeholder="Buscar encomenda por código ou cliente…"
						role="combobox"
						aria-controls="mp-enc-lista"
						aria-expanded={resultadosEnc.length > 0}
						aria-labelledby="mp-enc-label"
						aria-invalid={!!erros['encomenda']}
						class="{inputCls} {erros['encomenda'] ? inputErroCls : ''}"
					/>
					{#if buscandoEnc}
						<p class="mt-1 text-xs text-muted">Buscando encomendas…</p>
					{:else if erroBuscaEnc}
						<p role="alert" class="mt-1 text-xs text-danger">{erroBuscaEnc}</p>
					{:else if resultadosEnc.length > 0}
						<ul
							id="mp-enc-lista"
							role="listbox"
							aria-labelledby="mp-enc-label"
							class="mt-1 max-h-44 divide-y divide-border overflow-y-auto rounded-md border border-border bg-surface"
						>
							{#each resultadosEnc as enc (enc.id)}
								<li>
									<button
										type="button"
										role="option"
										aria-selected="false"
										onclick={() => selecionarEnc(enc)}
										class="flex w-full items-center justify-between gap-2 px-3 py-2 text-left transition hover:bg-elevated"
									>
										<span class="min-w-0">
											<span class="block font-mono text-xs text-ink">{enc.codigo}</span>
											<span class="block truncate text-xs text-muted">{enc.cliente.nome}</span>
										</span>
										<span class="shrink-0 font-mono text-xs text-muted tabnums"
											>{formatMoneyBRL(enc.valorFinal)}</span
										>
									</button>
								</li>
							{/each}
						</ul>
					{/if}
				{/if}
				{#if erros['encomenda']}
					<p role="alert" class="mt-1 text-xs text-danger">{erros['encomenda']}</p>
				{/if}
			</div>

			<div>
				<label for="mp-data" class={labelCls}>Data da venda</label>
				<input
					id="mp-data"
					type="date"
					value={dataVenda}
					onchange={(e) => {
						dataVenda = (e.currentTarget as HTMLInputElement).value;
						if (erros['dataVenda']) erros = { ...erros, dataVenda: '' };
					}}
					aria-invalid={!!erros['dataVenda']}
					class="{inputCls} {erros['dataVenda'] ? inputErroCls : ''}"
				/>
				{#if erros['dataVenda']}
					<p role="alert" class="mt-1 text-xs text-danger">{erros['dataVenda']}</p>
				{/if}
			</div>

			<div>
				<MoneyInput bind:value={taxa} label="Taxa da plataforma (R$)" />
				{#if erros['taxa']}
					<p role="alert" class="mt-1 text-xs text-danger">{erros['taxa']}</p>
				{/if}
				<p class="mt-1 text-xs text-muted">Informe o valor em reais, nunca em percentual.</p>
			</div>

			{#if liquidoPreview !== null}
				<div class="rounded-md border border-border bg-elevated/60 px-3 py-2.5" aria-live="polite">
					<p class="text-xs text-muted">
						Líquido previsto <span class="ml-1 font-mono text-sm font-semibold text-ink"
							>{formatMoneyBRL(liquidoPreview)}</span
						>
					</p>
					<p class="mt-0.5 text-[11px] text-muted">Pré-visualização — o líquido oficial é calculado pelo servidor.</p>
				</div>
			{/if}
		</div>
	{/snippet}
	{#snippet footer()}
		<button
			type="button"
			onclick={fecharModal}
			disabled={ocupado}
			class="rounded-md border border-border bg-elevated px-3.5 py-2 text-sm font-medium text-ink transition hover:bg-elevated/70 disabled:opacity-50"
		>
			Cancelar
		</button>
		<button
			type="button"
			onclick={salvarVenda}
			disabled={ocupado}
			aria-label="Salvar venda externa"
			class="rounded-md bg-brand px-3.5 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi disabled:opacity-50"
		>
			{ocupado ? 'Salvando…' : 'Salvar venda'}
		</button>
	{/snippet}
</Modal>
