<script lang="ts">
	import { goto, invalidateAll } from '$app/navigation';
	import type { PageProps } from './$types';
	import type { KanbanColuna, NomeFunc, Projeto, Tarefa, TarefaStatus } from '$lib/types/producao';
	import { KANBAN_COLUNA_META, TAREFA_STATUS_META, GRAVIDADE_META } from '$lib/utils/producao-status';
import {
	atribuirTarefa,
	moverTarefa,
	resolverPendencia5S
} from '$lib/api/producao/client';
import { toastError, toasts } from '$lib/stores/toast';
	import ModalNovaTarefa from '$lib/components/producao/ModalNovaTarefa.svelte';
	import PageHeader from '$lib/components/ui/PageHeader.svelte';
	import SearchInput from '$lib/components/ui/SearchInput.svelte';
	import Dropdown from '$lib/components/ui/Dropdown.svelte';
	import Chip from '$lib/components/ui/Chip.svelte';
	import StatusBadge from '$lib/components/ui/StatusBadge.svelte';
	import Avatar from '$lib/components/ui/Avatar.svelte';
	import Skeleton from '$lib/components/ui/Skeleton.svelte';
	import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte';
	import EmptyState from '$lib/components/ui/EmptyState.svelte';
	import Modal from '$lib/components/ui/Modal.svelte';
	import Select from '$lib/components/ui/Select.svelte';
	import Icon from '$lib/components/ui/Icon.svelte';

	let { data }: PageProps = $props();

	const canEditProducao = $derived(data.canEditProducao ?? false);

	const result = $derived(data.tarefas);
	const error = $derived(data.error);
	const tarefas = $derived(result?.dados ?? []);
	const loading = $derived(result === null && error === null);
	const hasError = $derived(error !== null && result === null);

	const params = $derived(data.params);
	const projetos = $derived(data.projetos as Projeto[]);
	const responsaveis = $derived(data.responsaveis as NomeFunc[]);

	// ---- modelo: Tarefa não expõe coluna Kanban nem checklist ----
	const COLUNA_PARA_STATUS: Record<KanbanColuna, TarefaStatus> = {
		Fila: 'Pendente',
		Produção: 'Em andamento',
		Acabamento: 'Em andamento',
		Pronto: 'Concluída',
		Entregue: 'Concluída'
	};
	const ORDEM_COLUNAS: KanbanColuna[] = ['Fila', 'Produção', 'Acabamento', 'Pronto', 'Entregue'];

	function statusParaColuna(status: TarefaStatus): KanbanColuna {
		if (status === 'Pendente') return 'Fila';
		if (status === 'Concluída') return 'Pronto';
		return 'Produção';
	}

	interface ItemChecklist {
		id: string;
		item: string;
		concluido: boolean;
	}

	interface TarefaDetalhe extends Tarefa {
		checklist?: ItemChecklist[];
		coluna?: string;
	}

	// coluna local em sessão (funciona mesmo sem o backend expor coluna)
	let colunas = $state<Record<string, KanbanColuna>>({});

	function colunaDe(tarefa: Tarefa): KanbanColuna {
		return colunas[tarefa.id] ?? statusParaColuna(tarefa.status);
	}

	async function mover(tarefa: Tarefa, direcao: -1 | 1): Promise<void> {
		if (!canEditProducao) return;
		const atual = colunaDe(tarefa);
		const idx = ORDEM_COLUNAS.indexOf(atual);
		const alvoIdx = idx + direcao;
		if (alvoIdx < 0 || alvoIdx >= ORDEM_COLUNAS.length) return;
		const alvo = ORDEM_COLUNAS[alvoIdx];
		try {
			colunas = { ...colunas, [tarefa.id]: alvo };
			await moverTarefa(tarefa.id, { status: COLUNA_PARA_STATUS[alvo] });
			toasts.success(`Tarefa movida para "${KANBAN_COLUNA_META[alvo].label}"`);
		} catch (err) {
			colunas = { ...colunas, [tarefa.id]: atual };
			toastError(err, 'Não foi possível mover a tarefa');
		}
	}

	const kpiCls = 'rounded-xl border border-border bg-surface p-4';

	const pendentes = $derived(tarefas.filter((t) => t.status === 'Pendente').length);
	const emAndamento = $derived(tarefas.filter((t) => t.status === 'Em andamento').length);
	const atrasadas = $derived(tarefas.filter((t) => t.status === 'Atrasada').length);
	const concluidas = $derived(tarefas.filter((t) => t.status === 'Concluída').length);

	const hasFilters = $derived(
		params.search !== '' ||
			params.status.length > 0 ||
			params.prioridade.length > 0 ||
			params.responsavel.length > 0
	);

	const statusOptions = $derived(
		Object.entries(TAREFA_STATUS_META).map(([id, meta]) => ({ id, label: meta.label }))
	);
	const prioridadeOptions = $derived(
		(['Baixa', 'Média', 'Moderada', 'Alta'] as const).map((id) => ({ id, label: id }))
	);
	const responsavelOptions = $derived(
		responsaveis.map((p) => ({ id: p.id, label: p.nome }))
	);

	const filtrado = $derived(
		tarefas.filter((t) => {
			const termo = params.search.toLowerCase().trim();
			const coincideBusca =
				termo === '' ||
				t.titulo.toLowerCase().includes(termo) ||
				t.codigo.toLowerCase().includes(termo) ||
				t.responsavel.nome.toLowerCase().includes(termo) ||
				(t.projeto?.nome.toLowerCase().includes(termo) ?? false);
			const coincideStatus = params.status.length === 0 || params.status.includes(t.status);
			const coincidePrioridade =
				params.prioridade.length === 0 || params.prioridade.includes(t.prioridade);
			const coincideResp =
				params.responsavel.length === 0 || params.responsavel.includes(t.responsavel.id);
			return coincideBusca && coincideStatus && coincidePrioridade && coincideResp;
		})
	);

	const colunasComTarefas = $derived(
		ORDEM_COLUNAS.map((coluna) => ({
			coluna,
			itens: filtrado.filter((t) => colunaDe(t) === coluna)
		}))
	);

	// ---- modais ----
	let modalNova = $state(false);
	let modalDetalhe = $state<TarefaDetalhe | null>(null);

	// ---- atribuir responsável ----
	let novoResponsavelId = $state('');
	$effect(() => {
		if (modalDetalhe) novoResponsavelId = modalDetalhe.responsavel.id;
	});

	async function atribuir(): Promise<void> {
		if (!modalDetalhe || !novoResponsavelId) return;
		try {
			await atribuirTarefa(modalDetalhe.id, { responsavelId: novoResponsavelId });
			toasts.success('Responsável atualizado');
			modalDetalhe = null;
			await invalidateAll();
		} catch (err) {
			toastError(err, 'Não foi possível atribuir a tarefa');
		}
	}

	// ---- resolver pendência ----
	async function resolverPendencia(): Promise<void> {
		if (!modalDetalhe?.pendencia5s) return;
		try {
			await resolverPendencia5S(modalDetalhe.pendencia5s.id, {});
			toasts.success('Pendência 5S resolvida');
			modalDetalhe = null;
			await invalidateAll();
		} catch (err) {
			toastError(err, 'Não foi possível resolver a pendência');
		}
	}

	function aoCriada(_tarefa: Tarefa): void {
		modalNova = false;
		void invalidateAll();
	}

	function navegar(overrides: Record<string, unknown>): string {
		const merged = {
			search: params.search,
			status: params.status,
			prioridade: params.prioridade,
			responsavel: params.responsavel,
			view: params.view,
			...overrides
		};
		const url = new URLSearchParams();
		for (const [chave, valor] of Object.entries(merged)) {
			if (Array.isArray(valor)) {
				for (const v of valor) if (v) url.append(chave, v);
			} else if (valor !== undefined && valor !== '') {
				url.append(chave, String(valor));
			}
		}
		const qs = url.toString();
		return `/producao/tarefas${qs ? `?${qs}` : ''}`;
	}

	function toggleFiltro(chave: 'status' | 'prioridade' | 'responsavel', id: string): void {
		const atual = params[chave];
		const lista: string[] = atual;
		const proximo = lista.includes(id) ? lista.filter((v) => v !== id) : [...lista, id];
		void goto(navegar({ [chave]: proximo, page: 1 }));
	}

	function limparFiltro(chave: 'status' | 'prioridade' | 'responsavel', id?: string): void {
		const atual = params[chave];
		const proximo = id ? atual.filter((v) => v !== id) : [];
		void goto(navegar({ [chave]: proximo, page: 1 }));
	}

	function limparTodos(): void {
		void goto(navegar({ search: '', status: [], prioridade: [], responsavel: [], page: 1 }));
	}

	function alternarVisao(visao: 'kanban' | 'lista'): void {
		void goto(navegar({ view: visao, page: 1 }));
	}

	function formatarData(iso?: string): string {
		if (!iso) return '—';
		const [a, m, d] = iso.slice(0, 10).split('-');
		return d ? `${d}/${m}/${a}` : '—';
	}

	function aoDetalhar(t: Tarefa): void {
		modalDetalhe = t as TarefaDetalhe;
	}

	const detalheChecklist = $derived((modalDetalhe?.checklist ?? []) as ItemChecklist[]);
	const detalheFeitos = $derived(detalheChecklist.filter((i) => i.concluido).length);
	const detalhePct = $derived(
		detalheChecklist.length > 0
			? Math.round((detalheFeitos / detalheChecklist.length) * 100)
			: 0
	);

	const inputCls =
		'w-full rounded-lg border border-border bg-elevated px-3 py-2.5 text-sm text-ink placeholder:text-muted/60 focus:border-brand focus:outline-none focus:ring-1 focus:ring-brand/40';
