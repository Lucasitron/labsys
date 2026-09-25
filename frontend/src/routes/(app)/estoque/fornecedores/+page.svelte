<script lang="ts">
	import { goto, invalidateAll } from '$app/navigation';
	import type { PageProps } from './$types';
	import type { Fornecedor } from '$lib/types/stock';
	import PageHeader from '$lib/components/ui/PageHeader.svelte';
	import SearchInput from '$lib/components/ui/SearchInput.svelte';
	import TableSkeleton from '$lib/components/ui/TableSkeleton.svelte';
	import EmptyState from '$lib/components/ui/EmptyState.svelte';
	import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte';
	import Icon from '$lib/components/ui/Icon.svelte';

	let { data }: PageProps = $props();

	const loaded = $derived(!!data);
	const canEdit = $derived(data?.canEdit ?? false);
	const search = $derived(data?.search ?? '');
	const fornecedores = $derived<Fornecedor[]>(data?.fornecedores ?? []);
	const forbidden = $derived(data?.forbidden ?? false);
	const error = $derived(data?.error ?? null);
	const filtering = $derived(search.trim() !== '');

	function initials(nome: string): string {
		return nome
			.split(/\s+/)
			.filter(Boolean)
			.slice(0, 2)
			.map((part) => part[0]?.toUpperCase() ?? '')
			.join('');
	}

	function handleSearch(value: string): void {
		const params = new URLSearchParams();
		if (value.trim()) params.set('search', value.trim());
		const qs = params.toString();
		void goto(`/estoque/fornecedores${qs ? `?${qs}` : ''}`, { invalidateAll: true });
	}

	function limparBusca(): void {
		void goto('/estoque/fornecedores', { invalidateAll: true });
	}
</script>

<svelte:head>
	<title>Fornecedores — Estoque — FabLab</title>
</svelte:head>

<div class="space-y-4">
	<PageHeader
		title="Fornecedores"
		subtitle="Cadastro de fornecedores e empresas parceiras"
	>
		{#snippet children()}
			{#if canEdit}
				<a
					data-testid="for-novo"
					href="/estoque/fornecedores/novo"
					class="inline-flex items-center gap-1.5 rounded-lg bg-brand px-3 py-2 text-sm font-semibold text-white shadow-lg shadow-brand/20 transition-colors hover:bg-brandhi"
				>
					<Icon name="plus" class="h-4 w-4" /> Novo fornecedor
				</a>
			{/if}
		{/snippet}
	</PageHeader>

	{#if !loaded}
		<div class="overflow-hidden rounded-xl border border-border bg-surface">
			<div class="border-b border-border bg-elevated/50 px-4 py-2.5">
				<TableSkeleton rows={1} columns={3} class="!space-y-0" />
			</div>
			<div class="p-4" data-testid="for-carregando">
				<TableSkeleton rows={6} columns={3} />
			</div>
		</div>
	{:else if forbidden}
		<div data-testid="for-sem-permissao">
			<div class="rounded-xl border border-border bg-surface">
				<EmptyState
					icon="lock"
					title="Sem permissão"
					description="Seu perfil não pode consultar fornecedores. Procure um administrador para liberar o acesso ou entrar em contato."
				/>
			</div>
		</div>
	{:else if error}
		<div data-testid="for-erro">
			<ErrorBanner
				message="Não foi possível carregar os fornecedores"
				hint={error}
				onRetry={() => void invalidateAll()}
			/>
		</div>
	{:else}
		<div data-testid="for-busca" class="rounded-xl border border-border bg-surface p-4">
			<SearchInput
				value={search}
				onSearch={handleSearch}
				placeholder="Buscar por nome, CNPJ ou contato…"
				label="Buscar fornecedores"
			/>
		</div>

		{#if fornecedores.length === 0}
			<div class="rounded-xl border border-border bg-surface" data-testid={filtering ? 'for-vazio-filtro' : 'for-vazio'}>
				{#if filtering}
					<EmptyState
						icon="search"
						title="Nenhum fornecedor encontrado"
						description="Não há fornecedores que correspondam à busca. Ajuste o termo ou limpe a busca."
					>
						{#snippet children()}
							<button
								data-testid="for-limpar-busca"
								onclick={limparBusca}
								class="rounded-lg border border-border bg-surface px-3 py-2 text-sm font-medium text-ink transition-colors hover:border-brand/50 hover:text-brandhi"
							>
								Limpar busca
							</button>
						{/snippet}
					</EmptyState>
				{:else}
					<EmptyState
						icon="building-office"
						title="Ainda não há fornecedores"
						description="Cadastre o primeiro fornecedor para usá-lo nas entradas de estoque."
					>
						{#snippet children()}
							{#if canEdit}
								<a
									data-testid="for-vazio-novo"
									href="/estoque/fornecedores/novo"
									class="inline-flex items-center gap-1.5 rounded-lg bg-brand px-3 py-2 text-sm font-semibold text-white transition-colors hover:bg-brandhi"
								>
									<Icon name="plus" class="h-4 w-4" /> Novo fornecedor
								</a>
							{/if}
						{/snippet}
					</EmptyState>
				{/if}
			</div>
		{:else}
			<div class="overflow-hidden rounded-xl border border-border bg-surface" data-testid="for-lista">
				<div class="overflow-x-auto">
					<table class="w-full text-sm">
						<thead>
							<tr class="border-b border-border text-left text-[11px] uppercase tracking-wide text-muted">
								<th class="px-4 py-2.5 font-medium">Fornecedor</th>
								<th class="px-4 py-2.5 font-medium">CNPJ</th>
								<th class="px-4 py-2.5 font-medium">Contato</th>
							</tr>
						</thead>
						<tbody class="divide-y divide-border">
							{#each fornecedores as fornecedor (fornecedor.id)}
								<tr class="transition-colors hover:bg-elevated/40">
									<td class="px-4 py-3">
										<span class="flex items-center gap-2.5">
											<span
												class="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg border border-brand/30 bg-brand/10 text-[10px] font-bold text-brand"
												aria-hidden="true"
											>
												{initials(fornecedor.nome)}
											</span>
											<span class="min-w-0">
												<span class="block truncate text-sm font-medium text-ink">{fornecedor.nome}</span>
											</span>
										</span>
									</td>
									<td class="px-4 py-3 font-mono text-xs text-muted">
										{fornecedor.cnpj || '—'}
									</td>
									<td class="px-4 py-3 text-sm text-muted">
										{fornecedor.contato || '—'}
									</td>
								</tr>
							{/each}
						</tbody>
					</table>
				</div>
				<div class="border-t border-border px-4 py-3" data-testid="for-contagem">
					<span class="text-xs text-muted">
						{fornecedores.length} {fornecedores.length === 1 ? 'registro' : 'registros'}
					</span>
				</div>
			</div>
		{/if}
	{/if}
</div>