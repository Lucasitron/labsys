<script lang="ts">
	import { goto } from '$app/navigation';
	import { get } from 'svelte/store';
	import { auth } from '$lib/stores/auth';
	import {
		atribuirTarefaFinal,
		avaliarTreinamento,
		getSessao,
		listTreinamentos
	} from '$lib/api/rh/treinamentos';
	import type { Treinamento, TreinamentoSessao } from '$lib/types/rh';
	import { toUserMessage } from '$lib/utils/errors';
	import { trainingStatusMeta } from '$lib/utils/rh-status';
	import { canEditRH, isAdmin, isInstrutor } from '$lib/utils/permissions';
	import { toasts } from '$lib/stores/toast';
	import PageHeader from '$lib/components/ui/PageHeader.svelte';
	import StatusBadge from '$lib/components/ui/StatusBadge.svelte';
	import Avatar from '$lib/components/ui/Avatar.svelte';
	import Modal from '$lib/components/ui/Modal.svelte';
	import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte';
	import EmptyState from '$lib/components/ui/EmptyState.svelte';
	import TableSkeleton from '$lib/components/ui/TableSkeleton.svelte';
	import Icon from '$lib/components/ui/Icon.svelte';

	const usuario = $derived(get(auth).user);
	const permitido = $derived(isAdmin(usuario) || isInstrutor(usuario));
	const podeGerir = $derived(permitido || canEditRH(usuario));

	$effect(() => {
		if (!permitido) void goto('/pessoas/treinamentos');
	});

	type Aba = 'abertos' | 'agora' | 'passados';
	const ABA_TITULO: Record<Aba, string> = {
		abertos: 'Em aberto',
		agora: 'Agora',
		passados: 'Passados sem avaliação'
	};

	let treinamentos = $state<Treinamento[]>([]);
	let sessao = $state<TreinamentoSessao | null>(null);
	let carregando = $state(true);
	let erro = $state<string | null>(null);
	let erroSessao = $state<string | null>(null);
	let aba = $state<Aba>('agora');
	let recarregar = $state(0);
	let mutando = $state(false);

	let agoraId = $state<string | null>(null);

	$effect(() => {
		if (!permitido) return;
		void recarregar;
		const ctrl = new AbortController();
		const sinal = ctrl.signal;
		carregando = true;
		erro = null;
		erroSessao = null;
		sessao = null;
		agoraId = null;

		void listTreinamentos({}, fetch)
			.then((res) => {
				if (sinal.aborted) return;
				treinamentos = res.trainings;
				const atual = res.trainings.find((t) => t.status === 'em_andamento') ?? null;
				agoraId = atual?.id ?? null;
				if (!atual) return;
				void getSessao(atual.id, fetch)
					.then((s) => {
						if (!sinal.aborted) sessao = s;
					})
					.catch((err: unknown) => {
						if (sinal.aborted) return;
						erroSessao =
							err instanceof Error ? err.message : 'Não foi possível carregar a sessão atual';
					});
			})
			.catch((err: unknown) => {
				if (sinal.aborted) return;
				erro = err instanceof Error ? err.message : 'Não foi possível carregar as sessões';
			})
			.finally(() => {
				if (!sinal.aborted) carregando = false;
			});

		return () => ctrl.abort();
	});

	function tentarNovamente(): void {
		recarregar += 1;
	}

	const agora = $derived(agoraId ? (treinamentos.find((t) => t.id === agoraId) ?? null) : null);
	const abertos = $derived(
		treinamentos.filter((t) => ['aberto', 'agendado', 'solicitado', 'pendente'].includes(t.status))
	);
	const emAndamento = $derived(treinamentos.filter((t) => t.status === 'em_andamento'));
	const passados = $derived(treinamentos.filter((t) => t.status === 'concluido'));
	const visiveis = $derived(aba === 'abertos' ? abertos : aba === 'agora' ? emAndamento : passados);

	function tempoRestante(fimIso: string): string {
		const fim = new Date(fimIso).getTime();
		if (Number.isNaN(fim)) return '—';
		const diff = Math.max(0, fim - Date.now());
		const horas = Math.floor(diff / 3_600_000);
		const minutos = Math.round((diff % 3_600_000) / 60_000);
		return `Faltam ${horas}h ${minutos}min`;
	}

	function formatarHorario(inicio: string, fim: string): string {
		const di = new Date(inicio);
		const df = new Date(fim);
		const hora = new Intl.DateTimeFormat('pt-BR', { hour: '2-digit', minute: '2-digit' });
		if (Number.isNaN(di.getTime())) return '—';
		return Number.isNaN(df.getTime()) ? hora.format(di) : `${hora.format(di)}–${hora.format(df)}`;
	}

	// ---- Modal tarefa final (POST …/tarefa-final 🔴) ----

	let modalTarefa = $state(false);
	let tarefaTitulo = $state('');
	let tarefaDescricao = $state('');
	let tarefaConclusao = $state('');
	let erroTarefa = $state<string | null>(null);

	function abrirTarefa(): void {
		tarefaTitulo = '';
		tarefaDescricao = '';
		tarefaConclusao = '';
		erroTarefa = null;
		modalTarefa = true;
	}

	async function confirmarTarefa(): Promise<void> {
		if (mutando || !agora) return;
		if (!tarefaTitulo.trim()) {
			erroTarefa = 'Informe o título da tarefa.';
			return;
		}
		if (!tarefaConclusao) {
			erroTarefa = 'Informe a data de conclusão.';
			return;
		}
		mutando = true;
		try {
			await atribuirTarefaFinal(agora.id, {
				title: tarefaTitulo.trim(),
				...(tarefaDescricao.trim() ? { description: tarefaDescricao.trim() } : {}),
				...(agora.group ? { groupId: agora.group.id } : {}),
				dataConclusao: tarefaConclusao
			});
			toasts.success('Tarefa final atribuída com sucesso.');
			modalTarefa = false;
			tentarNovamente();
		} catch (err) {
			erroTarefa = toUserMessage(err).message;
		} finally {
			mutando = false;
		}
	}

	// ---- Modal avaliar (POST …/avaliar 🔴) ----

	let alvoAvaliar = $state<Treinamento | null>(null);
	let nota = $state('');
	let feedback = $state('');
	let erroAvaliar = $state<string | null>(null);

	function abrirAvaliar(t: Treinamento): void {
		alvoAvaliar = t;
		nota = '';
		feedback = '';
		erroAvaliar = null;
	}

	async function confirmarAvaliar(): Promise<void> {
		if (mutando || !alvoAvaliar) return;
		const valor = Number(nota.replace(',', '.'));
		if (!nota.trim() || Number.isNaN(valor) || valor < 0 || valor > 10) {
			erroAvaliar = 'Informe uma nota de 0 a 10.';
			return;
		}
		mutando = true;
		try {
			await avaliarTreinamento(alvoAvaliar.id, { nota: valor, feedback: feedback.trim() });
			toasts.success('Avaliação registrada com sucesso.');
			alvoAvaliar = null;
			tentarNovamente();
		} catch (err) {
			erroAvaliar = toUserMessage(err).message;
		} finally {
			mutando = false;
		}
	}

	// ---- Modal material (placeholder: conteúdo real fora de escopo R-8) ----

	let material = $state<string | null>(null);
	const materiais = $derived(sessao?.materials ?? []);

	const inputCls =
		'w-full rounded-md border border-border bg-elevated px-3 py-2.5 text-sm text-ink placeholder:text-muted/60 focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/30 transition';
	const labelCls = 'mb-1 block text-xs font-medium text-muted';
	const btnPrimario =
		'inline-flex items-center gap-1.5 rounded-md bg-brand px-3.5 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi disabled:opacity-50';
	const btnSecundario =
		'inline-flex items-center gap-1.5 rounded-md border border-border bg-elevated px-3.5 py-2 text-sm font-medium text-ink transition hover:bg-elevated/70 disabled:opacity-50';
