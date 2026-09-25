<script lang="ts">
	import { goto } from '$app/navigation';
	import { page } from '$app/state';
	import { get } from 'svelte/store';
	import type { PageProps } from './$types';
	import { auth } from '$lib/stores/auth';
	import { ApiError } from '$lib/api/client';
	import { createPessoa } from '$lib/api/rh/pessoas';
	import {
		avaliarMembro,
		createGrupo,
		listPS,
		moverEstagio
	} from '$lib/api/rh/processo-seletivo';
	import type { PersonOption } from '$lib/api/rh';
	import type { PsCandidate, PsGroup, PsStage } from '$lib/types/rh';
	import { initials, formatNumber } from '$lib/utils/format';
	import { toUserMessage } from '$lib/utils/errors';
	import { psStageMeta } from '$lib/utils/rh-status';
	import { isTutor } from '$lib/utils/permissions';
	import { toasts } from '$lib/stores/toast';
	import PageHeader from '$lib/components/ui/PageHeader.svelte';
	import SearchInput from '$lib/components/ui/SearchInput.svelte';
	import Select from '$lib/components/ui/Select.svelte';
	import KanbanBoard from '$lib/components/ui/KanbanBoard.svelte';
	import StatusBadge from '$lib/components/ui/StatusBadge.svelte';
	import Avatar from '$lib/components/ui/Avatar.svelte';
	import Modal from '$lib/components/ui/Modal.svelte';
	import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte';
	import EmptyState from '$lib/components/ui/EmptyState.svelte';
	import TableSkeleton from '$lib/components/ui/TableSkeleton.svelte';
	import Icon from '$lib/components/ui/Icon.svelte';
	import PeoplePicker from '$lib/components/estoque/PeoplePicker.svelte';

	let { data }: PageProps = $props();

	const canEdit = $derived(data.canEdit ?? false);
	const usuario = $derived(get(auth).user);
	const podeGerir = $derived(canEdit || isTutor(usuario));

	const ESTAGIOS: PsStage[] = ['inscrito', 'triagem', 'entrevista', 'aprovado', 'reprovado'];
	const TONE_COLUNA: Record<PsStage, 'muted' | 'brand' | 'warn' | 'success' | 'danger'> = {
		inscrito: 'muted',
		triagem: 'brand',
		entrevista: 'warn',
		aprovado: 'success',
		reprovado: 'danger'
	};

	const query = $derived(page.url.searchParams);

	const filtroEstagio = $derived.by((): PsStage | '' => {
		const bruto = (query.get('estagio') ?? '').trim();
		return (ESTAGIOS as string[]).includes(bruto) ? (bruto as PsStage) : '';
	});

	const ESTAGIO_OPCOES = [
		{ id: '', label: 'Todos os estágios' },
		...ESTAGIOS.map((id) => ({ id, label: psStageMeta(id).label }))
	];

	let grupos = $state<PsGroup[]>([]);
	let contagens = $state({ inscritos: 0, grupos: 0, em_avaliacao: 0, aprovados: 0 });
	let carregando = $state(true);
	let erro = $state<string | null>(null);
	let busca = $state('');
	let recarregar = $state(0);
	let mutando = $state(false);

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
		const estagio = filtroEstagio;
		void recarregar;

		const ctrl = new AbortController();
		const sinal = ctrl.signal;
		carregando = true;
		erro = null;

		void listPS(estagio ? { estagio } : {}, fetchCancelavel(sinal))
			.then((res) => {
				if (sinal.aborted) return;
				grupos = res.groups;
				contagens = res.counts;
			})
			.catch((err: unknown) => {
				if (sinal.aborted) return;
				erro = err instanceof Error ? err.message : 'Não foi possível carregar o processo seletivo';
			})
			.finally(() => {
				if (!sinal.aborted) carregando = false;
			});

		return () => ctrl.abort();
	});

	function navegarEstagio(estagio: PsStage | ''): void {
		const params = new URLSearchParams();
		if (estagio) params.set('estagio', estagio);
		const qs = params.toString();
		void goto(`/pessoas/processo-seletivo${qs ? `?${qs}` : ''}`, { keepFocus: true });
	}

	function tentarNovamente(): void {
		recarregar += 1;
	}

	function atualizar(): void {
		recarregar += 1;
	}

	const termo = $derived(busca.trim().toLowerCase());

	const gruposFiltrados = $derived(
		grupos.filter((grupo) => {
			if (!termo) return true;
			const emGrupo =
				grupo.name.toLowerCase().includes(termo) || grupo.code.toLowerCase().includes(termo);
			const emEtapa = psStageMeta(grupo.stage).label.toLowerCase().includes(termo);
			const emCandidato = grupo.candidates.some((c) => c.name.toLowerCase().includes(termo));
			return emGrupo || emEtapa || emCandidato;
		})
	);

	const colunas = $derived(
		ESTAGIOS.map((estagio) => {
			const meta = psStageMeta(estagio);
			const deEtapa = gruposFiltrados.filter((g) => g.stage === estagio);
			return {
				id: estagio,
				label: meta.label,
				tone: TONE_COLUNA[estagio],
				count: deEtapa.length,
				cards: deEtapa.map((g) => ({
					id: g.id,
					title: `${g.code} · ${g.name}`,
					description: `${g.membersCount} ${g.membersCount === 1 ? 'membro' : 'membros'}`
				}))
			};
		})
	);

	function grupoPorId(id: string): PsGroup | undefined {
		return gruposFiltrados.find((g) => g.id === id);
	}

	async function mover(grupo: PsGroup, destino: PsStage): Promise<void> {
		if (mutando || grupo.stage === destino) return;
		mutando = true;
		try {
			await moverEstagio(grupo.id, destino);
			toasts.success(`${grupo.code} movido para ${psStageMeta(destino).label}.`);
			if (grupoAtivo?.id === grupo.id) {
				grupoAtivo = { ...grupoAtivo, stage: destino };
			}
			atualizar();
		} catch (err) {
			toasts.danger(toUserMessage(err).message);
		} finally {
			mutando = false;
		}
	}

	// ---- Modais ----

	let modalInscricao = $state(false);
	let nomeInsc = $state('');
	let emailInsc = $state('');
	let cursoInsc = $state('');
	let periodoInsc = $state('');
	let dispInsc = $state('');
	let errosInsc = $state<Record<string, string>>({});

	let modalCriarGrupo = $state(false);
	let nomeGrupo = $state('');
	let tutorSel = $state<PersonOption | null>(null);
	let membrosSel = $state<PersonOption[]>([]);
	let erroGrupo = $state<string | null>(null);
	let chavePickers = $state(0);

	let grupoAtivo = $state<PsGroup | null>(null);

	let alvoAval = $state<{ grupoId: string; candidato: PsCandidate } | null>(null);
	let resultadoAval = $state<'aprovado' | 'reprovado'>('aprovado');
	let notaAval = $state('');
	let feedbackAval = $state('');
	let erroAval = $state<string | null>(null);

	function abrirInscricao(): void {
		nomeInsc = '';
		emailInsc = '';
		cursoInsc = '';
		periodoInsc = '';
		dispInsc = '';
		errosInsc = {};
		modalInscricao = true;
	}

	function abrirCriarGrupo(): void {
		nomeGrupo = '';
		tutorSel = null;
		membrosSel = [];
		erroGrupo = null;
		chavePickers += 1;
		modalCriarGrupo = true;
	}

	function abrirGrupo(grupo: PsGroup): void {
		grupoAtivo = grupo;
	}

	function abrirAvaliar(grupoId: string, candidato: PsCandidate): void {
		alvoAval = { grupoId, candidato };
		resultadoAval = 'aprovado';
		notaAval = candidato.nota !== null && candidato.nota !== undefined ? String(candidato.nota) : '';
		feedbackAval = candidato.feedback ?? '';
		erroAval = null;
	}

	async function salvarInscricao(): Promise<void> {
		if (mutando) return;
		const novos: Record<string, string> = {};
		if (!nomeInsc.trim()) novos['nome'] = 'Informe o nome completo.';
		if (!emailInsc.trim()) novos['email'] = 'Informe o e-mail.';
		else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(emailInsc.trim()))
			novos['email'] = 'Informe um e-mail válido.';
		if (!cursoInsc.trim()) novos['curso'] = 'Informe o curso.';
		errosInsc = novos;
		if (Object.keys(novos).length > 0) return;
		mutando = true;
		try {
			await createPessoa({
				name: nomeInsc.trim(),
				email: emailInsc.trim(),
				type: 'voluntario',
				nivel: 'recrutando',
				...(cursoInsc.trim() ? { cursoTurma: cursoInsc.trim() } : {}),
				...(periodoInsc.trim() ? { periodo: periodoInsc.trim() } : {}),
				...(dispInsc.trim() ? { disponibilidades: dispInsc.trim() } : {})
			});
			toasts.success('Inscrição registrada com sucesso.');
			modalInscricao = false;
			atualizar();
		} catch (err) {
			if (err instanceof ApiError && err.status === 409) {
				errosInsc = { ...errosInsc, email: 'Este e-mail já está em uso.' };
			} else {
				toasts.danger(toUserMessage(err).message);
			}
		} finally {
			mutando = false;
		}
	}

	function adicionarMembro(pessoa: PersonOption | null): void {
		if (!pessoa) return;
		if (tutorSel?.id === pessoa.id) {
			erroGrupo = 'O tutor já está vinculado ao grupo.';
			return;
		}
		if (!membrosSel.some((m) => m.id === pessoa.id)) {
			membrosSel = [...membrosSel, pessoa];
		}
		chavePickers += 1;
		erroGrupo = null;
	}

	function removerMembro(id: string): void {
		membrosSel = membrosSel.filter((m) => m.id !== id);
	}

	async function salvarGrupo(): Promise<void> {
		if (mutando) return;
		if (!nomeGrupo.trim()) {
			erroGrupo = 'Informe o nome do grupo.';
			return;
		}
		if (!tutorSel) {
			erroGrupo = 'Selecione o tutor do grupo.';
			return;
		}
		mutando = true;
		try {
			await createGrupo({
				name: nomeGrupo.trim(),
				tutorId: tutorSel.id,
				memberIds: membrosSel.map((m) => m.id)
			});
			toasts.success('Grupo criado com sucesso.');
			modalCriarGrupo = false;
			atualizar();
		} catch (err) {
			erroGrupo = toUserMessage(err).message;
		} finally {
			mutando = false;
		}
	}

	async function salvarAvaliacao(): Promise<void> {
		if (mutando || !alvoAval) return;
		// bind:value em type="number" coage para number/null — normalizar antes de validar.
		const textoNota = String(notaAval ?? '').replace(',', '.');
		const nota = Number(textoNota);
		if (textoNota.trim() === '' || !Number.isFinite(nota) || nota < 0 || nota > 10) {
			erroAval = 'Informe uma nota de 0 a 10.';
			return;
		}
		mutando = true;
		try {
			await avaliarMembro(alvoAval.grupoId, alvoAval.candidato.id, {
				nota,
				feedback: feedbackAval.trim(),
				resultado: resultadoAval
			});
			toasts.success(`Avaliação de ${alvoAval.candidato.name} registrada.`);
			alvoAval = null;
			grupoAtivo = null;
			atualizar();
		} catch (err) {
			erroAval = toUserMessage(err).message;
		} finally {
			mutando = false;
		}
	}

	const inputCls =
		'w-full rounded-md border border-border bg-elevated px-3 py-2.5 text-sm text-ink placeholder:text-muted/60 focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/30 transition';
	const inputErroCls = 'border-danger/60 focus:border-danger focus:ring-danger/30';
	const labelCls = 'mb-1 block text-xs font-medium text-muted';
	const btnPrimario =
		'inline-flex items-center gap-1.5 rounded-md bg-brand px-3.5 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi disabled:opacity-50';
	const btnSecundario =
		'inline-flex items-center gap-1.5 rounded-md border border-border bg-elevated px-3.5 py-2 text-sm font-medium text-ink transition hover:border-brand/50 hover:text-brandhi disabled:opacity-50';
