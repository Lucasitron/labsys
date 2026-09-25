<script lang="ts">
	import { page } from '$app/state';
	import { get } from 'svelte/store';
	import { ApiError } from '$lib/api/client';
	import { auth } from '$lib/stores/auth';
	import {
		atribuirTarefaFinal,
		avaliarTreinamento,
		getSessao,
		getTreinamento
	} from '$lib/api/rh/treinamentos';
	import type { TreinamentoDetail, TreinamentoSessao } from '$lib/types/rh';
	import { toUserMessage } from '$lib/utils/errors';
	import { trainingStatusMeta } from '$lib/utils/rh-status';
	import { canEditRH, isInstrutor } from '$lib/utils/permissions';
	import { toasts } from '$lib/stores/toast';
	import StatusBadge from '$lib/components/ui/StatusBadge.svelte';
	import Avatar from '$lib/components/ui/Avatar.svelte';
	import Modal from '$lib/components/ui/Modal.svelte';
	import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte';
	import EmptyState from '$lib/components/ui/EmptyState.svelte';
	import TableSkeleton from '$lib/components/ui/TableSkeleton.svelte';
	import Icon from '$lib/components/ui/Icon.svelte';

	const usuario = $derived(get(auth).user);
	const podeAvaliar = $derived(isInstrutor(usuario) || canEditRH(usuario));
	const id = $derived(page.params['id'] ?? '');

	let detalhe = $state<TreinamentoDetail | null>(null);
	let sessao = $state<TreinamentoSessao | null>(null);
	let carregando = $state(true);
	let naoEncontrado = $state(false);
	let erroBloco = $state<string | null>(null);
	let erroSessao = $state<string | null>(null);
	let recarregar = $state(0);
	let mutando = $state(false);

	$effect(() => {
		const treinamentoId = id;
		void recarregar;
		if (!treinamentoId) return;

		const ctrl = new AbortController();
		const sinal = ctrl.signal;
		carregando = true;
		naoEncontrado = false;
		erroBloco = null;
		erroSessao = null;
		sessao = null;

		void getTreinamento(treinamentoId, fetch)
			.then((res) => {
				if (sinal.aborted) return;
				detalhe = res;
			})
			.catch((err: unknown) => {
				if (sinal.aborted) return;
				if (err instanceof ApiError && err.status === 404) {
					naoEncontrado = true;
					return;
				}
				erroBloco = err instanceof Error ? err.message : 'Não foi possível carregar o treinamento';
			})
			.finally(() => {
				if (!sinal.aborted) carregando = false;
			});

		void getSessao(treinamentoId, fetch)
			.then((res) => {
				if (!sinal.aborted) sessao = res;
			})
			.catch((err: unknown) => {
				if (sinal.aborted) return;
				erroSessao =
					err instanceof Error ? err.message : 'Não foi possível carregar a sessão atual';
			});

		return () => ctrl.abort();
	});

	function tentarNovamente(): void {
		recarregar += 1;
	}

	const sessaoAtiva = $derived(sessao !== null && !erroSessao);
	const status = $derived(detalhe ? trainingStatusMeta(detalhe.status) : null);

	const fmtData = new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: '2-digit' });
	const fmtDataAno = new Intl.DateTimeFormat('pt-BR', {
		day: '2-digit',
		month: '2-digit',
		year: 'numeric'
	});
	const fmtMedia = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 1 });

	function formatarData(iso: string | undefined): string {
		if (!iso) return '—';
		const data = new Date(iso.length <= 10 ? `${iso}T12:00:00` : iso);
		if (Number.isNaN(data.getTime())) return iso;
		return (iso.length <= 10 ? fmtDataAno : fmtData).format(data);
	}

	function formatarHorario(inicio: string, fim: string): string {
		const di = new Date(inicio);
		const df = new Date(fim);
		if (Number.isNaN(di.getTime())) return '—';
		const fmt = new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: '2-digit' });
		const hora = new Intl.DateTimeFormat('pt-BR', { hour: '2-digit', minute: '2-digit' });
		const fimTxt = Number.isNaN(df.getTime()) ? '' : `–${hora.format(df)}`;
		return `${fmt.format(di)} · ${hora.format(di)}${fimTxt}`;
	}

	// ---- Modal avaliar (POST …/avaliar 🔴) ----

	let modalAvaliar = $state(false);
	let nota = $state('');
	let feedback = $state('');
	let erroAvaliar = $state<string | null>(null);

	function abrirAvaliar(): void {
		nota = '';
		feedback = '';
		erroAvaliar = null;
		modalAvaliar = true;
	}

	async function confirmarAvaliar(): Promise<void> {
		if (mutando || !detalhe) return;
		const valor = Number(nota.replace(',', '.'));
		if (!nota.trim() || Number.isNaN(valor) || valor < 0 || valor > 10) {
			erroAvaliar = 'Informe uma nota de 0 a 10.';
			return;
		}
		mutando = true;
		try {
			await avaliarTreinamento(detalhe.id, { nota: valor, feedback: feedback.trim() });
			toasts.success('Avaliação registrada com sucesso.');
			modalAvaliar = false;
			tentarNovamente();
		} catch (err) {
			erroAvaliar = toUserMessage(err).message;
		} finally {
			mutando = false;
		}
	}

	// ---- Modal atribuir tarefa final (POST …/tarefa-final 🔴) ----

	let modalTarefa = $state(false);
	let tarefaTitulo = $state('');
	let tarefaConclusao = $state('');
	let erroTarefa = $state<string | null>(null);

	function abrirTarefa(): void {
		tarefaTitulo = detalhe?.finalTask?.title ?? '';
		tarefaConclusao = '';
		erroTarefa = null;
		modalTarefa = true;
	}

	async function confirmarTarefa(): Promise<void> {
		if (mutando || !detalhe) return;
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
			await atribuirTarefaFinal(detalhe.id, {
				title: tarefaTitulo.trim(),
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

	const inputCls =
		'w-full rounded-md border border-border bg-elevated px-3 py-2.5 text-sm text-ink placeholder:text-muted/60 focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/30 transition';
	const labelCls = 'mb-1 block text-xs font-medium text-muted';
	const btnPrimario =
		'inline-flex items-center gap-1.5 rounded-md bg-brand px-3.5 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi disabled:opacity-50';
	const btnSecundario =
		'inline-flex items-center gap-1.5 rounded-md border border-border bg-elevated px-3.5 py-2 text-sm font-medium text-ink transition hover:bg-elevated/70 disabled:opacity-50';
</script>

<svelte:head>
	<title>{detalhe?.title ?? 'Treinamento'} — Treinamentos — FabLab</title>
</svelte:head>

{#if carregando}
	<div class="space-y-5" aria-hidden="true">
		<div class="h-4 w-40 animate-pulse rounded bg-muted/20"></div>
		<div class="animate-pulse rounded-xl border border-border bg-surface p-5">
			<div class="h-6 w-1/2 rounded bg-muted/20"></div>
			<div class="mt-3 h-3 w-1/3 rounded bg-muted/20"></div>
		</div>
		<div class="grid grid-cols-2 gap-4 lg:grid-cols-4">
			{#each [0, 1, 2, 3] as i (i)}
				<div class="animate-pulse rounded-xl border border-border bg-elevated p-4">
					<div class="h-3 w-20 rounded bg-muted/20"></div>
					<div class="mt-2 h-7 w-14 rounded bg-muted/20"></div>
				</div>
			{/each}
		</div>
	</div>
{:else if naoEncontrado || (!detalhe && erroBloco)}
	<div class="rounded-xl border border-border bg-surface">
		<EmptyState
			icon="squares"
			title="Treinamento não encontrado"
			description="O treinamento pode ter sido removido ou o endereço está incorreto."
		>
			{#snippet children()}
				<a
					href="/pessoas/treinamentos"
					data-testid="training-detail-back"
					class={btnSecundario}
					aria-label="Voltar para a visão geral de treinamentos"
				>
					<Icon name="arrow-left" class="h-4 w-4" /> Voltar à visão geral
				</a>
			{/snippet}
		</EmptyState>
	</div>
{:else if detalhe && status}
	<div data-testid="training-detail" class="space-y-5">
		<a
			href="/pessoas/treinamentos"
			data-testid="training-detail-back"
			class="inline-flex w-fit items-center gap-1.5 text-xs font-medium text-muted transition hover:text-ink"
			aria-label="Voltar à visão geral de treinamentos"
		>
			<Icon name="arrow-left" class="h-3.5 w-3.5" /> Voltar à visão geral
		</a>

		{#if erroBloco}
			<ErrorBanner
				message="Não foi possível atualizar o treinamento"
				hint="Verifique sua conexão e tente novamente. Se persistir, contate o suporte."
				onRetry={tentarNovamente}
			/>
		{/if}

		<section aria-label="Resumo do treinamento" class="overflow-hidden rounded-xl border border-border bg-surface">
			<div class="flex flex-col gap-4 p-5 lg:flex-row lg:items-center">
				<div class="min-w-0 flex-1">
					<div class="flex flex-wrap items-center gap-3">
						<h2 data-testid="training-detail-title" class="text-xl font-semibold tracking-tight text-ink">
							{detalhe.title}
						</h2>
						<StatusBadge label={status.label} color={status.color} />
					</div>
					<div class="mt-2 flex flex-wrap items-center gap-x-4 gap-y-1.5 text-xs text-muted">
						{#if detalhe.machine}
							<span data-testid="training-detail-machine" class="inline-flex items-center gap-1.5">
								<Icon name="cube" class="h-3.5 w-3.5" />{detalhe.machine}
							</span>
						{/if}
						<span data-testid="training-detail-instructor" class="inline-flex items-center gap-1.5">
							<Avatar
								name={detalhe.instructor.name}
								initialsOverride={detalhe.instructor.initials}
								size="xs"
							/>
							{detalhe.instructor.name} · Instrutor(a)
						</span>
						{#if detalhe.group}
							<span>Grupo {detalhe.group.label}</span>
						{/if}
					</div>
				</div>
				<div class="flex shrink-0 items-center gap-2">
					{#if sessaoAtiva}
						<a
							href="/pessoas/treinamentos/sessao"
							data-testid="training-open-session"
							class={btnPrimario}
							aria-label="Abrir sessão de {detalhe.title}"
						>
							Abrir sessão
						</a>
					{/if}
				</div>
			</div>
			{#if erroSessao}
				<p role="alert" class="border-t border-warn/30 bg-warn/10 px-5 py-2 text-xs text-warn">
					Sessão atual indisponível: {erroSessao}
				</p>
			{/if}
		</section>

		<section aria-label="Indicadores" class="grid grid-cols-2 gap-4 lg:grid-cols-4">
			<div class="rounded-xl border border-border bg-elevated p-4">
				<p class="text-xs text-muted">Concluíram</p>
				<p class="mt-1 font-mono text-2xl font-semibold text-ink tabular-nums">
					{detalhe.doneCount ?? 0}<span class="text-base text-muted">/{detalhe.groupSize ?? 0}</span>
				</p>
			</div>
			<div class="rounded-xl border border-border bg-elevated p-4">
				<p class="text-xs text-muted">Nota média</p>
				<p class="mt-1 font-mono text-2xl font-semibold text-success tabular-nums">
					{detalhe.average !== undefined ? fmtMedia.format(detalhe.average) : '—'}
				</p>
			</div>
			<div class="rounded-xl border border-border bg-elevated p-4">
				<p class="text-xs text-muted">Turma presente</p>
				<p class="mt-1 font-mono text-2xl font-semibold text-ink tabular-nums">
					{sessao ? sessao.present : '—'}
				</p>
				<p data-testid="training-detail-session" class="mt-0.5 text-xs text-muted">
					{sessao ? 'sessão em andamento' : 'sem sessão ativa'}
				</p>
			</div>
			<div class="rounded-xl border border-border bg-elevated p-4">
				<p class="text-xs text-muted">Próxima turma</p>
				<p class="mt-1 font-mono text-2xl font-semibold text-ink tabular-nums">
					{detalhe.nextClassAt ? formatarData(detalhe.nextClassAt) : '—'}
				</p>
			</div>
		</section>

		<div class="grid gap-5 lg:grid-cols-3">
			<div class="space-y-5 lg:col-span-2">
				<section
					data-testid="training-detail-info"
					aria-label="Informações do treinamento"
					class="rounded-xl border border-border bg-surface p-5"
				>
					<h3 class="text-sm font-semibold text-ink">Informações</h3>
					<dl class="mt-3 grid grid-cols-2 gap-x-4 gap-y-3 text-sm">
						<div>
							<dt class="text-xs font-medium text-muted">Trilha</dt>
							<dd class="mt-0.5 text-ink">{detalhe.track ?? '—'}</dd>
						</div>
						<div>
							<dt class="text-xs font-medium text-muted">Grupo</dt>
							<dd class="mt-0.5 text-ink">{detalhe.group?.label ?? '—'}</dd>
						</div>
						<div>
							<dt class="text-xs font-medium text-muted">Carga horária</dt>
							<dd class="mt-0.5 font-mono text-ink tabular-nums">{detalhe.load ?? '—'}</dd>
						</div>
						<div>
							<dt class="text-xs font-medium text-muted">Data de início</dt>
							<dd class="mt-0.5 font-mono text-ink tabular-nums">
								{formatarData(detalhe.startsAt)}
							</dd>
						</div>
						<div>
							<dt class="text-xs font-medium text-muted">Inscrições</dt>
							<dd class="mt-0.5 font-mono text-ink tabular-nums">
								{detalhe.doneCount ?? 0}/{detalhe.groupSize ?? 0}
							</dd>
						</div>
						<div>
							<dt class="text-xs font-medium text-muted">Instrutor(a)</dt>
							<dd class="mt-0.5 text-ink">{detalhe.instructor.name}</dd>
						</div>
					</dl>
				</section>

				<section
					data-testid="training-detail-agenda"
					aria-label="Agenda do treinamento"
					class="rounded-xl border border-border bg-surface p-5"
				>
					<h3 class="text-sm font-semibold text-ink">Agenda</h3>
					{#if !detalhe.agenda || detalhe.agenda.length === 0}
						<p class="mt-3 text-sm text-muted">Nenhuma sessão agendada.</p>
					{:else}
						<div class="mt-3 overflow-x-auto">
							<table class="w-full min-w-[520px] text-sm">
								<thead>
									<tr
										class="border-b border-border text-left text-[11px] uppercase tracking-wide text-muted"
									>
										<th class="py-2 pr-4 font-medium">Data/Horário</th>
										<th class="py-2 pr-4 font-medium">Sessão</th>
										<th class="py-2 pr-4 font-medium">Presença</th>
										<th class="py-2 font-medium">Status</th>
									</tr>
								</thead>
								<tbody>
									{#each detalhe.agenda as item (item.id)}
										{@const m = trainingStatusMeta(item.status)}
										<tr class="border-b border-border transition last:border-0 hover:bg-elevated/40">
											<td class="py-2.5 pr-4 font-mono text-xs text-muted tabular-nums">
												{formatarHorario(item.startsAt, item.endsAt)}
											</td>
											<td class="py-2.5 pr-4 text-ink">{item.title}</td>
											<td class="py-2.5 pr-4 font-mono text-xs text-muted tabular-nums">
												{item.present ?? '—'}
											</td>
											<td class="py-2.5">
												<StatusBadge label={m.label} color={m.color} />
											</td>
										</tr>
									{/each}
								</tbody>
							</table>
						</div>
					{/if}
				</section>
			</div>

			<div class="space-y-5">
				<section
					data-testid="training-detail-evaluation"
					aria-label="Avaliação do treinamento"
					class="rounded-xl border border-border bg-surface p-5"
				>
					<h3 class="text-sm font-semibold text-ink">Avaliação</h3>
					{#if !detalhe.evaluation || detalhe.evaluation.length === 0}
						<p class="mt-3 text-sm text-muted">Nenhum critério cadastrado.</p>
					{:else}
						<ul class="mt-3 space-y-2 text-sm">
							{#each detalhe.evaluation as c (c.criterion)}
								<li class="flex items-center justify-between gap-2">
									<span class="text-ink">{c.criterion}</span>
									<span class="font-mono text-xs text-muted tabular-nums">peso {c.weight}</span>
								</li>
							{/each}
						</ul>
					{/if}
					{#if podeAvaliar}
						<div class="mt-4 flex flex-col gap-2">
							<button
								type="button"
								onclick={abrirAvaliar}
								class={btnPrimario}
								aria-label="Avaliar treinamento {detalhe.title}"
							>
								Avaliar treinamento
							</button>
							<button
								type="button"
								onclick={abrirTarefa}
								class={btnSecundario}
								aria-label="Atribuir tarefa final em {detalhe.title}"
							>
								Atribuir tarefa final
							</button>
						</div>
					{/if}
				</section>

				{#if detalhe.finalTask}
					<section aria-label="Tarefa final" class="rounded-xl border border-border bg-surface p-5">
						<h3 class="text-sm font-semibold text-ink">Tarefa final</h3>
						<p class="mt-2 text-sm text-ink">{detalhe.finalTask.title}</p>
						<p class="mt-1 font-mono text-xs text-muted tabular-nums">
							Conclusão: {formatarData(detalhe.finalTask.dueDate)}
						</p>
					</section>
				{/if}
			</div>
		</div>
	</div>
{/if}

<Modal
	open={modalAvaliar}
	title="Avaliar treinamento"
	subtitle={detalhe ? detalhe.title : ''}
	onClose={() => (modalAvaliar = false)}
	width="sm"
>
	{#snippet children()}
		<div class="space-y-4">
			{#if erroAvaliar}
				<p role="alert" class="rounded-md border border-danger/30 bg-danger/10 px-3 py-2 text-xs text-danger">
					{erroAvaliar}
				</p>
			{/if}
			<div>
				<label for="ava-nota" class={labelCls}>
					Nota (0 a 10) <span class="text-danger" aria-hidden="true">*</span>
				</label>
				<input
					id="ava-nota"
					type="text"
					inputmode="decimal"
					bind:value={nota}
					placeholder="Ex.: 8,5"
					class="{inputCls} font-mono tabular-nums"
				/>
			</div>
			<div>
				<label for="ava-feedback" class={labelCls}>Feedback</label>
				<textarea
					id="ava-feedback"
					bind:value={feedback}
					rows="3"
					placeholder="Pontos fortes e a melhorar…"
					class="{inputCls} resize-y"
				></textarea>
			</div>
		</div>
	{/snippet}
	{#snippet footer()}
		<button
			type="button"
			onclick={() => (modalAvaliar = false)}
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
	open={modalTarefa}
	title="Atribuir tarefa final"
	subtitle={detalhe ? detalhe.title : ''}
	onClose={() => (modalTarefa = false)}
	width="sm"
>
	{#snippet children()}
		<div class="space-y-4">
			{#if erroTarefa}
				<p role="alert" class="rounded-md border border-danger/30 bg-danger/10 px-3 py-2 text-xs text-danger">
					{erroTarefa}
				</p>
			{/if}
			<div>
				<label for="tar-titulo" class={labelCls}>
					Título <span class="text-danger" aria-hidden="true">*</span>
				</label>
				<input
					id="tar-titulo"
					type="text"
					bind:value={tarefaTitulo}
					placeholder="Ex.: Imprimir suporte de celular"
					class={inputCls}
				/>
			</div>
			<div>
				<label for="tar-conclusao" class={labelCls}>
					Data de conclusão <span class="text-danger" aria-hidden="true">*</span>
				</label>
				<input
					id="tar-conclusao"
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
