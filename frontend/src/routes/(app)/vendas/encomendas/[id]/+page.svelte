<script lang="ts">
	import { goto, invalidateAll } from '$app/navigation';
	import type { PageProps } from './$types';
	import { get } from 'svelte/store';
	import { auth } from '$lib/stores/auth';
	import type { KanbanStatus, OrdemOrigem } from '$lib/types/vendas';
	import { moverKanban } from '$lib/api/vendas/encomendas';
	import { ApiError } from '$lib/api/client';
	import { canEditVendas } from '$lib/utils/permissions';
	import { toUserMessage } from '$lib/utils/errors';
	import { toasts } from '$lib/stores/toast';
	import { formatNumber } from '$lib/utils/format';
	import { formatDateBR, formatMoneyBRL } from '$lib/utils/vendas-format';
	import { kanbanStatusMeta } from '$lib/utils/vendas-status';
	import StatusBadge from '$lib/components/ui/StatusBadge.svelte';
	import TypeBadge from '$lib/components/ui/TypeBadge.svelte';
	import Tabs from '$lib/components/ui/Tabs.svelte';
	import Timeline, { type TimelineItem } from '$lib/components/ui/Timeline.svelte';
	import EmptyState from '$lib/components/ui/EmptyState.svelte';
	import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte';
	import Icon from '$lib/components/ui/Icon.svelte';
	import RowActions from '$lib/components/ui/RowActions.svelte';
	import SolicitarEdicaoModal from '../../components/SolicitarEdicaoModal.svelte';

	let { data }: PageProps = $props();

	type Detalhe = NonNullable<typeof data.encomenda>;

	const usuario = $derived(get(auth).user);
	const encomenda = $derived(data.encomenda);
	const naoEncontrado = $derived(data.notFound);
	const erroPagina = $derived(data.error);
	const id = $derived(data.id);
	const aba = $derived(data.tab);

	const podeEditar = $derived(
		encomenda ? canEditVendas(usuario, { createdBy: encomenda.createdBy }) : false
	);
	const meta = $derived(encomenda ? kanbanStatusMeta(encomenda.statusKanban) : null);
	const entregue = $derived(encomenda?.statusKanban === 'Entregue');

	const ETAPAS: KanbanStatus[] = ['Fila', 'Produção', 'Acabamento', 'Pronto', 'Entregue'];

	const destinos = $derived<KanbanStatus[]>(
		encomenda ? ETAPAS.filter((e) => e !== encomenda.statusKanban) : []
	);

	function voltar(): void {
		if (window.history.length > 1) window.history.back();
		else void goto('/vendas/encomendas');
	}

	function trocarAba(nova: string): void {
		const qs = new URLSearchParams(window.location.search);
		qs.set('tab', nova);
		void goto(`/vendas/encomendas/${id}?${qs.toString()}`);
	}

	function dataBR(iso: string | null | undefined): string {
		if (!iso || Number.isNaN(new Date(iso).getTime())) return '—';
		return formatDateBR(iso);
	}

	function diasPrevisao(iso: string): string {
		const tempo = new Date(iso).getTime();
		if (Number.isNaN(tempo)) return '—';
		const dias = Math.ceil((tempo - Date.now()) / 86_400_000);
		if (dias > 1) return `faltam ${formatNumber(dias)} dias`;
		if (dias === 1) return 'prevista para amanhã';
		if (dias === 0) return 'prevista para hoje';
		return `atrasada há ${formatNumber(Math.abs(dias))} ${Math.abs(dias) === 1 ? 'dia' : 'dias'}`;
	}

	function origemTone(origem: OrdemOrigem): 'brand' | 'success' | 'warn' | 'muted' {
		if (origem === 'Orçamento') return 'brand';
		if (origem === 'Venda direta') return 'success';
		if (origem === 'Marketplace') return 'warn';
		return 'muted';
	}

	function tempoNaEtapa(enc: Detalhe): string {
		return typeof enc.tempoNaEtapaDias === 'number'
			? `${formatNumber(enc.tempoNaEtapaDias)} ${enc.tempoNaEtapaDias === 1 ? 'dia' : 'dias'}`
			: '—';
	}

	const abas = [
		{ id: 'visao-geral', label: 'Visão geral' },
		{ id: 'itens', label: 'Itens' },
		{ id: 'historico', label: 'Histórico' }
	];

	let sugerirAberto = $state(false);
	let avancarAberto = $state(false);
	let avancando = $state(false);

	// ---- Avançar etapa (PUT /api/vendas/encomendas/{id}/kanban 🔴) ----

	async function avancar(destino: KanbanStatus): Promise<void> {
		avancarAberto = false;
		if (avancando || !encomenda) return;
		avancando = true;
		try {
			await moverKanban(id, { statusKanban: destino });
			toasts.success(`Encomenda ${encomenda.codigo} movida para ${destino}.`);
			await invalidateAll();
		} catch (err) {
			if (err instanceof ApiError && err.status === 409) {
				toasts.warn('Conflito de versão — atualize a tela.');
			} else {
				toasts.danger(toUserMessage(err).message);
			}
		} finally {
			avancando = false;
		}
	}

	// ---- Timeline do histórico servido (historico_status_encomenda) ----

	const timelineItens = $derived<TimelineItem[]>(
		(encomenda?.historico ?? []).map((h, i) => ({
			id: `${h.statusAnterior}-${h.statusNovo}-${i}`,
			tone: kanbanStatusMeta(h.statusNovo).color,
			title: `${h.statusAnterior} → ${h.statusNovo}`,
			...(h.observacao?.trim() ? { body: h.observacao } : {}),
			meta: `${h.responsavel} · ${dataBR(h.dataAlteracao)}`
		}))
	);

	const itens = $derived(encomenda?.itens ?? []);
