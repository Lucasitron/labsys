<script lang="ts">
	import { onMount } from 'svelte';
	import type { Pendencia5S, PenalidadeTipo } from '$lib/types/producao';
	import {
		GRAVIDADE_META,
		PENDENCIA_STATUS_META,
		PENALIDADE_TIPO_META,
		gravidadeMeta
	} from '$lib/utils/producao-status';
	import {
		registrarPenalidadeParaPendencia5S,
		resolverPendencia5S
	} from '$lib/api/producao/client';
	import { ApiError, NetworkError } from '$lib/api/client';
	import Modal from '$lib/components/ui/Modal.svelte';

	export interface HistoricoResolucao {
		data: string;
		observacao: string;
	}

	interface Props {
		open: boolean;
		pendencia: Pendencia5S;
		itensNaoConformes?: string[];
		anexos?: string[];
		penalidadeSugerida?: PenalidadeTipo | null;
		historico?: HistoricoResolucao[];
		isAdmin: boolean;
		canResolve: boolean;
		onClose: () => void;
		onResolvida?: (pendencia: Pendencia5S) => void;
		onPenalidadeRegistrada?: (pendencia: Pendencia5S) => void;
	}

	let {
		open,
		pendencia,
		itensNaoConformes = [],
		anexos = [],
		penalidadeSugerida = null,
		historico = [],
		isAdmin,
		canResolve,
		onClose,
		onResolvida,
		onPenalidadeRegistrada
	}: Props = $props();

	let resolvendo = $state(false);
	let registrando = $state(false);
	let erro = $state('');
	let observacao = $state('');
	let tipoPenalidade = $state<PenalidadeTipo>('Aviso');
	let motivo = $state('');

	$effect(() => {
		if (open) {
			erro = '';
			observacao = '';
			motivo = '';
			tipoPenalidade = penalidadeSugerida ?? 'Aviso';
		}
	});

	onMount(() => {
		const onKey = (e: KeyboardEvent): void => {
			if (open && e.key === 'Escape') onClose();
		};
		document.addEventListener('keydown', onKey);
		return () => document.removeEventListener('keydown', onKey);
	});

	const hoje = $derived(new Date().toISOString().slice(0, 10));
	const prazoVencido = $derived(
		pendencia.status === 'Aberta' && !!pendencia.prazo && pendencia.prazo < hoje
	);
	const gravidadeMetaV = $derived(gravidadeMeta(pendencia.gravidade));
	const situacaoMeta = $derived(PENDENCIA_STATUS_META[pendencia.status]);

	const tiposPenalidade: PenalidadeTipo[] = ['Aviso', 'Advertência', 'Suspensão', 'Expulsão'];

	function formatarData(iso: string): string {
		if (!iso) return '—';
		const [a, m, d] = iso.slice(0, 10).split('-');
		return `${d}/${m}/${a}`;
	}

	function mensagemErro(err: unknown): string {
		if (err instanceof ApiError || err instanceof NetworkError) return err.message;
		return 'Não foi possível concluir a operação.';
	}

	async function resolver(event: SubmitEvent): Promise<void> {
		event.preventDefault();
		resolvendo = true;
		erro = '';
		try {
			const atualizada = await resolverPendencia5S(pendencia.id, {
				observacao: observacao.trim() || undefined
			});
			onResolvida?.(atualizada);
		} catch (err) {
			erro = mensagemErro(err);
		} finally {
			resolvendo = false;
		}
	}

	async function registrarPenalidade(event: SubmitEvent): Promise<void> {
		event.preventDefault();
		registrando = true;
		erro = '';
		try {
			const atualizada = await registrarPenalidadeParaPendencia5S(pendencia.id, {
				tipo: tipoPenalidade,
				motivo: motivo.trim()
			});
			onPenalidadeRegistrada?.(atualizada);
		} catch (err) {
			erro = mensagemErro(err);
		} finally {
			registrando = false;
		}
	}

	const gravidadeTones: Record<string, string> = {
		success: 'border-success/30 bg-success/15 text-success',
		warn: 'border-warn/30 bg-warn/15 text-warn',
		danger: 'border-danger/30 bg-danger/15 text-danger',
		brand: 'border-brand/30 bg-brand/15 text-brandhi',
		muted: 'border-border bg-elevated/40 text-muted',
		ink: 'border-border bg-elevated/40 text-ink'
	};

	const inputCls =
		'w-full rounded-lg border border-border bg-elevated px-3 py-2.5 text-sm text-ink placeholder:text-muted/60 focus:border-brand focus:outline-none focus:ring-1 focus:ring-brand/40';
