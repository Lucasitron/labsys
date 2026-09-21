<script lang="ts">
	import { goto, invalidateAll } from '$app/navigation';
	import type { PageProps } from './$types';
	import { criarItem } from '$lib/api/stock/items';
	import { toasts, toastError } from '$lib/stores/toast';
	import PageHeader from '$lib/components/ui/PageHeader.svelte';
	import Select from '$lib/components/ui/Select.svelte';
	import Icon from '$lib/components/ui/Icon.svelte';

	let { data }: PageProps = $props();

	const UNITS = ['un', 'kg', 'g', 'm', 'm²', 'cm', 'l', 'ml', 'caixa', 'par', 'rolo', 'folha'];

	let name = $state('');
	let description = $state('');
	let category = $state('');
	let unit = $state('');
	let location = $state('');
	let initialQuantity = $state('');
	let minimum = $state('');

	let errors = $state<Record<string, string>>({});
	let submitting = $state(false);
	let saveMode = $state<'single' | 'another'>('single');

	function numeric(value: string): number {
		const n = Number(value.replace(',', '.'));
		return Number.isFinite(n) ? n : 0;
	}

	function validate(): boolean {
		const next: Record<string, string> = {};
		if (!name.trim()) next.name = 'Informe o nome';
		if (!category) next.category = 'Selecione a categoria';
		if (!unit) next.unit = 'Selecione a unidade';
		if (!location) next.location = 'Selecione a localização';
		if (minimum && numeric(minimum) < 0) next.minimum = 'Valor inválido';
		errors = next;
		return Object.keys(next).length === 0;
	}

	function clearField(field: string): void {
		if (errors[field]) {
			const next = { ...errors };
			delete next[field];
			errors = next;
		}
	}

	async function handleSubmit(e: Event, mode: 'single' | 'another'): Promise<void> {
		e.preventDefault();
		saveMode = mode;
		if (!validate()) return;

		submitting = true;
		try {
			await criarItem({
				nome: name.trim(),
				descricao: description.trim() || undefined,
				categoria: category as 'INSUMO' | 'FERRAMENTA' | 'PECA',
				unidadeMedida: unit,
				quantidadeAtual: initialQuantity ? numeric(initialQuantity) : 0,
				estoqueMinimo: minimum ? numeric(minimum) : 0,
				idLocalizacao: location || null
			});

			toasts.success(`Item "${name.trim()}" criado`);
			if (mode === 'another') {
				reset();
			} else {
				await goto('/estoque/itens', { invalidateAll: true });
			}
		} catch (err) {
			toastError(err, 'Não foi possível salvar o item');
		} finally {
			submitting = false;
		}
	}

	function reset(): void {
		name = '';
		description = '';
		category = '';
		unit = '';
		location = '';
		initialQuantity = '';
		minimum = '';
		errors = {};
	}

	function cancel(): void {
		void goto('/estoque/itens');
	}
</script>

<svelte:head>
	<title>Novo item — Estoque — FabLab</title>
</svelte:head>

<PageHeader
	title="Novo item"
	subtitle="Cadastre um novo item no inventário do FabLab"
	backHref="/estoque/itens"
