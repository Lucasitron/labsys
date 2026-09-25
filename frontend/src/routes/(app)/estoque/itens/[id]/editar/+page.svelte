<script lang="ts">
	import { goto, invalidateAll } from '$app/navigation';
	import type { PageProps } from './$types';
	import type { CategoriaEnum } from '$lib/types/stock';
	import { ApiError } from '$lib/api/client';
	import { atualizarItem } from '$lib/api/stock/items';
	import { toasts, toastError } from '$lib/stores/toast';
	import { fmtQty } from '$lib/utils/stock-format';
	import { quantityTone } from '$lib/utils/stock-status';
	import PageHeader from '$lib/components/ui/PageHeader.svelte';
	import Select from '$lib/components/ui/Select.svelte';
	import Icon from '$lib/components/ui/Icon.svelte';
	import EmptyState from '$lib/components/ui/EmptyState.svelte';
	import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte';
	import Skeleton from '$lib/components/ui/Skeleton.svelte';

	let { data }: PageProps = $props();

	const item = $derived(data.item);
	const loadError = $derived(data.error);
	const notFound = $derived(data.notFound);

	const UNITS = ['un', 'kg', 'g', 'm', 'm²', 'cm', 'l', 'ml', 'caixa', 'par', 'rolo', 'folha'];

	let name = $state(item?.nome ?? '');
	let description = $state(item?.descricao ?? '');
	let category = $state(item?.categoria ?? '');
	let unit = $state(item?.unidadeMedida ?? '');
	let location = $state(item?.localizacao?.id ?? '');
	let minimum = $state(item ? String(item.estoqueMinimo ?? 0) : '');

	let errors = $state<Record<string, string>>({});
	let serverError = $state('');
	let submitting = $state(false);

	const toneText = {
		success: 'text-success',
		warn: 'text-warn',
		danger: 'text-danger',
		brand: 'text-brandhi',
		muted: 'text-muted',
		ink: 'text-ink'
	} as const;

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
		if (!name.trim()) next.name = 'Informe o nome';
		if (!category) next.category = 'Selecione a categoria';
		if (!unit) next.unit = 'Selecione a unidade';
		if (minimum.length > 0 && numeric(minimum) < 0) next.minimum = 'Valor inválido';
		errors = next;
		return Object.keys(next).length === 0;
	}

	async function handleSubmit(e: Event): Promise<void> {
		e.preventDefault();
		serverError = '';
		if (!validate()) return;
		submitting = true;
		try {
			await atualizarItem(item!.id, {
				nome: name.trim(),
				descricao: description.trim() || undefined,
				categoria: category as CategoriaEnum,
				unidadeMedida: unit,
				estoqueMinimo: minimum ? numeric(minimum) : 0,
				idLocalizacao: location || null
			});
			toasts.success(`Item "${name.trim()}" atualizado`);
			await goto(`/estoque/itens/${item!.id}`);
		} catch (err) {
			if (err instanceof ApiError && (err.status === 409 || err.status === 422)) {
				serverError = err.message;
			} else {
				toastError(err, 'Não foi possível salvar o item');
			}
		} finally {
			submitting = false;
		}
	}

	function cancel(): void {
		if (item) void goto(`/estoque/itens/${item.id}`);
		else void goto('/estoque/itens');
	}
</script>

<svelte:head>
	<title>{item ? `Editar ${item.nome}` : 'Editar item'} — Estoque — FabLab</title>
</svelte:head>

