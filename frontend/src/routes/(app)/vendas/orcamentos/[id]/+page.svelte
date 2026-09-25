<script lang="ts">
	import { goto, invalidateAll } from '$app/navigation';
	import type { PageProps } from './$types';
	import { get } from 'svelte/store';
	import { auth } from '$lib/stores/auth';
	import { createEncomenda } from '$lib/api/vendas/encomendas';
	import { ApiError } from '$lib/api/client';
	import { canEditVendas } from '$lib/utils/permissions';
	import { toUserMessage } from '$lib/utils/errors';
	import { toasts } from '$lib/stores/toast';
	import { formatNumber } from '$lib/utils/format';
	import { formatDateBR, formatMoneyBRL } from '$lib/utils/vendas-format';
	import { orcamentoStatusMeta } from '$lib/utils/vendas-status';
	import StatusBadge from '$lib/components/ui/StatusBadge.svelte';
	import TypeBadge from '$lib/components/ui/TypeBadge.svelte';
	import Timeline, { type TimelineItem } from '$lib/components/ui/Timeline.svelte';
	import EmptyState from '$lib/components/ui/EmptyState.svelte';
	import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte';
	import Icon from '$lib/components/ui/Icon.svelte';
	import RowActions from '$lib/components/ui/RowActions.svelte';
	import SolicitarEdicaoModal from '../../components/SolicitarEdicaoModal.svelte';

	let { data }: PageProps = $props();

	const usuario = $derived(get(auth).user);
	const orcamento = $derived(data.orcamento);
	const naoEncontrado = $derived(data.notFound);
	const erroPagina = $derived(data.error);
	const id = $derived(data.id);

	const podeEditar = $derived(
		orcamento ? canEditVendas(usuario, { createdBy: orcamento.createdBy }) : false
	);
	const meta = $derived(orcamento ? orcamentoStatusMeta(orcamento.status) : null);

	// Banner de conversão: só Aprovado sem encomenda (estado servido; some via refetch).
	const mostraConversao = $derived(
		orcamento !== null && orcamento.status === 'Aprovado' && !orcamento.encomendaId
	);

	function voltar(): void {
		if (window.history.length > 1) window.history.back();
		else void goto('/vendas/orcamentos');
	}

	function dataBR(iso: string | null | undefined): string {
		if (!iso || Number.isNaN(new Date(iso).getTime())) return '—';
		return formatDateBR(iso);
	}

	function diasValidade(iso: string): string {
		const tempo = new Date(iso).getTime();
		if (Number.isNaN(tempo)) return '—';
		const dias = Math.ceil((tempo - Date.now()) / 86_400_000);
		if (dias > 1) return `faltam ${formatNumber(dias)} dias`;
		if (dias === 1) return 'vence amanhã';
		if (dias === 0) return 'vence hoje';
		return `vencido há ${formatNumber(Math.abs(dias))} ${Math.abs(dias) === 1 ? 'dia' : 'dias'}`;
	}

	function duplicarIndisponivel(): void {
		toasts.warn('Duplicação indisponível — backend pendente (D-2).');
	}

	let sugerirAberto = $state(false);

	// ---- Conversão em encomenda (POST /api/vendas/encomendas {idOrcamento} 🔴) ----

	let convertendo = $state(false);

	async function criarEncomendaDoOrcamento(): Promise<void> {
		if (!orcamento) return;
		if (!canEditVendas(usuario, { createdBy: orcamento.createdBy })) {
			toasts.warn('Somente o criador ou Admin pode mover esta encomenda.');
			return;
		}
		if (convertendo) return;
		convertendo = true;
		try {
			const encomenda = await createEncomenda({ idOrcamento: id });
			toasts.success(`Encomenda ${encomenda.codigo} criada na Fila.`);
			await invalidateAll();
		} catch (err) {
			if (err instanceof ApiError && err.status === 403) {
				toasts.warn('Sem permissão para esta ação.');
			} else {
				toasts.danger(toUserMessage(err).message);
			}
		} finally {
			convertendo = false;
		}
	}

	// ---- Timeline do estado servido (backend não expõe histórico de orçamento) ----

	const timelineItens = $derived.by<TimelineItem[]>(() => {
		if (!orcamento || !meta) return [];
		const itensTimeline: TimelineItem[] = [
			{
				id: 'status-atual',
				tone: meta.color,
				title: `Status atual: ${orcamento.status}`,
				body: `Orçamento ${orcamento.codigo} de ${formatMoneyBRL(orcamento.valorTotal)}.`,
				meta: `Validade ${dataBR(orcamento.validade)}`
			}
		];
		if (orcamento.encomendaId) {
			itensTimeline.push({
				id: 'encomenda',
				tone: 'success',
				title: 'Encomenda criada',
				body: 'Convertido a partir deste orçamento aprovado.',
				meta: orcamento.encomendaId
			});
		}
		return itensTimeline;
	});
