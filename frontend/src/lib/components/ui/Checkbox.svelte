<script lang="ts">
	interface Props {
		checked?: boolean;
		indeterminate?: boolean;
		onChange?: (checked: boolean) => void;
		label?: string;
		disabled?: boolean;
	}

	let {
		checked = false,
		indeterminate = false,
		onChange,
		label = '',
		disabled = false
	}: Props = $props();
</script>

<label class="inline-flex cursor-pointer items-center gap-2 {disabled ? 'cursor-not-allowed opacity-50' : ''}">
	<input
		type="checkbox"
		{checked}
		{indeterminate}
		{disabled}
		onchange={(e) => onChange?.((e.currentTarget as HTMLInputElement).checked)}
		class="h-4 w-4 rounded border-border bg-surface text-brand accent-brand focus:ring-brand/40"
	/>
	{#if label}
		<span class="text-sm text-ink">{label}</span>
	{/if}
</label>