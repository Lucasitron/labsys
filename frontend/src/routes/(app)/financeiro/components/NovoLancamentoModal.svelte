<script lang="ts">
	import { createLancamento } from '$lib/api/financeiro/lancamentos';
	import type { CategoriaFinanceira, TipoLancamento } from '$lib/types/financeiro';
	import { toUserMessage } from '$lib/utils/errors';
	import { toasts } from '$lib/stores/toast';
	import Modal from '$lib/components/ui/Modal.svelte';
	import Select from '$lib/components/ui/Select.svelte';
	import MoneyInput from '$lib/components/ui/MoneyInput.svelte';

	interface Props {
		open: boolean;
		categorias: CategoriaFinanceira[];
		idPrefix: string;
		title?: string;
		subtitle?: string;
		confirmLabel?: string;
		categoriaWarn?: string;
		sucessoMsg?: string;
		observacaoLabel?: string;
		observacaoPlaceholder?: string;
		referenciaPlaceholder?: string;
		pagamentoLabel?: string;
		semCategoriaHint?: string;
		tipoFixo?: TipoLancamento;
		onClose: () => void;
		onCreated: () => Promise<void> | void;
	}

	let {
		open,
		categorias,
		idPrefix,
		title = 'Novo lançamento',
		subtitle = 'Registre uma entrada ou saída',
		confirmLabel = 'Criar lançamento',
		categoriaWarn = 'Selecione a categoria do lançamento.',
		sucessoMsg = 'Lançamento criado com sucesso.',
		observacaoLabel = 'Observação',
		observacaoPlaceholder = 'Opcional',
		referenciaPlaceholder = 'Ex.: EN-2051',
		pagamentoLabel = 'Pagamento',
		semCategoriaHint = 'Crie uma categoria em Lançamentos antes de lançar.',
		tipoFixo,
		onClose,
		onCreated
	}: Props = $props();

	const opcoes = $derived(categorias.map((c) => ({ id: c.id, label: c.nome })));

	let categoria = $state('');
	let tipo = $state<TipoLancamento>('Entrada');
	let valor = $state<number | null>(null);
	let vencimento = $state('');
	let pagamento = $state('');
	let referencia = $state('');
	let observacao = $state('');
	let ocupado = $state(false);

	$effect(() => {
		if (open) {
			categoria = '';
			tipo = tipoFixo ?? 'Entrada';
			valor = null;
			vencimento = '';
			pagamento = '';
			referencia = '';
			observacao = '';
		}
	});

	async function confirmar(): Promise<void> {
		if (!categoria) {
			toasts.warn(categoriaWarn);
			return;
		}
		if (valor === null || valor <= 0) {
			toasts.warn('Informe um valor maior que zero.');
			return;
		}
		if (!vencimento) {
			toasts.warn('Informe a data de vencimento.');
			return;
		}
		ocupado = true;
		try {
			await createLancamento({
				idCategoria: categoria,
				tipo: tipoFixo ?? tipo,
				valor,
				dataVencimento: vencimento,
				dataPagamento: pagamento || null,
				idReferenciaExterna: referencia.trim() || undefined,
				observacao: observacao.trim() || undefined
			});
			toasts.success(sucessoMsg);
			onClose();
			await onCreated();
		} catch (err) {
			toasts.danger(toUserMessage(err).message);
		} finally {
			ocupado = false;
		}
	}
</script>

<Modal {open} {title} {subtitle} {onClose} width="sm">
	{#snippet children()}
		<div class="space-y-3">
			<Select
				id="{idPrefix}-categoria"
				label="Categoria"
				options={opcoes}
				value={categoria}
				onChange={(v) => (categoria = v)}
				required
				placeholder={categorias.length === 0 ? 'Nenhuma categoria cadastrada' : 'Selecione…'}
				hint={categorias.length === 0 ? semCategoriaHint : ''}
			/>
			{#if !tipoFixo}
				<fieldset>
					<legend class="mb-1 block text-xs font-medium text-muted">Tipo</legend>
					<div class="flex gap-2">
						{#each [{ id: 'Entrada', label: 'Entrada' }, { id: 'Saída', label: 'Saída' }] as opt (opt.id)}
							<label
								class="flex flex-1 cursor-pointer items-center gap-2 rounded-md border px-3 py-2.5 text-sm transition {tipo === opt.id
									? 'border-brand/60 bg-brand/10 text-ink'
									: 'border-border bg-elevated text-muted'}"
							>
								<input
									type="radio"
									name="{idPrefix}-tipo"
									value={opt.id}
									checked={tipo === opt.id}
									onchange={() => (tipo = opt.id as TipoLancamento)}
									class="h-4 w-4 accent-brand"
								/>
								{opt.label}
							</label>
						{/each}
					</div>
				</fieldset>
			{/if}
			<MoneyInput bind:value={valor} label="Valor" />
			<div class="grid grid-cols-2 gap-3">
				<div>
					<label for="{idPrefix}-vencimento" class="mb-1 block text-xs font-medium text-muted">
						Vencimento <span class="text-danger">*</span>
					</label>
					<input
						id="{idPrefix}-vencimento"
						type="date"
						bind:value={vencimento}
						class="w-full rounded-md border border-border bg-elevated px-3 py-2.5 text-sm text-ink focus:border-brand focus:ring-2 focus:ring-brand/30 focus:outline-none"
					/>
				</div>
				<div>
					<label for="{idPrefix}-pagamento" class="mb-1 block text-xs font-medium text-muted">
						{pagamentoLabel}
					</label>
					<input
						id="{idPrefix}-pagamento"
						type="date"
						bind:value={pagamento}
						class="w-full rounded-md border border-border bg-elevated px-3 py-2.5 text-sm text-ink focus:border-brand focus:ring-2 focus:ring-brand/30 focus:outline-none"
					/>
				</div>
			</div>
			<div>
				<label for="{idPrefix}-referencia" class="mb-1 block text-xs font-medium text-muted">
					Referência
				</label>
				<input
					id="{idPrefix}-referencia"
					type="text"
					bind:value={referencia}
					placeholder={referenciaPlaceholder}
					class="w-full rounded-md border border-border bg-elevated px-3 py-2.5 font-mono text-sm text-ink placeholder:text-muted/60 focus:border-brand focus:ring-2 focus:ring-brand/30 focus:outline-none"
				/>
			</div>
			<div>
				<label for="{idPrefix}-observacao" class="mb-1 block text-xs font-medium text-muted">
					{observacaoLabel}
				</label>
				<textarea
					id="{idPrefix}-observacao"
					bind:value={observacao}
					rows={2}
					placeholder={observacaoPlaceholder}
					class="w-full rounded-md border border-border bg-elevated px-3 py-2.5 text-sm text-ink placeholder:text-muted/60 focus:border-brand focus:ring-2 focus:ring-brand/30 focus:outline-none"
				></textarea>
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
			onclick={() => void confirmar()}
			disabled={ocupado}
			class="rounded-md bg-brand px-4 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi disabled:opacity-50"
		>
			{ocupado ? 'Salvando…' : confirmLabel}
		</button>
	{/snippet}
</Modal>
