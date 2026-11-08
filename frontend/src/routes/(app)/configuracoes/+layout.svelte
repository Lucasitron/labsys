<script lang="ts">
	import ToastHost from '$lib/components/ui/ToastHost.svelte';

	let { children }: { children: import('svelte').Snippet } = $props();
</script>

{@render children()}
<ToastHost />