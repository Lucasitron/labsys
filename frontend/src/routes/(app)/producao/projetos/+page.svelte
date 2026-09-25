<script lang="ts">
	import { goto, invalidateAll } from '$app/navigation';
	import type { PageProps } from './$types';
	import type { NomeFunc, Projeto, ProjetoStatus } from '$lib/types/producao';
	import { PROJETO_STATUS_META } from '$lib/utils/producao-status';
	import ModalNovoProjeto from '$lib/components/producao/ModalNovoProjeto.svelte';
	import PageHeader from '$lib/components/ui/PageHeader.svelte';
	import SearchInput from '$lib/components/ui/SearchInput.svelte';
	import Dropdown from '$lib/components/ui/Dropdown.svelte';
	import Chip from '$lib/components/ui/Chip.svelte';
	import StatusBadge from '$lib/components/ui/StatusBadge.svelte';
	import Avatar from '$lib/components/ui/Avatar.svelte';
	import Skeleton from '$lib/components/ui/Skeleton.svelte';
	import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte';
	import EmptyState from '$lib/components/ui/EmptyState.svelte';
	import Icon from '$lib/components/ui/Icon.svelte';

	let { data }: PageProps = $props();

	const canEditProducao = $derived(data.canEditProducao ?? false);

	const result = $derived(data.projetos);
	const error = $derived(data.error);
	const projetos = $derived(result?.dados ?? []);
	const loading = $derived(result === null && error === null);
	const hasError = $derived(error !== null && result === null);

	const params = $derived(data.params);
	const responsaveis = $derived(data.responsaveis as NomeFunc[]);

	const hasFilters = $derived(params.search !== '' || params.status.length > 0);

	const kpiCls = 'rounded-xl border border-border bg-surface p-4';

	const totalProjetos = $derived(projetos.length);
	const emAndamento = $derived(projetos.filter((p) => p.status === 'Em andamento').length);
	const concluidos = $derived(projetos.filter((p) => p.status === 'Concluído').length);
	const atrasados = $derived(
		projetos.filter((p) => {
			if (p.status !== 'Em andamento' && p.status !== 'Planejado') return false;
			const limite = new Date();
			limite.setDate(limite.getDate() + 7);
			return new Date(p.prazo) <= limite;
		}).length
	);

	const statusOptions = $derived(
		Object.entries(PROJETO_STATUS_META).map(([id, meta]) => ({
			id,
			label: meta.label
		}))
	);

	const filtrado = $derived(
		projetos.filter((p) => {
			const termo = params.search.toLowerCase().trim();
			const coincideBusca =
				termo === '' ||
				p.nome.toLowerCase().includes(termo) ||
				p.codigo.toLowerCase().includes(termo) ||
				p.responsavel.nome.toLowerCase().includes(termo);
			const coincideStatus = params.status.length === 0 || params.status.includes(p.status);
			return coincideBusca && coincideStatus;
		})
	);

	let modalNovo = $state(false);

	function navegar(overrides: Record<string, unknown>): string {
		const merged = { search: params.search, status: params.status, ...overrides };
		const url = new URLSearchParams();
		for (const [chave, valor] of Object.entries(merged)) {
			if (Array.isArray(valor)) {
				for (const v of valor) if (v) url.append(chave, v);
			} else if (valor !== undefined && valor !== '') {
				url.append(chave, String(valor));
			}
		}
		const qs = url.toString();
		return `/producao/projetos${qs ? `?${qs}` : ''}`;
	}

	function toggleStatus(id: string): void {
		const atual = params.status;
		const proximo = atual.includes(id as ProjetoStatus)
			? atual.filter((v) => v !== id)
			: ([...atual, id] as ProjetoStatus[]);
		void goto(navegar({ status: proximo, page: 1 }));
	}

	function limparFiltro(id?: string): void {
		const atual = params.status;
		const proximo = id
			? atual.filter((v) => v !== id)
			: ([] as ProjetoStatus[]);
		void goto(navegar({ status: proximo, page: 1 }));
	}

	function limparTodos(): void {
		void goto(navegar({ search: '', status: [], page: 1 }));
	}

	function aoSalvo(_projeto: Projeto): void {
		modalNovo = false;
		void invalidateAll();
	}

	function formatarData(iso?: string): string {
		if (!iso) return '—';
		const [a, m, d] = iso.slice(0, 10).split('-');
		return d ? `${d}/${m}/${a}` : '—';
	}

	const tresCoisas = ['Código único', 'Tema descrito', 'Responsável (RGA) vinculado'];
