<script lang="ts">
	import { goto, invalidateAll } from '$app/navigation';
	import type { PageProps } from './$types';
	import type {
		HistoryEntry,
		Loan,
		Movement,
		MovementSummary,
		PageInfo,
		Supplier,
		Tone
	} from '$lib/types/stock';
	import {
		entryKindMeta,
		exitReasonMeta,
		loanComputedMeta,
		quantityTone,
		statusMeta
	} from '$lib/utils/stock-status';
	import { fmtMoney, fmtDate, fmtQty } from '$lib/utils/stock-format';
	import { toasts, toastError } from '$lib/stores/toast';
	import {
		deleteItem,
		fetchItemHistory,
		fetchItemLoans,
		fetchItemMovements,
		fetchItemSuppliers
	} from '$lib/api/stock/items';
	import StatusBadge from '$lib/components/ui/StatusBadge.svelte';
	import Avatar from '$lib/components/ui/Avatar.svelte';
	import EmptyState from '$lib/components/ui/EmptyState.svelte';
	import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte';
	import Skeleton from '$lib/components/ui/Skeleton.svelte';
	import TableSkeleton from '$lib/components/ui/TableSkeleton.svelte';
	import Pagination from '$lib/components/ui/Pagination.svelte';
	import RowActions from '$lib/components/ui/RowActions.svelte';
	import ConfirmDialog from '$lib/components/ui/ConfirmDialog.svelte';
	import Icon from '$lib/components/ui/Icon.svelte';

	let { data }: PageProps = $props();
	const canEdit = $derived(data.canEdit ?? false);
	const detail = $derived(data.detail);
	const loadError = $derived(data.error);

	let activeTab = $state('visao');
	const TABS: { id: string; label: string }[] = [
		{ id: 'visao', label: 'Visão geral' },
		{ id: 'movimentacoes', label: 'Movimentações' },
		{ id: 'emprestimos', label: 'Empréstimos' },
		{ id: 'fornecedores', label: 'Fornecedores' },
		{ id: 'historico', label: 'Histórico' }
	];

	const KEY_MOV = 'movimentacoes';
	const KEY_LOANS = 'emprestimos';
	const KEY_SUP = 'fornecedores';
	const KEY_HIST = 'historico';

	let tabLoaded = $state<Record<string, boolean>>({});
	let tabLoading = $state<Record<string, boolean>>({});
	let tabError = $state<Record<string, string | null>>({});

	let mov = $state<{
		summary: MovementSummary | null;
		movements: Movement[];
		pagination: PageInfo | null;
	} | null>(null);
	let movPage = $state(1);

	let loans = $state<Loan[] | null>(null);
	let suppliers = $state<Supplier[] | null>(null);
	let history = $state<HistoryEntry[] | null>(null);

	async function activate(tab: string): Promise<void> {
		if (!detail) return;
		activeTab = tab;

		if (tab === KEY_MOV && !tabLoaded[KEY_MOV] && !tabLoading[KEY_MOV]) {
			await loadMovements(1);
		} else if (tab === KEY_LOANS && loans === null && !tabLoading[KEY_LOANS]) {
			tabLoading[KEY_LOANS] = true;
			tabError[KEY_LOANS] = null;
			try {
				loans = await fetchItemLoans(fetch, detail.id);
				tabLoaded[KEY_LOANS] = true;
			} catch (err) {
				tabError[KEY_LOANS] = err instanceof Error ? err.message : 'Erro ao carregar empréstimos';
			} finally {
				tabLoading[KEY_LOANS] = false;
			}
		} else if (tab === KEY_SUP && suppliers === null && !tabLoading[KEY_SUP]) {
			tabLoading[KEY_SUP] = true;
			tabError[KEY_SUP] = null;
			try {
				suppliers = await fetchItemSuppliers(fetch, detail.id);
				tabLoaded[KEY_SUP] = true;
			} catch (err) {
				tabError[KEY_SUP] = err instanceof Error ? err.message : 'Erro ao carregar fornecedores';
			} finally {
				tabLoading[KEY_SUP] = false;
			}
		} else if (tab === KEY_HIST && history === null && !tabLoading[KEY_HIST]) {
			tabLoading[KEY_HIST] = true;
			tabError[KEY_HIST] = null;
			try {
				history = await fetchItemHistory(fetch, detail.id);
				tabLoaded[KEY_HIST] = true;
			} catch (err) {
				tabError[KEY_HIST] = err instanceof Error ? err.message : 'Erro ao carregar histórico';
			} finally {
				tabLoading[KEY_HIST] = false;
			}
		}
	}

	async function loadMovements(page: number): Promise<void> {
		if (!detail || tabLoading[KEY_MOV]) return;
		tabLoading[KEY_MOV] = true;
		tabError[KEY_MOV] = null;
		movPage = page;
		try {
			const res = await fetchItemMovements(fetch, detail.id, page);
			mov = { summary: res.summary, movements: res.movements, pagination: res.pagination };
			tabLoaded[KEY_MOV] = true;
		} catch (err) {
			tabError[KEY_MOV] = err instanceof Error ? err.message : 'Erro ao carregar movimentações';
		} finally {
			tabLoading[KEY_MOV] = false;
		}
	}

	function retryTab(tab: string): void {
		tabError[tab] = null;
		tabLoading[tab] = false;
		if (tab === KEY_MOV) {
			tabLoaded[KEY_MOV] = false;
			void loadMovements(movPage);
		} else {
			loans = null;
			suppliers = null;
			history = null;
			tabLoaded[tab] = false;
			void activate(tab);
		}
	}

	// ---- meta helpers ----

	const TONE_TEXT: Record<Tone, string> = {
		success: 'text-success',
		warn: 'text-warn',
		danger: 'text-danger',
		brand: 'text-brandhi',
		muted: 'text-muted',
		ink: 'text-ink'
	};

	function qtyClass(current: number, minimum: number): string {
		return TONE_TEXT[quantityTone(current, minimum)];
	}

	function movementMeta(m: Movement): { label: string; color: Tone } {
		if (m.type === 'in' && m.kind) return entryKindMeta(m.kind);
		if (m.type === 'out' && m.reason) return exitReasonMeta(m.reason);
		return {
			label: m.type === 'in' ? 'Entrada' : 'Saída',
			color: m.type === 'in' ? 'success' : 'danger'
		};
	}

	const DOT_TONE: Record<Tone, string> = {
		success: 'bg-success',
		warn: 'bg-warn',
		danger: 'bg-danger',
		brand: 'bg-brand',
		muted: 'bg-muted',
		ink: 'bg-ink'
	};

	// ---- exclusão ----

	let deleteOpen = $state(false);
	let deleting = $state(false);

	async function handleDelete(): Promise<void> {
		if (!detail) return;
		deleting = true;
		try {
			await deleteItem(detail.id);
			toasts.success('Item excluído');
			await goto('/estoque/itens', { invalidateAll: true });
		} catch (err) {
			toastError(err, 'Não foi possível excluir o item');
			deleteOpen = false;
			deleting = false;
		}
	}
