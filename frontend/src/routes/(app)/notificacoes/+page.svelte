<script lang="ts">
	import { goto } from '$app/navigation';
	import { page } from '$app/state';
	import { get } from 'svelte/store';
	import { listNotifications, markAllRead, markRead } from '$lib/api/notifications';
	import type { ListNotificationsParams } from '$lib/api/notifications';
	import type { Notification, NotificationType, PagedNotifications } from '$lib/types/notifications';
	import { NOTIFICATION_TYPES, TYPE_META } from '$lib/utils/notification-format';
	import { relativeTime } from '$lib/utils/format';
	import { unreadCount } from '$lib/stores/notifications';
	import { toasts, toastError } from '$lib/stores/toast';
	import Chip from '$lib/components/ui/Chip.svelte';
	import EmptyState from '$lib/components/ui/EmptyState.svelte';
	import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte';
	import Icon from '$lib/components/ui/Icon.svelte';
	import PageHeader from '$lib/components/ui/PageHeader.svelte';
	import Pagination from '$lib/components/ui/Pagination.svelte';
	import Select from '$lib/components/ui/Select.svelte';
	import Skeleton from '$lib/components/ui/Skeleton.svelte';
	import Tabs from '$lib/components/ui/Tabs.svelte';
	import NotificationRow from './components/NotificationRow.svelte';

	const PAGE_SIZE = 10;
	const SKELETON_ROWS = [0, 1, 2, 3, 4];

	const STATUS_TABS = [
		{ id: 'all', label: 'Todas' },
		{ id: 'unread', label: 'Não lidas' }
	];

	let data: PagedNotifications<Notification> | null = $state(null);
	let loading = $state(true);
	let error = $state<null | string>(null);
	let count = $state(get(unreadCount));
	let updatedAt = $state(new Date().toISOString());
	let retryTick = $state(0);
	let fetchCounter = $state(0);

	const query = $derived(page.url.searchParams);

	const currentPage = $derived.by(() => {
		const raw = Number(query.get('page') ?? '1');
		return Number.isFinite(raw) && raw >= 1 ? Math.floor(raw) : 1;
	});

	const currentType = $derived.by(() => {
		const raw = (query.get('type') ?? '').trim();
		return NOTIFICATION_TYPES.includes(raw as NotificationType) ? (raw as NotificationType) : '';
	});

	const currentStatus = $derived(query.get('status') === 'unread' ? 'unread' : 'all');

	const activeTab = $derived(currentStatus);

	const hasFilters = $derived(Boolean(currentType) || currentStatus === 'unread');

	const typeLabel = $derived.by(() => {
		if (!currentType) return '';
		return TYPE_META[currentType].label;
	});

	const typeOptions = $derived(NOTIFICATION_TYPES.map((t) => ({ id: t, label: TYPE_META[t].label })));

	const heartText = $derived.by(() => {
		if (!updatedAt) return '';
		return `Atualizado · ${relativeTime(updatedAt)}`;
	});

	$effect(() => {
		const type = currentType;
		const status = currentStatus;
		const targetPage = currentPage;
		void retryTick;

		loading = true;
		error = null;
		data = null;

		const fetchId = ++fetchCounter;

		const params: ListNotificationsParams = {
			page: targetPage,
			pageSize: PAGE_SIZE,
			type: type || undefined,
			read: status === 'unread' ? false : undefined
		};

		void listNotifications(params)
			.then((result) => {
				if (fetchId !== fetchCounter) return;
				data = result;
			})
			.catch((err: unknown) => {
				if (fetchId !== fetchCounter) return;
				error = err instanceof Error ? err.message : 'Falha ao carregar notificações';
			})
			.finally(() => {
				if (fetchId !== fetchCounter) return;
				loading = false;
				updatedAt = new Date().toISOString();
			});
	});

	$effect(() => {
		const unsubscribe = unreadCount.subscribe((value) => {
			count = value;
			updatedAt = new Date().toISOString();
		});
		return unsubscribe;
	});

	function navegar(overrides: { page?: number; type?: string; status?: 'all' | 'unread' } = {}): string {
		const type = overrides.type === undefined ? currentType : overrides.type;
		const status = overrides.status === undefined ? currentStatus : overrides.status;
		const targetPage = overrides.page ?? currentPage;

		const params = new URLSearchParams();
		if (targetPage > 1) params.set('page', String(targetPage));
		if (type) params.set('type', type);
		if (status === 'unread') params.set('status', 'unread');

		const queryString = params.toString();
		return queryString ? `/notificacoes?${queryString}` : '/notificacoes';
	}

	function onTab(id: string): void {
		if (id !== 'all' && id !== 'unread') return;
		void goto(navegar({ status: id, page: 1 }), { keepFocus: true });
	}

	function onTypeChange(value: string): void {
		const type = NOTIFICATION_TYPES.includes(value as NotificationType) ? value : '';
		void goto(navegar({ type, page: 1 }), { keepFocus: true });
	}

	function onPageClick(target: number): void {
		void goto(navegar({ page: target }), { keepFocus: true });
	}

	function limparTipo(): void {
		void goto(navegar({ type: '', page: 1 }), { keepFocus: true });
	}

	function limparStatus(): void {
		void goto(navegar({ status: 'all', page: 1 }), { keepFocus: true });
	}

	function limparFiltros(): void {
		void goto('/notificacoes', { keepFocus: true });
	}

	function retry(): void {
		retryTick += 1;
	}

	async function onRead(notification: Notification): Promise<void> {
		if (notification.read) return;

		notification.read = true;
		count = Math.max(0, count - 1);
		updatedAt = new Date().toISOString();

		try {
			await markRead(notification.id);
		} catch (err: unknown) {
			notification.read = false;
			count = get(unreadCount);
			toastError('Erro ao atualizar a notificação');
			void err;
		}
	}

	async function markAll(): Promise<void> {
		const items = data?.items ?? [];
		const snapshot = items.map((n) => n.read);
		items.forEach((n) => {
			n.read = true;
		});
		count = 0;
		updatedAt = new Date().toISOString();

		try {
			await markAllRead();
			count = get(unreadCount);
			toasts.success('Todas as notificações marcadas como lidas');
		} catch (err: unknown) {
			items.forEach((n, index) => {
				n.read = snapshot[index];
			});
			count = get(unreadCount);
			toastError('Erro ao marcar as notificações como lidas');
			void err;
		}
	}
