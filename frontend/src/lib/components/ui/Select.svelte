<script lang="ts">
	interface Option {
		id: string;
		label: string;
	}

	interface Props {
		id: string;
		label?: string;
		options: Option[];
		value?: string;
		placeholder?: string;
		onChange?: (value: string) => void;
		required?: boolean;
		disabled?: boolean;
		hint?: string;
	}

	let {
		id,
		label = '',
		options,
		value = '',
		placeholder = 'Selecione…',
		onChange,
		required = false,
		disabled = false,
		hint = ''
	}: Props = $props();
</script>

<div>
	{#if label}
		<label for={id} class="mb-1 block text-xs font-medium text-muted">
			{label}
			{#if required}<span class="text-danger">*</span>{/if}
		</label>
	{/if}
	<select
		{id}
		{value}
		onchange={(e) => onChange?.((e.currentTarget as HTMLSelectElement).value)}
		{required}
		{disabled}
		class="w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm text-ink focus:border-brand focus:outline-none focus:ring-1 focus:ring-brand/40 disabled:cursor-not-allowed disabled:opacity-50 {value
			? ''
			: 'text-muted'}"
	>
		<option value="" disabled>{placeholder}</option>
		{#each options as opt (opt.id)}
			<option value={opt.id} class="text-ink">{opt.label}</option>
		{/each}
	</select>
	{#if hint}
		<p class="mt-1 text-xs text-muted">{hint}</p>
	{/if}
</div>