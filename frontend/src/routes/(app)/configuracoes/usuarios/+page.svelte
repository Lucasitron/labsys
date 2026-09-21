<script lang="ts">
	import { goto, invalidateAll } from '$app/navigation';
	import type { PageProps } from './$types';
	import type { Nivel, SituacaoUsuario, Usuario } from '$lib/types/configuracoes';
	import type { Tone } from '$lib/types/stock';
	import {
		atualizarUsuario,
		alterarStatus,
		criarUsuario,
		type UsuarioPayload
	} from '$lib/api/configuracoes/usuarios';
	import { toasts, toastError } from '$lib/stores/toast';
	import PageHeader from '$lib/components/ui/PageHeader.svelte';
	import SearchInput from '$lib/components/ui/SearchInput.svelte';
	import Dropdown from '$lib/components/ui/Dropdown.svelte';
	import Chip from '$lib/components/ui/Chip.svelte';
	import Pagination from '$lib/components/ui/Pagination.svelte';
	import StatusBadge from '$lib/components/ui/StatusBadge.svelte';
	import Avatar from '$lib/components/ui/Avatar.svelte';
	import Icon from '$lib/components/ui/Icon.svelte';
	import ModalUsuario from '$lib/components/configuracoes/ModalUsuario.svelte';
	import ModalConfirm from '$lib/components/configuracoes/ModalConfirm.svelte';

	let { data }: PageProps = $props();

	const params = $derived(data.params);
	const resultado = $derived(data.resultado);
	const error = $derived(data.error);

	const usuarios = $derived(resultado?.dados ?? []);
	const paginacao = $derived(resultado?.paginacao);

	const loading = $derived(resultado === null && error === null);
	const hasError = $derived(error !== null && resultado === null);

	const hasFilters = $derived(
		params.search !== '' || params.nivel !== '' || params.situacao !== ''
	);

	const NIVEL_LABELS: Record<Nivel, string> = {
		0: 'Admin',
		1: 'Bolsista',
		2: 'Voluntário',
		3: 'Estagiário',
		4: 'Recrutando'
	};

	const NIVEL_TONES: Record<Nivel, Tone> = {
		0: 'brand',
		1: 'muted',
		2: 'muted',
		3: 'warn',
		4: 'danger'
	};

	const SITUACAO_TONES: Record<SituacaoUsuario, Tone> = {
		Ativo: 'success',
		Pendente: 'warn',
		Desativado: 'muted'
	};

	const nivelOptions = (Object.keys(NIVEL_LABELS) as unknown as Nivel[]).map((n) => ({
		id: String(n),
		label: NIVEL_LABELS[n]
	}));

	const situacaoOptions = (
		Object.keys(SITUACAO_TONES) as SituacaoUsuario[]
	).map((s) => ({ id: s, label: s }));

	function formatarData(iso: string | null | undefined): string {
		if (!iso) return '—';
		const data = new Date(iso);
		if (Number.isNaN(data.getTime())) return '—';
		return new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' }).format(data);
	}

	interface Query {
		[key: string]: string | number | undefined;
	}

	function navegar(overrides: Query): string {
		const base: Query = {
			page: params.page,
			search: params.search,
			nivel: params.nivel,
			situacao: params.situacao
		};
		const merged = { ...base, ...overrides };
		const url = new URLSearchParams();
		for (const [chave, valor] of Object.entries(merged)) {
			if (valor !== undefined && valor !== '') url.append(chave, String(valor));
		}
		const qs = url.toString();
		return `/configuracoes/usuarios${qs ? `?${qs}` : ''}`;
	}

	function onSearch(termo: string): void {
		void goto(navegar({ search: termo, page: 1 }));
	}

	function toggleFiltro(chave: 'nivel' | 'situacao', id: string): void {
		const atual = params[chave];
		const proximo = atual === id ? '' : id;
		void goto(navegar({ [chave]: proximo, page: 1 }));
	}

	function limparFiltro(chave: 'nivel' | 'situacao'): void {
		void goto(navegar({ [chave]: '', page: 1 }));
	}

	function limparTodos(): void {
		void goto(navegar({ search: '', nivel: '', situacao: '', page: 1 }));
	}

	function onPage(page: number): void {
		void goto(navegar({ page }));
	}

	// ---- Modais ----

	let modalAberto = $state(false);
	let usuarioAlvo = $state<Usuario | null>(null);
	let confirmar = $state(false);
	let alvoConfirm: Usuario | null = $state(null);
	let busy = $state(false);

	function abrirNovo(): void {
		usuarioAlvo = null;
		modalAberto = true;
	}

	function abrirEditar(usuario: Usuario): void {
		usuarioAlvo = usuario;
		modalAberto = true;
	}

	async function salvarUsuario(payload: UsuarioPayload): Promise<void> {
		busy = true;
		try {
			if (usuarioAlvo) {
				await atualizarUsuario(usuarioAlvo.id, payload);
				toasts.success('Usuário atualizado com sucesso.');
			} else {
				await criarUsuario(payload);
				toasts.success('Convite enviado ao usuário.');
			}
			modalAberto = false;
			usuarioAlvo = null;
		} catch (err) {
			toastError(err, 'Não foi possível salvar o usuário.');
		} finally {
			busy = false;
			await invalidateAll();
		}
	}

	function pedirDesativar(usuario: Usuario): void {
		alvoConfirm = usuario;
		confirmar = true;
	}

	async function confirmarStatus(): Promise<void> {
		if (!alvoConfirm) return;
		busy = true;
		try {
			await alterarStatus(alvoConfirm.id, 'Desativado');
			toasts.success(`${alvoConfirm.nome} foi desativado.`);
			confirmar = false;
			alvoConfirm = null;
		} catch (err) {
			toastError(err, 'Não foi possível alterar a situação do usuário.');
		} finally {
			busy = false;
			await invalidateAll();
		}
	}
