<script lang="ts">
	import { get } from 'svelte/store';
	import { auth } from '$lib/stores/auth';
	import { canSeeLoans, canSeeMachines } from '$lib/utils/permissions';
	import type { DashboardKpis } from '$lib/types/dashboard';
	import KpiCard from './KpiCard.svelte';

	interface Props {
		kpis: DashboardKpis | null;
		error?: boolean;
		empty?: boolean;
	}

	let { kpis, error = false, empty = false }: Props = $props();

	const user = $derived(get(auth).user);
	const showLoans = $derived(canSeeLoans(user));
	const showMachines = $derived(canSeeMachines(user));

	const orders = $derived(kpis?.ordersActive);
	const loans = $derived(kpis?.loansOpen);
	const machines = $derived(kpis?.machinesActive);
	const notifications = $derived(kpis?.notificationsUnread);
</script>

<div class="space-y-4">
	<KpiCard
		label="Encomendas ativas"
		value={orders?.value ?? 0}
		delta={orders?.delta}
		deltaTone={orders?.deltaTone ?? 'success'}
		{empty}
		{error}
		icon="orders"
	/>

	{#if showLoans}
		<KpiCard
			label="Empréstimos em aberto"
			value={loans?.value ?? 0}
			delta={loans?.delta}
			deltaTone={loans?.deltaTone ?? 'danger'}
			restricted={loans?.restricted ?? true}
			{empty}
			{error}
			icon="loans"
		/>
	{/if}

	{#if showMachines}
		<KpiCard
			label="Máquinas ativas"
			value={machines?.value ?? 0}
			total={machines?.total}
			delta={machines?.delta}
			deltaTone={machines?.deltaTone ?? 'warn'}
			restricted={machines?.restricted ?? true}
			{empty}
			{error}
			icon="machines"
		/>
	{/if}

	<KpiCard
		label="Notificações não lidas"
		value={notifications?.value ?? 0}
		delta={empty ? undefined : notifications?.delta ?? 'desde ontem'}
		deltaTone="muted"
		{empty}
		{error}
		icon="bell"
	/>
</div>