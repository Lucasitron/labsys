<script lang="ts">
	import { initials } from '$lib/utils/format';
	import type { Tone } from '$lib/types/stock';

	const PALETTES: Record<Tone, string> = {
		success: 'bg-success/15 text-success',
		warn: 'bg-warn/15 text-warn',
		danger: 'bg-danger/15 text-danger',
		brand: 'bg-brand/15 text-brandhi',
		muted: 'bg-muted/15 text-muted',
		ink: 'bg-ink/10 text-ink'
	};

	interface Props {
		name?: string;
		tone?: Tone;
		size?: 'xs' | 'sm' | 'md';
		initialsOverride?: string;
	}

	let { name = '', tone = 'ink', size = 'sm', initialsOverride = '' }: Props = $props();

	const sizes = {
		xs: 'h-6 w-6 text-[10px]',
		sm: 'h-8 w-8 text-xs',
		md: 'h-10 w-10 text-sm'
	};
</script>

<span
	class="inline-flex shrink-0 items-center justify-center rounded-full font-semibold {sizes[size]} {PALETTES[tone]}"
	aria-hidden="true"
>
	{initialsOverride || initials(name)}
</span>