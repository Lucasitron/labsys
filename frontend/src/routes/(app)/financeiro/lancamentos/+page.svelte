<script lang="ts">
	import { goto, invalidateAll } from '$app/navigation';
	import type { PageProps } from './$types';
	import type {
		CategoriaFinanceira,
		CategoriaTipo,
		Lancamento,
		TipoLancamento
	} from '$lib/types/financeiro';
	import { createLancamento, registrarPagamento } from '$lib/api/financeiro/lancamentos';
	import { createCategoria } from '$lib/api/financeiro/categorias';
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
	import BulkActionsBar from '$lib/components/ui/BulkActionsBar.svelte';
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
		params.search !== '' ||
			params.status !== '' ||
			params.tipo !== '' ||
			params.categoria !== '' ||
			params.periodo !== '' ||
			params.ordenar !== ''
	);

	const STATUS_OPCOES = ['Pendente', 'Pago', 'Atrasado', 'Cancelado'].map((id) => ({ id, label: id }));
	const TIPO_OPCOES = ['Entrada', 'Saída'].map((id) => ({ id, label: id }));
	const CATEGORIA_OPCOES = $derived(categorias.map((c) => ({ id: c.id, label: c.nome })));
	const PERIODO_OPCOES = [
		{ id: '7d', label: 'Últimos 7 dias' },
		{ id: '30d', label: 'Últimos 30 dias' },
		{ id: 'mes', label: 'Este mês' },
		{ id: 'ano', label: 'Este ano' }
	];
	const ORDENAR_OPCOES = [
		{ id: 'recentes', label: 'Mais recentes' },
		{ id: 'vencimento', label: 'Vencimento' },
		{ id: 'valor-maior', label: 'Maior valor' },
		{ id: 'valor-menor', label: 'Menor valor' }
	];

	function categoriaLabel(id: string): string {
		return categorias.find((c) => c.id === id)?.nome ?? id;
	}

	interface Query {
		[key: string]: string | number | undefined;
	}

	function navegar(overrides: Query): void {
		const base: Query = {
			search: params.search || undefined,
			status: params.status || undefined,
			tipo: params.tipo || undefined,
			categoria: params.categoria || undefined,
			periodo: params.periodo || undefined,
			ordenar: params.ordenar || undefined,
			page: params.page,
			pageSize: params.pageSize === 10 ? undefined : params.pageSize
		};
		const merged = { ...base, ...overrides };
		const url = new URLSearchParams();
		for (const [chave, valor] of Object.entries(merged)) {
			if (valor !== undefined && valor !== '') url.append(chave, String(valor));
		}
		const qs = url.toString();
		void goto(`/financeiro/lancamentos${qs ? `?${qs}` : ''}`);
	}

	function onSearch(termo: string): void {
		navegar({ search: termo || undefined, page: 1 });
	}

	function alternarFiltro(
		chave: 'status' | 'tipo' | 'categoria' | 'periodo' | 'ordenar',
		id: string
	): void {
		const atual = params[chave];
		navegar({ [chave]: atual === id ? undefined : id, page: 1 });
	}

	function limparFiltro(chave: 'status' | 'tipo' | 'categoria' | 'periodo' | 'ordenar'): void {
		navegar({ [chave]: undefined, page: 1 });
	}

	function limparTodos(): void {
		navegar({
			search: undefined,
			status: undefined,
			tipo: undefined,
			categoria: undefined,
			periodo: undefined,
			ordenar: undefined,
			page: 1
		});
	}

	function onPage(page: number): void {
		navegar({ page });
	}

	function onPageSize(size: number): void {
		navegar({ pageSize: size === 10 ? undefined : size, page: 1 });
	}

	function tentarNovamente(): void {
		void goto(`/financeiro/lancamentos${window.location.search}`, { invalidateAll: true });
	}

	function descricao(l: Lancamento): string {
		return l.observacao?.trim() ? (l.observacao as string) : l.categoria.nome;
	}

	function referencia(l: Lancamento): string {
		return l.idReferenciaExterna ? `${l.codigo} • REF ${l.idReferenciaExterna}` : l.codigo;
	}

	// ---- Seleção + bulk ----

	let selecionados = $state<string[]>([]);
	const algumSelecionado = $derived(selecionados.length > 0);
	const todosIds = $derived(lancamentos.map((l) => l.id));
	const todosSelecionados = $derived(
		lancamentos.length > 0 && selecionados.length === lancamentos.length
	);
	const parcial = $derived(selecionados.length > 0 && !todosSelecionados);

	function alternarTodos(marcar: boolean): void {
		selecionados = marcar ? [...todosIds] : [];
	}

	function alternarUm(id: string, marcado: boolean): void {
		selecionados = marcado ? [...selecionados, id] : selecionados.filter((s) => s !== id);
	}

	function limparSelecao(): void {
		selecionados = [];
	}

	$effect(() => {
		if (resultado && resultado.lancamentos.length === 0 && params.page > 1) {
			navegar({ page: params.page - 1 });
		}
	});

	$effect(() => {
		void params.page;
		void params.search;
		void params.status;
		void params.tipo;
		void params.categoria;
		void params.periodo;
		void params.ordenar;
		selecionados = [];
	});

	// ---- Modal: nova categoria ----

	let modalCategoria = $state(false);
	let catNome = $state('');
	let catTipo = $state<CategoriaTipo>('Receita');
	let catDescricao = $state('');
	let ocupado = $state(false);

	function abrirCategoria(): void {
		catNome = '';
		catTipo = 'Receita';
		catDescricao = '';
		modalCategoria = true;
	}

	async function confirmarCategoria(): Promise<void> {
		if (!catNome.trim()) {
			toasts.warn('Informe o nome da categoria.');
			return;
		}
		ocupado = true;
		try {
			await createCategoria({
				nome: catNome.trim(),
				tipo: catTipo,
				descricao: catDescricao.trim() || undefined
			});
			toasts.success('Categoria criada com sucesso.');
			modalCategoria = false;
			await invalidateAll();
		} catch (err) {
			toasts.danger(toUserMessage(err).message);
		} finally {
			ocupado = false;
		}
	}

	// ---- Modal: novo lançamento ----

	let modalNovo = $state(false);
	let novoCategoria = $state('');
	let novoTipo = $state<TipoLancamento>('Entrada');
	let novoValor = $state<number | null>(null);
	let novoVencimento = $state('');
	let novoPagamento = $state('');
	let novoReferencia = $state('');
	let novoObservacao = $state('');

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
			toasts.warn('Selecione a categoria do lançamento.');
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
			toasts.success('Lançamento criado com sucesso.');
			modalNovo = false;
			await invalidateAll();
		} catch (err) {
			toasts.danger(toUserMessage(err).message);
		} finally {
			ocupado = false;
		}
	}

	// ---- Modal: detalhe + pagamento ----

	let detalhe = $state<Lancamento | null>(null);
	let modalPagamento = $state(false);
	let pagamentoAlvos = $state<string[]>([]);
	let pagamentoData = $state('');
	let pagamentoObs = $state('');

	function hojeISO(): string {
		return new Date().toISOString().slice(0, 10);
	}

	function abrirDetalhe(l: Lancamento): void {
		detalhe = l;
	}

	function pedirPagamento(ids: string[]): void {
		pagamentoAlvos = ids;
		pagamentoData = hojeISO();
		pagamentoObs = '';
		detalhe = null;
		modalPagamento = true;
	}

	async function confirmarPagamento(): Promise<void> {
		if (pagamentoAlvos.length === 0) return;
		if (!pagamentoData) {
			toasts.warn('Informe a data do recebimento/pagamento.');
			return;
		}
		ocupado = true;
		try {
			await Promise.all(
				pagamentoAlvos.map((id) =>
					registrarPagamento(id, {
						dataPagamento: pagamentoData,
						observacao: pagamentoObs.trim() || undefined
					})
				)
			);
			toasts.success(
				pagamentoAlvos.length === 1
					? 'Pagamento registrado com sucesso.'
					: `${pagamentoAlvos.length} pagamentos registrados com sucesso.`
			);
			modalPagamento = false;
			pagamentoAlvos = [];
			selecionados = [];
			await invalidateAll();
		} catch (err) {
			toasts.danger(toUserMessage(err).message);
		} finally {
			ocupado = false;
		}
	}

	function acaoLinha(l: Lancamento, acao: string): void {
		if (acao === 'detalhe') abrirDetalhe(l);
		else if (acao === 'pagamento') pedirPagamento([l.id]);
	}

	const detalheStatus = $derived(detalhe ? lancamentoStatusMeta(detalhe.status) : null);
	const detalheTipo = $derived(detalhe ? tipoMeta(detalhe.tipo) : null);