</script>

<svelte:head>
	<title>Sessão — Treinamentos — FabLab</title>
</svelte:head>

{#if !permitido}
	<div class="rounded-xl border border-border bg-surface p-4" aria-hidden="true">
		<div class="h-4 w-48 animate-pulse rounded bg-muted/20"></div>
	</div>
{:else}
	<div class="space-y-5">
		<PageHeader title="Sessão do instrutor" subtitle="Acompanhe a sessão em andamento.">
			{#snippet children()}
				<div class="flex items-center gap-2" aria-label="Instrutor(a) responsável">
					<Avatar name={usuario?.name ?? ''} size="sm" />
					<span class="text-sm text-muted">
						<span class="font-medium text-ink">{usuario?.name ?? '—'}</span> · Instrutor(a)
					</span>
				</div>
			{/snippet}
		</PageHeader>

		{#if erro && !carregando}
			<ErrorBanner message="Não foi possível carregar as sessões" hint="Verifique sua conexão e tente novamente. Se persistir, contate o suporte." onRetry={tentarNovamente} />
		{:else if carregando}
			<div class="rounded-xl border border-border bg-surface p-4">
				<TableSkeleton rows={4} columns={4} />
			</div>
		{:else}
			{#if agora && sessao && !erroSessao}
				{@const st = trainingStatusMeta(agora.status)}
				<section
					id="sessao-atual"
					data-testid="sessao-atual"
					aria-label="Sessão atual em andamento"
					class="overflow-hidden rounded-xl border border-brand/40 bg-gradient-to-br from-brand/25 via-surface to-surface p-5 shadow-2xl shadow-black/40"
				>
					<p class="inline-flex items-center gap-2 text-[11px] font-semibold tracking-wide text-brandhi">
						<span class="relative flex h-2 w-2" aria-hidden="true">
							<span class="absolute inline-flex h-full w-full animate-ping rounded-full bg-brand opacity-60"></span>
							<span class="relative inline-flex h-2 w-2 rounded-full bg-brand"></span>
						</span>
						EM ANDAMENTO · AGORA
					</p>
					<h2 class="mt-2 text-2xl font-semibold tracking-tight text-ink">{agora.title}</h2>
					<p class="mt-1 font-mono text-xs text-muted tabular-nums">
						{agora.group?.label ?? 'Sem grupo'} · {formatarHorario(sessao.startsAt, sessao.endsAt)} ·
						{agora.machine ?? 'Máquina a definir'}
					</p>
					<div class="mt-3 flex flex-wrap items-center gap-3 text-sm">
						<StatusBadge label={st.label} color={st.color} />
						<span class="font-mono text-xs text-muted tabular-nums">
							Turma {sessao.present}/{agora.groupSize ?? sessao.present} presentes
						</span>
						<span class="font-mono text-xs text-brandhi tabular-nums">
							{tempoRestante(sessao.endsAt)}
						</span>
					</div>
					<div class="mt-4 grid gap-2 sm:grid-cols-3" aria-label="Materiais da sessão">
						<button
							type="button"
							onclick={() => (material = 'Documentação')}
							class="rounded-xl border border-border bg-surface p-3 text-left transition hover:border-brand/40"
							aria-label="Abrir documentação da sessão"
						>
							<Icon name="document" class="h-5 w-5 text-brandhi" />
							<p class="mt-1.5 text-sm font-medium text-ink">Documentação</p>
							<p class="text-xs text-muted">Guia da máquina</p>
						</button>
						<button
							type="button"
							onclick={() => (material = 'Materiais de apoio')}
							class="rounded-xl border border-border bg-surface p-3 text-left transition hover:border-brand/40"
							aria-label="Abrir materiais de apoio da sessão"
						>
							<Icon name="folder" class="h-5 w-5 text-brandhi" />
							<p class="mt-1.5 text-sm font-medium text-ink">Apoio</p>
							<p class="text-xs text-muted">Checklists</p>
						</button>
						<button
							type="button"
							onclick={() => (material = 'Modelos prontos')}
							class="rounded-xl border border-border bg-surface p-3 text-left transition hover:border-brand/40"
							aria-label="Abrir modelos prontos da sessão"
						>
							<Icon name="cube" class="h-5 w-5 text-brandhi" />
							<p class="mt-1.5 text-sm font-medium text-ink">Modelos</p>
							<p class="text-xs text-muted">STL + gcode</p>
						</button>
					</div>
					{#if podeGerir}
						<div class="mt-4 flex flex-wrap gap-2">
							<button
								type="button"
								onclick={abrirTarefa}
								class={btnSecundario}
								aria-label="Atribuir tarefa final na sessão atual"
							>
								Atribuir tarefa final
							</button>
							{#if agora}
								<button
									type="button"
									onclick={() => abrirAvaliar(agora)}
									class={btnPrimario}
									aria-label="Avaliar treinamento da sessão atual"
								>
									Avaliar
								</button>
							{/if}
						</div>
					{/if}
				</section>
			{:else}
				<div class="rounded-xl border border-border bg-surface">
					<EmptyState
						icon="clock"
						title="Nenhuma sessão em andamento"
						description={erroSessao ?? 'Não há treinamento com sessão ativa agora.'}
					/>
				</div>
			{/if}

			<div role="tablist" aria-label="Situação dos treinamentos" class="flex flex-wrap gap-1.5">
				{#each (['abertos', 'agora', 'passados'] as Aba[]) as id (id)}
					{@const count = id === 'abertos' ? abertos.length : id === 'agora' ? emAndamento.length : passados.length}
					<button
						type="button"
						role="tab"
						aria-selected={aba === id}
						onclick={() => (aba = id)}
						aria-label="{ABA_TITULO[id]} ({count})"
						class="inline-flex items-center gap-1.5 rounded-md px-3 py-1.5 text-sm font-medium transition {aba ===
						id
							? 'bg-brand/15 text-brandhi'
							: 'text-muted hover:text-ink'}"
					>
						{ABA_TITULO[id]}
						<span
							class="inline-flex h-4 min-w-4 items-center justify-center rounded-full px-1 text-[10px] font-bold {aba ===
							id
								? 'bg-brand text-white'
								: 'bg-muted/15 text-muted'}"
						>
							{count}
						</span>
					</button>
				{/each}
			</div>

			{#if visiveis.length === 0}
				<div class="rounded-xl border border-border bg-surface">
					<EmptyState
						icon="squares"
						title={`Nenhum treinamento ${ABA_TITULO[aba].toLowerCase()}`}
						description="Os treinamentos desta situação aparecerão aqui."
					/>
				</div>
			{:else}
				<div class="grid gap-3 md:grid-cols-2">
					{#each visiveis as t (t.id)}
						{@const m = trainingStatusMeta(t.status)}
						<article class="rounded-xl border border-border bg-surface p-4">
							<div class="flex items-start justify-between gap-2">
								<a
									href="/pessoas/treinamentos/{t.id}"
									class="text-sm font-semibold text-ink transition hover:text-brandhi"
								>
									{t.title}
								</a>
								<StatusBadge label={m.label} color={m.color} />
							</div>
							<p class="mt-1 text-xs text-muted">
								{t.group?.label ?? 'Sem grupo'} · {t.instructor.name}
							</p>
							<div class="mt-3 flex flex-wrap gap-2">
								{#if aba === 'agora'}
									<a
										href="#sessao-atual"
										class={btnSecundario}
										aria-label="Abrir sessão de {t.title}"
									>
										Abrir sessão
									</a>
								{:else if aba === 'passados'}
									{#if podeGerir}
										<button
											type="button"
											onclick={() => abrirAvaliar(t)}
											class={btnPrimario}
											aria-label="Avaliar {t.title}"
										>
											Avaliar treinamento
										</button>
									{/if}
								{:else}
									<a
										href="/pessoas/treinamentos/{t.id}"
										class={btnSecundario}
										aria-label="Ver detalhe de {t.title}"
									>
										Ver detalhe
									</a>
								{/if}
							</div>
						</article>
					{/each}
				</div>
			{/if}
		{/if}
	</div>
{/if}

<Modal
	open={modalTarefa}
	title="Atribuir tarefa final"
	subtitle={agora ? agora.title : ''}
	onClose={() => (modalTarefa = false)}
>
	{#snippet children()}
		<div data-testid="tarefa-final" class="space-y-4">
			{#if erroTarefa}
				<p role="alert" class="rounded-md border border-danger/30 bg-danger/10 px-3 py-2 text-xs text-danger">
					{erroTarefa}
				</p>
			{/if}
			<div>
				<label for="ses-tar-titulo" class={labelCls}>
					Título <span class="text-danger" aria-hidden="true">*</span>
				</label>
				<input
					id="ses-tar-titulo"
					type="text"
					bind:value={tarefaTitulo}
					placeholder="Ex.: Imprimir suporte de celular"
					class={inputCls}
				/>
			</div>
			<div>
				<label for="ses-tar-descricao" class={labelCls}>Descrição</label>
				<textarea
					id="ses-tar-descricao"
					bind:value={tarefaDescricao}
					rows="2"
					placeholder="Detalhes da tarefa…"
					class="{inputCls} resize-y"
				></textarea>
			</div>
			<div class="grid grid-cols-2 gap-4">
				<div>
					<span class={labelCls}>Treinamento</span>
					<p class="rounded-md border border-border bg-elevated px-3 py-2.5 text-sm text-ink">
						{agora?.title ?? '—'}
					</p>
				</div>
				<div>
					<span class={labelCls}>Grupo</span>
					<p class="rounded-md border border-border bg-elevated px-3 py-2.5 text-sm text-ink">
						{agora?.group?.label ?? '—'}
					</p>
				</div>
			</div>
			<div>
				<label for="ses-tar-modelo" class={labelCls}>Modelo pronto</label>
				<input
					id="ses-tar-modelo"
					type="text"
					value="Suporte de celular (STL + gcode)"
					readonly
					class="{inputCls} font-mono opacity-70"
				/>
			</div>
			<div>
				<label for="ses-tar-conclusao" class={labelCls}>
					Data de conclusão <span class="text-danger" aria-hidden="true">*</span>
				</label>
				<input
					id="ses-tar-conclusao"
					type="date"
					bind:value={tarefaConclusao}
					class="{inputCls} font-mono tabular-nums"
				/>
			</div>
		</div>
	{/snippet}
	{#snippet footer()}
		<button
			type="button"
			onclick={() => (modalTarefa = false)}
			disabled={mutando}
			class="rounded-md border border-border bg-surface px-4 py-2 text-sm font-medium text-ink transition hover:bg-elevated disabled:opacity-50"
		>
			Cancelar
		</button>
		<button
			type="button"
			onclick={() => void confirmarTarefa()}
			disabled={mutando}
			class="rounded-md bg-brand px-4 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi disabled:opacity-50"
		>
			{mutando ? 'Atribuindo…' : 'Atribuir tarefa'}
		</button>
	{/snippet}
</Modal>

<Modal
	open={alvoAvaliar !== null}
	title="Avaliar treinamento"
	subtitle={alvoAvaliar ? alvoAvaliar.title : ''}
	onClose={() => (alvoAvaliar = null)}
	width="sm"
>
	{#snippet children()}
		<div data-testid="sessao-avaliar" class="space-y-4">
			{#if erroAvaliar}
				<p role="alert" class="rounded-md border border-danger/30 bg-danger/10 px-3 py-2 text-xs text-danger">
					{erroAvaliar}
				</p>
			{/if}
			<div>
				<label for="ses-ava-nota" class={labelCls}>
					Nota (0 a 10) <span class="text-danger" aria-hidden="true">*</span>
				</label>
				<input
					id="ses-ava-nota"
					type="text"
					inputmode="decimal"
					bind:value={nota}
					placeholder="Ex.: 9,0"
					class="{inputCls} font-mono tabular-nums"
				/>
			</div>
			<div>
				<label for="ses-ava-feedback" class={labelCls}>Feedback</label>
				<textarea
					id="ses-ava-feedback"
					bind:value={feedback}
					rows="3"
					placeholder="Desempenho da turma…"
					class="{inputCls} resize-y"
				></textarea>
			</div>
		</div>
	{/snippet}
	{#snippet footer()}
		<button
			type="button"
			onclick={() => (alvoAvaliar = null)}
			disabled={mutando}
			class="rounded-md border border-border bg-surface px-4 py-2 text-sm font-medium text-ink transition hover:bg-elevated disabled:opacity-50"
		>
			Cancelar
		</button>
		<button
			type="button"
			onclick={() => void confirmarAvaliar()}
			disabled={mutando}
			class="rounded-md bg-brand px-4 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi disabled:opacity-50"
		>
			{mutando ? 'Salvando…' : 'Salvar avaliação'}
		</button>
	{/snippet}
</Modal>

<Modal
	open={material !== null}
	title={material ?? 'Material'}
	subtitle="Conteúdo carregado ao abrir a sessão."
	onClose={() => (material = null)}
	width="sm"
>
	{#snippet children()}
		<div
			class="flex flex-col items-center justify-center gap-2 rounded-xl border border-dashed border-border bg-elevated/50 px-4 py-8 text-center"
		>
			<Icon name="document" class="h-8 w-8 text-muted" />
			<p class="text-sm font-medium text-ink">{material}</p>
			<p class="max-w-xs text-xs text-muted">Conteúdo real fora de escopo do MVP (R-8).</p>
		</div>
	{/snippet}
</Modal>
