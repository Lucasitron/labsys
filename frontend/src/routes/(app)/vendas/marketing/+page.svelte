<script lang="ts">
	import { goto, invalidateAll } from '$app/navigation';
	import type { PageProps } from './$types';
	import type { Prioridade, TarefaMarketing, TarefaStatus } from '$lib/types/vendas';
	import { createTarefa, updateTarefa } from '$lib/api/vendas/marketing';
	import { toUserMessage } from '$lib/utils/errors';
	import { toasts } from '$lib/stores/toast';
	import { formatNumber } from '$lib/utils/format';
	import { formatDateBR } from '$lib/utils/vendas-format';
	import { prioridadeMeta, tarefaStatusMeta } from '$lib/utils/vendas-status';
	import PageHeader from '$lib/components/ui/PageHeader.svelte';
	import SearchInput from '$lib/components/ui/SearchInput.svelte';
	import Avatar from '$lib/components/ui/Avatar.svelte';
	import Chip from '$lib/components/ui/Chip.svelte';
	import StatusBadge from '$lib/components/ui/StatusBadge.svelte';
	import TableSkeleton from '$lib/components/ui/TableSkeleton.svelte';
	import EmptyState from '$lib/components/ui/EmptyState.svelte';
	import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte';
	import RowActions from '$lib/components/ui/RowActions.svelte';
	import Icon from '$lib/components/ui/Icon.svelte';
	import Modal from '$lib/components/ui/Modal.svelte';

	let { data }: PageProps = $props();

	const params = $derived(data.params);
	const resultado = $derived(data.resultado);
	const erro = $derived(data.error);

	const tarefas = $derived<TarefaMarketing[]>(resultado?.tarefas ?? []);
	const contagens = $derived<Record<string, number>>(resultado?.counts ?? {});

	const carregando = $derived(resultado === null && erro === null);
	const comErro = $derived(erro !== null && resultado === null);

	const filtrado = $derived(params.search !== '' || params.status !== '' || params.prioridade !== '');

	const STATUS_OPCOES: TarefaStatus[] = ['Pendente', 'Em Andamento', 'Concluída'];
	const PRIORIDADE_OPCOES: Prioridade[] = ['Baixa', 'Média', 'Alta'];

	function numero(valor: unknown): number | null {
		const n = Number(valor);
		return Number.isFinite(n) ? n : null;
	}

	// KPIs servidos pelo backend (nunca derivados no client).
	function kpi(chaves: string[]): string {
		for (const chave of chaves) {
			const n = numero(contagens[chave]);
			if (n !== null) return formatNumber(n);
		}
		return '—';
	}

	function dataBR(iso: string | undefined): string {
		if (!iso || Number.isNaN(new Date(iso).getTime())) return '—';
		return formatDateBR(iso);
	}

	function prazoVencido(tarefa: TarefaMarketing): boolean {
		if (!tarefa.dataFim) return false;
		if (tarefa.status === 'Concluída') return false;
		const limite = new Date(tarefa.dataFim).getTime();
		if (Number.isNaN(limite)) return false;
		const hoje = new Date();
		hoje.setHours(0, 0, 0, 0);
		const dias = (limite - hoje.getTime()) / 86_400_000;
		return dias < 0 || dias <= 7;
	}

	interface Query {
		[key: string]: string | number | undefined;
	}

	function navegar(overrides: Query): void {
		const base: Query = {
			search: params.search || undefined,
			status: params.status || undefined,
			prioridade: params.prioridade || undefined,
			page: params.page,
			pageSize: params.pageSize === 10 ? undefined : params.pageSize
		};
		const merged = { ...base, ...overrides };
		const url = new URLSearchParams();
		for (const [chave, valor] of Object.entries(merged)) {
			if (valor !== undefined && valor !== '') url.append(chave, String(valor));
		}
		const qs = url.toString();
		void goto(`/vendas/marketing${qs ? `?${qs}` : ''}`);
	}

	function onSearch(termo: string): void {
		navegar({ search: termo || undefined, page: 1 });
	}

	function onStatus(valor: string): void {
		navegar({ status: valor || undefined, page: 1 });
	}

	function onPrioridade(valor: string): void {
		navegar({ prioridade: valor || undefined, page: 1 });
	}

	function limparFiltros(): void {
		navegar({ search: undefined, status: undefined, prioridade: undefined, page: 1 });
	}

	function onPage(page: number): void {
		navegar({ page });
	}

	function tentarNovamente(): void {
		void goto(`/vendas/marketing${window.location.search}`, { invalidateAll: true });
	}

	// ---- Mudar status via ⋮ (PUT /api/vendas/tarefas-marketing/{id} 🔴) ----

	let atualizandoId = $state<string | null>(null);

	async function mudarStatus(tarefa: TarefaMarketing, status: TarefaStatus): Promise<void> {
		if (atualizandoId) return;
		atualizandoId = tarefa.id;
		try {
			await updateTarefa(tarefa.id, { status });
			toasts.success(`Tarefa "${tarefa.titulo}" atualizada para ${status}.`);
			await invalidateAll();
		} catch (err) {
			toasts.danger(toUserMessage(err).message);
		} finally {
			atualizandoId = null;
		}
	}

	function acaoLinha(tarefa: TarefaMarketing, acao: string): void {
		if (acao === 'concluir') void mudarStatus(tarefa, 'Concluída');
		else if (acao === 'andamento') void mudarStatus(tarefa, 'Em Andamento');
		else if (acao === 'reabrir') void mudarStatus(tarefa, 'Pendente');
	}

	// ---- Modal Nova tarefa (POST /api/vendas/tarefas-marketing 🔴) ----

	let modalAberto = $state(false);
	let titulo = $state('');
	let descricao = $state('');
	let responsavelId = $state('');
	let dataInicio = $state('');
	let prazo = $state('');
	let prioridade = $state<Prioridade>('Média');
	let erros = $state<Record<string, string>>({});
	let ocupado = $state(false);

	function abrirModal(): void {
		titulo = '';
		descricao = '';
		responsavelId = '';
		dataInicio = '';
		prazo = '';
		prioridade = 'Média';
		erros = {};
		modalAberto = true;
	}

	function fecharModal(): void {
		if (ocupado) return;
		modalAberto = false;
	}

	async function salvarTarefa(): Promise<void> {
		if (ocupado) return;
		const novos: Record<string, string> = {};
		if (!titulo.trim()) novos['titulo'] = 'Informe o título da tarefa.';
		if (!responsavelId.trim()) novos['responsavelId'] = 'Informe o responsável.';
		if (!prazo) novos['prazo'] = 'Informe o prazo.';
		else if (Number.isNaN(new Date(prazo).getTime())) novos['prazo'] = 'Informe um prazo válido.';
		erros = novos;
		if (Object.keys(novos).length > 0) return;
		ocupado = true;
		try {
			await createTarefa({
				titulo: titulo.trim(),
				...(descricao.trim() ? { descricao: descricao.trim() } : {}),
				responsavelId: responsavelId.trim(),
				...(dataInicio ? { dataInicio } : {}),
				dataFim: prazo,
				prioridade
			});
			toasts.success('Tarefa de marketing criada.');
			modalAberto = false;
			await invalidateAll();
		} catch (err) {
			toasts.danger(toUserMessage(err).message);
		} finally {
			ocupado = false;
		}
	}

	const inputCls =
		'w-full rounded-md border border-border bg-elevated px-3 py-2.5 text-sm text-ink placeholder:text-muted/60 focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/30 transition';
	const inputErroCls = 'border-danger/60 focus:border-danger focus:ring-danger/30';
	const labelCls = 'mb-1 block text-xs font-medium text-muted';

	const temProxima = $derived(tarefas.length >= params.pageSize);
