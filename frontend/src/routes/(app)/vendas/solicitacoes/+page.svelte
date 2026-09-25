<script lang="ts">
	import { goto, invalidateAll } from '$app/navigation';
	import type { PageProps } from './$types';
	import { get } from 'svelte/store';
	import { auth } from '$lib/stores/auth';
	import type { SolicitacaoEdicao, SolicitacaoStatus } from '$lib/types/vendas';
	import { decidirSolicitacao } from '$lib/api/vendas/solicitacoes';
	import { ApiError } from '$lib/api/client';
	import { canDecideVendas } from '$lib/utils/permissions';
	import { toUserMessage } from '$lib/utils/errors';
	import { toasts } from '$lib/stores/toast';
	import { formatNumber } from '$lib/utils/format';
	import PageHeader from '$lib/components/ui/PageHeader.svelte';
	import Avatar from '$lib/components/ui/Avatar.svelte';
	import StatusBadge from '$lib/components/ui/StatusBadge.svelte';
	import EmptyState from '$lib/components/ui/EmptyState.svelte';
	import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte';
	import Icon from '$lib/components/ui/Icon.svelte';
	import Modal from '$lib/components/ui/Modal.svelte';

	let { data }: PageProps = $props();

	const usuario = $derived(get(auth).user);
	const params = $derived(data.params);
	const resultado = $derived(data.resultado);
	const erro = $derived(data.error);

	const solicitacoes = $derived<SolicitacaoEdicao[]>(resultado?.solicitacoes ?? []);
	const contagens = $derived<Record<string, number>>(resultado?.counts ?? {});

	const carregando = $derived(resultado === null && erro === null);
	const comErro = $derived(erro !== null && resultado === null);

	const ABAS = [
		{ id: 'pendentes', label: 'Pendentes' },
		{ id: 'aprovadas', label: 'Aprovadas' },
		{ id: 'rejeitadas', label: 'Rejeitadas' }
	];

	function numero(valor: unknown): number | null {
		const n = Number(valor);
		return Number.isFinite(n) ? n : null;
	}

	function contagemAba(id: string): number | null {
		switch (id) {
			case 'pendentes':
				return numero(contagens['Pendentes'] ?? contagens['Pendente']);
			case 'aprovadas':
				return numero(contagens['Aprovadas'] ?? contagens['Aprovada']);
			case 'rejeitadas':
				return numero(contagens['Rejeitadas'] ?? contagens['Rejeitada']);
			default:
				return null;
		}
	}

	// KPIs servidos pelo backend (nunca derivados no client).
	function kpi(chaves: string[]): string {
		for (const chave of chaves) {
			const n = numero(contagens[chave]);
			if (n !== null) return formatNumber(n);
		}
		return '—';
	}

	function statusMeta(status: SolicitacaoStatus): { label: string; color: 'warn' | 'success' | 'danger' } {
		if (status === 'Aprovada') return { label: 'Aprovada', color: 'success' };
		if (status === 'Rejeitada') return { label: 'Rejeitada', color: 'danger' };
		return { label: 'Pendente', color: 'warn' };
	}

	function alvoTone(tipo: string): 'brand' | 'warn' | 'success' | 'muted' {
		if (tipo === 'CLI') return 'brand';
		if (tipo === 'OC') return 'warn';
		if (tipo === 'EN') return 'success';
		return 'muted';
	}

	const somentePendentes = $derived(params.tab === 'pendentes');

	function trocarAba(id: string): void {
		const qs = id === 'pendentes' ? '' : `?tab=${id}`;
		void goto(`/vendas/solicitacoes${qs}`);
	}

	function tentarNovamente(): void {
		void goto(`/vendas/solicitacoes${window.location.search}`, { invalidateAll: true });
	}

	// ---- Modal de decisão (PUT /api/vendas/solicitacoes/{id} 🟡) ----

	let alvo = $state<SolicitacaoEdicao | null>(null);
	let aprovada = $state(true);
	let motivo = $state('');
	let erroMotivo = $state('');
	let ocupado = $state(false);

	function abrirDecisao(solicitacao: SolicitacaoEdicao, decisao: boolean): void {
		alvo = solicitacao;
		aprovada = decisao;
		motivo = '';
		erroMotivo = '';
	}

	function fecharDecisao(): void {
		if (ocupado) return;
		alvo = null;
	}

	async function confirmarDecisao(): Promise<void> {
		if (!alvo || ocupado) return;
		if (!canDecideVendas(usuario)) {
			toasts.warn('Sem permissão para esta ação.');
			return;
		}
		if (!aprovada && !motivo.trim()) {
			erroMotivo = 'Informe o motivo da rejeição.';
			return;
		}
		ocupado = true;
		try {
			await decidirSolicitacao(alvo.id, {
				aprovada,
				...(aprovada ? {} : { motivo: motivo.trim() })
			});
			toasts.success(aprovada ? 'Solicitação aprovada.' : 'Solicitação rejeitada.');
			alvo = null;
			await invalidateAll();
		} catch (err) {
			if (err instanceof ApiError && err.status === 403) {
				toasts.warn('Sem permissão para esta ação.');
			} else if (err instanceof ApiError && err.status === 400) {
				erroMotivo = 'Informe o motivo da rejeição.';
			} else {
				toasts.danger(toUserMessage(err).message);
			}
		} finally {
			ocupado = false;
		}
	}

	const inputCls =
		'w-full rounded-md border border-border bg-elevated px-3 py-2.5 text-sm text-ink placeholder:text-muted/60 focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/30 transition';
	const labelCls = 'mb-1 block text-xs font-medium text-muted';
