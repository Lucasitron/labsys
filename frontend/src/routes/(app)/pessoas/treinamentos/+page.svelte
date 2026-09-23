<script lang="ts">
	import type { PageProps } from './$types';
	import { listTreinamentos } from '$lib/api/rh/treinamentos';
	import type { PersonOption } from '$lib/api/rh';
	import type { Treinamento, TrainingStatus } from '$lib/types/rh';
	import { formatNumber } from '$lib/utils/format';
	import { toUserMessage } from '$lib/utils/errors';
	import { trainingStatusMeta } from '$lib/utils/rh-status';
	import { toasts } from '$lib/stores/toast';
	import PageHeader from '$lib/components/ui/PageHeader.svelte';
	import SearchInput from '$lib/components/ui/SearchInput.svelte';
	import StatusBadge from '$lib/components/ui/StatusBadge.svelte';
	import Avatar from '$lib/components/ui/Avatar.svelte';
	import DonutChart from '$lib/components/ui/DonutChart.svelte';
	import Modal from '$lib/components/ui/Modal.svelte';
	import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte';
	import EmptyState from '$lib/components/ui/EmptyState.svelte';
	import Pagination from '$lib/components/ui/Pagination.svelte';
	import Icon from '$lib/components/ui/Icon.svelte';
	import PeoplePicker from '$lib/components/estoque/PeoplePicker.svelte';

	let { data }: PageProps = $props();

	const canEdit = $derived(data.canEdit ?? false);
	const TAMANHO_PAGINA = 6;

	let treinamentos = $state<Treinamento[]>([]);
	let carregando = $state(true);
	let erro = $state<string | null>(null);
	let busca = $state('');
	let pagina = $state(1);
	let recarregar = $state(0);

	function fetchCancelavel(sinal: AbortSignal): typeof fetch {
		return (input, init) => {
			if (sinal.aborted) return Promise.reject(new DOMException('Abortada', 'AbortError'));
			return new Promise<Response>((resolve, reject) => {
				const aoAbortar = (): void => reject(new DOMException('Abortada', 'AbortError'));
				sinal.addEventListener('abort', aoAbortar, { once: true });
				fetch(input, init).then(
					(res) => {
						sinal.removeEventListener('abort', aoAbortar);
						resolve(res);
					},
					(err: unknown) => {
						sinal.removeEventListener('abort', aoAbortar);
						reject(err);
					}
				);
			});
		};
	}

	$effect(() => {
		const termo = busca.trim();
		void recarregar;

		const ctrl = new AbortController();
		const sinal = ctrl.signal;
		carregando = true;
		erro = null;

		void listTreinamentos(termo ? { search: termo } : {}, fetchCancelavel(sinal))
			.then((res) => {
				if (sinal.aborted) return;
				treinamentos = res.trainings;
			})
			.catch((err: unknown) => {
				if (sinal.aborted) return;
				erro = err instanceof Error ? err.message : 'Não foi possível carregar os treinamentos';
			})
			.finally(() => {
				if (!sinal.aborted) carregando = false;
			});

		return () => ctrl.abort();
	});

	function aoBuscar(valor: string): void {
		busca = valor;
		pagina = 1;
	}

	function tentarNovamente(): void {
		recarregar += 1;
	}

	const totalPaginas = $derived(Math.max(1, Math.ceil(treinamentos.length / TAMANHO_PAGINA)));
	const paginaSegura = $derived(Math.min(pagina, totalPaginas));
	const visiveis = $derived(
		treinamentos.slice((paginaSegura - 1) * TAMANHO_PAGINA, paginaSegura * TAMANHO_PAGINA)
	);

	interface GrupoDonut {
		rotulo: string;
		total: number;
		treinados: number;
		emTreinamento: number;
		pendentes: number;
	}

	const STATUS_ATIVOS: TrainingStatus[] = ['aberto', 'agendado', 'em_andamento'];

	const donuts = $derived.by((): GrupoDonut[] => {
		const grupos = new Map<string, Treinamento[]>();
		for (const t of treinamentos) {
			const rotulo = t.group?.label ?? 'Sem grupo';
			const lista = grupos.get(rotulo) ?? [];
			lista.push(t);
			grupos.set(rotulo, lista);
		}
		return [...grupos.entries()].map(([rotulo, lista]) => {
			const treinados = lista.filter((t) => t.status === 'concluido').length;
			const emTreinamento = lista.filter((t) => STATUS_ATIVOS.includes(t.status)).length;
			return {
				rotulo,
				total: lista.length,
				treinados,
				emTreinamento,
				pendentes: Math.max(0, lista.length - treinados - emTreinamento)
			};
		});
	});

	function progresso(t: Treinamento): number {
		const total = t.groupSize ?? 0;
		if (total <= 0) return 0;
		return Math.min(100, Math.round(((t.doneCount ?? 0) / total) * 100));
	}

	// ---- Modal novo treinamento (POST 🔴 D-9: sem endpoint no backend) ----

	let modal = $state(false);
	let titulo = $state('');
	let descricao = $state('');
	let instrutor = $state<PersonOption | null>(null);
	let maquina = $state('');
	let duracao = $state('');
	let trilha = $state('');
	let errosForm = $state<Record<string, string>>({});
	let chaveInstrutor = $state(0);

	function abrirModal(): void {
		titulo = '';
		descricao = '';
		instrutor = null;
		maquina = '';
		duracao = '';
		trilha = '';
		errosForm = {};
		chaveInstrutor += 1;
		modal = true;
	}

	function salvar(): void {
		const novos: Record<string, string> = {};
		if (!titulo.trim()) novos['titulo'] = 'Informe o título.';
		if (!instrutor) novos['instrutor'] = 'Selecione o(a) instrutor(a).';
		if (!maquina.trim()) novos['maquina'] = 'Informe a máquina.';
		errosForm = novos;
		if (Object.keys(novos).length > 0) return;
		toasts.warn('Criação indisponível: endpoint POST /api/rh/treinamentos pendente (D-9).');
	}

	const inputCls =
		'w-full rounded-md border border-border bg-elevated px-3 py-2.5 text-sm text-ink placeholder:text-muted/60 focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/30 transition';
	const inputErroCls = 'border-danger/60 focus:border-danger focus:ring-danger/30';
	const labelCls = 'mb-1 block text-xs font-medium text-muted';
	const btnPrimario =
		'inline-flex items-center gap-1.5 rounded-md bg-brand px-3.5 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi disabled:opacity-50';
	const btnSecundario =
		'inline-flex items-center gap-1.5 rounded-md border border-border bg-elevated px-3.5 py-2 text-sm font-medium text-ink transition hover:bg-elevated/70';
