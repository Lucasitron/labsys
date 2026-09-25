<script lang="ts">
	import { goto, invalidateAll } from '$app/navigation';
	import type { PageProps } from './$types';
	import type { CategoriaFinanceira, Lancamento } from '$lib/types/financeiro';
	import { registrarPagamento } from '$lib/api/financeiro/lancamentos';
	import { lancamentoStatusMeta, tipoMeta } from '$lib/utils/financeiro-status';
	import { formatSignedBRL } from '$lib/utils/financeiro-format';
	import { formatDateBR, formatMoneyBRL } from '$lib/utils/vendas-format';
	import { toUserMessage } from '$lib/utils/errors';
	import { toasts } from '$lib/stores/toast';
	import PageHeader from '$lib/components/ui/PageHeader.svelte';
	import SearchInput from '$lib/components/ui/SearchInput.svelte';
	import Dropdown from '$lib/components/ui/Dropdown.svelte';
	import Chip from '$lib/components/ui/Chip.svelte';
	import Pagination from '$lib/components/ui/Pagination.svelte';
	import StatusBadge from '$lib/components/ui/StatusBadge.svelte';
	import TypeBadge from '$lib/components/ui/TypeBadge.svelte';
	import Icon from '$lib/components/ui/Icon.svelte';
	import TableSkeleton from '$lib/components/ui/TableSkeleton.svelte';
	import EmptyState from '$lib/components/ui/EmptyState.svelte';
	import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte';
	import RowActions from '$lib/components/ui/RowActions.svelte';
	import Modal from '$lib/components/ui/Modal.svelte';
	import NovoLancamentoModal from '../components/NovoLancamentoModal.svelte';

	let { data }: PageProps = $props();

	const params = $derived(data.params);
	const resultado = $derived(data.resultado);
	const erro = $derived(data.error);
	const categorias = $derived(data.categorias as CategoriaFinanceira[]);
	const categoriasError = $derived(data.categoriasError);

	const lancamentos = $derived(resultado?.lancamentos ?? []);
	const paginacao = $derived(resultado?.pagination);
	const resumo = $derived(resultado?.resumo);
	const counts = $derived(resultado?.counts);

	const carregando = $derived(resultado === null && erro === null);
	const comErro = $derived(erro !== null && resultado === null);

	const temFiltros = $derived(
		params.search !== '' || params.status !== '' || params.vencimento !== ''
	);

	const STATUS_OPCOES = ['Pendente', 'Atrasado'].map((id) => ({ id, label: id }));
	const VENCIMENTO_OPCOES = [
		{ id: 'vencidas', label: 'Vencidas' },
		{ id: 'mes', label: 'Vencem este mês' },
		{ id: 'proximos-30d', label: 'Próximos 30 dias' }
	];
	// ---- Modal: nova conta (lançamento com tipo fixo Saída) ----

	let modalNovo = $state(false);

	function abrirNovo(): void {
		modalNovo = true;
	}

	function dataLocal(iso: string): Date {
		const [a, m, d] = iso.slice(0, 10).split('-').map(Number);
		return new Date(a, (m || 1) - 1, d || 1);
	}

	// Filtro de vencimento aplicado sobre a página servida (sem param na API — D-4).
	function noVencimento(l: Lancamento): boolean {
		if (params.vencimento === '') return true;
		if (params.vencimento === 'vencidas') return l.status === 'Atrasado';
		const hoje = new Date();
		hoje.setHours(0, 0, 0, 0);
		const venc = dataLocal(l.dataVencimento);
		if (params.vencimento === 'mes') {
			return venc.getFullYear() === hoje.getFullYear() && venc.getMonth() === hoje.getMonth();
		}
		const limite = new Date(hoje);
		limite.setDate(limite.getDate() + 30);
		return venc >= hoje && venc <= limite;
	}

	const visiveis = $derived(lancamentos.filter(noVencimento));

	interface Query {
		[key: string]: string | number | undefined;
	}

	function navegar(overrides: Query): void {
		const base: Query = {
			search: params.search || undefined,
			status: params.status || undefined,
			vencimento: params.vencimento || undefined,
			page: params.page
		};
		const merged = { ...base, ...overrides };
		const url = new URLSearchParams();
		for (const [chave, valor] of Object.entries(merged)) {
			if (valor !== undefined && valor !== '') url.append(chave, String(valor));
		}
		const qs = url.toString();
		void goto(`/financeiro/contas-pagar${qs ? `?${qs}` : ''}`);
	}

	function onSearch(termo: string): void {
		navegar({ search: termo || undefined, page: 1 });
	}

	function alternarFiltro(chave: 'status' | 'vencimento', id: string): void {
		const atual = params[chave];
		navegar({ [chave]: atual === id ? undefined : id, page: 1 });
	}

	function limparFiltro(chave: 'status' | 'vencimento'): void {
		navegar({ [chave]: undefined, page: 1 });
	}

	function limparTodos(): void {
		navegar({ search: undefined, status: undefined, vencimento: undefined, page: 1 });
	}

	function onPage(page: number): void {
		navegar({ page });
	}

	function tentarNovamente(): void {
		void goto(`/financeiro/contas-pagar${window.location.search}`, { invalidateAll: true });
	}

	function contaDe(l: Lancamento): string {
		return l.observacao?.trim() ? (l.observacao as string) : l.categoria.nome;
	}

	function referencia(l: Lancamento): string {
		return l.idReferenciaExterna ? `${l.codigo} • REF ${l.idReferenciaExterna}` : l.codigo;
	}

	// ---- Modal: nova conta via componente compartilhado (tipo fixo Saída) ----

	let ocupado = $state(false);

	// ---- Modal: detalhe + pagamento ----

	let detalhe = $state<Lancamento | null>(null);
	let modalPagamento = $state(false);
	let pagamentoAlvo = $state<Lancamento | null>(null);
	let pagamentoData = $state('');
	let pagamentoObs = $state('');

	function hojeISO(): string {
		return new Date().toISOString().slice(0, 10);
	}

	function abrirDetalhe(l: Lancamento): void {
		detalhe = l;
	}

	function pedirPagamento(l: Lancamento): void {
		pagamentoAlvo = l;
		pagamentoData = hojeISO();
		pagamentoObs = '';
		detalhe = null;
		modalPagamento = true;
	}

	async function confirmarPagamento(): Promise<void> {
		if (!pagamentoAlvo) return;
		if (!pagamentoData) {
			toasts.warn('Informe a data do pagamento.');
			return;
		}
		ocupado = true;
		try {
			await registrarPagamento(pagamentoAlvo.id, {
				dataPagamento: pagamentoData,
				observacao: pagamentoObs.trim() || undefined
			});
			toasts.success('Pagamento registrado com sucesso.');
			modalPagamento = false;
			pagamentoAlvo = null;
			await invalidateAll();
		} catch (err) {
			toasts.danger(toUserMessage(err).message);
		} finally {
			ocupado = false;
		}
	}

	function acaoLinha(l: Lancamento, acao: string): void {
		if (acao === 'detalhe') abrirDetalhe(l);
		else if (acao === 'pagamento') pedirPagamento(l);
	}

	const detalheStatus = $derived(detalhe ? lancamentoStatusMeta(detalhe.status) : null);
	const detalheTipo = $derived(detalhe ? tipoMeta(detalhe.tipo) : null);
	const vencidas = $derived(counts?.Atrasado ?? 0);
