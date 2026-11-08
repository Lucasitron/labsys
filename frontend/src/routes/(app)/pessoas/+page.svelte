<script lang="ts">
	import { goto, invalidateAll } from '$app/navigation';
	import type { PageProps } from './$types';
	import type { Nivel, Person } from '$lib/types/rh';
	import { deletePessoa, updatePessoa } from '$lib/api/rh/pessoas';
	import { auth } from '$lib/stores/auth';
	import { get } from 'svelte/store';
	import { isAdmin } from '$lib/utils/permissions';
	import { nivelMeta, personStatusMeta } from '$lib/utils/rh-status';
	import { toUserMessage } from '$lib/utils/errors';
	import { toasts } from '$lib/stores/toast';
	import PageHeader from '$lib/components/ui/PageHeader.svelte';
	import SearchInput from '$lib/components/ui/SearchInput.svelte';
	import Dropdown from '$lib/components/ui/Dropdown.svelte';
	import Chip from '$lib/components/ui/Chip.svelte';
	import Pagination from '$lib/components/ui/Pagination.svelte';
	import StatusBadge from '$lib/components/ui/StatusBadge.svelte';
	import Avatar from '$lib/components/ui/Avatar.svelte';
	import Icon from '$lib/components/ui/Icon.svelte';
	import TableSkeleton from '$lib/components/ui/TableSkeleton.svelte';
	import EmptyState from '$lib/components/ui/EmptyState.svelte';
	import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte';
	import RowActions from '$lib/components/ui/RowActions.svelte';
	import BulkActionsBar from '$lib/components/ui/BulkActionsBar.svelte';
	import ConfirmDialog from '$lib/components/ui/ConfirmDialog.svelte';
	import Modal from '$lib/components/ui/Modal.svelte';
	import Select from '$lib/components/ui/Select.svelte';

	let { data }: PageProps = $props();

	const canEdit = $derived(data.canEdit ?? false);
	const usuario = $derived(get(auth).user);
	const params = $derived(data.params);
	const resultado = $derived(data.resultado);
	const erro = $derived(data.error);

	const pessoas = $derived(resultado?.people ?? []);
	const paginacao = $derived(resultado?.pagination);

	const carregando = $derived(resultado === null && erro === null);
	const comErro = $derived(erro !== null && resultado === null);

	const temFiltros = $derived(
		params.search !== '' || params.setor !== '' || params.nivel !== '' || params.status !== ''
	);

	const STATUS_OPCOES = ['ativo', 'inativo', 'afastado'].map((id) => ({
		id,
		label: personStatusMeta(id as 'ativo' | 'inativo' | 'afastado').label
	}));

	const NIVEL_OPCOES = $derived(
		(['admin', 'bolsista', 'voluntario', 'estagiario', 'recrutando'] as Nivel[])
			.map((id) => ({ id, label: nivelMeta(id).label }))
			.filter((n) => n.id !== 'admin' || isAdmin(usuario))
	);

	const SETOR_OPCOES = ['Administrativo', 'Eletrônica', 'Software', 'Mecatrônica', 'Design'].map(
		(id) => ({ id, label: id })
	);

	function nivelLabel(nivel: Nivel): string {
		return nivelMeta(nivel).label;
	}

	interface Query {
		[key: string]: string | number | undefined;
	}

	function navegar(overrides: Query): void {
		const base: Query = {
			search: params.search || undefined,
			setor: params.setor || undefined,
			nivel: params.nivel || undefined,
			status: params.status || undefined,
			page: params.page,
			pageSize: params.pageSize === 10 ? undefined : params.pageSize
		};
		const merged = { ...base, ...overrides };
		const url = new URLSearchParams();
		for (const [chave, valor] of Object.entries(merged)) {
			if (valor !== undefined && valor !== '') url.append(chave, String(valor));
		}
		const qs = url.toString();
		void goto(`/pessoas${qs ? `?${qs}` : ''}`);
	}

	function onSearch(termo: string): void {
		navegar({ search: termo || undefined, page: 1 });
	}

	function alternarFiltro(chave: 'setor' | 'nivel' | 'status', id: string): void {
		const atual = params[chave];
		navegar({ [chave]: atual === id ? undefined : id, page: 1 });
	}

	function limparFiltro(chave: 'setor' | 'nivel' | 'status'): void {
		navegar({ [chave]: undefined, page: 1 });
	}

	function limparTodos(): void {
		navegar({ search: undefined, setor: undefined, nivel: undefined, status: undefined, page: 1 });
	}

	function onPage(page: number): void {
		navegar({ page });
	}

	function onPageSize(size: number): void {
		navegar({ pageSize: size === 10 ? undefined : size, page: 1 });
	}

	function tentarNovamente(): void {
		void goto(`/pessoas${window.location.search}`, { invalidateAll: true });
	}

	function abrirPessoa(id: string): void {
		void goto(`/pessoas/${id}`);
	}

	// ---- Seleção + bulk ----

	let selecionados = $state<string[]>([]);
	const algumSelecionado = $derived(selecionados.length > 0);
	const todosIds = $derived(pessoas.map((p) => p.id));
	const todosSelecionados = $derived(
		pessoas.length > 0 && selecionados.length === pessoas.length
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
		if (resultado && resultado.people.length === 0 && params.page > 1) {
			navegar({ page: params.page - 1 });
		}
	});

	// Limpa seleção ao trocar de página/filtros.
	$effect(() => {
		void params.page;
		void params.search;
		void params.setor;
		void params.nivel;
		void params.status;
		selecionados = [];
	});

	// ---- Excluir (contrato assumido 🟡: DELETE /api/rh/pessoas/{id}) ----

	let confirmarExclusao = $state(false);
	let alvoExclusao = $state<string[]>([]);
	let ocupado = $state(false);

	function pedirExcluir(ids: string[]): void {
		alvoExclusao = ids;
		confirmarExclusao = true;
	}

	async function excluirPessoa(id: string): Promise<void> {
		await deletePessoa(id);
	}

	async function confirmarExcluir(): Promise<void> {
		if (alvoExclusao.length === 0) return;
		ocupado = true;
		try {
			await Promise.all(alvoExclusao.map((id) => excluirPessoa(id)));
			toasts.success(
				alvoExclusao.length === 1
					? 'Pessoa excluída com sucesso.'
					: `${alvoExclusao.length} pessoas excluídas com sucesso.`
			);
			confirmarExclusao = false;
			selecionados = [];
			alvoExclusao = [];
			await invalidateAll();
		} catch (err) {
			toasts.danger(toUserMessage(err).message);
		} finally {
			ocupado = false;
		}
	}

	// ---- Alterar nível em massa (PUT /api/rh/pessoas/{id} 🟡) ----

	let modalNivel = $state(false);
	let nivelAlvo = $state<Nivel>('voluntario');

	function pedirAlterarNivel(): void {
		nivelAlvo = 'voluntario';
		modalNivel = true;
	}

	async function confirmarAlterarNivel(): Promise<void> {
		if (selecionados.length === 0) return;
		ocupado = true;
		try {
			await Promise.all(selecionados.map((id) => updatePessoa(id, { nivel: nivelAlvo })));
			toasts.success(`Nível alterado para ${nivelLabel(nivelAlvo)}.`);
			modalNivel = false;
			selecionados = [];
			await invalidateAll();
		} catch (err) {
			toasts.danger(toUserMessage(err).message);
		} finally {
			ocupado = false;
		}
	}

	function acaoLinha(pessoa: Person, acao: string): void {
		if (acao === 'ver') abrirPessoa(pessoa.id);
		else if (acao === 'editar') void goto(`/pessoas/${pessoa.id}/editar`);
		else if (acao === 'excluir' && canEdit) pedirExcluir([pessoa.id]);
	}

	function formatarData(iso: string | undefined): string {
		if (!iso) return '—';
		const data = new Date(iso);
		if (Number.isNaN(data.getTime())) return '—';
		return new Intl.DateTimeFormat('pt-BR', {
			day: '2-digit',
			month: '2-digit',
			year: 'numeric'
		}).format(data);
	}