</script>

<svelte:head>
	<title>Tarefas / Kanban — Produção — FabLab</title>
</svelte:head>

<div class="space-y-4">
	<PageHeader
		title="Tarefas"
		subtitle="Quadro Kanban de encomendas e tarefas do laboratório, com fluxo de Fila até Entregue."
	>
		{#snippet children()}
			<div class="inline-flex items-center gap-0.5 rounded-lg border border-border bg-surface p-0.5">
				<button
					type="button"
					aria-pressed={params.view === 'kanban'}
					onclick={() => alternarVisao('kanban')}
					class="rounded-md px-3 py-1.5 text-sm font-medium transition-colors {params.view === 'kanban'
						? 'bg-brand/15 text-brandhi'
						: 'text-muted hover:text-ink'}"
				>
					Kanban
				</button>
				<button
					type="button"
					aria-pressed={params.view === 'lista'}
					onclick={() => alternarVisao('lista')}
					class="rounded-md px-3 py-1.5 text-sm font-medium transition-colors {params.view === 'lista'
						? 'bg-brand/15 text-brandhi'
						: 'text-muted hover:text-ink'}"
				>
					Lista
				</button>
			</div>
			{#if canEditProducao}
				<button
					type="button"
					data-testid="tar-nova"
					onclick={() => (modalNova = true)}
					class="inline-flex items-center gap-1.5 rounded-lg bg-brand px-3 py-2 text-sm font-semibold text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi"
				>
					<Icon name="plus" class="h-4 w-4" /> Nova tarefa
				</button>
			{/if}
		{/snippet}
	</PageHeader>

	<div class="grid grid-cols-2 gap-3 lg:grid-cols-4">
		<div class={kpiCls}>
			<span class="text-xs text-muted">Pendentes</span>
			<p class="mt-1 text-2xl font-semibold text-warn tabular-nums">{pendentes}</p>
		</div>
		<div class={kpiCls}>
			<span class="text-xs text-muted">Em andamento</span>
			<p class="mt-1 text-2xl font-semibold text-brandhi tabular-nums">{emAndamento}</p>
		</div>
		<div class={kpiCls}>
			<span class="text-xs text-muted">Atrasadas</span>
			<p class="mt-1 text-2xl font-semibold text-danger tabular-nums">{atrasadas}</p>
		</div>
		<div class={kpiCls}>
			<span class="text-xs text-muted">Concluídas</span>
			<p class="mt-1 text-2xl font-semibold text-success tabular-nums">{concluidas}</p>
		</div>
	</div>

	{#if hasError}
		<ErrorBanner
			message="Não foi possível carregar as tarefas"
			hint={error ?? ''}
			onRetry={() => void goto('/producao/tarefas', { invalidateAll: true })}
		/>
	{:else if loading}
		<div class="grid grid-cols-2 gap-3 lg:grid-cols-5">
			{#each ORDEM_COLUNAS as col (col)}
				<div class="space-y-2 rounded-xl border border-border bg-surface p-3">
					<Skeleton class="h-4 w-2/3" />
					<Skeleton class="h-16 w-full" />
					<Skeleton class="h-16 w-full" />
				</div>
			{/each}
		</div>
	{:else}
		<!-- Filtros -->
		<div class="flex flex-wrap items-center gap-2">
			<SearchInput
				value={params.search}
				onSearch={(v) => void goto(navegar({ search: v, page: 1 }))}
				placeholder="Buscar tarefa, código ou responsável…"
				class="min-w-56 flex-1"
			/>
			<Dropdown
				label="Status"
				options={statusOptions}
				selected={params.status}
				onToggle={(id) => void toggleFiltro('status', id)}
				onClear={() => void limparFiltro('status')}
			/>
			<Dropdown
				label="Prioridade"
				options={prioridadeOptions}
				selected={params.prioridade}
				onToggle={(id) => void toggleFiltro('prioridade', id)}
				onClear={() => void limparFiltro('prioridade')}
			/>
			<Dropdown
				label="Responsável"
				options={responsavelOptions}
				selected={params.responsavel}
				onToggle={(id) => void toggleFiltro('responsavel', id)}
				onClear={() => void limparFiltro('responsavel')}
			/>
		</div>

		{#if hasFilters}
			<div class="flex flex-wrap items-center gap-1.5">
				{#each params.status as s (s)}
					<Chip label={TAREFA_STATUS_META[s]?.label ?? s} onRemove={() => void limparFiltro('status', s)} />
				{/each}
				{#each params.prioridade as p (p)}
					<Chip label={p} onRemove={() => void limparFiltro('prioridade', p)} />
				{/each}
				{#each params.responsavel as r (r)}
					<Chip
						label={responsaveis.find((p) => p.id === r)?.nome ?? 'Responsável'}
						onRemove={() => void limparFiltro('responsavel', r)}
					/>
				{/each}
				<button
					onclick={() => void limparTodos()}
					class="text-xs font-medium text-brandhi transition hover:text-brand"
				>
					Limpar filtros
				</button>
			</div>
		{/if}

		<!-- Conteúdo -->
		{#if tarefas.length === 0}
			<div class="rounded-xl border border-border bg-surface">
				<EmptyState
					icon="stack"
					title="Ainda não há tarefas"
					description="Crie a primeira tarefa para começar o fluxo do Kanban."
				>
					{#snippet children()}
						{#if canEditProducao}
							<button
								type="button"
								data-testid="tar-nova"
								onclick={() => (modalNova = true)}
								class="inline-flex items-center gap-1.5 rounded-lg bg-brand px-3 py-2 text-sm font-semibold text-white transition hover:bg-brandhi"
							>
								<Icon name="plus" class="h-4 w-4" /> Nova tarefa
							</button>
						{/if}
					{/snippet}
				</EmptyState>
			</div>
		{:else if filtrado.length === 0}
			<div class="rounded-xl border border-border bg-surface">
				<EmptyState
					icon="filter"
					title="Nenhuma tarefa encontrada"
					description="Ajuste ou limpe os filtros para ver mais resultados."
				>
					{#snippet children()}
						<button
							onclick={() => void limparTodos()}
							class="rounded-lg border border-border bg-surface px-3 py-2 text-sm font-medium text-ink transition hover:border-brand/50 hover:text-brandhi"
						>
							Limpar filtros
						</button>
					{/snippet}
				</EmptyState>
			</div>

		<!-- Lista -->
		{:else if params.view === 'lista'}
			<div class="overflow-x-auto rounded-xl border border-border bg-surface">
				<table class="w-full min-w-[720px]">
					<thead class="border-b border-border bg-elevated/40">
						<tr>
							<th class="px-3 py-2 text-left text-[11px] font-semibold uppercase tracking-wide text-muted">Código</th>
							<th class="px-3 py-2 text-left text-[11px] font-semibold uppercase tracking-wide text-muted">Título</th>
							<th class="px-3 py-2 text-left text-[11px] font-semibold uppercase tracking-wide text-muted">Responsável</th>
							<th class="px-3 py-2 text-left text-[11px] font-semibold uppercase tracking-wide text-muted">Prazo</th>
							<th class="px-3 py-2 text-right text-[11px] font-semibold uppercase tracking-wide text-muted">Status</th>
						</tr>
					</thead>
					<tbody>
						{#each filtrado as tarefa (tarefa.id)}
							<tr
								data-testid="tar-row"
								class="cursor-pointer border-b border-border transition hover:bg-elevated/40 last:border-0"
								onclick={() => aoDetalhar(tarefa)}
							>
								<td class="px-3 py-2 font-mono text-xs text-muted">{tarefa.codigo}</td>
								<td class="max-w-xs truncate px-3 py-2 text-sm font-medium text-ink">{tarefa.titulo}</td>
								<td class="px-3 py-2">
									<span class="flex items-center gap-1.5">
										<Avatar name={tarefa.responsavel.nome} size="xs" />
										<span class="text-sm text-ink">{tarefa.responsavel.nome}</span>
									</span>
								</td>
								<td class="px-3 py-2 text-sm tabular-nums text-muted">{formatarData(tarefa.prazo)}</td>
								<td class="px-3 py-2 text-right">
									<StatusBadge {...TAREFA_STATUS_META[tarefa.status]} />
								</td>
							</tr>
						{/each}
					</tbody>
				</table>
			</div>

		<!-- Kanban -->
		{:else}
			<div class="grid grid-cols-1 gap-3 md:grid-cols-2 xl:grid-cols-5">
				{#each colunasComTarefas as col (col.coluna)}
					<section
						aria-label={`Coluna ${KANBAN_COLUNA_META[col.coluna].label}`}
						class="flex min-h-[260px] flex-col rounded-xl border border-border bg-surface/60 p-3"
					>
						<header class="flex items-center justify-between gap-2 px-1">
							<h3 class="text-xs font-semibold uppercase tracking-wide text-muted">
								{KANBAN_COLUNA_META[col.coluna].label}
							</h3>
							<span class="inline-flex h-5 min-w-5 items-center justify-center rounded-full bg-elevated px-1.5 text-[11px] font-bold text-muted tabular-nums">
								{col.itens.length}
							</span>
						</header>

						<div class="mt-3 flex-1 space-y-2">
							{#if col.itens.length === 0}
								<div class="flex h-20 items-center justify-center rounded-lg border border-dashed border-border text-[11px] text-muted">
									Sem tarefas
								</div>
							{:else}
{#each col.itens as tarefa (tarefa.id)}
								{@const idxColuna = ORDEM_COLUNAS.indexOf(colunaDe(tarefa))}
								{@const alvoEsquerda = ORDEM_COLUNAS[Math.max(0, idxColuna - 1)]}
								{@const alvoDireita = ORDEM_COLUNAS[Math.min(ORDEM_COLUNAS.length - 1, idxColuna + 1)]}
								<article
										data-testid="tar-card"
										class="group rounded-lg border border-border bg-surface p-3 shadow-sm transition hover:border-brand/40"
									>
										<button
											type="button"
											onclick={() => aoDetalhar(tarefa)}
											class="block w-full text-left focus:outline-none focus-visible:ring-2 focus-visible:ring-brand/50"
										>
											<div class="flex items-center justify-between gap-2">
												<span class="font-mono text-[10px] text-muted">{tarefa.codigo}</span>
												{#if tarefa.pendencia5s}
													<span
														class="rounded-full bg-danger/10 px-1.5 py-0.5 text-[9px] font-semibold text-danger"
														title={`Pendência 5S: ${tarefa.pendencia5s.titulo}`}
													>
														pendência
													</span>
												{/if}
											</div>
											<h4 class="mt-1 text-sm font-semibold leading-snug text-ink group-hover:text-brandhi">
												{tarefa.titulo}
											</h4>
											{#if tarefa.projeto}
												<p class="mt-0.5 truncate font-mono text-[10px] text-muted">{tarefa.projeto.codigo}</p>
											{/if}
											<div class="mt-2 flex items-center justify-between gap-2">
												<StatusBadge {...TAREFA_STATUS_META[tarefa.status]} />
												<span title={tarefa.responsavel.nome}>
													<Avatar name={tarefa.responsavel.nome} size="xs" />
												</span>
											</div>
										</button>

										{#if canEditProducao}
											<div class="mt-2 flex items-center justify-between border-t border-border pt-2">
												<button
													type="button"
													aria-label={`Mover para a esquerda (${KANBAN_COLUNA_META[alvoEsquerda].label})`}
													disabled={idxColuna === 0}
													onclick={() => void mover(tarefa, -1)}
													class="rounded-md p-1 text-muted transition hover:bg-elevated hover:text-brandhi disabled:cursor-not-allowed disabled:opacity-30"
												>
													<Icon name="arrow-left" class="h-4 w-4" />
												</button>
												<button
													type="button"
													aria-label={`Mover para a direita (${KANBAN_COLUNA_META[alvoDireita].label})`}
													disabled={idxColuna === ORDEM_COLUNAS.length - 1}
													onclick={() => void mover(tarefa, 1)}
													class="rounded-md p-1 text-muted transition hover:bg-elevated hover:text-brandhi disabled:cursor-not-allowed disabled:opacity-30"
												>
													<Icon name="arrow-right" class="h-4 w-4" />
												</button>
											</div>
										{/if}
									</article>
								{/each}
							{/if}
						</div>
					</section>
				{/each}
			</div>

			{#if !canEditProducao}
				<p class="text-[11px] text-muted">
					Ir para a próxima etapa fica disponível apenas para quem pode editar produção.
				</p>
			{/if}

			<p class="rounded-lg border border-border bg-surface px-3 py-2 text-[11px] text-muted">
				Reorganização por arrastar e soltar fica disponível quando o backend expuser a coluna Kanban
				(<span class="font-mono">/producao/tarefas</span> ainda não retorna <span class="font-mono">coluna</span> nem
				<span class="font-mono">checklist</span> — 🟡 contrato pendente).
			</p>
		{/if}
	{/if}

	<ModalNovaTarefa
		open={modalNova}
		projetos={projetos}
		responsaveis={responsaveis}
		onClose={() => (modalNova = false)}
		onCriada={aoCriada}
	/>
</div>

{#if modalDetalhe}
	{@const tarefa = modalDetalhe}
	<Modal
		open={modalDetalhe !== null}
		title={tarefa.titulo}
		subtitle={`${tarefa.codigo} · vence em ${formatarData(tarefa.prazo)}`}
		onClose={() => (modalDetalhe = null)}
		width="lg"
	>
		{#snippet children()}
			<div class="space-y-4">
				<div class="flex flex-wrap items-center gap-2">
					<StatusBadge {...TAREFA_STATUS_META[tarefa.status]} />
					{#if tarefa.prioridade === 'Alta'}
						<span class="rounded-full bg-danger/10 px-2 py-0.5 text-[11px] font-medium text-danger">Prioridade alta</span>
					{/if}
					{#if tarefa.projeto}
						<span class="rounded-full border border-border bg-elevated/60 px-2 py-0.5 font-mono text-[11px] text-muted">
							{tarefa.projeto.codigo}
						</span>
					{/if}
				</div>

				{#if tarefa.descricao}
					<p class="text-sm leading-relaxed text-muted">{tarefa.descricao}</p>
				{/if}

				<div class="rounded-lg border border-border bg-elevated/30 p-3">
					<p class="text-xs text-muted">Responsável</p>
					<div class="mt-2 flex items-center gap-2">
						<Avatar name={tarefa.responsavel.nome} size="sm" />
						<span class="text-sm text-ink">{tarefa.responsavel.nome}</span>
					</div>

					{#if canEditProducao}
						<label class="mt-3 block">
							<span class="mb-1.5 block text-xs font-medium text-muted">Atribuir responsável</span>
							<div class="flex gap-2">
								<Select
									id="tarefa-responsavel"
									options={responsaveis.map((p) => ({ id: p.id, label: p.nome }))}
									value={novoResponsavelId}
									onChange={(v) => (novoResponsavelId = v)}
								/>
								<button
									type="button"
									data-testid="tar-atribuir"
									onclick={() => void atribuir()}
									disabled={novoResponsavelId === tarefa.responsavel.id}
									class="shrink-0 rounded-md border border-border bg-surface px-3 text-sm font-medium text-ink transition hover:border-brand/50 hover:text-brandhi disabled:cursor-not-allowed disabled:opacity-40"
								>
									Atribuir
								</button>
							</div>
						</label>
					{/if}
				</div>

				{#if detalheChecklist.length > 0}
					<div class="rounded-lg border border-border bg-elevated/30 p-3">
						<div class="flex items-center justify-between">
							<p class="text-xs font-medium text-ink">Checklist ({detalheFeitos}/{detalheChecklist.length})</p>
							<span class="text-xs font-semibold text-brandhi tabular-nums">{detalhePct}%</span>
						</div>
						<div class="mt-2 h-1.5 w-full overflow-hidden rounded-full bg-elevated">
							<div
								class="h-full rounded-full bg-brand transition-all"
								style={`width: ${detalhePct}%`}
							></div>
						</div>
						<ul class="mt-3 space-y-1.5">
							{#each detalheChecklist as item (item.id)}
								<li class="flex items-center gap-2 text-sm text-ink">
									{#if item.concluido}
										<Icon name="check" class="h-4 w-4 shrink-0 text-success" />
									{:else}
										<span class="h-4 w-4 shrink-0 rounded border border-border bg-elevated" aria-hidden="true"></span>
									{/if}
									<span class={item.concluido ? 'text-muted line-through' : ''}>{item.item}</span>
								</li>
							{/each}
						</ul>
					</div>
				{/if}

				{#if tarefa.pendencia5s}
					<div class="rounded-lg border border-danger/30 bg-danger/10 p-3">
						<div class="flex flex-wrap items-center justify-between gap-2">
							<div>
								<p class="text-xs font-medium text-danger">Pendência 5S vinculada</p>
								<p class="mt-0.5 text-sm text-ink">{tarefa.pendencia5s.titulo}</p>
								<p class="mt-1">
									<StatusBadge {...GRAVIDADE_META[tarefa.pendencia5s.gravidade]} />
								</p>
							</div>
							{#if canEditProducao}
								<button
									type="button"
									data-testid="tar-resolver-pendencia"
									onclick={() => void resolverPendencia()}
									class="rounded-md bg-danger px-3 py-2 text-sm font-medium text-white transition hover:bg-danger/90"
								>
									Resolver pendência
								</button>
							{/if}
						</div>
					</div>
				{/if}
			</div>
		{/snippet}
	</Modal>
{/if}