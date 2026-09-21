<script lang="ts">
	import { goto } from '$app/navigation';
	import type { PageProps } from './$types';
	import type { StockItem } from '$lib/types/stock';
	import type { PersonOption as RhPersonOption } from '$lib/api/rh';
	import { ApiError } from '$lib/api/client';
	import { criarEmprestimo } from '$lib/api/stock/loans';
	import { toasts, toastError } from '$lib/stores/toast';
	import { fmtDate, fmtQty, toDateInputValue } from '$lib/utils/stock-format';
	import PageHeader from '$lib/components/ui/PageHeader.svelte';
	import ItemPicker from '$lib/components/estoque/ItemPicker.svelte';
	import PeoplePicker from '$lib/components/estoque/PeoplePicker.svelte';
	import Icon from '$lib/components/ui/Icon.svelte';

	let { data }: PageProps = $props();

	let item = $state<StockItem | null>(null);
	let person = $state<RhPersonOption | null>(null);
	let quantity = $state('');
	let prazo = $state(toDateInputValue());
	let observation = $state('');

	let errors = $state<Record<string, string>>({});
	let serverError = $state('');
	let submitting = $state(false);

	const unit = $derived(item?.unidadeMedida ?? '');

	function numeric(value: string): number {
		const n = Number(value.replace(',', '.'));
		return Number.isFinite(n) ? n : 0;
	}

	function clearField(field: string): void {
		if (errors[field]) {
			const next = { ...errors };
			delete next[field];
			errors = next;
		}
	}

	function validate(): boolean {
		const next: Record<string, string> = {};
		if (!item) next.item = 'Selecione o item';
		else if (!person) {
			next.person = 'Selecione a pessoa que levará o item';
		}
		const qty = numeric(quantity);
		if (!quantity || qty <= 0) next.quantity = 'Informe uma quantidade maior que zero';
		else if (item && qty > item.quantidadeAtual) {
			next.quantity = `Quantidade disponível: ${fmtQty(item.quantidadeAtual, unit)}. Informe um valor menor ou igual.`;
		}
		if (!prazo) next.prazo = 'Informe a data prevista de devolução';
		errors = next;
		return Object.keys(next).length === 0;
	}

	async function handleSubmit(e: Event): Promise<void> {
		e.preventDefault();
		serverError = '';
		if (!validate()) return;
		submitting = true;
		try {
			await criarEmprestimo({
				idItem: item!.id,
				idPessoa: person!.id,
				quantidade: numeric(quantity),
				dataDevolucaoPrevista: prazo,
				observacao: observation.trim() || undefined
			});
			toasts.success(`Empréstimo de ${fmtQty(numeric(quantity), unit)} registrado`);
			await goto('/estoque/emprestimos', { invalidateAll: true });
		} catch (err) {
			if (err instanceof ApiError && (err.status === 409 || err.status === 422)) {
				serverError = err.message;
				if (err.status === 422) errors = { ...errors, quantity: err.message };
			} else {
				toastError(err, 'Não foi possível registrar o empréstimo');
			}
		} finally {
			submitting = false;
		}
	}
</script>

<svelte:head>
	<title>Novo empréstimo — Estoque — FabLab</title>
</svelte:head>

<PageHeader
	title="Novo empréstimo"
	subtitle="Registre a retirada de um item com prazo de devolução"
	backHref="/estoque/emprestimos"
