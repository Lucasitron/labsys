<script lang="ts">
	import { goto } from '$app/navigation';
	import type { PageProps } from './$types';
	import { get } from 'svelte/store';
	import { auth } from '$lib/stores/auth';
	import type { Orcamento } from '$lib/types/vendas';
	import { canEditVendas } from '$lib/utils/permissions';
	import { formatNumber } from '$lib/utils/format';
	import { formatDateBR, formatMoneyBRL } from '$lib/utils/vendas-format';
	import { orcamentoStatusMeta } from '$lib/utils/vendas-status';
	import { toasts } from '$lib/stores/toast';
	import PageHeader from '$lib/components/ui/PageHeader.svelte';
	import SearchInput from '$lib/components/ui/SearchInput.svelte';
	import Pagination from '$lib/components/ui/Pagination.svelte';
	import Avatar from '$lib/components/ui/Avatar.svelte';
	import Icon from '$lib/components/ui/Icon.svelte';
	import StatusBadge from '$lib/components/ui/StatusBadge.svelte';
	import TableSkeleton from '$lib/components/ui/TableSkeleton.svelte';
	import EmptyState from '$lib/components/ui/EmptyState.svelte';
	import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte';
	import RowActions from '$lib/components/ui/RowActions.svelte';
	import SolicitarEdicaoModal from '../components/SolicitarEdicaoModal.svelte';

	let { data }: PageProps = $props();

	const usuario = $derived(get(auth).user);
	const params = $derived(data.params);
	const resultado = $derived(data.resultado);
	const erro = $derived(data.error);

	const orcamentos = $derived(resultado?.orcamentos ?? []);
	const contagens = $derived<Record<string, number>>(resultado?.counts ?? {});
	const paginacao = $derived(
		(resultado as { pagination?: { page: number; totalPages: number; totalItems: number } } | null)
			?.pagination ?? null
	);

	const carregando = $derived(resultado === null && erro === null);
	const comErro = $derived(erro !== null && resultado === null);

	const temBusca = $derived(params.search !== '');
	const filtrado = $derived(params.tab !== 'todos' || temBusca);

	const ABAS = [
		{ id: 'todos', label: 'Todos' },
		{ id: 'pendentes', label: 'Pendentes' },
		{ id: 'aprovados', label: 'Aprovados' },
		{ id: 'ajuste', label: 'Em ajuste' },
		{ id: 'recusados', label: 'Recusados' }
	];

	function numero(valor: unknown): number | null {
		const n = Number(valor);
		return Number.isFinite(n) ? n : null;
	}

	// Contagens servidas pelo backend (nunca derivadas no client).
	function contagemAba(id: string): number | null {
		switch (id) {
			case 'pendentes':
				return numero(contagens['Pendentes'] ?? contagens['Pendente']);
			case 'aprovados':
				return numero(contagens['Aprovados'] ?? contagens['Aprovado']);
			case 'ajuste':
				return numero(contagens['Ajustes'] ?? contagens['Ajuste']);
			case 'recusados':
				return numero(contagens['Recusados'] ?? contagens['Recusado']);
			default:
				return numero(contagens['Todos'] ?? contagens['Total']);
		}
	}

	function kpi(chaves: string[]): string {
		for (const chave of chaves) {
			const n = numero(contagens[chave]);
			if (n !== null) return formatNumber(n);
		}
		return '—';
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
			search: params.search || undefined,
			page: params.page,
			pageSize: params.pageSize === 10 ? undefined : params.pageSize
		};
		const merged = { ...base, ...overrides };
		const url = new URLSearchParams();
		for (const [chave, valor] of Object.entries(merged)) {
			if (valor !== undefined && valor !== '') url.append(chave, String(valor));
		}
		const qs = url.toString();
		void goto(`/vendas/orcamentos${qs ? `?${qs}` : ''}`);
	}

	function trocarAba(id: string): void {
		navegar({ tab: id === 'todos' ? undefined : id, page: 1 });
	}

	function onSearch(termo: string): void {
		navegar({ search: termo || undefined, page: 1 });
	}

	function limparFiltros(): void {
		navegar({ tab: undefined, search: undefined, page: 1 });
	}

	function onPage(page: number): void {
		navegar({ page });
	}

	function onPageSize(size: number): void {
		navegar({ pageSize: size === 10 ? undefined : size, page: 1 });
	}

	function tentarNovamente(): void {
		void goto(`/vendas/orcamentos${window.location.search}`, { invalidateAll: true });
	}

	function abrirOrcamento(id: string): void {
		void goto(`/vendas/orcamentos/${id}`);
	}

	function duplicarIndisponivel(): void {
		toasts.warn('Duplicação indisponível — backend pendente (D-2).');
	}

	let sugerirAlvo = $state<Orcamento | null>(null);

	function acaoLinha(orcamento: Orcamento, acao: string): void {
		if (acao === 'ver') abrirOrcamento(orcamento.id);
		else if (acao === 'editar') abrirOrcamento(orcamento.id);
		else if (acao === 'sugerir') sugerirAlvo = orcamento;
		else if (acao === 'duplicar') duplicarIndisponivel();
	}

	const temProxima = $derived(orcamentos.length >= params.pageSize);
