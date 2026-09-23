<script lang="ts">
	import type { PageProps } from './$types';
	import { listGuias } from '$lib/api/rh/treinamentos';
	import type { Guia } from '$lib/types/rh';
	import { relativeTime } from '$lib/utils/format';
	import { guideStatusMeta } from '$lib/utils/rh-status';
	import { toasts } from '$lib/stores/toast';
	import PageHeader from '$lib/components/ui/PageHeader.svelte';
	import SearchInput from '$lib/components/ui/SearchInput.svelte';
	import Select from '$lib/components/ui/Select.svelte';
	import StatusBadge from '$lib/components/ui/StatusBadge.svelte';
	import Modal from '$lib/components/ui/Modal.svelte';
	import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte';
	import EmptyState from '$lib/components/ui/EmptyState.svelte';
	import Icon from '$lib/components/ui/Icon.svelte';

	let { data }: PageProps = $props();

	const canEdit = $derived(data.canEdit ?? false);

	interface MaquinaGuias {
		id: string;
		label: string;
		guides: Guia[];
	}

	let maquinas = $state<MaquinaGuias[]>([]);
	let carregando = $state(true);
	let erro = $state<string | null>(null);
	let busca = $state('');
	let maquinaFiltro = $state('');
	let recarregar = $state(0);
	let guiaAtivo = $state<{ guia: Guia; maquina: string } | null>(null);

	$effect(() => {
		void recarregar;
		const ctrl = new AbortController();
		const sinal = ctrl.signal;
		carregando = true;
		erro = null;

		void listGuias({}, fetch)
			.then((res) => {
				if (sinal.aborted) return;
				maquinas = res.machines;
			})
			.catch((err: unknown) => {
				if (sinal.aborted) return;
				erro = err instanceof Error ? err.message : 'Não foi possível carregar os guias';
			})
			.finally(() => {
				if (!sinal.aborted) carregando = false;
			});

		return () => ctrl.abort();
	});

	function tentarNovamente(): void {
		recarregar += 1;
	}

	function limparFiltros(): void {
		busca = '';
		maquinaFiltro = '';
	}

	const opcoesMaquina = $derived([
		{ id: '', label: 'Todas as máquinas' },
		...maquinas.map((m) => ({ id: m.id, label: m.label }))
	]);

	const filtradas = $derived.by((): MaquinaGuias[] => {
		const termo = busca.trim().toLowerCase();
		return maquinas
			.filter((m) => !maquinaFiltro || m.id === maquinaFiltro)
			.map((m) => ({
				...m,
				guides: m.guides.filter(
					(g) =>
						!termo ||
						g.title.toLowerCase().includes(termo) ||
						(g.trilha ?? '').toLowerCase().includes(termo) ||
						g.author.toLowerCase().includes(termo)
				)
			}))
			.filter((m) => m.guides.length > 0);
	});

	const temFiltro = $derived(busca.trim() !== '' || maquinaFiltro !== '');
	const btnPrimario =
		'inline-flex items-center gap-1.5 rounded-md bg-brand px-3.5 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi disabled:opacity-50';
	const btnSecundario =
		'inline-flex items-center gap-1.5 rounded-md border border-border bg-elevated px-3.5 py-2 text-sm font-medium text-ink transition hover:bg-elevated/70';
</script>

<svelte:head>
	<title>Documentação — Treinamentos — FabLab</title>
</svelte:head>

