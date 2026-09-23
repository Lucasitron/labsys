<script lang="ts">
	import { goto, invalidateAll } from '$app/navigation';
	import type { PageProps } from './$types';
	import { get } from 'svelte/store';
	import { auth } from '$lib/stores/auth';
	import type { Cliente, Tag } from '$lib/types/vendas';
	import type { Tone } from '$lib/types/stock';
	import { bulkAdicionarTag, deleteCliente, listTags } from '$lib/api/vendas/clientes';
	import { ApiError } from '$lib/api/client';
	import { canEditVendas } from '$lib/utils/permissions';
	import { toUserMessage } from '$lib/utils/errors';
	import { toasts } from '$lib/stores/toast';
	import PageHeader from '$lib/components/ui/PageHeader.svelte';
	import SearchInput from '$lib/components/ui/SearchInput.svelte';
	import Dropdown from '$lib/components/ui/Dropdown.svelte';
	import Chip from '$lib/components/ui/Chip.svelte';
	import Pagination from '$lib/components/ui/Pagination.svelte';
	import Avatar from '$lib/components/ui/Avatar.svelte';
	import Icon from '$lib/components/ui/Icon.svelte';
	import TypeBadge from '$lib/components/ui/TypeBadge.svelte';
	import TableSkeleton from '$lib/components/ui/TableSkeleton.svelte';
	import EmptyState from '$lib/components/ui/EmptyState.svelte';
	import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte';
	import RowActions from '$lib/components/ui/RowActions.svelte';
	import BulkActionsBar from '$lib/components/ui/BulkActionsBar.svelte';
	import ConfirmDialog from '$lib/components/ui/ConfirmDialog.svelte';
	import Modal from '$lib/components/ui/Modal.svelte';
	import Select from '$lib/components/ui/Select.svelte';
	import SolicitarEdicaoModal from '../components/SolicitarEdicaoModal.svelte';

	let { data }: PageProps = $props();

	const usuario = $derived(get(auth).user);
	const params = $derived(data.params);
	const resultado = $derived(data.resultado);
	const erro = $derived(data.error);

	const clientes = $derived(resultado?.clientes ?? []);
	const paginacao = $derived(resultado?.pagination);

	const carregando = $derived(resultado === null && erro === null);
	const comErro = $derived(erro !== null && resultado === null);

	const temFiltros = $derived(
		params.search !== '' || params.tipo !== '' || params.tags !== '' || params.ordenar !== ''
	);

	const TIPO_OPCOES = [
		{ id: 'pf', label: 'Pessoa física' },
		{ id: 'pj', label: 'Pessoa jurídica' }
	];

	const ORDENAR_OPCOES = [
		{ id: 'nome', label: 'Nome A–Z' },
		{ id: 'recentes', label: 'Mais recentes' },
		{ id: 'compras', label: 'Mais compras' }
	];

	function tipoLabel(tipo: Cliente['tipoPessoa']): string {
		return tipo === 'pf' ? 'Pessoa física' : 'Pessoa jurídica';
	}

	function tagTone(cor: Tone): 'brand' | 'success' | 'warn' | 'danger' | 'muted' {
		return cor === 'ink' ? 'muted' : cor;
	}

	// ---- Tags para o filtro (servidas; erro silencioso — filtro segue com as da página) ----

	let tagsDisponiveis = $state<Tag[]>([]);

	$effect(() => {
		const ctrl = new AbortController();
		const sinal = ctrl.signal;
		listTags((input, init) => fetch(input, { ...init, signal: sinal }))
			.then((tags) => {
				if (!sinal.aborted) tagsDisponiveis = tags;
			})
			.catch(() => {
				if (!sinal.aborted) tagsDisponiveis = [];
			});
		return () => ctrl.abort();
	});

	const TAG_OPCOES = $derived(tagsDisponiveis.map((t) => ({ id: t.id, label: t.nome })));

	interface Query {
		[key: string]: string | number | undefined;
	}

	function navegar(overrides: Query): void {
		const base: Query = {
			search: params.search || undefined,
			tipo: params.tipo || undefined,
			tags: params.tags || undefined,
			ordenar: params.ordenar || undefined,
			page: params.page,
			pageSize: params.pageSize === 10 ? undefined : params.pageSize
		};
		const merged = { ...base, ...overrides };
		const url = new URLSearchParams();
		for (const [chave, valor] of Object.entries(merged)) {
			if (valor !== undefined && valor !== '') url.append(chave, String(valor));
		}
		const qs = url.toString();
		void goto(`/vendas/clientes${qs ? `?${qs}` : ''}`);
	}

	function onSearch(termo: string): void {
		navegar({ search: termo || undefined, page: 1 });
	}

	function alternarFiltro(chave: 'tipo' | 'tags' | 'ordenar', id: string): void {
		const atual = params[chave];
		navegar({ [chave]: atual === id ? undefined : id, page: 1 });
	}

	function limparFiltro(chave: 'tipo' | 'tags' | 'ordenar'): void {
		navegar({ [chave]: undefined, page: 1 });
	}

	function limparTodos(): void {
		navegar({ search: undefined, tipo: undefined, tags: undefined, ordenar: undefined, page: 1 });
	}

	function onPage(page: number): void {
		navegar({ page });
	}

	function onPageSize(size: number): void {
		navegar({ pageSize: size === 10 ? undefined : size, page: 1 });
	}

	function tentarNovamente(): void {
		void goto(`/vendas/clientes${window.location.search}`, { invalidateAll: true });
	}

	function abrirCliente(id: string): void {
		void goto(`/vendas/clientes/${id}`);
	}

	// ---- Seleção + bulk ----

	let selecionados = $state<string[]>([]);
	const algumSelecionado = $derived(selecionados.length > 0);
	const todosIds = $derived(clientes.map((c) => c.id));
	const todosSelecionados = $derived(
		clientes.length > 0 && selecionados.length === clientes.length
	);
	const parcial = $derived(selecionados.length > 0 && !todosSelecionados);

	function alternarTodos(marcar: boolean): void {
		selecionados = marcar ? [...todosIds] : [];
	}

	function alternarUm(id: string, marcado: boolean): void {
		selecionados = marcado ? [...selecionados, id] : selecionados.filter((s) => s !== id);
	}

	function limparSelecao(): void {
		selecionados = [];
	}

	// Volta à última página válida quando a página atual esvazia (ex.: após excluir).
	$effect(() => {
		if (resultado && resultado.clientes.length === 0 && params.page > 1) {
			navegar({ page: params.page - 1 });
		}
	});

	// Limpa seleção ao trocar de página/filtros.
	$effect(() => {
		void params.page;
		void params.search;
		void params.tipo;
		void params.tags;
		void params.ordenar;
		selecionados = [];
	});

	// ---- Excluir (DELETE /api/vendas/clientes/{id} 🔴) ----

	let confirmarExclusao = $state(false);
	let alvoExclusao = $state<string | null>(null);
	let ocupado = $state(false);

	function pedirExcluir(id: string): void {
		alvoExclusao = id;
		confirmarExclusao = true;
	}

	async function confirmarExcluir(): Promise<void> {
		if (!alvoExclusao) return;
		const alvo = clientes.find((c) => c.id === alvoExclusao);
		if (alvo && !canEditVendas(usuario, { createdBy: alvo.createdBy })) {
			toasts.warn('Sem permissão para esta ação.');
			return;
		}
		ocupado = true;
		try {
			await deleteCliente(alvoExclusao);
			toasts.success('Cliente excluído com sucesso.');
			confirmarExclusao = false;
			alvoExclusao = null;
			selecionados = [];
			await invalidateAll();
		} catch (err) {
			if (err instanceof ApiError && err.status === 403) {
				toasts.warn('Sem permissão para esta ação.');
			} else {
				toasts.danger(toUserMessage(err).message);
			}
		} finally {
			ocupado = false;
		}
	}

	// ---- Bulk: adicionar tag (POST /api/vendas/clientes/bulk-tag 🔴 D-2) ----

	let modalTag = $state(false);
	let tagAlvo = $state('');

	function pedirAdicionarTag(): void {
		tagAlvo = '';
		modalTag = true;
	}

	async function confirmarAdicionarTag(): Promise<void> {
		if (selecionados.length === 0 || !tagAlvo) return;
		const alheio = selecionados.some((id) => {
			const alvo = clientes.find((c) => c.id === id);
			return alvo ? !canEditVendas(usuario, { createdBy: alvo.createdBy }) : true;
		});
		if (alheio) {
			toasts.warn('Somente o criador ou Admin pode etiquetar estes clientes.');
			return;
		}
		ocupado = true;
		try {
			await bulkAdicionarTag(selecionados, tagAlvo);
			toasts.success(
				selecionados.length === 1 ? 'Tag adicionada ao cliente.' : 'Tag adicionada aos clientes.'
			);
			modalTag = false;
			selecionados = [];
			await invalidateAll();
		} catch (err) {
			if (err instanceof ApiError && err.status === 403) {
				toasts.warn('Sem permissão para esta ação.');
			} else {
				toasts.danger(toUserMessage(err).message);
			}
		} finally {
			ocupado = false;
		}
	}

	// ---- Bulk: solicitar alteração (uma solicitação agregada 🟡) ----

	let solicitarBulk = $state(false);

	// ---- Linha: sugerir alteração (modal 🟡) ----

	let sugerirAlvo = $state<Cliente | null>(null);

	function acaoLinha(cliente: Cliente, acao: string): void {
		if (acao === 'ver') abrirCliente(cliente.id);
		else if (acao === 'editar') void goto(`/vendas/clientes/novo?editar=${cliente.id}`);
		else if (acao === 'sugerir') sugerirAlvo = cliente;
		else if (acao === 'duplicar') void goto(`/vendas/clientes/novo?duplicar=${cliente.id}`);
		else if (acao === 'excluir') pedirExcluir(cliente.id);
	}