</script>

<svelte:head>
	<title>Marketing — Vendas — FabLab</title>
</svelte:head>

<div class="space-y-4">
	<PageHeader
		title="Marketing"
		subtitle="Tarefas internas da equipe para divulgar o laboratório."
	>
		{#snippet children()}
			<button
				type="button"
				onclick={abrirModal}
				data-testid="mk-nova"
				aria-label="Nova tarefa de marketing"
				class="inline-flex items-center gap-1.5 rounded-md bg-brand px-3.5 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi"
			>
				<Icon name="plus" class="h-4 w-4" /> Nova tarefa
			</button>
		{/snippet}
	</PageHeader>

	{#if comErro}
		<ErrorBanner
			message="Não foi possível carregar as tarefas de marketing"
			hint="Verifique sua conexão e tente novamente. Se persistir, contate o suporte."
			onRetry={tentarNovamente}
			testid="mk-retry"
		/>
	{:else if carregando}
		<div class="grid grid-cols-2 gap-4 lg:grid-cols-4" aria-hidden="true">
			{#each [1, 2, 3, 4] as i (i)}
				<div class="h-24 animate-pulse rounded-xl border border-border bg-elevated"></div>
			{/each}
		</div>
		<div class="h-10 animate-pulse rounded-lg bg-elevated"></div>
		<div class="rounded-xl border border-border bg-surface p-4">
			<TableSkeleton rows={6} columns={5} />
		</div>
	{:else}
		<!-- KPIs servidos pelo backend (nunca derivados no client) -->
		<section class="grid grid-cols-2 gap-4 lg:grid-cols-4" aria-label="Indicadores">
			<div class="rounded-xl border border-border bg-elevated p-4">
				<p class="text-xs text-muted">Pendentes</p>
				<p class="mt-1 font-mono text-2xl font-semibold tabnums text-ink">
					{kpi(['Pendentes', 'Pendente'])}
				</p>
				<p class="mt-0.5 text-xs text-muted">aguardando início</p>
			</div>
			<div class="rounded-xl border border-border bg-elevated p-4">
				<p class="text-xs text-muted">Em andamento</p>
				<p class="mt-1 font-mono text-2xl font-semibold tabnums text-ink">
					{kpi(['Em andamento', 'Em Andamento'])}
				</p>
				<p class="mt-0.5 text-xs text-muted">em execução</p>
			</div>
			<div class="rounded-xl border border-border bg-elevated p-4">
				<p class="text-xs text-muted">Concluídas</p>
				<p class="mt-1 font-mono text-2xl font-semibold tabnums text-ink">
					{kpi(['Concluídas', 'Concluída'])}
				</p>
				<p class="mt-0.5 text-xs text-muted">finalizadas</p>
			</div>
			<div class="rounded-xl border border-border bg-elevated p-4">
				<p class="text-xs text-muted">Alta prioridade</p>
				<p class="mt-1 font-mono text-2xl font-semibold tabnums text-ink">
					{kpi(['Alta prioridade', 'Alta'])}
				</p>
				<p class="mt-0.5 text-xs text-muted">atenção imediata</p>
			</div>
		</section>

		<div class="flex flex-wrap items-end gap-2">
			<div class="min-w-56 flex-1">
				<SearchInput
					value={params.search}
					onSearch={onSearch}
					placeholder="Buscar por título ou responsável…"
					delay={300}
					label="Buscar tarefas"
				/>
			</div>
			<div class="w-44">
				<label for="mk-status" class="mb-1 block text-xs font-medium text-muted">Status</label>
				<select
					id="mk-status"
					value={params.status}
					onchange={(e) => onStatus((e.currentTarget as HTMLSelectElement).value)}
					aria-label="Filtrar por status"
					class="w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm text-ink focus:border-brand focus:outline-none focus:ring-1 focus:ring-brand/40"
				>
					<option value="">Todos</option>
					{#each STATUS_OPCOES as s (s)}
						<option value={s}>{s}</option>
					{/each}
				</select>
			</div>
			<div class="w-44">
				<label for="mk-prioridade" class="mb-1 block text-xs font-medium text-muted">Prioridade</label>
				<select
					id="mk-prioridade"
					value={params.prioridade}
					onchange={(e) => onPrioridade((e.currentTarget as HTMLSelectElement).value)}
					aria-label="Filtrar por prioridade"
					class="w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm text-ink focus:border-brand focus:outline-none focus:ring-1 focus:ring-brand/40"
				>
					<option value="">Todas</option>
					{#each PRIORIDADE_OPCOES as p (p)}
						<option value={p}>{p}</option>
					{/each}
				</select>
			</div>
		</div>

		{#if filtrado}
			<div class="flex flex-wrap items-center gap-2" aria-label="Filtros ativos">
				{#if params.search}
					<Chip label={`Busca: ${params.search}`} onRemove={() => onSearch('')} />
				{/if}
				{#if params.status}
					<Chip label={params.status} onRemove={() => onStatus('')} />
				{/if}
				{#if params.prioridade}
					<Chip label={params.prioridade} onRemove={() => onPrioridade('')} />
				{/if}
				<button
					type="button"
					onclick={limparFiltros}
					class="text-xs font-medium text-muted transition hover:text-brandhi"
				>
					Limpar filtros
				</button>
			</div>
		{/if}

		{#if tarefas.length === 0}
			<div class="rounded-xl border border-border bg-surface">
				{#if filtrado}
					<EmptyState
						icon="search"
						title="Nenhuma tarefa encontrada"
						description="Nenhuma tarefa combina com os filtros aplicados."
					>
						{#snippet children()}
							<button
								type="button"
								onclick={limparFiltros}
								class="rounded-md border border-border bg-elevated px-3 py-2 text-sm font-medium text-ink transition hover:border-brand/50 hover:text-brandhi"
							>
								Limpar filtros
							</button>
						{/snippet}
					</EmptyState>
				{:else}
					<EmptyState
						icon="calendar"
						title="Nenhuma tarefa cadastrada"
						description="Crie a primeira tarefa para organizar a divulgação do laboratório."
					>
						{#snippet children()}
							<button
								type="button"
								onclick={abrirModal}
								class="inline-flex items-center gap-1.5 rounded-md bg-brand px-3.5 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi"
							>
								<Icon name="plus" class="h-4 w-4" /> Nova tarefa
							</button>
						{/snippet}
					</EmptyState>
				{/if}
			</div>
		{:else}
			<div class="overflow-hidden rounded-xl border border-border bg-surface">
				<div class="overflow-x-auto">
					<table class="w-full min-w-[820px] text-sm">
						<thead>
							<tr
								class="border-b border-border bg-elevated/50 text-left text-[11px] uppercase tracking-wide text-muted"
							>
								<th class="px-4 py-2.5 font-medium">Tarefa</th>
								<th class="px-4 py-2.5 font-medium">Responsável</th>
								<th class="px-4 py-2.5 font-medium">Prazo</th>
								<th class="px-4 py-2.5 font-medium">Prioridade</th>
								<th class="px-4 py-2.5 font-medium">Status</th>
								<th class="w-12 px-4 py-2.5 text-right font-medium">
									<span class="sr-only">Ações</span>
								</th>
							</tr>
						</thead>
						<tbody>
							{#each tarefas as tarefa (tarefa.id)}
								{@const statusMeta = tarefaStatusMeta(tarefa.status)}
								{@const prioMeta = prioridadeMeta(tarefa.prioridade)}
								{@const vencido = prazoVencido(tarefa)}
								<tr
									data-testid="mk-tarefa"
									class="border-b border-border transition last:border-0 hover:bg-elevated/40"
								>
									<td class="max-w-72 px-4 py-3">
										<p class="font-medium text-ink">{tarefa.titulo}</p>
										{#if tarefa.descricao}
											<p class="mt-0.5 truncate text-xs text-muted">{tarefa.descricao}</p>
										{/if}
									</td>
									<td class="px-4 py-3">
										<div class="flex items-center gap-2">
											<Avatar name={tarefa.responsavelId} size="sm" tone="brand" />
											<span class="max-w-32 truncate text-xs text-muted">{tarefa.responsavelId}</span>
										</div>
									</td>
									<td
										class="px-4 py-3 font-mono text-xs tabnums {vencido
											? 'font-semibold text-danger'
											: 'text-muted'}"
									>
										{dataBR(tarefa.dataFim)}
									</td>
									<td class="px-4 py-3">
										<StatusBadge label={prioMeta.label} color={prioMeta.color} />
									</td>
									<td class="px-4 py-3">
										<StatusBadge label={statusMeta.label} color={statusMeta.color} />
									</td>
									<td class="px-4 py-3 text-right">
										<RowActions
											label={`Ações da tarefa ${tarefa.titulo}`}
											actions={[
												{
													id: 'concluir',
													label: 'Concluir',
													icon: 'check',
													hidden: tarefa.status === 'Concluída'
												},
												{
													id: 'andamento',
													label: 'Em andamento',
													icon: 'arrow-right',
													hidden: tarefa.status === 'Em Andamento'
												},
												{
													id: 'reabrir',
													label: 'Reabrir',
													icon: 'arrow-uturn-left',
													hidden: tarefa.status === 'Pendente'
												}
											]}
											onSelect={(id) => acaoLinha(tarefa, id)}
										/>
									</td>
								</tr>
							{/each}
						</tbody>
					</table>
				</div>
				<div class="border-t border-border px-4 py-3">
					<div class="flex flex-wrap items-center justify-between gap-2">
						<p class="text-xs text-muted">página {params.page} · {tarefas.length} tarefas</p>
						<div class="flex items-center gap-1">
							<button
								type="button"
								onclick={() => onPage(params.page - 1)}
								disabled={params.page <= 1}
								aria-label="Página anterior"
								class="inline-flex h-8 w-8 items-center justify-center rounded-lg border border-border bg-surface text-muted transition-colors hover:border-brand/50 hover:text-brandhi disabled:cursor-not-allowed disabled:opacity-40"
							>
								<Icon name="arrow-left" class="h-4 w-4" />
							</button>
							<button
								type="button"
								onclick={() => onPage(params.page + 1)}
								disabled={!temProxima}
								aria-label="Próxima página"
								class="inline-flex h-8 w-8 items-center justify-center rounded-lg border border-border bg-surface text-muted transition-colors hover:border-brand/50 hover:text-brandhi disabled:cursor-not-allowed disabled:opacity-40"
							>
								<Icon name="arrow-right" class="h-4 w-4" />
							</button>
						</div>
					</div>
				</div>
			</div>
		{/if}
	{/if}
</div>

<Modal
	open={modalAberto}
	title="Nova tarefa"
	subtitle="Cadastre uma atividade de divulgação do laboratório."
	onClose={fecharModal}
>
	{#snippet children()}
		<div data-testid="modal-nova-tarefa" class="space-y-4">
			<div>
				<label for="mk-titulo" class={labelCls}>Título <span class="text-danger">*</span></label>
				<input
					id="mk-titulo"
					type="text"
					value={titulo}
					oninput={(e) => {
						titulo = (e.currentTarget as HTMLInputElement).value;
						if (erros['titulo']) erros = { ...erros, titulo: '' };
					}}
					placeholder="Ex.: Campanha de boas-vindas às escolas"
					aria-invalid={!!erros['titulo']}
					class="{inputCls} {erros['titulo'] ? inputErroCls : ''}"
				/>
				{#if erros['titulo']}
					<p role="alert" class="mt-1 text-xs text-danger">{erros['titulo']}</p>
				{/if}
			</div>

			<div>
				<label for="mk-descricao" class={labelCls}>Descrição</label>
				<textarea
					id="mk-descricao"
					value={descricao}
					oninput={(e) => (descricao = (e.currentTarget as HTMLTextAreaElement).value)}
					rows={3}
					placeholder="Detalhe o objetivo da tarefa…"
					class="{inputCls} resize-y"
				></textarea>
			</div>

			<div>
				<label for="mk-responsavel" class={labelCls}>Responsável <span class="text-danger">*</span></label>
				<input
					id="mk-responsavel"
					type="text"
					value={responsavelId}
					oninput={(e) => {
						responsavelId = (e.currentTarget as HTMLInputElement).value;
						if (erros['responsavelId']) erros = { ...erros, responsavelId: '' };
					}}
					placeholder="ID ou nome do responsável"
					aria-invalid={!!erros['responsavelId']}
					class="{inputCls} {erros['responsavelId'] ? inputErroCls : ''}"
				/>
				{#if erros['responsavelId']}
					<p role="alert" class="mt-1 text-xs text-danger">{erros['responsavelId']}</p>
				{/if}
			</div>

			<div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
				<div>
					<label for="mk-inicio" class={labelCls}>Início</label>
					<input
						id="mk-inicio"
						type="date"
						value={dataInicio}
						onchange={(e) => (dataInicio = (e.currentTarget as HTMLInputElement).value)}
						class={inputCls}
					/>
				</div>
				<div>
					<label for="mk-prazo" class={labelCls}>Prazo <span class="text-danger">*</span></label>
					<input
						id="mk-prazo"
						type="date"
						value={prazo}
						onchange={(e) => {
							prazo = (e.currentTarget as HTMLInputElement).value;
							if (erros['prazo']) erros = { ...erros, prazo: '' };
						}}
						aria-invalid={!!erros['prazo']}
						class="{inputCls} {erros['prazo'] ? inputErroCls : ''}"
					/>
					{#if erros['prazo']}
						<p role="alert" class="mt-1 text-xs text-danger">{erros['prazo']}</p>
					{/if}
				</div>
			</div>

			<fieldset>
				<legend class="{labelCls} mb-2">Prioridade <span class="text-danger">*</span></legend>
				<div class="flex flex-wrap gap-2" role="radiogroup" aria-label="Prioridade">
					{#each PRIORIDADE_OPCOES as p (p)}
						<label
							class="inline-flex cursor-pointer items-center gap-1.5 rounded-full border px-3 py-1.5 text-xs font-medium transition {prioridade ===
							p
								? 'border-brand bg-brand/10 text-brandhi'
								: 'border-border bg-surface text-muted hover:text-ink'}"
						>
							<input
								type="radio"
								name="mk-prioridade-radio"
								value={p}
								checked={prioridade === p}
								onchange={() => (prioridade = p)}
								class="sr-only"
							/>
							{p}
						</label>
					{/each}
				</div>
			</fieldset>
		</div>
	{/snippet}
	{#snippet footer()}
		<button
			type="button"
			onclick={fecharModal}
			disabled={ocupado}
			class="rounded-md border border-border bg-elevated px-3.5 py-2 text-sm font-medium text-ink transition hover:bg-elevated/70 disabled:opacity-50"
		>
			Cancelar
		</button>
		<button
			type="button"
			onclick={salvarTarefa}
			disabled={ocupado}
			aria-label="Salvar tarefa"
			class="rounded-md bg-brand px-3.5 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi disabled:opacity-50"
		>
			{ocupado ? 'Salvando…' : 'Salvar tarefa'}
		</button>
	{/snippet}
</Modal>
