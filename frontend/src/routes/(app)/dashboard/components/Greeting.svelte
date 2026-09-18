<script lang="ts">
	import { firstName, greeting } from '$lib/utils/format';

	interface Props {
		name: string;
		sub?: string;
	}

	let { name, sub = 'Aqui está o resumo de hoje no FabLab.' }: Props = $props();

	const saudacao = $derived(greeting());
	const first = $derived(firstName(name));
</script>

<div>
	<h1 class="text-xl font-semibold tracking-tight">
		{saudacao}, {first}.
	</h1>
	<p class="mt-0.5 text-sm text-muted">{sub}</p>
</div>