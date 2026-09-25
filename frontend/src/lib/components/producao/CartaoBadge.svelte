<script lang="ts">
	import type { Cartao5S, Tone } from '$lib/types/producao';
	import { CARTAO_META } from '$lib/utils/producao-status';

	interface Props {
		cartao: Cartao5S;
		label?: string;
	}

	let { cartao, label = '' }: Props = $props();

	const meta = $derived(CARTAO_META[cartao]);
	const texto = $derived(label || meta.label);

	const TONES: Record<Tone, string> = {
		success: 'border-success/30 bg-success/15 text-success',
		warn: 'border-warn/30 bg-warn/15 text-warn',
		danger: 'border-danger/30 bg-danger/15 text-danger',
		brand: 'border-brand/30 bg-brand/15 text-brandhi',
		muted: 'border-border bg-elevated/40 text-muted',
		ink: 'border-border bg-elevated/40 text-ink'
	};

	const DOTS: Record<Tone, string> = {
		success: 'bg-success',
		warn: 'bg-warn',
		danger: 'bg-danger',
		brand: 'bg-brand',
		muted: 'bg-muted',
		ink: 'bg-ink'
	};
</script>

<span
	data-testid={`prd-cartao-${cartao.toLowerCase()}`}
	class="inline-flex items-center gap-1.5 rounded-full border px-2.5 py-0.5 text-xs font-semibold whitespace-nowrap {TONES[meta.color]}"
>
	<span class="h-1.5 w-1.5 rounded-full {DOTS[meta.color]}" aria-hidden="true"></span>
	{texto}
</span>