</script>

<svelte:head>
	<title>Lançamentos — Financeiro — FabLab</title>
</svelte:head>

<div class="space-y-4">
	<PageHeader title="Lançamentos" subtitle="Entradas e saídas do laboratório com vencimento e baixa.">
		{#snippet children()}
			<button
				type="button"
				onclick={abrirCategoria}
				class="rounded-md border border-border bg-elevated px-3.5 py-2 text-sm font-medium text-ink transition hover:border-brand/50 hover:text-brandhi"
			>
				Nova categoria
			</button>
			<button
				type="button"
				data-testid="lan-novo"
				onclick={abrirNovo}
				aria-label="Novo lançamento"
				class="inline-flex items-center gap-1.5 rounded-md bg-brand px-3.5 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi"
			>
				<Icon name="plus" class="h-4 w-4" /> Novo lançamento
			</button>
		{/snippet}
	</PageHeader>

	{#if comErro}
		<ErrorBanner
			message="Não foi possível carregar os lançamentos"
			hint="Verifique sua conexão e tente novamente. Se persistir, contate o suporte."
			onRetry={tentarNovamente}
			testid="lan-retry"
		/>
	{:else if carregando}
		<div class="grid gap-3 sm:grid-cols-2 lg:grid-cols-4" aria-hidden="true">
			{#each Array(4) as _, i (i)}
				<div class="h-20 animate-pulse rounded-xl bg-elevated"></div>
			{/each}
		</div>
		<div class="flex flex-wrap items-center gap-2" aria-hidden="true">
			<div class="h-10 w-full min-w-56 max-w-xs animate-pulse rounded-lg bg-elevated sm:flex-1"></div>
			<div class="h-10 w-32 animate-pulse rounded-lg bg-elevated"></div>
			<div class="h-10 w-32 animate-pulse rounded-lg bg-elevated"></div>
		</div>
		<div class="rounded-xl border border-border bg-surface p-4">
			<TableSkeleton rows={6} columns={6} />
		</div>
	{:else}
		{#if resumo}
			<div class="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
				<div class="rounded-xl border border-border bg-elevated p-4">
					<p class="text-xs text-muted">Entradas</p>
					<p class="mt-1 font-mono text-lg font-semibold text-success tabular-nums">
						{formatMoneyBRL(resumo.entradas)}
					</p>
				</div>
				<div class="rounded-xl border border-border bg-elevated p-4">
					<p class="text-xs text-muted">Saídas</p>
					<p class="mt-1 font-mono text-lg font-semibold text-danger tabular-nums">
						{formatMoneyBRL(resumo.saidas)}
					</p>
				</div>
				<div class="rounded-xl border border-border bg-elevated p-4">
					<p class="text-xs text-muted">Pendente</p>
					<p class="mt-1 font-mono text-lg font-semibold text-warn tabular-nums">
						{formatMoneyBRL(resumo.pendente)}
					</p>
					{#if counts}
						<p class="mt-0.5 text-xs text-danger">{counts.Atrasado} atrasados</p>
					{/if}
				</div>
				<div class="rounded-xl border border-border bg-elevated p-4">
					<p class="text-xs text-muted">Lançamentos</p>
					<p class="mt-1 font-mono text-lg font-semibold text-ink tabular-nums">
						{paginacao?.totalItems ?? 0}
					</p>
					{#if counts}
						<p class="mt-0.5 text-xs text-muted">{counts.Pendente} pendentes · {counts.Pago} pagos</p>
					{/if}
				</div>
			</div>
		{/if}

		{#if categoriasError}
			<p role="alert" class="rounded-xl border border-warn/30 bg-warn/10 px-4 py-2.5 text-xs text-warn">
				{categoriasError}. O filtro de categoria pode estar incompleto.
			</p>
		{/if}

		<div class="flex flex-wrap items-center gap-2">
			<div class="min-w-56 flex-1">
				<SearchInput
					value={params.search}
					onSearch={onSearch}
					placeholder="Buscar por descrição, categoria ou referência…"
					delay={300}
					label="Buscar lançamentos"
				/>
			</div>
			<Dropdown
				label="Status"
				options={STATUS_OPCOES}
				selected={params.status ? [params.status] : []}
				onToggle={(id) => alternarFiltro('status', id)}
				onClear={() => limparFiltro('status')}
				search={false}
			/>
			<Dropdown
				label="Tipo"
				options={TIPO_OPCOES}
				selected={params.tipo ? [params.tipo] : []}
				onToggle={(id) => alternarFiltro('tipo', id)}
				onClear={() => limparFiltro('tipo')}
				search={false}
			/>
			<Dropdown
				label="Categoria"
				options={CATEGORIA_OPCOES}
				selected={params.categoria ? [params.categoria] : []}
				onToggle={(id) => alternarFiltro('categoria', id)}
				onClear={() => limparFiltro('categoria')}
			/>
			<Dropdown
				label="Período"
				options={PERIODO_OPCOES}
				selected={params.periodo ? [params.periodo] : []}
				onToggle={(id) => alternarFiltro('periodo', id)}
				onClear={() => limparFiltro('periodo')}
				search={false}
			/>
			<Dropdown
				label="Ordenar"
				options={ORDENAR_OPCOES}
				selected={params.ordenar ? [params.ordenar] : []}
				onToggle={(id) => alternarFiltro('ordenar', id)}
				onClear={() => limparFiltro('ordenar')}
				search={false}
			/>
		</div>

		{#if temFiltros}
			<div data-testid="fin-chips" class="flex flex-wrap items-center gap-1.5">
				{#if params.search}
					<Chip label={`Busca: ${params.search}`} onRemove={() => onSearch('')} />
				{/if}
				{#if params.status}
					<Chip label={params.status} onRemove={() => limparFiltro('status')} />
				{/if}
				{#if params.tipo}
					<Chip label={params.tipo} onRemove={() => limparFiltro('tipo')} />
				{/if}
				{#if params.categoria}
					<Chip
						label={categoriaLabel(params.categoria)}
						onRemove={() => limparFiltro('categoria')}
					/>
				{/if}
				{#if params.periodo}
					<Chip
						label={PERIODO_OPCOES.find((o) => o.id === params.periodo)?.label ?? params.periodo}
						onRemove={() => limparFiltro('periodo')}
					/>
				{/if}
				{#if params.ordenar}
					<Chip
						label={ORDENAR_OPCOES.find((o) => o.id === params.ordenar)?.label ?? params.ordenar}
						onRemove={() => limparFiltro('ordenar')}
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

		{#if lancamentos.length === 0}
			<div class="rounded-xl border border-border bg-surface">
				{#if temFiltros}
					<EmptyState
						icon="search"
						title="Nenhum lançamento encontrado"
						description="Nenhum lançamento combina com os filtros aplicados."
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
						title="Nenhum lançamento registrado"
						description="Registre o primeiro lançamento para começar o fluxo de caixa do laboratório."
					>
						{#snippet children()}
							<button
								type="button"
								onclick={abrirNovo}
								class="inline-flex items-center gap-1.5 rounded-md bg-brand px-3.5 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi"
							>
								<Icon name="plus" class="h-4 w-4" /> Novo lançamento
							</button>
						{/snippet}
					</EmptyState>
				{/if}
			</div>
		{:else}
			<div class="hidden overflow-hidden rounded-xl border border-border bg-surface md:block">
				<div class="overflow-x-auto">
					<table class="w-full min-w-[860px] text-sm">
						<thead>
							<tr
								class="border-b border-border bg-elevated/50 text-left text-[11px] uppercase tracking-wide text-muted"
							>
								<th class="w-10 px-4 py-2.5 font-medium">
									<input
										type="checkbox"
										data-testid="select-all-rows"
										checked={todosSelecionados}
										indeterminate={parcial}
										onchange={(e) => alternarTodos((e.currentTarget as HTMLInputElement).checked)}
										aria-label="Selecionar todos os lançamentos da página"
										class="h-4 w-4 rounded border-border bg-surface accent-brand"
									/>
								</th>
								<th class="px-4 py-2.5 font-medium">Vencimento</th>
								<th class="px-4 py-2.5 font-medium">Descrição</th>
								<th class="px-4 py-2.5 font-medium">Categoria</th>
								<th class="px-4 py-2.5 font-medium">Tipo</th>
								<th class="px-4 py-2.5 text-right font-medium">Valor</th>
								<th class="px-4 py-2.5 font-medium">Status</th>
								<th class="w-12 px-4 py-2.5 text-right font-medium">
									<span class="sr-only">Ações</span>
								</th>
							</tr>
						</thead>
						<tbody>
							{#each lancamentos as l (l.id)}
								{@const sel = selecionados.includes(l.id)}
								{@const st = lancamentoStatusMeta(l.status)}
								{@const tp = tipoMeta(l.tipo)}
								{@const atrasado = l.status === 'Atrasado'}
								<tr
									data-testid="lan-row"
									onclick={() => abrirDetalhe(l)}
									class="cursor-pointer border-b border-border transition last:border-0 hover:bg-elevated/40 {sel
										? 'bg-brand/5'
										: ''} {atrasado ? 'bg-danger/[0.03]' : ''}"
								>
									<td class="px-4 py-3" onclick={(e) => e.stopPropagation()}>
										<input
											type="checkbox"
											data-testid="lan-checkbox"
											checked={sel}
											onchange={(e) =>
												alternarUm(l.id, (e.currentTarget as HTMLInputElement).checked)}
											aria-label={`Selecionar ${l.codigo}`}
											class="h-4 w-4 rounded border-border bg-surface accent-brand"
										/>
									</td>
									<td class="px-4 py-3 font-mono text-xs tabular-nums {atrasado ? 'text-danger' : 'text-muted'}">
										{formatDateBR(l.dataVencimento)}
									</td>
									<td class="px-4 py-3">
										<p class="truncate font-medium text-ink">{descricao(l)}</p>
										<p class="font-mono text-xs text-muted">{referencia(l)}</p>
									</td>
									<td class="px-4 py-3 text-xs text-muted">{l.categoria.nome}</td>
									<td class="px-4 py-3">
										<TypeBadge tone={tp.color === 'success' ? 'success' : 'danger'} label={tp.label} />
									</td>
									<td
										class="px-4 py-3 text-right font-mono text-sm tabular-nums {l.tipo === 'Entrada'
											? 'text-success'
											: 'text-danger'}"
									>
										{formatSignedBRL(l.valor, l.tipo)}
									</td>
									<td class="px-4 py-3">
										<StatusBadge label={st.label} color={st.color} />
									</td>
									<td class="px-4 py-3 text-right" onclick={(e) => e.stopPropagation()}>
										<RowActions
											label={`Ações de ${l.codigo}`}
											actions={[
												{ id: 'detalhe', label: 'Ver detalhe', icon: 'eye' },
												{ id: 'pagamento', label: 'Registrar pagamento', icon: 'check' }
											]}
											onSelect={(id) => acaoLinha(l, id)}
										/>
									</td>
								</tr>
							{/each}
						</tbody>
					</table>
				</div>
				{#if paginacao}
					<div class="space-y-2 border-t border-border px-4 py-3">
						<p class="text-xs text-muted">
							Mostrando {lancamentos.length} de {paginacao.totalItems} lançamentos
						</p>
						<Pagination
							page={paginacao.page}
							totalPages={paginacao.totalPages}
							totalItems={paginacao.totalItems}
							pageSize={params.pageSize}
							onPage={onPage}
							onPageSize={onPageSize}
							label="lançamentos"
						/>
					</div>
				{/if}
			</div>

			<div class="space-y-2 md:hidden">
				{#each lancamentos as l (l.id)}
					{@const st = lancamentoStatusMeta(l.status)}
					{@const tp = tipoMeta(l.tipo)}
					<article
						data-testid="lan-row"
						class="rounded-xl border border-border bg-surface p-4 {l.status === 'Atrasado'
							? 'bg-danger/[0.03]'
							: ''}"
					>
						<div class="flex items-start gap-3">
							<input
								type="checkbox"
								data-testid="lan-checkbox"
								checked={selecionados.includes(l.id)}
								onchange={(e) => alternarUm(l.id, (e.currentTarget as HTMLInputElement).checked)}
								aria-label={`Selecionar ${l.codigo}`}
								class="mt-1 h-4 w-4 shrink-0 rounded border-border bg-surface accent-brand"
							/>
							<button
								type="button"
								onclick={() => abrirDetalhe(l)}
								aria-label={`Ver ${l.codigo}`}
								class="min-w-0 flex-1 text-left"
							>
								<span class="block truncate text-sm font-medium text-ink">{descricao(l)}</span>
								<span class="block font-mono text-xs text-muted">{referencia(l)}</span>
								<span class="mt-1 block font-mono text-xs {l.status === 'Atrasado' ? 'text-danger' : 'text-muted'}">
									Vence em {formatDateBR(l.dataVencimento)} · {l.categoria.nome}
								</span>
								<span class="mt-2 flex flex-wrap items-center gap-1.5">
									<TypeBadge tone={tp.color === 'success' ? 'success' : 'danger'} label={tp.label} />
									<StatusBadge label={st.label} color={st.color} />
								</span>
								<span
									class="mt-1.5 block font-mono text-sm tabular-nums {l.tipo === 'Entrada'
										? 'text-success'
										: 'text-danger'}"
								>
									{formatSignedBRL(l.valor, l.tipo)}
								</span>
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
				{#if paginacao}
					<div class="space-y-2 rounded-xl border border-border bg-surface px-4 py-3">
						<p class="text-xs text-muted">
							Mostrando {lancamentos.length} de {paginacao.totalItems} lançamentos
						</p>
						<Pagination
							page={paginacao.page}
							totalPages={paginacao.totalPages}
							totalItems={paginacao.totalItems}
							pageSize={params.pageSize}
							onPage={onPage}
							onPageSize={onPageSize}
							label="lançamentos"
						/>
					</div>
				{/if}
			</div>
		{/if}
	{/if}
</div>

{#if algumSelecionado}
	<div data-testid="bulk-bar">
		<span data-testid="bulk-count" class="sr-only">{selecionados.length} selecionados</span>
		<BulkActionsBar
			count={selecionados.length}
			onCancel={limparSelecao}
			actions={[{ label: 'Registrar pagamento/recebimento', onClick: () => pedirPagamento(selecionados) }]}
		/>
	</div>
{/if}

<Modal
	open={modalCategoria}
	title="Nova categoria"
	subtitle="Cadastre uma categoria financeira"
	onClose={() => (modalCategoria = false)}
	width="sm"
>
	{#snippet children()}
		<div class="space-y-3">
			<div>
				<label for="cat-nome" class="mb-1 block text-xs font-medium text-muted">
					Nome <span class="text-danger">*</span>
				</label>
				<input
					id="cat-nome"
					type="text"
					bind:value={catNome}
					placeholder="Ex.: Vendas, Aluguel…"
					class="w-full rounded-md border border-border bg-elevated px-3 py-2.5 text-sm text-ink placeholder:text-muted/60 focus:border-brand focus:ring-2 focus:ring-brand/30 focus:outline-none"
				/>
			</div>
			<fieldset>
				<legend class="mb-1 block text-xs font-medium text-muted">Tipo</legend>
				<div class="flex gap-2">
					{#each [{ id: 'Receita', label: 'Receita' }, { id: 'Despesa', label: 'Despesa' }] as opt (opt.id)}
						<label
							class="flex flex-1 cursor-pointer items-center gap-2 rounded-md border px-3 py-2.5 text-sm transition {catTipo === opt.id
								? 'border-brand/60 bg-brand/10 text-ink'
								: 'border-border bg-elevated text-muted'}"
						>
							<input
								type="radio"
								name="cat-tipo"
								value={opt.id}
								checked={catTipo === opt.id}
								onchange={() => (catTipo = opt.id as CategoriaTipo)}
								class="h-4 w-4 accent-brand"
							/>
							{opt.label}
						</label>
					{/each}
				</div>
			</fieldset>
			<div>
				<label for="cat-descricao" class="mb-1 block text-xs font-medium text-muted">Descrição</label>
				<textarea
					id="cat-descricao"
					bind:value={catDescricao}
					rows={2}
					placeholder="Opcional"
					class="w-full rounded-md border border-border bg-elevated px-3 py-2.5 text-sm text-ink placeholder:text-muted/60 focus:border-brand focus:ring-2 focus:ring-brand/30 focus:outline-none"
				></textarea>
			</div>
		</div>
	{/snippet}
	{#snippet footer()}
		<button
			type="button"
			onclick={() => (modalCategoria = false)}
			disabled={ocupado}
			class="rounded-md border border-border bg-surface px-4 py-2 text-sm font-medium text-ink transition hover:bg-elevated disabled:opacity-50"
		>
			Cancelar
		</button>
		<button
			type="button"
			onclick={() => void confirmarCategoria()}
			disabled={ocupado || !catNome.trim()}
			class="rounded-md bg-brand px-4 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi disabled:opacity-50"
		>
			{ocupado ? 'Salvando…' : 'Criar categoria'}
		</button>
	{/snippet}
</Modal>

<Modal
	open={modalNovo}
	title="Novo lançamento"
	subtitle="Registre uma entrada ou saída"
	onClose={() => (modalNovo = false)}
	width="sm"
>
	{#snippet children()}
		<div class="space-y-3">
			<Select
				id="novo-categoria"
				label="Categoria"
				options={CATEGORIA_OPCOES}
				value={novoCategoria}
				onChange={(v) => (novoCategoria = v)}
				required
				placeholder={categorias.length === 0 ? 'Nenhuma categoria cadastrada' : 'Selecione…'}
				hint={categorias.length === 0 ? 'Crie uma categoria antes de lançar.' : ''}
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
								name="novo-tipo"
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
					<label for="novo-vencimento" class="mb-1 block text-xs font-medium text-muted">
						Vencimento <span class="text-danger">*</span>
					</label>
					<input
						id="novo-vencimento"
						type="date"
						bind:value={novoVencimento}
						class="w-full rounded-md border border-border bg-elevated px-3 py-2.5 text-sm text-ink focus:border-brand focus:ring-2 focus:ring-brand/30 focus:outline-none"
					/>
				</div>
				<div>
					<label for="novo-pagamento" class="mb-1 block text-xs font-medium text-muted">
						Pagamento
					</label>
					<input
						id="novo-pagamento"
						type="date"
						bind:value={novoPagamento}
						class="w-full rounded-md border border-border bg-elevated px-3 py-2.5 text-sm text-ink focus:border-brand focus:ring-2 focus:ring-brand/30 focus:outline-none"
					/>
				</div>
			</div>
			<div>
				<label for="novo-referencia" class="mb-1 block text-xs font-medium text-muted">
					Referência
				</label>
				<input
					id="novo-referencia"
					type="text"
					bind:value={novoReferencia}
					placeholder="Ex.: EN-2051"
					class="w-full rounded-md border border-border bg-elevated px-3 py-2.5 font-mono text-sm text-ink placeholder:text-muted/60 focus:border-brand focus:ring-2 focus:ring-brand/30 focus:outline-none"
				/>
			</div>
			<div>
				<label for="novo-observacao" class="mb-1 block text-xs font-medium text-muted">
					Observação
				</label>
				<textarea
					id="novo-observacao"
					bind:value={novoObservacao}
					rows={2}
					placeholder="Opcional"
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
			{ocupado ? 'Salvando…' : 'Criar lançamento'}
		</button>
	{/snippet}
</Modal>

<Modal
	open={detalhe !== null}
	title={detalhe ? detalhe.codigo : 'Lançamento'}
	subtitle="Ficha do lançamento"
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
					<dd
						class="font-mono font-semibold tabular-nums {detalhe.tipo === 'Entrada'
							? 'text-success'
							: 'text-danger'}"
					>
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
						<dt class="text-xs text-muted">Observação</dt>
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
				onclick={() => detalhe && pedirPagamento([detalhe.id])}
				class="rounded-md bg-brand px-4 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi"
			>
				Registrar {detalhe.tipo === 'Entrada' ? 'recebimento' : 'pagamento'}
			</button>
		{/if}
	{/snippet}
</Modal>

<Modal
	open={modalPagamento}
	title={pagamentoAlvos.length > 1
		? `Registrar pagamento (${pagamentoAlvos.length})`
		: 'Registrar recebimento/pagamento'}
	subtitle="A baixa muda o status para Pago"
	onClose={() => (modalPagamento = false)}
	width="sm"
>
	{#snippet children()}
		<div class="space-y-3">
			<div>
				<label for="pag-data" class="mb-1 block text-xs font-medium text-muted">
					Data <span class="text-danger">*</span>
				</label>
				<input
					id="pag-data"
					type="date"
					bind:value={pagamentoData}
					class="w-full rounded-md border border-border bg-elevated px-3 py-2.5 text-sm text-ink focus:border-brand focus:ring-2 focus:ring-brand/30 focus:outline-none"
				/>
			</div>
			<div>
				<label for="pag-obs" class="mb-1 block text-xs font-medium text-muted">Observação</label>
				<textarea
					id="pag-obs"
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
