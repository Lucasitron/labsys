<script lang="ts">
	import { formatMoneyBRL } from '$lib/utils/vendas-format';

	interface Props {
		value: number | null;
		label?: string;
	}

	let { value = $bindable(null), label = 'Valor' }: Props = $props();

	const id = $props.id();

	let texto = $state(value == null ? '' : formatMoneyBRL(value));
	let erro = $state('');
	let focado = $state(false);

	$effect(() => {
		if (!focado) {
			texto = value == null ? '' : formatMoneyBRL(value);
			if (value != null) erro = '';
		}
	});

	function aoDigitar(evento: Event): void {
		const alvo = evento.target as HTMLInputElement;
		const digitos = alvo.value.replace(/\D/g, '');
		if (digitos === '') {
			value = null;
			texto = alvo.value;
			erro = alvo.value.trim() === '' ? '' : 'Informe um valor em reais.';
			return;
		}
		value = Number(digitos) / 100;
		texto = formatMoneyBRL(value);
		erro = '';
	}
</script>

<div>
	{#if label}
		<label for={id} class="mb-1 block text-xs font-medium text-muted">{label}</label>
	{/if}
	<input
		{id}
		type="text"
		inputmode="decimal"
		autocomplete="off"
		placeholder="R$ 0,00"
		aria-label={label ? undefined : 'Valor em reais'}
		aria-invalid={erro !== ''}
		value={texto}
		oninput={aoDigitar}
		onfocus={() => (focado = true)}
		onblur={() => (focado = false)}
		class="w-full rounded-md border border-border bg-elevated px-3 py-2.5 font-mono text-sm text-ink placeholder:text-muted/60 focus:border-brand focus:ring-2 focus:ring-brand/30 focus:outline-none {erro
			? 'border-danger/60 focus:border-danger focus:ring-danger/30'
			: ''}"
	/>
	{#if erro}
		<p role="alert" class="mt-1 text-xs text-danger">{erro}</p>
	{/if}
</div>
