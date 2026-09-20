<script lang="ts">
	interface Props {
		rows?: number;
		columns?: number;
		class?: string;
	}

	let { rows = 5, columns = 5, class: className = 'w-full' }: Props = $props();
</script>

<div class="animate-pulse space-y-2 {className}" aria-hidden="true">
	{#each Array(rows) as _, r (r)}
		{#each Array(columns) as _, c (c)}
			<span
				class="mb-2 mr-2 inline-block h-3 rounded bg-muted/15"
				style:width={`${Math.min(100, 30 + ((r * 13 + c * 29) % 60))}%`}
			></span>
		{/each}
		<br />
	{/each}
</div>