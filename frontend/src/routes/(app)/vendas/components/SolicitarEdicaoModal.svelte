<script lang="ts">
	import Modal from '$lib/components/ui/Modal.svelte';
	import Select from '$lib/components/ui/Select.svelte';
	import { createSolicitacao } from '$lib/api/vendas/solicitacoes';
	import { toUserMessage } from '$lib/utils/errors';
	import { toasts } from '$lib/stores/toast';
	import type { SolicitacaoAlvo, TipoSolicitacao } from '$lib/types/vendas';

	interface Props {
		alvo: SolicitacaoAlvo;
		campo?: string;
		valorAtual?: string;
		onClose: () => void;
		onSent?: () => void;
	}

	let { alvo, campo = '', valorAtual = '', onClose, onSent }: Props = $props();

	const TIPO_OPCOES: TipoSolicitacao[] = [
		'Alteração de dados',
		'Mudança de status',
		'Mover encomenda',
		'Outra'
	];

	let tipo = $state<TipoSolicitacao>('Alteração de dados');
	let campoAtual = $state(campo);
	$effect(() => {
		campoAtual = campo;
	});
	let valorProposto = $state('');
	let justificativa = $state('');
	let ocupado = $state(false);
	let erros = $state<Record<string, string>>({});
	let erroTopo = $state<string | null>(null);

	const ALVO_ROTULO: Record<SolicitacaoAlvo['tipo'], string> = {
		CLI: 'Cliente',
		OC: 'Orçamento',
		EN: 'Encomenda'
	};

	const inputCls =
		'w-full rounded-md border border-border bg-elevated px-3 py-2.5 text-sm text-ink placeholder:text-muted/60 focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/30 transition';
	const inputErroCls = 'border-danger/60 focus:border-danger focus:ring-danger/30';
	const labelCls = 'mb-1 block text-xs font-medium text-muted';

	function validar(): boolean {
		const novos: Record<string, string> = {};
		if (!valorProposto.trim()) novos['valorProposto'] = 'Informe o valor proposto.';
		if (!justificativa.trim()) novos['justificativa'] = 'Informe a justificativa.';
		else if (justificativa.trim().length < 10)
			novos['justificativa'] = 'A justificativa deve ter ao menos 10 caracteres.';
		erros = novos;
		return Object.keys(novos).length === 0;
	}

	async function enviar(): Promise<void> {
		if (ocupado) return;
		erroTopo = null;
		if (!validar()) {
			erroTopo = 'Verifique os campos destacados e tente novamente.';
			return;
		}
		ocupado = true;
		try {
			await createSolicitacao({
				tipo,
				alvo,
				...(campoAtual.trim() ? { campo: campoAtual.trim() } : {}),
				...(valorAtual ? { valorAtual } : {}),
				valorProposto: valorProposto.trim(),
				justificativa: justificativa.trim()
			});
			toasts.success('Solicitação enviada para análise.');
			onSent?.();
			onClose();
		} catch (err) {
			erroTopo = toUserMessage(err).message;
		} finally {
			ocupado = false;
		}
	}
</script>

<div data-testid="modal-solicitar-edicao">
	<Modal open title="Sugerir alteração" subtitle={alvo.nome} {onClose} width="md">
		{#snippet children()}
			<div class="space-y-4">
				{#if erroTopo}
					<p role="alert" class="rounded-md border border-danger/30 bg-danger/10 px-3 py-2 text-sm text-danger">
						{erroTopo}
					</p>
				{/if}

				<p class="rounded-md border border-border bg-elevated/50 px-3 py-2 text-xs text-muted">
					{ALVO_ROTULO[alvo.tipo]} · {alvo.nome}
				</p>

				<Select
					id="solic-tipo"
					label="Tipo de alteração"
					options={TIPO_OPCOES.map((t) => ({ id: t, label: t }))}
					value={tipo}
					onChange={(v) => (tipo = v as TipoSolicitacao)}
					required
				/>

				<div>
					<label for="solic-campo" class={labelCls}>Campo</label>
					<input
						id="solic-campo"
						type="text"
						bind:value={campoAtual}
						placeholder="Ex.: telefone, endereço, data de entrega"
						disabled={ocupado}
						class={inputCls}
					/>
				</div>

				{#if valorAtual}
					<div>
						<label for="solic-atual" class={labelCls}>Valor atual</label>
						<input
							id="solic-atual"
							type="text"
							value={valorAtual}
							readonly
							aria-readonly="true"
							class="{inputCls} opacity-70"
						/>
					</div>
				{/if}

				<div>
					<label for="solic-proposto" class={labelCls}>
						Valor proposto <span class="text-danger">*</span>
					</label>
					<input
						id="solic-proposto"
						type="text"
						bind:value={valorProposto}
						placeholder="Informe o valor desejado"
						disabled={ocupado}
						aria-invalid={erros['valorProposto'] ? 'true' : undefined}
						class="{inputCls} {erros['valorProposto'] ? inputErroCls : ''}"
					/>
					{#if erros['valorProposto']}
						<p role="alert" class="mt-1 text-xs text-danger">{erros['valorProposto']}</p>
					{/if}
				</div>

				<div>
					<label for="solic-justificativa" class={labelCls}>
						Justificativa <span class="text-danger">*</span>
					</label>
					<textarea
						id="solic-justificativa"
						bind:value={justificativa}
						rows={3}
						placeholder="Explique o motivo da alteração solicitada"
						disabled={ocupado}
						aria-invalid={erros['justificativa'] ? 'true' : undefined}
						class="{inputCls} resize-y {erros['justificativa'] ? inputErroCls : ''}"
					></textarea>
					{#if erros['justificativa']}
						<p role="alert" class="mt-1 text-xs text-danger">{erros['justificativa']}</p>
					{/if}
				</div>
			</div>
		{/snippet}
		{#snippet footer()}
			<button
				type="button"
				onclick={onClose}
				disabled={ocupado}
				class="rounded-md border border-border bg-surface px-4 py-2 text-sm font-medium text-ink transition hover:bg-elevated disabled:opacity-50"
			>
				Cancelar
			</button>
			<button
				type="button"
				onclick={() => void enviar()}
				disabled={ocupado}
				class="rounded-md bg-brand px-4 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi disabled:opacity-50"
			>
				{ocupado ? 'Enviando…' : 'Enviar solicitação'}
			</button>
		{/snippet}
	</Modal>
</div>
