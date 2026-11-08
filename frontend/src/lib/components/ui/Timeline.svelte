<script lang="ts">
	import type { Tone } from '$lib/types/stock';

	export interface TimelineItem {
		id: string;
		tone: Tone;
		title: string;
		body?: string;
		meta?: string;
	}

	interface Props {
		items: TimelineItem[];
		label?: string;
	}

	let { items, label = 'Histórico' }: Props = $props();

	const DOTS: Record<Tone, string> = {
		success: 'bg-success',
		warn: 'bg-warn',
		danger: 'bg-danger',
		brand: 'bg-brand',
		muted: 'bg-muted',
		ink: 'bg-ink'
	};
</script>

<ul role="list" aria-label={label} class="relative space-y-4 border-l border-border pl-6">
	{#each items as item (item.id)}
		<li role="listitem" class="relative">
			<span
				aria-hidden="true"
				class="absolute top-1 -left-[31px] h-3 w-3 rounded-full ring-4 ring-surface {DOTS[item.tone]}"
			></span>
			<p class="text-sm font-medium text-ink">{item.title}</p>
			{#if item.body}
				<p class="mt-0.5 text-sm text-muted">{item.body}</p>
			{/if}
			{#if item.meta}
				<p class="mt-0.5 text-xs text-muted">{item.meta}</p>
			{/if}
		</li>
	{/each}
</ul>
