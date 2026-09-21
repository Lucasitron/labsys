<script lang="ts">
	import { goto, invalidateAll } from '$app/navigation';
	import type { PageProps } from './$types';
	import type { Projeto, Tarefa } from '$lib/types/producao';
	import { PROJETO_STATUS_META, TAREFA_STATUS_META } from '$lib/utils/producao-status';
	import { ABAS_PROJETO, type AbaProjeto } from './+page';
	import ModalSaidaProjeto from '$lib/components/producao/ModalSaidaProjeto.svelte';
	import PageHeader from '$lib/components/ui/PageHeader.svelte';
	import StatusBadge from '$lib/components/ui/StatusBadge.svelte';
	import Avatar from '$lib/components/ui/Avatar.svelte';
	import Skeleton from '$lib/components/ui/Skeleton.svelte';
	import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte';
	import EmptyState from '$lib/components/ui/EmptyState.svelte';
	import Tabs from '$lib/components/ui/Tabs.svelte';
	import Modal from '$lib/components/ui/Modal.svelte';
	import Icon from '$lib/components/ui/Icon.svelte';

	let { data }: PageProps = $props();

	const canEditProducao = $derived(data.canEditProducao ?? false);

	const projeto = $derived(data.projeto as Projeto | null);
	const materiais = $derived(data.materiais);
	const tarefas = $derived(data.tarefas as Tarefa[]);
	const aba = $derived(data.aba);
	const erroPagina = $derived(data.erroPagina);
	const erroMateriais = $derived(data.erroMateriais);
	const erroTarefas = $derived(data.erroTarefas);

	const loading = $derived(projeto === null && erroPagina === null);

	const tabs = $derived(
		ABAS_PROJETO.map((a) => ({
			id: a.id,
			label: a.label,
			count:
				a.id === 'materiais'
					? materiais.length || undefined
					: a.id === 'tarefas'
						? tarefas.length || undefined
						: undefined
		}))
	);

	let modalSaida = $state(false);
	let modalNota = $state('');

	function irParaAba(id: string): void {
		void goto(`/producao/projetos/${projeto?.id ?? ''}?tab=${id}`, { invalidateAll: true });
	}

	function aoRegistrado(_saida: unknown): void {
		modalSaida = false;
		void invalidateAll();
	}

	function semContrato(tipo: string): void {
		modalNota =
			`Ação "${tipo}" ainda não está disponível: o endpoint de contrato do backend está pendente (🟡). Fale com o Admin.`;
	}

	function formatarData(iso?: string): string {
		if (!iso) return '—';
		const [a, m, d] = iso.slice(0, 10).split('-');
		return d ? `${d}/${m}/${a}` : '—';
	}

	const thCls = 'px-3 py-2 text-left text-[11px] font-semibold uppercase tracking-wide text-muted';
	const tdCls = 'px-3 py-2 text-sm text-ink';

	function prioridadeChip(prioridade: Tarefa['prioridade']): string {
		const map: Record<Tarefa['prioridade'], string> = {
			Baixa: 'inline-flex rounded-full bg-muted/10 px-2 py-0.5 text-[11px] font-medium text-muted',
			Média: 'inline-flex rounded-full bg-warn/10 px-2 py-0.5 text-[11px] font-medium text-warn',
			Moderada: 'inline-flex rounded-full bg-brand/10 px-2 py-0.5 text-[11px] font-medium text-brandhi',
			Alta: 'inline-flex rounded-full bg-danger/10 px-2 py-0.5 text-[11px] font-medium text-danger'
		};
		return map[prioridade];
	}
</script>

<svelte:head>
	<title>{projeto ? projeto.nome : 'Projeto'} — Produção — FabLab</title>
</svelte:head>