</script>

<svelte:head>
	<title>Contas a pagar — Financeiro — FabLab</title>
</svelte:head>

<div class="space-y-4">
	<PageHeader
		title="Contas a pagar"
		subtitle="Saídas pendentes e atrasadas derivadas dos lançamentos."
	>
		{#snippet children()}
			<button
				type="button"
				data-testid="cp-nova"
				onclick={abrirNovo}
				aria-label="Nova conta a pagar"
				class="inline-flex items-center gap-1.5 rounded-md bg-brand px-3.5 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi"
			>
				<Icon name="plus" class="h-4 w-4" /> Nova conta a pagar
			</button>
		{/snippet}
	</PageHeader>

	{#if comErro}
		<ErrorBanner
			message="Não foi possível carregar as contas a pagar"
			hint="Verifique sua conexão e tente novamente. Se persistir, contate o suporte."
			onRetry={tentarNovamente}
			testid="cp-retry"
		/>
	{:else if carregando}
		<div class="grid gap-3 sm:grid-cols-2 lg:grid-cols-4" aria-hidden="true">
			{#each Array(4) as _, i (i)}
				<div class="h-20 animate-pulse rounded-xl bg-elevated"></div>
			{/each}
		</div>
		<div class="rounded-xl border border-border bg-surface p-4">
			<TableSkeleton rows={6} columns={6} />
		</div>
	{:else}
		{#if resumo && counts}
			<div class="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
				<div class="rounded-xl border border-border bg-elevated p-4">
					<p class="text-xs text-muted">A pagar no mês</p>
					<p class="mt-1 font-mono text-lg font-semibold text-danger tabular-nums">
						{formatMoneyBRL(resumo.pendente)}
					</p>
					<p class="mt-0.5 text-xs text-muted">{counts.Pendente} pendentes</p>
				</div>
				<div class="rounded-xl border border-border bg-elevated p-4">
					<p class="text-xs text-muted">Vencidas</p>
					<p
						class="mt-1 font-mono text-lg font-semibold tabular-nums {vencidas > 0
							? 'text-danger'
							: 'text-success'}"
					>
						{vencidas}
					</p>
					<p class="mt-0.5 text-xs {vencidas > 0 ? 'text-danger' : 'text-success'}">
						{vencidas === 0 ? 'Em dia ✓' : `${vencidas} contas vencidas`}
					</p>
				</div>
				<div class="rounded-xl border border-border bg-elevated p-4">
					<p class="text-xs text-muted">Pagas no mês</p>
					<p class="mt-1 font-mono text-lg font-semibold text-success tabular-nums">
						{formatMoneyBRL(resumo.saidas)}
					</p>
					<p class="mt-0.5 text-xs text-muted">{counts.Pago} pagas</p>
				</div>
				<div class="rounded-xl border border-border bg-elevated p-4">
					<p class="text-xs text-muted">Contas em aberto</p>
					<p class="mt-1 font-mono text-lg font-semibold text-ink tabular-nums">
						{paginacao?.totalItems ?? 0}
					</p>
					<p class="mt-0.5 text-xs text-muted">nesta vista</p>
				</div>
			</div>
		{/if}

		{#if categoriasError}
			<p role="alert" class="rounded-xl border border-warn/30 bg-warn/10 px-4 py-2.5 text-xs text-warn">
				{categoriasError}. O modal de nova conta pode estar sem categorias.
			</p>
		{/if}

		<div class="flex flex-wrap items-center gap-2">
			<div class="min-w-56 flex-1">
				<SearchInput
					value={params.search}
					onSearch={onSearch}
					placeholder="Buscar por conta, categoria ou referência…"
					delay={300}
					label="Buscar contas a pagar"
				/>
			</div>
			<div data-testid="fpanel-cp-status">
				<Dropdown
					label="Status"
					options={STATUS_OPCOES}
					selected={params.status ? [params.status] : []}
					onToggle={(id) => alternarFiltro('status', id)}
					onClear={() => limparFiltro('status')}
					search={false}
				/>
			</div>
			<div data-testid="fpanel-cp-vence">
				<Dropdown
					label="Vencimento"
					options={VENCIMENTO_OPCOES}
					selected={params.vencimento ? [params.vencimento] : []}
					onToggle={(id) => alternarFiltro('vencimento', id)}
					onClear={() => limparFiltro('vencimento')}
					search={false}
				/>
			</div>
		</div>

		{#if temFiltros}
			<div data-testid="fin-chips" class="flex flex-wrap items-center gap-1.5">
				{#if params.search}
					<Chip label={`Busca: ${params.search}`} onRemove={() => onSearch('')} />
				{/if}
				{#if params.status}
					<Chip label={params.status} onRemove={() => limparFiltro('status')} />
				{/if}
				{#if params.vencimento}
					<Chip
						label={VENCIMENTO_OPCOES.find((o) => o.id === params.vencimento)?.label ??
							params.vencimento}
						onRemove={() => limparFiltro('vencimento')}
					/>
				{/if}
				<button
					type="button"
					onclick={limparTodos}
					class="text-xs font-medium text-brandhi transition-colors hover:text-brand"
				>
					Limpar filtros
				</button>
			</div>
		{/if}

		{#if visiveis.length === 0}
			<div class="rounded-xl border border-border bg-surface">
				{#if temFiltros}
					<EmptyState
						icon="search"
						title="Nenhuma conta encontrada"
						description="Nenhuma conta a pagar combina com os filtros aplicados."
					>
						{#snippet children()}
							<button
								type="button"
								onclick={limparTodos}
								class="rounded-md border border-border bg-elevated px-3 py-2 text-sm font-medium text-ink transition hover:border-brand/50 hover:text-brandhi"
							>
								Limpar filtros
							</button>
						{/snippet}
					</EmptyState>
				{:else}
					<EmptyState
						icon="document"
						title="Nenhuma conta a pagar"
						description="Não há saídas pendentes ou atrasadas no momento."
					>
						{#snippet children()}
							<button
								type="button"
								onclick={abrirNovo}
								class="inline-flex items-center gap-1.5 rounded-md bg-brand px-3.5 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi"
							>
								<Icon name="plus" class="h-4 w-4" /> Nova conta a pagar
							</button>
						{/snippet}
					</EmptyState>
				{/if}
			</div>
		{:else}
			<div class="hidden overflow-hidden rounded-xl border border-border bg-surface md:block">
				<div class="overflow-x-auto">
					<table class="w-full min-w-[820px] text-sm">
						<thead>
							<tr
								class="border-b border-border bg-elevated/50 text-left text-[11px] uppercase tracking-wide text-muted"
							>
								<th class="px-4 py-2.5 font-medium">Conta / fornecedor</th>
								<th class="px-4 py-2.5 font-medium">Categoria</th>
								<th class="px-4 py-2.5 font-medium">Vencimento</th>
								<th class="px-4 py-2.5 text-right font-medium">Valor</th>
								<th class="px-4 py-2.5 font-medium">Status</th>
								<th class="w-32 px-4 py-2.5 text-right font-medium">
									<span class="sr-only">Ações</span>
								</th>
							</tr>
						</thead>
						<tbody>
							{#each visiveis as l (l.id)}
								{@const st = lancamentoStatusMeta(l.status)}
								{@const atrasado = l.status === 'Atrasado'}
								<tr
									data-testid="cp-row"
									onclick={() => abrirDetalhe(l)}
									class="cursor-pointer border-b border-border transition last:border-0 hover:bg-elevated/40 {atrasado
										? 'bg-danger/[0.03]'
										: ''}"
								>
									<td class="px-4 py-3">
										<p class="truncate font-medium text-ink">{contaDe(l)}</p>
										<p class="font-mono text-xs text-muted">{referencia(l)}</p>
									</td>
									<td class="px-4 py-3 text-xs text-muted">{l.categoria.nome}</td>
									<td
										class="px-4 py-3 font-mono text-xs tabular-nums {atrasado
											? 'text-danger'
											: 'text-muted'}"
									>
										{formatDateBR(l.dataVencimento)}
									</td>
									<td class="px-4 py-3 text-right font-mono text-sm text-danger tabular-nums">
										{formatSignedBRL(l.valor, l.tipo)}
									</td>
									<td class="px-4 py-3">
										<StatusBadge label={st.label} color={st.color} />
									</td>
									<td class="px-4 py-3" onclick={(e) => e.stopPropagation()}>
										<div class="flex items-center justify-end gap-1.5">
											<button
												type="button"
												data-testid="cp-pagar"
												onclick={() => pedirPagamento(l)}
												aria-label={`Registrar pagamento de ${l.codigo}`}
												class="rounded-md border border-success/30 bg-success/10 px-2.5 py-1.5 text-xs font-medium text-success transition hover:bg-success/20"
											>
												Pagar
											</button>
											<RowActions
												label={`Ações de ${l.codigo}`}
												actions={[
													{ id: 'detalhe', label: 'Ver detalhe', icon: 'eye' },
													{ id: 'pagamento', label: 'Registrar pagamento', icon: 'check' }
												]}
												onSelect={(id) => acaoLinha(l, id)}
											/>
										</div>
									</td>
								</tr>
							{/each}
						</tbody>
					</table>
				</div>
				{#if paginacao && resumo}
					<div class="space-y-2 border-t border-border px-4 py-3">
						<p class="text-xs text-muted">
							{paginacao.totalItems} contas a pagar · total {formatMoneyBRL(resumo.pendente)}
						</p>
						<Pagination
							page={paginacao.page}
							totalPages={paginacao.totalPages}
							totalItems={paginacao.totalItems}
							onPage={onPage}
							label="contas"
						/>
					</div>
				{/if}
			</div>

			<div class="space-y-2 md:hidden">
				{#each visiveis as l (l.id)}
					{@const st = lancamentoStatusMeta(l.status)}
					<article
						data-testid="cp-row"
						class="rounded-xl border border-border bg-surface p-4 {l.status === 'Atrasado'
							? 'bg-danger/[0.03]'
							: ''}"
					>
						<button
							type="button"
							onclick={() => abrirDetalhe(l)}
							aria-label={`Ver ${l.codigo}`}
							class="block w-full text-left"
						>
							<span class="block truncate text-sm font-medium text-ink">{contaDe(l)}</span>
							<span class="block font-mono text-xs text-muted">{referencia(l)}</span>
							<span
								class="mt-1 block font-mono text-xs {l.status === 'Atrasado'
									? 'text-danger'
									: 'text-muted'}"
							>
								Vence em {formatDateBR(l.dataVencimento)} · {l.categoria.nome}
							</span>
							<span class="mt-2 flex flex-wrap items-center gap-1.5">
								<StatusBadge label={st.label} color={st.color} />
							</span>
							<span class="mt-1.5 block font-mono text-sm text-danger tabular-nums">
								{formatSignedBRL(l.valor, l.tipo)}
							</span>
						</button>
						<div class="mt-3 flex items-center justify-end gap-1.5">
							<button
								type="button"
								data-testid="cp-pagar"
								onclick={() => pedirPagamento(l)}
								aria-label={`Registrar pagamento de ${l.codigo}`}
								class="rounded-md border border-success/30 bg-success/10 px-2.5 py-1.5 text-xs font-medium text-success transition hover:bg-success/20"
							>
								Pagar
							</button>
							<RowActions
								label={`Ações de ${l.codigo}`}
								actions={[
									{ id: 'detalhe', label: 'Ver detalhe', icon: 'eye' },
									{ id: 'pagamento', label: 'Registrar pagamento', icon: 'check' }
								]}
								onSelect={(id) => acaoLinha(l, id)}
							/>
						</div>
					</article>
				{/each}
				{#if paginacao && resumo}
					<div class="space-y-2 rounded-xl border border-border bg-surface px-4 py-3">
						<p class="text-xs text-muted">
							{paginacao.totalItems} contas a pagar · total {formatMoneyBRL(resumo.pendente)}
						</p>
						<Pagination
							page={paginacao.page}
							totalPages={paginacao.totalPages}
							totalItems={paginacao.totalItems}
							onPage={onPage}
							label="contas"
						/>
					</div>
				{/if}
			</div>
		{/if}
	{/if}
</div>

<NovoLancamentoModal
	open={modalNovo}
	categorias={categorias}
	idPrefix="cp-novo"
	tipoFixo="Saída"
	title="Nova conta a pagar"
	subtitle="Registre uma saída com vencimento"
	confirmLabel="Criar conta a pagar"
	categoriaWarn="Selecione a categoria da conta."
	sucessoMsg="Conta a pagar criada com sucesso."
	observacaoLabel="Conta / fornecedor"
	observacaoPlaceholder="Ex.: Aluguel do laboratório"
	referenciaPlaceholder="Ex.: ABR-2026"
	onClose={() => (modalNovo = false)}
	onCreated={() => invalidateAll()}
/>

<Modal
	open={detalhe !== null}
	title={detalhe ? detalhe.codigo : 'Conta'}
	subtitle="Ficha da conta a pagar"
	onClose={() => (detalhe = null)}
	width="sm"
>
	{#snippet children()}
		{#if detalhe && detalheStatus && detalheTipo}
			<dl class="space-y-2.5 text-sm">
				<div class="flex items-center justify-between gap-2">
					<dt class="text-xs text-muted">Status</dt>
					<dd><StatusBadge label={detalheStatus.label} color={detalheStatus.color} /></dd>
				</div>
				<div class="flex items-center justify-between gap-2">
					<dt class="text-xs text-muted">Tipo</dt>
					<dd>
						<TypeBadge
							tone={detalheTipo.color === 'success' ? 'success' : 'danger'}
							label={detalheTipo.label}
						/>
					</dd>
				</div>
				<div class="flex items-center justify-between gap-2">
					<dt class="text-xs text-muted">Valor</dt>
					<dd class="font-mono font-semibold text-danger tabular-nums">
						{formatSignedBRL(detalhe.valor, detalhe.tipo)}
					</dd>
				</div>
				<div class="flex items-center justify-between gap-2">
					<dt class="text-xs text-muted">Categoria</dt>
					<dd class="text-ink">{detalhe.categoria.nome}</dd>
				</div>
				<div class="flex items-center justify-between gap-2">
					<dt class="text-xs text-muted">Vencimento</dt>
					<dd class="font-mono text-xs text-ink">{formatDateBR(detalhe.dataVencimento)}</dd>
				</div>
				<div class="flex items-center justify-between gap-2">
					<dt class="text-xs text-muted">Pagamento</dt>
					<dd class="font-mono text-xs text-ink">
						{detalhe.dataPagamento ? formatDateBR(detalhe.dataPagamento) : '—'}
					</dd>
				</div>
				{#if detalhe.idReferenciaExterna}
					<div class="flex items-center justify-between gap-2">
						<dt class="text-xs text-muted">Referência</dt>
						<dd class="font-mono text-xs text-ink">{detalhe.idReferenciaExterna}</dd>
					</div>
				{/if}
				{#if detalhe.observacao}
					<div>
						<dt class="text-xs text-muted">Conta / fornecedor</dt>
						<dd class="mt-0.5 text-ink">{detalhe.observacao}</dd>
					</div>
				{/if}
			</dl>
		{/if}
	{/snippet}
	{#snippet footer()}
		<button
			type="button"
			onclick={() => (detalhe = null)}
			class="rounded-md border border-border bg-surface px-4 py-2 text-sm font-medium text-ink transition hover:bg-elevated"
		>
			Fechar
		</button>
		{#if detalhe && detalhe.status !== 'Pago' && detalhe.status !== 'Cancelado'}
			<button
				type="button"
				onclick={() => detalhe && pedirPagamento(detalhe)}
				class="rounded-md bg-brand px-4 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi"
			>
				Registrar pagamento
			</button>
		{/if}
	{/snippet}
</Modal>

<Modal
	open={modalPagamento}
	title="Registrar pagamento"
	subtitle={pagamentoAlvo ? `A baixa de ${pagamentoAlvo.codigo} muda o status para Pago` : ''}
	onClose={() => (modalPagamento = false)}
	width="sm"
>
	{#snippet children()}
		<div class="space-y-3">
			<div>
				<label for="cp-pag-data" class="mb-1 block text-xs font-medium text-muted">
					Data <span class="text-danger">*</span>
				</label>
				<input
					id="cp-pag-data"
					type="date"
					bind:value={pagamentoData}
					class="w-full rounded-md border border-border bg-elevated px-3 py-2.5 text-sm text-ink focus:border-brand focus:ring-2 focus:ring-brand/30 focus:outline-none"
				/>
			</div>
			<div>
				<label for="cp-pag-obs" class="mb-1 block text-xs font-medium text-muted">Observação</label>
				<textarea
					id="cp-pag-obs"
					bind:value={pagamentoObs}
					rows={2}
					placeholder="Ex.: pago via Pix"
					class="w-full rounded-md border border-border bg-elevated px-3 py-2.5 text-sm text-ink placeholder:text-muted/60 focus:border-brand focus:ring-2 focus:ring-brand/30 focus:outline-none"
				></textarea>
			</div>
		</div>
	{/snippet}
	{#snippet footer()}
		<button
			type="button"
			onclick={() => (modalPagamento = false)}
			disabled={ocupado}
			class="rounded-md border border-border bg-surface px-4 py-2 text-sm font-medium text-ink transition hover:bg-elevated disabled:opacity-50"
		>
			Cancelar
		</button>
		<button
			type="button"
			onclick={() => void confirmarPagamento()}
			disabled={ocupado || !pagamentoData}
			class="rounded-md bg-brand px-4 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi disabled:opacity-50"
		>
			{ocupado ? 'Registrando…' : 'Registrar'}
		</button>
	{/snippet}
</Modal>
