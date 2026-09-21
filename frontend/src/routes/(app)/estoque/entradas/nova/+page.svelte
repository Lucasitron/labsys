<script lang="ts">
	import { goto, invalidateAll } from '$app/navigation';
	import type { PageProps } from './$types';
	import type { StockItem, EntryKind } from '$lib/types/stock';
	import { criarEntrada } from '$lib/api/stock/movements';
	import { toasts, toastError } from '$lib/stores/toast';
	import { toDateInputValue, fmtQty } from '$lib/utils/stock-format';
	import { entryKindMeta } from '$lib/utils/stock-status';
	import PageHeader from '$lib/components/ui/PageHeader.svelte';
	import Select from '$lib/components/ui/Select.svelte';
	import RadioCards from '$lib/components/ui/RadioCards.svelte';
	import ItemPicker from '$lib/components/estoque/ItemPicker.svelte';
	import Icon, { type IconName } from '$lib/components/ui/Icon.svelte';

	let { data }: PageProps = $props();

	const KINDS: EntryKind[] = ['compra', 'doacao', 'devolucao', 'ajuste'];
	const KIND_OPTIONS = KINDS.map((k) => {
		const meta = entryKindMeta(k);
		const icon: IconName =
			k === 'compra'
				? 'shopping-bag'
				: k === 'doacao'
					? 'gift'
					: k === 'devolucao'
						? 'arrow-uturn-left'
						: 'adjustments';
		return {
			id: k,
			label: meta.label,
			icon,
			color: meta.color
		};
	});

	let item = $state<StockItem | null>(null);
	let quantity = $state('');
	let kind = $state<EntryKind>('compra');
	let date = $state(toDateInputValue());
	let supplierId = $state('');
	let notafiscal = $state('');
	let unitValue = $state('');
	let observation = $state('');

	let errors = $state<Record<string, string>>({});
	let submitting = $state(false);

	const unit = $derived(item?.unidadeMedida ?? '');
	const supplierRequired = $derived(kind === 'compra' || kind === 'ajuste');

	function numeric(value: string): number {
		const n = Number(value.replace(',', '.'));
		return Number.isFinite(n) ? n : 0;
	}

	function validate(): boolean {
		const next: Record<string, string> = {};
		if (!item) next.item = 'Selecione o item';
		if (!quantity || numeric(quantity) <= 0) next.quantity = 'Informe uma quantidade maior que zero';
		if (!date) next.date = 'Informe a data';
		if (supplierRequired && !supplierId) next.supplierId = 'Selecione o fornecedor';
		if (kind === 'compra' && unitValue && numeric(unitValue) < 0) next.unitValue = 'Valor inválido';
		errors = next;
		return Object.keys(next).length === 0;
	}

	async function handleSubmit(e: Event, mode: 'single' | 'another'): Promise<void> {
		e.preventDefault();
		if (!validate()) return;
		submitting = true;
		try {
			await criarEntrada({
				idItem: item!.id,
				idFornecedor: supplierRequired ? supplierId || null : null,
				quantidade: numeric(quantity),
				valorUnitario: kind === 'compra' && unitValue ? numeric(unitValue) : null,
				dataEntrada: date,
				notaFiscal: kind === 'compra' ? notafiscal.trim() || undefined : undefined,
				observacao: observation.trim() || undefined
			});
			toasts.success(`Entrada de ${fmtQty(numeric(quantity), unit)} registrada`);
			if (mode === 'another') {
				reset();
			} else {
				await goto('/estoque/entradas', { invalidateAll: true });
			}
		} catch (err) {
			toastError(err, 'Não foi possível registrar a entrada');
		} finally {
			submitting = false;
		}
	}

	function reset(): void {
		item = null;
		quantity = '';
		kind = 'compra';
		date = toDateInputValue();
		supplierId = '';
		notafiscal = '';
		unitValue = '';
		observation = '';
		errors = {};
	}
</script>

<svelte:head>
	<title>Nova entrada — Estoque — FabLab</title>
</svelte:head>

