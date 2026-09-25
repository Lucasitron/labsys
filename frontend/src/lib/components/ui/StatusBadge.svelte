<script lang="ts">
	import type { Tone } from '$lib/types/stock';
	import Icon, { type IconName } from './Icon.svelte';

	const TONES: Record<Tone, string> = {
		success: 'bg-success/10 text-success',
		warn: 'bg-warn/10 text-warn',
		danger: 'bg-danger/10 text-danger',
		brand: 'bg-brand/10 text-brandhi',
		muted: 'bg-muted/10 text-muted',
		ink: 'bg-ink/10 text-ink'
	};

	const DOTS: Record<Tone, string> = {
		success: 'bg-success',
		warn: 'bg-warn',
		danger: 'bg-danger',
		brand: 'bg-brand',
		muted: 'bg-muted',
		ink: 'bg-ink'
	};

	interface Props {
		label: string;
		color?: Tone;
		icon?: IconName | null;
		outline?: boolean;
	}

	let { label, color: tone = 'muted', icon = null, outline = false }: Props = $props();
</script>

<span
	class:outline
	class="inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-medium whitespace-nowrap {outline
		? 'border border-border bg-transparent text-muted'
		: TONES[tone]}"
>
	{#if icon}
		<Icon name={icon} class="h-3 w-3" />
	{:else}
		<span class="h-1.5 w-1.5 rounded-full {DOTS[tone]}" aria-hidden="true"></span>
	{/if}
	{label}
</span>