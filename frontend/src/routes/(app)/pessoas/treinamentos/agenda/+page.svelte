<script lang="ts">
	import { goto } from '$app/navigation';
	import { page } from '$app/state';
	import type { PageProps } from './$types';
	import { getDisponibilidade, listAgenda } from '$lib/api/rh/treinamentos';
	import type {
		InstrutorDisponibilidade,
		Treinamento,
		TrainingStatus
	} from '$lib/types/rh';
	import { disponibilidadeMeta, trainingStatusMeta } from '$lib/utils/rh-status';
	import { toasts } from '$lib/stores/toast';
	import PageHeader from '$lib/components/ui/PageHeader.svelte';
	import StatusBadge from '$lib/components/ui/StatusBadge.svelte';
	import TypeBadge from '$lib/components/ui/TypeBadge.svelte';
	import Avatar from '$lib/components/ui/Avatar.svelte';
	import Modal from '$lib/components/ui/Modal.svelte';
	import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte';
	import EmptyState from '$lib/components/ui/EmptyState.svelte';
	import TableSkeleton from '$lib/components/ui/TableSkeleton.svelte';
	import Icon from '$lib/components/ui/Icon.svelte';

	let { data }: PageProps = $props();

	const canEdit = $derived(data.canEdit ?? false);
	const ABAS = ['todos', 'pendentes', 'solicitados', 'agendados'] as const;
	type Aba = (typeof ABAS)[number];
	const ABA_TITULO: Record<Aba, string> = {
		todos: 'Todos',
		pendentes: 'Pendentes',
		solicitados: 'Solicitados',
		agendados: 'Agendados'
	};

	const query = $derived(page.url.searchParams);
	const abaAtiva = $derived.by((): Aba => {
		const bruto = (query.get('tab') ?? '').trim();
		return (ABAS as readonly string[]).includes(bruto) ? (bruto as Aba) : 'todos';
	});

	let itens = $state<Treinamento[]>([]);
	let disponibilidade = $state<InstrutorDisponibilidade[]>([]);
	let carregando = $state(true);
	let carregandoDisp = $state(true);
	let erro = $state<string | null>(null);
	let erroDisp = $state<string | null>(null);
	let recarregar = $state(0);

	$effect(() => {
		void recarregar;
		const ctrl = new AbortController();
		const sinal = ctrl.signal;
		carregando = true;
		erro = null;

		void listAgenda({}, fetch)
			.then((res) => {
				if (sinal.aborted) return;
				itens = res.trainings;
			})
			.catch((err: unknown) => {
				if (sinal.aborted) return;
				erro = err instanceof Error ? err.message : 'Não foi possível carregar a agenda';
			})
			.finally(() => {
				if (!sinal.aborted) carregando = false;
			});

		carregandoDisp = true;
		erroDisp = null;
		void getDisponibilidade(fetch)
			.then((res) => {
				if (!sinal.aborted) disponibilidade = res;
			})
			.catch((err: unknown) => {
				if (sinal.aborted) return;
				erroDisp =
					err instanceof Error ? err.message : 'Não foi possível carregar a disponibilidade';
			})
			.finally(() => {
				if (!sinal.aborted) carregandoDisp = false;
			});

		return () => ctrl.abort();
	});

	function navegarAba(aba: Aba): void {
		const params = new URLSearchParams();
		if (aba !== 'todos') params.set('tab', aba);
		const qs = params.toString();
		void goto(`/pessoas/treinamentos/agenda${qs ? `?${qs}` : ''}`, { keepFocus: true });
	}

	function tentarNovamente(): void {
		recarregar += 1;
	}

	const STATUS_ABA: Record<Exclude<Aba, 'todos'>, TrainingStatus[]> = {
		pendentes: ['pendente'],
		solicitados: ['solicitado'],
		agendados: ['agendado', 'em_andamento']
	};

	const contagens = $derived({
		pendentes: itens.filter((t) => t.status === 'pendente').length,
		solicitados: itens.filter((t) => t.status === 'solicitado').length,
		agendados: itens.filter((t) => t.status === 'agendado').length,
		emAndamento: itens.filter((t) => t.status === 'em_andamento').length
	});

	const filtrados = $derived(
		abaAtiva === 'todos' ? itens : itens.filter((t) => STATUS_ABA[abaAtiva].includes(t.status))
	);

	function emBreve(_acao: string): void {
		toasts.warn('Ação indisponível: endpoint de agenda pendente (D-9).');
	}

	// ---- Modal agendar (POST 🔴 D-9: sem endpoint no backend) ----

	let modal = $state(false);
	let fTreinamento = $state('');
	let fGrupo = $state('');
	let fInstrutor = $state('');
	let fMaquina = $state('');
	let fData = $state('');
	let fInicio = $state('');
	let fFim = $state('');
	let errosForm = $state<Record<string, string>>({});

	function abrirModal(): void {
		fTreinamento = '';
		fGrupo = '';
		fInstrutor = '';
		fMaquina = '';
		fData = '';
		fInicio = '';
		fFim = '';
		errosForm = {};
		modal = true;
	}

	const disponibilidadeOk = $derived(
		fInstrutor.trim().length > 1 && fData !== '' && fInicio !== '' && fFim !== ''
	);

	function formatarDataLonga(iso: string): string {
		const data = new Date(`${iso}T12:00:00`);
		if (Number.isNaN(data.getTime())) return iso;
		return new Intl.DateTimeFormat('pt-BR', {
			day: '2-digit',
			month: '2-digit',
			year: 'numeric'
		}).format(data);
	}

	function salvar(): void {
		const novos: Record<string, string> = {};
		if (!fTreinamento.trim()) novos['treinamento'] = 'Informe o treinamento.';
		if (!fGrupo.trim()) novos['grupo'] = 'Informe o grupo.';
		if (!fInstrutor.trim()) novos['instrutor'] = 'Informe o(a) instrutor(a).';
		if (!fMaquina.trim()) novos['maquina'] = 'Informe a máquina.';
		if (!fData) novos['data'] = 'Informe a data.';
		if (!fInicio) novos['inicio'] = 'Informe o início.';
		if (!fFim) novos['fim'] = 'Informe o fim.';
		errosForm = novos;
		if (Object.keys(novos).length > 0) return;
		toasts.warn('Agendamento indisponível: endpoint de agenda pendente (D-9).');
	}

	const inputCls =
		'w-full rounded-md border border-border bg-elevated px-3 py-2.5 text-sm text-ink placeholder:text-muted/60 focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/30 transition';
	const inputErroCls = 'border-danger/60 focus:border-danger focus:ring-danger/30';
	const labelCls = 'mb-1 block text-xs font-medium text-muted';
	const btnPrimario =
		'inline-flex items-center gap-1.5 rounded-md bg-brand px-3.5 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi disabled:opacity-50';
