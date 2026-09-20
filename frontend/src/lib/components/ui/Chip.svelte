<script lang="ts">
	import Icon from './Icon.svelte';

	interface Props {
		label: string;
		count?: number;
		active?: boolean;
		onRemove?: () => void;
		onToggle?: () => void;
	}

	let { label, count = 0, active = false, onRemove, onToggle }: Props = $props();

	const removable = $derived(onRemove !== undefined);
	const clickable = $derived(onToggle !== undefined);
</script>

<span
	class:active
	onclick={clickable ? onToggle : undefined}
	role={clickable ? 'button' : undefined}
	tabindex={clickable ? 0 : undefined}
	onkeydown={(e) => {
		if (clickable && (e.key === 'Enter' || e.key === ' ')) {
			e.preventDefault();
			onToggle?.();
		}
	}}
	class="group inline-flex items-center gap-1 rounded-full border py-1 pl-3 pr-1.5 text-xs font-medium transition-colors {clickable
		? 'cursor-pointer border-border bg-surface hover:border-brand/50 hover:text-brandhi'
		: 'border-border bg-surface text-muted'} {active
		? 'border-brand border text-brandhi'
		: ''}"
>
	{label}
	{#if count > 0}
		<span class="text-[10px] font-bold text-muted">{count}</span>
	{/if}
	{#if removable}
		<button
			onclick={() => onRemove?.()}
			class="rounded-full p-0.5 text-muted transition-colors hover:bg-border/50 hover:text-ink"
			aria-label={`Remover filtro ${label}`}
		>
			<Icon name="x-mark" class="h-3 w-3" />
		</button>
	{/if}
</span>