>
	{#snippet children()}
		<button
			type="button"
			onclick={() => void goto('/estoque/emprestimos')}
			class="rounded-lg border border-border bg-surface px-3 py-2 text-sm font-medium text-ink transition-colors hover:bg-border/40"
		>
			Cancelar
		</button>
		<button
			data-testid="submit-emprestimo"
			type="button"
			onclick={(e) => handleSubmit(e)}
			disabled={submitting}
			class="inline-flex items-center gap-1.5 rounded-lg bg-brand px-3 py-2 text-sm font-semibold text-white shadow-lg shadow-brand/20 transition-colors hover:bg-brandhi disabled:opacity-50"
		>
			<Icon name="check" class="h-4 w-4" /> {submitting ? 'Salvando…' : 'Registrar empréstimo'}
		</button>
	{/snippet}
</PageHeader>

{#if serverError}
	<div
		data-testid="emp-novo-server-error"
		role="alert"
		class="rounded-xl border border-danger/30 bg-danger/5 px-4 py-3 text-sm text-danger"
	>
		{serverError}
	</div>
{/if}

<form onsubmit={(e) => handleSubmit(e)} class="space-y-5" novalidate data-testid="emp-novo-form">
	<!-- Item -->
	<section class="rounded-xl border border-border bg-surface p-6">
		<h2 class="mb-4 text-xs font-medium uppercase tracking-wide text-muted">Item emprestado</h2>
		<div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
			<div class="sm:col-span-2">
				<label class="mb-1 block text-xs font-medium text-muted">
					Item <span class="text-danger">*</span>
				</label>
				<ItemPicker
					selected={item}
					showStock
					hint="Busque pelo nome ou descrição do item."
					onSelect={(i) => {
						item = i;
						clearField('item');
						clearField('quantity');
					}}
				/>
				{#if errors.item}
					<p class="mt-1 text-xs font-medium text-danger">{errors.item}</p>
				{/if}
			</div>

			<div>
				<label for="emp-qty" class="mb-1 block text-xs font-medium text-muted">
					Quantidade <span class="text-danger">*</span>
				</label>
				<div class="relative">
					<input
						id="emp-qty"
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
				<label for="emp-prazo" class="mb-1 block text-xs font-medium text-muted">
					Prazo de devolução <span class="text-danger">*</span>
				</label>
				<input
					id="emp-prazo"
					type="date"
					min={toDateInputValue()}
					bind:value={prazo}
					class="w-full rounded-lg border border-border bg-elevated px-3 py-2.5 text-sm text-ink focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/30 {errors.prazo
						? 'border-danger'
						: ''}"
				/>
				<p class="mt-1 text-xs text-muted">Data prevista para o retorno do item.</p>
				{#if errors.prazo}
					<p class="mt-1 text-xs font-medium text-danger">{errors.prazo}</p>
				{/if}
			</div>
		</div>
	</section>

	<!-- Tomador -->
	<section class="rounded-xl border border-border bg-surface p-6">
		<h2 class="mb-4 text-xs font-medium uppercase tracking-wide text-muted">Tomador</h2>
		<div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
			<div class="sm:col-span-2">
				<label class="mb-1 block text-xs font-medium text-muted">
					Pessoa <span class="text-danger">*</span>
				</label>
				<PeoplePicker
					selected={person}
					hint="Pessoa que está levando o item emprestado."
					onSelect={(p) => {
						person = p;
						clearField('person');
					}}
				/>
				{#if errors.person}
					<p class="mt-1 text-xs font-medium text-danger">{errors.person}</p>
				{/if}
			</div>
		</div>
	</section>

	<!-- Observações -->
	<section class="rounded-xl border border-border bg-surface p-6">
		<h2 class="mb-4 text-xs font-medium uppercase tracking-wide text-muted">Observações</h2>
		<div class="grid grid-cols-1 gap-4">
			<div>
				<label for="emp-obs" class="mb-1 block text-xs font-medium text-muted">Observações</label>
				<textarea
					id="emp-obs"
					rows="3"
					bind:value={observation}
					placeholder="Condição do item, responsável pela retirada, etc."
					class="w-full rounded-lg border border-border bg-elevated px-3 py-2.5 text-sm text-ink placeholder:text-muted/60 focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/30"
				></textarea>
				<p class="mt-1 text-xs text-muted">
					Informações adicionais sobre o empréstimo ({observation.trim().length} caracteres).
				</p>
			</div>
		</div>
	</section>
</form>

{#if item}
	<div class="rounded-xl border border-brand/30 bg-brand/5 px-4 py-3 text-sm text-ink">
		<strong class="font-semibold">Resumo:</strong>
		{fmtQty(numeric(quantity) || 0, unit)}
		de <strong class="font-medium">{item.nome}</strong> para
		<strong class="font-medium">{person?.name ?? '…'}</strong>, devolução prevista para
		<strong class="font-medium">{fmtDate(prazo)}</strong>.
	</div>
{/if}