</script>

<svelte:head>
	<title>Notificações · LAMS</title>
	<meta name="description" content="Central de notificações do LAMS." />
</svelte:head>

<div class="space-y-4">
	<PageHeader title="Notificações" subtitle="Central de notificações do LAMS">
		<span data-testid="notifications-heartbeat" class="text-xs tabnums text-muted">{heartText}</span>
		{#if count > 0}
			<button
				type="button"
				data-testid="notification-mark-all-read"
				onclick={markAll}
				class="flex items-center gap-1.5 rounded-md border border-border bg-elevated px-2.5 py-1.5 text-xs font-medium text-ink transition hover:border-brand/50 hover:text-brandhi"
			>
				<Icon name="check" class="h-3.5 w-3.5 text-success" />
				Marcar todas como lidas
			</button>
		{/if}
	</PageHeader>

	<div data-testid="notifications-filters" class="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
		<div class="flex flex-wrap items-center gap-2">
			<div data-testid="notifications-status-tabs">
				<Tabs tabs={STATUS_TABS} active={activeTab} onChange={onTab} />
			</div>
			<label for="notif-type" class="sr-only">Filtrar por tipo de notificação</label>
			<div class="w-full sm:w-60" data-testid="notifications-type-filter">
				<Select
					id="notif-type"
					label=""
					options={typeOptions}
					value={currentType}
					placeholder="Todos os tipos"
					onChange={onTypeChange}
				/>
			</div>
		</div>

		{#if hasFilters}
			<button
				type="button"
				data-testid="notifications-clear-filters"
				onclick={limparFiltros}
				class="inline-flex items-center gap-1.5 rounded-md px-2.5 py-1.5 text-xs font-medium text-muted transition hover:text-ink"
			>
				<Icon name="x-mark" class="h-3.5 w-3.5" />
				Limpar filtros
			</button>
		{/if}
	</div>

	<div class="rounded-lg border border-border bg-surface">
		{#if error}
			<div class="p-4" data-testid="notifications-error">
				<ErrorBanner
					message={error}
					hint="Não foi possível carregar suas notificações. Verifique sua conexão e tente novamente."
					onRetry={retry}
				/>
			</div>
		{:else if loading && data === null}
			<div class="divide-y divide-border/40" data-testid="notifications-skeleton">
				{#each SKELETON_ROWS as _, i (i)}
					<div class="flex items-start gap-3 px-4 py-4">
						<Skeleton class="mt-1 h-9 w-9 rounded-lg" />
						<div class="flex-1 space-y-2">
							<Skeleton class="h-4 w-1/3" />
							<Skeleton class="h-3 w-2/3" />
							<Skeleton class="h-3 w-1/4" />
						</div>
					</div>
				{/each}
			</div>
		{:else if data && data.items.length > 0}
			<div class="divide-y divide-border/40">
				{#each data.items as notification (notification.id)}
					<NotificationRow {notification} onRead={onRead} />
				{/each}
			</div>
			<div class="border-t border-border/60 px-4 py-3" data-testid="notifications-pagination">
				<Pagination page={currentPage} totalPages={data.totalPages} onPage={onPageClick} />
			</div>
		{:else if data && data.items.length === 0}
			{#if hasFilters}
				<div data-testid="notifications-empty-filtered">
					<EmptyState
						icon="filter"
						title="Nenhuma notificação encontrada"
						description="Não encontramos notificações para os filtros selecionados. Ajuste ou limpe os filtros para ver mais resultados."
					>
						<div class="flex flex-wrap items-center justify-center gap-2">
							{#if currentType}
								<Chip label={`Tipo: ${typeLabel}`} onRemove={limparTipo} />
							{/if}
							{#if currentStatus === 'unread'}
								<Chip label="Status: Não lidas" onRemove={limparStatus} />
							{/if}
							<button
								type="button"
								data-testid="notifications-clear-filters"
								onclick={limparFiltros}
								class="inline-flex items-center gap-1 rounded-full border border-border bg-elevated px-2.5 py-1 text-[11px] font-medium text-muted transition hover:text-ink"
							>
								<Icon name="x-mark" class="h-3 w-3" />
								Limpar filtros
							</button>
						</div>
					</EmptyState>
				</div>
			{:else}
				<div data-testid="notifications-empty">
					<EmptyState
						icon="bell"
						title="Nenhuma notificação por aqui"
						description="Quando algo novo chegar, você verá aqui. Enquanto isso, explore o painel."
					>
						<a
							href="/dashboard"
							class="inline-flex h-9 items-center gap-2 rounded-lg bg-brand px-4 text-sm font-medium text-ink transition hover:bg-brandhi"
						>
							Ir para o painel
							<Icon name="arrow-right" class="h-4 w-4" />
						</a>
					</EmptyState>
				</div>
			{/if}
		{/if}
	</div>
</div>