</script>

<svelte:head>
	<title>Projetos universitários — Produção — FabLab</title>
</svelte:head>

<div class="space-y-4">
	<PageHeader
		title="Projetos universitários"
		subtitle="Cadastro, acompanhamento e histórico dos projetos com prazo e responsável."
	>
		{#snippet children()}
			{#if canEditProducao}
				<button
					type="button"
					data-testid="proj-novo"
					onclick={() => (modalNovo = true)}
					class="inline-flex items-center gap-1.5 rounded-lg bg-brand px-3 py-2 text-sm font-semibold text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi"
				>
					<Icon name="plus" class="h-4 w-4" /> Novo projeto
				</button>
			{/if}
		{/snippet}
	</PageHeader>

	<div class="grid grid-cols-2 gap-3 lg:grid-cols-4">
		<div class={kpiCls}>
			<span class="text-xs text-muted">Projetos</span>
			<p class="mt-1 text-2xl font-semibold text-ink tabular-nums">{totalProjetos}</p>
		</div>
		<div class={kpiCls}>
			<span class="text-xs text-muted">Em andamento</span>
			<p class="mt-1 text-2xl font-semibold text-brandhi tabular-nums">{emAndamento}</p>
		</div>
		<div class={kpiCls}>
			<span class="text-xs text-muted">Concluídos</span>
			<p class="mt-1 text-2xl font-semibold text-success tabular-nums">{concluidos}</p>
		</div>
		<div class={kpiCls}>
			<span class="text-xs text-muted">Com prazo apertado</span>
			<p class="mt-1 text-2xl font-semibold text-danger tabular-nums">{atrasados}</p>
		</div>
	</div>

	{#if hasError}
		<ErrorBanner
			message="Não foi possível carregar os projetos"
			hint={error ?? ''}
			onRetry={() => void goto('/producao/projetos', { invalidateAll: true })}
		/>
	{:else if loading}
		<div class="grid grid-cols-1 gap-3 md:grid-cols-2 xl:grid-cols-3">
			{#each [0, 1, 2, 3, 4, 5] as i (i)}
				<div class="space-y-3 rounded-xl border border-border bg-surface p-4">
					<Skeleton class="h-5 w-1/2" />
					<Skeleton class="h-4 w-2/3" />
					<Skeleton class="h-4 w-1/3" />
					<Skeleton class="h-10 w-full" />
				</div>
			{/each}
		</div>
	{:else}
		<!-- Filtros -->
		<div class="flex flex-wrap items-center gap-2">
			<SearchInput
				value={params.search}
				onSearch={(v) => void goto(navegar({ search: v, page: 1 }))}
				placeholder="Buscar por código, tema ou responsável…"
				class="min-w-56 flex-1"
			/>
			<Dropdown
				label="Status"
				options={statusOptions}
				selected={params.status}
				onToggle={(id) => void toggleStatus(id)}
				onClear={() => void limparFiltro()}
			/>
		</div>

		{#if hasFilters}
			<div class="flex flex-wrap items-center gap-1.5">
				{#each params.status as s (s)}
					<Chip label={PROJETO_STATUS_META[s]?.label ?? s} onRemove={() => void limparFiltro(s)} />
				{/each}
				<button
					onclick={() => void limparTodos()}
					class="text-xs font-medium text-brandhi transition hover:text-brand"
				>
					Limpar filtros
				</button>
			</div>
		{/if}

		<!-- Conteúdo -->
		{#if projetos.length === 0}
			<div class="rounded-xl border border-border bg-surface">
				<EmptyState
					icon="folder"
					title="Ainda não há projetos"
					description="Para iniciar um projeto é preciso código único, tema descrito e responsável vinculado."
				>
					{#snippet children()}
						{#if canEditProducao}
							<button
								type="button"
								data-testid="proj-novo"
								onclick={() => (modalNovo = true)}
								class="inline-flex items-center gap-1.5 rounded-lg bg-brand px-3 py-2 text-sm font-semibold text-white transition hover:bg-brandhi"
							>
								<Icon name="plus" class="h-4 w-4" /> Novo projeto
							</button>
						{/if}
					{/snippet}
				</EmptyState>
			</div>
		{:else if filtrado.length === 0}
			<div class="rounded-xl border border-border bg-surface">
				<EmptyState
					icon="filter"
					title="Nenhum projeto encontrado"
					description="Ajuste ou limpe os filtros para ver mais resultados."
				>
					{#snippet children()}
						<button
							onclick={() => void limparTodos()}
							class="rounded-lg border border-border bg-surface px-3 py-2 text-sm font-medium text-ink transition hover:border-brand/50 hover:text-brandhi"
						>
							Limpar filtros
						</button>
					{/snippet}
				</EmptyState>
			</div>
		{:else}
			<div class="grid grid-cols-1 gap-3 md:grid-cols-2 xl:grid-cols-3">
				{#each filtrado as projeto (projeto.id)}
					<article
						data-testid="proj-card"
						class="group flex flex-col rounded-xl border border-border bg-surface p-4 transition hover:border-brand/40"
					>
						<a
							href={`/producao/projetos/${projeto.id}`}
							class="flex flex-1 flex-col focus:outline-none focus-visible:ring-2 focus-visible:ring-brand/50"
						>
							<div class="flex items-start justify-between gap-2">
								<h3 class="truncate text-sm font-semibold text-ink group-hover:text-brandhi">
									{projeto.nome}
								</h3>
								<span class="shrink-0 rounded-full border border-border bg-elevated/60 px-2 py-0.5 font-mono text-[10px] text-muted">
									{projeto.codigo}
								</span>
							</div>

							<p class="mt-2 line-clamp-2 text-xs text-muted">{projeto.descricao}</p>

							<div class="mt-3 flex items-center gap-2">
								<Avatar name={projeto.responsavel.nome} size="xs" />
								<span class="truncate text-xs text-ink">{projeto.responsavel.nome}</span>
							</div>

							<div class="mt-auto space-y-1 pt-3 text-[11px] text-muted">
								<p class="flex items-center justify-between gap-2">
									<span>Prazo</span>
									<span class="tabular-nums text-ink">{formatarData(projeto.prazo)}</span>
								</p>
								<p class="flex items-center justify-between gap-2">
									<span>Progresso</span>
									<span class="tabular-nums text-ink">{projeto.progresso}%</span>
								</p>
							</div>

							<div class="mt-2 h-1.5 w-full overflow-hidden rounded-full bg-elevated">
								<div
									class="h-full rounded-full bg-brand transition-all"
									style={`width: ${Math.min(100, Math.max(0, projeto.progresso))}%`}
								></div>
							</div>

							<div class="mt-3 flex items-center justify-between gap-2">
								<StatusBadge {...PROJETO_STATUS_META[projeto.status]} />
								<Icon name="arrow-right" class="h-4 w-4 text-muted transition group-hover:translate-x-0.5 group-hover:text-brandhi" />
							</div>
						</a>
					</article>
				{/each}
			</div>
		{/if}
	{/if}

	<ModalNovoProjeto
		open={modalNovo}
		projeto={null}
		responsaveis={responsaveis}
		onClose={() => (modalNovo = false)}
		onSalvo={aoSalvo}
	/>
</div>