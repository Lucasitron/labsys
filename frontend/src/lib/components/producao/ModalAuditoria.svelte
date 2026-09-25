<script lang="ts">
	import { onMount } from 'svelte';
	import type { Inspecao5S } from '$lib/types/producao';
	import { notaToCartao, CARTAO_META } from '$lib/utils/producao-status';
	import { concluirInspecao5S } from '$lib/api/producao/client';
	import { ApiError, NetworkError } from '$lib/api/client';
	import Modal from '$lib/components/ui/Modal.svelte';

	interface Props {
		open: boolean;
		inspecao: Inspecao5S;
		itens: string[];
		onClose: () => void;
		onConcluida?: (atualizada: Inspecao5S) => void;
	}

	let { open, inspecao, itens, onClose, onConcluida }: Props = $props();

	let selecionados = $state<boolean[]>([]);
	let enviando = $state(false);
	let erro = $state('');

	$effect(() => {
		if (open) {
			const base =
				inspecao.itens?.length === itens.length
					? inspecao.itens.map((i) => i.conforme)
					: itens.map(() => true);
			selecionados = base;
			erro = '';
		}
	});

	onMount(() => {
		const onKey = (e: KeyboardEvent): void => {
			if (open && e.key === 'Escape') onClose();
		};
		document.addEventListener('keydown', onKey);
		return () => document.removeEventListener('keydown', onKey);
	});

	const conformes = $derived(selecionados.filter(Boolean).length);
	const total = $derived(itens.length);
	const nota = $derived(selecionados.length > 0 ? Math.round((conformes / total) * 100) : 0);
	const cartao = $derived(notaToCartao(nota));
	const cartaoMeta = $derived(CARTAO_META[cartao]);

	function marcar(idx: number, conforme: boolean): void {
		selecionados = selecionados.map((v, i) => (i === idx ? conforme : v));
	}

	function mensagemErro(err: unknown): string {
		if (err instanceof ApiError || err instanceof NetworkError) return err.message;
		return 'Não foi possível concluir a auditoria.';
	}

	async function concluir(event: SubmitEvent): Promise<void> {
		event.preventDefault();
		enviando = true;
		erro = '';
		try {
			const atualizada = await concluirInspecao5S(inspecao.id, {
				itens: selecionados.map((conforme, idx) => ({ s: idx + 1, conforme }))
			});
			onConcluida?.(atualizada);
		} catch (err) {
			erro = mensagemErro(err);
		} finally {
			enviando = false;
		}
	}

	const cartaoTones: Record<string, string> = {
		success: 'text-success border-success/30 bg-success/10',
		warn: 'text-warn border-warn/30 bg-warn/10',
		danger: 'text-danger border-danger/30 bg-danger/10'
	};

	
</script>

<Modal {open} title={`Auditoria — ${inspecao.setor.nome}`} subtitle={inspecao.status} {onClose} width="lg">
	{#snippet children()}
		<form id="modal-auditoria-form" onsubmit={concluir} novalidate>
			{#if erro}
				<div role="alert" class="mb-4 rounded-xl border border-danger/30 bg-danger/10 px-4 py-3 text-sm text-danger">
					{erro}
				</div>
			{/if}

			{#if itens.length === 0}
				<div class="rounded-xl border border-border bg-elevated/40 p-4 text-sm text-muted">
					Nenhum item de checklist disponível para esta auditoria.
				</div>
			{:else}
				<div class="space-y-2">
					{#each itens as item, idx (idx)}
						<div class="rounded-lg border border-border bg-elevated/50 p-3">
							<p class="text-sm font-medium text-ink">{item}</p>
							<div class="mt-2 flex flex-wrap gap-4">
								<label class="inline-flex cursor-pointer items-center gap-2 text-xs text-muted">
									<input
										type="radio"
										name={`aud-item-${idx}`}
										checked={selecionados[idx]}
										onchange={() => marcar(idx, true)}
										class="h-4 w-4 rounded-full border-border accent-brand focus:ring-brand/40"
									/>
									Conforme
								</label>
								<label class="inline-flex cursor-pointer items-center gap-2 text-xs text-muted">
									<input
										type="radio"
										name={`aud-item-${idx}`}
										checked={!selecionados[idx]}
										onchange={() => marcar(idx, false)}
										class="h-4 w-4 rounded-full border-border accent-brand focus:ring-brand/40"
									/>
									Não conforme
								</label>
							</div>
						</div>
					{/each}
				</div>

				<div class="mt-4 rounded-xl border border-warn/30 bg-warn/10 p-3">
					<div class="flex flex-wrap items-center gap-3">
						<div class="text-xs text-muted">
							Prévia automática
							<span class="ml-1 text-sm font-semibold text-ink tabular-nums">
								{conformes}/{total} · {nota}%
							</span>
						</div>
						<span
							class="ml-auto inline-flex items-center gap-1.5 rounded-full border px-2.5 py-0.5 text-xs font-semibold {cartaoTones[cartaoMeta.color]}"
						>
							Cartão {cartao}
						</span>
					</div>
					<p class="mt-2 text-[11px] text-muted">
						Faixas: Verde ≥90 · Vermelho &lt;70 · senão Amarelo. A nota é calculada automaticamente
						(itens marcados ÷ total) e o backend recalcula na conclusão.
					</p>
				</div>
			{/if}
		</form>
	{/snippet}

	{#snippet footer()}
		<button type="button" onclick={onClose} class="rounded-md px-4 py-2 text-sm text-muted transition hover:text-ink">
			Cancelar
		</button>
		<button
			type="submit"
			form="modal-auditoria-form"
			data-testid="aud-concluir"
			disabled={enviando || itens.length === 0}
			class="rounded-md bg-brand px-4 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi disabled:cursor-not-allowed disabled:opacity-50"
		>
			{enviando ? 'Concluindo…' : 'Concluir auditoria'}
		</button>
	{/snippet}
</Modal>