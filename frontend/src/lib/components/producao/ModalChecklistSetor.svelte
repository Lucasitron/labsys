<script lang="ts">
	import { onMount } from 'svelte';
	import type { Setor5S } from '$lib/types/producao';
	import {
		aprovarAlteracaoDoSetor,
		atualizarChecklistDoSetor
	} from '$lib/api/producao/client';
	import { ApiError, NetworkError } from '$lib/api/client';
	import Modal from '$lib/components/ui/Modal.svelte';

	interface Props {
		open: boolean;
		setor: Setor5S;
		itens: string[];
		nota?: number | null;
		isAdmin: boolean;
		canEdit: boolean;
		onClose: () => void;
		onSalvo?: (setor: Setor5S, proposta: boolean) => void;
	}

	let { open, setor, itens, nota = null, isAdmin, canEdit, onClose, onSalvo }: Props = $props();

	const PADRAO_5S = [
		'Seiri — separar o útil do desnecessário',
		'Seiton — arrumar e organizar',
		'Seiso — limpar e inspecionar',
		'Seiketsu — padronizar',
		'Shitsuke — disciplina'
	];

	let itensLocal = $state<string[]>([]);
	let enviando = $state(false);
	let erro = $state('');

	$effect(() => {
		if (open) {
			itensLocal = [...itens];
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

	function adicionarItem(): void {
		itensLocal = [...itensLocal, ''];
	}

	function removerItem(idx: number): void {
		itensLocal = itensLocal.filter((_, i) => i !== idx);
	}

	const padroesCount = $derived(PADRAO_5S.length + itensLocal.length);

	function mensagemErro(err: unknown): string {
		if (err instanceof ApiError || err instanceof NetworkError) return err.message;
		return 'Não foi possível salvar o checklist.';
	}

	async function salvar(event: SubmitEvent): Promise<void> {
		event.preventDefault();
		const limpos = itensLocal.map((i) => i.trim()).filter((i) => i.length > 0);
		if (limpos.length === 0) {
			erro = 'Adicione ao menos um item específico.';
			return;
		}
		enviando = true;
		erro = '';
		try {
			let atualizado: Setor5S;
			if (isAdmin) {
				atualizado = await atualizarChecklistDoSetor(setor.id, { itens: limpos });
				onSalvo?.(atualizado, false);
			} else {
				atualizado = await aprovarAlteracaoDoSetor(setor.id, { aprovado: false });
				onSalvo?.(atualizado, true);
			}
		} catch (err) {
			erro = mensagemErro(err);
		} finally {
			enviando = false;
		}
	}

	const labelCta = $derived(isAdmin ? 'Salvar alterações' : 'Enviar p/ aprovação');
	const testIdCta = $derived(isAdmin ? 'set-checklist-save' : 'set-checklist-enviar');
</script>

<Modal
	{open}
	title={`Checklist do setor · ${setor.nome}`}
	subtitle="Os 5 S são obrigatórios e travados; itens específicos são editáveis (1 item = 1 ponto)"
	{onClose}
	width="lg"
>
	{#snippet children()}
		<form id="modal-checklist-form" onsubmit={salvar} novalidate>
			{#if erro}
				<div role="alert" class="mb-4 rounded-xl border border-danger/30 bg-danger/10 px-4 py-3 text-sm text-danger">
					{erro}
				</div>
			{/if}

			<div class="mb-4 flex items-center justify-between gap-2 rounded-lg border border-border bg-elevated/30 p-3">
				<span class="text-[11px] text-muted">Salvando como</span>
				<span class="inline-flex items-center gap-1.5 text-xs font-medium text-ink">
					<span class="h-1.5 w-1.5 rounded-full {isAdmin ? 'bg-brand' : 'bg-warn'}" aria-hidden="true"></span>
					{isAdmin ? 'Admin (salva direto)' : 'Responsável / Auditor (envia p/ aprovação)'}
				</span>
			</div>

			<div class="space-y-2">
				{#each PADRAO_5S as item}
					<label class="flex cursor-pointer items-center gap-3 rounded-lg border border-border bg-elevated/50 p-3 text-sm text-ink">
						<input type="checkbox" checked disabled class="h-4 w-4 rounded border-border accent-brand" />
						{item}
					</label>
				{/each}
			</div>

			<div class="mt-4 overflow-hidden rounded-xl border border-border">
				<div class="border-b border-border bg-elevated/40 px-4 py-2.5 text-xs font-medium text-muted">
					Itens específicos do setor · cada item vale 1 ponto na nota
				</div>
				{#if itensLocal.length === 0}
					<div class="px-4 py-6 text-center text-sm text-muted">Nenhum item específico cadastrado.</div>
				{:else}
					<div class="divide-y divide-border">
						{#each itensLocal as item, idx (idx)}
							<div class="flex items-center gap-2 px-4 py-2.5">
								<span class="h-3 w-3 shrink-0 rounded border border-brand/40 bg-brand/5" aria-hidden="true"></span>
								<input
									type="text"
									bind:value={itensLocal[idx]}
									placeholder="Descrição do item…"
									aria-label={`Item específico ${idx + 1}`}
									class="flex-1 bg-transparent text-sm text-ink outline-none placeholder:text-muted/60 focus:border-brand"
								/>
								<button
									type="button"
									onclick={() => removerItem(idx)}
									class="rounded p-1 text-muted transition hover:bg-elevated hover:text-danger"
									aria-label={`Remover item ${idx + 1}`}
								>
									×
								</button>
							</div>
						{/each}
					</div>
				{/if}
				<div class="border-t border-border px-4 py-2.5">
					<button
						type="button"
						onclick={adicionarItem}
						class="inline-flex items-center gap-1.5 text-xs font-medium text-brand transition hover:text-brandhi"
					>
						<span class="inline-flex h-3.5 w-3.5 items-center justify-center rounded-full border border-current text-[11px] leading-none" aria-hidden="true">+</span>
						Adicionar item
					</button>
				</div>
			</div>

			{#if nota !== null && nota !== undefined}
				<div class="mt-4 flex flex-wrap items-center gap-2 rounded-lg border border-border bg-elevated/30 p-3 text-xs text-muted">
					<span>Nota atual do setor:</span>
					<span class="text-sm font-semibold text-ink tabular-nums">{nota}</span>
					<span class="text-[11px]">(o checklist travado tem {padroesCount} itens no total)</span>
				</div>
			{/if}

			<div class="mt-4 rounded-lg border border-border bg-elevated/30 p-3 text-xs text-muted">
				Sem reordenação e sem peso — cada item vale 1 ponto.{' '}
				{#if !isAdmin}
					Alterações propostas seguem para aprovação do Admin; como Admin, o salvamento é direto.
				{/if}
			</div>
		</form>
	{/snippet}

	{#snippet footer()}
		<button type="button" onclick={onClose} class="rounded-md px-4 py-2 text-sm text-muted transition hover:text-ink">
			Cancelar
		</button>
		{#if canEdit}
			<button
				type="submit"
				form="modal-checklist-form"
				data-testid={testIdCta}
				disabled={enviando}
				class="rounded-md bg-brand px-4 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi disabled:cursor-not-allowed disabled:opacity-50"
			>
				{enviando ? 'Enviando…' : labelCta}
			</button>
		{/if}
	{/snippet}
</Modal>