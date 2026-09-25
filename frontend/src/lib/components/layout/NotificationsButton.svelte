<script lang="ts">
	import { goto } from '$app/navigation';
	import { unreadCount } from '$lib/stores/notifications';

	let count = $state(0);

	$effect(() => {
		const unsub = unreadCount.subscribe((value) => {
			count = value;
		});
		return unsub;
	});
</script>

<button
	type="button"
	onclick={() => goto('/notificacoes')}
	aria-label="Notificações ({count} não lidas)"
	class="relative rounded-md p-2 text-muted transition hover:bg-elevated hover:text-ink"
>
	<svg
		class="h-4 w-4"
		fill="none"
		viewBox="0 0 24 24"
		stroke="currentColor"
		stroke-width="2"
		aria-hidden="true"
	>
		<path
			stroke-linecap="round"
			stroke-linejoin="round"
			d="M14.857 17.082a23.848 23.848 0 005.454-1.31A8.967 8.967 0 0118 9.75v-.7V9A6 6 0 006 9v.75a8.967 8.967 0 01-2.312 6.022c1.733.64 3.56 1.085 5.455 1.31m5.714 0a24.255 24.255 0 01-5.714 0m5.714 0a3 3 0 11-5.714 0"
		/>
	</svg>
	{#if count > 0}
		<span
			class="absolute -right-0.5 -top-0.5 rounded-full bg-danger px-1.5 py-0.5 text-[10px] font-semibold leading-none text-white"
		>
			{count}
		</span>
	{/if}
</button>