</script>

<svelte:head>
	<title>Clientes — Vendas — FabLab</title>
</svelte:head>

<div class="space-y-4">
	<PageHeader title="Clientes" subtitle="Cadastre e segmente os clientes do laboratório.">
		{#snippet children()}
			<a
				href="/vendas/clientes/novo"
				class="inline-flex items-center gap-1.5 rounded-md bg-brand px-3.5 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi"
			>
				<Icon name="plus" class="h-4 w-4" /> Novo cliente
			</a>
		{/snippet}
	</PageHeader>

	{#if comErro}
		<ErrorBanner
			message="Não foi possível carregar os clientes"
			hint="Verifique sua conexão e tente novamente. Se persistir, contate o suporte."
			onRetry={tentarNovamente}
			testid="cliente-retry"
		/>
	{:else if carregando}
		<div class="flex flex-wrap items-center gap-2" aria-hidden="true">
			<div class="h-10 w-full min-w-56 max-w-xs animate-pulse rounded-lg bg-elevated sm:flex-1"></div>
			<div class="h-10 w-40 animate-pulse rounded-lg bg-elevated"></div>
			<div class="h-10 w-40 animate-pulse rounded-lg bg-elevated"></div>
			<div class="h-10 w-40 animate-pulse rounded-lg bg-elevated"></div>
		</div>
		<div class="rounded-xl border border-border bg-surface p-4">
			<TableSkeleton rows={6} columns={4} />
		</div>
	{:else}
		<div class="flex flex-wrap items-center gap-2">
			<div class="min-w-56 flex-1">
				<SearchInput
					value={params.search}
					onSearch={onSearch}
					placeholder="Buscar por nome, e-mail ou documento…"
					delay={300}
					label="Buscar clientes"
				/>
			</div>
			<Dropdown
				label="Tipo"
				options={TIPO_OPCOES}
				selected={params.tipo ? [params.tipo] : []}
				onToggle={(id) => alternarFiltro('tipo', id)}
				onClear={() => limparFiltro('tipo')}
				search={false}
			/>
			<Dropdown
				label="Tags"
				options={TAG_OPCOES}
				selected={params.tags ? [params.tags] : []}
				onToggle={(id) => alternarFiltro('tags', id)}
				onClear={() => limparFiltro('tags')}
			/>
			<Dropdown
				label="Ordenar"
				options={ORDENAR_OPCOES}
				selected={params.ordenar ? [params.ordenar] : []}
				onToggle={(id) => alternarFiltro('ordenar', id)}
				onClear={() => limparFiltro('ordenar')}
				search={false}
			/>
		</div>

		{#if temFiltros}
			<div class="flex flex-wrap items-center gap-1.5">
				{#if params.search}
					<Chip label={`Busca: ${params.search}`} onRemove={() => onSearch('')} />
				{/if}
				{#if params.tipo}
					<Chip
						label={TIPO_OPCOES.find((o) => o.id === params.tipo)?.label ?? params.tipo}
						onRemove={() => limparFiltro('tipo')}
					/>
				{/if}
				{#if params.tags}
					<Chip
						label={TAG_OPCOES.find((o) => o.id === params.tags)?.label ?? 'Tag'}
						onRemove={() => limparFiltro('tags')}
					/>
				{/if}
				{#if params.ordenar}
					<Chip
						label={ORDENAR_OPCOES.find((o) => o.id === params.ordenar)?.label ?? params.ordenar}
						onRemove={() => limparFiltro('ordenar')}
					/>
				{/if}
				<button
					type="button"
					onclick={limparTodos}
					class="text-xs font-medium text-brandhi transition-colors hover:text-brand"
				>
					Limpar filtros
				</button>
			</div>
		{/if}

		{#if clientes.length === 0}
			<div class="rounded-xl border border-border bg-surface">
				{#if temFiltros}
					<EmptyState
						icon="search"
						title="Nenhum cliente encontrado"
						description="Nenhum cliente combina com os filtros aplicados."
					>
						{#snippet children()}
							<button
								type="button"
								onclick={limparTodos}
								class="rounded-md border border-border bg-elevated px-3 py-2 text-sm font-medium text-ink transition hover:border-brand/50 hover:text-brandhi"
							>
								Limpar filtros
							</button>
						{/snippet}
					</EmptyState>
				{:else}
					<EmptyState
						icon="users"
						title="Nenhum cliente cadastrado"
						description="Cadastre o primeiro cliente para começar a gerir as vendas do laboratório."
					>
						{#snippet children()}
							<a
								href="/vendas/clientes/novo"
								class="inline-flex items-center gap-1.5 rounded-md bg-brand px-3.5 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi"
							>
								<Icon name="plus" class="h-4 w-4" /> Novo cliente
							</a>
						{/snippet}
					</EmptyState>
				{/if}
			</div>
		{:else}
			<!-- Tabela desktop -->
			<div class="hidden overflow-hidden rounded-xl border border-border bg-surface md:block">
				<div class="overflow-x-auto">
					<table class="w-full min-w-[760px] text-sm">
						<thead>
							<tr class="border-b border-border bg-elevated/50 text-left text-[11px] uppercase tracking-wide text-muted">
								<th class="w-10 px-4 py-2.5 font-medium">
									<input
										type="checkbox"
										data-testid="select-all"
										checked={todosSelecionados}
										indeterminate={parcial}
										onchange={(e) => alternarTodos((e.currentTarget as HTMLInputElement).checked)}
										aria-label="Selecionar todos os clientes da página"
										class="h-4 w-4 rounded border-border bg-surface accent-brand"
									/>
								</th>
								<th class="px-4 py-2.5 font-medium">Cliente</th>
								<th class="px-4 py-2.5 font-medium">Código</th>
								<th class="px-4 py-2.5 font-medium">Tags</th>
								<th class="px-4 py-2.5 font-medium">Tipo</th>
								<th class="w-12 px-4 py-2.5 text-right font-medium">
									<span class="sr-only">Ações</span>
								</th>
							</tr>
						</thead>
						<tbody>
							{#each clientes as cliente (cliente.id)}
								{@const sel = selecionados.includes(cliente.id)}
								{@const podeEditar = canEditVendas(usuario, { createdBy: cliente.createdBy })}
								<tr
									data-testid="cliente-row"
									onclick={() => abrirCliente(cliente.id)}
									class="cursor-pointer border-b border-border transition last:border-0 hover:bg-elevated/40 {sel
										? 'bg-brand/5'
										: ''}"
								>
									<td class="px-4 py-3" onclick={(e) => e.stopPropagation()}>
										<input
											type="checkbox"
											data-testid="cliente-checkbox"
											checked={sel}
											onchange={(e) =>
												alternarUm(cliente.id, (e.currentTarget as HTMLInputElement).checked)}
											aria-label={`Selecionar ${cliente.nome}`}
											class="h-4 w-4 rounded border-border bg-surface accent-brand"
										/>
									</td>
									<td class="px-4 py-3">
										<div class="flex min-w-0 items-center gap-3">
											<Avatar name={cliente.nome} size="sm" tone="brand" />
											<div class="min-w-0">
												<p class="truncate font-medium text-ink">{cliente.nome}</p>
												<p class="truncate text-xs text-muted">{cliente.email}</p>
											</div>
										</div>
									</td>
									<td class="px-4 py-3 font-mono text-xs text-muted">{cliente.codigo}</td>
									<td class="px-4 py-3">
										<div class="flex max-w-56 flex-wrap gap-1">
											{#each cliente.tags as tag (tag.id)}
												<TypeBadge tone={tagTone(tag.cor)} label={tag.nome} />
											{:else}
												<span class="text-xs text-muted">—</span>
											{/each}
										</div>
									</td>
									<td class="px-4 py-3">
										<TypeBadge
											tone={cliente.tipoPessoa === 'pf' ? 'success' : 'brand'}
											label={tipoLabel(cliente.tipoPessoa)}
										/>
									</td>
									<td class="px-4 py-3 text-right" onclick={(e) => e.stopPropagation()}>
										<RowActions
											label={`Ações de ${cliente.nome}`}
											actions={[
												{ id: 'ver', label: 'Ver', icon: 'eye' },
												{ id: 'editar', label: 'Editar', icon: 'pencil', hidden: !podeEditar },
												{
													id: 'sugerir',
													label: 'Sugerir alteração',
													icon: 'document',
													hidden: podeEditar
												},
												{ id: 'duplicar', label: 'Duplicar', icon: 'duplicate' },
												{
													id: 'excluir',
													label: 'Excluir',
													icon: 'trash',
													tone: 'danger',
													hidden: !podeEditar
												}
											]}
											onSelect={(id) => acaoLinha(cliente, id)}
										/>
									</td>
								</tr>
							{/each}
						</tbody>
					</table>
				</div>
				{#if paginacao}
					<div class="border-t border-border px-4 py-3">
						<Pagination
							page={paginacao.page}
							totalPages={paginacao.totalPages}
							totalItems={paginacao.totalItems}
							pageSize={params.pageSize}
							onPage={onPage}
							onPageSize={onPageSize}
							label="clientes"
						/>
					</div>
				{/if}
			</div>

			<!-- Cards mobile -->
			<div class="space-y-2 md:hidden">
				{#each clientes as cliente (cliente.id)}
					{@const podeEditar = canEditVendas(usuario, { createdBy: cliente.createdBy })}
					<article data-testid="cliente-row" class="rounded-xl border border-border bg-surface p-4">
						<div class="flex items-start gap-3">
							<input
								type="checkbox"
								data-testid="cliente-checkbox"
								checked={selecionados.includes(cliente.id)}
								onchange={(e) =>
									alternarUm(cliente.id, (e.currentTarget as HTMLInputElement).checked)}
								aria-label={`Selecionar ${cliente.nome}`}
								class="mt-1 h-4 w-4 shrink-0 rounded border-border bg-surface accent-brand"
							/>
							<button
								type="button"
								onclick={() => abrirCliente(cliente.id)}
								aria-label={`Ver ${cliente.nome}`}
								class="flex min-w-0 flex-1 items-start gap-3 text-left"
							>
								<Avatar name={cliente.nome} size="sm" tone="brand" />
								<span class="min-w-0 flex-1">
									<span class="block truncate text-sm font-medium text-ink">{cliente.nome}</span>
									<span class="block truncate text-xs text-muted">{cliente.email}</span>
									<span class="mt-0.5 block font-mono text-xs text-muted">{cliente.codigo}</span>
									<span class="mt-2 flex flex-wrap items-center gap-1.5">
										{#each cliente.tags as tag (tag.id)}
											<TypeBadge tone={tagTone(tag.cor)} label={tag.nome} />
										{/each}
										<TypeBadge
											tone={cliente.tipoPessoa === 'pf' ? 'success' : 'brand'}
											label={tipoLabel(cliente.tipoPessoa)}
										/>
									</span>
								</span>
							</button>
							<RowActions
								label={`Ações de ${cliente.nome}`}
								actions={[
									{ id: 'ver', label: 'Ver', icon: 'eye' },
									{ id: 'editar', label: 'Editar', icon: 'pencil', hidden: !podeEditar },
									{
										id: 'sugerir',
										label: 'Sugerir alteração',
										icon: 'document',
										hidden: podeEditar
									},
									{ id: 'duplicar', label: 'Duplicar', icon: 'duplicate' },
									{
										id: 'excluir',
										label: 'Excluir',
										icon: 'trash',
										tone: 'danger',
										hidden: !podeEditar
									}
								]}
								onSelect={(id) => acaoLinha(cliente, id)}
							/>
						</div>
					</article>
				{/each}
				{#if paginacao}
					<div class="rounded-xl border border-border bg-surface px-4 py-3">
						<Pagination
							page={paginacao.page}
							totalPages={paginacao.totalPages}
							totalItems={paginacao.totalItems}
							pageSize={params.pageSize}
							onPage={onPage}
							onPageSize={onPageSize}
							label="clientes"
						/>
					</div>
				{/if}
			</div>
		{/if}
	{/if}
</div>

{#if algumSelecionado}
	<div data-testid="bulk-bar">
		<BulkActionsBar
			count={selecionados.length}
			onCancel={limparSelecao}
			actions={[
				{ label: 'Adicionar tag', onClick: pedirAdicionarTag },
				{ label: 'Solicitar alteração', onClick: () => (solicitarBulk = true) }
			]}
		/>
	</div>
{/if}

<ConfirmDialog
	open={confirmarExclusao}
	title="Excluir cliente"
	message="Tem certeza que deseja excluir este cliente? Esta ação não pode ser desfeita."
	confirmLabel="Excluir"
	loading={ocupado}
	onCancel={() => {
		confirmarExclusao = false;
		alvoExclusao = null;
	}}
	onConfirm={() => void confirmarExcluir()}
/>

<Modal
	open={modalTag}
	title="Adicionar tag"
	subtitle={`${selecionados.length} ${selecionados.length === 1 ? 'cliente selecionado' : 'clientes selecionados'}`}
	onClose={() => (modalTag = false)}
	width="sm"
>
	{#snippet children()}
		<Select
			id="bulk-tag"
			label="Tag"
			options={TAG_OPCOES}
			value={tagAlvo}
			onChange={(v) => (tagAlvo = v)}
			required
			hint={TAG_OPCOES.length === 0 ? 'Nenhuma tag cadastrada. Crie tags na tela de novo cliente.' : ''}
		/>
	{/snippet}
	{#snippet footer()}
		<button
			type="button"
			onclick={() => (modalTag = false)}
			disabled={ocupado}
			class="rounded-md border border-border bg-surface px-4 py-2 text-sm font-medium text-ink transition hover:bg-elevated disabled:opacity-50"
		>
			Cancelar
		</button>
		<button
			type="button"
			onclick={() => void confirmarAdicionarTag()}
			disabled={ocupado || !tagAlvo}
			class="rounded-md bg-brand px-4 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi disabled:opacity-50"
		>
			{ocupado ? 'Aplicando…' : 'Aplicar'}
		</button>
	{/snippet}
</Modal>

{#if solicitarBulk}
	<SolicitarEdicaoModal
		alvo={{
			tipo: 'CLI',
			id: selecionados.join(','),
			nome: `${selecionados.length} ${selecionados.length === 1 ? 'cliente selecionado' : 'clientes selecionados'}`
		}}
		onClose={() => (solicitarBulk = false)}
		onSent={limparSelecao}
	/>
{/if}

{#if sugerirAlvo}
	<SolicitarEdicaoModal
		alvo={{ tipo: 'CLI', id: sugerirAlvo.id, nome: sugerirAlvo.nome }}
		onClose={() => (sugerirAlvo = null)}
	/>
{/if}
