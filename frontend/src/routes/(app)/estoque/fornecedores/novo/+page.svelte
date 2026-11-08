<script lang="ts">
	import { goto } from '$app/navigation';
	import type { PageProps } from './$types';
	import { criarFornecedor } from '$lib/api/stock/suppliers';
	import { ApiError } from '$lib/api/client';
	import { toasts, toastError } from '$lib/stores/toast';
	import PageHeader from '$lib/components/ui/PageHeader.svelte';
	import Icon from '$lib/components/ui/Icon.svelte';

	let { data }: PageProps = $props();

	let nome = $state('');
	let contato = $state('');
	let cnpj = $state('');

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
		if (!nome.trim()) next.nome = 'Informe o nome do fornecedor';
		errors = next;
		return Object.keys(next).length === 0;
	}

	async function handleSubmit(e: Event): Promise<void> {
		e.preventDefault();
		serverError = '';
		if (!validate()) return;
		submitting = true;
		try {
			await criarFornecedor({
				nome: nome.trim(),
				contato: contato.trim() || undefined,
				cnpj: cnpj.trim() || undefined
			});
			toasts.success('Fornecedor cadastrado');
			await goto('/estoque/fornecedores', { invalidateAll: true });
		} catch (err) {
			if (err instanceof ApiError && (err.status === 409 || err.status === 422)) {
				serverError = err.message;
				if (err.status === 409) {
					errors = { ...errors, cnpj: err.message };
				} else {
					errors = { ...errors, nome: err.message };
				}
			} else {
				toastError(err, 'Não foi possível cadastrar o fornecedor');
			}
		} finally {
			submitting = false;
		}
	}
</script>

<svelte:head>
	<title>Novo fornecedor — Estoque — FabLab</title>
</svelte:head>

<div class="mx-auto max-w-3xl space-y-4">
	<PageHeader
		title="Novo fornecedor"
		subtitle="Cadastre uma empresa parceira para as entradas de estoque."
		backHref="/estoque/fornecedores"
	>
		{#snippet children()}
			<button
				type="button"
				onclick={() => void goto('/estoque/fornecedores')}
				class="rounded-lg border border-border bg-surface px-3 py-2 text-sm font-medium text-ink transition-colors hover:bg-border/40"
			>
				Cancelar
			</button>
			<button
				data-testid="submit-fornecedor"
				type="button"
				onclick={(e) => handleSubmit(e)}
				disabled={submitting}
				class="inline-flex items-center gap-1.5 rounded-lg bg-brand px-3 py-2 text-sm font-semibold text-white shadow-lg shadow-brand/20 transition-colors hover:bg-brandhi disabled:cursor-not-allowed disabled:opacity-50"
			>
				<Icon name="check" class="h-4 w-4" /> {submitting ? 'Salvando…' : 'Salvar fornecedor'}
			</button>
		{/snippet}
	</PageHeader>

	{#if serverError}
		<div
			data-testid="fornecedor-server-error"
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
		data-testid="fornecedor-form"
	>
		<div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
			<div class="sm:col-span-2">
				<label for="fornecedor-nome" class="mb-1 block text-xs font-medium text-muted">
					Nome <span class="text-danger">*</span>
				</label>
				<input
					id="fornecedor-nome"
					data-testid="fornecedor-nome"
					type="text"
					bind:value={nome}
					placeholder="Ex.: TechSup Componentes Ltda"
					oninput={() => clearField('nome')}
					class="w-full rounded-lg border border-border bg-elevated px-3 py-2.5 text-sm text-ink placeholder:text-muted/60 focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/30 {errors.nome
						? 'border-danger'
						: ''}"
				/>
				{#if errors.nome}
					<p class="mt-1 text-xs font-medium text-danger">{errors.nome}</p>
				{/if}
			</div>

			<div>
				<label for="fornecedor-cnpj" class="mb-1 block text-xs font-medium text-muted">
					CNPJ
				</label>
				<input
					id="fornecedor-cnpj"
					data-testid="fornecedor-cnpj"
					type="text"
					maxlength="32"
					bind:value={cnpj}
					placeholder="00.000.000/0000-00"
					oninput={() => clearField('cnpj')}
					class="w-full rounded-lg border border-border bg-elevated px-3 py-2.5 font-mono text-sm text-ink placeholder:text-muted/60 focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/30 {errors.cnpj
						? 'border-danger'
						: ''}"
				/>
				{#if errors.cnpj}
					<p class="mt-1 text-xs font-medium text-danger">{errors.cnpj}</p>
				{/if}
			</div>

			<div>
				<label for="fornecedor-contato" class="mb-1 block text-xs font-medium text-muted">
					Contato
				</label>
				<input
					id="fornecedor-contato"
					type="tel"
					bind:value={contato}
					placeholder="(11) 91234-5678"
					class="w-full rounded-lg border border-border bg-elevated px-3 py-2.5 font-mono text-sm text-ink placeholder:text-muted/60 focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/30"
				/>
				<p class="mt-1 text-xs text-muted">Telefone, e-mail ou outro canal de contato.</p>
			</div>
		</div>
	</form>
</div>