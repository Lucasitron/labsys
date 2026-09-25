<script lang="ts">
	import type { Tone } from '$lib/types/stock';
	import Icon, { type IconName } from './Icon.svelte';

	export interface RadioCardOption {
		id: string;
		label: string;
		description?: string;
		icon?: IconName | null;
		color?: Tone;
	}

	interface Props {
		options: RadioCardOption[];
		value: string;
		onChange: (id: string) => void;
		name: string;
		disabled?: boolean;
	}

	let { options, value, onChange, name, disabled = false }: Props = $props();

	const BORDER: Record<Tone, string> = {
		success: 'border-success text-success',
		warn: 'border-warn text-warn',
		danger: 'border-danger text-danger',
		brand: 'border-brand text-brandhi',
		muted: 'border-ink text-ink',
		ink: 'border-ink text-ink'
	};

	const DOT: Record<Tone, string> = {
		success: 'bg-success',
		warn: 'bg-warn',
		danger: 'bg-danger',
		brand: 'bg-brand',
		muted: 'bg-muted',
		ink: 'bg-ink'
	};
</script>

<div class="grid grid-cols-1 gap-2 sm:grid-cols-2 lg:grid-cols-4">
	{#each options as opt (opt.id)}
		<button
			type="button"
			name={name}
			value={opt.id}
			onclick={() => onChange(opt.id)}
			disabled={disabled}
			aria-pressed={value === opt.id}
			class="relative flex items-start gap-2.5 rounded-xl border bg-elevated p-3 text-left transition-all {value === opt.id
				? `${BORDER[opt.color ?? 'ink']} ring-1 ring-current`
				: 'border-border hover:border-brand/40'} disabled:cursor-not-allowed disabled:opacity-50"
		>
			<span
				class="mt-0.5 flex h-4 w-4 shrink-0 items-center justify-center rounded-full border-2 {value === opt.id
					? `border-current ${DOT[opt.color ?? 'ink']}`
					: 'border-border'}"
				aria-hidden="true"
			>
				{#if value === opt.id}
					<span class="h-1.5 w-1.5 rounded-full bg-base"></span>
				{/if}
			</span>
			<span class="min-w-0">
				<span class="flex items-center gap-1.5 text-sm font-medium text-ink {value === opt.id ? 'text-ink' : ''}">
					{#if opt.icon}
						<Icon name={opt.icon} class="h-4 w-4" />
					{/if}
					{opt.label}
				</span>
				{#if opt.description}
					<span class="mt-0.5 block text-xs text-muted">{opt.description}</span>
				{/if}
			</span>
		</button>
	{/each}
</div>