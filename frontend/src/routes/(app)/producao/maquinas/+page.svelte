<script lang="ts">
	import { goto, invalidateAll } from '$app/navigation';
	import type { PageProps } from './$types';
	import { CARTOES } from './+page';
	import type { NomeFunc } from '$lib/types/producao';
	import { CARTAO_META, MAQUINA_STATUS_META } from '$lib/utils/producao-status';
	import { criarMaquina } from '$lib/api/producao/client';
	import { ApiError, NetworkError } from '$lib/api/client';
	import { toasts } from '$lib/stores/toast';
	import CartaoBadge from '$lib/components/producao/CartaoBadge.svelte';
	import PageHeader from '$lib/components/ui/PageHeader.svelte';
	import SearchInput from '$lib/components/ui/SearchInput.svelte';
	import Dropdown from '$lib/components/ui/Dropdown.svelte';
	import Chip from '$lib/components/ui/Chip.svelte';
	import StatusBadge from '$lib/components/ui/StatusBadge.svelte';
	import Avatar from '$lib/components/ui/Avatar.svelte';
	import Skeleton from '$lib/components/ui/Skeleton.svelte';
	import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte';
	import EmptyState from '$lib/components/ui/EmptyState.svelte';
	import Modal from '$lib/components/ui/Modal.svelte';
	import Icon from '$lib/components/ui/Icon.svelte';

	let { data }: PageProps = $props();

	const canEditProducao = $derived(data.canEditProducao ?? false);

	const result = $derived(data.maquinas);
	const error = $derived(data.error);
	const maquinas = $derived(result?.dados ?? []);
	const loading = $derived(result === null && error === null);
	const hasError = $derived(error !== null && result === null);

	const params = $derived(data.params);
	const responsaveis = $derived(data.responsaveisManutencao as NomeFunc[]);

	const kpiCls = 'rounded-xl border border-border bg-surface p-4';

	const totalMaquinas = $derived(maquinas.length);
	const foraVerde = $derived(maquinas.filter((m) => m.cartao !== 'Verde').length);
	const emManutencao = $derived(maquinas.filter((m) => m.status === 'Em manutenção').length);
	const suspensas = $derived(maquinas.filter((m) => m.status === 'Suspensa').length);

	const cartaoOptions = $derived(
		CARTOES.map((c) => ({ id: c, label: CARTAO_META[c].label }))
	);

	const categoriaOptions = $derived.by(() => {
		const mapa = new Map<string, string>();
		for (const m of maquinas) if (!mapa.has(m.categoria)) mapa.set(m.categoria, m.categoria);
		return [...mapa.entries()].map(([id, label]) => ({ id, label }));
	});

	const hasFilters = $derived(
		params.search !== '' || params.categoria.length > 0 || params.cartao.length > 0
	);

	const filtrado = $derived(
		maquinas.filter((m) => {
			const termo = params.search.toLowerCase().trim();
			const coincideBusca =
				termo === '' ||
				m.nome.toLowerCase().includes(termo) ||
				m.codigo.toLowerCase().includes(termo) ||
				m.categoria.toLowerCase().includes(termo) ||
				m.responsavelManutencao.nome.toLowerCase().includes(termo);
			const coincideCategoria =
				params.categoria.length === 0 || params.categoria.includes(m.categoria);
			const coincideCartao = params.cartao.length === 0 || params.cartao.includes(m.cartao);
			return coincideBusca && coincideCategoria && coincideCartao;
		})
	);

	// ---- modal criar ----
	let modalMaquina = $state<'novo' | null>(null);
	let formNome = $state('');
	let formCategoria = $state('');
	let formResponsavelId = $state('');
	let formEnviando = $state(false);
	let formErro = $state('');

	$effect(() => {
		if (modalMaquina === 'novo') {
			formNome = '';
			formCategoria = '';
			formResponsavelId = '';
			formErro = '';
		}
	});

	async function salvarMaquina(event: SubmitEvent): Promise<void> {
		event.preventDefault();
		if (modalMaquina !== 'novo') return;
		if (!formNome.trim() || !formCategoria.trim() || !formResponsavelId) {
			formErro = 'Preencha nome, categoria e responsável de manutenção.';
			return;
		}
		formEnviando = true;
		formErro = '';
		try {
			await criarMaquina({
				nome: formNome.trim(),
				categoria: formCategoria.trim(),
				responsavelManutencaoId: formResponsavelId
			});
			toasts.success('Máquina criada');
			modalMaquina = null;
			await invalidateAll();
		} catch (err) {
			formErro =
				err instanceof ApiError || err instanceof NetworkError
					? err.message
					: 'Não foi possível criar a máquina.';
		} finally {
			formEnviando = false;
		}
	}

	function navegar(overrides: Record<string, unknown>): string {
		const merged = {
			search: params.search,
			categoria: params.categoria,
			cartao: params.cartao,
			...overrides
		};
		const url = new URLSearchParams();
		for (const [chave, valor] of Object.entries(merged)) {
			if (Array.isArray(valor)) {
				for (const v of valor) if (v) url.append(chave, v);
			} else if (valor !== undefined && valor !== '') {
				url.append(chave, String(valor));
			}
		}
		const qs = url.toString();
		return `/producao/maquinas${qs ? `?${qs}` : ''}`;
	}

	function toggleFiltro(chave: 'cartao' | 'categoria', id: string): void {
		const atual = params[chave];
		const lista: string[] = atual;
		const proximo = lista.includes(id) ? lista.filter((v) => v !== id) : [...lista, id];
		void goto(navegar({ [chave]: proximo, page: 1 }));
	}

	function limparFiltro(chave: 'cartao' | 'categoria', id?: string): void {
		const atual = params[chave];
		const proximo = id ? atual.filter((v) => v !== id) : [];
		void goto(navegar({ [chave]: proximo, page: 1 }));
	}

	function limparTodos(): void {
		void goto(navegar({ search: '', categoria: [], cartao: [], page: 1 }));
	}

	const inputCls =
		'w-full rounded-lg border border-border bg-elevated px-3 py-2.5 text-sm text-ink placeholder:text-muted/60 focus:border-brand focus:outline-none focus:ring-1 focus:ring-brand/40';
	const kpiLabel = 'text-xs text-muted';
	const kpiValue = 'mt-1 text-2xl font-semibold tabular-nums';

	function limparErroForm(): void {
		if (formErro && modalMaquina === 'novo') formErro = '';
	}
