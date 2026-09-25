<script lang="ts">
	import { history } from '$lib/api/notifications';
	import type {
		HistoryEntry,
		HistoryFilters,
		NotificationChannel,
		NotificationType
	} from '$lib/types/notifications';
	import { CHANNEL_BADGE, NOTIFICATION_TYPES, TONE_STYLES, TYPE_META } from '$lib/utils/notification-format';
	import Chip from '$lib/components/ui/Chip.svelte';
	import EmptyState from '$lib/components/ui/EmptyState.svelte';
	import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte';
	import Icon from '$lib/components/ui/Icon.svelte';
	import PageHeader from '$lib/components/ui/PageHeader.svelte';
	import Pagination from '$lib/components/ui/Pagination.svelte';
	import SearchInput from '$lib/components/ui/SearchInput.svelte';
	import Select from '$lib/components/ui/Select.svelte';
	import StatusBadge from '$lib/components/ui/StatusBadge.svelte';
	import TableSkeleton from '$lib/components/ui/TableSkeleton.svelte';

	const PAGE_SIZE = 10;

	const PERIODS = [
		{ id: 'today', label: 'Hoje' },
		{ id: '7d', label: 'Últimos 7 dias' },
		{ id: '30d', label: 'Últimos 30 dias' },
		{ id: 'year', label: 'Este ano' }
	];

	const STATUS_OPTIONS = [
		{ id: 'all', label: 'Todas' },
		{ id: 'unread', label: 'Não lidas' },
		{ id: 'read', label: 'Lidas' }
	];

	type StatusFilter = 'all' | 'unread' | 'read';

	const STATUS_LABELS: Record<Exclude<StatusFilter, 'all'>, string> = {
		unread: 'Não lidas',
		read: 'Lidas'
	};

	const tipoOptions = NOTIFICATION_TYPES.map((t) => ({ id: t, label: TYPE_META[t].label }));
	const canalOptions = (Object.keys(CHANNEL_BADGE) as NotificationChannel[]).map((c) => ({
		id: c,
		label: CHANNEL_BADGE[c].label
	}));

	let busca = $state('');
	let tipo = $state<'' | NotificationType>('');
	let canal = $state<'' | NotificationChannel>('');
	let statusLeitura = $state<StatusFilter>('all');
	let periodo = $state('');
	let pageNumber = $state(1);

	let allItems: HistoryEntry[] = $state([]);
	let totalItems = $state(0);
	let loading = $state(true);
	let error = $state<string | null>(null);
	let retryTick = $state(0);
	let fetchCounter = 0;

	const filters = $derived.by(() => {
		const f: HistoryFilters = {};
		if (busca.trim()) f.search = busca.trim();
		if (tipo) f.type = tipo as NotificationType;
		if (canal) f.channel = canal as NotificationChannel;
		if (statusLeitura === 'unread') f.read = false;
		if (statusLeitura === 'read') f.read = true;
		if (periodo) f.period = periodo;
		return f;
	});

	const hasFilters = $derived(
		busca.trim() !== '' || tipo !== '' || canal !== '' || statusLeitura !== 'all' || periodo !== ''
	);

	const totalPages = $derived(Math.max(1, Math.ceil(totalItems / PAGE_SIZE)));

	const pageItems = $derived.by(() => {
		const start = (pageNumber - 1) * PAGE_SIZE;
		return allItems.slice(start, start + PAGE_SIZE);
	});

	const chipPeriodo = $derived(PERIODS.find((p) => p.id === periodo));

	$effect(() => {
		void retryTick;
		loading = true;
		error = null;
		const id = ++fetchCounter;
		void history({ ...filters })
			.then((res) => {
				if (id !== fetchCounter) return;
				allItems = res.items;
				totalItems = res.total;
			})
			.catch((err: unknown) => {
				if (id !== fetchCounter) return;
				error =
					err instanceof Error ? err.message : 'Não foi possível carregar o histórico.';
			})
			.finally(() => {
				if (id !== fetchCounter) return;
				loading = false;
			});
	});

	function onSearch(value: string): void {
		busca = value;
		pageNumber = 1;
	}

	function onTipo(value: string): void {
		tipo = NOTIFICATION_TYPES.includes(value as NotificationType)
			? (value as NotificationType)
			: '';
		pageNumber = 1;
	}

	function onCanal(value: string): void {
		canal = canalOptions.some((c) => c.id === value) ? (value as NotificationChannel) : '';
		pageNumber = 1;
	}

	function onStatus(value: string): void {
		statusLeitura = value === 'unread' || value === 'read' ? value : 'all';
		pageNumber = 1;
	}

	function onPeriodo(value: string): void {
		periodo = value;
		pageNumber = 1;
	}

	function limparTodos(): void {
		busca = '';
		tipo = '';
		canal = '';
		statusLeitura = 'all';
		periodo = '';
		pageNumber = 1;
	}

	function onPage(pagina: number): void {
		pageNumber = pagina;
	}

	function formatarEnvio(iso: string): string {
		const data = new Date(iso);
		if (Number.isNaN(data.getTime())) return '—';
		return new Intl.DateTimeFormat('pt-BR', {
			day: '2-digit',
			month: '2-digit',
			year: 'numeric',
			hour: '2-digit',
			minute: '2-digit'
		}).format(data);
	}
