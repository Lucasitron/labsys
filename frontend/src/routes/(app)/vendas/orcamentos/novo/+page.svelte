<script lang="ts">
	import { goto } from '$app/navigation';
	import { page } from '$app/state';
	import { get } from 'svelte/store';
	import { auth } from '$lib/stores/auth';
	import type {
		Cliente,
		CreateOrcamentoPayload,
		ItemOrcamento,
		Orcamento
	} from '$lib/types/vendas';
	import type { StockItem } from '$lib/types/stock';
	import { getCliente, listClientes } from '$lib/api/vendas/clientes';
	import {
		createOrcamento,
		getOrcamento,
		updateOrcamento
	} from '$lib/api/vendas/orcamentos';
	import { listarItens } from '$lib/api/stock/items';
	import { canEditVendas } from '$lib/utils/permissions';
	import { toUserMessage } from '$lib/utils/errors';
	import { toasts } from '$lib/stores/toast';
	import { formatNumber } from '$lib/utils/format';
	import { formatMoneyBRL } from '$lib/utils/vendas-format';
	import PageHeader from '$lib/components/ui/PageHeader.svelte';
	import SearchInput from '$lib/components/ui/SearchInput.svelte';
	import Avatar from '$lib/components/ui/Avatar.svelte';
	import Icon from '$lib/components/ui/Icon.svelte';
	import TypeBadge from '$lib/components/ui/TypeBadge.svelte';
	import MoneyInput from '$lib/components/ui/MoneyInput.svelte';
	import Modal from '$lib/components/ui/Modal.svelte';
	import RadioCards from '$lib/components/ui/RadioCards.svelte';
	import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte';
	import SolicitarEdicaoModal from '../../components/SolicitarEdicaoModal.svelte';

	const usuario = get(auth).user;

	const editarId = $derived(page.url.searchParams.get('editar') ?? '');
	const modoEdicao = $derived(editarId !== '');

	const inputCls =
		'w-full rounded-md border border-border bg-elevated px-3 py-2.5 text-sm text-ink placeholder:text-muted/60 focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/30 transition';
	const inputErroCls = 'border-danger/60 focus:border-danger focus:ring-danger/30';
	const labelCls = 'mb-1 block text-xs font-medium text-muted';
	const cardCls = 'rounded-xl border border-border bg-surface p-5';

	function isoHojeMais30(): string {
		const d = new Date(Date.now() + 30 * 86_400_000);
		const mm = String(d.getMonth() + 1).padStart(2, '0');
		const dd = String(d.getDate()).padStart(2, '0');
		return `${d.getFullYear()}-${mm}-${dd}`;
	}

	// ---- Estado do formulário ----

	interface ClienteSel {
		id: string;
		nome: string;
		email?: string;
	}

	let clienteSel = $state<ClienteSel | null>(null);
	let itens = $state<ItemOrcamento[]>([]);
	let desconto = $state<number | null>(null);
	let validade = $state(isoHojeMais30());
	let observacoes = $state('');

	let ocupado = $state(false);
	let erros = $state<Record<string, string>>({});
	let erroTopo = $state<string | null>(null);

	// ---- Origem em modo edição (?editar=) ----

	let origemEstado = $state<'idle' | 'carregando' | 'pronto' | 'erro'>('idle');
	let origemCodigo = $state('');
	let podeEditarOrigem = $state(true);
	let sugerirEdicao = $state(false);

	function aplicarOrigem(orc: Orcamento): void {
		origemCodigo = orc.codigo;
		clienteSel = { id: orc.cliente.id, nome: orc.cliente.nome };
		itens = (orc.itens ?? []).map((it) => ({
			descricao: it.descricao,
			quantidade: it.quantidade,
			valorUnitario: it.valorUnitario,
			...(it.material ? { material: { ...it.material } } : {}),
			...(it.horas !== undefined ? { horas: it.horas } : {}),
			...(it.compra === true ? { compra: true } : {})
		}));
		if (orc.validade) validade = orc.validade.slice(0, 10);
		observacoes = orc.observacoes ?? '';
		podeEditarOrigem = canEditVendas(usuario, { createdBy: orc.createdBy });
		void getCliente(orc.cliente.id)
			.then((c: Cliente) => {
				clienteSel = { id: c.id, nome: c.nome, email: c.email };
			})
			.catch(() => {});
	}

	$effect(() => {
		if (!editarId || origemEstado !== 'idle') return;
		origemEstado = 'carregando';
		const ctrl = new AbortController();
		const sinal = ctrl.signal;
		getOrcamento(editarId, (input, init) => fetch(input, { ...init, signal: sinal }))
			.then((orc) => {
				if (!sinal.aborted) {
					aplicarOrigem(orc);
					origemEstado = 'pronto';
				}
			})
			.catch(() => {
				if (!sinal.aborted) origemEstado = 'erro';
			});
		return () => ctrl.abort();
	});

	// ---- Busca de cliente (debounce 300ms + AbortController) ----

	let resultadosCliente = $state<Cliente[]>([]);
	let buscandoCliente = $state(false);
	let erroBuscaCliente = $state<string | null>(null);
	let ultimoTermoCliente = $state('');
	let timerBusca: ReturnType<typeof setTimeout> | null = null;
	let ctrlBusca: AbortController | null = null;

	function onBuscarCliente(termo: string): void {
		if (timerBusca) clearTimeout(timerBusca);
		if (!termo.trim()) {
			resultadosCliente = [];
			buscandoCliente = false;
			erroBuscaCliente = null;
			ultimoTermoCliente = '';
			ctrlBusca?.abort();
			return;
		}
		ultimoTermoCliente = termo.trim();
		buscandoCliente = true;
		erroBuscaCliente = null;
		timerBusca = setTimeout(() => void executarBuscaCliente(ultimoTermoCliente), 300);
	}

	async function executarBuscaCliente(termo: string): Promise<void> {
		ctrlBusca?.abort();
		const ctrl = new AbortController();
		ctrlBusca = ctrl;
		const sinal = ctrl.signal;
		try {
			const res = await listClientes(
				{ search: termo, page: 1, pageSize: 8 },
				(input, init) => fetch(input, { ...init, signal: sinal })
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
		clienteSel = { id: c.id, nome: c.nome, email: c.email };
		resultadosCliente = [];
		if (erros['cliente']) erros = { ...erros, cliente: '' };
	}

	function removerCliente(): void {
		clienteSel = null;
	}

	function focarBuscaCliente(): void {
		(
			document
				.getElementById('orc-cliente')
				?.querySelector('input[type="search"]') as HTMLInputElement | null
		)?.focus();
	}

	// ---- Overlay modal-item-orcamento ----

	let modalAberto = $state(false);
	let indiceEdicao = $state<number | null>(null);
	let origem = $state<'estoque' | 'compra'>('estoque');
	let buscaMaterial = $state('');
	let materiais = $state<StockItem[]>([]);
	let materiaisCarregando = $state(false);
	let materiaisErro = $state<string | null>(null);
	let materialSelId = $state<string | null>(null);
	let precisaComprar = $state(false);

	let descricao = $state('');
	let tipoMaterial = $state('');
	let qtdMaterial = $state(0);
	let unidadeMaterial = $state('un');
	let horas = $state(0);
	let quantidade = $state(1);
	let valorUnitario = $state<number | null>(null);
	let faltas = $state<string[]>([]);

	let materiaisCtrl: AbortController | null = null;

	const materiaisFiltrados = $derived(
		buscaMaterial.trim()
			? materiais.filter((m) => m.nome.toLowerCase().includes(buscaMaterial.trim().toLowerCase()))
			: materiais
	);

	const subtotalItem = $derived(
		quantidade > 0 && valorUnitario !== null ? quantidade * valorUnitario : null
	);

	async function carregarMateriais(): Promise<void> {
		if (materiaisCarregando) return;
		materiaisCarregando = true;
		materiaisErro = null;
		materiaisCtrl?.abort();
		const ctrl = new AbortController();
		materiaisCtrl = ctrl;
		const sinal = ctrl.signal;
		try {
			const lista = await listarItens({}, (input, init) =>
				fetch(input, { ...init, signal: sinal })
			);
			if (!sinal.aborted) materiais = lista;
		} catch (err) {
			if (sinal.aborted) return;
			materiaisErro = err instanceof Error ? err.message : 'Erro ao carregar materiais.';
		} finally {
			if (!sinal.aborted) materiaisCarregando = false;
		}
	}

	function abrirOverlay(indice: number | null): void {
		faltas = [];
		buscaMaterial = '';
		materialSelId = null;
		if (indice === null) {
			indiceEdicao = null;
			origem = 'estoque';
			precisaComprar = false;
			descricao = '';
			tipoMaterial = '';
			qtdMaterial = 0;
			unidadeMaterial = 'un';
			horas = 0;
			quantidade = 1;
			valorUnitario = null;
		} else {
			const it = itens[indice];
			if (!it) return;
			indiceEdicao = indice;
			origem = it.compra === true ? 'compra' : 'estoque';
			precisaComprar = it.compra === true;
			descricao = it.descricao;
			tipoMaterial = it.material?.tipo ?? '';
			qtdMaterial = it.material?.quantidade ?? 0;
			unidadeMaterial = it.material?.unidade ?? 'un';
			horas = it.horas ?? 0;
			quantidade = it.quantidade;
			valorUnitario = it.valorUnitario;
		}
		modalAberto = true;
		if (origem === 'estoque' && materiais.length === 0 && !materiaisErro) {
			void carregarMateriais();
		}
	}

	function selecionarMaterial(m: StockItem): void {
		materialSelId = m.id;
		tipoMaterial = m.nome;
		unidadeMaterial = m.unidadeMedida;
		if (!descricao.trim()) descricao = m.nome;
	}

	function focarCampoItem(campo: string): void {
		const raiz = document.getElementById('modal-item-orcamento');
		if (!raiz) return;
		if (campo === 'valor unitário') {
			(raiz.querySelector('input[inputmode="decimal"]') as HTMLInputElement | null)?.focus();
			return;
		}
		const ids: Record<string, string> = {
			descrição: 'item-descricao',
			material: 'item-material',
			'quantidade do material': 'item-qtd-material',
			quantidade: 'item-quantidade'
		};
		document.getElementById(ids[campo] ?? '')?.focus();
	}

	function salvarItem(): void {
		const pendentes: string[] = [];
		if (!descricao.trim()) pendentes.push('descrição');
		if (!tipoMaterial.trim()) pendentes.push('material');
		if (!(qtdMaterial > 0)) pendentes.push('quantidade do material');
		if (!(quantidade > 0)) pendentes.push('quantidade');
		if (valorUnitario === null || !(valorUnitario >= 0)) pendentes.push('valor unitário');
		faltas = pendentes;
		if (pendentes.length > 0) {
			focarCampoItem(pendentes[0] ?? '');
			return;
		}
		const item: ItemOrcamento = {
			descricao: descricao.trim(),
			quantidade,
			valorUnitario: valorUnitario ?? 0,
			material: {
				tipo: tipoMaterial.trim(),
				quantidade: qtdMaterial,
				unidade: unidadeMaterial.trim() || 'un'
			},
			...(horas > 0 ? { horas } : {}),
			...(origem === 'compra' && precisaComprar ? { compra: true } : {})
		};
		if (indiceEdicao === null) itens = [...itens, item];
		else itens = itens.map((it, i) => (i === indiceEdicao ? item : it));
		if (erros['itens']) erros = { ...erros, itens: '' };
		modalAberto = false;
	}

	function removerItem(indice: number): void {
		itens = itens.filter((_, i) => i !== indice);
	}

	// ---- Totais (prévia local; backend é autoritativo) ----

	const subtotalItens = $derived(
		itens.reduce((acc, it) => acc + it.quantidade * it.valorUnitario, 0)
	);
	const descontoNum = $derived(desconto ?? 0);
	const totalOrc = $derived(Math.max(0, subtotalItens - descontoNum));

	function itensPayload(): ItemOrcamento[] {
		return itens.map((it) => ({
			descricao: it.descricao,
			quantidade: it.quantidade,
			valorUnitario: it.valorUnitario,
			...(it.material ? { material: { ...it.material } } : {}),
			...(it.horas !== undefined ? { horas: it.horas } : {}),
			...(it.compra === true ? { compra: true } : {})
		}));
	}

	async function salvar(): Promise<void> {
		if (ocupado) return;
		const novos: Record<string, string> = {};
		if (!clienteSel) novos['cliente'] = 'Selecione o cliente do orçamento.';
		if (itens.length === 0) novos['itens'] = 'Adicione ao menos 1 item ao orçamento.';
		if (!validade) novos['validade'] = 'Informe a validade do orçamento.';
		erros = novos;
		erroTopo = null;
		if (Object.keys(novos).length > 0) {
			erroTopo = 'Verifique os campos destacados e tente novamente.';
			if (novos['cliente']) focarBuscaCliente();
			return;
		}
		ocupado = true;
		try {
			const payload: CreateOrcamentoPayload = {
				clienteId: (clienteSel as ClienteSel).id,
				itens: itensPayload(),
				validade,
				...(observacoes.trim() ? { observacoes: observacoes.trim() } : {}),
				...(descontoNum > 0 ? { desconto: descontoNum } : {})
			};
			const salvo = modoEdicao
				? await updateOrcamento(editarId, payload)
				: await createOrcamento(payload);
			toasts.success(modoEdicao ? 'Orçamento atualizado.' : 'Orçamento criado com sucesso.');
			void goto(`/vendas/orcamentos/${salvo.id}`);
		} catch (err) {
			erroTopo = toUserMessage(err).message;
		} finally {
			ocupado = false;
		}
	}

	function voltar(): void {
		if (window.history.length > 1) window.history.back();
		else void goto('/vendas/orcamentos');
	}

	const UNIDADES = ['kg', 'g', 'un', 'm', 'cm', 'L', 'ml'];
	const salvarDesabilitado = $derived(ocupado || (modoEdicao && !podeEditarOrigem));
</script>

<svelte:head>
	<title>{modoEdicao ? 'Editar orçamento' : 'Novo orçamento'} — Vendas — FabLab</title>
</svelte:head>

<div class="space-y-4 pb-24">
	<div>
		<button
			type="button"
			onclick={voltar}
			class="inline-flex items-center gap-1.5 text-xs font-medium text-muted transition-colors hover:text-ink"
		>
			<Icon name="arrow-left" class="h-3.5 w-3.5" /> Voltar aos orçamentos
		</button>
		<div class="mt-1">
			<PageHeader
				title={modoEdicao
					? `Editar orçamento${origemCodigo ? ` ${origemCodigo}` : ''}`
					: 'Novo orçamento'}
				subtitle={modoEdicao
					? 'Revise cliente, itens, prazos e salve as alterações.'
					: 'Monte o orçamento com cliente, itens e prazos.'}
			/>
		</div>
	</div>

	{#if erroTopo}
		<ErrorBanner message={erroTopo} hint="Confira os campos destacados abaixo." />
	{/if}

	{#if modoEdicao && origemEstado === 'carregando'}
		<div class="{cardCls} space-y-3" aria-hidden="true">
			<div class="h-5 w-40 animate-pulse rounded bg-elevated"></div>
			<div class="h-10 animate-pulse rounded-lg bg-elevated"></div>
		</div>
	{:else if modoEdicao && origemEstado === 'erro'}
		<div class={cardCls}>
			<ErrorBanner
				message="Não foi possível carregar o orçamento"
				hint="Verifique sua conexão e tente novamente."
				onRetry={() => (origemEstado = 'idle')}
			/>
		</div>
	{:else}
		{#if modoEdicao && !podeEditarOrigem}
			<div
				role="alert"
				class="flex flex-wrap items-center justify-between gap-2 rounded-xl border border-warn/30 bg-warn/10 px-4 py-3"
			>
				<p class="text-sm text-warn">Você não pode editar este orçamento. Sugira uma alteração.</p>
				<button
					type="button"
					onclick={() => (sugerirEdicao = true)}
					class="rounded-md border border-warn/30 bg-warn/15 px-3 py-1.5 text-xs font-medium text-warn transition hover:bg-warn/25"
				>
					Sugerir alteração
				</button>
			</div>
		{/if}

		<!-- Card Cliente -->
		<section id="orc-cliente" data-testid="orc-cliente" class={cardCls} aria-label="Cliente do orçamento">
			<h2 class="text-sm font-semibold text-ink">Cliente <span class="text-danger">*</span></h2>
			{#if clienteSel}
				<div
					class="mt-3 flex flex-wrap items-center gap-3 rounded-lg border border-border bg-elevated/50 p-3"
				>
					<Avatar name={clienteSel.nome} size="sm" tone="brand" />
					<div class="min-w-0 flex-1">
						<p class="truncate text-sm font-medium text-ink">{clienteSel.nome}</p>
						{#if clienteSel.email}
							<p class="truncate text-xs text-muted">{clienteSel.email}</p>
						{/if}
					</div>
					{#if !modoEdicao}
						<button
							type="button"
							onclick={removerCliente}
							aria-label={`Remover ${clienteSel.nome}`}
							class="inline-flex h-8 w-8 items-center justify-center rounded-md text-muted transition hover:bg-border/40 hover:text-ink"
						>
							<Icon name="x-mark" class="h-4 w-4" />
						</button>
					{/if}
				</div>
			{:else}
				<div class="mt-3 space-y-2">
					<SearchInput
						value=""
						onSearch={onBuscarCliente}
						placeholder="Buscar cliente por nome ou documento…"
						delay={300}
						label="Buscar cliente por nome ou documento"
					/>
					{#if buscandoCliente}
						<p class="text-xs text-muted" role="status">Buscando clientes…</p>
					{:else if erroBuscaCliente}
						<ErrorBanner
							message="Não foi possível buscar clientes"
							hint={erroBuscaCliente}
							onRetry={() => {
								if (ultimoTermoCliente) void executarBuscaCliente(ultimoTermoCliente);
							}}
						/>
					{:else if resultadosCliente.length > 0}
						<ul role="listbox" aria-label="Clientes encontrados" class="space-y-1">
							{#each resultadosCliente as c (c.id)}
								<li>
									<button
										type="button"
										role="option"
										aria-selected="false"
										onclick={() => selecionarCliente(c)}
										class="flex w-full items-center gap-3 rounded-lg border border-transparent p-2 text-left transition hover:border-brand/40 hover:bg-elevated/60"
									>
										<Avatar name={c.nome} size="sm" tone="brand" />
										<span class="min-w-0 flex-1">
											<span class="block truncate text-sm font-medium text-ink">{c.nome}</span>
											<span class="block truncate font-mono text-xs text-muted">
												{c.codigo} · {c.documento}
											</span>
										</span>
									</button>
								</li>
							{/each}
						</ul>
					{/if}
					<p class="text-xs text-muted">
						Não encontrou?
						<a href="/vendas/clientes/novo" class="font-medium text-brandhi hover:text-brand">
							Cadastrar novo cliente
						</a>
					</p>
				</div>
			{/if}
			{#if erros['cliente']}
				<p role="alert" class="mt-2 text-xs text-danger">{erros['cliente']}</p>
			{/if}
		</section>

		<!-- Card Itens -->
		<section class={cardCls} aria-label="Itens do orçamento">
			<div class="flex flex-wrap items-center justify-between gap-2">
				<h2 class="text-sm font-semibold text-ink">Itens <span class="text-danger">*</span></h2>
				<button
					type="button"
					data-testid="orc-item-add"
					onclick={() => abrirOverlay(null)}
					aria-label="Adicionar item ao orçamento"
					class="inline-flex items-center gap-1.5 rounded-md border border-border bg-elevated px-3 py-2 text-sm font-medium text-ink transition hover:border-brand/50 hover:text-brandhi"
				>
					<Icon name="plus" class="h-4 w-4" /> Adicionar item
				</button>
			</div>
			{#if itens.length === 0}
				<p class="mt-3 rounded-lg border border-dashed border-border p-4 text-center text-xs text-muted">
					Nenhum item adicionado. Use “Adicionar item” para montar o orçamento.
				</p>
			{:else}
				<div class="mt-3 overflow-x-auto rounded-lg border border-border">
					<table class="w-full min-w-[680px] text-sm">
						<thead>
							<tr
								class="border-b border-border bg-elevated/50 text-left text-[11px] uppercase tracking-wide text-muted"
							>
								<th class="px-4 py-2.5 font-medium">Item</th>
								<th class="px-4 py-2.5 text-right font-medium">Horas</th>
								<th class="px-4 py-2.5 text-right font-medium">Qtd</th>
								<th class="px-4 py-2.5 text-right font-medium">Valor unit.</th>
								<th class="px-4 py-2.5 text-right font-medium">Subtotal</th>
								<th class="w-20 px-4 py-2.5 text-right font-medium">
									<span class="sr-only">Ações</span>
								</th>
							</tr>
						</thead>
						<tbody>
							{#each itens as item, i (i)}
								<tr class="border-b border-border transition last:border-0 hover:bg-elevated/40">
									<td class="px-4 py-3">
										<div class="flex flex-wrap items-center gap-1.5">
											<TypeBadge
												tone={item.compra === true ? 'warn' : 'brand'}
												label={item.compra === true ? 'Compra' : 'Estoque'}
											/>
											<span class="font-medium text-ink">{item.descricao}</span>
										</div>
										{#if item.material}
											<p class="mt-0.5 text-xs text-muted">
												{item.material.tipo} · {formatNumber(item.material.quantidade)}
												{item.material.unidade}
											</p>
										{/if}
									</td>
									<td class="px-4 py-3 text-right font-mono text-sm tabnums text-ink">
										{item.horas !== undefined ? formatNumber(item.horas) : '—'}
									</td>
									<td class="px-4 py-3 text-right font-mono text-sm tabnums text-ink">
										{formatNumber(item.quantidade)}
									</td>
									<td class="px-4 py-3 text-right font-mono text-sm tabnums text-ink">
										{formatMoneyBRL(item.valorUnitario)}
									</td>
									<td
										class="px-4 py-3 text-right font-mono text-sm font-medium tabnums text-ink"
									>
										{formatMoneyBRL(item.quantidade * item.valorUnitario)}
									</td>
									<td class="px-4 py-3">
										<div class="flex items-center justify-end gap-1">
											<button
												type="button"
												onclick={() => abrirOverlay(i)}
												aria-label={`Editar ${item.descricao}`}
												class="inline-flex h-8 w-8 items-center justify-center rounded-md text-muted transition hover:bg-border/40 hover:text-ink"
											>
												<Icon name="pencil" class="h-4 w-4" />
											</button>
											<button
												type="button"
												onclick={() => removerItem(i)}
												aria-label={`Remover ${item.descricao}`}
												class="inline-flex h-8 w-8 items-center justify-center rounded-md text-muted transition hover:bg-danger/15 hover:text-danger"
											>
												<Icon name="trash" class="h-4 w-4" />
											</button>
										</div>
									</td>
								</tr>
							{/each}
						</tbody>
					</table>
				</div>
			{/if}
			{#if erros['itens']}
				<p role="alert" class="mt-2 text-xs text-danger">{erros['itens']}</p>
			{/if}
		</section>

		<!-- Card Resumo -->
		<section class="{cardCls} bg-elevated/40" aria-label="Resumo do orçamento">
			<h2 class="text-sm font-semibold text-ink">Resumo</h2>
			<dl class="mt-3 space-y-2 text-sm">
				<div class="flex items-center justify-between">
					<dt class="text-muted">Subtotal</dt>
					<dd class="font-mono tabnums text-ink">{formatMoneyBRL(subtotalItens)}</dd>
				</div>
				<div class="flex flex-wrap items-center justify-between gap-2">
					<dt class="text-muted"><label for="orc-desconto">Desconto (R$)</label></dt>
					<dd class="w-40">
						<MoneyInput bind:value={desconto} label="" />
					</dd>
				</div>
				<div class="flex items-center justify-between border-t border-border pt-2">
					<dt class="font-semibold text-ink">Total</dt>
					<dd data-testid="orc-total" class="font-mono text-lg font-semibold tabnums text-ink">
						{formatMoneyBRL(totalOrc)}
					</dd>
				</div>
			</dl>
			<p class="mt-2 text-xs text-muted">
				Prévia calculada localmente — o total final é confirmado pelo servidor ao salvar.
			</p>
		</section>

		<!-- Card Prazos -->
		<section class={cardCls} aria-label="Prazos e observações">
			<h2 class="text-sm font-semibold text-ink">Prazos e observações</h2>
			<div class="mt-3 grid gap-4 sm:grid-cols-2">
				<div>
					<label for="orc-validade" class={labelCls}>
						Validade <span class="text-danger">*</span>
					</label>
					<input
						id="orc-validade"
						type="date"
						bind:value={validade}
						aria-invalid={erros['validade'] ? 'true' : undefined}
						class="{inputCls} {erros['validade'] ? inputErroCls : ''}"
					/>
					{#if erros['validade']}
						<p role="alert" class="mt-1 text-xs text-danger">{erros['validade']}</p>
					{/if}
				</div>
				<div>
					<label for="orc-observacoes" class={labelCls}>Observações</label>
					<textarea
						id="orc-observacoes"
						bind:value={observacoes}
						rows={3}
						placeholder="Condições, prazos de produção, detalhes do pedido"
						class="{inputCls} resize-y"
					></textarea>
				</div>
			</div>
		</section>
	{/if}
</div>

<!-- Footer fixo -->
{#if !(modoEdicao && (origemEstado === 'carregando' || origemEstado === 'erro'))}
	<div
		class="fixed inset-x-0 bottom-0 z-20 border-t border-border bg-surface/95 backdrop-blur"
	>
		<div class="mx-auto flex max-w-5xl flex-wrap items-center justify-end gap-2 px-4 py-3">
			<button
				type="button"
				onclick={voltar}
				disabled={ocupado}
				class="rounded-md border border-border bg-surface px-4 py-2 text-sm font-medium text-ink transition hover:bg-elevated disabled:opacity-50"
			>
				Cancelar
			</button>
			<button
				type="button"
				data-testid="submit-orcamento"
				onclick={() => void salvar()}
				disabled={salvarDesabilitado}
				aria-label={modoEdicao ? 'Salvar alterações do orçamento' : 'Salvar orçamento'}
				class="rounded-md bg-brand px-4 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi disabled:opacity-50"
			>
				{ocupado ? 'Salvando…' : modoEdicao ? 'Salvar alterações' : 'Salvar orçamento'}
			</button>
		</div>
	</div>
{/if}

<!-- Overlay modal-item-orcamento -->
{#if modalAberto}
	<div id="modal-item-orcamento" data-testid="modal-item-orcamento">
		<Modal
			open={modalAberto}
			title={indiceEdicao === null ? 'Adicionar item' : 'Editar item'}
			subtitle="Origem do material e preço de venda"
			onClose={() => (modalAberto = false)}
			width="lg"
		>
			{#snippet children()}
				<div class="space-y-4">
					<RadioCards
						name="item-origem"
						options={[
							{
								id: 'estoque',
								label: 'Do estoque',
								description: 'Material disponível no laboratório',
								color: 'brand'
							},
							{
								id: 'compra',
								label: 'Fora do estoque',
								description: 'Material precisa ser comprado',
								color: 'warn'
							}
						]}
						value={origem}
						onChange={(v) => (origem = v === 'compra' ? 'compra' : 'estoque')}
					/>

					{#if origem === 'estoque'}
						<div>
							<SearchInput
								value={buscaMaterial}
								onSearch={(v) => (buscaMaterial = v)}
								placeholder="Buscar material no estoque…"
								delay={300}
								label="Buscar material no estoque"
							/>
							{#if materiaisCarregando}
								<p class="mt-2 text-xs text-muted" role="status">Carregando materiais…</p>
							{:else if materiaisErro}
								<div class="mt-2">
									<ErrorBanner
										message="Não foi possível carregar os materiais"
										hint={materiaisErro}
										onRetry={() => void carregarMateriais()}
									/>
								</div>
							{:else if materiaisFiltrados.length === 0}
								<p class="mt-2 text-xs text-muted">
									{buscaMaterial
										? 'Nenhum material combina com a busca.'
										: 'Nenhum material em estoque. Troque a origem ou cadastre o material no estoque.'}
								</p>
							{:else}
								<ul
									role="listbox"
									aria-label="Materiais disponíveis"
									class="mt-2 max-h-44 space-y-1 overflow-y-auto"
								>
									{#each materiaisFiltrados.slice(0, 20) as m (m.id)}
										<li>
											<button
												type="button"
												role="option"
												aria-selected={materialSelId === m.id}
												onclick={() => selecionarMaterial(m)}
												class="flex w-full items-center justify-between gap-2 rounded-lg border p-2 text-left transition {materialSelId ===
												m.id
													? 'border-brand bg-brand/10'
													: 'border-transparent hover:border-brand/40 hover:bg-elevated/60'}"
											>
												<span class="min-w-0">
													<span class="block truncate text-sm font-medium text-ink">{m.nome}</span>
													<span class="block text-xs text-muted">
														Em estoque: {formatNumber(m.quantidadeAtual)}
														{m.unidadeMedida}
													</span>
												</span>
												{#if materialSelId === m.id}
													<Icon name="check" class="h-4 w-4 shrink-0 text-brandhi" />
												{/if}
											</button>
										</li>
									{/each}
								</ul>
							{/if}
						</div>
					{:else}
						<div class="space-y-3 rounded-lg border border-warn/30 bg-warn/5 p-3">
							<p class="text-xs text-muted">
								Informe o tipo de material livre. Marque abaixo se ele precisa ser comprado.
							</p>
							<label class="flex cursor-pointer items-center gap-2 text-sm text-ink">
								<input
									type="checkbox"
									bind:checked={precisaComprar}
									class="h-4 w-4 rounded border-border bg-surface accent-brand"
								/>
								Precisa ser comprado
							</label>
						</div>
					{/if}

					<div class="grid gap-4 sm:grid-cols-2">
						<div class="sm:col-span-2">
							<label for="item-descricao" class={labelCls}>
								Descrição <span class="text-danger">*</span>
							</label>
							<input
								id="item-descricao"
								type="text"
								bind:value={descricao}
								placeholder="Ex.: Suporte de celular em PLA"
								class={inputCls}
							/>
						</div>
						<div>
							<label for="item-material" class={labelCls}>
								Material <span class="text-danger">*</span>
							</label>
							<input
								id="item-material"
								type="text"
								bind:value={tipoMaterial}
								placeholder={origem === 'estoque' ? 'Selecione acima ou digite' : 'Ex.: PLA preto'}
								class={inputCls}
							/>
						</div>
						<div class="grid grid-cols-2 gap-2">
							<div>
								<label for="item-qtd-material" class={labelCls}>
									Qtd. material <span class="text-danger">*</span>
								</label>
								<input
									id="item-qtd-material"
									type="number"
									bind:value={qtdMaterial}
									min={0}
									step="any"
									class="{inputCls} font-mono"
								/>
							</div>
							<div>
								<label for="item-unidade" class={labelCls}>Unidade</label>
								<input
									id="item-unidade"
									type="text"
									bind:value={unidadeMaterial}
									list="item-unidades"
									class={inputCls}
								/>
								<datalist id="item-unidades">
									{#each UNIDADES as u (u)}
										<option value={u}></option>
									{/each}
								</datalist>
							</div>
						</div>
						<div>
							<label for="item-horas" class={labelCls}>Horas</label>
							<input
								id="item-horas"
								type="number"
								bind:value={horas}
								min={0}
								step="any"
								class="{inputCls} font-mono"
							/>
						</div>
						<div>
							<label for="item-quantidade" class={labelCls}>
								Quantidade <span class="text-danger">*</span>
							</label>
							<input
								id="item-quantidade"
								type="number"
								bind:value={quantidade}
								min={1}
								step="1"
								class="{inputCls} font-mono"
							/>
						</div>
						<div class="sm:col-span-2">
							<MoneyInput bind:value={valorUnitario} label="Valor unitário *" />
						</div>
					</div>

					<div class="flex items-center justify-between rounded-lg border border-border bg-elevated/50 px-3 py-2">
						<span class="text-xs text-muted">Prévia do subtotal (qtd × valor unit.)</span>
						<span class="font-mono text-sm font-semibold tabnums text-ink">
							{subtotalItem !== null ? formatMoneyBRL(subtotalItem) : '—'}
						</span>
					</div>

					{#if faltas.length > 0}
						<p role="alert" class="rounded-lg border border-danger/30 bg-danger/10 px-3 py-2 text-xs text-danger">
							Faltam informações: {faltas.join(', ')}.
						</p>
					{/if}
				</div>
			{/snippet}
			{#snippet footer()}
				<button
					type="button"
					onclick={() => (modalAberto = false)}
					class="rounded-md border border-border bg-surface px-4 py-2 text-sm font-medium text-ink transition hover:bg-elevated"
				>
					Cancelar
				</button>
				<button
					type="button"
					onclick={salvarItem}
					class="rounded-md bg-brand px-4 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi"
				>
					{indiceEdicao === null ? 'Adicionar item' : 'Salvar item'}
				</button>
			{/snippet}
		</Modal>
	</div>
{/if}

{#if sugerirEdicao && modoEdicao}
	<SolicitarEdicaoModal
		alvo={{ tipo: 'OC', id: editarId, nome: origemCodigo || editarId }}
		onClose={() => (sugerirEdicao = false)}
	/>
{/if}