</script>

<svelte:head>
	<title>Orçamentos — Vendas — FabLab</title>
</svelte:head>

<div class="space-y-4">
	<PageHeader title="Orçamentos" subtitle="Acompanhe os orçamentos do laboratório até a aprovação.">
		{#snippet children()}
			<a
				href="/vendas/orcamentos/novo"
				class="inline-flex items-center gap-1.5 rounded-md bg-brand px-3.5 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi"
			>
				<Icon name="plus" class="h-4 w-4" /> Novo orçamento
			</a>
		{/snippet}
	</PageHeader>

	{#if comErro}
		<ErrorBanner
			message="Não foi possível carregar os orçamentos"
			hint="Verifique sua conexão e tente novamente. Se persistir, contate o suporte."
			onRetry={tentarNovamente}
			testid="orc-retry"
		/>
	{:else if carregando}
		<div class="grid grid-cols-2 gap-4 lg:grid-cols-4" aria-hidden="true">
			{#each [1, 2, 3, 4] as i (i)}
				<div class="h-24 animate-pulse rounded-xl border border-border bg-elevated"></div>
			{/each}
		</div>
		<div class="h-10 animate-pulse rounded-lg bg-elevated"></div>
		<div class="rounded-xl border border-border bg-surface p-4">
			<TableSkeleton rows={6} columns={5} />
		</div>
	{:else}
		<!-- KPIs servidos pelo backend (nunca derivados no client) -->
		<section class="grid grid-cols-2 gap-4 lg:grid-cols-4" aria-label="Indicadores">
			<div class="rounded-xl border border-border bg-elevated p-4">
				<p class="text-xs text-muted">Pendentes</p>
				<p class="mt-1 font-mono text-2xl font-semibold tabnums text-ink">
					{kpi(['Pendentes', 'Pendente'])}
				</p>
				<p class="mt-0.5 text-xs text-muted">aguardando decisão</p>
			</div>
			<div class="rounded-xl border border-border bg-elevated p-4">
				<p class="text-xs text-muted">Aprovados</p>
				<p class="mt-1 font-mono text-2xl font-semibold tabnums text-ink">
					{kpi(['Aprovados', 'Aprovado'])}
				</p>
				<p class="mt-0.5 text-xs text-muted">prontos p/ encomenda</p>
			</div>
			<div class="rounded-xl border border-border bg-elevated p-4">
				<p class="text-xs text-muted">Em ajuste</p>
				<p class="mt-1 font-mono text-2xl font-semibold tabnums text-ink">
					{kpi(['Ajustes', 'Ajuste'])}
				</p>
				<p class="mt-0.5 text-xs text-muted">precisam de revisão</p>
			</div>
			<div class="rounded-xl border border-border bg-elevated p-4">
				<p class="text-xs text-muted">Recusados</p>
				<p class="mt-1 font-mono text-2xl font-semibold tabnums text-ink">
					{kpi(['Recusados', 'Recusado'])}
				</p>
				<p class="mt-0.5 text-xs text-muted">encerrados</p>
			</div>
		</section>

		<div class="flex flex-wrap items-center gap-2">
			<div class="min-w-56 flex-1">
				<SearchInput
					value={params.search}
					onSearch={onSearch}
					placeholder="Buscar por código ou cliente…"
					delay={300}
					label="Buscar orçamentos"
				/>
			</div>
		</div>

		<div
			role="tablist"
			aria-label="Filtrar orçamentos por status"
			class="flex items-center gap-1 overflow-x-auto border-b border-border px-1"
		>
			{#each ABAS as aba (aba.id)}
				{@const count = contagemAba(aba.id)}
				{@const ativa = params.tab === aba.id}
				<button
					type="button"
					role="tab"
					aria-selected={ativa}
					data-testid="orc-tab-{aba.id}"
					onclick={() => trocarAba(aba.id)}
					class="inline-flex shrink-0 items-center gap-1.5 border-b-2 px-3 py-2 text-sm font-medium transition {ativa
						? 'border-brand text-brand'
						: 'border-transparent text-muted hover:text-ink'}"
				>
					{aba.label}
					{#if count !== null}
						<span
							class="inline-flex h-4 min-w-4 items-center justify-center rounded-full px-1 text-[10px] font-bold {ativa
								? 'bg-brand text-white'
								: 'bg-muted/15 text-muted'}"
						>{count}</span
						>
					{/if}
				</button>
			{/each}
		</div>

		{#if orcamentos.length === 0}
			<div class="rounded-xl border border-border bg-surface">
				{#if filtrado}
					<EmptyState
						icon="search"
						title="Nenhum orçamento encontrado"
						description={params.tab === 'todos'
							? 'Nenhum orçamento combina com a busca aplicada.'
							: 'Nenhum orçamento nesta situação.'}
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
						icon="document"
						title="Nenhum orçamento cadastrado"
						description="Crie o primeiro orçamento para começar o ciclo de vendas do laboratório."
					>
						{#snippet children()}
							<a
								href="/vendas/orcamentos/novo"
								class="inline-flex items-center gap-1.5 rounded-md bg-brand px-3.5 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi"
							>
								<Icon name="plus" class="h-4 w-4" /> Novo orçamento
							</a>
						{/snippet}
					</EmptyState>
				{/if}
			</div>
		{:else}
			<div class="overflow-hidden rounded-xl border border-border bg-surface">
				<div class="overflow-x-auto">
					<table class="w-full min-w-[820px] text-sm">
						<thead>
							<tr
								class="border-b border-border bg-elevated/50 text-left text-[11px] uppercase tracking-wide text-muted"
							>
								<th class="px-4 py-2.5 font-medium">Código</th>
								<th class="px-4 py-2.5 font-medium">Cliente</th>
								<th class="px-4 py-2.5 text-right font-medium">Itens</th>
								<th class="px-4 py-2.5 text-right font-medium">Valor</th>
								<th class="px-4 py-2.5 font-medium">Validade</th>
								<th class="px-4 py-2.5 font-medium">Status</th>
								<th class="w-12 px-4 py-2.5 text-right font-medium">
									<span class="sr-only">Ações</span>
								</th>
							</tr>
						</thead>
						<tbody>
							{#each orcamentos as orcamento (orcamento.id)}
								{@const meta = orcamentoStatusMeta(orcamento.status)}
								{@const podeEditar = canEditVendas(usuario, { createdBy: orcamento.createdBy })}
								<tr
									data-testid="orc-row"
									onclick={() => abrirOrcamento(orcamento.id)}
									class="cursor-pointer border-b border-border transition last:border-0 hover:bg-elevated/40"
								>
									<td class="px-4 py-3 font-mono text-xs text-muted">{orcamento.codigo}</td>
									<td class="px-4 py-3">
										<div class="flex min-w-0 items-center gap-3">
											<Avatar name={orcamento.cliente.nome} size="sm" tone="brand" />
											<span class="truncate font-medium text-ink">{orcamento.cliente.nome}</span>
										</div>
									</td>
									<td class="px-4 py-3 text-right font-mono text-sm tabnums text-ink">
										{formatNumber(orcamento.qtdItens)}
									</td>
									<td class="px-4 py-3 text-right font-mono text-sm tabnums text-ink">
										{formatMoneyBRL(orcamento.valorTotal)}
									</td>
									<td class="px-4 py-3 font-mono text-xs text-muted tabnums">
										{dataBR(orcamento.validade)}
									</td>
									<td class="px-4 py-3">
										<StatusBadge label={meta.label} color={meta.color} />
									</td>
									<td class="px-4 py-3 text-right" onclick={(e) => e.stopPropagation()}>
										<RowActions
											label={`Ações do orçamento ${orcamento.codigo}`}
											actions={[
												{ id: 'ver', label: 'Ver', icon: 'eye' },
												{ id: 'editar', label: 'Editar', icon: 'pencil', hidden: !podeEditar },
												{
													id: 'sugerir',
													label: 'Sugerir alteração',
													icon: 'document',
													hidden: podeEditar
												},
												{ id: 'duplicar', label: 'Duplicar', icon: 'duplicate' }
											]}
											onSelect={(id) => acaoLinha(orcamento, id)}
										/>
									</td>
								</tr>
							{/each}
						</tbody>
					</table>
				</div>
				<div class="border-t border-border px-4 py-3">
					{#if paginacao}
						<Pagination
							page={paginacao.page}
							totalPages={paginacao.totalPages}
							totalItems={paginacao.totalItems}
							pageSize={params.pageSize}
							onPage={onPage}
							onPageSize={onPageSize}
							label="orçamentos"
						/>
					{:else}
						<div class="flex flex-wrap items-center justify-between gap-2">
							<p class="text-xs text-muted">página {params.page} · {orcamentos.length} orçamentos</p>
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
										<Icon name="chevron-left" class="h-4 w-4" />
									</button>
									<button
										type="button"
										onclick={() => onPage(params.page + 1)}
										disabled={!temProxima}
										aria-label="Próxima página"
										class="inline-flex h-8 w-8 items-center justify-center rounded-lg border border-border bg-surface text-muted transition-colors hover:border-brand/50 hover:text-brandhi disabled:cursor-not-allowed disabled:opacity-40"
									>
										<Icon name="chevron-right" class="h-4 w-4" />
									</button>
								</div>
							</div>
						</div>
					{/if}
				</div>
			</div>
		{/if}
	{/if}
</div>

{#if sugerirAlvo}
	<SolicitarEdicaoModal
		alvo={{ tipo: 'OC', id: sugerirAlvo.id, nome: sugerirAlvo.codigo }}
		onClose={() => (sugerirAlvo = null)}
	/>
{/if}