</script>

<Modal
	{open}
	title={`Pendência · ${pendencia.titulo}`}
	subtitle={pendencia.setor.nome}
	{onClose}
	width="lg"
>
	{#snippet children()}
		{#if erro}
			<div role="alert" class="mb-4 rounded-xl border border-danger/30 bg-danger/10 px-4 py-3 text-sm text-danger">
				{erro}
			</div>
		{/if}

		<div class="mb-4 flex flex-wrap items-center gap-2 rounded-lg border border-border bg-elevated/30 p-3 text-xs">
			<span class="inline-flex items-center gap-1.5 rounded-full border px-2.5 py-0.5 font-semibold {gravidadeTones[gravidadeMetaV.color]}">
				{pendencia.gravidade}
			</span>
			<span class="inline-flex items-center gap-1.5 rounded-full border px-2.5 py-0.5 font-semibold {gravidadeTones[situacaoMeta.color]}">
				{pendencia.status}
			</span>
			{#if prazoVencido}
				<span class="ml-auto text-danger">
					Prazo vencido em {formatarData(pendencia.prazo)}
				</span>
			{/if}
		</div>

		<dl class="grid overflow-hidden rounded-lg border border-border text-sm sm:grid-cols-[150px_1fr]">
			<div class="bg-elevated/40 px-3 py-2 text-xs text-muted">Origem</div>
			<div class="px-3 py-2 text-xs">
				{pendencia.origem?.inspecaoId
					? `Auditoria ${pendencia.origem.inspecaoId} · ${formatarData(pendencia.origem.data)}`
					: 'Auditoria (referência)'}
			</div>
			<div class="bg-elevated/40 px-3 py-2 text-xs text-muted">Auditor</div>
			<div class="px-3 py-2 text-xs">{pendencia.origem?.auditor?.nome ?? '—'}</div>
			<div class="bg-elevated/40 px-3 py-2 text-xs text-muted">Setor</div>
			<div class="px-3 py-2 text-xs">{pendencia.setor.nome}</div>
			<div class="bg-elevated/40 px-3 py-2 text-xs text-muted">Responsável</div>
			<div class="px-3 py-2 text-xs">{pendencia.responsavel.nome}</div>
			<div class="bg-elevated/40 px-3 py-2 text-xs text-muted">Situação</div>
			<div class="px-3 py-2 text-xs">
				<span class="font-medium {situacaoMeta.color === 'success' ? 'text-success' : 'text-warn'}">{pendencia.status}</span>
			</div>
			<div class="bg-elevated/40 px-3 py-2 text-xs text-muted">Prazo</div>
			<div class={`px-3 py-2 text-xs ${prazoVencido ? 'font-medium text-danger' : ''}`}>
				{formatarData(pendencia.prazo)} · 5 dias úteis para resolver
			</div>
		</dl>

		{#if itensNaoConformes.length > 0}
			<div class="mt-4">
				<h3 class="mb-1.5 block text-xs font-medium text-muted">Itens não conformes</h3>
				<div class="flex flex-wrap gap-2">
					{#each itensNaoConformes as item}
						<span class="inline-block rounded-full border border-danger/30 bg-danger/5 px-3 py-1 text-xs text-danger">{item}</span>
					{/each}
				</div>
			</div>
		{/if}

		<div class="mt-4">
			<h3 class="mb-1.5 block text-xs font-medium text-muted">Fotos / anexos</h3>
			{#if anexos.length === 0}
				<div class="rounded-lg border border-border bg-elevated/40 p-4 text-center text-xs text-muted">
					Nenhum anexo registrado.
				</div>
			{:else}
				<div class="grid grid-cols-3 gap-3">
					{#each anexos as anexo, idx (anexo)}
						<div class="flex aspect-[4/3] items-center justify-center rounded-lg border border-border bg-elevated/40 text-center text-[10px] text-muted">
							{anexo}
							{#if idx === 0}<span class="sr-only">anexo</span>{/if}
						</div>
					{/each}
				</div>
				<p class="mt-1 text-[10px] text-muted">Placeholders de anexos — upload previsto para o contrato 🟡.</p>
			{/if}
		</div>

		{#if penalidadeSugerida}
			<div class="mt-4 flex flex-wrap items-center gap-2 rounded-lg border border-warn/30 bg-warn/10 p-3 text-xs text-muted">
				<span class="inline-flex items-center gap-1">
					<span class="h-2 w-2 rounded-full bg-warn" aria-hidden="true"></span>
					Penalidade sugerida:
				</span>
				<span class="inline-flex items-center gap-1.5 rounded-full border px-2.5 py-0.5 text-xs font-semibold {gravidadeTones[PENALIDADE_TIPO_META[penalidadeSugerida].color]}">
					{PENALIDADE_TIPO_META[penalidadeSugerida].label}
				</span>
			</div>
		{/if}

		<div class="mt-4">
			<h3 class="mb-1.5 block text-xs font-medium text-muted">Histórico da resolução</h3>
			{#if historico.length === 0}
				<div class="rounded-lg border border-border bg-elevated/30 p-3 text-xs text-muted">
					Nenhuma resolução registrada ainda.
				</div>
			{:else}
				<div class="space-y-2">
					{#each historico as h (h.data + h.observacao)}
						<div class="rounded-lg border border-border bg-elevated/30 p-3 text-xs text-muted">
							<span class="font-medium text-ink">{formatarData(h.data)}</span> — {h.observacao}
						</div>
					{/each}
				</div>
			{/if}
		</div>

		{#if pendencia.status === 'Aberta' && canResolve}
			<form id="pend-resolver-form" onsubmit={resolver} class="mt-4 rounded-xl border border-border bg-elevated/40 p-4">
				<h3 class="mb-1.5 block text-xs font-medium text-muted">Resolver pendência</h3>
				<label class="block">
					<span class="mb-1 block text-xs text-muted">O que foi feito *</span>
					<textarea
						bind:value={observacao}
						rows="3"
						placeholder="Descreva a resolução…"
						class={inputCls}
					></textarea>
				</label>
				<button
					type="submit"
					data-testid="pend-resolver"
					disabled={resolvendo}
					class="mt-3 rounded-md bg-brand px-4 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi disabled:cursor-not-allowed disabled:opacity-50"
				>
					{resolvendo ? 'Resolvendo…' : 'Marcar como resolvida'}
				</button>
			</form>
		{/if}

		{#if isAdmin}
			<form id="pend-penalidade-form" onsubmit={registrarPenalidade} class="mt-4 rounded-xl border border-border bg-warn/5 p-4">
				<h3 class="mb-1.5 block text-xs font-medium text-muted">Registrar penalidade (Admin)</h3>
				<div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
					<label class="block">
						<span class="mb-1 block text-xs text-muted">Tipo de penalidade *</span>
						<select bind:value={tipoPenalidade} class={inputCls}>
							{#each tiposPenalidade as t}
								<option value={t}>{t}{t === 'Aviso' && penalidadeSugerida ? ' (sugerido)' : ''}</option>
							{/each}
						</select>
					</label>
				</div>
				<label class="mt-3 block">
					<span class="mb-1 block text-xs text-muted">Justificativa *</span>
					<textarea
						bind:value={motivo}
						rows="2"
						placeholder="Motivo registrado pelo Admin…"
						class={inputCls}
					></textarea>
				</label>
				<button
					type="submit"
					data-testid="pend-penalidade"
					disabled={registrando || motivo.trim().length === 0}
					class="mt-3 rounded-md border border-warn/40 bg-warn/15 px-4 py-2 text-sm font-medium text-warn transition hover:bg-warn/20 disabled:cursor-not-allowed disabled:opacity-50"
				>
					{registrando ? 'Aplicando…' : 'Aplicar penalidade'}
				</button>
			</form>
		{/if}
	{/snippet}

	{#snippet footer()}
		<button type="button" onclick={onClose} class="rounded-md px-4 py-2 text-sm text-muted transition hover:text-ink">
			Fechar
		</button>
	{/snippet}
</Modal>