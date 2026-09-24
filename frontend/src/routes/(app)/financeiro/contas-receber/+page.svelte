<script lang="ts">
	import { goto, invalidateAll } from '$app/navigation';
	import type { PageProps } from './$types';
	import type {
		CategoriaFinanceira,
		Lancamento,
		TipoLancamento
	} from '$lib/types/financeiro';
	import { createLancamento, registrarPagamento } from '$lib/api/financeiro/lancamentos';
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
	import Avatar from '$lib/components/ui/Avatar.svelte';
	import Icon from '$lib/components/ui/Icon.svelte';
	import TableSkeleton from '$lib/components/ui/TableSkeleton.svelte';
	import EmptyState from '$lib/components/ui/EmptyState.svelte';
	import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte';
	import RowActions from '$lib/components/ui/RowActions.svelte';
	import Modal from '$lib/components/ui/Modal.svelte';
	import Select from '$lib/components/ui/Select.svelte';
	import MoneyInput from '$lib/components/ui/MoneyInput.svelte';

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
		params.search !== '' || params.status !== '' || params.origem !== ''
	);

	const STATUS_OPCOES = ['Pendente', 'Atrasado'].map((id) => ({ id, label: id }));
	const ORIGEM_OPCOES = ['Encomendas', 'Marketplace', 'Outros'].map((id) => ({
		id,
		label: id
	}));
	const CATEGORIA_OPCOES = $derived(categorias.map((c) => ({ id: c.id, label: c.nome })));

	// Origem derivada do prefixo da referência servida (sem campo próprio na API — D-4).
	function origemDe(l: Lancamento): string {
		const ref = (l.idReferenciaExterna ?? '').toUpperCase();
		if (ref.startsWith('EN')) return 'Encomendas';
		if (ref.startsWith('MK') || ref.startsWith('MP')) return 'Marketplace';
		return 'Outros';
	}

	function origemTone(origem: string): 'brand' | 'success' | 'warn' | 'danger' | 'muted' {
		if (origem === 'Encomendas') return 'brand';
		if (origem === 'Marketplace') return 'warn';
		return 'muted';
	}

	const visiveis = $derived(
		params.origem === '' ? lancamentos : lancamentos.filter((l) => origemDe(l) === params.origem)
	);

	interface Query {
		[key: string]: string | number | undefined;
	}

	function navegar(overrides: Query): void {
		const base: Query = {
			search: params.search || undefined,
			status: params.status || undefined,
			origem: params.origem || undefined,
			page: params.page
		};
		const merged = { ...base, ...overrides };
		const url = new URLSearchParams();
		for (const [chave, valor] of Object.entries(merged)) {
			if (valor !== undefined && valor !== '') url.append(chave, String(valor));
		}
		const qs = url.toString();
		void goto(`/financeiro/contas-receber${qs ? `?${qs}` : ''}`);
	}

	function onSearch(termo: string): void {
		navegar({ search: termo || undefined, page: 1 });
	}

	function alternarFiltro(chave: 'status' | 'origem', id: string): void {
		const atual = params[chave];
		navegar({ [chave]: atual === id ? undefined : id, page: 1 });
	}

	function limparFiltro(chave: 'status' | 'origem'): void {
		navegar({ [chave]: undefined, page: 1 });
	}

	function limparTodos(): void {
		navegar({ search: undefined, status: undefined, origem: undefined, page: 1 });
	}

	function onPage(page: number): void {
		navegar({ page });
	}

	function tentarNovamente(): void {
		void goto(`/financeiro/contas-receber${window.location.search}`, { invalidateAll: true });
	}

	function clienteDe(l: Lancamento): string {
		return l.observacao?.trim() ? (l.observacao as string) : l.categoria.nome;
	}

	function referenciaDe(l: Lancamento): string {
		return l.idReferenciaExterna ?? '—';
	}

	// ---- Modal: novo recebimento (lançamento pré-selecionado Entrada) ----

	let modalNovo = $state(false);
	let novoCategoria = $state('');
	let novoTipo = $state<TipoLancamento>('Entrada');
	let novoValor = $state<number | null>(null);
	let novoVencimento = $state('');
	let novoPagamento = $state('');
	let novoReferencia = $state('');
	let novoObservacao = $state('');
	let ocupado = $state(false);

	function abrirNovo(): void {
		novoCategoria = '';
		novoTipo = 'Entrada';
		novoValor = null;
		novoVencimento = '';
		novoPagamento = '';
		novoReferencia = '';
		novoObservacao = '';
		modalNovo = true;
	}

	async function confirmarNovo(): Promise<void> {
		if (!novoCategoria) {
			toasts.warn('Selecione a categoria do recebimento.');
			return;
		}
		if (novoValor === null || novoValor <= 0) {
			toasts.warn('Informe um valor maior que zero.');
			return;
		}
		if (!novoVencimento) {
			toasts.warn('Informe a data de vencimento.');
			return;
		}
		ocupado = true;
		try {
			await createLancamento({
				idCategoria: novoCategoria,
				tipo: novoTipo,
				valor: novoValor,
				dataVencimento: novoVencimento,
				dataPagamento: novoPagamento || null,
				idReferenciaExterna: novoReferencia.trim() || undefined,
				observacao: novoObservacao.trim() || undefined
			});
			toasts.success('Recebimento criado com sucesso.');
			modalNovo = false;
			await invalidateAll();
		} catch (err) {
			toasts.danger(toUserMessage(err).message);
		} finally {
			ocupado = false;
		}
	}

	// ---- Modal: detalhe + recebimento ----

	let detalhe = $state<Lancamento | null>(null);
	let modalRecebimento = $state(false);
	let recebimentoAlvo = $state<Lancamento | null>(null);
	let recebimentoData = $state('');
	let recebimentoObs = $state('');

	function hojeISO(): string {
		return new Date().toISOString().slice(0, 10);
	}

	function abrirDetalhe(l: Lancamento): void {
		detalhe = l;
	}

	function pedirRecebimento(l: Lancamento): void {
		recebimentoAlvo = l;
		recebimentoData = hojeISO();
		recebimentoObs = '';
		detalhe = null;
		modalRecebimento = true;
	}

	async function confirmarRecebimento(): Promise<void> {
		if (!recebimentoAlvo) return;
		if (!recebimentoData) {
			toasts.warn('Informe a data do recebimento.');
			return;
		}
		ocupado = true;
		try {
			await registrarPagamento(recebimentoAlvo.id, {
				dataPagamento: recebimentoData,
				observacao: recebimentoObs.trim() || undefined
			});
			toasts.success('Recebimento registrado com sucesso.');
			modalRecebimento = false;
			recebimentoAlvo = null;
			await invalidateAll();
		} catch (err) {
			toasts.danger(toUserMessage(err).message);
		} finally {
			ocupado = false;
		}
	}

	function acaoLinha(l: Lancamento, acao: string): void {
		if (acao === 'detalhe') abrirDetalhe(l);
		else if (acao === 'recebimento') pedirRecebimento(l);
	}

	const detalheStatus = $derived(detalhe ? lancamentoStatusMeta(detalhe.status) : null);
	const detalheTipo = $derived(detalhe ? tipoMeta(detalhe.tipo) : null);
	const vencidos = $derived(counts?.Atrasado ?? 0);