</script>

<svelte:head>
	<title>Agenda — Treinamentos — FabLab</title>
</svelte:head>

<div class="space-y-5">
	<PageHeader title="Agenda de treinamentos" subtitle="Solicitações, agendamentos e disponibilidade.">
		{#snippet children()}
			{#if canEdit}
				<button type="button" onclick={abrirModal} class={btnPrimario} aria-label="Agendar treinamento">
					<Icon name="plus" class="h-4 w-4" /> Agendar
				</button>
			{/if}
		{/snippet}
	</PageHeader>

	<section aria-label="Resumo da agenda" class="grid grid-cols-2 gap-3 lg:grid-cols-4">
		<div class="rounded-xl border border-border bg-elevated p-4">
			<p class="text-xs text-muted">Pendentes</p>
			<p class="mt-1 font-mono text-2xl font-semibold text-ink tabular-nums">
				{contagens.pendentes}
			</p>
		</div>
		<div class="rounded-xl border border-border bg-elevated p-4">
			<p class="text-xs text-muted">Solicitados</p>
			<p class="mt-1 font-mono text-2xl font-semibold text-ink tabular-nums">
				{contagens.solicitados}
			</p>
		</div>
		<div class="rounded-xl border border-border bg-elevated p-4">
			<p class="text-xs text-muted">Agendados</p>
			<p class="mt-1 font-mono text-2xl font-semibold text-ink tabular-nums">
				{contagens.agendados}
			</p>
		</div>
		<div class="rounded-xl border border-border bg-elevated p-4">
			<p class="text-xs text-muted">Em andamento</p>
			<p class="mt-1 font-mono text-2xl font-semibold text-ink tabular-nums">
				{contagens.emAndamento}
			</p>
		</div>
	</section>

	<div role="tablist" aria-label="Situação da agenda" class="flex flex-wrap items-center gap-1.5">
		{#each ABAS as aba (aba)}
			{@const count =
				aba === 'todos'
					? itens.length
					: aba === 'pendentes'
						? contagens.pendentes
						: aba === 'solicitados'
							? contagens.solicitados
							: contagens.agendados + contagens.emAndamento}
			<button
				type="button"
				role="tab"
				aria-selected={abaAtiva === aba}
				data-testid={aba === 'solicitados' ? 'agenda-tab-solicitados' : `agenda-tab-${aba}`}
				onclick={() => navegarAba(aba)}
				aria-label="Filtrar agenda: {ABA_TITULO[aba]} ({count})"
				class="inline-flex items-center gap-1.5 rounded-md px-3 py-1.5 text-sm font-medium transition {abaAtiva ===
				aba
					? 'bg-brand/15 text-brandhi'
					: 'text-muted hover:text-ink'}"
			>
				{ABA_TITULO[aba]}
				<span
					class="inline-flex h-4 min-w-4 items-center justify-center rounded-full px-1 text-[10px] font-bold {abaAtiva ===
					aba
						? 'bg-brand text-white'
						: 'bg-muted/15 text-muted'}"
				>
					{count}
				</span>
			</button>
		{/each}
	</div>

	{#if erro && !carregando}
		<ErrorBanner message="Não foi possível carregar a agenda" hint="Verifique sua conexão e tente novamente. Se persistir, contate o suporte." onRetry={tentarNovamente} />
	{:else if carregando}
		<div class="rounded-xl border border-border bg-surface p-4">
			<TableSkeleton rows={5} columns={6} />
		</div>
	{:else if filtrados.length === 0}
		<div class="rounded-xl border border-border bg-surface">
			<EmptyState
				icon="calendar"
				title={`Nenhum item ${ABA_TITULO[abaAtiva].toLowerCase()}`}
				description="Ajuste o filtro ou agende um novo treinamento."
			/>
		</div>
	{:else}
		<section aria-label={`Agenda ${ABA_TITULO[abaAtiva].toLowerCase()}`}>
			<div class="overflow-hidden rounded-xl border border-border bg-surface">
				<div class="overflow-x-auto">
					<table class="w-full min-w-[900px] text-sm">
						<thead>
							<tr
								class="border-b border-border bg-elevated/50 text-left text-[11px] uppercase tracking-wide text-muted"
							>
								<th class="px-4 py-2.5 font-medium">Treinamento</th>
								<th class="px-4 py-2.5 font-medium">Grupo</th>
								<th class="px-4 py-2.5 font-medium">Instrutor(a)</th>
								<th class="px-4 py-2.5 font-medium">Máquina</th>
								<th class="px-4 py-2.5 font-medium">Data/Horário</th>
								<th class="px-4 py-2.5 font-medium">Status</th>
								<th class="w-44 px-4 py-2.5 text-right font-medium">
									<span class="sr-only">Ação</span>
								</th>
							</tr>
						</thead>
						<tbody>
							{#each filtrados as t (t.id)}
								{@const st = trainingStatusMeta(t.status)}
								<tr class="border-b border-border transition last:border-0 hover:bg-elevated/40">
									<td class="px-4 py-3">
										<a
											href="/pessoas/treinamentos/{t.id}"
											class="font-medium text-ink transition hover:text-brandhi"
										>
											{t.title}
										</a>
									</td>
									<td class="px-4 py-3 text-muted">{t.group?.label ?? '—'}</td>
									<td class="px-4 py-3">
										<div class="flex items-center gap-2">
											<Avatar
												name={t.instructor.name}
												initialsOverride={t.instructor.initials}
												size="xs"
											/>
											<span class="text-ink">{t.instructor.name}</span>
										</div>
									</td>
									<td class="px-4 py-3 font-mono text-xs text-muted">{t.machine ?? '—'}</td>
									<td class="px-4 py-3 font-mono text-xs text-muted tabular-nums">—</td>
									<td class="px-4 py-3">
										<StatusBadge label={st.label} color={st.color} />
									</td>
									<td class="px-4 py-3">
										<div class="flex items-center justify-end gap-1.5">
											{#if t.status === 'em_andamento' || t.status === 'agendado'}
												<a
													href="/pessoas/treinamentos/sessao"
													class="rounded-md border border-brand/40 bg-brand/10 px-2.5 py-1.5 text-xs font-medium text-brandhi transition hover:bg-brand/20"
													aria-label="Abrir sessão de {t.title}"
												>
													Abrir sessão
												</a>
											{:else if t.status === 'solicitado' && canEdit}
												<button
													type="button"
													onclick={() => emBreve('Confirmação')}
													aria-label="Confirmar {t.title}"
													class="rounded-md border border-success/40 bg-success/10 px-2.5 py-1.5 text-xs font-medium text-success transition hover:bg-success/20"
												>
													Confirmar
												</button>
											{:else if t.status === 'pendente' && canEdit}
												<button
													type="button"
													onclick={() => emBreve('Definição de turma')}
													aria-label="Definir turma de {t.title}"
													class="rounded-md border border-border bg-elevated px-2.5 py-1.5 text-xs font-medium text-ink transition hover:bg-elevated/70"
												>
													Definir turma
												</button>
											{/if}
										</div>
									</td>
								</tr>
							{/each}
						</tbody>
					</table>
				</div>
			</div>
		</section>
	{/if}

	<section aria-label="Disponibilidade dos instrutores" class="space-y-3">
		<h2 class="text-sm font-semibold text-ink">Disponibilidade dos instrutores</h2>
		{#if erroDisp && !carregandoDisp}
			<ErrorBanner
				message="Não foi possível carregar a disponibilidade"
				hint="Verifique sua conexão e tente novamente. Se persistir, contate o suporte."
				onRetry={tentarNovamente}
			/>
		{:else if carregandoDisp}
			<div class="rounded-xl border border-border bg-surface p-4">
				<TableSkeleton rows={3} columns={4} />
			</div>
		{:else if disponibilidade.length === 0}
			<div class="rounded-xl border border-border bg-surface">
				<EmptyState
					icon="users"
					title="Sem dados de disponibilidade"
					description="Nenhum(a) instrutor(a) com disponibilidade informada."
				/>
			</div>
		{:else}
			<ul class="grid gap-3 md:grid-cols-2">
				{#each disponibilidade as d (d.id)}
					{@const meta = disponibilidadeMeta(d.disponibilidade)}
					<li
						data-testid="agenda-instrutor"
						class="rounded-xl border border-border bg-surface p-4"
					>
						<div class="flex items-center justify-between gap-2">
							<div class="flex min-w-0 items-center gap-2.5">
								<Avatar name={d.name} size="sm" />
								<div class="min-w-0">
									<p class="truncate text-sm font-medium text-ink">{d.name}</p>
									{#if d.matricula}
										<p class="font-mono text-xs text-muted">{d.matricula}</p>
									{/if}
								</div>
							</div>
							<StatusBadge label={meta.label} color={meta.color} />
						</div>
						{#if d.maquinas && d.maquinas.length > 0}
							<div class="mt-3 flex flex-wrap gap-1.5" aria-label="Máquinas habilitadas">
								{#each d.maquinas as m (m)}
									<TypeBadge tone="brand" label={m} />
								{/each}
							</div>
						{/if}
						<p class="mt-3 font-mono text-xs text-muted tabular-nums">
							Tarefas hoje: {(d.tarefasHojeHoras ?? 0).toString().replace('.', ',')}h
						</p>
						{#if d.nota}
							<p class="mt-1 text-xs text-muted">{d.nota}</p>
						{/if}
					</li>
				{/each}
			</ul>
			<p class="text-xs text-muted">
				A disponibilidade consome o tempo das tarefas de produção, encomendas e projetos — nunca
				triagens do processo seletivo (responsabilidade do tutor).
			</p>
		{/if}
	</section>
</div>

<Modal
	open={modal}
	title="Agendar treinamento"
	subtitle="Sessão ministrada por instrutor(a)."
	onClose={() => (modal = false)}
>
	{#snippet children()}
		<div data-testid="modal-agendar" class="space-y-4">
			<div>
				<label for="age-treinamento" class={labelCls}>
					Treinamento <span class="text-danger" aria-hidden="true">*</span>
				</label>
				<input
					id="age-treinamento"
					type="text"
					bind:value={fTreinamento}
					placeholder="Ex.: Operação da impressora 3D"
					aria-invalid={!!errosForm['treinamento']}
					class="{inputCls} {errosForm['treinamento'] ? inputErroCls : ''}"
				/>
				{#if errosForm['treinamento']}
					<p class="mt-1 text-xs text-danger">{errosForm['treinamento']}</p>
				{/if}
			</div>
			<div class="grid grid-cols-2 gap-4">
				<div>
					<label for="age-grupo" class={labelCls}>
						Grupo <span class="text-danger" aria-hidden="true">*</span>
					</label>
					<input
						id="age-grupo"
						type="text"
						bind:value={fGrupo}
						placeholder="Ex.: G-01"
						aria-invalid={!!errosForm['grupo']}
						class="{inputCls} {errosForm['grupo'] ? inputErroCls : ''}"
					/>
					{#if errosForm['grupo']}<p class="mt-1 text-xs text-danger">{errosForm['grupo']}</p>{/if}
				</div>
				<div>
					<label for="age-instrutor" class={labelCls}>
						Instrutor(a) <span class="text-danger" aria-hidden="true">*</span>
					</label>
					<input
						id="age-instrutor"
						type="text"
						bind:value={fInstrutor}
						placeholder="Ex.: Pedro Santos"
						aria-invalid={!!errosForm['instrutor']}
						class="{inputCls} {errosForm['instrutor'] ? inputErroCls : ''}"
					/>
					{#if errosForm['instrutor']}
						<p class="mt-1 text-xs text-danger">{errosForm['instrutor']}</p>
					{/if}
				</div>
			</div>
			<div>
				<label for="age-maquina" class={labelCls}>
					Máquina <span class="text-danger" aria-hidden="true">*</span>
				</label>
				<input
					id="age-maquina"
					type="text"
					bind:value={fMaquina}
					placeholder="Ex.: Impressora 3D"
					aria-invalid={!!errosForm['maquina']}
					class="{inputCls} {errosForm['maquina'] ? inputErroCls : ''}"
				/>
				{#if errosForm['maquina']}<p class="mt-1 text-xs text-danger">{errosForm['maquina']}</p>{/if}
			</div>
			<div class="grid grid-cols-3 gap-4">
				<div>
					<label for="age-data" class={labelCls}>
						Data <span class="text-danger" aria-hidden="true">*</span>
					</label>
					<input
						id="age-data"
						type="date"
						bind:value={fData}
						aria-invalid={!!errosForm['data']}
						class="{inputCls} font-mono tabular-nums {errosForm['data'] ? inputErroCls : ''}"
					/>
					{#if errosForm['data']}<p class="mt-1 text-xs text-danger">{errosForm['data']}</p>{/if}
				</div>
				<div>
					<label for="age-inicio" class={labelCls}>
						Início <span class="text-danger" aria-hidden="true">*</span>
					</label>
					<input
						id="age-inicio"
						type="time"
						bind:value={fInicio}
						aria-invalid={!!errosForm['inicio']}
						class="{inputCls} font-mono tabular-nums {errosForm['inicio'] ? inputErroCls : ''}"
					/>
					{#if errosForm['inicio']}<p class="mt-1 text-xs text-danger">{errosForm['inicio']}</p>{/if}
				</div>
				<div>
					<label for="age-fim" class={labelCls}>
						Fim <span class="text-danger" aria-hidden="true">*</span>
					</label>
					<input
						id="age-fim"
						type="time"
						bind:value={fFim}
						aria-invalid={!!errosForm['fim']}
						class="{inputCls} font-mono tabular-nums {errosForm['fim'] ? inputErroCls : ''}"
					/>
					{#if errosForm['fim']}<p class="mt-1 text-xs text-danger">{errosForm['fim']}</p>{/if}
				</div>
			</div>
			{#if disponibilidadeOk}
				<p
					role="status"
					class="rounded-md border border-success/30 bg-success/10 px-3 py-2.5 text-xs text-success"
				>
					{fInstrutor.trim()} está livre em {formatarDataLonga(fData)} · {fInicio}–{fFim}.
				</p>
			{/if}
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
			Confirmar agendamento
		</button>
	{/snippet}
</Modal>