</script>

<svelte:head>
	<title>Processo seletivo — Pessoas — FabLab</title>
</svelte:head>

<div class="space-y-4">
	<PageHeader title="Processo seletivo" subtitle="Triagem de candidatos organizada em grupos.">
		{#snippet children()}
			{#if podeGerir}
				<button type="button" onclick={abrirInscricao} class={btnSecundario} aria-label="Nova inscrição">
					<Icon name="plus" class="h-4 w-4" /> Nova inscrição
				</button>
				<button
					type="button"
					data-testid="ps-criar-grupo"
					onclick={abrirCriarGrupo}
					class={btnPrimario}
					aria-label="Criar grupo"
				>
					<Icon name="plus" class="h-4 w-4" /> Criar grupo
				</button>
			{/if}
		{/snippet}
	</PageHeader>

	{#if erro && !carregando}
		<ErrorBanner
			message="Não foi possível carregar o processo seletivo"
			hint="Verifique sua conexão e tente novamente. Se persistir, contate o suporte."
			onRetry={tentarNovamente}
		/>
	{:else}
		<section aria-label="Resumo do processo seletivo" class="grid grid-cols-2 gap-3 lg:grid-cols-4">
			{#if carregando}
				{#each [0, 1, 2, 3] as i (i)}
					<div class="animate-pulse rounded-xl border border-border bg-elevated p-4" aria-hidden="true">
						<div class="h-3 w-20 rounded bg-muted/20"></div>
						<div class="mt-2 h-7 w-12 rounded bg-muted/20"></div>
					</div>
				{/each}
			{:else}
				<div class="rounded-xl border border-border bg-elevated p-4">
					<p class="text-xs text-muted">Inscritos</p>
					<p class="mt-1 font-mono text-2xl font-semibold text-ink tabular-nums">
						{formatNumber(contagens.inscritos)}
					</p>
				</div>
				<div class="rounded-xl border border-border bg-elevated p-4" data-testid="ps-kpi-grupos">
					<p class="text-xs text-muted">Grupos</p>
					<p class="mt-1 font-mono text-2xl font-semibold text-ink tabular-nums">
						{formatNumber(contagens.grupos)}
					</p>
				</div>
				<div class="rounded-xl border border-border bg-elevated p-4">
					<p class="text-xs text-muted">Em avaliação</p>
					<p class="mt-1 font-mono text-2xl font-semibold text-ink tabular-nums">
						{formatNumber(contagens.em_avaliacao)}
					</p>
				</div>
				<div class="rounded-xl border border-border bg-elevated p-4">
					<p class="text-xs text-muted">Aprovados</p>
					<p class="mt-1 font-mono text-2xl font-semibold text-ink tabular-nums">
						{formatNumber(contagens.aprovados)}
					</p>
				</div>
			{/if}
		</section>

		<div class="flex flex-wrap items-center gap-2">
			<div class="min-w-56 flex-1">
				<SearchInput
					value={busca}
					onSearch={(v) => (busca = v)}
					placeholder="Buscar por nome, grupo ou etapa…"
					delay={300}
					label="Buscar no processo seletivo"
				/>
			</div>
			<div class="w-52">
				<Select
					id="ps-estagio"
					label=""
					options={ESTAGIO_OPCOES}
					value={filtroEstagio}
					placeholder="Filtrar por estágio"
					onChange={(v) => navegarEstagio(v as PsStage | '')}
				/>
			</div>
		</div>

		{#if carregando}
			<div class="rounded-xl border border-border bg-surface p-4">
				<TableSkeleton rows={4} columns={3} />
			</div>
		{:else if gruposFiltrados.length === 0}
			<div class="rounded-xl border border-border bg-surface">
				<EmptyState
					icon="users"
					title={termo || filtroEstagio ? 'Nenhum grupo encontrado' : 'Nenhum grupo no processo seletivo'}
					description={termo || filtroEstagio
						? 'Ajuste a busca ou o filtro de estágio.'
						: 'Crie um grupo para começar a triagem de candidatos.'}
				>
					{#snippet children()}
						{#if podeGerir && !termo && !filtroEstagio}
							<button type="button" onclick={abrirCriarGrupo} class={btnPrimario}>
								<Icon name="plus" class="h-4 w-4" /> Criar grupo
							</button>
						{/if}
					{/snippet}
				</EmptyState>
			</div>
		{:else}
			<section aria-label="Grupos">
				<div class="flex gap-2 overflow-x-auto pb-2">
					{#each gruposFiltrados as grupo (grupo.id)}
						{@const meta = psStageMeta(grupo.stage)}
						<button
							type="button"
							onclick={() => abrirGrupo(grupo)}
							aria-label={`Abrir grupo ${grupo.code} ${grupo.name}`}
							class="flex min-w-36 shrink-0 flex-col gap-1 rounded-xl border border-border bg-surface p-3 text-left transition hover:border-brand/50"
						>
							<span class="font-mono text-xs font-semibold text-brandhi">{grupo.code}</span>
							<span class="truncate text-sm font-medium text-ink">{grupo.name}</span>
							<span class="text-xs text-muted">
								{grupo.membersCount}
								{grupo.membersCount === 1 ? 'membro' : 'membros'}
							</span>
							<StatusBadge label={meta.label} color={meta.color} />
						</button>
					{/each}
				</div>
			</section>

			<KanbanBoard columns={colunas}>
				{#snippet children({ card })}
					{@const grupo = grupoPorId(card.id)}
					{#if grupo}
						<div class="flex flex-wrap items-center gap-1" aria-label={`Candidatos de ${grupo.name}`}>
							{#each grupo.candidates.slice(0, 6) as cand (cand.id)}
								{@const m = psStageMeta(cand.stage)}
								<span
									title={`${cand.name} · ${m.label}`}
									class="inline-flex h-6 w-6 items-center justify-center rounded-full bg-surface text-[10px] font-semibold text-ink ring-1 ring-border"
								>
									{initials(cand.name)}
								</span>
							{/each}
							{#if grupo.candidates.length > 6}
								<span class="text-[10px] text-muted">+{grupo.candidates.length - 6}</span>
							{/if}
						</div>
						<div class="flex flex-wrap items-center gap-1.5">
							<button
								type="button"
								onclick={() => abrirGrupo(grupo)}
								aria-label={`Ver grupo ${grupo.name}`}
								class="rounded-md border border-border bg-surface px-2.5 py-1 text-xs font-medium text-ink transition hover:border-brand/50 hover:text-brandhi"
							>
								Ver grupo
							</button>
							{#if podeGerir}
								{@const idx = ESTAGIOS.indexOf(grupo.stage)}
								{#if idx > 0}
									<button
										type="button"
										disabled={mutando}
										onclick={() => void mover(grupo, ESTAGIOS[idx - 1] as PsStage)}
										aria-label={`Voltar ${grupo.code} para ${psStageMeta(ESTAGIOS[idx - 1] as PsStage).label}`}
										class="rounded-md border border-border bg-surface px-2 py-1 text-xs text-muted transition hover:text-ink disabled:opacity-50"
									>
										←
									</button>
								{/if}
								{#if idx < ESTAGIOS.length - 1}
									<button
										type="button"
										disabled={mutando}
										onclick={() => void mover(grupo, ESTAGIOS[idx + 1] as PsStage)}
										aria-label={`Avançar ${grupo.code} para ${psStageMeta(ESTAGIOS[idx + 1] as PsStage).label}`}
										class="rounded-md border border-border bg-surface px-2 py-1 text-xs text-muted transition hover:text-ink disabled:opacity-50"
									>
										→
									</button>
								{/if}
							{/if}
						</div>
					{/if}
				{/snippet}
			</KanbanBoard>
		{/if}
	{/if}
</div>

<!-- Modal: nova inscrição -->
<Modal
	open={modalInscricao}
	title="Nova inscrição"
	subtitle="Cadastra um candidato como recrutando."
	onClose={() => (modalInscricao = false)}
>
	{#snippet children()}
		<div data-testid="modal-nova-inscricao" class="space-y-4">
			<div>
				<label for="insc-nome" class={labelCls}>
					Nome completo <span class="text-danger" aria-hidden="true">*</span>
				</label>
				<input
					id="insc-nome"
					type="text"
					bind:value={nomeInsc}
					placeholder="Nome e sobrenome"
					autocomplete="name"
					aria-invalid={!!errosInsc['nome']}
					class="{inputCls} {errosInsc['nome'] ? inputErroCls : ''}"
				/>
				{#if errosInsc['nome']}<p class="mt-1 text-xs text-danger">{errosInsc['nome']}</p>{/if}
			</div>
			<div>
				<label for="insc-email" class={labelCls}>
					E-mail <span class="text-danger" aria-hidden="true">*</span>
				</label>
				<input
					id="insc-email"
					type="email"
					bind:value={emailInsc}
					placeholder="nome@email.com"
					autocomplete="email"
					aria-invalid={!!errosInsc['email']}
					class="{inputCls} {errosInsc['email'] ? inputErroCls : ''}"
				/>
				{#if errosInsc['email']}<p class="mt-1 text-xs text-danger">{errosInsc['email']}</p>{/if}
			</div>
			<div class="grid gap-4 sm:grid-cols-2">
				<div>
					<label for="insc-curso" class={labelCls}>
						Curso <span class="text-danger" aria-hidden="true">*</span>
					</label>
					<input
						id="insc-curso"
						type="text"
						bind:value={cursoInsc}
						placeholder="Ex.: Sistemas de Informação"
						aria-invalid={!!errosInsc['curso']}
						class="{inputCls} {errosInsc['curso'] ? inputErroCls : ''}"
					/>
					{#if errosInsc['curso']}<p class="mt-1 text-xs text-danger">{errosInsc['curso']}</p>{/if}
				</div>
				<div>
					<label for="insc-periodo" class={labelCls}>Período</label>
					<input
						id="insc-periodo"
						type="text"
						bind:value={periodoInsc}
						placeholder="Ex.: 4ª fase"
						class={inputCls}
					/>
				</div>
			</div>
			<div>
				<label for="insc-disp" class={labelCls}>Disponibilidade</label>
				<input
					id="insc-disp"
					type="text"
					bind:value={dispInsc}
					placeholder="Ex.: quartas à tarde"
					class={inputCls}
				/>
			</div>
		</div>
	{/snippet}
	{#snippet footer()}
		<button
			type="button"
			onclick={() => (modalInscricao = false)}
			disabled={mutando}
			class="rounded-md border border-border bg-surface px-4 py-2 text-sm font-medium text-ink transition hover:bg-elevated disabled:opacity-50"
		>
			Cancelar
		</button>
		<button
			type="button"
			onclick={() => void salvarInscricao()}
			disabled={mutando}
			class="rounded-md bg-brand px-4 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi disabled:opacity-50"
		>
			{mutando ? 'Salvando…' : 'Registrar inscrição'}
		</button>
	{/snippet}
</Modal>

<!-- Modal: criar grupo -->
<Modal
	open={modalCriarGrupo}
	title="Criar grupo"
	subtitle="Reúna inscritos para a dinâmica do processo seletivo."
	onClose={() => (modalCriarGrupo = false)}
>
	{#snippet children()}
		<div data-testid="modal-criar-grupo" class="space-y-4">
			{#if erroGrupo}
				<p role="alert" class="rounded-md border border-danger/30 bg-danger/10 px-3 py-2 text-xs text-danger">
					{erroGrupo}
				</p>
			{/if}
			<div>
				<label for="grupo-nome" class={labelCls}>
					Nome do grupo <span class="text-danger" aria-hidden="true">*</span>
				</label>
				<input
					id="grupo-nome"
					type="text"
					bind:value={nomeGrupo}
					placeholder="Ex.: Eletrônica"
					class={inputCls}
				/>
			</div>
			<div>
				<span id="grupo-tutor-label" class={labelCls}>
					Tutor(a) responsável <span class="text-danger" aria-hidden="true">*</span>
				</span>
				{#key chavePickers}
					<PeoplePicker
						selected={tutorSel}
						onSelect={(p) => (tutorSel = p)}
						placeholder="Buscar tutor por nome…"
						hint="Acompanha o grupo na dinâmica e na evolução de níveis."
					/>
				{/key}
			</div>
			<div>
				<span id="grupo-membros-label" class={labelCls}>Membros</span>
				{#key `membros-${chavePickers}`}
					<PeoplePicker
						selected={null}
						onSelect={adicionarMembro}
						placeholder="Buscar inscrito para adicionar…"
					/>
				{/key}
				{#if membrosSel.length > 0}
					<ul class="mt-2 space-y-1.5" aria-label="Membros selecionados">
						{#each membrosSel as membro (membro.id)}
							<li
								class="flex items-center justify-between gap-2 rounded-md border border-border bg-elevated px-3 py-1.5"
							>
								<span class="flex items-center gap-2 text-sm text-ink">
									<Avatar name={membro.name} size="xs" />
									{membro.name}
								</span>
								<button
									type="button"
									onclick={() => removerMembro(membro.id)}
									aria-label={`Remover ${membro.name} do grupo`}
									class="rounded-md p-1 text-muted transition hover:text-danger"
								>
									<Icon name="x-mark" class="h-4 w-4" />
								</button>
							</li>
						{/each}
					</ul>
				{/if}
			</div>
		</div>
	{/snippet}
	{#snippet footer()}
		<button
			type="button"
			onclick={() => (modalCriarGrupo = false)}
			disabled={mutando}
			class="rounded-md border border-border bg-surface px-4 py-2 text-sm font-medium text-ink transition hover:bg-elevated disabled:opacity-50"
		>
			Cancelar
		</button>
		<button
			type="button"
			onclick={() => void salvarGrupo()}
			disabled={mutando}
			class="rounded-md bg-brand px-4 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi disabled:opacity-50"
		>
			{mutando ? 'Salvando…' : 'Criar grupo'}
		</button>
	{/snippet}
</Modal>

<!-- Modal: detalhe do grupo -->
<Modal
	open={grupoAtivo !== null}
	title={grupoAtivo ? `${grupoAtivo.code} · ${grupoAtivo.name}` : 'Grupo'}
	subtitle={grupoAtivo
		? `${grupoAtivo.membersCount} ${grupoAtivo.membersCount === 1 ? 'membro' : 'membros'} · ${psStageMeta(grupoAtivo.stage).label}`
		: ''}
	onClose={() => (grupoAtivo = null)}
	width="lg"
>
	{#snippet children()}
		{#if grupoAtivo}
			<div data-testid="modal-grupo" class="space-y-3">
				{#if grupoAtivo.candidates.length === 0}
					<p class="text-sm text-muted">Nenhum candidato neste grupo.</p>
				{:else}
					<ul class="space-y-2">
						{#each grupoAtivo.candidates as cand (cand.id)}
							{@const m = psStageMeta(cand.stage)}
							<li
								class="flex flex-wrap items-center gap-3 rounded-lg border border-border bg-elevated p-3"
							>
								<Avatar name={cand.name} initialsOverride={cand.initials} size="sm" />
								<div class="min-w-0 flex-1">
									<p class="truncate text-sm font-medium text-ink">{cand.name}</p>
									<div class="mt-1 flex flex-wrap items-center gap-1.5">
										<StatusBadge label={m.label} color={m.color} />
										{#if cand.nota !== null && cand.nota !== undefined}
											<span class="font-mono text-xs text-muted">Nota {cand.nota}</span>
										{/if}
									</div>
									{#if cand.feedback}
										<p class="mt-1 text-xs text-muted">{cand.feedback}</p>
									{/if}
								</div>
								{#if podeGerir}
									<button
										type="button"
										data-testid="ps-avaliar"
										onclick={() => {
											const atual = grupoAtivo;
											if (atual) abrirAvaliar(atual.id, cand);
										}}
										aria-label={`Avaliar ${cand.name}`}
										class="rounded-md border border-border bg-surface px-3 py-1.5 text-xs font-medium text-ink transition hover:border-brand/50 hover:text-brandhi"
									>
										Avaliar
									</button>
								{/if}
							</li>
						{/each}
					</ul>
				{/if}
				{#if podeGerir}
					<div class="flex flex-wrap items-center gap-2 pt-1">
						<span class="text-xs text-muted">Mover grupo para:</span>
						{#each ESTAGIOS as estagio (estagio)}
							{@const meta = psStageMeta(estagio)}
							<button
								type="button"
								disabled={mutando || estagio === grupoAtivo.stage}
								onclick={() => {
									const atual = grupoAtivo;
									if (atual) void mover(atual, estagio);
								}}
								aria-label={`Mover ${grupoAtivo.code} para ${meta.label}`}
								aria-pressed={estagio === grupoAtivo.stage}
								class="rounded-full border px-2.5 py-1 text-xs font-medium transition disabled:opacity-50 {estagio ===
								grupoAtivo.stage
									? 'border-brand/60 bg-brand/15 text-brandhi'
									: 'border-border text-muted hover:text-ink'}"
							>
								{meta.label}
							</button>
						{/each}
					</div>
				{/if}
			</div>
		{/if}
	{/snippet}
</Modal>

<!-- Modal: avaliar candidato -->
<Modal
	open={alvoAval !== null}
	title={alvoAval ? `Avaliar ${alvoAval.candidato.name}` : 'Avaliar candidato'}
	subtitle="Avaliação sempre individual."
	onClose={() => (alvoAval = null)}
>
	{#snippet children()}
		{#if alvoAval}
			<div data-testid="modal-avaliar-ps" class="space-y-4">
				{#if erroAval}
					<p role="alert" class="rounded-md border border-danger/30 bg-danger/10 px-3 py-2 text-xs text-danger">
						{erroAval}
					</p>
				{/if}
				<fieldset>
					<legend class={labelCls}>Resultado</legend>
					<div class="flex gap-2">
						<label
							class="flex flex-1 cursor-pointer items-center gap-2 rounded-md border px-3 py-2.5 text-sm transition {resultadoAval ===
							'aprovado'
								? 'border-success/60 bg-success/10 text-ink'
								: 'border-border text-muted hover:text-ink'}"
						>
							<input
								type="radio"
								name="avaliar-resultado"
								value="aprovado"
								bind:group={resultadoAval}
								class="accent-brand"
							/>
							Aprovado
						</label>
						<label
							class="flex flex-1 cursor-pointer items-center gap-2 rounded-md border px-3 py-2.5 text-sm transition {resultadoAval ===
							'reprovado'
								? 'border-danger/60 bg-danger/10 text-ink'
								: 'border-border text-muted hover:text-ink'}"
						>
							<input
								type="radio"
								name="avaliar-resultado"
								value="reprovado"
								bind:group={resultadoAval}
								class="accent-brand"
							/>
							Reprovado
						</label>
					</div>
				</fieldset>
				<div>
					<label for="avaliar-nota" class={labelCls}>
						Nota (0 a 10) <span class="text-danger" aria-hidden="true">*</span>
					</label>
					<input
						id="avaliar-nota"
						type="number"
						min="0"
						max="10"
						step="0.5"
						bind:value={notaAval}
						placeholder="Ex.: 8,5"
						class="{inputCls} font-mono tabular-nums"
					/>
				</div>
				<div>
					<label for="avaliar-feedback" class={labelCls}>Feedback</label>
					<textarea
						id="avaliar-feedback"
						bind:value={feedbackAval}
						rows="3"
						placeholder="Pontos fortes e pontos a desenvolver…"
						class="{inputCls} resize-y"
					></textarea>
				</div>
			</div>
		{/if}
	{/snippet}
	{#snippet footer()}
		<button
			type="button"
			onclick={() => (alvoAval = null)}
			disabled={mutando}
			class="rounded-md border border-border bg-surface px-4 py-2 text-sm font-medium text-ink transition hover:bg-elevated disabled:opacity-50"
		>
			Cancelar
		</button>
		<button
			type="button"
			onclick={() => void salvarAvaliacao()}
			disabled={mutando}
			class="rounded-md bg-brand px-4 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi disabled:opacity-50"
		>
			{mutando ? 'Salvando…' : 'Salvar avaliação'}
		</button>
	{/snippet}
</Modal>
