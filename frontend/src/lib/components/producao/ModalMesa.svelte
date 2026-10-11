<script lang="ts">
	import { onMount } from 'svelte';
	import type { Mesa5S, NomeFunc, Projeto } from '$lib/types/producao';
	import { criarMesa } from '$lib/api/producao/client';
	import { ApiError, NetworkError } from '$lib/api/client';
	import Modal from '$lib/components/ui/Modal.svelte';

	interface Props {
		open: boolean;
		projetos: Projeto[];
		membros: NomeFunc[];
		onClose: () => void;
		onCriada?: (mesa: Mesa5S) => void;
	}

	let { open, membros, onClose, onCriada }: Props = $props();

	let nome = $state('');
	let mesaNumero = $state('');
	let membroId = $state('');
	let prazo = $state('');
	let enviando = $state(false);
	let erro = $state('');

	$effect(() => {
		if (open) {
			erro = '';
			nome = '';
			mesaNumero = '';
			membroId = '';
			prazo = '';
		}
	});

	onMount(() => {
		const onKey = (e: KeyboardEvent): void => {
			if (open && e.key === 'Escape') onClose();
		};
		document.addEventListener('keydown', onKey);
		return () => document.removeEventListener('keydown', onKey);
	});

	function mensagemErro(err: unknown): string {
		if (err instanceof ApiError || err instanceof NetworkError) return err.message;
		if (err instanceof Error) return err.message;
		return 'Não foi possível criar a mesa.';
	}

	async function salvar(event: SubmitEvent): Promise<void> {
		event.preventDefault();
		enviando = true;
		erro = '';
		try {
			const mesa = await criarMesa({
				idFuncionario: membroId,
				idMesa: mesaNumero,
				nomeProjeto: nome.trim(),
				prazoExecucao: prazo || undefined
			});
			onCriada?.(mesa);
		} catch (err) {
			erro = mensagemErro(err);
		} finally {
			enviando = false;
		}
	}

	const inputCls =
		'w-full rounded-lg border border-border bg-elevated px-3 py-2.5 text-sm text-ink placeholder:text-muted/60 focus:border-brand focus:outline-none focus:ring-1 focus:ring-brand/40';
</script>

<Modal {open} title="Criar mesa de projeto" subtitle="Vincular projeto universitário à mesa com QR no totem" {onClose} width="md">
	{#snippet children()}
		<form id="modal-mesa-form" data-testid="mesa-criar" onsubmit={salvar} novalidate>
			{#if erro}
				<div role="alert" class="mb-4 rounded-xl border border-danger/30 bg-danger/10 px-4 py-3 text-sm text-danger">
					{erro}
				</div>
			{/if}

			<label class="block">
				<span class="mb-1.5 block text-xs font-medium text-muted">Nome do projeto *</span>
				<input
					type="text"
					bind:value={nome}
					required
					maxlength="150"
					placeholder="ex.: Robô seguidor de linha"
					class={inputCls}
				/>
			</label>

			<div class="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2">
				<label class="block">
					<span class="mb-1.5 block text-xs font-medium text-muted">Número da mesa *</span>
					<input
						type="number"
						bind:value={mesaNumero}
						required
						min="1"
						step="1"
						placeholder="ex.: 4"
						class={inputCls}
					/>
				</label>

				<label class="block">
					<span class="mb-1.5 block text-xs font-medium text-muted">Prazo de execução</span>
					<input type="date" bind:value={prazo} class={inputCls} />
				</label>
			</div>

			<label class="mt-4 block">
				<span class="mb-1.5 block text-xs font-medium text-muted">Membro responsável *</span>
				<select bind:value={membroId} required class={inputCls}>
					<option value="" disabled>Selecione o responsável…</option>
					{#each membros as m (m.id)}
						<option value={m.id}>{m.nome}</option>
					{/each}
				</select>
				{#if membros.length === 0}
					<p class="mt-1 text-xs text-muted">Nenhum integrante disponível.</p>
				{/if}
			</label>

		</form>
	{/snippet}

	{#snippet footer()}
		<button type="button" onclick={onClose} class="rounded-md px-4 py-2 text-sm text-muted transition hover:text-ink">
			Cancelar
		</button>
		<button
			type="submit"
			form="modal-mesa-form"
			data-testid="mesa-save"
			disabled={enviando || nome.trim().length === 0 || membroId === '' || mesaNumero === ''}
			class="rounded-md bg-brand px-4 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi disabled:cursor-not-allowed disabled:opacity-50"
		>
			{enviando ? 'Criando…' : 'Criar mesa'}
		</button>
	{/snippet}
</Modal>