</script>

<svelte:head>
	<title>Solicitações de edição — Vendas — FabLab</title>
</svelte:head>

<div class="space-y-4">
	<PageHeader
		title="Solicitações de edição"
		subtitle="Pedidos de alteração liberados pelo Admin ou responsável de Vendas."
	/>

	{#if comErro}
		<ErrorBanner
			message="Não foi possível carregar as solicitações"
			hint="Verifique sua conexão e tente novamente. Se persistir, contate o suporte."
			onRetry={tentarNovamente}
			testid="sol-retry"
		/>
	{:else if carregando}
		<div class="grid grid-cols-2 gap-4 lg:grid-cols-4" aria-hidden="true">
			{#each [1, 2, 3, 4] as i (i)}
				<div class="h-24 animate-pulse rounded-xl border border-border bg-elevated"></div>
			{/each}
		</div>
		<div class="h-10 animate-pulse rounded-lg bg-elevated"></div>
		<div class="space-y-3">
			{#each [1, 2, 3] as i (i)}
				<div class="h-36 animate-pulse rounded-xl border border-border bg-surface"></div>
			{/each}
		</div>
	{:else}
		<!-- KPIs servidos pelo backend (nunca derivados no client) -->
		<section class="grid grid-cols-2 gap-4 lg:grid-cols-4" aria-label="Indicadores">
			<div class="rounded-xl border border-border bg-elevated p-4">
				<p class="text-xs text-muted">Pendentes de decisão</p>
				<p class="mt-1 font-mono text-2xl font-semibold tabnums text-ink">
					{kpi(['Pendentes', 'Pendente'])}
				</p>
				<p class="mt-0.5 text-xs text-muted">aguardando análise</p>
			</div>
			<div class="rounded-xl border border-border bg-elevated p-4">
				<p class="text-xs text-muted">Aprovadas no mês</p>
				<p class="mt-1 font-mono text-2xl font-semibold tabnums text-ink">
					{kpi(['Aprovadas no mês', 'Aprovadas/mês', 'Aprovadas', 'Aprovada'])}
				</p>
				<p class="mt-0.5 text-xs text-muted">liberadas</p>
			</div>
			<div class="rounded-xl border border-border bg-elevated p-4">
				<p class="text-xs text-muted">Rejeitadas no mês</p>
				<p class="mt-1 font-mono text-2xl font-semibold tabnums text-ink">
					{kpi(['Rejeitadas no mês', 'Rejeitadas/mês', 'Rejeitadas', 'Rejeitada'])}
				</p>
				<p class="mt-0.5 text-xs text-muted">recusadas</p>
			</div>
			<div class="rounded-xl border border-border bg-elevated p-4">
				<p class="text-xs text-muted">Para revisar hoje</p>
				<p class="mt-1 font-mono text-2xl font-semibold tabnums text-ink">
					{kpi(['Revisar hoje', 'Para revisar hoje', 'Hoje'])}
				</p>
				<p class="mt-0.5 text-xs text-muted">prioridade do dia</p>
			</div>
		</section>

		<div
			role="tablist"
			aria-label="Filtrar solicitações por situação"
			class="flex items-center gap-1 overflow-x-auto border-b border-border px-1"
		>
			{#each ABAS as aba (aba.id)}
				{@const count = contagemAba(aba.id)}
				{@const ativa = params.tab === aba.id}
				<button
					type="button"
					role="tab"
					aria-selected={ativa}
					data-testid="sol-tab-{aba.id}"
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

		{#if solicitacoes.length === 0}
			<div class="rounded-xl border border-border bg-surface">
				<EmptyState
					icon="document"
					title={somentePendentes
						? 'Nenhuma solicitação pendente'
						: 'Nenhuma solicitação nesta situação'}
					description={somentePendentes
						? 'Quando alguém sugerir alteração em um registro, o pedido aparece aqui para decisão.'
						: 'Nenhuma solicitação decidida aparece nesta aba.'}
				/>
			</div>
		{:else}
			<ul class="space-y-3" aria-label="Solicitações">
				{#each solicitacoes as solicitacao (solicitacao.id)}
					{@const meta = statusMeta(solicitacao.status)}
					{@const pendente = solicitacao.status === 'Pendente'}
					<li
						class="rounded-xl border border-border bg-elevated/30 p-4"
					>
						<div class="flex flex-wrap items-start justify-between gap-3">
							<div class="flex min-w-0 items-center gap-3">
								<Avatar name={solicitacao.solicitante} size="sm" tone="brand" />
								<div class="min-w-0">
									<p class="truncate text-sm font-medium text-ink">
										{solicitacao.solicitante}
									</p>
									<p class="text-xs text-muted">solicitou uma alteração</p>
								</div>
							</div>
							<div class="flex flex-wrap items-center gap-2">
								<StatusBadge
									label="{solicitacao.alvo.tipo} · {solicitacao.alvo.nome}"
									color={alvoTone(solicitacao.alvo.tipo)}
								/>
								<StatusBadge label={meta.label} color={meta.color} />
							</div>
						</div>

						<p class="mt-3 text-sm text-ink">{solicitacao.tipo}</p>
						{#if solicitacao.campo}
							<p class="mt-0.5 text-xs text-muted">Campo: {solicitacao.campo}</p>
						{/if}
						<p class="mt-2 rounded-md border border-border bg-surface px-3 py-2 font-mono text-xs text-muted">
							{solicitacao.valorAtual || '—'} → {solicitacao.valorProposto || '—'}
						</p>
						<p class="mt-2 text-xs text-muted">Justificativa: {solicitacao.justificativa}</p>

						{#if pendente}
							<div class="mt-3 flex flex-wrap items-center gap-2">
								<button
									type="button"
									onclick={() => abrirDecisao(solicitacao, true)}
									data-testid="sol-aprovar"
									aria-label="Aprovar solicitação de {solicitacao.solicitante}"
									class="inline-flex items-center gap-1.5 rounded-md bg-success/15 border border-success/30 px-3 py-2 text-sm font-medium text-success transition hover:bg-success/25"
								>
									<Icon name="check" class="h-4 w-4" /> Aprovar
								</button>
								<button
									type="button"
									onclick={() => abrirDecisao(solicitacao, false)}
									data-testid="sol-rejeitar"
									aria-label="Rejeitar solicitação de {solicitacao.solicitante}"
									class="inline-flex items-center gap-1.5 rounded-md bg-danger/15 border border-danger/30 px-3 py-2 text-sm font-medium text-danger transition hover:bg-danger/25"
								>
									<Icon name="x-mark" class="h-4 w-4" /> Rejeitar
								</button>
							</div>
						{:else}
							<div class="mt-3 rounded-md border border-border bg-surface px-3 py-2">
								<p class="text-xs text-muted">
									Decidida por {solicitacao.decididoPor ?? '—'}{solicitacao.decididoEm
										? ` · ${solicitacao.decididoEm}`
										: ''}
									{#if solicitacao.motivo}
										<span class="mt-0.5 block text-ink">Motivo: {solicitacao.motivo}</span>
									{/if}
								</p>
							</div>
						{/if}
					</li>
				{/each}
			</ul>
		{/if}
	{/if}
</div>

<Modal
	open={alvo !== null}
	title={aprovada ? 'Aprovar solicitação' : 'Rejeitar solicitação'}
	subtitle={alvo ? `${alvo.tipo} · ${alvo.alvo.tipo} ${alvo.alvo.nome}` : ''}
	onClose={fecharDecisao}
>
	{#snippet children()}
		{#if alvo}
			<div data-testid="modal-decisao-solicitacao" class="space-y-4">
				<div class="rounded-md border border-border bg-elevated px-3 py-2.5">
					<p class="text-xs text-muted">Solicitante</p>
					<p class="text-sm font-medium text-ink">{alvo.solicitante}</p>
					<p class="mt-2 font-mono text-xs text-muted">
						{alvo.valorAtual || '—'} → {alvo.valorProposto || '—'}
					</p>
					<p class="mt-1 text-xs text-muted">Justificativa: {alvo.justificativa}</p>
				</div>

				<fieldset>
					<legend class="{labelCls} mb-2">Decisão <span class="text-danger">*</span></legend>
					<div class="flex flex-wrap gap-2" role="radiogroup" aria-label="Decisão">
						<label
							class="inline-flex cursor-pointer items-center gap-1.5 rounded-full border px-3 py-1.5 text-xs font-medium transition {aprovada
								? 'border-success bg-success/10 text-success'
								: 'border-border bg-surface text-muted hover:text-ink'}"
						>
							<input
								type="radio"
								name="sol-decisao"
								checked={aprovada}
								onchange={() => {
									aprovada = true;
									erroMotivo = '';
								}}
								class="sr-only"
							/>
							Aprovar
						</label>
						<label
							class="inline-flex cursor-pointer items-center gap-1.5 rounded-full border px-3 py-1.5 text-xs font-medium transition {!aprovada
								? 'border-danger bg-danger/10 text-danger'
								: 'border-border bg-surface text-muted hover:text-ink'}"
						>
							<input
								type="radio"
								name="sol-decisao"
								checked={!aprovada}
								onchange={() => (aprovada = false)}
								class="sr-only"
							/>
							Rejeitar
						</label>
					</div>
				</fieldset>

				{#if !aprovada}
					<div>
						<label for="sol-motivo" class={labelCls}>Motivo <span class="text-danger">*</span></label>
						<textarea
							id="sol-motivo"
							value={motivo}
							oninput={(e) => {
								motivo = (e.currentTarget as HTMLTextAreaElement).value;
								if (erroMotivo && motivo.trim()) erroMotivo = '';
							}}
							rows={3}
							placeholder="Explique por que a solicitação foi rejeitada…"
							aria-invalid={erroMotivo !== ''}
							class="{inputCls} resize-y {erroMotivo ? 'border-danger/60 focus:border-danger focus:ring-danger/30' : ''}"
						></textarea>
						{#if erroMotivo}
							<p role="alert" class="mt-1 text-xs text-danger">{erroMotivo}</p>
						{/if}
					</div>
				{/if}
			</div>
		{/if}
	{/snippet}
	{#snippet footer()}
		<button
			type="button"
			onclick={fecharDecisao}
			disabled={ocupado}
			class="rounded-md border border-border bg-elevated px-3.5 py-2 text-sm font-medium text-ink transition hover:bg-elevated/70 disabled:opacity-50"
		>
			Cancelar
		</button>
		<button
			type="button"
			onclick={confirmarDecisao}
			disabled={ocupado}
			data-testid="sol-decidir"
			aria-label={aprovada ? 'Confirmar aprovação' : 'Confirmar rejeição'}
			class="rounded-md bg-brand px-3.5 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi disabled:opacity-50"
		>
			{ocupado ? 'Decidindo…' : aprovada ? 'Confirmar aprovação' : 'Confirmar rejeição'}
		</button>
	{/snippet}
</Modal>