</script>

<svelte:head>
	<title>Máquinas — Produção — FabLab</title>
</svelte:head>

<div class="space-y-4">
	<PageHeader
		title="Máquinas"
		subtitle="Inventário do parque de máquinas, cartões 5S e status de manutenção."
	>
		{#snippet children()}
			{#if canEditProducao}
				<button
					type="button"
					data-testid="maq-nova"
					onclick={() => (modalMaquina = 'novo')}
					class="inline-flex items-center gap-1.5 rounded-lg bg-brand px-3 py-2 text-sm font-semibold text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi"
				>
					<Icon name="plus" class="h-4 w-4" /> Nova máquina
				</button>
			{/if}
		{/snippet}
	</PageHeader>

	<div class="grid grid-cols-2 gap-3 lg:grid-cols-4">
		<div class={kpiCls}>
			<span class={kpiLabel}>Máquinas</span>
			<p class={`${kpiValue} text-ink`}>{totalMaquinas}</p>
		</div>
		<div class={kpiCls}>
			<span class={kpiLabel}>Fora do verde</span>
			<p class={`${kpiValue} text-danger`}>{foraVerde}</p>
		</div>
		<div class={kpiCls}>
			<span class={kpiLabel}>Em manutenção</span>
			<p class={`${kpiValue} text-warn`}>{emManutencao}</p>
		</div>
		<div class={kpiCls}>
			<span class={kpiLabel}>Suspensas</span>
			<p class={`${kpiValue} text-danger`}>{suspensas}</p>
		</div>
	</div>

	{#if hasError}
		<ErrorBanner
			message="Não foi possível carregar as máquinas"
			hint={error ?? ''}
			onRetry={() => void goto('/producao/maquinas', { invalidateAll: true })}
		/>
	{:else if loading}
		<div class="grid grid-cols-1 gap-3 md:grid-cols-2 xl:grid-cols-4">
			{#each [0, 1, 2, 3] as i (i)}
				<div class="space-y-3 rounded-xl border border-border bg-surface p-4">
					<Skeleton class="h-5 w-2/3" />
					<Skeleton class="h-4 w-1/2" />
					<Skeleton class="h-10 w-full" />
					<Skeleton class="h-3 w-3/4" />
				</div>
			{/each}
		</div>
	{:else}
		<!-- Filtros -->
		<div class="flex flex-wrap items-center gap-2">
			<SearchInput
				value={params.search}
				onSearch={(v) => void goto(navegar({ search: v, page: 1 }))}
				placeholder="Buscar máquina, código ou categoria…"
				class="min-w-56 flex-1"
			/>
			<Dropdown
				label="Categoria"
				options={categoriaOptions}
				selected={params.categoria}
				onToggle={(id) => void toggleFiltro('categoria', id)}
				onClear={() => void limparFiltro('categoria')}
			/>
			<Dropdown
				label="Cartão 5S"
				options={cartaoOptions}
				selected={params.cartao}
				onToggle={(id) => void toggleFiltro('cartao', id)}
				onClear={() => void limparFiltro('cartao')}
			/>
		</div>

		{#if hasFilters}
			<div class="flex flex-wrap items-center gap-1.5">
				{#each params.categoria as c (c)}
					<Chip label={c} onRemove={() => void limparFiltro('categoria', c)} />
				{/each}
				{#each params.cartao as c (c)}
					<Chip label={CARTAO_META[c].label} onRemove={() => void limparFiltro('cartao', c)} />
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
		{#if maquinas.length === 0}
			<div class="rounded-xl border border-border bg-surface">
				<EmptyState
					icon="cube"
					title="Ainda não há máquinas cadastradas"
					description="Cadastre a primeira máquina do parque para começar."
				>
					{#snippet children()}
						{#if canEditProducao}
							<button
								type="button"
								data-testid="maq-nova"
								onclick={() => (modalMaquina = 'novo')}
								class="inline-flex items-center gap-1.5 rounded-lg bg-brand px-3 py-2 text-sm font-semibold text-white transition hover:bg-brandhi"
							>
								<Icon name="plus" class="h-4 w-4" /> Nova máquina
							</button>
						{/if}
					{/snippet}
				</EmptyState>
			</div>
		{:else if filtrado.length === 0}
			<div class="rounded-xl border border-border bg-surface">
				<EmptyState
					icon="filter"
					title="Nenhuma máquina encontrada"
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
			<div class="grid grid-cols-1 gap-3 md:grid-cols-2 xl:grid-cols-4">
				{#each filtrado as maquina (maquina.id)}
					<article
						data-testid="maq-card"
						class="flex flex-col rounded-xl border border-border bg-surface p-4 transition hover:border-brand/40"
					>
						<a
							href={`/producao/maquinas/${maquina.id}`}
							class="group flex flex-1 flex-col focus:outline-none focus-visible:ring-2 focus-visible:ring-brand/50"
						>
							<div class="flex items-start justify-between gap-2">
								<h3 class="truncate text-sm font-semibold text-ink group-hover:text-brandhi">
									{maquina.nome}
								</h3>
								<span class="shrink-0 rounded-full border border-border bg-elevated/60 px-2 py-0.5 font-mono text-[10px] text-muted">
									{maquina.codigo}
								</span>
							</div>
							<p class="mt-1 text-xs capitalize text-muted">{maquina.categoria}</p>

							<div class="mt-3 flex items-center gap-2">
								<Avatar name={maquina.responsavelManutencao.nome} size="xs" />
								<span class="truncate text-xs text-ink">{maquina.responsavelManutencao.nome}</span>
							</div>

							<div class="mt-auto flex items-center justify-between pt-3">
								<StatusBadge {...MAQUINA_STATUS_META[maquina.status]} />
								<CartaoBadge cartao={maquina.cartao} />
							</div>
						</a>
					</article>
				{/each}
			</div>
		{/if}
	{/if}

	{#if modalMaquina === 'novo'}
		<Modal
			open={modalMaquina === 'novo'}
			title="Nova máquina"
			subtitle="Cadastro de máquina no parque do laboratório"
			onClose={formEnviando ? undefined : () => (modalMaquina = null)}
			width="md"
		>
			{#snippet children()}
				<form id="modal-maquina-form" data-testid="maq-nova" onsubmit={salvarMaquina} novalidate>
					{#if formErro}
						<div role="alert" class="mb-4 rounded-xl border border-danger/30 bg-danger/10 px-4 py-3 text-sm text-danger">
							{formErro}
						</div>
					{/if}

					<label class="block">
						<span class="mb-1.5 block text-xs font-medium text-muted">Nome *</span>
						<input
							type="text"
							bind:value={formNome}
							placeholder="ex.: Corte a laser"
							class={inputCls}
							oninput={limparErroForm}
						/>
					</label>

					<label class="mt-4 block">
						<span class="mb-1.5 block text-xs font-medium text-muted">Categoria *</span>
						<input
							type="text"
							bind:value={formCategoria}
							placeholder="ex.: a laser"
							class={inputCls}
							oninput={limparErroForm}
						/>
					</label>

					<label class="mt-4 block">
						<span class="mb-1.5 block text-xs font-medium text-muted">Responsável de manutenção *</span>
						<select bind:value={formResponsavelId} class={inputCls} onchange={limparErroForm}>
							<option value="" disabled>Selecione…</option>
							{#each responsaveis as resp (resp.id)}
								<option value={resp.id}>{resp.nome}</option>
							{/each}
						</select>
						{#if responsaveis.length === 0}
							<p class="mt-1 text-[11px] text-muted">
								Nenhum responsável disponível neste contexto — o vínculo depende do RH (lista completa).
							</p>
						{/if}
					</label>
				</form>
			{/snippet}
			{#snippet footer()}
				<button
					type="button"
					onclick={() => (modalMaquina = null)}
					disabled={formEnviando}
					class="rounded-md px-4 py-2 text-sm text-muted transition hover:text-ink disabled:opacity-50"
				>
					Cancelar
				</button>
				<button
					type="submit"
					form="modal-maquina-form"
					disabled={formEnviando}
					class="rounded-md bg-brand px-4 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi disabled:cursor-not-allowed disabled:opacity-50"
				>
					{formEnviando ? 'Salvando…' : 'Criar máquina'}
				</button>
			{/snippet}
		</Modal>
	{/if}
</div>

