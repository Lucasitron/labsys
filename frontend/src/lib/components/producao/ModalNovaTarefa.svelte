<script lang="ts">
import { apiErrorMessage } from '$lib/utils/errors';
import { onMount } from 'svelte';
import type { NomeFunc, Prioridade, Projeto, Tarefa } from '$lib/types/producao';
	import { criarTarefa } from '$lib/api/producao/client';
		import Modal from '$lib/components/ui/Modal.svelte';

	interface Props {
		open: boolean;
		projetos: Projeto[];
		responsaveis: NomeFunc[];
		onClose: () => void;
		onCriada?: (tarefa: Tarefa) => void;
	}

	let { open, projetos, responsaveis, onClose, onCriada }: Props = $props();

	const prioridades: Prioridade[] = ['Baixa', 'Média', 'Alta'];

	let titulo = $state('');
	let projetoId = $state('');
	let responsavelId = $state('');
	let prioridade = $state<Prioridade>('Média');
	let inicio = $state('');
	let prazo = $state('');
	let descricao = $state('');
	let enviando = $state(false);
	let erro = $state('');

	$effect(() => {
		if (open) {
			titulo = '';
			projetoId = '';
			responsavelId = '';
			prioridade = 'Média';
			inicio = '';
			prazo = '';
			descricao = '';
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


	async function salvar(event: SubmitEvent): Promise<void> {
		event.preventDefault();
		enviando = true;
		erro = '';
		try {
			const tarefa = await criarTarefa({
				titulo: titulo.trim(),
				descricao: descricao.trim() || undefined,
				idProjeto: projetoId,
				idResponsavel: responsavelId || undefined,
				dataInicio: inicio || undefined,
				dataFimPrevista: prazo || undefined,
				prioridade
			});
			onCriada?.(tarefa);
		} catch (err) {
			erro = apiErrorMessage(err, 'Não foi possível criar a tarefa.');
		} finally {
			enviando = false;
		}
	}
	const inputCls =
		'w-full rounded-lg border border-border bg-elevated px-3 py-2.5 text-sm text-ink placeholder:text-muted/60 focus:border-brand focus:outline-none focus:ring-1 focus:ring-brand/40';
	const labelCls = 'mb-1.5 block text-xs font-medium text-muted';
	const textareaCls = `${inputCls} resize-none`;
</script>

<Modal {open} title="Nova tarefa" subtitle="Tarefa sempre vinculada a um projeto" {onClose} width="lg">
	{#snippet children()}
		<form id="modal-tarefa-form" onsubmit={salvar} novalidate>
			{#if erro}
				<div role="alert" class="mb-4 rounded-xl border border-danger/30 bg-danger/10 px-4 py-3 text-sm text-danger">
					{erro}
				</div>
			{/if}

			<label class="block">
				<span class={labelCls}>Título *</span>
				<input
					type="text"
					bind:value={titulo}
					required
					maxlength="120"
					placeholder="O que precisa ser feito…"
					class={inputCls}
				/>
			</label>

			<div class="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2">
				<label class="block">
					<span class={labelCls}>Projeto *</span>
					<select bind:value={projetoId} required class={inputCls}>
						<option value="" disabled>Selecione o projeto…</option>
						{#each projetos as p (p.id)}
							<option value={p.id}>{p.codigo} · {p.nome}</option>
						{/each}
					</select>
					{#if projetos.length === 0}
						<p class="mt-1 text-[11px] text-muted">Nenhum projeto disponível.</p>
					{/if}
				</label>

				<label class="block">
					<span class={labelCls}>Responsável</span>
					<select bind:value={responsavelId} class={inputCls}>
						<option value="" disabled>Selecione o responsável…</option>
						{#each responsaveis as r (r.id)}
							<option value={r.id}>{r.nome}</option>
						{/each}
					</select>
					{#if responsaveis.length === 0}
						<p class="mt-1 text-[11px] text-muted">Nenhum integrante disponível.</p>
					{/if}
				</label>
			</div>

			<fieldset class="mt-4">
				<legend class={labelCls}>Prioridade</legend>
				<div class="grid grid-cols-2 gap-2 sm:grid-cols-4">
					{#each prioridades as pr}
						<label
							class="inline-flex cursor-pointer items-center gap-1.5 rounded-lg border px-3 py-2 text-xs font-medium transition {prioridade === pr
								? 'border-brand bg-brand/10 text-brandhi'
								: 'border-border bg-elevated text-muted hover:border-brand/40'}"
						>
							<input
								type="radio"
								name="tar-prioridade"
								value={pr}
								checked={prioridade === pr}
								onchange={() => (prioridade = pr)}
								class="h-3.5 w-3.5 accent-brand"
							/>
							{pr}
						</label>
					{/each}
				</div>
			</fieldset>

			<div class="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2">
				<label class="block">
					<span class={labelCls}>Início</span>
					<input type="date" bind:value={inicio} class={inputCls} />
				</label>

				<label class="block">
					<span class={labelCls}>Prazo</span>
					<input type="date" bind:value={prazo} class={inputCls} />
				</label>
			</div>

			<label class="mt-4 block">
				<span class={labelCls}>Descrição</span>
				<textarea
					bind:value={descricao}
					rows="2"
					placeholder="Detalhes da tarefa…"
					class={textareaCls}
				></textarea>
			</label>
		</form>
	{/snippet}

	{#snippet footer()}
		<button type="button" onclick={onClose} class="rounded-md px-4 py-2 text-sm text-muted transition hover:text-ink">
			Cancelar
		</button>
		<button
			type="submit"
			form="modal-tarefa-form"
			data-testid="tar-nova"
			disabled={enviando || titulo.trim().length === 0 || projetoId === ''}
			class="rounded-md bg-brand px-4 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi disabled:cursor-not-allowed disabled:opacity-50"
		>
			{enviando ? 'Criando…' : 'Criar tarefa'}
		</button>
	{/snippet}
</Modal>