</script>

<svelte:head>
	<title>{orcamento ? `${orcamento.codigo} — Orçamentos` : 'Orçamento — FabLab'}</title>
</svelte:head>

<div class="space-y-4">
	<div class="flex flex-wrap items-center justify-between gap-2">
		<button
			type="button"
			onclick={voltar}
			class="inline-flex items-center gap-1.5 text-xs font-medium text-muted transition-colors hover:text-ink"
		>
			<Icon name="arrow-left" class="h-3.5 w-3.5" /> Voltar aos orçamentos
		</button>
		{#if orcamento && !naoEncontrado}
			<div class="flex items-center gap-2">
				{#if podeEditar}
					<a
						href={`/vendas/orcamentos/novo?editar=${id}`}
						class="inline-flex items-center gap-1.5 rounded-md border border-border bg-elevated px-3 py-2 text-sm transition hover:border-brand/50 hover:text-brandhi"
					>
						<Icon name="pencil" class="h-3.5 w-3.5" /> Editar
					</a>
				{:else}
					<button
						type="button"
						onclick={() => (sugerirAberto = true)}
						class="inline-flex items-center gap-1.5 rounded-md border border-border bg-elevated px-3 py-2 text-sm transition hover:border-brand/50 hover:text-brandhi"
					>
						<Icon name="document" class="h-3.5 w-3.5" /> Sugerir alteração
					</button>
				{/if}
				<RowActions
					label={`Ações do orçamento ${orcamento.codigo}`}
					actions={[
						...(orcamento.encomendaId
							? [{ id: 'ver-encomenda', label: 'Ver encomenda', icon: 'eye' as const }]
							: []),
						{ id: 'duplicar', label: 'Duplicar', icon: 'duplicate' }
					]}
					onSelect={(acao) => {
						if (acao === 'duplicar') duplicarIndisponivel();
						else if (acao === 'ver-encomenda' && orcamento.encomendaId)
							void goto(`/vendas/encomendas/${orcamento.encomendaId}`);
					}}
				/>
			</div>
		{/if}
	</div>

	{#if naoEncontrado}
		<div class="rounded-xl border border-border bg-surface">
			<EmptyState
				icon="document"
				title="Orçamento não encontrado"
				description="O orçamento pode ter sido excluído ou o endereço está incorreto."
			>
				{#snippet children()}
					<button
						type="button"
						onclick={voltar}
						class="rounded-md border border-border bg-elevated px-3 py-2 text-sm font-medium text-ink transition hover:border-brand/50 hover:text-brandhi"
					>
						Voltar à lista
					</button>
				{/snippet}
			</EmptyState>
		</div>
	{:else if erroPagina || !orcamento || !meta}
		<ErrorBanner
			message="Não foi possível carregar o orçamento"
			hint="Verifique sua conexão e tente novamente. Se persistir, contate o suporte."
			onRetry={() => void goto(`/vendas/orcamentos/${id}`, { invalidateAll: true })}
			testid="orc-detalhe-retry"
		/>
	{:else}
		<!-- Hero -->
		<section
			class="overflow-hidden rounded-xl border border-border bg-surface"
			aria-label="Resumo do orçamento"
		>
			<div class="flex flex-col gap-3 p-5 sm:flex-row sm:items-center sm:justify-between">
				<div class="min-w-0">
					<div class="flex flex-wrap items-center gap-2">
						<h1 class="font-mono text-2xl font-semibold tracking-tight text-ink">
							{orcamento.codigo}
						</h1>
						<StatusBadge label={meta.label} color={meta.color} />
					</div>
					<p class="mt-1.5 text-sm text-muted">
						Cliente
						<a
							href={`/vendas/clientes/${orcamento.cliente.id}`}
							class="font-medium text-brandhi hover:text-brand"
						>
							{orcamento.cliente.nome}
						</a>
						· validade {dataBR(orcamento.validade)}
					</p>
				</div>
				<div class="shrink-0">
					<p class="text-xs text-muted">Valor total</p>
					<p class="font-mono text-2xl font-semibold tabnums text-ink">
						{formatMoneyBRL(orcamento.valorTotal)}
					</p>
				</div>
			</div>
		</section>

		<!-- Banner de conversão (só Aprovado sem encomenda) -->
		{#if mostraConversao && podeEditar}
			<section
				aria-label="Converter orçamento em encomenda"
				class="flex flex-col gap-3 rounded-xl border border-brand/40 bg-brand/10 p-5 sm:flex-row sm:items-center sm:justify-between"
			>
				<div>
					<h2 class="text-sm font-semibold text-ink">Orçamento aprovado</h2>
					<p class="mt-0.5 text-xs text-muted">
						Converta em encomenda para iniciar a produção. A encomenda nasce na Fila.
					</p>
				</div>
				<button
					type="button"
					data-testid="orc-criar-encomenda"
					onclick={() => void criarEncomendaDoOrcamento()}
					disabled={convertendo}
					class="inline-flex shrink-0 items-center gap-1.5 rounded-md bg-brand px-4 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi disabled:opacity-50"
				>
					<Icon name="plus" class="h-4 w-4" />
					{convertendo ? 'Criando…' : 'Aprovar e criar encomenda'}
				</button>
			</section>
		{/if}

		<!-- Encomenda vinculada (após conversão, via refetch) -->
		{#if orcamento.encomendaId}
			<section
				data-testid="orc-encomenda-criada"
				aria-label="Encomenda vinculada"
				class="flex flex-col gap-2 rounded-xl border border-success/30 bg-success/10 p-5 sm:flex-row sm:items-center sm:justify-between"
			>
				<div>
					<h2 class="text-sm font-semibold text-ink">Encomenda criada</h2>
					<p class="mt-0.5 font-mono text-xs text-muted">{orcamento.encomendaId}</p>
				</div>
				<a
					href={`/vendas/encomendas/${orcamento.encomendaId}`}
					class="inline-flex shrink-0 items-center gap-1.5 rounded-md border border-success/30 bg-success/15 px-3 py-2 text-sm font-medium text-success transition hover:bg-success/25"
				>
					Ver encomenda
				</a>
			</section>
		{/if}

		<!-- KPIs (valores servidos; validade/dias é apresentação da data servida) -->
		<section class="grid grid-cols-2 gap-4 lg:grid-cols-4" aria-label="Indicadores">
			<div class="rounded-xl border border-border bg-elevated p-4">
				<p class="text-xs text-muted">Valor</p>
				<p class="mt-1 font-mono text-2xl font-semibold tabnums text-ink">
					{formatMoneyBRL(orcamento.valorTotal)}
				</p>
				<p class="mt-0.5 text-xs text-muted">total do orçamento</p>
			</div>
			<div class="rounded-xl border border-border bg-elevated p-4">
				<p class="text-xs text-muted">Itens</p>
				<p class="mt-1 font-mono text-2xl font-semibold tabnums text-ink">
					{formatNumber(orcamento.qtdItens)}
				</p>
				<p class="mt-0.5 text-xs text-muted">no orçamento</p>
			</div>
			<div class="rounded-xl border border-border bg-elevated p-4">
				<p class="text-xs text-muted">Validade</p>
				<p class="mt-1 font-mono text-2xl font-semibold tabnums text-ink">
					{dataBR(orcamento.validade)}
				</p>
				<p class="mt-0.5 text-xs text-muted">{diasValidade(orcamento.validade)}</p>
			</div>
			<div class="rounded-xl border border-border bg-elevated p-4">
				<p class="text-xs text-muted">Cliente</p>
				<p class="mt-1 truncate text-2xl font-semibold text-ink" title={orcamento.cliente.nome}>
					{orcamento.cliente.nome}
				</p>
				<p class="mt-0.5 font-mono text-xs text-muted">{orcamento.cliente.id.slice(0, 8)}</p>
			</div>
		</section>

		<div class="grid gap-4 lg:grid-cols-3">
			<div class="space-y-4 lg:col-span-2">
				<!-- Itens -->
				<section
					class="overflow-hidden rounded-xl border border-border bg-surface"
					aria-label="Itens do orçamento"
				>
					<div class="border-b border-border px-5 py-3">
						<h2 class="text-sm font-semibold text-ink">Itens</h2>
					</div>
					{#if !orcamento.itens || orcamento.itens.length === 0}
						<EmptyState
							icon="box"
							title="Sem itens detalhados"
							description="O servidor não retornou o detalhamento de itens deste orçamento."
						/>
					{:else}
						<div class="overflow-x-auto">
							<table class="w-full min-w-[520px] text-sm">
								<thead>
									<tr
										class="border-b border-border bg-elevated/50 text-left text-[11px] uppercase tracking-wide text-muted"
									>
										<th class="px-5 py-2.5 font-medium">Descrição</th>
										<th class="px-4 py-2.5 text-right font-medium">Qtd</th>
										<th class="px-4 py-2.5 text-right font-medium">Unitário</th>
										<th class="px-5 py-2.5 text-right font-medium">Subtotal</th>
									</tr>
								</thead>
								<tbody>
									{#each orcamento.itens as item, i (i)}
										<tr class="border-b border-border transition last:border-0 hover:bg-elevated/40">
											<td class="px-5 py-3">
												<div class="flex flex-wrap items-center gap-1.5">
													{#if item.compra === true}
														<TypeBadge tone="warn" label="Compra" />
													{/if}
													<span class="font-medium text-ink">{item.descricao}</span>
												</div>
												{#if item.material}
													<p class="mt-0.5 text-xs text-muted">
														{item.material.tipo} · {formatNumber(item.material.quantidade)}
														{item.material.unidade}
														{#if item.horas !== undefined}
															· {formatNumber(item.horas)}h
														{/if}
													</p>
												{:else if item.horas !== undefined}
													<p class="mt-0.5 text-xs text-muted">{formatNumber(item.horas)}h</p>
												{/if}
											</td>
											<td class="px-4 py-3 text-right font-mono text-sm tabnums text-ink">
												{formatNumber(item.quantidade)}
											</td>
											<td class="px-4 py-3 text-right font-mono text-sm tabnums text-ink">
												{formatMoneyBRL(item.valorUnitario)}
											</td>
											<td
												class="px-5 py-3 text-right font-mono text-sm font-medium tabnums text-ink"
											>
												{formatMoneyBRL(item.quantidade * item.valorUnitario)}
											</td>
										</tr>
									{/each}
								</tbody>
							</table>
						</div>
					{/if}
				</section>

				<!-- Histórico -->
				<section
					class="rounded-xl border border-border bg-surface p-5"
					aria-label="Resumo do orçamento"
				>
					<h2 class="text-sm font-semibold text-ink">Resumo do orçamento</h2>
					<div class="mt-3">
						<Timeline items={timelineItens} label="Resumo do orçamento" />
					</div>
				</section>
			</div>

			<div class="space-y-4">
				<!-- Cliente -->
				<section class="rounded-xl border border-border bg-surface p-5" aria-label="Cliente">
					<h2 class="text-sm font-semibold text-ink">Cliente</h2>
					<a
						href={`/vendas/clientes/${orcamento.cliente.id}`}
						class="mt-2 block rounded-lg border border-border bg-elevated/50 p-3 transition hover:border-brand/50"
					>
						<p class="truncate text-sm font-medium text-ink">{orcamento.cliente.nome}</p>
						<p class="mt-0.5 font-mono text-xs text-muted">ver cadastro</p>
					</a>
				</section>

				<!-- Observações -->
				<section class="rounded-xl border border-border bg-surface p-5" aria-label="Observações">
					<h2 class="text-sm font-semibold text-ink">Observações</h2>
					<p class="mt-2 text-sm text-muted">
						{orcamento.observacoes?.trim() ? orcamento.observacoes : '—'}
					</p>
				</section>
			</div>
		</div>
	{/if}
</div>

{#if sugerirAberto && orcamento}
	<SolicitarEdicaoModal
		alvo={{ tipo: 'OC', id, nome: orcamento.codigo }}
		onClose={() => (sugerirAberto = false)}
	/>
{/if}