</script>

<svelte:head>
	<title>Pessoas — FabLab</title>
</svelte:head>

<div class="space-y-4">
	<PageHeader title="Pessoas" subtitle="Gerencie equipe, candidatos e contatos do laboratório.">
		{#snippet children()}
			{#if canEdit}
				<a
					href="/pessoas/novo"
					class="inline-flex items-center gap-1.5 rounded-md bg-brand px-3.5 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi"
				>
					<Icon name="plus" class="h-4 w-4" /> Novo cadastro
				</a>
			{/if}
		{/snippet}
	</PageHeader>

	{#if comErro}
		<ErrorBanner
			message="Não foi possível carregar as pessoas"
			hint="Verifique sua conexão e tente novamente. Se persistir, contate o suporte."
			onRetry={tentarNovamente}
			testid="person-retry"
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
					placeholder="Buscar por nome, matrícula ou e-mail…"
					delay={300}
					label="Buscar pessoas"
				/>
			</div>
			<Dropdown
				label="Status"
				options={STATUS_OPCOES}
				selected={params.status ? [params.status] : []}
				onToggle={(id) => alternarFiltro('status', id)}
				onClear={() => limparFiltro('status')}
				search={false}
			/>
			<Dropdown
				label="Setor"
				options={SETOR_OPCOES}
				selected={params.setor ? [params.setor] : []}
				onToggle={(id) => alternarFiltro('setor', id)}
				onClear={() => limparFiltro('setor')}
				search={false}
			/>
			<Dropdown
				label="Nível"
				options={NIVEL_OPCOES}
				selected={params.nivel ? [params.nivel] : []}
				onToggle={(id) => alternarFiltro('nivel', id)}
				onClear={() => limparFiltro('nivel')}
				search={false}
			/>
		</div>

		{#if temFiltros}
			<div class="flex flex-wrap items-center gap-1.5">
				{#if params.search}
					<Chip label={`Busca: ${params.search}`} onRemove={() => onSearch('')} />
				{/if}
				{#if params.status}
					<Chip
						label={STATUS_OPCOES.find((o) => o.id === params.status)?.label ?? params.status}
						onRemove={() => limparFiltro('status')}
					/>
				{/if}
				{#if params.setor}
					<Chip label={params.setor} onRemove={() => limparFiltro('setor')} />
				{/if}
				{#if params.nivel}
					<Chip
						label={NIVEL_OPCOES.find((o) => o.id === params.nivel)?.label ?? params.nivel}
						onRemove={() => limparFiltro('nivel')}
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

		{#if pessoas.length === 0}
			<div class="rounded-xl border border-border bg-surface">
				{#if temFiltros}
					<EmptyState
						icon="search"
						title="Nenhuma pessoa encontrada"
						description="Nenhuma pessoa combina com os filtros aplicados."
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
						title="Nenhuma pessoa cadastrada"
						description="Cadastre a primeira pessoa para começar a gerir a equipe do laboratório."
					>
						{#snippet children()}
							{#if canEdit}
								<a
									href="/pessoas/novo"
									class="inline-flex items-center gap-1.5 rounded-md bg-brand px-3.5 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi"
								>
									<Icon name="plus" class="h-4 w-4" /> Novo cadastro
								</a>
							{/if}
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
								{#if canEdit}
									<th class="w-10 px-4 py-2.5 font-medium">
										<input
											type="checkbox"
											data-testid="select-all"
											checked={todosSelecionados}
											indeterminate={parcial}
											onchange={(e) => alternarTodos((e.currentTarget as HTMLInputElement).checked)}
											aria-label="Selecionar todas as pessoas da página"
											class="h-4 w-4 rounded border-border bg-surface accent-brand"
										/>
									</th>
								{/if}
								<th class="px-4 py-2.5 font-medium">Pessoa</th>
								<th class="px-4 py-2.5 font-medium">Matrícula</th>
								<th class="px-4 py-2.5 font-medium">Nível</th>
								<th class="px-4 py-2.5 font-medium">Grupo</th>
								<th class="px-4 py-2.5 font-medium">Status</th>
								{#if canEdit}
									<th class="w-12 px-4 py-2.5 text-right font-medium">
										<span class="sr-only">Ações</span>
									</th>
								{/if}
							</tr>
						</thead>
						<tbody>
							{#each pessoas as pessoa (pessoa.id)}
								{@const sel = selecionados.includes(pessoa.id)}
								{@const st = personStatusMeta(pessoa.status)}
								{@const nv = nivelMeta(pessoa.nivel)}
								<tr
									data-testid="person-row"
									onclick={() => abrirPessoa(pessoa.id)}
									class="cursor-pointer border-b border-border transition last:border-0 hover:bg-elevated/40 {sel
										? 'bg-brand/5'
										: ''}"
								>
									{#if canEdit}
										<td class="px-4 py-3" onclick={(e) => e.stopPropagation()}>
											<input
												type="checkbox"
												data-testid="person-checkbox"
												checked={sel}
												onchange={(e) =>
													alternarUm(pessoa.id, (e.currentTarget as HTMLInputElement).checked)}
												aria-label={`Selecionar ${pessoa.name}`}
												class="h-4 w-4 rounded border-border bg-surface accent-brand"
											/>
										</td>
									{/if}
									<td class="px-4 py-3">
										<div class="flex min-w-0 items-center gap-3">
											<Avatar name={pessoa.name} initialsOverride={pessoa.initials} size="sm" tone="brand" />
											<div class="min-w-0">
												<p class="truncate font-medium text-ink">{pessoa.name}</p>
												<p class="truncate text-xs text-muted">{pessoa.email}</p>
											</div>
										</div>
									</td>
									<td class="px-4 py-3 font-mono text-xs text-muted">{pessoa.matricula}</td>
									<td class="px-4 py-3">
										<StatusBadge label={nv.label} color={nv.color} />
									</td>
									<td class="px-4 py-3 text-xs text-muted">{pessoa.group?.label ?? '—'}</td>
									<td class="px-4 py-3">
										<StatusBadge label={st.label} color={st.color} />
									</td>
									{#if canEdit}
										<td class="px-4 py-3 text-right" onclick={(e) => e.stopPropagation()}>
											<RowActions
												label={`Ações de ${pessoa.name}`}
												actions={[
													{ id: 'ver', label: 'Ver', icon: 'eye' },
													{ id: 'editar', label: 'Editar', icon: 'pencil' },
													{ id: 'excluir', label: 'Excluir', icon: 'trash', tone: 'danger' }
												]}
												onSelect={(id) => acaoLinha(pessoa, id)}
											/>
										</td>
									{/if}
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
							label="pessoas"
						/>
					</div>
				{/if}
			</div>

			<!-- Cards mobile -->
			<div class="space-y-2 md:hidden">
				{#each pessoas as pessoa (pessoa.id)}
					{@const st = personStatusMeta(pessoa.status)}
					{@const nv = nivelMeta(pessoa.nivel)}
					<article
						data-testid="person-row"
						class="rounded-xl border border-border bg-surface p-4"
					>
						<div class="flex items-start gap-3">
							{#if canEdit}
								<input
									type="checkbox"
									data-testid="person-checkbox"
									checked={selecionados.includes(pessoa.id)}
									onchange={(e) =>
										alternarUm(pessoa.id, (e.currentTarget as HTMLInputElement).checked)}
									aria-label={`Selecionar ${pessoa.name}`}
									class="mt-1 h-4 w-4 shrink-0 rounded border-border bg-surface accent-brand"
								/>
							{/if}
							<button
								type="button"
								onclick={() => abrirPessoa(pessoa.id)}
								aria-label={`Ver ${pessoa.name}`}
								class="flex min-w-0 flex-1 items-start gap-3 text-left"
							>
								<Avatar name={pessoa.name} initialsOverride={pessoa.initials} size="sm" tone="brand" />
								<span class="min-w-0 flex-1">
									<span class="block truncate text-sm font-medium text-ink">{pessoa.name}</span>
									<span class="block truncate text-xs text-muted">{pessoa.email}</span>
									<span class="mt-0.5 block font-mono text-xs text-muted">{pessoa.matricula}</span>
									<span class="mt-2 flex flex-wrap items-center gap-1.5">
										<StatusBadge label={nv.label} color={nv.color} />
										<StatusBadge label={st.label} color={st.color} />
									</span>
									<span class="mt-1.5 block text-xs text-muted">
										{pessoa.group?.label ?? 'Sem grupo'} · desde {formatarData(pessoa.joinedAt)}
									</span>
								</span>
							</button>
							{#if canEdit}
								<RowActions
									label={`Ações de ${pessoa.name}`}
									actions={[
										{ id: 'ver', label: 'Ver', icon: 'eye' },
										{ id: 'editar', label: 'Editar', icon: 'pencil' },
										{ id: 'excluir', label: 'Excluir', icon: 'trash', tone: 'danger' }
									]}
									onSelect={(id) => acaoLinha(pessoa, id)}
								/>
							{/if}
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
							label="pessoas"
						/>
					</div>
				{/if}
			</div>
		{/if}
	{/if}
</div>

{#if canEdit && algumSelecionado}
	<div data-testid="bulk-bar">
		<BulkActionsBar
			count={selecionados.length}
			onCancel={limparSelecao}
			actions={[
				{ label: 'Alterar nível', onClick: pedirAlterarNivel },
				{ label: 'Excluir', onClick: () => pedirExcluir(selecionados), danger: true }
			]}
		/>
	</div>
{/if}

<ConfirmDialog
	open={confirmarExclusao}
	title={alvoExclusao.length === 1 ? 'Excluir pessoa' : `Excluir ${alvoExclusao.length} pessoas`}
	message={alvoExclusao.length === 1
		? 'Tem certeza que deseja excluir esta pessoa? Esta ação não pode ser desfeita.'
		: `Tem certeza que deseja excluir ${alvoExclusao.length} pessoas? Esta ação não pode ser desfeita.`}
	confirmLabel="Excluir"
	loading={ocupado}
	onCancel={() => {
		confirmarExclusao = false;
		alvoExclusao = [];
	}}
	onConfirm={() => void confirmarExcluir()}
/>

<Modal
	open={modalNivel}
	title="Alterar nível"
	subtitle={`${selecionados.length} ${selecionados.length === 1 ? 'pessoa selecionada' : 'pessoas selecionadas'}`}
	onClose={() => (modalNivel = false)}
	width="sm"
>
	{#snippet children()}
		<Select
			id="bulk-nivel"
			label="Novo nível"
			options={NIVEL_OPCOES}
			value={nivelAlvo}
			onChange={(v) => (nivelAlvo = v as Nivel)}
			required
		/>
	{/snippet}
	{#snippet footer()}
		<button
		type="button"
			onclick={() => (modalNivel = false)}
			disabled={ocupado}
			class="rounded-md border border-border bg-surface px-4 py-2 text-sm font-medium text-ink transition hover:bg-elevated disabled:opacity-50"
		>
			Cancelar
		</button>
		<button
			type="button"
			onclick={() => void confirmarAlterarNivel()}
			disabled={ocupado}
			class="rounded-md bg-brand px-4 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi disabled:opacity-50"
		>
			{ocupado ? 'Salvando…' : 'Salvar'}
		</button>
	{/snippet}
</Modal>
