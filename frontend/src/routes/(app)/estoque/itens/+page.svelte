<script lang="ts">
	import { goto, invalidateAll } from '$app/navigation';
	import type { PageProps } from './$types';
	import type { StockItem, Tone } from '$lib/types/stock';
	import { fetchStatuses, exportItemsCsv, bulkAction } from '$lib/api/stock/items';
	import { statusMeta, quantityTone } from '$lib/utils/stock-status';
	import { fmtQty } from '$lib/utils/stock-format';
	import { toasts, toastError } from '$lib/stores/toast';
	import PageHeader from '$lib/components/ui/PageHeader.svelte';
	import SearchInput from '$lib/components/ui/SearchInput.svelte';
	import Dropdown from '$lib/components/ui/Dropdown.svelte';
	import StatusBadge from '$lib/components/ui/StatusBadge.svelte';
	import Chip from '$lib/components/ui/Chip.svelte';
	import Pagination from '$lib/components/ui/Pagination.svelte';
	import TableSkeleton from '$lib/components/ui/TableSkeleton.svelte';
	import EmptyState from '$lib/components/ui/EmptyState.svelte';
	import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte';
	import Checkbox from '$lib/components/ui/Checkbox.svelte';
	import RowActions, { type RowAction } from '$lib/components/ui/RowActions.svelte';
	import BulkActionsBar from '$lib/components/ui/BulkActionsBar.svelte';
	import Modal from '$lib/components/ui/Modal.svelte';
	import Select from '$lib/components/ui/Select.svelte';
	import ConfirmDialog from '$lib/components/ui/ConfirmDialog.svelte';
	import Icon from '$lib/components/ui/Icon.svelte';

	let { data }: PageProps = $props();
	const canEdit = $derived(data.canEdit ?? false);

	const result = $derived(data.result);
	const insError = $derived(data.error);

	const items = $derived(result?.items ?? []);
	const pagination = $derived(result?.pagination);
	const filters = $derived(result?.filters);

	const statusOptions = $derived(fetchStatuses());

	const hasFilters = $derived(
		data.params.search !== '' ||
			data.params.categories.length > 0 ||
			data.params.locations.length > 0 ||
			data.params.statuses.length > 0
	);

	// ---- seleção ----

	let selected = $state(new Set<string>());
	let lastPageKey = $state(0);
	let bulkOpen = $state(false);
	let deleteOpen = $state(false);
	let categoryTarget = $state('');
	let busy = $state(false);

	$effect(() => {
		if (!pagination) return;
		const key = pagination.page * 1000 + pagination.pageSize;
		if (key !== lastPageKey) {
			selected = new Set();
			lastPageKey = key;
		}
	});

	const selectedCount = $derived(selected.size);
	const pageVisibleSelected = $derived(items.filter((i) => selected.has(i.id)).length);
	const allPageSelected = $derived(items.length > 0 && pageVisibleSelected === items.length);
	const somePageSelected = $derived(pageVisibleSelected > 0 && !allPageSelected);

	const chipCats = $derived(
		filters?.categories.filter((c) => data.params.categories.includes(c.id)) ?? []
	);
	const chipLocs = $derived(
		filters?.locations.filter((l) => data.params.locations.includes(l.id)) ?? []
	);
	const chipStatus = $derived(statusOptions.filter((s) => data.params.statuses.includes(s.id)));

	const cardCategories = $derived(filters?.categories ?? []);

	// ---- URL como fonte de verdade ----

	interface Query {
		[key: string]: string | number | string[] | undefined;
	}

	function updateUrl(overrides: Query): void {
		const base: Query = {
			search: data.params.search,
			category: data.params.categories,
			location: data.params.locations,
			status: data.params.statuses,
			page: data.params.page,
			pageSize: data.params.pageSize,
			sort: data.params.sort
		};
		const merged = { ...base, ...overrides };

		const search = new URLSearchParams();
		for (const [key, value] of Object.entries(merged)) {
			if (Array.isArray(value)) {
				for (const v of value) if (v) search.append(key, v);
			} else if (value !== undefined && value !== '') {
				search.append(key, String(value));
			}
		}
		const qs = search.toString();
		goto(`/estoque/itens${qs ? `?${qs}` : ''}`, { invalidateAll: true });
	}

	function filtersOnly(): Query {
		return {
			page: data.params.page,
			pageSize: data.params.pageSize,
			sort: data.params.sort
		};
	}

	function handleSearch(value: string): void {
		updateUrl({ ...filtersOnly(), search: value || undefined, page: 1 });
	}

	function toggleFilter(key: 'category' | 'location' | 'status', id: string): void {
		const current =
			key === 'category'
				? data.params.categories
				: key === 'location'
					? data.params.locations
					: data.params.statuses;
		const next = current.includes(id) ? current.filter((v) => v !== id) : [...current, id];
		updateUrl({ ...filtersOnly(), [key]: next, page: 1 });
	}

	function clearFilter(key: 'category' | 'location' | 'status'): void {
		updateUrl({ ...filtersOnly(), [key]: [], page: 1 });
	}

	function clearAllFilters(): void {
		updateUrl({ page: 1, pageSize: data.params.pageSize });
	}

	function onPage(page: number): void {
		updateUrl({ ...filtersOnly(), page });
	}

	function onPageSize(size: number): void {
		updateUrl({ ...filtersOnly(), pageSize: size, page: 1 });
	}

	function toggleSort(col: 'name' | 'qtd'): void {
		const key = col === 'name' ? 'name' : 'qtd';
		const next = data.params.sort === `${key}:asc` ? `${key}:desc` : `${key}:asc`;
		updateUrl({ ...filtersOnly(), sort: next, page: 1 });
	}

	function isSortActive(col: 'name' | 'qtd'): boolean {
		return data.params.sort.startsWith(col);
	}

	function isSortAsc(col: 'name' | 'qtd'): boolean {
		return data.params.sort === `${col}:asc`;
	}

	// ---- seleção / bulk ----

	function toggle(id: string): void {
		const next = new Set(selected);
		if (next.has(id)) next.delete(id);
		else next.add(id);
		selected = next;
	}

	function toggleAll(): void {
		const next = new Set(selected);
		for (const item of items) {
			if (allPageSelected) next.delete(item.id);
			else next.add(item.id);
		}
		selected = next;
	}

	function clearSelection(): void {
		selected = new Set();
	}

	async function handleBulkCategory(): Promise<void> {
		if (!categoryTarget) return;
		busy = true;
		try {
			const res = await bulkAction([...selected], 'updateCategory', categoryTarget);
			toasts.success(`Categoria atualizada em ${res.updated} item(ns)`);
			selected = new Set();
			categoryTarget = '';
			bulkOpen = false;
			await invalidateAll();
		} catch (err) {
			toastError(err, 'Não foi possível alterar a categoria');
		} finally {
			busy = false;
		}
	}

	async function handleBulkDelete(): Promise<void> {
		busy = true;
		try {
			const res = await bulkAction([...selected], 'delete');
			toasts.success(`${res.updated} item(ns) excluído(s)`);
			deleteOpen = false;
			const emptyAfter = items.length === 1 && (data.params.page ?? 1) > 1;
			if (emptyAfter) {
				selected = new Set();
				updateUrl({ ...filtersOnly(), page: (data.params.page ?? 1) - 1 });
			} else {
				selected = new Set();
				await invalidateAll();
			}
		} catch (err) {
			toastError(err, 'Não foi possível excluir os itens');
		} finally {
			busy = false;
		}
	}

	async function handleExport(): Promise<void> {
		try {
			await exportItemsCsv(data.params);
			toasts.info('Exportação CSV iniciada');
		} catch (err) {
			toastError(err, 'Não foi possível exportar');
		}
	}

	// ---- visual helpers ----

	const QTY_TONES: Record<Tone, string> = {
		success: 'text-success',
		warn: 'text-warn',
		danger: 'text-danger',
		brand: 'text-brandhi',
		muted: 'text-muted',
		ink: 'text-ink'
	};

	function qtyClass(item: StockItem): string {
		return QTY_TONES[quantityTone(item.quantity.current, item.quantity.minimum)];
	}

	const gridCols = $derived(
		canEdit
			? 'md:grid-cols-[32px_1fr_120px_130px_90px_120px_40px]'
			: 'md:grid-cols-[1fr_120px_130px_90px_120px]'
	);

	function rowActions(_item: StockItem): RowAction[] {
		return [{ id: 'view', label: 'Ver detalhes', icon: 'eye' }];
	}

	function onRowAction(item: StockItem, id: string): void {
		if (id === 'view') void goto(`/estoque/itens/${item.id}`);
	}

	function openDetail(item: StockItem): void {
		void goto(`/estoque/itens/${item.id}`);
	}