<div class="space-y-4">
	{#if loading}
		<div class="space-y-4">
			<Skeleton class="h-8 w-2/3" />
			<Skeleton class="h-24 w-full" />
			<Skeleton class="h-24 w-full" />
		</div>
	{:else if !projeto}
		<ErrorBanner
			message="Não foi possível carregar o projeto"
			hint={erroPagina ?? ''}
			onRetry={() => void goto(`/producao/projetos`, { invalidateAll: true })}
		/>
	{:else}
		<PageHeader
			title={projeto.nome}
			subtitle={`${projeto.codigo} · criado por ${projeto.responsavel.nome}`}
			backHref="/producao/projetos"
		>
			{#snippet children()}
				<StatusBadge {...PROJETO_STATUS_META[projeto.status]} />
			{/snippet}
		</PageHeader>

		<Tabs tabs={tabs} active={aba} onChange={irParaAba} />

		<!-- aba Geral -->
		{#if aba === 'geral'}
			<section data-testid="proj-detail" class="space-y-4">
				<div class="rounded-xl border border-border bg-surface p-4">
					<dl class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
						<div>
							<dt class="text-xs text-muted">Responsável (RGA)</dt>
							<dd class="mt-1.5 flex items-center gap-2">
								<Avatar name={projeto.responsavel.nome} size="sm" />
								<span class="text-sm text-ink">{projeto.responsavel.nome}</span>
							</dd>
						</div>
						<div>
							<dt class="text-xs text-muted">Prazo</dt>
							<dd class="mt-1.5 text-sm text-ink tabular-nums">{formatarData(projeto.prazo)}</dd>
						</div>
						<div>
							<dt class="text-xs text-muted">Progresso</dt>
							<dd class="mt-1.5 flex items-center gap-2">
								<span class="text-sm font-semibold text-ink tabular-nums">{projeto.progresso}%</span>
								<div class="h-1.5 flex-1 overflow-hidden rounded-full bg-elevated">
									<div
										class="h-full rounded-full bg-brand transition-all"
										style={`width: ${Math.min(100, Math.max(0, projeto.progresso))}%`}
									></div>
								</div>
							</dd>
						</div>
					</dl>

					{#if projeto.descricao}
						<div class="mt-4 border-t border-border pt-4">
							<p class="text-xs text-muted">Descrição</p>
							<p class="mt-1 text-sm leading-relaxed text-ink">{projeto.descricao}</p>
						</div>
					{/if}

					{#if projeto.cliente}
						<div class="mt-4 border-t border-border pt-4">
							<p class="text-xs text-muted">Cliente</p>
							<p class="mt-1 text-sm text-ink">{projeto.cliente}</p>
						</div>
					{/if}
				</div>
			</section>

		<!-- aba Documentação -->
		{:else if aba === 'documentacao'}
			<section data-testid="proj-doc" class="rounded-xl border border-border bg-surface p-4">
				<div class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
					<div>
						<h2 class="text-sm font-semibold text-ink">Documentação do projeto</h2>
						<p class="mt-1 max-w-xl text-xs text-muted">
							Envio de arquivos/imagens/links do projeto (catálogo, projeto mecânico, G-Code, DFT…).
						</p>
					</div>
					<button
						type="button"
						disabled
						title="Contrato pendente"
						onclick={() => semContrato('envio de documentos')}
						class="inline-flex items-center justify-center gap-1.5 rounded-lg bg-elevated px-3 py-2 text-sm font-medium text-muted"
					>
						<Icon name="arrow-up-tray" class="h-4 w-4" /> Enviar arquivo
					</button>
				</div>

				<div class="mt-4 rounded-lg border border-warn/30 bg-warn/10 p-3 text-xs text-muted">
					Envio/leitura de documentos depende do endpoint <span class="font-mono">/producao/projetos/:id/documentos</span> —
					contrato pendente (🟡).
				</div>

				<div class="mt-4 rounded-lg border border-border bg-elevated/30 p-4">
					<EmptyState
						icon="document"
						title="Nenhum documento disponível"
						description="Quando o contrato estiver ativo, os arquivos e links do projeto aparecerão aqui."
					/>
				</div>
			</section>

		<!-- aba Materiais -->
		{:else if aba === 'materiais'}
			<section data-testid="proj-materiais" class="space-y-3">
				<div class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
					<div>
						<h2 class="text-sm font-semibold text-ink">Materiais do projeto</h2>
						<p class="mt-1 text-xs text-muted">
							Quantidades necessárias vs. disponíveis e registro de saída de estoque.
						</p>
					</div>
					{#if canEditProducao && materiais.length > 0}
						<button
							type="button"
							data-testid="proj-materiais"
							onclick={() => (modalSaida = true)}
							class="inline-flex items-center gap-1.5 rounded-lg bg-brand px-3 py-2 text-sm font-semibold text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi"
						>
							<Icon name="shopping-bag" class="h-4 w-4" /> Registrar saída
						</button>
					{/if}
				</div>

				{#if erroMateriais}
					<ErrorBanner
						message="Não foi possível carregar os materiais"
						hint={erroMateriais}
						onRetry={() => void goto(`/producao/projetos/${projeto.id}?tab=materiais`, { invalidateAll: true })}
					/>
				{:else if materiais.length === 0}
					<div class="rounded-xl border border-border bg-surface">
						<EmptyState
							icon="cube"
							title="Nenhum material vinculado"
							description="Os materiais necessários aparecerão aqui quando forem cadastrados."
						/>
					</div>
				{:else}
					<div class="overflow-x-auto rounded-xl border border-border bg-surface">
						<table class="w-full min-w-[560px]">
							<thead class="border-b border-border bg-elevated/40">
								<tr>
									<th class={thCls}>Item</th>
									<th class={thCls}>Código</th>
									<th class={thCls}>Necessário</th>
									<th class={thCls}>Disponível</th>
									<th class="px-3 py-2 text-right text-[11px] font-semibold uppercase tracking-wide text-muted">Situação</th>
								</tr>
							</thead>
							<tbody>
								{#each materiais as material (material.codigo)}
									{@const falta = material.quantidadeNecessaria > material.disponivel}
									<tr class="border-b border-border last:border-0">
										<td class={tdCls}>{material.item}</td>
										<td class={`${tdCls} font-mono text-xs text-muted`}>{material.codigo}</td>
										<td class={`${tdCls} tabular-nums`}>{material.quantidadeNecessaria}</td>
										<td class={`${tdCls} tabular-nums`}>{material.disponivel}</td>
										<td class="px-3 py-2 text-right">
											<span
												class="inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[11px] font-medium {falta
													? 'bg-danger/10 text-danger'
													: 'bg-success/10 text-success'}"
											>
												<Icon name={falta ? 'alert-triangle' : 'check'} class="h-3 w-3" />
												{falta ? 'Faltante' : 'Suficiente'}
											</span>
										</td>
									</tr>
								{/each}
							</tbody>
						</table>
					</div>

					{#if !canEditProducao}
						<p class="text-[11px] text-muted">
							Ações de saída de material ficam disponíveis apenas para quem pode editar produção.
						</p>
					{/if}
				{/if}
			</section>

		<!-- aba Tarefas -->
		{:else}
			<section class="space-y-3">
				<div>
					<h2 class="text-sm font-semibold text-ink">Tarefas do projeto</h2>
					<p class="mt-1 text-xs text-muted">Tarefas vinculadas a este projeto.</p>
				</div>

				{#if erroTarefas}
					<ErrorBanner
						message="Não foi possível carregar as tarefas"
						hint={erroTarefas}
						onRetry={() => void goto(`/producao/projetos/${projeto.id}?tab=tarefas`, { invalidateAll: true })}
					/>
				{:else if tarefas.length === 0}
					<div class="rounded-xl border border-border bg-surface">
						<EmptyState
							icon="stack"
							title="Nenhuma tarefa neste projeto"
							description="Crie tarefas no quadro Kanban e vincule a este projeto."
						/>
					</div>
				{:else}
					<div class="overflow-x-auto rounded-xl border border-border bg-surface">
						<table class="w-full min-w-[600px]">
							<thead class="border-b border-border bg-elevated/40">
								<tr>
									<th class={thCls}>Código</th>
									<th class={thCls}>Título</th>
									<th class={thCls}>Responsável</th>
									<th class={thCls}>Prioridade</th>
									<th class={thCls}>Prazo</th>
									<th class="px-3 py-2 text-right text-[11px] font-semibold uppercase tracking-wide text-muted">Status</th>
								</tr>
							</thead>
							<tbody>
								{#each tarefas as tarefa (tarefa.id)}
									<tr class="border-b border-border last:border-0">
										<td class={`${tdCls} font-mono text-xs text-muted`}>{tarefa.codigo}</td>
										<td class={tdCls}>{tarefa.titulo}</td>
										<td class={tdCls}>
											<span class="flex items-center gap-1.5">
												<Avatar name={tarefa.responsavel.nome} size="xs" />
												{tarefa.responsavel.nome}
											</span>
										</td>
										<td class={tdCls}>
											<span class={prioridadeChip(tarefa.prioridade)}>
												{tarefa.prioridade}
											</span>
										</td>
										<td class={`${tdCls} tabular-nums`}>{formatarData(tarefa.prazo)}</td>
										<td class="px-3 py-2 text-right">
											<StatusBadge {...TAREFA_STATUS_META[tarefa.status]} />
										</td>
									</tr>
								{/each}
							</tbody>
						</table>
					</div>
				{/if}
			</section>
		{/if}
	{/if}

	{#if projeto}
		<ModalSaidaProjeto
			open={modalSaida}
			projeto={{ id: projeto.id, codigo: projeto.codigo, nome: projeto.nome }}
			materiais={materiais}
			onClose={() => (modalSaida = false)}
			onRegistrado={aoRegistrado}
		/>
	{/if}
</div>

{#if modalNota}
	<Modal
		open={modalNota !== ''}
		title="Contrato pendente"
		onClose={() => (modalNota = '')}
		width="sm"
	>
		{#snippet children()}
			<p class="text-sm text-muted">{modalNota}</p>
		{/snippet}
		{#snippet footer()}
			<button
				type="button"
				onclick={() => (modalNota = '')}
				class="w-full rounded-md bg-brand px-4 py-2 text-sm font-medium text-white transition hover:bg-brandhi"
			>
				Entendi
			</button>
		{/snippet}
	</Modal>
{/if}