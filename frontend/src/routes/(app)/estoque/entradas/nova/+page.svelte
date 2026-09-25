<script lang="ts">
	import { goto, invalidateAll } from '$app/navigation';
	import type { PageProps } from './$types';
	import type { StockItem } from '$lib/types/stock';
	import { criarEntrada } from '$lib/api/stock/movements';
	import { ApiError } from '$lib/api/client';
	import { toasts, toastError } from '$lib/stores/toast';
	import { toDateInputValue, fmtQty } from '$lib/utils/stock-format';
	import PageHeader from '$lib/components/ui/PageHeader.svelte';
	import Select from '$lib/components/ui/Select.svelte';
	import ItemPicker from '$lib/components/estoque/ItemPicker.svelte';
	import Icon from '$lib/components/ui/Icon.svelte';

	let { data }: PageProps = $props();

	let item = $state<StockItem | null>(null);
	let quantity = $state('');
	let date = $state(toDateInputValue());
	let supplierId = $state('');
	let notafiscal = $state('');
	let unitValue = $state('');
	let observation = $state('');

	let errors = $state<Record<string, string>>({});
	let serverError = $state('');
	let submitting = $state(false);

	const unit = $derived(item?.unidadeMedida ?? '');
	const noSuppliers = $derived(data.suppliers.length === 0);

	function numeric(value: string): number {
		const n = Number(value.replace(',', '.'));
		return Number.isFinite(n) ? n : 0;
	}

	function validate(): boolean {
		const next: Record<string, string> = {};
		if (!item) next.item = 'Selecione o item';
		if (!quantity || numeric(quantity) <= 0) next.quantity = 'Informe uma quantidade maior que zero';
		if (!date) next.date = 'Informe a data';
		if (noSuppliers) {
			next.supplierId = 'Lista de fornecedores indisponível — contate o administrador';
		} else if (!supplierId) {
			next.supplierId = 'Selecione o fornecedor';
		}
		if (unitValue && numeric(unitValue) < 0) next.unitValue = 'Valor inválido';
		errors = next;
		return Object.keys(next).length === 0;
	}

	async function handleSubmit(e: Event, mode: 'single' | 'another'): Promise<void> {
		e.preventDefault();
		serverError = '';
		if (!validate()) return;
		submitting = true;
		try {
			await criarEntrada({
				idItem: item!.id,
				idFornecedor: supplierId || null,
				quantidade: numeric(quantity),
				valorUnitario: unitValue ? numeric(unitValue) : null,
				dataEntrada: date,
				notaFiscal: notafiscal.trim() || undefined,
				observacao: observation.trim() || undefined
			});
			toasts.success(`Entrada de ${fmtQty(numeric(quantity), unit)} registrada`);
			if (mode === 'another') {
				reset();
			} else {
				await goto('/estoque/entradas', { invalidateAll: true });
			}
		} catch (err) {
			if (err instanceof ApiError && (err.status === 409 || err.status === 422)) {
				serverError = err.message;
			} else {
				toastError(err, 'Não foi possível registrar a entrada');
			}
		} finally {
			submitting = false;
		}
	}

	function clearField(field: string): void {
		if (errors[field]) {
			const next = { ...errors };
			delete next[field];
			errors = next;
		}
	}

	function reset(): void {
		item = null;
		quantity = '';
		date = toDateInputValue();
		supplierId = '';
		notafiscal = '';
		unitValue = '';
		observation = '';
		errors = {};
		serverError = '';
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
			data-testid="submit-entrada"
			type="button"
			onclick={(e) => handleSubmit(e, 'single')}
			disabled={submitting}
			class="inline-flex items-center gap-1.5 rounded-lg bg-brand px-3 py-2 text-sm font-semibold text-white shadow-lg shadow-brand/20 transition-colors hover:bg-brandhi disabled:opacity-50"
		>
			<Icon name="check" class="h-4 w-4" /> {submitting ? 'Salvando…' : 'Registrar entrada'}
		</button>
	{/snippet}
</PageHeader>

{#if serverError}
	<div
		data-testid="ent-nova-server-error"
		role="alert"
		class="rounded-xl border border-danger/30 bg-danger/5 px-4 py-3 text-sm text-danger"
	>
		{serverError}
	</div>
{/if}

{#if noSuppliers}
	<div
		data-testid="ent-nova-aviso"
		class="rounded-xl border border-warn/30 bg-warn/5 px-4 py-3 text-sm text-ink"
	>
		<strong class="font-semibold">Atenção 🟡</strong> A lista de fornecedores não pôde ser
		carregada. O registro de entrada fica bloqueado até o administrador disponibilizá-la.
	</div>
{/if}

<form onsubmit={(e) => handleSubmit(e, 'single')} class="space-y-5" novalidate data-testid="ent-nova-form">
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
						clearField('item');
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

	<!-- Fornecedor e detalhes -->
	<section class="rounded-xl border border-border bg-surface p-6">
		<h2 class="mb-4 text-xs font-medium uppercase tracking-wide text-muted">Fornecedor e detalhes</h2>
		<div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
			<div>
				<Select
					id="entry-supplier"
					label="Fornecedor *"
					options={data.suppliers}
					value={supplierId}
					disabled={noSuppliers}
					onChange={(v) => {
						supplierId = v;
						clearField('supplierId');
					}}
					placeholder="Selecione…"
				/>
				{#if errors.supplierId}
					<p class="mt-1 text-xs font-medium text-danger">{errors.supplierId}</p>
				{/if}
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
					class="w-full rounded-lg border border-border bg-elevated px-3 py-2.5 text-sm text-ink focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/30 {errors.unitValue
						? 'border-danger'
						: ''}"
				/>
				{#if errors.unitValue}
					<p class="mt-1 text-xs font-medium text-danger">{errors.unitValue}</p>
				{/if}
			</div>

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