<PageHeader title="Nova entrada" subtitle="Registre a entrada de itens no estoque" backHref="/estoque/entradas">
	{#snippet children()}
		<button
			type="button"
			onclick={() => void goto('/estoque/entradas')}
			class="rounded-lg border border-border bg-surface px-3 py-2 text-sm font-medium text-ink transition-colors hover:bg-border/40"
		>
			Cancelar
		</button>
		<button
			type="button"
			onclick={(e) => handleSubmit(e, 'another')}
			disabled={submitting}
			class="rounded-lg border border-border bg-surface px-3 py-2 text-sm font-medium text-ink transition-colors hover:border-brand/50 disabled:opacity-50"
		>
			Salvar e adicionar outro
		</button>
		<button
			type="button"
			onclick={(e) => handleSubmit(e, 'single')}
			disabled={submitting}
			class="inline-flex items-center gap-1.5 rounded-lg bg-brand px-3 py-2 text-sm font-semibold text-white shadow-lg shadow-brand/20 transition-colors hover:bg-brandhi disabled:opacity-50"
		>
			<Icon name="check" class="h-4 w-4" /> {submitting ? 'Salvando…' : 'Registrar entrada'}
		</button>
	{/snippet}
</PageHeader>

<form onsubmit={(e) => handleSubmit(e, 'single')} class="space-y-5" novalidate>
	<!-- Item -->
	<section class="rounded-xl border border-border bg-surface p-6">
		<h2 class="mb-4 text-xs font-medium uppercase tracking-wide text-muted">Item</h2>
		<div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
			<div class="sm:col-span-2">
				<label class="mb-1 block text-xs font-medium text-muted">
					Item <span class="text-danger">*</span>
				</label>
				<ItemPicker
					selected={item}
					onSelect={(i) => {
						item = i;
						if (errors.item) {
							const next = { ...errors };
							delete next.item;
							errors = next;
						}
					}}
				/>
				{#if errors.item}
					<p class="mt-1 text-xs font-medium text-danger">{errors.item}</p>
				{/if}
			</div>

			<div>
				<label for="entry-qty" class="mb-1 block text-xs font-medium text-muted">
					Quantidade <span class="text-danger">*</span>
				</label>
				<div class="relative">
					<input
						id="entry-qty"
						type="number"
						min="0"
						step="any"
						bind:value={quantity}
						inputmode="decimal"
						class="w-full rounded-lg border border-border bg-elevated px-3 py-2.5 pr-14 text-sm text-ink focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/30 {errors.quantity
							? 'border-danger'
							: ''}"
					/>
					<span class="pointer-events-none absolute inset-y-0 right-3 flex items-center text-sm text-muted">
						{unit || 'un'}
					</span>
				</div>
				{#if errors.quantity}
					<p class="mt-1 text-xs font-medium text-danger">{errors.quantity}</p>
				{/if}
			</div>

			<div>
				<label for="entry-date" class="mb-1 block text-xs font-medium text-muted">
					Data <span class="text-danger">*</span>
				</label>
				<input
					id="entry-date"
					type="date"
					max={toDateInputValue()}
					bind:value={date}
					class="w-full rounded-lg border border-border bg-elevated px-3 py-2.5 text-sm text-ink focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/30"
				/>
				{#if errors.date}
					<p class="mt-1 text-xs font-medium text-danger">{errors.date}</p>
				{/if}
			</div>
		</div>
	</section>

	<!-- Tipo de entrada -->
	<section class="rounded-xl border border-border bg-surface p-6">
		<h2 class="mb-4 text-xs font-medium uppercase tracking-wide text-muted">
			Tipo de entrada
		</h2>
		<RadioCards name="entry-kind" options={KIND_OPTIONS} value={kind} onChange={(id) => (kind = id as EntryKind)} />
	</section>

	<!-- Detalhes -->
	<section class="rounded-xl border border-border bg-surface p-6">
		<h2 class="mb-4 text-xs font-medium uppercase tracking-wide text-muted">Detalhes</h2>
		<div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
			{#if supplierRequired}
				<div>
					<Select
						id="entry-supplier"
						label="Fornecedor *"
						options={data.suppliers}
						value={supplierId}
						onChange={(v) => (supplierId = v)}
						placeholder="Selecione…"
					/>
					{#if errors.supplierId}
						<p class="mt-1 text-xs font-medium text-danger">{errors.supplierId}</p>
					{/if}
				</div>
			{:else}
				<div>
					<label class="mb-1 block text-xs font-medium text-muted">Origem</label>
					<p class="rounded-lg border border-border bg-elevated px-3 py-2.5 text-sm text-muted">
						{kind === 'devolucao' ? 'Devolução de empréstimo' : 'Doação recebida'}
					</p>
				</div>
			{/if}

			{#if kind === 'compra'}
				<div>
						<label for="entry-nf" class="mb-1 block text-xs font-medium text-muted">
							Nota fiscal
						</label>
						<input
							id="entry-nf"
							type="text"
							bind:value={notafiscal}
							placeholder="000123"
							class="w-full rounded-lg border border-border bg-elevated px-3 py-2.5 text-sm text-ink focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/30"
						/>
					</div>
					<div>
						<label for="entry-value" class="mb-1 block text-xs font-medium text-muted">
							Valor unitário (R$)
						</label>
						<input
							id="entry-value"
							type="number"
							min="0"
							step="0.01"
							bind:value={unitValue}
							inputmode="decimal"
							class="w-full rounded-lg border border-border bg-elevated px-3 py-2.5 text-sm text-ink focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/30"
						/>
						{#if errors.unitValue}
							<p class="mt-1 text-xs font-medium text-danger">{errors.unitValue}</p>
						{/if}
					</div>
			{/if}

			<div class="sm:col-span-2">
				<label for="entry-obs" class="mb-1 block text-xs font-medium text-muted">
					Observações
				</label>
				<textarea
					id="entry-obs"
					rows="3"
					bind:value={observation}
					placeholder="Informações adicionais sobre a entrada."
					class="w-full rounded-lg border border-border bg-elevated px-3 py-2.5 text-sm text-ink placeholder:text-muted/60 focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/30"
				></textarea>
			</div>
		</div>
	</section>
</form>