<script lang="ts">
	import { goto } from '$app/navigation';
	import type { Movement, Tone } from '$lib/types/stock';
	import { entryKindMeta, exitReasonMeta } from '$lib/utils/stock-status';
	import { fmtMoney, fmtDate, fmtQty } from '$lib/utils/stock-format';
	import type { MovementsResult } from '$lib/types/stock';
	import StatusBadge from '$lib/components/ui/StatusBadge.svelte';
	import Avatar from '$lib/components/ui/Avatar.svelte';
	import Chip from '$lib/components/ui/Chip.svelte';
	import SearchInput from '$lib/components/ui/SearchInput.svelte';
	import Dropdown from '$lib/components/ui/Dropdown.svelte';
	import Select from '$lib/components/ui/Select.svelte';
	import Pagination from '$lib/components/ui/Pagination.svelte';
	import TableSkeleton from '$lib/components/ui/TableSkeleton.svelte';
	import EmptyState from '$lib/components/ui/EmptyState.svelte';
	import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte';
	import Icon from '$lib/components/ui/Icon.svelte';

	export interface MovementListState {
		search: string;
		keys: string[];
		period: string;
		page: number;
		pageSize: number;
	}

	interface Props {
		kind: 'entrada' | 'saida';
		path: string;
		paramKey: 'kind' | 'reason';
		params: MovementListState;
		result: MovementsResult | null;
		error: string | null;
		canEdit: boolean;
	}

	let { kind, path, paramKey, params, result, error, canEdit }: Props = $props();

	const isIn = $derived(kind === 'entrada');
	const columns = $derived(isIn ? 'md:grid-cols-[110px_1fr_90px_120px_160px_110px]' : 'md:grid-cols-[110px_1fr_90px_150px_140px_180px]');

	const movements = $derived(result?.movements ?? []);
	const pagination = $derived(result?.pagination);
	const filters = $derived(result?.filters);

	const filterOptions = $derived(isIn ? filters?.kinds ?? [] : filters?.reasons ?? []);

	const FALLBACK_PERIODS = [
		{ id: 'today', label: 'Hoje' },
		{ id: '7d', label: 'Últimos 7 dias' },
		{ id: '30d', label: 'Últimos 30 dias' },
		{ id: 'year', label: 'Este ano' }
	];
	const periodOptions = $derived(
		filters?.periods && filters.periods.length > 0 ? filters.periods : FALLBACK_PERIODS
	);

	const hasFilters = $derived(params.search !== '' || params.keys.length > 0 || params.period !== '');

	const chipKeys = $derived(filterOptions.filter((f) => params.keys.includes(f.id)));
	const chipPeriod = $derived(periodOptions.find((p) => p.id === params.period));

	interface Query {
		[key: string]: string | number | string[] | undefined;
	}

	function updateUrl(overrides: Query): void {
		const base: Query = {
			search: params.search,
			[paramKey]: params.keys,
			period: params.period,
			page: params.page,
			pageSize: params.pageSize
		};
		const merged = { ...base, ...overrides };
		const search = new URLSearchParams();
		for (const [key, value] of Object.entries(merged)) {
			if (value === undefined) continue;
			if (Array.isArray(value)) {
				for (const v of value) if (v) search.append(key, v);
			} else if (value !== '') {
				search.append(key, String(value));
			}
		}
		const qs = search.toString();
		void goto(`${path}${qs ? `?${qs}` : ''}`, { invalidateAll: true });
	}

	function handleSearch(value: string): void {
		updateUrl({ search: value || undefined, page: 1 });
	}

	function toggleKey(id: string): void {
		const next = params.keys.includes(id)
			? params.keys.filter((v) => v !== id)
			: [...params.keys, id];
		updateUrl({ [paramKey]: next, page: 1 });
	}

	function clearKeys(): void {
		updateUrl({ [paramKey]: [], page: 1 });
	}

	function onPeriod(value: string): void {
		updateUrl({ period: value || undefined, page: 1 });
	}

	function clearAll(): void {
		updateUrl({ page: 1, pageSize: params.pageSize });
	}

	function onPage(page: number): void {
		updateUrl({ page });
	}

	function onPageSize(size: number): void {
		updateUrl({ pageSize: size, page: 1 });
	}

	function metaOf(m: Movement): { label: string; color: Tone } {
		return isIn
			? entryKindMeta(m.kind ?? 'ajuste')
			: exitReasonMeta(m.reason ?? 'consumo_interno');
	}