</script>

<svelte:head>
	<title>Histórico · Notificações · FabLab</title>
	<meta name="description" content="Histórico de todas as notificações enviadas no sistema." />
</svelte:head>

<div class="space-y-4">
	<PageHeader
		title="Histórico de notificações"
		subtitle="Todas as notificações enviadas no sistema, com filtros para auditoria."
	>
		{#snippet children()}
			<StatusBadge label="Restrito ao Admin" color="warn" icon="lock" />
		{/snippet}
	</PageHeader>

	<div data-testid="history-filters" class="flex flex-wrap items-end gap-2">
		<div class="min-w-56 flex-1">
			<SearchInput
				value={busca}
				onSearch={onSearch}
				placeholder="Buscar por título ou destinatário…"
				label="Buscar no histórico"
			/>
		</div>
		<div class="w-40">
			<Select
				id="hist-tipo"
				label="Tipo"
				options={tipoOptions}
				value={tipo}
				onChange={onTipo}
				placeholder="Todos"
			/>
		</div>
		<div class="w-40">
			<Select
				id="hist-canal"
				label="Canal"
				options={canalOptions}
				value={canal}
				onChange={onCanal}
				placeholder="Todos"
			/>
		</div>
		<div class="w-40">
			<Select
				id="hist-status"
				label="Status"
				options={STATUS_OPTIONS}
				value={statusLeitura}
				onChange={onStatus}
				placeholder="Todas"
			/>
		</div>
		<div class="w-44">
			<Select
				id="hist-periodo"
				label="Período"
				options={PERIODS}
				value={periodo}
				onChange={onPeriodo}
				placeholder="Qualquer"
			/>
		</div>
	</div>

	{#if hasFilters}
		<div class="flex flex-wrap items-center gap-1.5">
			{#if busca.trim()}
				<Chip label={`Busca: ${busca.trim()}`} onRemove={() => onSearch('')} />
			{/if}
			{#if tipo}
				<Chip label={TYPE_META[tipo].label} onRemove={() => onTipo('')} />
			{/if}
			{#if canal}
				<Chip label={CHANNEL_BADGE[canal].label} onRemove={() => onCanal('')} />
			{/if}
			{#if statusLeitura !== 'all'}
				<Chip label={STATUS_LABELS[statusLeitura]} onRemove={() => onStatus('all')} />
			{/if}
			{#if chipPeriodo}
				<Chip label={chipPeriodo.label} onRemove={() => onPeriodo('')} />
			{/if}
			<button
				type="button"
				data-testid="history-clear-filters"
				onclick={limparTodos}
				class="text-xs font-medium text-brandhi transition-colors hover:text-brand"
			>
				Limpar filtros
			</button>
		</div>
	{/if}

	<div class="overflow-hidden rounded-xl border border-border bg-surface">
		{#if error}
			<div class="p-4" data-testid="history-error">
				<ErrorBanner
					message={error}
					hint="Não foi possível carregar o histórico. Verifique sua conexão e tente novamente."
					onRetry={() => {
						retryTick += 1;
					}}
				/>
			</div>
		{:else if loading}
			<div aria-busy="true" data-testid="history-skeleton">
				<div class="border-b border-border bg-elevated/50 px-4 py-2.5">
					<TableSkeleton rows={1} columns={6} class="!space-y-0" />
				</div>
				<div class="p-4">
					<TableSkeleton rows={7} columns={6} />
				</div>
			</div>
		{:else if allItems.length === 0}
			{#if hasFilters}
				<div data-testid="history-empty-filtered">
					<EmptyState
						icon="filter"
						title="Nenhum envio encontrado"
						description="Não encontramos envios para os filtros selecionados. Ajuste ou limpe os filtros para ver mais resultados."
					>
						{#snippet children()}
							<button
								type="button"
								onclick={limparTodos}
								class="rounded-lg border border-border bg-surface px-3 py-2 text-sm font-medium text-ink transition-colors hover:border-brand/50 hover:text-brandhi"
							>
								Limpar filtros
							</button>
						{/snippet}
					</EmptyState>
				</div>
			{:else}
				<div data-testid="history-empty">
					<EmptyState
						icon="bell"
						title="Nenhum envio registrado"
						description="Notificações enviadas pelo sistema aparecerão aqui para auditoria."
					/>
				</div>
			{/if}
		{:else}
			<div class="overflow-x-auto">
				<table data-testid="history-table" class="w-full min-w-[820px] text-sm">
					<thead>
						<tr class="border-b border-border text-left text-[11px] uppercase tracking-wide text-muted">
							<th class="bg-elevated/50 px-4 py-2.5 font-medium">Notificação</th>
							<th class="bg-elevated/50 px-4 py-2.5 font-medium">Tipo</th>
							<th class="bg-elevated/50 px-4 py-2.5 font-medium">Destinatário</th>
							<th class="bg-elevated/50 px-4 py-2.5 font-medium">Canal</th>
							<th class="bg-elevated/50 px-4 py-2.5 font-medium">Status</th>
							<th class="bg-elevated/50 px-4 py-2.5 font-medium">Enviada em</th>
						</tr>
					</thead>
					<tbody>
						{#each pageItems as row (row.id)}
							{@const meta = TYPE_META[row.type] ?? TYPE_META.sistema}
							<tr
								data-testid="history-row"
								class="border-b border-border transition-colors hover:bg-elevated/40"
							>
								<td class="px-4 py-3">
									<p class="max-w-72 truncate font-medium text-ink">{row.title}</p>
								</td>
								<td class="px-4 py-3">
									<span
										class="inline-flex items-center gap-1 rounded-full border px-2 py-0.5 text-[11px] font-medium whitespace-nowrap {TONE_STYLES[meta.tone]}"
									>
										<Icon name={meta.icon} class="h-3 w-3" />
										{meta.label}
									</span>
								</td>
								<td class="px-4 py-3">
									<span class="truncate text-xs text-muted">{row.recipient}</span>
								</td>
								<td class="px-4 py-3">
									<StatusBadge
										label={CHANNEL_BADGE[row.channel].label}
										color={CHANNEL_BADGE[row.channel].tone}
									/>
								</td>
								<td class="px-4 py-3">
									{#if row.read}
										<StatusBadge label="Lida" color="muted" />
									{:else}
										<StatusBadge label="Não lida" color="brand" />
									{/if}
								</td>
								<td class="px-4 py-3">
									<span class="font-mono text-xs tabnums text-muted">
										{formatarEnvio(row.sentAt)}
									</span>
								</td>
							</tr>
						{/each}
					</tbody>
				</table>
			</div>
			<div class="border-t border-border px-4 py-3" data-testid="history-pagination">
				<Pagination
					page={pageNumber}
					totalPages={totalPages}
					totalItems={totalItems}
					onPage={onPage}
					label="envios"
				/>
			</div>
		{/if}
	</div>
</div>