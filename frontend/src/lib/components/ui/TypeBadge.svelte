<script lang="ts">
	type Tone = 'brand' | 'success' | 'warn' | 'danger' | 'muted';

	const TONES: Record<Tone, string> = {
		success: 'bg-success/10 text-success',
		warn: 'bg-warn/10 text-warn',
		danger: 'bg-danger/10 text-danger',
		brand: 'bg-brand/10 text-brandhi',
		muted: 'bg-muted/10 text-muted'
	};

	interface Props {
		tone?: Tone;
		label: string;
	}

	let { tone = 'muted', label }: Props = $props();
</script>

<span
	aria-label={label}
	class="inline-flex items-center rounded-md px-2 py-0.5 text-xs font-medium whitespace-nowrap {TONES[tone]}"
>
	{label}
</span>