</script>

<svelte:head>
	<title>Contas a receber — Financeiro — FabLab</title>
</svelte:head>

<div class="space-y-4">
	<PageHeader
		title="Contas a receber"
		subtitle="Entradas pendentes e atrasadas derivadas dos lançamentos."
	>
		{#snippet children()}
			<button
				type="button"
				data-testid="cr-nova"
				onclick={abrirNovo}
				aria-label="Novo recebimento"
				class="inline-flex items-center gap-1.5 rounded-md bg-brand px-3.5 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi"
			>
				<Icon name="plus" class="h-4 w-4" /> Novo recebimento
			</button>
		{/snippet}
	</PageHeader>

	{#if comErro}
		<ErrorBanner
			message="Não foi possível carregar as contas a receber"
			hint="Verifique sua conexão e tente novamente. Se persistir, contate o suporte."
			onRetry={tentarNovamente}
			testid="cr-retry"
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
					<p class="text-xs text-muted">A receber no mês</p>
					<p class="mt-1 font-mono text-lg font-semibold text-success tabular-nums">
						{formatMoneyBRL(resumo.pendente)}
					</p>
					<p class="mt-0.5 text-xs text-muted">{counts.Pendente} pendentes</p>
				</div>
				<div class="rounded-xl border border-border bg-elevated p-4">
					<p class="text-xs text-muted">Inadimplência</p>
					<p
						class="mt-1 font-mono text-lg font-semibold tabular-nums {vencidos > 0
							? 'text-danger'
							: 'text-success'}"
					>
						{vencidos}
					</p>
					<p class="mt-0.5 text-xs {vencidos > 0 ? 'text-danger' : 'text-success'}">
						{vencidos === 0 ? 'Nenhum recebível vencido' : `${vencidos} recebíveis vencidos`}
					</p>
				</div>
				<div class="rounded-xl border border-border bg-elevated p-4">
					<p class="text-xs text-muted">Recebido no mês</p>
					<p class="mt-1 font-mono text-lg font-semibold text-success tabular-nums">
						{formatMoneyBRL(resumo.entradas)}
					</p>
					<p class="mt-0.5 text-xs text-muted">{counts.Pago} recebidos</p>
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
				{categoriasError}. O modal de novo recebimento pode estar sem categorias.
			</p>
		{/if}

		<div class="flex flex-wrap items-center gap-2">
			<div class="min-w-56 flex-1">
				<SearchInput
					value={params.search}
					onSearch={onSearch}
					placeholder="Buscar por cliente, origem ou referência…"
					delay={300}
					label="Buscar contas a receber"
				/>
			</div>
			<div data-testid="fpanel-cr-status">
				<Dropdown
					label="Status"
					options={STATUS_OPCOES}
					selected={params.status ? [params.status] : []}
					onToggle={(id) => alternarFiltro('status', id)}
					onClear={() => limparFiltro('status')}
					search={false}
				/>
			</div>
			<div data-testid="fpanel-cr-origem">
				<Dropdown
					label="Origem"
					options={ORIGEM_OPCOES}
					selected={params.origem ? [params.origem] : []}
					onToggle={(id) => alternarFiltro('origem', id)}
					onClear={() => limparFiltro('origem')}
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
				{#if params.origem}
					<Chip label={params.origem} onRemove={() => limparFiltro('origem')} />
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
						title="Nenhum recebível encontrado"
						description="Nenhuma conta a receber combina com os filtros aplicados."
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
						title="Nenhuma conta a receber"
						description="Não há entradas pendentes ou atrasadas no momento."
					>
						{#snippet children()}
							<button
								type="button"
								onclick={abrirNovo}
								class="inline-flex items-center gap-1.5 rounded-md bg-brand px-3.5 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi"
							>
								<Icon name="plus" class="h-4 w-4" /> Novo recebimento
							</button>
						{/snippet}
					</EmptyState>
				{/if}
			</div>
		{:else}
			<div class="hidden overflow-hidden rounded-xl border border-border bg-surface md:block">
				<div class="overflow-x-auto">
					<table class="w-full min-w-[880px] text-sm">
						<thead>
							<tr
								class="border-b border-border bg-elevated/50 text-left text-[11px] uppercase tracking-wide text-muted"
							>
								<th class="px-4 py-2.5 font-medium">Cliente / origem</th>
								<th class="px-4 py-2.5 font-medium">Referência</th>
								<th class="px-4 py-2.5 font-medium">Origem</th>
								<th class="px-4 py-2.5 font-medium">Vencimento</th>
								<th class="px-4 py-2.5 text-right font-medium">Valor</th>
								<th class="px-4 py-2.5 font-medium">Status</th>
								<th class="w-36 px-4 py-2.5 text-right font-medium">
									<span class="sr-only">Ações</span>
								</th>
							</tr>
						</thead>
						<tbody>
							{#each visiveis as l (l.id)}
								{@const st = lancamentoStatusMeta(l.status)}
								{@const atrasado = l.status === 'Atrasado'}
								{@const origem = origemDe(l)}
								<tr
									data-testid="cr-row"
									onclick={() => abrirDetalhe(l)}
									class="cursor-pointer border-b border-border transition last:border-0 hover:bg-elevated/40 {atrasado
										? 'bg-danger/[0.03]'
										: ''}"
								>
									<td class="px-4 py-3">
										<div class="flex items-center gap-2.5">
											<Avatar name={clienteDe(l)} tone={atrasado ? 'danger' : 'brand'} />
											<div class="min-w-0">
												<p class="truncate font-medium text-ink">{clienteDe(l)}</p>
												{#if atrasado}
													<p class="text-xs text-danger">
														Atrasado · venceu em {formatDateBR(l.dataVencimento)}
													</p>
												{:else}
													<p class="text-xs text-muted">
														Vence em {formatDateBR(l.dataVencimento)}
													</p>
												{/if}
											</div>
										</div>
									</td>
									<td class="px-4 py-3 font-mono text-xs text-muted">{referenciaDe(l)}</td>
									<td class="px-4 py-3">
										<TypeBadge tone={origemTone(origem)} label={origem} />
									</td>
									<td
										class="px-4 py-3 font-mono text-xs tabular-nums {atrasado
											? 'text-danger'
											: 'text-muted'}"
									>
										{formatDateBR(l.dataVencimento)}
									</td>
									<td class="px-4 py-3 text-right font-mono text-sm text-success tabular-nums">
										{formatSignedBRL(l.valor, l.tipo)}
									</td>
									<td class="px-4 py-3">
										<StatusBadge label={st.label} color={st.color} />
									</td>
									<td class="px-4 py-3" onclick={(e) => e.stopPropagation()}>
										<div class="flex items-center justify-end gap-1.5">
											<button
												type="button"
												data-testid="cr-receber"
												onclick={() => pedirRecebimento(l)}
												aria-label={`Registrar recebimento de ${l.codigo}`}
												class="rounded-md border border-success/30 bg-success/10 px-2.5 py-1.5 text-xs font-medium text-success transition hover:bg-success/20"
											>
												Receber
											</button>
											<RowActions
												label={`Ações de ${l.codigo}`}
												actions={[
													{ id: 'detalhe', label: 'Ver detalhe', icon: 'eye' },
													{ id: 'recebimento', label: 'Registrar recebimento', icon: 'check' }
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
							{paginacao.totalItems} recebíveis · pendente + atrasado = {formatMoneyBRL(
								resumo.pendente
							)}
						</p>
						<Pagination
							page={paginacao.page}
							totalPages={paginacao.totalPages}
							totalItems={paginacao.totalItems}
							onPage={onPage}
							label="recebíveis"
						/>
					</div>
				{/if}
			</div>

			<div class="space-y-2 md:hidden">
				{#each visiveis as l (l.id)}
					{@const st = lancamentoStatusMeta(l.status)}
					{@const origem = origemDe(l)}
					<article
						data-testid="cr-row"
						class="rounded-xl border border-border bg-surface p-4 {l.status === 'Atrasado'
							? 'bg-danger/[0.03]'
							: ''}"
					>
						<div class="flex items-start gap-2.5">
							<Avatar name={clienteDe(l)} tone={l.status === 'Atrasado' ? 'danger' : 'brand'} />
							<button
								type="button"
								onclick={() => abrirDetalhe(l)}
								aria-label={`Ver ${l.codigo}`}
								class="min-w-0 flex-1 text-left"
							>
								<span class="block truncate text-sm font-medium text-ink">{clienteDe(l)}</span>
								<span class="block font-mono text-xs text-muted">{referenciaDe(l)}</span>
								{#if l.status === 'Atrasado'}
									<span class="mt-0.5 block text-xs text-danger">
										Atrasado · venceu em {formatDateBR(l.dataVencimento)}
									</span>
								{:else}
									<span class="mt-0.5 block text-xs text-muted">
										Vence em {formatDateBR(l.dataVencimento)}
									</span>
								{/if}
								<span class="mt-2 flex flex-wrap items-center gap-1.5">
									<TypeBadge tone={origemTone(origem)} label={origem} />
									<StatusBadge label={st.label} color={st.color} />
								</span>
								<span class="mt-1.5 block font-mono text-sm text-success tabular-nums">
									{formatSignedBRL(l.valor, l.tipo)}
								</span>
							</button>
						</div>
						<div class="mt-3 flex items-center justify-end gap-1.5">
							<button
								type="button"
								data-testid="cr-receber"
								onclick={() => pedirRecebimento(l)}
								aria-label={`Registrar recebimento de ${l.codigo}`}
								class="rounded-md border border-success/30 bg-success/10 px-2.5 py-1.5 text-xs font-medium text-success transition hover:bg-success/20"
							>
								Receber
							</button>
							<RowActions
								label={`Ações de ${l.codigo}`}
								actions={[
									{ id: 'detalhe', label: 'Ver detalhe', icon: 'eye' },
									{ id: 'recebimento', label: 'Registrar recebimento', icon: 'check' }
								]}
								onSelect={(id) => acaoLinha(l, id)}
							/>
						</div>
					</article>
				{/each}
				{#if paginacao && resumo}
					<div class="space-y-2 rounded-xl border border-border bg-surface px-4 py-3">
						<p class="text-xs text-muted">
							{paginacao.totalItems} recebíveis · pendente + atrasado = {formatMoneyBRL(
								resumo.pendente
							)}
						</p>
						<Pagination
							page={paginacao.page}
							totalPages={paginacao.totalPages}
							totalItems={paginacao.totalItems}
							onPage={onPage}
							label="recebíveis"
						/>
					</div>
				{/if}
			</div>
		{/if}
	{/if}
</div>

<Modal
	open={modalNovo}
	title="Novo recebimento"
	subtitle="Registre uma entrada com vencimento"
	onClose={() => (modalNovo = false)}
	width="sm"
>
	{#snippet children()}
		<div class="space-y-3">
			<Select
				id="cr-nova-categoria"
				label="Categoria"
				options={CATEGORIA_OPCOES}
				value={novoCategoria}
				onChange={(v) => (novoCategoria = v)}
				required
				placeholder={categorias.length === 0 ? 'Nenhuma categoria cadastrada' : 'Selecione…'}
				hint={categorias.length === 0 ? 'Crie uma categoria em Lançamentos antes de lançar.' : ''}
			/>
			<fieldset>
				<legend class="mb-1 block text-xs font-medium text-muted">Tipo</legend>
				<div class="flex gap-2">
					{#each [{ id: 'Entrada', label: 'Entrada' }, { id: 'Saída', label: 'Saída' }] as opt (opt.id)}
						<label
							class="flex flex-1 cursor-pointer items-center gap-2 rounded-md border px-3 py-2.5 text-sm transition {novoTipo === opt.id
								? 'border-brand/60 bg-brand/10 text-ink'
								: 'border-border bg-elevated text-muted'}"
						>
							<input
								type="radio"
								name="cr-novo-tipo"
								value={opt.id}
								checked={novoTipo === opt.id}
								onchange={() => (novoTipo = opt.id as TipoLancamento)}
								class="h-4 w-4 accent-brand"
							/>
							{opt.label}
						</label>
					{/each}
				</div>
			</fieldset>
			<MoneyInput bind:value={novoValor} label="Valor" />
			<div class="grid grid-cols-2 gap-3">
				<div>
					<label for="cr-novo-vencimento" class="mb-1 block text-xs font-medium text-muted">
						Vencimento <span class="text-danger">*</span>
					</label>
					<input
						id="cr-novo-vencimento"
						type="date"
						bind:value={novoVencimento}
						class="w-full rounded-md border border-border bg-elevated px-3 py-2.5 text-sm text-ink focus:border-brand focus:ring-2 focus:ring-brand/30 focus:outline-none"
					/>
				</div>
				<div>
					<label for="cr-novo-pagamento" class="mb-1 block text-xs font-medium text-muted">
						Recebimento
					</label>
					<input
						id="cr-novo-pagamento"
						type="date"
						bind:value={novoPagamento}
						class="w-full rounded-md border border-border bg-elevated px-3 py-2.5 text-sm text-ink focus:border-brand focus:ring-2 focus:ring-brand/30 focus:outline-none"
					/>
				</div>
			</div>
			<div>
				<label for="cr-novo-referencia" class="mb-1 block text-xs font-medium text-muted">
					Referência
				</label>
				<input
					id="cr-novo-referencia"
					type="text"
					bind:value={novoReferencia}
					placeholder="Ex.: EN-2051"
					class="w-full rounded-md border border-border bg-elevated px-3 py-2.5 font-mono text-sm text-ink placeholder:text-muted/60 focus:border-brand focus:ring-2 focus:ring-brand/30 focus:outline-none"
				/>
			</div>
			<div>
				<label for="cr-novo-observacao" class="mb-1 block text-xs font-medium text-muted">
					Cliente / origem
				</label>
				<textarea
					id="cr-novo-observacao"
					bind:value={novoObservacao}
					rows={2}
					placeholder="Ex.: Escola parceira — oficina"
					class="w-full rounded-md border border-border bg-elevated px-3 py-2.5 text-sm text-ink placeholder:text-muted/60 focus:border-brand focus:ring-2 focus:ring-brand/30 focus:outline-none"
				></textarea>
			</div>
		</div>
	{/snippet}
	{#snippet footer()}
		<button
			type="button"
			onclick={() => (modalNovo = false)}
			disabled={ocupado}
			class="rounded-md border border-border bg-surface px-4 py-2 text-sm font-medium text-ink transition hover:bg-elevated disabled:opacity-50"
		>
			Cancelar
		</button>
		<button
			type="button"
			onclick={() => void confirmarNovo()}
			disabled={ocupado}
			class="rounded-md bg-brand px-4 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi disabled:opacity-50"
		>
			{ocupado ? 'Salvando…' : 'Criar recebimento'}
		</button>
	{/snippet}
</Modal>

<Modal
	open={detalhe !== null}
	title={detalhe ? detalhe.codigo : 'Recebível'}
	subtitle="Ficha da conta a receber"
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
					<dd class="font-mono font-semibold text-success tabular-nums">
						{formatSignedBRL(detalhe.valor, detalhe.tipo)}
					</dd>
				</div>
				<div class="flex items-center justify-between gap-2">
					<dt class="text-xs text-muted">Origem</dt>
					<dd>
						<TypeBadge tone={origemTone(origemDe(detalhe))} label={origemDe(detalhe)} />
					</dd>
				</div>
				<div class="flex items-center justify-between gap-2">
					<dt class="text-xs text-muted">Vencimento</dt>
					<dd class="font-mono text-xs text-ink">{formatDateBR(detalhe.dataVencimento)}</dd>
				</div>
				<div class="flex items-center justify-between gap-2">
					<dt class="text-xs text-muted">Recebimento</dt>
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
						<dt class="text-xs text-muted">Cliente / origem</dt>
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
				onclick={() => detalhe && pedirRecebimento(detalhe)}
				class="rounded-md bg-brand px-4 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi"
			>
				Registrar recebimento
			</button>
		{/if}
	{/snippet}
</Modal>

<Modal
	open={modalRecebimento}
	title="Registrar recebimento"
	subtitle={recebimentoAlvo
		? `A baixa de ${recebimentoAlvo.codigo} muda o status para Pago`
		: ''}
	onClose={() => (modalRecebimento = false)}
	width="sm"
>
	{#snippet children()}
		<div class="space-y-3">
			<div>
				<label for="cr-rec-data" class="mb-1 block text-xs font-medium text-muted">
					Data <span class="text-danger">*</span>
				</label>
				<input
					id="cr-rec-data"
					type="date"
					bind:value={recebimentoData}
					class="w-full rounded-md border border-border bg-elevated px-3 py-2.5 text-sm text-ink focus:border-brand focus:ring-2 focus:ring-brand/30 focus:outline-none"
				/>
			</div>
			<div>
				<label for="cr-rec-obs" class="mb-1 block text-xs font-medium text-muted">Observação</label>
				<textarea
					id="cr-rec-obs"
					bind:value={recebimentoObs}
					rows={2}
					placeholder="Ex.: recebido via Pix"
					class="w-full rounded-md border border-border bg-elevated px-3 py-2.5 text-sm text-ink placeholder:text-muted/60 focus:border-brand focus:ring-2 focus:ring-brand/30 focus:outline-none"
				></textarea>
			</div>
		</div>
	{/snippet}
	{#snippet footer()}
		<button
			type="button"
			onclick={() => (modalRecebimento = false)}
			disabled={ocupado}
			class="rounded-md border border-border bg-surface px-4 py-2 text-sm font-medium text-ink transition hover:bg-elevated disabled:opacity-50"
		>
			Cancelar
		</button>
		<button
			type="button"
			onclick={() => void confirmarRecebimento()}
			disabled={ocupado || !recebimentoData}
			class="rounded-md bg-brand px-4 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi disabled:opacity-50"
		>
			{ocupado ? 'Registrando…' : 'Registrar'}
		</button>
	{/snippet}
</Modal>
