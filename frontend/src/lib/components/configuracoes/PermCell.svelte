<script lang="ts">
	import Select from '$lib/components/ui/Select.svelte';
	import type { Module } from '$lib/types/auth';
	import type { Nivel, PermissaoNivel } from '$lib/types/configuracoes';

	interface Props {
		modulo: Module;
		nivel: Nivel;
		valor: PermissaoNivel;
		admin: boolean;
		onChange?: (valor: PermissaoNivel) => void;
	}

	let { modulo, nivel, valor, admin, onChange }: Props = $props();

	const opcoes = [
		{ id: 'Editar', label: 'Editar' },
		{ id: 'Ver', label: 'Ver' },
		{ id: 'Nenhum', label: 'Nenhum' }
	];

	let selecao = $state<PermissaoNivel | null>(null);

	$effect(() => {
		void valor;
		selecao = null;
	});

	const atual = $derived(selecao ?? valor);
	const suja = $derived(atual !== valor);

	function mudar(novo: string): void {
		selecao = novo as PermissaoNivel;
		onChange?.(selecao);
	}
</script>

{#if admin}
	<div
		class="rounded-lg p-0.5 transition-colors {suja ? 'bg-brand/10 ring-2 ring-brand/50' : ''}"
		data-testid="cfg-perm-cell-{modulo}-{nivel}-{atual}"
	>
		<Select
			id="cfg-perm-{modulo}-{nivel}"
			label=""
			value={atual}
			options={opcoes}
			placeholder="Selecione…"
			onChange={mudar}
		/>
		{#if suja}
			<p class="mt-0.5 px-1 text-[10px] font-medium text-brandhi">não salvo</p>
		{/if}
	</div>
{/if}