<script lang="ts">
	import { onMount } from 'svelte';
	import type { NomeFunc, Projeto, ProjetoStatus } from '$lib/types/producao';
	import { atualizarProjeto, criarProjeto } from '$lib/api/producao/client';
	import { ApiError, NetworkError } from '$lib/api/client';
	import Modal from '$lib/components/ui/Modal.svelte';

	interface Props {
		open: boolean;
		projeto?: Projeto | null;
		responsaveis: NomeFunc[];
		onClose: () => void;
		onSalvo?: (projeto: Projeto) => void;
	}

	let { open, projeto = null, responsaveis, onClose, onSalvo }: Props = $props();

	const statuses: ProjetoStatus[] = ['Planejado', 'Em andamento', 'Concluído', 'Cancelado'];

	let nome = $state('');
	let cliente = $state('');
	let responsavelId = $state('');
	let prazo = $state('');
	let descricao = $state('');
	let status = $state<ProjetoStatus>('Planejado');
	let enviando = $state(false);
	let erro = $state('');

	const editando = $derived(projeto !== null);

	$effect(() => {
		if (open) {
			nome = projeto?.nome ?? '';
			cliente = projeto?.cliente ?? '';
			responsavelId = projeto?.responsavel?.id ?? '';
			prazo = projeto?.prazo ?? '';
			descricao = projeto?.descricao ?? '';
			status = projeto?.status ?? 'Planejado';
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

	const titulo = $derived(editando ? `Editar projeto · ${projeto?.codigo ?? ''}` : 'Novo projeto');

	function mensagemErro(err: unknown): string {
		if (err instanceof ApiError || err instanceof NetworkError) return err.message;
		return 'Não foi possível salvar o projeto.';
	}

	async function salvar(event: SubmitEvent): Promise<void> {
		event.preventDefault();
		enviando = true;
		erro = '';
		try {
			const payload = {
				nome: nome.trim(),
				cliente: cliente.trim() || undefined,
				descricao: descricao.trim() || undefined,
				responsavelId,
				prazo,
				status
			};
			const salvo = editando
				? await atualizarProjeto(projeto!.id, payload)
				: await criarProjeto({ ...payload, status: 'Planejado' });
			onSalvo?.(salvo);
		} catch (err) {
			erro = mensagemErro(err);
		} finally {
			enviando = false;
		}
	}

	const inputCls =
		'w-full rounded-lg border border-border bg-elevated px-3 py-2.5 text-sm text-ink placeholder:text-muted/60 focus:border-brand focus:outline-none focus:ring-1 focus:ring-brand/40';
	const labelCls = 'mb-1.5 block text-xs font-medium text-muted';
	const textareaCls = `${inputCls} resize-none`;
</script>

<Modal {open} title={titulo} subtitle={editando ? 'Preenchido com os dados atuais (modo editar)' : ''} {onClose} width="md">
	{#snippet children()}
		<form id="modal-projeto-form" onsubmit={salvar} novalidate>
			{#if erro}
				<div role="alert" class="mb-4 rounded-xl border border-danger/30 bg-danger/10 px-4 py-3 text-sm text-danger">
					{erro}
				</div>
			{/if}

			<label class="block">
				<span class={labelCls}>Nome do projeto *</span>
				<input
					type="text"
					bind:value={nome}
					required
					maxlength="120"
					placeholder="ex.: Rack de ventilação p/ sala de impressão"
					class={inputCls}
				/>
			</label>

			<div class="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2">
				<label class="block">
					<span class={labelCls}>Cliente</span>
					<input
						type="text"
						bind:value={cliente}
						maxlength="120"
						placeholder="Escola, empresa, interno…"
						class={inputCls}
					/>
				</label>

				<label class="block">
					<span class={labelCls}>Responsável *</span>
					<select bind:value={responsavelId} required class={inputCls}>
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

			<div class="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2">
				<label class="block">
					<span class={labelCls}>Fim previsto *</span>
					<input type="date" bind:value={prazo} required class={inputCls} />
				</label>

				{#if editando}
					<label class="block">
						<span class={labelCls}>Status</span>
						<select bind:value={status} class={inputCls}>
							{#each statuses as s}
								<option value={s}>{s}</option>
							{/each}
						</select>
					</label>
				{/if}
			</div>

			<label class="mt-4 block">
				<span class={labelCls}>Descrição</span>
				<textarea
					bind:value={descricao}
					rows="3"
					placeholder="Objetivo e escopo do projeto…"
					class={textareaCls}
				></textarea>
			</label>

			<div class="mt-4 rounded-lg border border-border bg-elevated/30 p-3 text-xs text-muted">
				{#if editando}
					Salvar aplica <span class="text-ink">PUT /producao/projetos/{'{id}'}</span> com o status atual.
				{:else}
					Novo projeto é criado com status <span class="font-medium text-ink">Planejado</span>.
				{/if}
			</div>
		</form>
	{/snippet}

	{#snippet footer()}
		<button type="button" onclick={onClose} class="rounded-md px-4 py-2 text-sm text-muted transition hover:text-ink">
			Cancelar
		</button>
		<button
			type="submit"
			form="modal-projeto-form"
			data-testid={editando ? 'proj-save' : 'proj-novo'}
			disabled={enviando || nome.trim().length === 0 || responsavelId === '' || prazo === ''}
			class="rounded-md bg-brand px-4 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi disabled:cursor-not-allowed disabled:opacity-50"
		>
			{enviando ? 'Salvando…' : editando ? 'Salvar alterações' : 'Criar projeto'}
		</button>
	{/snippet}
</Modal>