{#if loadError && !item}
	{#if notFound}
		<div data-testid="edit-empty" class="rounded-xl border border-border bg-surface">
			<div class="flex flex-col items-center justify-center px-6 py-16 text-center">
				<span
					class="mb-4 flex h-12 w-12 items-center justify-center rounded-xl border border-brand/30 bg-brand/10"
				>
					<Icon name="box" class="h-5 w-5 text-brand" />
				</span>
				<h3 class="mb-1 text-sm font-semibold text-ink">Item não encontrado</h3>
				<p class="mb-5 max-w-xs text-xs text-muted">
					O item pode ter sido excluído ou o endereço está incorreto. Verifique e tente novamente.
				</p>
				<a
					data-testid="edit-empty-back"
					href="/estoque/itens"
					class="inline-flex items-center gap-1.5 rounded-md border border-border bg-elevated px-3.5 py-2 text-xs font-medium text-ink transition-colors hover:bg-elevated/70"
				>
					<Icon name="chevron-left" class="h-3.5 w-3.5" /> Voltar para itens
				</a>
			</div>
		</div>
	{:else}
		<div data-testid="edit-error">
			<ErrorBanner
				message="Não foi possível carregar o item"
				hint={loadError}
				onRetry={() => void invalidateAll()}
			/>
		</div>
	{/if}
{:else if !item}
	<div data-testid="edit-loading" class="space-y-5">
		<div class="flex items-center gap-3">
			<Skeleton class="h-7 w-32" />
		</div>
		<Skeleton class="h-8 w-56" />
		<div class="space-y-5 rounded-xl border border-border bg-surface p-6">
			<Skeleton class="h-3 w-32" />
			<div class="grid grid-cols-1 gap-4 md:grid-cols-2">
				<div class="space-y-2">
					<Skeleton class="h-3 w-24" />
					<Skeleton class="h-10 w-full" />
				</div>
				<div class="space-y-2">
					<Skeleton class="h-3 w-24" />
					<Skeleton class="h-10 w-full" />
				</div>
			</div>
		</div>
		<div class="space-y-5 rounded-xl border border-border bg-surface p-6">
			<Skeleton class="h-3 w-32" />
			<div class="grid grid-cols-1 gap-4 md:grid-cols-3">
				{#each [0, 1, 2] as i (i)}
					<div class="space-y-2">
						<Skeleton class="h-3 w-24" />
						<Skeleton class="h-10 w-full" />
					</div>
				{/each}
			</div>
		</div>
		<div class="flex justify-end gap-3">
			<Skeleton class="h-10 w-28" />
			<Skeleton class="h-10 w-36" />
		</div>
	</div>
{:else}
	<div class="space-y-5">
		<a
			data-testid="back-detail"
			href={`/estoque/itens/${item.id}`}
			class="inline-flex items-center gap-2 text-xs font-medium text-muted transition-colors hover:text-ink"
		>
			<Icon name="chevron-left" class="h-3.5 w-3.5" /> Voltar para o item
		</a>

		<PageHeader
			title="Editar item"
			subtitle="Atualize os dados do insumo, ferramenta ou componente."
		>
			{#snippet children()}
				<button
					data-testid="cancel-edit"
					type="button"
					onclick={cancel}
					class="rounded-lg border border-border bg-surface px-3 py-2 text-sm font-medium text-ink transition-colors hover:bg-border/40"
				>
					Cancelar
				</button>
				<button
					data-testid="submit-item"
					type="button"
					onclick={(e) => handleSubmit(e)}
					disabled={submitting}
					class="inline-flex items-center gap-1.5 rounded-lg bg-brand px-3 py-2 text-sm font-semibold text-white shadow-lg shadow-brand/20 transition-colors hover:bg-brandhi disabled:cursor-not-allowed disabled:opacity-50"
				>
					<Icon name="check" class="h-4 w-4" /> {submitting ? 'Salvando…' : 'Salvar alterações'}
				</button>
			{/snippet}
		</PageHeader>

		{#if serverError}
			<div
				data-testid="edit-server-error"
				role="alert"
				class="rounded-xl border border-danger/30 bg-danger/5 px-4 py-3 text-sm text-danger"
			>
				{serverError}
			</div>
		{/if}

		<form
			onsubmit={(e) => handleSubmit(e)}
			class="space-y-5"
			novalidate
			data-testid="edit-item-form"
		>
			<!-- Identificação -->
			<section class="rounded-xl border border-border bg-surface p-6">
				<h2 class="mb-4 text-xs font-medium uppercase tracking-wide text-muted">Identificação</h2>
				<div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
					<div class="sm:col-span-2">
						<label for="item-name" class="mb-1 block text-xs font-medium text-muted">
							Nome <span class="text-danger">*</span>
						</label>
						<input
							id="item-name"
							data-testid="item-name"
							type="text"
							bind:value={name}
							oninput={() => clearField('name')}
							class="w-full rounded-lg border border-border bg-elevated px-3 py-2.5 text-sm text-ink placeholder:text-muted/60 focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/30 {errors.name ? 'border-danger' : ''}"
						/>
						{#if errors.name}
							<p class="mt-1 text-xs font-medium text-danger">{errors.name}</p>
						{/if}
					</div>

					<div class="sm:col-span-2">
						<label for="item-description" class="mb-1 block text-xs font-medium text-muted">
							Descrição
						</label>
						<textarea
							id="item-description"
							data-testid="item-description"
							rows="3"
							bind:value={description}
							class="w-full rounded-lg border border-border bg-elevated px-3 py-2.5 text-sm text-ink placeholder:text-muted/60 focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/30"
						></textarea>
					</div>
				</div>
			</section>

			<!-- Classificação -->
			<section class="rounded-xl border border-border bg-surface p-6">
				<h2 class="mb-4 text-xs font-medium uppercase tracking-wide text-muted">Classificação</h2>
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
						label="Localização"
						options={data.locations}
						value={location}
						onChange={(v) => {
							location = v;
							clearField('location');
						}}
						placeholder="Selecione…"
					/>
				</div>
				{#if errors.category || errors.unit}
					<p class="mt-2 text-xs font-medium text-danger">{errors.category ?? errors.unit}</p>
				{/if}
			</section>

			<!-- Quantidades -->
			<section class="rounded-xl border border-border bg-surface p-6">
				<h2 class="mb-4 text-xs font-medium uppercase tracking-wide text-muted">
					Quantidades e reposição
				</h2>
				<div class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
					<div>
						<span class="mb-1 block text-xs font-medium text-muted">Estoque atual</span>
						<div
							data-testid="item-stock"
							class="flex items-center rounded-lg border border-border bg-elevated/50 px-3 py-2.5 text-sm"
						>
							<span class="font-mono font-semibold {toneText[quantityTone(item.quantidadeAtual, item.estoqueMinimo)]}">
								{fmtQty(item.quantidadeAtual, item.unidadeMedida)}
							</span>
						</div>
						<p class="mt-1 text-[11px] {item.quantidadeAtual < item.estoqueMinimo ? 'text-warn' : 'text-muted'}">
							{item.quantidadeAtual < item.estoqueMinimo
								? 'abaixo do mínimo — ajuste o saldo por entradas/saídas'
								: 'O saldo é ajustado apenas por entradas e saídas — não é editável aqui.'}
						</p>
					</div>

					<div>
						<label for="item-min" class="mb-1 block text-xs font-medium text-muted">
							Estoque mínimo
						</label>
						<input
							id="item-min"
							data-testid="item-min"
							type="number"
							min="0"
							step="any"
							bind:value={minimum}
							oninput={() => clearField('minimum')}
							inputmode="decimal"
							class="w-full rounded-lg border border-border bg-elevated px-3 py-2.5 text-sm text-ink focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/30 {errors.minimum ? 'border-danger' : ''}"
						/>
						{#if errors.minimum}
							<p class="mt-1 text-xs font-medium text-danger">{errors.minimum}</p>
						{/if}
					</div>
				</div>
			</section>
		</form>
	</div>
{/if}