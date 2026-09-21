<script lang="ts">
	import { goto, invalidateAll } from '$app/navigation';
	import type { PageProps } from './$types';
	import type { StockItem, ExitReason } from '$lib/types/stock';
	import { ApiError } from '$lib/api/client';
	import { criarSaida } from '$lib/api/stock/movements';
	import { toasts, toastError } from '$lib/stores/toast';
	import { fmtQty } from '$lib/utils/stock-format';
	import { exitReasonMeta, EXIT_REASONS } from '$lib/utils/stock-status';
	import PageHeader from '$lib/components/ui/PageHeader.svelte';
	import Select from '$lib/components/ui/Select.svelte';
	import RadioCards from '$lib/components/ui/RadioCards.svelte';
	import ItemPicker from '$lib/components/estoque/ItemPicker.svelte';
	import Icon, { type IconName } from '$lib/components/ui/Icon.svelte';

	let { data }: PageProps = $props();

	const REASON_OPTIONS = EXIT_REASONS.map((r) => {
		const meta = exitReasonMeta(r);
		const icon: IconName =
			r === 'CONSUMO'
				? 'office-building'
				: r === 'PERDA'
					? 'alert-triangle'
					: r === 'AJUSTE'
						? 'adjustments'
						: 'folder';
		return {
			id: r,
			label: meta.label,
			icon,
			color: meta.color
		};
	});

	let item = $state<StockItem | null>(null);
	let quantity = $state('');
	let reason = $state<ExitReason>('CONSUMO');
	let projectId = $state('');
	let observation = $state('');

	let errors = $state<Record<string, string>>({});
	let submitting = $state(false);

	const unit = $derived(item?.unidadeMedida ?? '');

	function numeric(value: string): number {
		const n = Number(value.replace(',', '.'));
		return Number.isFinite(n) ? n : 0;
	}

	function validate(): boolean {
		const next: Record<string, string> = {};
		if (!item) next.item = 'Selecione o item';
		if (!quantity || numeric(quantity) <= 0) next.quantity = 'Informe uma quantidade maior que zero';
		errors = next;
		return Object.keys(next).length === 0;
	}

	async function handleSubmit(e: Event): Promise<void> {
		e.preventDefault();
		if (!validate()) return;
		submitting = true;
		try {
			await criarSaida({
				idItem: item!.id,
				quantidade: numeric(quantity),
				tipoSaida: reason,
				idReferencia: reason === 'CONSUMO' && projectId ? projectId : null,
				observacao: observation.trim() || undefined
			});
			toasts.success(`Saída de ${fmtQty(numeric(quantity), unit)} registrada`);
			await goto('/estoque/saidas', { invalidateAll: true });
		} catch (err) {
			if (err instanceof ApiError && err.status === 422) {
				errors = { ...errors, quantity: err.message };
			} else {
				toastError(err, 'Não foi possível registrar a saída');
			}
		} finally {
			submitting = false;
		}
	}
</script>

<svelte:head>
	<title>Nova saída — Estoque — FabLab</title>
</svelte:head>

<PageHeader title="Nova saída" subtitle="Registre a saída de itens do estoque" backHref="/estoque/saidas">
	{#snippet children()}
		<button
			type="button"
			onclick={() => void goto('/estoque/saidas')}
			class="rounded-lg border border-border bg-surface px-3 py-2 text-sm font-medium text-ink transition-colors hover:bg-border/40"
		>
			Cancelar
		</button>
		<button
			type="button"
			onclick={(e) => handleSubmit(e)}
			disabled={submitting}
			class="inline-flex items-center gap-1.5 rounded-lg bg-brand px-3 py-2 text-sm font-semibold text-white shadow-lg shadow-brand/20 transition-colors hover:bg-brandhi disabled:opacity-50"
		>
			<Icon name="check" class="h-4 w-4" /> {submitting ? 'Salvando…' : 'Registrar saída'}
		</button>
	{/snippet}
</PageHeader>

<form onsubmit={(e) => handleSubmit(e)} class="space-y-5" novalidate>
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
					showStock
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
				<label for="exit-qty" class="mb-1 block text-xs font-medium text-muted">
					Quantidade <span class="text-danger">*</span>
				</label>
				<div class="relative">
					<input
						id="exit-qty"
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
		</div>
	</section>

	<!-- Motivo -->
	<section class="rounded-xl border border-border bg-surface p-6">
		<h2 class="mb-4 text-xs font-medium uppercase tracking-wide text-muted">Motivo da saída</h2>
		<RadioCards
			name="exit-reason"
			options={REASON_OPTIONS}
			value={reason}
			onChange={(id) => {
				reason = id as ExitReason;
				errors = { ...errors, projectId: '' };
			}}
		/>
	</section>

	<!-- Detalhes -->
	<section class="rounded-xl border border-border bg-surface p-6">
		<h2 class="mb-4 text-xs font-medium uppercase tracking-wide text-muted">Detalhes</h2>
		<div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
			{#if reason === 'CONSUMO'}
				<div>
					<Select
						id="exit-project"
						label="Projeto vinculado"
						options={data.projects}
						value={projectId}
						onChange={(v) => (projectId = v)}
						placeholder="Selecione…"
					/>
				</div>
			{:else}
				<div>
					<label class="mb-1 block text-xs font-medium text-muted">Referência</label>
					<p class="rounded-lg border border-border bg-elevated px-3 py-2.5 text-sm text-muted">
						{exitReasonMeta(reason).label}
					</p>
				</div>
			{/if}

			<div class="sm:col-span-2">
				<label for="exit-obs" class="mb-1 block text-xs font-medium text-muted">
					Observações
				</label>
				<textarea
					id="exit-obs"
					rows="3"
					bind:value={observation}
					placeholder="Informações adicionais sobre a saída."
					class="w-full rounded-lg border border-border bg-elevated px-3 py-2.5 text-sm text-ink placeholder:text-muted/60 focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/30"
				></textarea>
			</div>
		</div>
	</section>
</form>