</script>

<div class="space-y-4">
	<!-- Toolkit -->
	<div class="flex flex-wrap items-center gap-2">
		<SearchInput
			value={params.search}
			onSearch={handleSearch}
			placeholder={isIn ? 'Buscar por item, fornecedor ou NF…' : 'Buscar por item, destino ou responsável…'}
			class="min-w-56 flex-1"
		/>
		<Dropdown
			label={isIn ? 'Tipo' : 'Motivo'}
			options={filterOptions}
			selected={params.keys}
			onToggle={toggleKey}
			onClear={clearKeys}
		/>
		<div class="w-44">
			<Select
				id="movement-period"
				options={periodOptions}
				value={params.period}
				onChange={onPeriod}
				placeholder="Período"
			/>
		</div>
	</div>

	{#if hasFilters}
		<div class="flex flex-wrap items-center gap-1.5">
			{#each chipKeys as k (k.id)}
				<Chip label={k.label} count={k.count} onRemove={() => toggleKey(k.id)} />
			{/each}
			{#if chipPeriod}
				<Chip label={chipPeriod.label} onRemove={() => onPeriod('')} />
			{/if}
			<button
				onclick={clearAll}
				class="text-xs font-medium text-brandhi transition-colors hover:text-brand"
			>
				Limpar filtros
			</button>
		</div>
	{/if}

	{#if error && !result}
		<ErrorBanner
			message={`Não foi possível carregar ${isIn ? 'as entradas' : 'as saídas'}`}
			hint={error}
			onRetry={clearAll}
		/>
	{:else if !result}
		<div class="overflow-hidden rounded-xl border border-border bg-surface">
			<div class="border-b border-border bg-elevated/50 px-4 py-2.5">
				<TableSkeleton rows={1} columns={3} class="!space-y-0" />
			</div>
			<div class="p-4"><TableSkeleton rows={7} /></div>
		</div>
	{:else if movements.length === 0}
		<div class="rounded-xl border border-border bg-surface">
			{#if hasFilters}
				<EmptyState
					icon="filter"
					title="Nenhum registro encontrado"
					description="Ajuste ou limpe os filtros para ver mais resultados."
				>
					{#snippet children()}
						<button
							onclick={clearAll}
							class="rounded-lg border border-border bg-surface px-3 py-2 text-sm font-medium text-ink transition-colors hover:border-brand/50 hover:text-brandhi"
						>
							Limpar filtros
						</button>
					{/snippet}
				</EmptyState>
			{:else}
				<EmptyState
					icon={isIn ? 'shopping-bag' : 'box'}
					title={isIn ? 'Sem entradas registradas' : 'Sem saídas registradas'}
					description={isIn ? 'Registre a primeira entrada de itens no estoque.' : 'Registre a primeira saída de itens do estoque.'}
				>
					{#snippet children()}
						{#if canEdit}
							<a
								href={isIn ? '/estoque/entradas/nova' : '/estoque/saidas/nova'}
								class="inline-flex items-center gap-1.5 rounded-lg bg-brand px-3 py-2 text-sm font-semibold text-white transition-colors hover:bg-brandhi"
							>
								<Icon name="plus" class="h-4 w-4" />
								{isIn ? 'Nova entrada' : 'Nova saída'}
							</a>
						{/if}
					{/snippet}
				</EmptyState>
			{/if}
		</div>
	{:else}
		<div class="hidden overflow-hidden rounded-xl border border-border bg-surface md:block">
			<div
				class="grid items-center gap-3 border-b border-border bg-elevated/50 px-4 py-2.5 text-[11px] font-medium uppercase tracking-wide text-muted {columns}"
			>
				<span>Data</span>
				<span>Item</span>
				<span class="text-right">Qtd</span>
				<span>{isIn ? 'Tipo' : 'Motivo'}</span>
				<span>{isIn ? 'Origem' : 'Destino'}</span>
				<span>{isIn ? 'Valor' : 'Responsável'}</span>
			</div>

			<div class="divide-y divide-border">
				{#each movements as m (m.id)}
					{@const meta = metaOf(m)}
					<div class="grid items-center gap-3 px-4 py-3 transition-colors hover:bg-elevated/40 {columns}">
						<span class="text-xs text-muted">{fmtDate(m.date)}</span>
						<div class="min-w-0">
							<p class="truncate font-mono text-xs text-muted">{m.item.code}</p>
							<p class="truncate text-sm text-ink">{m.item.name}</p>
						</div>
						<span class="text-right font-mono text-sm {isIn ? 'text-success' : 'text-danger'}">
							{isIn ? '+' : '−'}{fmtQty(m.quantity, m.item.unit)}
						</span>
						<span>
							<StatusBadge label={meta.label} color={meta.color} />
						</span>
						{#if isIn}
							<span class="truncate text-xs text-muted">{m.origin ?? '—'}</span>
							<span class="text-right font-mono text-xs text-muted">{fmtMoney(m.unitValue)}</span>
						{:else}
							<span class="truncate text-xs text-muted">{m.destination ?? '—'}</span>
							<span class="flex items-center gap-2">
								{#if m.responsible}
									<Avatar
										name={m.responsible.name}
										initialsOverride={m.responsible.initials}
										size="xs"
									/>
									<span class="text-sm text-ink">{m.responsible.name}</span>
								{:else}
									<span class="text-xs text-muted">—</span>
								{/if}
							</span>
						{/if}
					</div>
				{/each}
			</div>

			<div class="border-t border-border px-4 py-3">
				<Pagination
					page={pagination?.page ?? 1}
					totalPages={pagination?.totalPages ?? 0}
					totalItems={pagination?.totalItems}
					pageSize={params.pageSize}
					onPage={onPage}
					onPageSize={onPageSize}
					label={isIn ? 'entradas' : 'saídas'}
				/>
			</div>
		</div>

		<!-- mobile -->
		<div class="space-y-2 md:hidden" role="list">
			{#each movements as m (m.id)}
				{@const meta = metaOf(m)}
				<div role="listitem" class="rounded-xl border border-border bg-surface p-4">
					<div class="flex items-start justify-between gap-3">
						<div class="min-w-0">
							<p class="truncate font-mono text-xs text-muted">{m.item.code}</p>
							<p class="mt-0.5 truncate text-sm font-medium text-ink">{m.item.name}</p>
						</div>
						<span class="shrink-0 font-mono text-sm {isIn ? 'text-success' : 'text-danger'}">
							{isIn ? '+' : '−'}{fmtQty(m.quantity, m.item.unit)}
						</span>
					</div>
					<div class="mt-2 flex flex-wrap items-center justify-between gap-2">
						<StatusBadge label={meta.label} color={meta.color} />
						<span class="text-xs text-muted">{fmtDate(m.date)}</span>
					</div>
					<p class="mt-2 truncate text-xs text-muted">
						{isIn ? `Origem: ${m.origin ?? '—'}` : `Destino: ${m.destination ?? '—'}`}
					</p>
				</div>
			{/each}
			<div class="pt-2">
				<Pagination
					page={pagination?.page ?? 1}
					totalPages={pagination?.totalPages ?? 0}
					totalItems={pagination?.totalItems}
					pageSize={params.pageSize}
					onPage={onPage}
					onPageSize={onPageSize}
					label={isIn ? 'entradas' : 'saídas'}
				/>
			</div>
		</div>
	{/if}
</div>