</script>

<svelte:head>
	<title>{detail?.name ?? 'Item'} — Estoque — FabLab</title>
</svelte:head>

{#if loadError && !detail}
	<ErrorBanner message="Não foi possível carregar o item" hint={loadError} onRetry={invalidateAll} />
{:else if !detail}
	<!-- Loading -->
	<div class="space-y-4">
		<div class="rounded-xl border border-border bg-surface p-6">
			<Skeleton class="h-5 w-40" />
			<Skeleton class="mt-3 h-8 w-64" />
			<Skeleton class="mt-2 h-3 w-72 opacity-60" />
		</div>
		<div class="grid grid-cols-2 gap-4 lg:grid-cols-4">
			{#each [0, 1, 2, 3] as i (i)}
				<div class="rounded-xl border border-border bg-surface p-4">
					<Skeleton class="h-3 w-20" />
					<Skeleton class="mt-2 h-7 w-14" />
					<Skeleton class="mt-2 h-2 w-24 opacity-60" />
				</div>
			{/each}
		</div>
		<div class="rounded-xl border border-border bg-surface p-6">
			<TableSkeleton rows={6} />
		</div>
	</div>
{:else}
	<!-- Barra de ações -->
	<div class="flex items-center justify-between gap-4">
		<a
			href="/estoque/itens"
			class="inline-flex items-center gap-2 text-xs font-medium text-muted transition-colors hover:text-ink"
		>
			<Icon name="chevron-left" class="h-3.5 w-3.5" /> Voltar para itens
		</a>
		{#if canEdit}
			<RowActions
				actions={[{ id: 'delete', label: 'Excluir item', icon: 'trash', tone: 'danger' }]}
				onSelect={() => (deleteOpen = true)}
			/>
		{/if}
	</div>

	<!-- Hero -->
	<section class="rounded-xl border border-border bg-surface p-6">
		<div class="flex flex-wrap gap-5">
			<div
				class="flex h-24 w-24 shrink-0 items-center justify-center rounded-xl border border-border bg-elevated"
			>
				<Icon name="photo" class="h-10 w-10 text-muted/60" />
			</div>
			<div class="min-w-0 flex-1">
				<p class="font-mono text-xs text-muted">{detail.code}</p>
				<h1 class="mt-0.5 truncate text-2xl font-semibold tracking-tight text-ink">
					{detail.name}
				</h1>
				<p class="mt-1 text-sm text-muted">
					{detail.category.label} · {detail.location.label} · {detail.unit}
				</p>
				<p class="mt-2 text-xs text-muted">
					Atualizado {detail.updatedRelative ?? 'recentemente'}
					{#if detail.updatedBy}por <span class="text-ink">{detail.updatedBy}</span>{/if}
				</p>
			</div>
			<StatusBadge {...statusMeta(detail.status)} />
		</div>
	</section>

	<!-- KPIs -->
	<div class="grid grid-cols-2 gap-4 lg:grid-cols-4">
		<div class="rounded-xl border border-border bg-surface p-4">
			<span class="text-xs text-muted">Qtd. atual</span>
			<div class="mt-1 flex items-baseline gap-1.5">
				<span class="text-2xl font-semibold tracking-tight {qtyClass(detail.current, detail.minimum)}">
					{fmtQty(detail.current)}
				</span>
			</div>
			<p class="mt-1 text-[11px] text-muted">
				{detail.current < detail.minimum ? 'abaixo do mínimo' : 'dentro do esperado'}
			</p>
		</div>

		<div class="rounded-xl border border-border bg-surface p-4">
			<span class="text-xs text-muted">Qtd. mínima</span>
			<div class="mt-1 flex items-baseline gap-1.5">
				<span class="text-2xl font-semibold tracking-tight">{fmtQty(detail.minimum)}</span>
			</div>
			<p class="mt-1 text-[11px] text-muted">ponto de pedido: {detail.reorderPoint ?? '—'}</p>
		</div>

		<div class="rounded-xl border border-border bg-surface p-4">
			<span class="text-xs text-muted">Empréstimos</span>
			<div class="mt-1 flex items-baseline gap-1.5">
				<span class="text-2xl font-semibold tracking-tight">{detail.activeLoans}</span>
				<span class="text-xs text-muted">ativos</span>
			</div>
			<p class="mt-1 text-[11px] text-muted">
				último: {detail.lastLoan ? fmtDate(detail.lastLoan.at) : '—'}
			</p>
		</div>

		<div class="rounded-xl border border-border bg-surface p-4">
			<span class="text-xs text-muted">Última entrada</span>
			<div class="mt-1 flex items-baseline gap-1.5">
				<span class="text-2xl font-semibold tracking-tight">
					{detail.lastEntry ? fmtDate(detail.lastEntry.at) : '—'}
				</span>
			</div>
			<p class="mt-1 text-[11px] text-success">
				{#if detail.lastEntry}
					+{fmtQty(detail.lastEntry.quantity, detail.unit)} · {detail.lastEntry.kind}
				{/if}
			</p>
		</div>
	</div>

	<!-- Abas -->
	<section class="overflow-hidden rounded-xl border border-border bg-surface">
		<div class="flex items-center gap-1 overflow-x-auto border-b border-border px-2" role="tablist">
			{#each TABS as tab (tab.id)}
				<button
					role="tab"
					aria-selected={activeTab === tab.id}
					onclick={() => activate(tab.id)}
					class="whitespace-nowrap border-b-2 px-4 py-3 text-sm font-medium transition-colors {activeTab === tab.id
						? 'border-brand text-brand'
						: 'border-transparent text-muted hover:text-ink'}"
				>
					{tab.label}
				</button>
			{/each}
		</div>

		<div class="p-6">
			{#if activeTab === 'visao'}
				<div class="space-y-8">
					{#if detail.description}
						<div>
							<h2 class="mb-2 text-xs font-medium uppercase tracking-wide text-muted">
								Descrição
							</h2>
							<p class="text-sm leading-relaxed text-ink">{detail.description}</p>
						</div>
					{/if}

					<div>
						<h2 class="mb-3 text-xs font-medium uppercase tracking-wide text-muted">
							Especificações
						</h2>
						<dl
							class="grid grid-cols-1 divide-y divide-border/50 overflow-hidden rounded-lg border border-border sm:grid-cols-[220px_1fr]"
						>
							{@render specRow('Código interno', detail.code, 'font-mono')}
							{@render specRow('Categoria', detail.category.label)}
							{@render specRow('Unidade', detail.unit)}
							{@render specRow('Localização', detail.location.label)}
							{@render specRow('Estoque mínimo', fmtQty(detail.minimum, detail.unit))}
							{@render specRow('Estoque máximo', detail.maximum != null ? fmtQty(detail.maximum, detail.unit) : '—')}
							{@render specRow('Ponto de pedido', detail.reorderPoint != null ? fmtQty(detail.reorderPoint, detail.unit) : '—')}
							{@render specRow('Lead time (dias)', detail.leadTimeDays != null ? String(detail.leadTimeDays) : '—')}
							{@render specRow('Valor unitário', fmtMoney(detail.unitValue))}
						</dl>
					</div>

					{#if detail.bomUsage && detail.bomUsage.length > 0}
						<div>
							<h2 class="mb-3 text-xs font-medium uppercase tracking-wide text-muted">
								Usado em (BOM)
							</h2>
							<div class="divide-y divide-border border border-border rounded-lg">
								{#each detail.bomUsage as use (use.projectId)}
									<div class="flex items-center justify-between px-4 py-2.5">
										<span class="text-sm text-ink">{use.projectName}</span>
										<span class="font-mono text-sm text-muted">
											{use.qty} {use.unit}/unidade
										</span>
									</div>
								{/each}
							</div>
						</div>
					{/if}
				</div>

			{:else if activeTab === 'movimentacoes'}
				{#if tabLoading[KEY_MOV] && !mov}
					<TableSkeleton rows={5} />
				{:else if tabError[KEY_MOV] && !mov}
					<ErrorBanner
						message="Movimentações indisponíveis"
						hint={tabError[KEY_MOV]}
						onRetry={() => retryTab(KEY_MOV)}
					/>
				{:else if mov && mov.movements.length === 0}
					<EmptyState
						icon="box"
						title="Sem movimentações"
						description="Este item ainda não registrou entradas ou saídas."
					/>
				{:else if mov}
					<div class="space-y-4">
						<div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
							<div class="rounded-lg border border-success/30 bg-success/5 px-4 py-3">
								<span class="text-xs text-muted">Entradas</span>
								<p class="mt-0.5 text-lg font-semibold text-success">
									+{mov.summary?.entries?.count ?? 0} · {fmtQty(
										mov.summary?.entries?.sum ?? 0,
										detail.unit
									)}
								</p>
							</div>
							<div class="rounded-lg border border-danger/30 bg-danger/5 px-4 py-3">
								<span class="text-xs text-muted">Saídas</span>
								<p class="mt-0.5 text-lg font-semibold text-danger">
									−{mov.summary?.exits?.count ?? 0} · {fmtQty(
										mov.summary?.exits?.sum ?? 0,
										detail.unit
									)}
								</p>
							</div>
						</div>

						<div class="overflow-x-auto">
							<table class="w-full text-sm">
								<thead>
									<tr
										class="border-b border-border text-left text-[11px] uppercase tracking-wide text-muted"
									>
										<th class="px-4 py-2.5 font-medium">Data</th>
										<th class="px-4 py-2.5 font-medium">Tipo</th>
										<th class="px-4 py-2.5 text-right font-medium">Qtd</th>
										<th class="px-4 py-2.5 font-medium">Referência</th>
										<th class="px-4 py-2.5 font-medium">Responsável</th>
									</tr>
								</thead>
								<tbody class="divide-y divide-border">
									{#each mov.movements as m (m.id)}
										{@const meta = movementMeta(m)}
										<tr>
											<td class="px-4 py-3 text-muted">{fmtDate(m.date)}</td>
											<td class="px-4 py-3">
												<StatusBadge label={meta.label} color={meta.color} />
											</td>
											<td
												class="px-4 py-3 text-right font-mono {m.type === 'in' ? 'text-success' : 'text-danger'}"
											>
												{m.type === 'in' ? '+' : '−'}{fmtQty(m.quantity, m.item.unit)}
											</td>
											<td class="px-4 py-3 text-muted">{m.reference ?? '—'}</td>
											<td class="px-4 py-3 text-muted">{m.responsible?.name ?? '—'}</td>
										</tr>
									{/each}
								</tbody>
							</table>
						</div>

						<div class="border-t border-border pt-3">
							<Pagination
								page={movPage}
								totalPages={mov.pagination?.totalPages ?? 0}
								totalItems={mov.pagination?.totalItems}
								onPage={loadMovements}
								label="movimentações"
							/>
						</div>
					</div>
				{/if}

			{:else if activeTab === 'emprestimos'}
				{#if tabLoading[KEY_LOANS] && loans === null}
					<TableSkeleton rows={5} />
				{:else if tabError[KEY_LOANS] && loans === null}
					<ErrorBanner
						message="Empréstimos indisponíveis"
						hint={tabError[KEY_LOANS]}
						onRetry={() => retryTab(KEY_LOANS)}
					/>
				{:else if loans && loans.length === 0}
					<EmptyState
						icon="arrow-uturn-left"
						title="Sem empréstimos"
						description="Nenhum empréstimo registrado para este item."
					/>
				{:else if loans}
					<div class="overflow-x-auto">
						<table class="w-full text-sm">
							<thead>
								<tr
									class="border-b border-border text-left text-[11px] uppercase tracking-wide text-muted"
								>
									<th class="px-4 py-2.5 font-medium">Tomador</th>
									<th class="px-4 py-2.5 font-medium">Finalidade</th>
									<th class="px-4 py-2.5 font-medium">Qtd</th>
									<th class="px-4 py-2.5 font-medium">Retirada</th>
									<th class="px-4 py-2.5 font-medium">Prazo</th>
									<th class="px-4 py-2.5 font-medium">Status</th>
								</tr>
							</thead>
							<tbody class="divide-y divide-border">
								{#each loans as l (l.id)}
									{@const overdue = loanComputedMeta(l.computed).color === 'danger'}
									<tr class:border-l-2={overdue} class:border-danger={overdue}>
										<td class="px-4 py-3">
											<div class="flex items-center gap-2">
												<Avatar
													name={l.borrower.name}
													initialsOverride={l.borrower.initials}
													size="xs"
												/>
												<span class="text-ink">{l.borrower.name}</span>
											</div>
										</td>
										<td class="px-4 py-3 text-muted">{l.purpose ?? '—'}</td>
										<td class="px-4 py-3 font-mono">{fmtQty(l.quantity, l.item.unit)}</td>
										<td class="px-4 py-3 text-muted">{fmtDate(l.borrowDate)}</td>
										<td class="px-4 py-3 text-muted">
											{fmtDate(l.dueDate)}
											{#if l.computed === 'atrasado' && l.overdueDays > 0}
												<span class="ml-1 text-xs font-semibold text-danger">
													(atrasado {l.overdueDays}d)
												</span>
											{:else if l.computed === 'vence_hoje'}
												<span class="ml-1 text-xs font-semibold text-warn">(hoje)</span>
											{/if}
										</td>
										<td class="px-4 py-3">
											<StatusBadge {...loanComputedMeta(l.computed)} />
										</td>
									</tr>
								{/each}
							</tbody>
						</table>
					</div>
				{/if}

			{:else if activeTab === 'fornecedores'}
				{#if tabLoading[KEY_SUP] && suppliers === null}
					<TableSkeleton rows={5} />
				{:else if tabError[KEY_SUP] && suppliers === null}
					<ErrorBanner
						message="Fornecedores indisponíveis"
						hint={tabError[KEY_SUP]}
						onRetry={() => retryTab(KEY_SUP)}
					/>
				{:else if suppliers && suppliers.length === 0}
					<EmptyState
						icon="truck"
						title="Sem fornecedores"
						description="Nenhum fornecedor vinculado a este item."
					/>
				{:else if suppliers}
					<div class="overflow-x-auto">
						<table class="w-full text-sm">
							<thead>
								<tr
									class="border-b border-border text-left text-[11px] uppercase tracking-wide text-muted"
								>
									<th class="px-4 py-2.5 font-medium">Fornecedor</th>
									<th class="px-4 py-2.5 font-medium">Última compra</th>
									<th class="px-4 py-2.5 text-right font-medium">Valor</th>
									<th class="px-4 py-2.5 font-medium">Itens</th>
								</tr>
							</thead>
							<tbody class="divide-y divide-border">
								{#each suppliers as s (s.id)}
									<tr>
										<td class="px-4 py-3">
											<div class="flex items-center gap-2">
												<Avatar name={s.name} size="xs" />
												<div>
													<p class="text-ink">{s.name}</p>
													{#if s.lastPurchase?.notaFiscal}
														<p class="text-xs text-muted">NF {s.lastPurchase.notaFiscal}</p>
													{/if}
												</div>
											</div>
										</td>
										<td class="px-4 py-3 text-muted">
											{s.lastPurchase ? fmtDate(s.lastPurchase.date) : '—'}
										</td>
										<td class="px-4 py-3 text-right font-mono">
											{fmtMoney(s.lastPurchase?.value)}
										</td>
										<td class="px-4 py-3 text-muted">{s.itemsCount}</td>
									</tr>
								{/each}
							</tbody>
						</table>
					</div>
				{/if}

			{:else if activeTab === 'historico'}
				{#if tabLoading[KEY_HIST] && history === null}
					<TableSkeleton rows={5} />
				{:else if tabError[KEY_HIST] && history === null}
					<ErrorBanner
						message="Histórico indisponível"
						hint={tabError[KEY_HIST]}
						onRetry={() => retryTab(KEY_HIST)}
					/>
				{:else if history && history.length === 0}
					<EmptyState
						icon="clock"
						title="Sem histórico"
						description="Nenhuma alteração registrada para este item."
					/>
				{:else if history}
					<ul role="list" class="mx-2">
						{#each history as entry (entry.id)}
							<li class="relative flex gap-3 pb-6 pl-4 last:pb-0">
								<span
									class="absolute left-0 top-1.5 h-2 w-2 rounded-full {DOT_TONE[entry.tone]}"
									aria-hidden="true"></span>
								<span class="absolute bottom-0 left-[3px] top-5 w-px bg-border" aria-hidden="true"></span>
								<div>
									<p class="text-sm font-medium text-ink">{entry.title}</p>
									<p class="mt-0.5 text-xs text-muted">{entry.by} · {fmtDate(entry.at)}</p>
								</div>
							</li>
						{/each}
					</ul>
				{/if}
			{/if}
		</div>
	</section>
{/if}

{#snippet specRow(label: string, value: string, extra = '')}
	<div class="contents">
		<dt class="bg-elevated/40 px-4 py-2.5 text-xs text-muted">{label}</dt>
		<dd class="px-4 py-2.5 text-sm {extra}">{value}</dd>
	</div>
{/snippet}

<ConfirmDialog
	open={deleteOpen}
	title="Excluir item"
	message={`Esta ação apagará "${detail?.code ?? ''}" do estoque. Ela não pode ser desfeita.`}
	confirmLabel="Excluir"
	loading={deleting}
	onCancel={() => (deleteOpen = false)}
	onConfirm={handleDelete}
/>