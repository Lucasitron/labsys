<script lang="ts">
	import type { Snippet } from 'svelte';
	import SearchInput from './SearchInput.svelte';

	interface Props {
		onSearch: (value: string) => void;
		placeholder?: string;
		children?: Snippet;
		right?: Snippet;
		debounce?: number;
	}

	let { onSearch, placeholder, children, right, debounce = 350 }: Props = $props();
</script>

<div class="flex flex-wrap items-center gap-2">
	<div class="flex min-w-0 flex-1 items-center gap-2">
		<SearchInput onSearch={onSearch} {placeholder} delay={debounce} class="min-w-52 flex-1" />
		{#if children}
			{@render children()}
		{/if}
	</div>
	{#if right}
		<div class="flex items-center gap-2">{@render right()}</div>
	{/if}
</div>