</script>

<svelte:head>
	<title>Treinamentos — Pessoas — FabLab</title>
</svelte:head>

<div class="space-y-5">
	<PageHeader title="Treinamentos" subtitle="Visão geral dos treinamentos e progresso por grupo.">
		{#snippet children()}
			<a href="/pessoas/treinamentos/agenda" class={btnSecundario} aria-label="Ver agenda de treinamentos">
				<Icon name="calendar" class="h-4 w-4" /> Ver agenda
			</a>
			{#if canEdit}
				<button type="button" onclick={abrirModal} class={btnPrimario} aria-label="Novo treinamento">
					<Icon name="plus" class="h-4 w-4" /> Novo treinamento
				</button>
			{/if}
		{/snippet}
	</PageHeader>

	<SearchInput
		value={busca}
		placeholder="Buscar treinamento por título…"
		label="Buscar treinamento"
		delay={300}
		onSearch={aoBuscar}
	/>

	{#if erro && !carregando}
		<ErrorBanner
			message="Não foi possível carregar os treinamentos"
			hint={erro}
			onRetry={tentarNovamente}
		/>
	{:else if carregando}
		<div class="grid gap-4 md:grid-cols-2" aria-hidden="true">
			{#each [0, 1, 2, 3] as i (i)}
				<div class="animate-pulse rounded-xl border border-border bg-surface p-5">
					<div class="h-4 w-2/3 rounded bg-muted/20"></div>
					<div class="mt-3 h-3 w-1/2 rounded bg-muted/20"></div>
					<div class="mt-4 h-2 w-full rounded bg-muted/20"></div>
				</div>
			{/each}
		</div>
		<div class="grid gap-4 md:grid-cols-2" aria-hidden="true">
			{#each [0, 1] as i (i)}
				<div class="animate-pulse rounded-xl border border-border bg-surface p-5">
					<div class="mx-auto h-28 w-28 rounded-full bg-muted/20"></div>
				</div>
			{/each}
		</div>
	{:else if treinamentos.length === 0}
		<div class="rounded-xl border border-border bg-surface">
			<EmptyState
				icon="squares"
				title={busca.trim() ? 'Nenhum treinamento encontrado' : 'Nenhum treinamento em aberto'}
				description={busca.trim()
					? `Sem resultados para “${busca.trim()}”.`
					: 'Crie o primeiro treinamento para começar.'}
			>
				{#snippet children()}
					{#if busca.trim()}
						<button type="button" onclick={() => aoBuscar('')} class={btnSecundario}>
							Limpar busca
						</button>
					{:else if canEdit}
						<button type="button" onclick={abrirModal} class={btnPrimario}>
							<Icon name="plus" class="h-4 w-4" /> Novo treinamento
						</button>
					{/if}
				{/snippet}
			</EmptyState>
		</div>
	{:else}
		<section aria-label="Treinamentos em aberto">
			<div class="grid gap-4 md:grid-cols-2">
				{#each visiveis as t (t.id)}
					{@const st = trainingStatusMeta(t.status)}
					<article
						data-testid="training-card"
						class="rounded-xl border border-border bg-surface p-5 transition hover:border-brand/40"
					>
						<div class="flex items-start justify-between gap-3">
							<a
								href="/pessoas/treinamentos/{t.id}"
								class="text-sm font-semibold text-ink transition hover:text-brandhi"
								aria-label="Ver detalhe de {t.title}"
							>
								{t.title}
							</a>
							<StatusBadge label={st.label} color={st.color} />
						</div>
						<div class="mt-2 flex items-center gap-2">
							<Avatar
								name={t.instructor.name}
								initialsOverride={t.instructor.initials}
								size="xs"
							/>
							<span class="text-xs text-muted">
								Instrutor(a): <span class="text-ink">{t.instructor.name}</span>
							</span>
						</div>
						<div class="mt-3 flex items-center justify-between text-xs text-muted">
							<span>{t.group?.label ?? 'Sem grupo'}</span>
							<span class="font-mono tabular-nums">
								{formatNumber(t.doneCount ?? 0)} concluíram
							</span>
						</div>
						<div
							class="mt-1.5 h-1.5 overflow-hidden rounded-full bg-elevated"
							role="progressbar"
							aria-valuenow={progresso(t)}
							aria-valuemin={0}
							aria-valuemax={100}
							aria-label="Progresso de {t.title}"
						>
							<div class="h-full rounded-full bg-brand transition" style:width="{progresso(t)}%">
							</div>
						</div>
						{#if t.machine}
							<p class="mt-2 font-mono text-xs text-muted">{t.machine}</p>
						{/if}
					</article>
				{/each}
			</div>
			{#if treinamentos.length > TAMANHO_PAGINA}
				<div class="mt-4">
					<Pagination
						page={paginaSegura}
						totalPages={totalPaginas}
						totalItems={treinamentos.length}
						label="treinamento(s)"
						onPage={(p) => (pagina = p)}
					/>
				</div>
			{/if}
		</section>

		<section aria-label="Progresso por grupo" class="space-y-3">
			<h2 class="text-sm font-semibold text-ink">Progresso por grupo</h2>
			<div class="grid gap-4 md:grid-cols-2">
				{#each donuts as g (g.rotulo)}
					<div data-testid="donut-group" class="rounded-xl border border-border bg-surface p-5">
						<p class="mb-3 text-sm font-medium text-ink">{g.rotulo}</p>
						<DonutChart
							total={g.total}
							label="treinamentos"
							segments={[
								{ value: g.treinados, color: 'success', label: 'Treinados' },
								{ value: g.emTreinamento, color: 'brand', label: 'Em treinamento' },
								{ value: g.pendentes, color: 'warn', label: 'Pendentes' }
							]}
						/>
					</div>
				{/each}
			</div>
		</section>
	{/if}
</div>

<Modal
	open={modal}
	title="Novo treinamento"
	subtitle="Cadastro de treinamento ministrado por instrutor(a)."
	onClose={() => (modal = false)}
>
	{#snippet children()}
		<div data-testid="modal-treinamento" class="space-y-4">
			<div>
				<label for="tre-titulo" class={labelCls}>
					Título <span class="text-danger" aria-hidden="true">*</span>
				</label>
				<input
					id="tre-titulo"
					type="text"
					bind:value={titulo}
					placeholder="Ex.: Operação da impressora 3D"
					aria-invalid={!!errosForm['titulo']}
					class="{inputCls} {errosForm['titulo'] ? inputErroCls : ''}"
				/>
				{#if errosForm['titulo']}<p class="mt-1 text-xs text-danger">{errosForm['titulo']}</p>{/if}
			</div>
			<div>
				<label for="tre-descricao" class={labelCls}>Descrição</label>
				<textarea
					id="tre-descricao"
					bind:value={descricao}
					rows="2"
					placeholder="Resumo do conteúdo abordado…"
					class="{inputCls} resize-y"
				></textarea>
			</div>
			<div>
				<span id="tre-instrutor-label" class={labelCls}>
					Instrutor(a) <span class="text-danger" aria-hidden="true">*</span>
				</span>
				{#key chaveInstrutor}
					<PeoplePicker
						selected={instrutor}
						onSelect={(p) => {
							instrutor = p;
							errosForm = { ...errosForm, instrutor: '' };
						}}
						placeholder="Buscar instrutor(a) por nome…"
					/>
				{/key}
				{#if errosForm['instrutor']}
					<p class="mt-1 text-xs text-danger">{errosForm['instrutor']}</p>
				{/if}
			</div>
			<div class="grid grid-cols-2 gap-4">
				<div>
					<label for="tre-maquina" class={labelCls}>
						Máquina <span class="text-danger" aria-hidden="true">*</span>
					</label>
					<input
						id="tre-maquina"
						type="text"
						bind:value={maquina}
						placeholder="Ex.: Impressora 3D"
						aria-invalid={!!errosForm['maquina']}
						class="{inputCls} {errosForm['maquina'] ? inputErroCls : ''}"
					/>
					{#if errosForm['maquina']}
						<p class="mt-1 text-xs text-danger">{errosForm['maquina']}</p>
					{/if}
				</div>
				<div>
					<label for="tre-duracao" class={labelCls}>Duração</label>
					<input
						id="tre-duracao"
						type="text"
						bind:value={duracao}
						placeholder="Ex.: 8h"
						class={inputCls}
					/>
				</div>
			</div>
			<div>
				<label for="tre-trilha" class={labelCls}>Trilha</label>
				<input
					id="tre-trilha"
					type="text"
					bind:value={trilha}
					placeholder="Ex.: Manufatura aditiva"
					class={inputCls}
				/>
			</div>
		</div>
	{/snippet}
	{#snippet footer()}
		<button
			type="button"
			onclick={() => (modal = false)}
			class="rounded-md border border-border bg-surface px-4 py-2 text-sm font-medium text-ink transition hover:bg-elevated"
		>
			Cancelar
		</button>
		<button
			type="button"
			onclick={salvar}
			class="rounded-md bg-brand px-4 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi"
		>
			Salvar treinamento
		</button>
	{/snippet}
</Modal>
