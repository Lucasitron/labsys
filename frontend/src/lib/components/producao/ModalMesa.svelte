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

	let { open, projetos, membros, onClose, onCriada }: Props = $props();

	let nome = $state('');
	let projetoId = $state('');
	let membroId = $state('');
	let periodoExperimental = $state(false);
	let enviando = $state(false);
	let erro = $state('');

	$effect(() => {
		if (open) {
			erro = '';
			projetoId = '';
			membroId = '';
			periodoExperimental = false;
		}
	});

	onMount(() => {
		const onKey = (e: KeyboardEvent): void => {
			if (open && e.key === 'Escape') onClose();
		};
		document.addEventListener('keydown', onKey);
		return () => document.removeEventListener('keydown', onKey);
	});

	const qrTotem = $derived(`LAB://TOTEM/${(nome.trim() || 'mesa').toLowerCase().replace(/[^a-z0-9]+/g, '-')}`);

	function mensagemErro(err: unknown): string {
		if (err instanceof ApiError || err instanceof NetworkError) return err.message;
		return 'Não foi possível criar a mesa.';
	}

	async function salvar(event: SubmitEvent): Promise<void> {
		event.preventDefault();
		enviando = true;
		erro = '';
		try {
			const mesa = await criarMesa({
				nome: nome.trim(),
				projetoId: projetoId || undefined,
				membroId,
				periodoExperimental
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
				<span class="mb-1.5 block text-xs font-medium text-muted">Nome da mesa *</span>
				<input
					type="text"
					bind:value={nome}
					required
					maxlength="50"
					placeholder="ex.: Mesa 04"
					class={inputCls}
				/>
			</label>

			<label class="mt-4 block">
				<span class="mb-1.5 block text-xs font-medium text-muted">Projeto universitário *</span>
				<select bind:value={projetoId} required class={inputCls}>
					<option value="" disabled>Selecione o projeto…</option>
					{#each projetos as projeto (projeto.id)}
						<option value={projeto.id}>{projeto.codigo} · {projeto.nome}</option>
					{/each}
				</select>
				{#if projetos.length === 0}
					<p class="mt-1 text-xs text-muted">Nenhum projeto disponível para vincular.</p>
				{/if}
			</label>

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

			<label class="mt-4 inline-flex cursor-pointer items-center gap-2 text-sm text-ink">
				<input
					type="checkbox"
					bind:checked={periodoExperimental}
					class="h-4 w-4 rounded border-border accent-brand"
				/>
				Período experimental
			</label>
			<p class="mt-1 text-[11px] text-muted">
				No período experimental a mesa entra na rotina de auditoria do 5S e recebe o QR no totem.
			</p>

			<div class="mt-4 rounded-xl border border-border bg-elevated/40 p-3">
				<p class="mb-1.5 text-xs font-medium text-muted">QR Totem</p>
				<div class="flex items-center gap-3">
					<div class="flex h-16 w-16 shrink-0 items-center justify-center rounded-lg border border-dashed border-brand/40 bg-brand/5 font-mono text-[9px] text-brandhi">
						QR
					</div>
					<div class="min-w-0">
						<p class="truncate font-mono text-[11px] text-ink">{qrTotem}</p>
						<p class="mt-0.5 text-[11px] text-muted">QR gerado como texto placeholder (sem biblioteca externa).</p>
					</div>
				</div>
			</div>
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
			disabled={enviando || nome.trim().length === 0 || membroId === ''}
			class="rounded-md bg-brand px-4 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi disabled:cursor-not-allowed disabled:opacity-50"
		>
			{enviando ? 'Criando…' : 'Criar mesa'}
		</button>
	{/snippet}
</Modal>