</script>

<svelte:head>
	<title>Itens do estoque — Estoque — FabLab</title>
</svelte:head>

<div class="space-y-4">
	<PageHeader
		title="Itens do estoque"
		subtitle={pagination ? `${pagination.totalItems} itens cadastrados` : ''}
	>
		{#snippet children()}
			<button
				onclick={handleExport}
				class="inline-flex items-center gap-1.5 rounded-lg border border-border bg-surface px-3 py-2 text-sm font-medium text-ink transition-colors hover:border-brand/50 hover:text-brandhi"
			>
				<Icon name="arrow-down-tray" class="h-4 w-4" /> Exportar
			</button>
			{#if canEdit}
				<a
					href="/estoque/itens/novo"
					class="inline-flex items-center gap-1.5 rounded-lg bg-brand px-3 py-2 text-sm font-semibold text-white shadow-lg shadow-brand/20 transition-colors hover:bg-brandhi"
				>
					<Icon name="plus" class="h-4 w-4" /> Novo item
				</a>
			{/if}
		{/snippet}
	</PageHeader>

	<!-- Toolkit: busca + filtros -->
	<div class="flex flex-wrap items-center gap-2">
		<SearchInput
			value={data.params.search}
			onSearch={handleSearch}
			placeholder="Buscar por código ou nome…"
			class="min-w-56 flex-1"
		/>
		<Dropdown
			label="Categoria"
			options={filters?.categories ?? []}
			selected={data.params.categories}
			onToggle={(id) => toggleFilter('category', id)}
			onClear={() => clearFilter('category')}
		/>
		<Dropdown
			label="Localização"
			options={filters?.locations ?? []}
			selected={data.params.locations}
			onToggle={(id) => toggleFilter('location', id)}
			onClear={() => clearFilter('location')}
		/>
		<Dropdown
			label="Status"
			options={statusOptions}
			selected={data.params.statuses}
			onToggle={(id) => toggleFilter('status', id)}
			onClear={() => clearFilter('status')}
		/>
	</div>

	<!-- chips de filtro ativo -->
	{#if hasFilters}
		<div class="flex flex-wrap items-center gap-1.5">
			{#each chipCats as c (c.id)}
				<Chip label={c.label} count={c.count} onRemove={() => toggleFilter('category', c.id)} />
			{/each}
			{#each chipLocs as l (l.id)}
				<Chip
					label={l.label}
					count={l.count}
					onRemove={() => toggleFilter('location', l.id)}
				/>
			{/each}
			{#each chipStatus as s (s.id)}
				<Chip label={s.label} onRemove={() => toggleFilter('status', s.id)} />
			{/each}
			<button
				onclick={clearAllFilters}
				class="text-xs font-medium text-brandhi transition-colors hover:text-brand"
			>
				Limpar filtros
			</button>
		</div>
	{/if}

	{#if insError && !result}
		<ErrorBanner message="Não foi possível carregar os itens" hint={insError} onRetry={invalidateAll} />
	{:else if !result}
		<!-- Loading -->
		<div class="overflow-hidden rounded-xl border border-border bg-surface">
			<div class="border-b border-border bg-elevated/50 px-4 py-2.5">
				<TableSkeleton rows={1} columns={3} class="!space-y-0" />
			</div>
			<div class="p-4">
				<TableSkeleton rows={8} />
			</div>
		</div>
	{:else if items.length === 0}
		<div class="rounded-xl border border-border bg-surface">
			{#if hasFilters}
				<EmptyState
					icon="filter"
					title="Nenhum item encontrado"
					description="Ajuste ou limpe os filtros para ver mais resultados."
				>
					{#snippet children()}
						<button
							onclick={clearAllFilters}
							class="rounded-lg border border-border bg-surface px-3 py-2 text-sm font-medium text-ink transition-colors hover:border-brand/50 hover:text-brandhi"
						>
							Limpar filtros
						</button>
					{/snippet}
				</EmptyState>
			{:else}
				<EmptyState
					icon="box"
					title="Ainda não há itens no estoque"
					description="Cadastre o primeiro item para começar a controlar seu inventário."
				>
					{#snippet children()}
						{#if canEdit}
							<a
								href="/estoque/itens/novo"
								class="inline-flex items-center gap-1.5 rounded-lg bg-brand px-3 py-2 text-sm font-semibold text-white transition-colors hover:bg-brandhi"
							>
								<Icon name="plus" class="h-4 w-4" /> Novo item
							</a>
						{/if}
					{/snippet}
				</EmptyState>
			{/if}
		</div>
	{:else}
		<!-- Tabela desktop -->
		<div class="hidden overflow-hidden rounded-xl border border-border bg-surface md:block">
			<div
				class="grid items-center gap-3 border-b border-border bg-elevated/50 px-4 py-2.5 text-[11px] font-medium uppercase tracking-wide text-muted {gridCols}"
			>
				{#if canEdit}
					<Checkbox
						checked={allPageSelected}
						indeterminate={somePageSelected}
						onChange={toggleAll}
					/>
				{/if}
				<button
					onclick={() => toggleSort('name')}
					class="inline-flex items-center gap-1 text-left uppercase tracking-wide transition-colors hover:text-ink"
				>
					Item
					<span class={isSortActive('name') ? 'text-brandhi' : 'opacity-40'}>
						<Icon name={isSortAsc('name') ? 'chevron-up' : 'chevron-down'} class="h-3 w-3" />
					</span>
				</button>
				<span>Categoria</span>
				<span>Localização</span>
				<button
					onclick={() => toggleSort('qtd')}
					class="inline-flex items-center justify-end gap-1 text-right uppercase tracking-wide transition-colors hover:text-ink"
				>
					Qtd
					<span class={isSortActive('qtd') ? 'text-brandhi' : 'opacity-40'}>
						<Icon name={isSortAsc('qtd') ? 'chevron-up' : 'chevron-down'} class="h-3 w-3" />
					</span>
				</button>
				<span>Status</span>
				{#if canEdit}<span></span>{/if}
			</div>

			<div class="divide-y divide-border">
				{#each items as item (item.id)}
					<div
						onclick={() => openDetail(item)}
						role="button"
						tabindex="0"
						onkeydown={(e) => {
							if (e.key === 'Enter') openDetail(item);
						}}
						class="grid cursor-pointer items-center gap-3 px-4 py-3 transition-colors hover:bg-elevated/40 {gridCols} {selected.has(item.id)
							? 'border-l-2 border-brand bg-brand/5'
							: 'border-l-2 border-transparent'}"
					>
						{#if canEdit}
							<div onclick={(e) => e.stopPropagation()}>
								<Checkbox checked={selected.has(item.id)} onChange={() => toggle(item.id)} />
							</div>
						{/if}
						<div class="min-w-0">
							<p class="truncate font-mono text-xs text-muted">{item.code}</p>
							<p class="truncate text-sm text-ink">{item.name}</p>
						</div>
						<span
							class="w-fit rounded border border-border bg-elevated px-2 py-0.5 text-[10px] font-medium text-ink"
						>
							{item.category.label}
						</span>
						<span class="truncate text-xs text-muted">{item.location.label}</span>
						<span class="text-right font-mono text-sm {qtyClass(item)}">
							{fmtQty(item.quantity.current)}/{item.quantity.minimum}
						</span>
						<StatusBadge {...statusMeta(item.status)} />
						{#if canEdit}
							<div class="flex justify-end" onclick={(e) => e.stopPropagation()}>
								<RowActions
									actions={rowActions(item)}
									onSelect={(id) => onRowAction(item, id)}
								/>
							</div>
						{/if}
					</div>
				{/each}
			</div>

			<div class="border-t border-border px-4 py-3">
				<Pagination
					page={pagination?.page ?? 1}
					totalPages={pagination?.totalPages ?? 0}
					totalItems={pagination?.totalItems}
					pageSize={data.params.pageSize}
					onPage={onPage}
					onPageSize={onPageSize}
					label="itens"
				/>
			</div>
		</div>

		<!-- Cards mobile -->
		<div class="space-y-2 md:hidden" role="list">
			{#each items as item (item.id)}
				<div
					role="listitem"
					onclick={() => openDetail(item)}
					class="rounded-xl border border-border bg-surface p-4 transition-colors hover:bg-elevated/40 {selected.has(item.id)
						? 'border-brand'
						: ''}"
				>
					<div class="flex items-start justify-between gap-3">
						<div class="min-w-0">
							<p class="truncate font-mono text-xs text-muted">{item.code}</p>
							<p class="mt-0.5 truncate text-sm font-medium text-ink">{item.name}</p>
						</div>
						{#if canEdit}
							<div onclick={(e) => e.stopPropagation()}>
								<Checkbox checked={selected.has(item.id)} onChange={() => toggle(item.id)} />
							</div>
						{/if}
					</div>
					<div class="mt-2 flex items-center justify-between gap-2">
						<div class="flex items-center gap-2">
							<span
								class="rounded border border-border bg-elevated px-2 py-0.5 text-[10px] font-medium text-ink"
							>
								{item.category.label}
							</span>
							<span class="text-xs text-muted">{item.location.label}</span>
						</div>
						<span class="text-right font-mono text-sm {qtyClass(item)}">
							{fmtQty(item.quantity.current)}/{item.quantity.minimum}
						</span>
					</div>
					<div class="mt-2 flex items-center justify-between">
						<StatusBadge {...statusMeta(item.status)} />
						<Icon name="chevron-right" class="h-4 w-4 text-muted" />
					</div>
				</div>
			{/each}
			<div class="pt-1">
				<Pagination
					page={pagination?.page ?? 1}
					totalPages={pagination?.totalPages ?? 0}
					totalItems={pagination?.totalItems}
					pageSize={data.params.pageSize}
					onPage={onPage}
					onPageSize={onPageSize}
					label="itens"
				/>
			</div>
		</div>
	{/if}
</div>

{#if selectedCount > 0}
	<BulkActionsBar
		count={selectedCount}
		onCancel={clearSelection}
		actions={[
			{ label: 'Alterar categoria', onClick: () => (bulkOpen = true) },
			{ label: 'Excluir', danger: true, onClick: () => (deleteOpen = true) }
		]}
	/>
{/if}

<Modal
	open={bulkOpen}
	title="Alterar categoria"
	subtitle={`${selectedCount} item(ns) selecionado(s)`}
	onClose={busy ? undefined : () => (bulkOpen = false)}
	width="sm"
>
	{#snippet children()}
		<Select
			id="bulk-category"
			label="Nova categoria"
			options={cardCategories}
			value={categoryTarget}
			onChange={(v) => (categoryTarget = v)}
			placeholder="Selecione…"
		/>
	{/snippet}
	{#snippet footer()}
		<button
			onclick={() => (bulkOpen = false)}
			disabled={busy}
			class="rounded-lg border border-border bg-surface px-4 py-2 text-sm font-medium text-ink transition-colors hover:bg-border/40 disabled:opacity-50"
		>
			Cancelar
		</button>
		<button
			onclick={handleBulkCategory}
			disabled={busy || !categoryTarget}
			class="rounded-lg bg-brand px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-brandhi disabled:cursor-not-allowed disabled:opacity-50"
		>
			{busy ? 'Salvando…' : 'Aplicar'}
		</button>
	{/snippet}
</Modal>

<ConfirmDialog
	open={deleteOpen}
	title="Excluir itens"
	message={`Você está prestes a excluir ${selectedCount} item(ns). Esta ação não pode ser desfeita.`}
	confirmLabel="Excluir"
	loading={busy}
	onCancel={() => (deleteOpen = false)}
	onConfirm={handleBulkDelete}
/>