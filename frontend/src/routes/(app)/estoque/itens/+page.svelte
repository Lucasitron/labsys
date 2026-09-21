<script lang="ts">
	import { goto, invalidateAll } from '$app/navigation';
	import type { PageProps } from './$types';
	import type { StockItem, Tone, FilterOption } from '$lib/types/stock';
	import { exportarCsv, importarCsv } from '$lib/api/stock/items';
	import { statusMeta, quantityTone, categoriaMeta, localizacaoLabel } from '$lib/utils/stock-status';
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
	import Modal from '$lib/components/ui/Modal.svelte';
	import Icon from '$lib/components/ui/Icon.svelte';

	let { data }: PageProps = $props();
	const canEdit = $derived(data.canEdit ?? false);

	const result = $derived(data.result);
	const insError = $derived(data.error);

	const items = $derived(result?.items ?? []);
	const pagination = $derived(result?.pagination);
	const filters = $derived(result?.filters);

	const chipCats = $derived(
		(filters?.categories ?? [] as FilterOption[]).filter((c) => data.params.categories.includes(c.id))
	);
	const chipLocs = $derived(
		(filters?.locations ?? [] as FilterOption[]).filter((l) => data.params.locations.includes(l.id))
	);
	const chipStatus = $derived(
		(filters?.statuses ?? [] as FilterOption[]).filter((s) => data.params.statuses.includes(s.id))
	);

	const hasFilters = $derived(
		data.params.search !== '' ||
			data.params.categories.length > 0 ||
			data.params.locations.length > 0 ||
			data.params.statuses.length > 0
	);

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

	async function handleExport(): Promise<void> {
		try {
			await exportarCsv();
			toasts.info('Exportação CSV iniciada');
		} catch (err) {
			toastError(err, 'Não foi possível exportar');
		}
	}

	// ---- Importar CSV (multipart) ----

	let importOpen = $state(false);
	let importFile = $state<File | null>(null);
	let importError = $state('');
	let importing = $state(false);

	function openImport(): void {
		importFile = null;
		importError = '';
		importOpen = true;
	}

	async function handleImport(e: Event): Promise<void> {
		e.preventDefault();
		if (!importFile) {
			importError = 'Selecione um arquivo CSV para importar';
			return;
		}
		importing = true;
		importError = '';
		try {
			const criados = await importarCsv(importFile);
			toasts.success(
				criados.length === 1 ? '1 item importado' : `${criados.length} itens importados`
			);
			importOpen = false;
			await invalidateAll();
		} catch (err) {
			importError = err instanceof Error ? err.message : 'Não foi possível importar o arquivo';
		} finally {
			importing = false;
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
		return QTY_TONES[quantityTone(item.quantidadeAtual, item.estoqueMinimo)];
	}

	function categoryLabel(item: StockItem): string {
		return categoriaMeta(item.categoria).label;
	}

	function locationLabel(item: StockItem): string {
		return localizacaoLabel(item.localizacao);
	}

	const gridCols = 'md:grid-cols-[1fr_120px_130px_90px_120px]';

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
				data-testid="it-exportar"
				onclick={handleExport}
				class="inline-flex items-center gap-1.5 rounded-lg border border-border bg-surface px-3 py-2 text-sm font-medium text-ink transition-colors hover:border-brand/50 hover:text-brandhi"
			>
				<Icon name="arrow-down-tray" class="h-4 w-4" /> Exportar
			</button>
			{#if canEdit}
				<button
					data-testid="it-importar"
					onclick={openImport}
					class="inline-flex items-center gap-1.5 rounded-lg border border-border bg-surface px-3 py-2 text-sm font-medium text-ink transition-colors hover:border-brand/50 hover:text-brandhi"
				>
					<Icon name="arrow-up-tray" class="h-4 w-4" /> Importar
				</button>
			{/if}
			{#if canEdit}
				<a
					data-testid="it-novo"
					href="/estoque/itens/novo"
					class="inline-flex items-center gap-1.5 rounded-lg bg-brand px-3 py-2 text-sm font-semibold text-white shadow-lg shadow-brand/20 transition-colors hover:bg-brandhi"
				>
					<Icon name="plus" class="h-4 w-4" /> Novo item
				</a>
			{/if}
		{/snippet}
	</PageHeader>

	<!-- Toolkit: busca + filtros -->
	<div class="flex flex-wrap items-center gap-2" data-testid="it-filtros">
		<SearchInput
			value={data.params.search}
			onSearch={handleSearch}
			placeholder="Buscar por nome…"
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
			options={filters?.statuses ?? []}
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
				<Chip label={s.label} count={s.count} onRemove={() => toggleFilter('status', s.id)} />
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
		<div data-testid="it-error">
			<ErrorBanner
				message="Não foi possível carregar os itens"
				hint={insError}
				onRetry={invalidateAll}
			/>
		</div>
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
		<div class="rounded-xl border border-border bg-surface" data-testid="it-empty">
			{#if hasFilters}
				<EmptyState
					icon="filter"
					title="Nenhum item encontrado"
					description="Ajuste ou limpe os filtros para ver mais resultados."
				>
					{#snippet children()}
						<button
							data-testid="it-empty-limpar"
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
								data-testid="it-empty-novo"
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
		<div class="hidden overflow-hidden rounded-xl border border-border bg-surface md:block" data-testid="it-tabela">
			<div
				class="grid items-center gap-3 border-b border-border bg-elevated/50 px-4 py-2.5 text-[11px] font-medium uppercase tracking-wide text-muted {gridCols}"
			>
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
			</div>

			<div class="divide-y divide-border">
				{#each items as item (item.id)}
					<div
						data-testid="it-linha-{item.id}"
						onclick={() => openDetail(item)}
						role="button"
						tabindex="0"
						onkeydown={(e) => {
							if (e.key === 'Enter') openDetail(item);
						}}
						class="grid cursor-pointer items-center gap-3 px-4 py-3 transition-colors hover:bg-elevated/40 {gridCols} border-l-2 border-transparent"
					>
						<div class="min-w-0">
							<p class="truncate text-sm text-ink">{item.nome}</p>
							<p class="truncate text-xs text-muted">{item.unidadeMedida}</p>
						</div>
						<span
							class="w-fit rounded border border-border bg-elevated px-2 py-0.5 text-[10px] font-medium text-ink"
						>
							{categoryLabel(item)}
						</span>
						<span class="truncate text-xs text-muted">{locationLabel(item)}</span>
						<span class="text-right font-mono text-sm {qtyClass(item)}">
							{fmtQty(item.quantidadeAtual)}/{fmtQty(item.estoqueMinimo)}
						</span>
						<StatusBadge {...statusMeta(item.status)} />
					</div>
				{/each}
			</div>

			<div class="border-t border-border px-4 py-3" data-testid="it-paginacao">
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
					data-testid="it-card-{item.id}"
					role="button"
					tabindex="0"
					onclick={() => openDetail(item)}
					onkeydown={(e) => {
						if (e.key === 'Enter') openDetail(item);
					}}
					class="rounded-xl border border-border bg-surface p-4 transition-colors hover:bg-elevated/40"
				>
					<div class="flex items-start justify-between gap-3">
						<div class="min-w-0">
							<p class="truncate text-sm font-medium text-ink">{item.nome}</p>
						</div>
					</div>
					<div class="mt-2 flex items-center justify-between gap-2">
						<div class="flex items-center gap-2">
							<span
								class="rounded border border-border bg-elevated px-2 py-0.5 text-[10px] font-medium text-ink"
							>
								{categoryLabel(item)}
							</span>
							<span class="text-xs text-muted">{locationLabel(item)}</span>
						</div>
						<span class="text-right font-mono text-sm {qtyClass(item)}">
							{fmtQty(item.quantidadeAtual)}/{fmtQty(item.estoqueMinimo)}
						</span>
					</div>
					<div class="mt-2 flex items-center justify-between">
						<StatusBadge {...statusMeta(item.status)} />
						<Icon name="chevron-right" class="h-4 w-4 text-muted" />
					</div>
				</div>
			{/each}
			<div class="pt-1" data-testid="it-paginacao-mobile">
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

	<!-- Importar CSV -->
	<Modal
		open={importOpen}
		title="Importar itens (CSV)"
		subtitle="O arquivo deve seguir o formato de exportação dos itens."
		onClose={() => (importOpen = false)}
		width="md"
	>
		{#snippet children()}
			<form onsubmit={handleImport} data-testid="it-import-modal" novalidate>
				<div class="space-y-4">
					<div>
						<label
							for="it-import-file"
							class="mb-1 block text-xs font-medium text-muted"
						>
							Arquivo CSV <span class="text-danger">*</span>
						</label>
						<input
							id="it-import-file"
							data-testid="it-import-file"
							type="file"
							accept=".csv,text/csv"
							onchange={(e) => {
								importFile = (e.currentTarget as HTMLInputElement).files?.[0] ?? null;
								importError = '';
							}}
							aria-label="Selecionar arquivo CSV de itens"
							class="w-full rounded-lg border border-border bg-elevated px-3 py-2.5 text-sm text-ink file:mr-3 file:rounded-md file:border-0 file:bg-brand file:px-3 file:py-1.5 file:text-sm file:font-semibold file:text-white"
						/>
						{#if importError}
							<p class="mt-2 rounded-lg border border-danger/30 bg-danger/5 px-3 py-2 text-xs text-danger" data-testid="it-import-error">
								{importError}
							</p>
						{/if}
					</div>
					<div class="flex flex-wrap items-center justify-end gap-2">
						<button
							type="button"
							data-testid="it-import-cancelar"
							onclick={() => (importOpen = false)}
							class="rounded-lg border border-border bg-surface px-3 py-2 text-sm font-medium text-ink transition-colors hover:bg-border/40"
						>
							Cancelar
						</button>
						<button
							type="submit"
							data-testid="it-import-enviar"
							disabled={importing}
							class="inline-flex items-center gap-1.5 rounded-lg bg-brand px-3 py-2 text-sm font-semibold text-white shadow-lg shadow-brand/20 transition-colors hover:bg-brandhi disabled:cursor-not-allowed disabled:opacity-50"
						>
							<Icon name="arrow-up-tray" class="h-4 w-4" />
							{importing ? 'Importando…' : 'Importar'}
						</button>
					</div>
				</div>
			</form>
		{/snippet}
	</Modal>
</div>