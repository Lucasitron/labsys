<script lang="ts">
	import { browser } from '$app/environment';
	import { onMount, onDestroy } from 'svelte';
	import Sidebar from '$lib/components/layout/Sidebar.svelte';
	import Topbar from '$lib/components/layout/Topbar.svelte';
	import { startPolling, stopPolling } from '$lib/stores/notifications';

	let { children }: { children: import('svelte').Snippet } = $props();

	const COLLAPSE_KEY = 'fablab.sidebar.collapsed';

	let collapsed = $state(false);
	let mobileOpen = $state(false);

	$effect(() => {
		if (!browser) return;
		collapsed = localStorage.getItem(COLLAPSE_KEY) === 'true';
	});

	$effect(() => {
		if (!browser) return;
		const query = window.matchMedia('(min-width: 1024px)');
		if (query.matches) {
			mobileOpen = false;
		}

		function onChange(event: MediaQueryListEvent): void {
			if (event.matches) mobileOpen = false;
		}

		query.addEventListener('change', onChange);
		return () => query.removeEventListener('change', onChange);
	});

	function toggleCollapse(): void {
		collapsed = !collapsed;
		if (browser) localStorage.setItem(COLLAPSE_KEY, String(collapsed));
	}

	function closeMobile(): void {
		mobileOpen = false;
	}

	onMount(() => startPolling());
	onDestroy(() => stopPolling());
</script>

<div class="flex h-screen overflow-hidden bg-base text-ink">
	<a
		href="#main-content"
		class="sr-only focus:not-sr-only focus:absolute focus:left-2 focus:top-2 focus:z-[60] focus:rounded-md focus:bg-brand focus:px-3 focus:py-2 focus:text-xs focus:font-semibold focus:text-white"
	>
		Pular para o conteúdo
	</a>

	<Sidebar {collapsed} onCollapse={toggleCollapse} {mobileOpen} onCloseMobile={closeMobile} />

	<div class="flex min-w-0 flex-1 flex-col">
		<Topbar onToggleMenu={() => (mobileOpen = true)} />
		<main id="main-content" class="flex-1 space-y-6 overflow-y-auto p-6">
			{@render children()}
		</main>
	</div>
</div>