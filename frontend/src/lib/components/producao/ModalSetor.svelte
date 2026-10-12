<script lang="ts">
	import { errorMessage } from '$lib/utils/errors';
	import { onMount } from 'svelte';
	import type { NomeFunc, Setor5S } from '$lib/types/producao';
	import {
		adicionarResponsavelDoSetor,
		atualizarSetor5S,
		criarSetor5S
	} from '$lib/api/producao/client';
		import Modal from '$lib/components/ui/Modal.svelte';

	interface Props {
		open: boolean;
		setor?: Setor5S | null;
		pessoas: NomeFunc[];
		isAdmin: boolean;
		canEdit: boolean;
		onClose: () => void;
		onSalvo?: (setor: Setor5S, proposta: boolean) => void;
	}

	let { open, setor = null, pessoas, isAdmin, canEdit, onClose, onSalvo }: Props = $props();

	let nome = $state('');
	let numero = $state('');
	let descricao = $state('');
	let responsavelId = $state('');
	let dataInicioResp = $state(new Date().toISOString().slice(0, 10));
	let enviando = $state(false);
	let erro = $state('');

	const editando = $derived(setor !== null);

	$effect(() => {
		if (open) {
			nome = setor?.nome ?? '';
			numero = '';
			descricao = '';
			responsavelId = '';
			dataInicioResp = new Date().toISOString().slice(0, 10);
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

	const titulo = $derived(editando ? `Editar setor 5S · ${setor?.nome ?? ''}` : 'Novo setor 5S');


	async function salvar(event: SubmitEvent): Promise<void> {
		event.preventDefault();
		enviando = true;
		erro = '';
		try {
			if (!canEdit) return;
			const num = Number(numero);
			if (!Number.isInteger(num)) {
				erro = 'Informe o número do setor.';
				return;
			}
			const payload = {
				numero: num,
				nome: nome.trim(),
				descricao: descricao.trim() || undefined
			};
			let atualizado: Setor5S;
			if (isAdmin) {
				atualizado = editando
					? await atualizarSetor5S(setor!.id, payload)
					: await criarSetor5S(payload);
				if (responsavelId) {
					await adicionarResponsavelDoSetor(atualizado.id, {
						idFuncionario: responsavelId,
						dataInicio: dataInicioResp
					});
				}
				onSalvo?.(atualizado, false);
			} else if (editando) {
				erro = 'Aprovação de alteração indisponível — sem par no backend (Lote 3).';
				return;
			} else {
				atualizado = await criarSetor5S(payload);
				onSalvo?.(atualizado, true);
			}
		} catch (err) {
			erro = errorMessage(err, 'Não foi possível salvar o setor.');
		} finally {
			enviando = false;
		}
	}

	const labelCta = $derived(
		editando ? (isAdmin ? 'Salvar alterações' : 'Enviar p/ aprovação') : isAdmin ? 'Criar setor' : 'Enviar p/ aprovação'
	);
	const testIdCta = $derived(editando ? 'set-salvar' : 'set-criar');

	const inputCls =
		'w-full rounded-lg border border-border bg-elevated px-3 py-2.5 text-sm text-ink placeholder:text-muted/60 focus:border-brand focus:outline-none focus:ring-1 focus:ring-brand/40';
</script>

<Modal {open} title={titulo} {onClose} width="md">
	{#snippet children()}
		<form id="modal-setor-form" onsubmit={salvar} novalidate>
			{#if erro}
				<div role="alert" class="mb-4 rounded-xl border border-danger/30 bg-danger/10 px-4 py-3 text-sm text-danger">
					{erro}
				</div>
			{/if}

			<div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
				<label class="block">
					<span class="mb-1.5 block text-xs font-medium text-muted">Número do setor *</span>
					<input
						type="number"
						bind:value={numero}
						required
						min="1"
						step="1"
						placeholder="ex.: 12"
						class={inputCls}
					/>
				</label>

				<label class="block">
					<span class="mb-1.5 block text-xs font-medium text-muted">Nome do setor *</span>
					<input
						type="text"
						bind:value={nome}
						required
						maxlength="80"
						placeholder="ex.: Bancada de eletrônica"
						class={inputCls}
					/>
				</label>
			</div>

			<label class="mt-4 block">
				<span class="mb-1.5 block text-xs font-medium text-muted">Descrição</span>
				<textarea
					bind:value={descricao}
					rows="2"
					placeholder="Detalhes do setor…"
					class={`${inputCls} resize-none`}
				></textarea>
			</label>

			{#if isAdmin}
				<div class="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2">
					<label class="block">
						<span class="mb-1.5 block text-xs font-medium text-muted">Responsável (designação)</span>
						<select bind:value={responsavelId} class={inputCls}>
							<option value="">Sem designação…</option>
							{#each pessoas as p (p.id)}
								<option value={p.id}>{p.nome}</option>
							{/each}
						</select>
					</label>

					<label class="block">
						<span class="mb-1.5 block text-xs font-medium text-muted">Início da responsabilidade</span>
						<input type="date" bind:value={dataInicioResp} class={inputCls} />
					</label>
				</div>
			{/if}
		</form>
	{/snippet}

	{#snippet footer()}
		<button type="button" onclick={onClose} class="rounded-md px-4 py-2 text-sm text-muted transition hover:text-ink">
			Cancelar
		</button>
		{#if canEdit}
			<button
				type="submit"
				form="modal-setor-form"
				data-testid={testIdCta}
				disabled={enviando || nome.trim().length === 0 || numero === ''}
				class="rounded-md bg-brand px-4 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi disabled:cursor-not-allowed disabled:opacity-50"
			>
				{enviando ? 'Enviando…' : labelCta}
			</button>
		{/if}
	{/snippet}
</Modal>