</script>

<svelte:head>
	<title>Usuários — Configurações — FabLab</title>
</svelte:head>

<div class="space-y-4">
	<PageHeader
		title="Usuários"
		subtitle="Cadastro, convites e situação dos integrantes do FabLab."
	>
		{#snippet children()}
			<StatusBadge label="Restrito ao Admin" color="warn" />
			<button
				type="button"
				data-testid="cfg-user-new"
				onclick={abrirNovo}
				class="inline-flex items-center gap-1.5 rounded-lg bg-brand px-3.5 py-2 text-sm font-semibold text-white shadow-lg shadow-brand/20 transition-colors hover:bg-brandhi"
			>
				<Icon name="plus" class="h-4 w-4" /> Novo usuário
			</button>
		{/snippet}
	</PageHeader>

	{#if hasError}
		<div
			role="alert"
			data-testid="cfg-users-error"
			class="flex items-center gap-4 rounded-xl border border-border bg-surface p-5"
		>
			<span class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl border border-danger/30 bg-danger/15">
				<Icon name="warning" class="h-5 w-5 text-danger" />
			</span>
			<div class="min-w-0 flex-1">
				<h2 class="text-sm font-medium text-ink">Não foi possível carregar os usuários</h2>
				<p class="mt-0.5 text-xs text-muted">
					{error ?? 'Verifique sua conexão e tente novamente.'}
				</p>
			</div>
			<button
				type="button"
				data-testid="cfg-users-retry"
				onclick={() => void goto('/configuracoes/usuarios', { invalidateAll: true })}
				class="flex shrink-0 items-center gap-2 rounded-md border border-danger/30 bg-danger/15 px-3 py-2 text-xs font-medium text-danger transition hover:bg-danger/25"
			>
				Tentar novamente
			</button>
		</div>
	{:else if loading}
		<div class="flex flex-wrap items-center gap-2">
			<div class="h-10 w-full min-w-56 max-w-xs animate-pulse rounded-lg border border-border bg-elevated/60 sm:flex-1"></div>
			<div class="h-10 w-40 animate-pulse rounded-lg border border-border bg-elevated/60"></div>
			<div class="h-10 w-40 animate-pulse rounded-lg border border-border bg-elevated/60"></div>
		</div>
		<div data-testid="cfg-users-skeleton" class="overflow-hidden rounded-xl border border-border bg-surface">
			<div class="border-b border-border px-4 py-3">
				<div class="h-4 w-40 animate-pulse rounded bg-elevated"></div>
			</div>
			{#each [0, 1, 2, 3, 4] as i (i)}
				<div class="flex items-center gap-3 border-b border-border px-4 py-3">
					<div class="h-8 w-8 animate-pulse rounded-full bg-elevated"></div>
					<div class="flex-1 space-y-1.5">
						<div class="h-3 w-44 animate-pulse rounded bg-elevated"></div>
						<div class="h-2.5 w-60 animate-pulse rounded bg-elevated/60"></div>
					</div>
					<div class="h-5 w-16 animate-pulse rounded-full bg-elevated"></div>
					<div class="h-5 w-14 animate-pulse rounded-full bg-elevated"></div>
				</div>
			{/each}
		</div>
	{:else}
		<!-- Filtros -->
		<div class="flex flex-wrap items-center gap-2">
			<div data-testid="cfg-user-search" class="min-w-56 flex-1">
				<SearchInput
					value={params.search}
					onSearch={onSearch}
					placeholder="Buscar por nome ou e-mail…"
				/>
			</div>
			<div data-testid="cfg-user-filter-nivel">
				<Dropdown
					label="Nível"
					options={nivelOptions}
					selected={params.nivel ? [params.nivel] : []}
					onToggle={(id) => toggleFiltro('nivel', id)}
					onClear={() => limparFiltro('nivel')}
					search={false}
				/>
			</div>
			<div data-testid="cfg-user-filter-sit">
				<Dropdown
					label="Situação"
					options={situacaoOptions}
					selected={params.situacao ? [params.situacao] : []}
					onToggle={(id) => toggleFiltro('situacao', id)}
					onClear={() => limparFiltro('situacao')}
					search={false}
				/>
			</div>
		</div>

		{#if hasFilters}
			<div class="flex flex-wrap items-center gap-1.5">
				{#if params.nivel}
					<Chip
						label={NIVEL_LABELS[Number(params.nivel) as Nivel] ?? params.nivel}
						onRemove={() => limparFiltro('nivel')}
					/>
				{/if}
				{#if params.situacao}
					<Chip label={params.situacao} onRemove={() => limparFiltro('situacao')} />
				{/if}
				{#if params.search}
					<Chip label={`Busca: ${params.search}`} onRemove={() => onSearch('')} />
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

		{#if usuarios.length === 0}
			<div data-testid="cfg-users-empty" class="rounded-xl border border-border bg-surface">
				{#if hasFilters}
					<div class="flex flex-col items-center justify-center gap-2 px-4 py-14 text-center">
						<span class="flex h-12 w-12 items-center justify-center rounded-full bg-muted/10 text-muted">
							<Icon name="filter" class="h-6 w-6" />
						</span>
						<p class="text-sm font-medium text-ink">Nenhum usuário encontrado</p>
						<p class="max-w-sm text-sm text-muted">
							Nenhum usuário combina com os filtros aplicados.
						</p>
						<button
							type="button"
							onclick={limparTodos}
							class="mt-2 rounded-lg border border-border bg-surface px-3 py-2 text-sm font-medium text-ink transition-colors hover:border-brand/50 hover:text-brandhi"
						>
							Limpar filtros
						</button>
					</div>
				{:else}
					<div class="flex flex-col items-center justify-center gap-2 px-4 py-14 text-center">
						<span class="flex h-12 w-12 items-center justify-center rounded-full bg-muted/10 text-muted">
							<Icon name="users" class="h-6 w-6" />
						</span>
						<p class="text-sm font-medium text-ink">Nenhum usuário cadastrado</p>
						<p class="max-w-sm text-sm text-muted">
							Crie o primeiro usuário para começar a gerir contas e convites.
						</p>
						<button
							type="button"
							onclick={abrirNovo}
							class="mt-2 inline-flex items-center gap-1.5 rounded-lg bg-brand px-3.5 py-2 text-sm font-semibold text-white shadow-lg shadow-brand/20 transition-colors hover:bg-brandhi"
						>
							<Icon name="plus" class="h-4 w-4" /> Novo usuário
						</button>
					</div>
				{/if}
			</div>
		{:else}
			<div class="overflow-hidden rounded-xl border border-border bg-surface">
				<div class="overflow-x-auto">
					<table class="w-full min-w-[720px] text-sm">
						<thead>
							<tr class="border-b border-border text-left text-xs text-muted">
								<th class="px-4 py-3 font-medium">Usuário</th>
								<th class="px-4 py-3 font-medium">Nível</th>
								<th class="px-4 py-3 font-medium">Situação</th>
								<th class="hidden px-4 py-3 font-medium md:table-cell">Criado em</th>
								<th class="px-4 py-3 text-right font-medium">Ações</th>
							</tr>
						</thead>
						<tbody>
							{#each usuarios as usuario (usuario.id)}
								<tr
									data-testid="cfg-user-row"
									class="border-b border-border transition-colors hover:bg-elevated/40"
								>
									<td class="px-4 py-3">
										<div class="flex min-w-0 items-center gap-3">
											<div class="flex h-8 w-8 shrink-0 items-center justify-center rounded-full border border-border bg-elevated/60">
												<Avatar name={usuario.nome} size="xs" tone="muted" />
											</div>
											<div class="min-w-0">
												<p class="truncate font-medium text-ink">{usuario.nome}</p>
												<p class="truncate text-xs text-muted">{usuario.email}</p>
											</div>
										</div>
									</td>
									<td class="px-4 py-3">
										<StatusBadge
											label={NIVEL_LABELS[usuario.nivel] ?? String(usuario.nivel)}
											color={NIVEL_TONES[usuario.nivel] ?? 'muted'}
										/>
									</td>
									<td class="px-4 py-3">
										<StatusBadge label={usuario.situacao} color={SITUACAO_TONES[usuario.situacao]} />
									</td>
									<td class="hidden px-4 py-3 text-muted md:table-cell">
										{formatarData(usuario.criadoEm)}
									</td>
									<td class="px-4 py-3">
										<div class="flex items-center justify-end gap-1">
											<button
												type="button"
												onclick={() => abrirEditar(usuario)}
												class="rounded-md px-2.5 py-1 text-xs font-medium text-muted transition-colors hover:bg-brand/10 hover:text-brandhi"
											>
												Editar
											</button>
											{#if usuario.situacao !== 'Desativado'}
												<button
													type="button"
													onclick={() => pedirDesativar(usuario)}
													class="rounded-md px-2.5 py-1 text-xs font-medium text-muted transition-colors hover:bg-danger/10 hover:text-danger"
												>
													{usuario.situacao === 'Pendente' ? 'Remover' : 'Desativar'}
												</button>
											{/if}
										</div>
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
							onPage={onPage}
							label="usuários"
						/>
					</div>
				{/if}
			</div>
		{/if}
	{/if}
</div>

<ModalUsuario
	open={modalAberto}
	admin={true}
	usuario={usuarioAlvo}
	onClose={() => {
		modalAberto = false;
		usuarioAlvo = null;
	}}
	onSave={(payload) => salvarUsuario(payload)}
/>

<ModalConfirm
	open={confirmar}
	titulo="Confirmar desativação"
	mensagem={alvoConfirm
		? `Tem certeza que deseja ${alvoConfirm.situacao === 'Pendente' ? 'remover' : 'desativar'} ${alvoConfirm.nome}? Esta ação altera a situação da conta.`
		: ''}
	confirmLabel={alvoConfirm?.situacao === 'Pendente' ? 'Remover' : 'Desativar'}
	loading={busy}
	onConfirm={() => confirmarStatus()}
	onClose={() => {
		confirmar = false;
		alvoConfirm = null;
	}}
/>