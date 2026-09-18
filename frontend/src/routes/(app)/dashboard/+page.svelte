<script lang="ts">
	import { get } from 'svelte/store';
	import type { PageProps } from './$types';
	import { fetchSummary } from '$lib/api/dashboard';
	import { auth } from '$lib/stores/auth';
	import { canSeeMachines } from '$lib/utils/permissions';
	import type { DashboardSummary } from '$lib/types/dashboard';
	import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte';
	import Skeleton from '$lib/components/ui/Skeleton.svelte';
	import Greeting from './components/Greeting.svelte';
	import MyTasks from './components/MyTasks.svelte';
	import KpiGrid from './components/KpiGrid.svelte';
	import OrdersChart from './components/OrdersChart.svelte';
	import MachinesChart from './components/MachinesChart.svelte';
	import ActivityFeed from './components/ActivityFeed.svelte';

	let { data }: PageProps = $props();

	let summary = $state<DashboardSummary | null>(null);
	let error = $state<string | null>(null);
	let loading = $state(false);

	let settled = false;
	$effect(() => {
		if (settled) return;
		settled = true;
		summary = data.summary;
		error = data.error;
	});

	const user = $derived(get(auth).user);
	const showMachines = $derived(canSeeMachines(user));
	const hasError = $derived(error !== null);

	const isEmpty = $derived(
		summary !== null &&
			summary.tasks.length === 0 &&
			summary.activity.length === 0 &&
			summary.ordersByStatus.length === 0
	);

	const greetingSub = $derived(
		hasError
			? 'Alguns dados não puderam ser carregados.'
			: isEmpty
				? 'Ainda não há dados para exibir. Comece por aqui:'
				: 'Aqui está o resumo de hoje no FabLab.'
	);

	async function refetch(): Promise<void> {
		loading = true;
		error = null;
		try {
			summary = await fetchSummary();
		} catch (err) {
			summary = null;
			error =
				err instanceof Error ? err.message : 'Não foi possível carregar o dashboard';
		} finally {
			loading = false;
		}
	}
</script>

<svelte:head>
	<title>Dashboard — FabLab Management</title>
</svelte:head>

{#if hasError}
	<ErrorBanner
		message="Não foi possível carregar o dashboard"
		hint="Alguns dados podem estar desatualizados. Tente novamente em instantes."
		onRetry={refetch}
	/>
{/if}

<Greeting name={user?.name ?? ''} sub={greetingSub} />

{#if loading && summary === null}
	<!-- ===== LOADING (skeletons sem layout shift) ===== -->
	<div class="grid grid-cols-1 gap-4 lg:grid-cols-3">
		<section class="space-y-3 rounded-xl border border-border bg-surface p-5 lg:col-span-2">
			<div class="mb-2 flex items-center justify-between">
				<Skeleton class="h-4 w-32" />
				<Skeleton class="h-3 w-16 opacity-70" />
			</div>
			<div class="space-y-3">
				{#each [0, 1, 2] as i (i)}
					<div class="flex items-start gap-3">
						<Skeleton class="mt-0.5 h-4 w-4" />
						<div class="flex-1 space-y-1.5">
							<Skeleton class="h-3 w-2/3" />
							<Skeleton class="h-2.5 w-1/3 opacity-60" />
						</div>
						<Skeleton class="h-4 w-12" />
					</div>
				{/each}
			</div>
		</section>
		<div class="space-y-4">
			{#each [0, 1, 2, 3] as i (i)}
				<div class="space-y-2 rounded-xl border border-border bg-surface p-4">
					<Skeleton class="h-3 w-24" />
					<Skeleton class="h-6 w-16" />
				</div>
			{/each}
		</div>
	</div>

	<div class="grid grid-cols-1 gap-4 md:grid-cols-2">
		{#each [0, 1] as i (i)}
			<section class="space-y-4 rounded-xl border border-border bg-surface p-5">
				<Skeleton class="h-4 w-40" />
				<div class="flex items-center gap-6">
					<div class="h-32 w-32 shrink-0 animate-pulse rounded-full border-[14px] border-elevated"></div>
					<div class="flex-1 space-y-2">
						<Skeleton class="h-3 w-full" />
						<Skeleton class="h-3 w-4/5 opacity-70" />
						<Skeleton class="h-3 w-3/5 opacity-70" />
						<Skeleton class="h-3 w-2/3 opacity-70" />
					</div>
				</div>
			</section>
		{/each}
	</div>

	<section class="space-y-4 rounded-xl border border-border bg-surface p-5">
		<Skeleton class="h-4 w-32" />
		<div class="space-y-3">
			<Skeleton class="h-3 w-2/3" />
			<Skeleton class="h-3 w-1/2 opacity-70" />
			<Skeleton class="h-3 w-3/5 opacity-70" />
		</div>
	</section>
{:else}
	<!-- ===== CONTEÚDO ===== -->
	<div class="grid grid-cols-1 gap-4 lg:grid-cols-3">
		<div class="lg:col-span-2">
			<MyTasks tasks={summary?.tasks ?? []} error={hasError} empty={isEmpty} onRefetch={refetch} />
		</div>
		<div>
			<KpiGrid kpis={summary?.kpis ?? null} error={hasError} empty={isEmpty} />
		</div>
	</div>

	<div class="grid grid-cols-1 gap-4 md:grid-cols-2">
		<OrdersChart
			slices={summary?.ordersByStatus ?? []}
			error={hasError}
			class={showMachines ? '' : 'md:col-span-2'}
		/>
		{#if showMachines}
			<MachinesChart slices={summary?.machinesByStatus ?? []} error={hasError} />
		{/if}
	</div>

	<ActivityFeed items={summary?.activity ?? []} error={hasError} empty={isEmpty} />
{/if}