>
	{#snippet children()}
		<button
			onclick={cancel}
			class="rounded-lg border border-border bg-surface px-3 py-2 text-sm font-medium text-ink transition-colors hover:bg-border/40"
		>
			Cancelar
		</button>
		<button
			onclick={(e) => handleSubmit(e, 'another')}
			disabled={submitting}
			class="rounded-lg border border-border bg-surface px-3 py-2 text-sm font-medium text-ink transition-colors hover:border-brand/50 disabled:opacity-50"
		>
			Salvar e adicionar outro
		</button>
		<button
			onclick={(e) => handleSubmit(e, 'single')}
			disabled={submitting}
			class="inline-flex items-center gap-1.5 rounded-lg bg-brand px-3 py-2 text-sm font-semibold text-white shadow-lg shadow-brand/20 transition-colors hover:bg-brandhi disabled:cursor-not-allowed disabled:opacity-50"
		>
			<Icon name="check" class="h-4 w-4" /> {submitting ? 'Salvando…' : 'Salvar item'}
		</button>
	{/snippet}
</PageHeader>

<form onsubmit={(e) => handleSubmit(e, 'single')} class="space-y-5" novalidate>
	<!-- Identificação -->
	<section class="rounded-xl border border-border bg-surface p-6">
		<h2 class="mb-4 text-xs font-medium uppercase tracking-wide text-muted">
			Identificação
		</h2>
		<div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
			<div class="sm:col-span-2">
				<label for="item-name" class="mb-1 block text-xs font-medium text-muted">
					Nome <span class="text-danger">*</span>
				</label>
				<input
					id="item-name"
					type="text"
					bind:value={name}
					oninput={() => clearField('name')}
					placeholder="Filamento PLA Preto 1.75mm"
					class="w-full rounded-lg border border-border bg-elevated px-3 py-2.5 text-sm text-ink placeholder:text-muted/60 focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/30 {errors.name ? 'border-danger' : ''}"
				/>
				{#if errors.name}
					<p class="mt-1 text-xs font-medium text-danger">{errors.name}</p>
				{/if}
			</div>

			<div class="sm:col-span-2">
				<label for="item-desc" class="mb-1 block text-xs font-medium text-muted">
					Descrição
				</label>
				<textarea
					id="item-desc"
					rows="3"
					bind:value={description}
					placeholder="Detalhes sobre o item, utilização e características."
					class="w-full rounded-lg border border-border bg-elevated px-3 py-2.5 text-sm text-ink placeholder:text-muted/60 focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/30"
				></textarea>
			</div>
		</div>
	</section>

	<!-- Classificação -->
	<section class="rounded-xl border border-border bg-surface p-6">
		<h2 class="mb-4 text-xs font-medium uppercase tracking-wide text-muted">
			Classificação
		</h2>
		<div class="grid grid-cols-1 gap-4 sm:grid-cols-3">
			<Select
				id="item-category"
				label="Categoria *"
				options={data.categories}
				value={category}
				onChange={(v) => {
					category = v;
					clearField('category');
				}}
				placeholder="Selecione…"
			/>
			<Select
				id="item-unit"
				label="Unidade *"
				options={UNITS.map((u) => ({ id: u, label: u }))}
				value={unit}
				onChange={(v) => {
					unit = v;
					clearField('unit');
				}}
				placeholder="Selecione…"
			/>
			<Select
				id="item-location"
				label="Localização *"
				options={data.locations}
				value={location}
				onChange={(v) => {
					location = v;
					clearField('location');
				}}
				placeholder="Selecione…"
			/>
		</div>
		{#if errors.category || errors.unit || errors.location}
			<p class="mt-2 text-xs font-medium text-danger">
				{errors.category ?? errors.unit ?? errors.location}
			</p>
		{/if}
	</section>

	<!-- Quantidades -->
	<section class="rounded-xl border border-border bg-surface p-6">
		<h2 class="mb-4 text-xs font-medium uppercase tracking-wide text-muted">
			Quantidades
		</h2>
		<div class="grid grid-cols-2 gap-4 lg:grid-cols-2">
			<div>
				<label for="item-initial" class="mb-1 block text-xs font-medium text-muted">
					Estoque inicial
				</label>
				<input
					id="item-initial"
					type="number"
					min="0"
					step="any"
					bind:value={initialQuantity}
					inputmode="decimal"
					class="w-full rounded-lg border border-border bg-elevated px-3 py-2.5 text-sm text-ink focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/30"
				/>
				{#if unit}
					<p class="mt-1 text-xs text-muted">em {unit}</p>
				{/if}
			</div>
			<div>
				<label for="item-min" class="mb-1 block text-xs font-medium text-muted">
					Estoque mínimo
				</label>
				<input
					id="item-min"
					type="number"
					min="0"
					step="any"
					bind:value={minimum}
					oninput={() => clearField('minimum')}
					inputmode="decimal"
					class="w-full rounded-lg border border-border bg-elevated px-3 py-2.5 text-sm text-ink focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/30 {errors.minimum
						? 'border-danger'
						: ''}"
				/>
				{#if errors.minimum}
					<p class="mt-1 text-xs font-medium text-danger">{errors.minimum}</p>
				{/if}
			</div>
		</div>
	</section>
</form>