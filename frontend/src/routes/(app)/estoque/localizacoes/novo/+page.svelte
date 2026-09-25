<script lang="ts">
	import { goto } from '$app/navigation';
	import type { PageProps } from './$types';
	import { criarLocalizacao } from '$lib/api/stock/locations';
	import { ApiError } from '$lib/api/client';
	import { toasts, toastError } from '$lib/stores/toast';
	import PageHeader from '$lib/components/ui/PageHeader.svelte';
	import Icon from '$lib/components/ui/Icon.svelte';

	let { data }: PageProps = $props();

	let armario = $state('');
	let prateleira = $state('');
	let caixa = $state('');
	let descricao = $state('');

	let errors = $state<Record<string, string>>({});
	let serverError = $state('');
	let submitting = $state(false);

	function clearField(field: string): void {
		if (errors[field]) {
			const next = { ...errors };
			delete next[field];
			errors = next;
		}
	}

	function validate(): boolean {
		const next: Record<string, string> = {};
		if (!armario.trim()) next.armario = 'Informe o armário da localização';
		errors = next;
		return Object.keys(next).length === 0;
	}

	async function handleSubmit(e: Event): Promise<void> {
		e.preventDefault();
		serverError = '';
		if (!validate()) return;
		submitting = true;
		try {
			await criarLocalizacao({
				armario: armario.trim(),
				prateleira: prateleira.trim() || undefined,
				caixa: caixa.trim() || undefined,
				descricao: descricao.trim() || undefined
			});
			toasts.success('Localização cadastrada');
			await goto('/estoque/localizacoes', { invalidateAll: true });
		} catch (err) {
			if (err instanceof ApiError && (err.status === 409 || err.status === 422)) {
				serverError = err.message;
				errors = { ...errors, armario: err.message };
			} else {
				toastError(err, 'Não foi possível cadastrar a localização');
			}
		} finally {
			submitting = false;
		}
	}
</script>

<svelte:head>
	<title>Nova localização — Estoque — FabLab</title>
</svelte:head>

<div class="mx-auto max-w-3xl space-y-4">
	<PageHeader
		title="Nova localização"
		subtitle="Crie um ponto físico de armazenamento do estoque."
		backHref="/estoque/localizacoes"
	>
		{#snippet children()}
			<button
				type="button"
				onclick={() => void goto('/estoque/localizacoes')}
				class="rounded-lg border border-border bg-surface px-3 py-2 text-sm font-medium text-ink transition-colors hover:bg-border/40"
			>
				Cancelar
			</button>
			<button
				data-testid="submit-localizacao"
				type="button"
				onclick={(e) => handleSubmit(e)}
				disabled={submitting}
				class="inline-flex items-center gap-1.5 rounded-lg bg-brand px-3 py-2 text-sm font-semibold text-white shadow-lg shadow-brand/20 transition-colors hover:bg-brandhi disabled:cursor-not-allowed disabled:opacity-50"
			>
				<Icon name="check" class="h-4 w-4" />
				{submitting ? 'Salvando…' : 'Salvar localização'}
			</button>
		{/snippet}
	</PageHeader>

	{#if serverError}
		<div
			data-testid="localizacao-server-error"
			role="alert"
			class="rounded-xl border border-danger/30 bg-danger/5 px-4 py-3 text-sm text-danger"
		>
			{serverError}
		</div>
	{/if}

	<form
		onsubmit={(e) => handleSubmit(e)}
		class="rounded-xl border border-border bg-surface p-6"
		novalidate
		data-testid="localizacao-form"
	>
		<div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
			<div>
				<label for="localizacao-armario" class="mb-1 block text-xs font-medium text-muted">
					Armário <span class="text-danger">*</span>
				</label>
				<input
					id="localizacao-armario"
					data-testid="localizacao-codigo"
					type="text"
					maxlength="32"
					bind:value={armario}
					placeholder="Ex.: LOC-A1"
					oninput={() => clearField('armario')}
					class="w-full rounded-lg border border-border bg-elevated px-3 py-2.5 font-mono text-sm text-ink placeholder:text-muted/60 focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/30 {errors.armario
						? 'border-danger'
						: ''}"
				/>
				<p class="mt-1 text-xs text-muted">Identificação do armário (ou código da localização).</p>
				{#if errors.armario}
					<p class="mt-1 text-xs font-medium text-danger">{errors.armario}</p>
				{/if}
			</div>

			<div>
				<label for="localizacao-prateleira" class="mb-1 block text-xs font-medium text-muted">
					Prateleira
				</label>
				<input
					id="localizacao-prateleira"
					type="text"
					maxlength="32"
					bind:value={prateleira}
					placeholder="Ex.: A1"
					class="w-full rounded-lg border border-border bg-elevated px-3 py-2.5 text-sm text-ink placeholder:text-muted/60 focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/30"
				/>
			</div>

			<div>
				<label for="localizacao-caixa" class="mb-1 block text-xs font-medium text-muted">
					Caixa
				</label>
				<input
					id="localizacao-caixa"
					type="text"
					maxlength="32"
					bind:value={caixa}
					placeholder="Ex.: C-03"
					class="w-full rounded-lg border border-border bg-elevated px-3 py-2.5 text-sm text-ink placeholder:text-muted/60 focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/30"
				/>
			</div>

			<div class="sm:col-span-2">
				<label for="localizacao-descricao" class="mb-1 block text-xs font-medium text-muted">
					Descrição
				</label>
				<input
					id="localizacao-descricao"
					type="text"
					maxlength="120"
					bind:value={descricao}
					placeholder="Ex.: Prateleira inferior próxima à entrada do laboratório"
					class="w-full rounded-lg border border-border bg-elevated px-3 py-2.5 text-sm text-ink placeholder:text-muted/60 focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/30"
				/>
			</div>
		</div>
	</form>
</div>