<div class="space-y-5">
	<PageHeader title="Documentação" subtitle="Guias de operação por máquina (metadados).">
		{#snippet children()}
			{#if canEdit}
				<button
					type="button"
					onclick={() => toasts.info('Novo guia em breve.')}
					class={btnPrimario}
					aria-label="Novo guia"
				>
					<Icon name="plus" class="h-4 w-4" /> Novo guia
				</button>
			{/if}
		{/snippet}
	</PageHeader>

	<div class="flex flex-col gap-2 sm:flex-row">
		<SearchInput
			value={busca}
			placeholder="Buscar guia por título, trilha ou autor…"
			label="Buscar guia"
			delay={300}
			onSearch={(v) => (busca = v)}
			class="flex-1"
		/>
		<Select
			id="filtro-maquina"
			label=""
			options={opcoesMaquina}
			value={maquinaFiltro}
			placeholder="Todas as máquinas"
			onChange={(v) => (maquinaFiltro = v)}
		/>
	</div>

	{#if erro && !carregando}
		<ErrorBanner message="Não foi possível carregar os guias" hint="Verifique sua conexão e tente novamente. Se persistir, contate o suporte." onRetry={tentarNovamente} />
	{:else if carregando}
		<div class="grid gap-4 md:grid-cols-2" aria-hidden="true">
			{#each [0, 1, 2, 3] as i (i)}
				<div class="animate-pulse rounded-xl border border-border bg-surface p-5">
					<div class="h-5 w-1/2 rounded bg-muted/20"></div>
					<div class="mt-3 space-y-2">
						<div class="h-3 w-full rounded bg-muted/20"></div>
						<div class="h-3 w-2/3 rounded bg-muted/20"></div>
					</div>
				</div>
			{/each}
		</div>
	{:else if filtradas.length === 0}
		<div class="rounded-xl border border-border bg-surface">
			<EmptyState
				icon="document"
				title={temFiltro ? 'Nenhum guia para este filtro' : 'Nenhum guia cadastrado'}
				description={temFiltro
					? 'Ajuste a busca ou a máquina selecionada.'
					: 'Os guias de operação aparecerão aqui.'}
			>
				{#snippet children()}
					{#if temFiltro}
						<button type="button" onclick={limparFiltros} class={btnSecundario}>
							Limpar filtros
						</button>
					{/if}
				{/snippet}
			</EmptyState>
		</div>
	{:else}
		<div class="grid gap-4 md:grid-cols-2">
			{#each filtradas as m (m.id)}
				<section
					data-testid="guide-machine"
					aria-label="Guias de {m.label}"
					class="rounded-xl border border-border bg-surface p-5"
				>
					<div class="flex items-center gap-3">
						<span
							class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl border border-brand/30 bg-brand/10 text-brandhi"
							aria-hidden="true"
						>
							<Icon name="cube" class="h-5 w-5" />
						</span>
						<div class="min-w-0">
							<h2 class="truncate text-sm font-semibold text-ink">{m.label}</h2>
							<p class="text-xs text-muted">{m.guides.length} guia(s)</p>
						</div>
					</div>
					<ul class="mt-4 space-y-1">
						{#each m.guides as g (g.id)}
							{@const st = guideStatusMeta(g.status)}
							<li
								data-testid="guide-row"
								class="flex items-center gap-2 rounded-md px-2 py-2 transition hover:bg-elevated/60"
							>
								<div class="min-w-0 flex-1">
									<p class="truncate text-sm font-medium text-ink">{g.title}</p>
									<p class="mt-0.5 truncate text-xs text-muted">
										{g.trilha ?? 'Sem trilha'} · v{g.version} · {g.author}
									</p>
									<p class="mt-1 flex flex-wrap items-center gap-2">
										<StatusBadge label={st.label} color={st.color} />
										{#if g.updatedAt}
											<span class="text-[11px] text-muted">
												atualizado {relativeTime(g.updatedAt)}
											</span>
										{/if}
									</p>
								</div>
								<button
									type="button"
									onclick={() => (guiaAtivo = { guia: g, maquina: m.label })}
									aria-label="Ver guia {g.title}"
									class="inline-flex h-8 w-8 shrink-0 items-center justify-center rounded-md text-muted transition hover:bg-elevated hover:text-ink"
								>
									<Icon name="plus" class="h-4 w-4" />
								</button>
							</li>
						{/each}
					</ul>
				</section>
			{/each}
		</div>
	{/if}
</div>

<Modal
	open={guiaAtivo !== null}
	title={guiaAtivo?.guia.title ?? 'Guia'}
	subtitle={guiaAtivo ? `v${guiaAtivo.guia.version} · ${guiaAtivo.maquina}` : ''}
	onClose={() => (guiaAtivo = null)}
>
	{#snippet children()}
		{#if guiaAtivo}
			{@const st = guideStatusMeta(guiaAtivo.guia.status)}
			<div data-testid="modal-guia" class="space-y-4">
				<div class="flex flex-wrap gap-1.5" aria-label="Detalhes do guia">
					<StatusBadge label={st.label} color={st.color} />
					<span
						class="inline-flex items-center rounded-md bg-brand/10 px-2 py-0.5 text-xs font-medium whitespace-nowrap text-brandhi"
					>
						{guiaAtivo.maquina}
					</span>
					{#if guiaAtivo.guia.pages}
						<span
							class="inline-flex items-center rounded-md bg-muted/10 px-2 py-0.5 text-xs font-medium whitespace-nowrap text-muted"
						>
							{guiaAtivo.guia.pages} páginas
						</span>
					{/if}
					<span
						class="inline-flex items-center rounded-md bg-muted/10 px-2 py-0.5 text-xs font-medium whitespace-nowrap text-muted"
					>
						Material de apoio
					</span>
				</div>
				<div
					class="flex flex-col items-center justify-center gap-2 rounded-xl border border-dashed border-border bg-elevated/50 px-4 py-10 text-center"
				>
					<Icon name="document" class="h-8 w-8 text-muted" />
					<p class="text-sm font-medium text-ink">Prévia indisponível no MVP</p>
					<p class="max-w-xs text-xs text-muted">
						Conteúdo PDF/MDX fora de escopo — apenas metadados do guia.
					</p>
				</div>
				<div class="flex flex-col gap-2 sm:flex-row">
					<button
						type="button"
						onclick={() => toasts.info('Download do PDF em breve.')}
						class="{btnSecundario} flex-1 justify-center"
						aria-label="Baixar PDF de {guiaAtivo.guia.title}"
					>
						<Icon name="arrow-down-tray" class="h-4 w-4" /> Baixar PDF
					</button>
					<button
						type="button"
						onclick={() => toasts.info('Abertura do MDX em breve.')}
						class="{btnSecundario} flex-1 justify-center"
						aria-label="Abrir MDX de {guiaAtivo.guia.title}"
					>
						<Icon name="document" class="h-4 w-4" /> Abrir MDX
					</button>
					{#if canEdit}
						<button
							type="button"
							onclick={() => toasts.info('Edição de guia em breve.')}
							class="{btnSecundario} flex-1 justify-center"
							aria-label="Editar guia {guiaAtivo.guia.title}"
						>
							<Icon name="pencil" class="h-4 w-4" /> Editar
						</button>
					{/if}
				</div>
			</div>
		{/if}
	{/snippet}
</Modal>
