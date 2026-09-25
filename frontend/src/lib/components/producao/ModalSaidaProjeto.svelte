<script lang="ts">
	import { onMount } from 'svelte';
	import type { MaterialProjeto, Projeto, SaidaMaterial } from '$lib/types/producao';
	import { registrarSaidaMaterial } from '$lib/api/producao/client';
	import { ApiError, NetworkError } from '$lib/api/client';
	import Modal from '$lib/components/ui/Modal.svelte';

	interface Props {
		open: boolean;
		projeto: Pick<Projeto, 'id' | 'codigo' | 'nome'>;
		materiais: MaterialProjeto[];
		onClose: () => void;
		onRegistrado?: (saida: SaidaMaterial) => void;
	}

	let { open, projeto, materiais, onClose, onRegistrado }: Props = $props();

	let codigoSelecionado = $state('');
	let quantidade = $state(1);
	let enviando = $state(false);
	let erro = $state('');

	$effect(() => {
		if (open) {
			codigoSelecionado = materiais[0]?.codigo ?? '';
			quantidade = 1;
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

	const material = $derived(materiais.find((m) => m.codigo === codigoSelecionado) ?? null);
	const acimaDisponivel = $derived(material !== null && quantidade > material.disponivel);

	function mensagemErro(err: unknown): string {
		if (err instanceof ApiError || err instanceof NetworkError) return err.message;
		return 'Não foi possível registrar a saída.';
	}

	async function salvar(event: SubmitEvent): Promise<void> {
		event.preventDefault();
		if (!material || acimaDisponivel) return;
		enviando = true;
		erro = '';
		try {
			const saida = await registrarSaidaMaterial(projeto.id, {
				item: material.item,
				codigo: material.codigo,
				quantidade
			});
			onRegistrado?.(saida);
		} catch (err) {
			erro = mensagemErro(err);
		} finally {
			enviando = false;
		}
	}

	const inputCls =
		'w-full rounded-lg border border-border bg-elevated px-3 py-2.5 text-sm text-ink placeholder:text-muted/60 focus:border-brand focus:outline-none focus:ring-1 focus:ring-brand/40 disabled:cursor-not-allowed disabled:opacity-50';
	const labelCls = 'mb-1.5 block text-xs font-medium text-muted';
</script>

<Modal
	{open}
	title={`Registrar saída · ${projeto.codigo}`}
	subtitle="A4 🟡 — baixa automática no Estoque (via gateway)"
	{onClose}
	width="md"
>
	{#snippet children()}
		<form id="modal-saida-projeto-form" onsubmit={salvar} novalidate>
			{#if erro}
				<div role="alert" class="mb-4 rounded-xl border border-danger/30 bg-danger/10 px-4 py-3 text-sm text-danger">
					Não foi possível registrar a saída. {erro}
				</div>
			{/if}

			<label class="block">
				<span class={labelCls}>Item (do BOM do projeto) *</span>
				<select bind:value={codigoSelecionado} required class={inputCls}>
					<option value="" disabled>Selecione o material…</option>
					{#each materiais as m (m.codigo)}
						<option value={m.codigo}>
							{m.item} ({m.codigo}) · disponível {m.disponivel}
						</option>
					{/each}
				</select>
				{#if materiais.length === 0}
					<p class="mt-1 text-[11px] text-muted">Nenhum material no BOM deste projeto.</p>
				{/if}
			</label>

			<div class="mt-4 grid grid-cols-2 gap-4">
				<label class="block">
					<span class={labelCls}>Quantidade *</span>
					<input
						type="number"
						bind:value={quantidade}
						min="1"
						max={material?.disponivel ?? 1}
						required
						class={inputCls}
					/>
					{#if material}
						<p class="mt-1 text-[11px] {acimaDisponivel ? 'text-danger' : 'text-muted'}">
							Disponível: <span class="font-semibold tabular-nums">{material.disponivel}</span>
							{#if acimaDisponivel} — quantidade acima do disponível!{/if}
						</p>
					{/if}
				</label>

				<label class="block">
					<span class={labelCls}>Destino</span>
					<input type="text" value={`${projeto.codigo} · ${projeto.nome}`} disabled class={inputCls} />
					<p class="mt-1 text-[11px] text-muted">Saída vinculada ao projeto corrente.</p>
				</label>
			</div>

			<div class="mt-4 rounded-lg border border-warn/30 bg-warn/10 p-3 text-xs text-muted">
				A saída dá <span class="font-medium text-ink">baixa automática no Estoque</span> (via gateway) e fica
				vinculada a <span class="font-medium text-ink">{projeto.codigo}</span>. Novo endpoint — contrato
				pendente; se a API falhar, o erro é exibido aqui.
			</div>
		</form>
	{/snippet}

	{#snippet footer()}
		<button type="button" onclick={onClose} class="rounded-md px-4 py-2 text-sm text-muted transition hover:text-ink">
			Cancelar
		</button>
		<button
			type="submit"
			form="modal-saida-projeto-form"
			data-testid="proj-saida-material"
			disabled={enviando || material === null || acimaDisponivel || quantidade <= 0}
			class="rounded-md bg-brand px-4 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi disabled:cursor-not-allowed disabled:opacity-50"
		>
			{enviando ? 'Registrando…' : 'Registrar saída'}
		</button>
	{/snippet}
</Modal>