</script>

<svelte:head>
	<title>{encomenda ? `${encomenda.codigo} — Encomendas` : 'Encomenda — FabLab'}</title>
</svelte:head>

<div class="space-y-4">
	<div class="flex flex-wrap items-center justify-between gap-2">
		<button
			type="button"
			onclick={voltar}
			class="inline-flex items-center gap-1.5 text-xs font-medium text-muted transition-colors hover:text-ink"
		>
			<Icon name="arrow-left" class="h-3.5 w-3.5" /> Voltar às encomendas
		</button>
		{#if encomenda && !naoEncontrado}
			<div class="flex flex-wrap items-center gap-2">
				{#if !entregue}
					<div class="relative">
						<button
							type="button"
							data-testid="enc-avancar"
							aria-label="Avançar etapa da encomenda"
							aria-expanded={avancarAberto}
							aria-haspopup="menu"
							disabled={avancando}
							onclick={() => (avancarAberto = !avancarAberto)}
							class="inline-flex items-center gap-1.5 rounded-md bg-brand px-3.5 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi disabled:opacity-50"
						>
							<Icon name="arrow-right" class="h-4 w-4" />
							{avancando ? 'Movendo…' : 'Avançar etapa'}
						</button>
						{#if avancarAberto}
							<div
								role="menu"
								aria-label={`Mover ${encomenda.codigo} para`}
								class="absolute top-full right-0 z-30 mt-1 min-w-48 rounded-md border border-border bg-surface p-1 shadow-2xl shadow-black/50"
							>
								{#each destinos as destino (destino)}
									<button
										type="button"
										role="menuitem"
										onclick={() => void avancar(destino)}
										class="block w-full rounded px-2.5 py-1.5 text-left text-xs text-ink transition hover:bg-elevated hover:text-brandhi"
									>
										Mover para {destino}
									</button>
								{/each}
							</div>
						{/if}
					</div>
				{/if}
				{#if !podeEditar}
					<button
						type="button"
						onclick={() => (sugerirAberto = true)}
						class="inline-flex items-center gap-1.5 rounded-md border border-border bg-elevated px-3 py-2 text-sm transition hover:border-brand/50 hover:text-brandhi"
					>
						<Icon name="document" class="h-3.5 w-3.5" /> Sugerir alteração
					</button>
				{/if}
				<RowActions
					label={`Ações da encomenda ${encomenda.codigo}`}
					actions={[{ id: 'ver-cliente', label: 'Ver cliente', icon: 'eye' }]}
					onSelect={(acao) => {
						if (acao === 'ver-cliente') void goto(`/vendas/clientes/${encomenda.cliente.id}`);
					}}
				/>
			</div>
		{/if}
	</div>

	{#if naoEncontrado}
		<div class="rounded-xl border border-border bg-surface">
			<EmptyState
				icon="box"
				title="Encomenda não encontrada"
				description="A encomenda pode ter sido excluída ou o endereço está incorreto."
			>
				{#snippet children()}
					<button
						type="button"
						onclick={voltar}
						class="rounded-md border border-border bg-elevated px-3 py-2 text-sm font-medium text-ink transition hover:border-brand/50 hover:text-brandhi"
					>
						Voltar ao Kanban
					</button>
				{/snippet}
			</EmptyState>
		</div>
	{:else if erroPagina || !encomenda || !meta}
		<ErrorBanner
			message="Não foi possível carregar a encomenda"
			hint="Verifique sua conexão e tente novamente. Se persistir, contate o suporte."
			onRetry={() => void goto(`/vendas/encomendas/${id}${window.location.search}`, { invalidateAll: true })}
			testid="enc-detalhe-retry"
		/>
	{:else}
		<!-- Hero -->
		<section
			class="overflow-hidden rounded-xl border border-border bg-surface"
			aria-label="Resumo da encomenda"
		>
			<div class="flex flex-col gap-3 p-5 sm:flex-row sm:items-center sm:justify-between">
				<div class="min-w-0">
					<div class="flex flex-wrap items-center gap-2">
						<h1 class="font-mono text-2xl font-semibold tracking-tight text-ink">
							{encomenda.codigo}
						</h1>
						<StatusBadge label={meta.label} color={meta.color} />
						<TypeBadge tone={origemTone(encomenda.origem)} label={encomenda.origem} />
					</div>
					<p class="mt-1.5 text-sm text-muted">
						Cliente
						<a
							href={`/vendas/clientes/${encomenda.cliente.id}`}
							class="font-medium text-brandhi hover:text-brand"
						>
							{encomenda.cliente.nome}
						</a>
						· previsão {dataBR(encomenda.previsao)}
					</p>
				</div>
				<div class="shrink-0">
					<p class="text-xs text-muted">Valor final</p>
					<p class="font-mono text-2xl font-semibold tabnums text-ink">
						{formatMoneyBRL(encomenda.valorFinal)}
					</p>
				</div>
			</div>
		</section>

		<!-- KPIs (valores servidos; previsão/dias é apresentação da data servida) -->
		<section class="grid grid-cols-2 gap-4 lg:grid-cols-4" aria-label="Indicadores">
			<div class="rounded-xl border border-border bg-elevated p-4">
				<p class="text-xs text-muted">Valor</p>
				<p class="mt-1 font-mono text-2xl font-semibold tabnums text-ink">
					{formatMoneyBRL(encomenda.valorFinal)}
				</p>
				<p class="mt-0.5 text-xs text-muted">valor final</p>
			</div>
			<div class="rounded-xl border border-border bg-elevated p-4">
				<p class="text-xs text-muted">Previsão</p>
				<p class="mt-1 font-mono text-2xl font-semibold tabnums text-ink">
					{dataBR(encomenda.previsao)}
				</p>
				<p class="mt-0.5 text-xs text-muted">{diasPrevisao(encomenda.previsao)}</p>
			</div>
			<div class="rounded-xl border border-border bg-elevated p-4">
				<p class="text-xs text-muted">Itens</p>
				<p class="mt-1 font-mono text-2xl font-semibold tabnums text-ink">
					{formatNumber(encomenda.itensCount)}
				</p>
				<p class="mt-0.5 text-xs text-muted">na encomenda</p>
			</div>
			<div class="rounded-xl border border-border bg-elevated p-4">
				<p class="text-xs text-muted">Tempo na etapa</p>
				<p class="mt-1 font-mono text-2xl font-semibold tabnums text-ink">
					{tempoNaEtapa(encomenda)}
				</p>
				<p class="mt-0.5 text-xs text-muted">etapa {encomenda.statusKanban}</p>
			</div>
		</section>

		<!-- Abas -->
		<section class="overflow-hidden rounded-xl border border-border bg-surface" aria-label="Detalhes da encomenda">
			<div class="border-b border-border px-3 py-2" data-testid="enc-tabs">
				<Tabs tabs={abas} active={aba} onChange={trocarAba} />
			</div>

			{#if aba === 'visao-geral'}
				<div class="grid gap-4 p-5 lg:grid-cols-2">
					<div class="space-y-4">
						<div>
							<h2 class="text-xs font-medium uppercase tracking-wide text-muted">Cliente</h2>
							<a
								href={`/vendas/clientes/${encomenda.cliente.id}`}
								class="mt-2 block rounded-lg border border-border bg-elevated/50 p-3 transition hover:border-brand/50"
							>
								<p class="truncate text-sm font-medium text-ink">{encomenda.cliente.nome}</p>
								<p class="mt-0.5 font-mono text-xs text-muted">ver cadastro</p>
							</a>
						</div>
						<div>
							<h2 class="text-xs font-medium uppercase tracking-wide text-muted">Orçamento de origem</h2>
							{#if encomenda.orcamentoId}
								<a
									href={`/vendas/orcamentos/${encomenda.orcamentoId}`}
									class="mt-2 block rounded-lg border border-border bg-elevated/50 p-3 transition hover:border-brand/50"
								>
									<p class="font-mono text-sm font-medium text-ink">
										{encomenda.orcamentoCodigo ?? encomenda.orcamentoId.slice(0, 8)}
									</p>
									<p class="mt-0.5 font-mono text-xs text-muted">ver orçamento</p>
								</a>
							{:else}
								<p class="mt-2 rounded-lg border border-border bg-elevated/50 p-3 text-sm text-muted">
									Venda direta — sem orçamento vinculado.
								</p>
							{/if}
						</div>
					</div>
					<div class="space-y-4">
						<div>
							<h2 class="text-xs font-medium uppercase tracking-wide text-muted">Observações</h2>
							<p class="mt-2 rounded-lg border border-border bg-elevated/50 p-3 text-sm text-muted">
								{encomenda.observacoes?.trim() ? encomenda.observacoes : '—'}
							</p>
						</div>
						<div>
							<h2 class="text-xs font-medium uppercase tracking-wide text-muted">Marketplace vinculado</h2>
							{#if encomenda.marketplace}
								<p class="mt-2 rounded-lg border border-border bg-elevated/50 p-3 text-sm text-muted">
									{encomenda.marketplace.plataforma} ·
									<span class="font-mono text-xs">{encomenda.marketplace.codigoExterno}</span>
								</p>
							{:else}
								<p class="mt-2 rounded-lg border border-border bg-elevated/50 p-3 text-sm text-muted">
									Sem venda externa vinculada.
								</p>
							{/if}
						</div>
					</div>
				</div>
			{:else if aba === 'itens'}
				<div class="p-5">
					{#if itens.length === 0}
						<EmptyState
							icon="box"
							title="Sem itens detalhados"
							description="O servidor não retornou o detalhamento de itens desta encomenda."
						/>
					{:else}
						<div class="overflow-x-auto rounded-lg border border-border">
							<table class="w-full min-w-[520px] text-sm">
								<thead>
									<tr
										class="border-b border-border bg-elevated/50 text-left text-[11px] uppercase tracking-wide text-muted"
									>
										<th class="px-4 py-2.5 font-medium">Descrição</th>
										<th class="px-4 py-2.5 text-right font-medium">Qtd</th>
										<th class="px-4 py-2.5 text-right font-medium">Unitário</th>
										<th class="px-4 py-2.5 text-right font-medium">Subtotal</th>
									</tr>
								</thead>
								<tbody>
									{#each itens as item, i (i)}
										<tr class="border-b border-border transition last:border-0 hover:bg-elevated/40">
											<td class="px-4 py-3 font-medium text-ink">{item.descricao}</td>
											<td class="px-4 py-3 text-right font-mono text-sm tabnums text-ink">
												{formatNumber(item.quantidade)}
											</td>
											<td class="px-4 py-3 text-right font-mono text-sm tabnums text-ink">
												{formatMoneyBRL(item.valorUnitario)}
											</td>
											<td class="px-4 py-3 text-right font-mono text-sm font-medium tabnums text-ink">
												{formatMoneyBRL(item.quantidade * item.valorUnitario)}
											</td>
										</tr>
									{/each}
								</tbody>
							</table>
						</div>
					{/if}
				</div>
			{:else}
				<div class="p-5" data-testid="enc-tab-historico">
					{#if timelineItens.length === 0}
						<EmptyState
							icon="clock"
							title="Sem movimentações"
							description="Nenhuma mudança de etapa registrada para esta encomenda ainda."
						/>
					{:else}
						<Timeline items={timelineItens} label="Histórico da encomenda" />
					{/if}
				</div>
			{/if}
		</section>
	{/if}
</div>

{#if sugerirAberto && encomenda}
	<SolicitarEdicaoModal
		alvo={{ tipo: 'EN', id, nome: encomenda.codigo }}
		onClose={() => (sugerirAberto = false)}
	/>
{/if}
