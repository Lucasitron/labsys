<script lang="ts">
	import { goto, invalidateAll } from '$app/navigation';
	import type { PageProps } from './$types';
	import type { ChamadoStatus, Maquina, NomeFunc } from '$lib/types/producao';
	import {
		CARTAO_META,
		CHAMADO_STATUS_META,
		MAQUINA_STATUS_META,
		PREVENTIVA_STATUS_META
	} from '$lib/utils/producao-status';
	import {
		abrirChamadoMaquina,
		atualizarStatusMaquina,
		criarPreventivaMaquina
	} from '$lib/api/producao/client';
	import { ApiError, NetworkError } from '$lib/api/client';
	import { toasts, toastError } from '$lib/stores/toast';
	import { ABAS_MAQUINA } from './+page';
	import CartaoBadge from '$lib/components/producao/CartaoBadge.svelte';
	import PageHeader from '$lib/components/ui/PageHeader.svelte';
	import StatusBadge from '$lib/components/ui/StatusBadge.svelte';
	import Avatar from '$lib/components/ui/Avatar.svelte';
	import Skeleton from '$lib/components/ui/Skeleton.svelte';
	import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte';
	import EmptyState from '$lib/components/ui/EmptyState.svelte';
	import Tabs from '$lib/components/ui/Tabs.svelte';
	import Modal from '$lib/components/ui/Modal.svelte';
	import Select from '$lib/components/ui/Select.svelte';
	import Icon from '$lib/components/ui/Icon.svelte';

	let { data }: PageProps = $props();

	const canEditProducao = $derived(data.canEditProducao ?? false);

	const maquina = $derived(data.maquina as Maquina | null);
	const preventivas = $derived(data.preventivas);
	const chamados = $derived(data.chamados);
	const aba = $derived(data.aba);
	const erroPagina = $derived(data.erroPagina);
	const erroPreventivas = $derived(data.erroPreventivas);
	const erroChamados = $derived(data.erroChamados);

	const loading = $derived(maquina === null && erroPagina === null);

	const responsaveis = $derived.by(() => {
		const mapa = new Map<string, NomeFunc>();
		if (maquina) mapa.set(maquina.responsavelManutencao.id, maquina.responsavelManutencao);
		for (const chamado of chamados) mapa.set(chamado.responsavel.id, chamado.responsavel);
		for (const prev of preventivas) mapa.set(prev.maquina.id, {
			id: prev.maquina.id,
			nome: prev.maquina.nome
		});
		return [...mapa.values()];
	});

	const tabs = $derived(
		ABAS_MAQUINA.map((a) => ({
			id: a.id,
			label: a.label,
			count:
				a.id === 'chamados'
					? chamados.length || undefined
					: a.id === 'manutencao'
						? preventivas.length || undefined
						: undefined
		}))
	);

	let modalChamado = $state(false);
	let modalPreventiva = $state(false);
	let modalNota = $state('');

	// ---- abrir chamado ----
	let chamadoClassificacao = $state('');
	let chamadoRelato = $state('');
	let chamadoResponsavelId = $state('');
	let chamadoEnviando = $state(false);
	let chamadoErro = $state('');

	// ---- agendar preventiva ----
	let preventivaData = $state('');
	let preventivaEnviando = $state(false);
	let preventivaErro = $state('');

	$effect(() => {
		if (modalChamado) {
			chamadoClassificacao = '';
			chamadoRelato = '';
			chamadoResponsavelId = '';
			chamadoErro = '';
		}
		if (modalPreventiva) {
			preventivaData = '';
			preventivaErro = '';
		}
	});

	const classificacoes = ['Elétrica', 'Mecânica', 'Software', 'Peça de reposição', 'Segurança', 'Outro'];

	function irParaAba(id: string): void {
		void goto(`/producao/maquinas/${maquina?.id ?? ''}?tab=${id}`, { invalidateAll: true });
	}

	async function salvarChamado(event: SubmitEvent): Promise<void> {
		event.preventDefault();
		if (!maquina) return;
		if (!chamadoClassificacao || !chamadoRelato.trim() || !chamadoResponsavelId) {
			chamadoErro = 'Preencha classificação, relato e responsável.';
			return;
		}
		chamadoEnviando = true;
		chamadoErro = '';
		try {
			await abrirChamadoMaquina(maquina.id, {
				classificacao: chamadoClassificacao,
				relato: chamadoRelato.trim(),
				responsavelId: chamadoResponsavelId
			});
			toasts.success('Chamado aberto');
			modalChamado = false;
			await invalidateAll();
		} catch (err) {
			chamadoErro =
				err instanceof ApiError || err instanceof NetworkError
					? err.message
					: 'Não foi possível abrir o chamado.';
		} finally {
			chamadoEnviando = false;
		}
	}

	async function salvarPreventiva(event: SubmitEvent): Promise<void> {
		event.preventDefault();
		if (!maquina) return;
		if (!preventivaData) {
			preventivaErro = 'Informe a data programada.';
			return;
		}
		preventivaEnviando = true;
		preventivaErro = '';
		try {
			await criarPreventivaMaquina(maquina.id, { dataProgramada: preventivaData });
			toasts.success('Preventiva agendada');
			modalPreventiva = false;
			await invalidateAll();
		} catch (err) {
			preventivaErro =
				err instanceof ApiError || err instanceof NetworkError
					? err.message
					: 'Não foi possível agendar a preventiva.';
		} finally {
			preventivaEnviando = false;
		}
	}

	async function alternarSuspensao(): Promise<void> {
		if (!maquina) return;
		try {
			if (maquina.status === 'Suspensa') {
				await atualizarStatusMaquina(maquina.id, { status: 'Disponível' });
				toasts.success('Máquina liberada');
			} else {
				await atualizarStatusMaquina(maquina.id, { status: 'Suspensa' });
				toasts.success('Máquina suspensa');
			}
			await invalidateAll();
		} catch (err) {
			toastError(err, 'Não foi possível atualizar o status da máquina');
		}
	}

	function semContrato(tipo: string): void {
		modalNota = `Ação "${tipo}" depende de endpoint ainda pendente no backend (🟡). Fale com o Admin.`;
	}

	function formatarData(iso?: string): string {
		if (!iso) return '—';
		const [a, m, d] = iso.slice(0, 10).split('-');
		return d ? `${d}/${m}/${a}` : '—';
	}

	function pendenteCompra(chamado: { status: ChamadoStatus; pendenteCompra?: boolean }): boolean {
		return chamado.status === 'Aguardando compra' || chamado.pendenteCompra === true;
	}

	const inputCls =
		'w-full rounded-lg border border-border bg-elevated px-3 py-2.5 text-sm text-ink placeholder:text-muted/60 focus:border-brand focus:outline-none focus:ring-1 focus:ring-brand/40';
	const thCls = 'px-3 py-2 text-left text-[11px] font-semibold uppercase tracking-wide text-muted';
	const tdCls = 'px-3 py-2 text-sm text-ink';
</script>

<svelte:head>
	<title>{maquina ? maquina.nome : 'Máquina'} — Produção — FabLab</title>
</svelte:head>

<div class="space-y-4">
	{#if loading}
		<div class="space-y-4">
			<Skeleton class="h-8 w-2/3" />
			<Skeleton class="h-24 w-full" />
			<Skeleton class="h-24 w-full" />
		</div>
	{:else if !maquina}
		<ErrorBanner
			message="Não foi possível carregar a máquina"
			hint={erroPagina ?? ''}
			onRetry={() => void goto(`/producao/maquinas`, { invalidateAll: true })}
		/>
	{:else}
		<PageHeader
			title={maquina.nome}
			subtitle={`${maquina.codigo} · ${maquina.categoria}`}
			backHref="/producao/maquinas"
		>
			{#snippet children()}
				<StatusBadge {...MAQUINA_STATUS_META[maquina.status]} />
				<CartaoBadge cartao={maquina.cartao} />
				{#if canEditProducao}
					<button
						type="button"
						data-testid="maq-chamado"
						onclick={() => (modalChamado = true)}
						class="inline-flex items-center gap-1.5 rounded-lg border border-border bg-surface px-3 py-2 text-sm font-medium text-ink transition hover:border-brand/50 hover:text-brandhi"
					>
						<Icon name="alert-triangle" class="h-4 w-4" /> Abrir chamado
					</button>
					<button
						type="button"
						data-testid="maq-suspender"
						onclick={() => void alternarSuspensao()}
						class="inline-flex items-center gap-1.5 rounded-lg bg-danger px-3 py-2 text-sm font-semibold text-white shadow-lg shadow-danger/20 transition hover:bg-danger/90"
					>
						<Icon name={maquina.status === 'Suspensa' ? 'check' : 'warning'} class="h-4 w-4" />
						{maquina.status === 'Suspensa' ? 'Liberar' : 'Suspender'}
					</button>
				{/if}
			{/snippet}
		</PageHeader>

		<Tabs tabs={tabs} active={aba} onChange={irParaAba} />

		<!-- aba Geral -->
		{#if aba === 'geral'}
			<section data-testid="maq-tab-geral" class="rounded-xl border border-border bg-surface p-4">
				<dl class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
					<div>
						<dt class="text-xs text-muted">Categoria</dt>
						<dd class="mt-1.5 text-sm capitalize text-ink">{maquina.categoria}</dd>
					</div>
					<div>
						<dt class="text-xs text-muted">Responsável de manutenção</dt>
						<dd class="mt-1.5 flex items-center gap-2">
							<Avatar name={maquina.responsavelManutencao.nome} size="sm" />
							<span class="text-sm text-ink">{maquina.responsavelManutencao.nome}</span>
						</dd>
					</div>
					<div>
						<dt class="text-xs text-muted">Cartão 5S</dt>
						<dd class="mt-1.5"><CartaoBadge cartao={maquina.cartao} /></dd>
					</div>
				</dl>
			</section>

		<!-- aba Documentação -->
		{:else if aba === 'documentacao'}
			<section class="rounded-xl border border-border bg-surface p-4">
				<h2 class="text-sm font-semibold text-ink">Documentação da máquina</h2>
				<p class="mt-1 max-w-xl text-xs text-muted">
					Manuais, ficha técnica, firmware e documentação de segurança.
				</p>
				<div class="mt-4 rounded-lg border border-warn/30 bg-warn/10 p-3 text-xs text-muted">
					Leitura/envio de documentos depende do endpoint <span class="font-mono">/producao/maquinas/:id/documentos</span> —
					contrato pendente (🟡).
				</div>
				<div class="mt-4 rounded-lg border border-border bg-elevated/30 p-4">
					<EmptyState
						icon="document"
						title="Nenhum documento disponível"
						description="Quando o contrato estiver ativo, os manuais e arquivos aparecerão aqui."
					/>
				</div>
			</section>

		<!-- aba Receitas -->
		{:else if aba === 'receitas'}
			<section class="rounded-xl border border-border bg-surface p-4">
				<h2 class="text-sm font-semibold text-ink">Receitas e usos</h2>
				<p class="mt-1 max-w-xl text-xs text-muted">
					Relatório consolidado de usos da máquina por receita.
				</p>
				<div class="mt-4 rounded-lg border border-warn/30 bg-warn/10 p-3 text-xs text-muted">
					Relatório de usos depende do endpoint de relatório — contrato pendente (🟡).
				</div>
				<div class="mt-4 rounded-lg border border-border bg-elevated/30 p-4">
					<EmptyState
						icon="chart"
						title="Sem relatório disponível"
						description="O consolidado de receitas/usos aparecerá quando o contrato estiver ativo."
					/>
				</div>
			</section>

		<!-- aba Pré-flight -->
		{:else if aba === 'preflight'}
			<section class="rounded-xl border border-border bg-surface p-4">
				<h2 class="text-sm font-semibold text-ink">Pré-flight</h2>
				<p class="mt-1 max-w-xl text-xs text-muted">
					Checklist de segurança antes do primeiro uso do dia.
				</p>
				<div class="mt-4 rounded-lg border border-warn/30 bg-warn/10 p-3 text-xs text-muted">
					Checklists de pré-flight dependem de endpoint próprio — contrato pendente (🟡).
				</div>
				<div class="mt-4 rounded-lg border border-border bg-elevated/30 p-4">
					<EmptyState
						icon="stack"
						title="Sem checklists cadastrados"
						description="Os checklists de pré-flight aparecerão quando o contrato estiver ativo."
					/>
				</div>
			</section>

		<!-- aba Manutenção -->
		{:else if aba === 'manutencao'}
			<section class="space-y-3">
				<div class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
					<div>
						<h2 class="text-sm font-semibold text-ink">Manutenções preventivas</h2>
						<p class="mt-1 text-xs text-muted">
							Manutenções programadas e histórico de manutenções da máquina.
						</p>
					</div>
					{#if canEditProducao}
						<button
							type="button"
							data-testid="maq-preventiva"
							onclick={() => (modalPreventiva = true)}
							class="inline-flex items-center gap-1.5 rounded-lg bg-brand px-3 py-2 text-sm font-semibold text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi"
						>
							<Icon name="calendar" class="h-4 w-4" /> Agendar preventiva
						</button>
					{/if}
				</div>

				{#if erroPreventivas}
					<ErrorBanner
						message="Não foi possível carregar as preventivas"
						hint={erroPreventivas}
						onRetry={() => void goto(`/producao/maquinas/${maquina.id}?tab=manutencao`, { invalidateAll: true })}
					/>
				{:else if preventivas.length === 0}
					<div class="rounded-xl border border-border bg-surface">
						<EmptyState
							icon="calendar"
							title="Nenhuma preventiva agendada"
							description="Agende a primeira manutenção preventiva desta máquina."
						/>
					</div>
				{:else}
					<div class="overflow-x-auto rounded-xl border border-border bg-surface">
						<table class="w-full min-w-[540px]">
							<thead class="border-b border-border bg-elevated/40">
								<tr>
									<th class={thCls}>Código</th>
									<th class={thCls}>Data programada</th>
									<th class="px-3 py-2 text-right text-[11px] font-semibold uppercase tracking-wide text-muted">Status</th>
								</tr>
							</thead>
							<tbody>
								{#each preventivas as prev (prev.id)}
									<tr class="border-b border-border last:border-0">
										<td class={`${tdCls} font-mono text-xs text-muted`}>{prev.codigo}</td>
										<td class={`${tdCls} tabular-nums`}>{formatarData(prev.dataProgramada)}</td>
										<td class="px-3 py-2 text-right">
											<StatusBadge {...PREVENTIVA_STATUS_META[prev.status]} />
										</td>
									</tr>
								{/each}
							</tbody>
						</table>
					</div>
				{/if}

				{#if !canEditProducao}
					<p class="text-[11px] text-muted">
						Agendar preventivas fica disponível apenas para quem pode editar produção.
					</p>
				{/if}
			</section>

		<!-- aba Chamados -->
		{:else}
			<section class="space-y-3">
				<div class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
					<div>
						<h2 class="text-sm font-semibold text-ink">Chamados de manutenção</h2>
						<p class="mt-1 text-xs text-muted">
							Ocorrências abertas para esta máquina e seu acompanhamento.
						</p>
					</div>
					{#if canEditProducao}
						<button
							type="button"
							data-testid="maq-chamado"
							onclick={() => (modalChamado = true)}
							class="inline-flex items-center gap-1.5 rounded-lg bg-brand px-3 py-2 text-sm font-semibold text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi"
						>
							<Icon name="plus" class="h-4 w-4" /> Abrir chamado
						</button>
					{/if}
				</div>

				{#if erroChamados}
					<ErrorBanner
						message="Não foi possível carregar os chamados"
						hint={erroChamados}
						onRetry={() => void goto(`/producao/maquinas/${maquina.id}?tab=chamados`, { invalidateAll: true })}
					/>
				{:else if chamados.length === 0}
					<div class="rounded-xl border border-border bg-surface">
						<EmptyState
							icon="alert-triangle"
							title="Nenhum chamado aberto"
							description="Quando houver ocorrências de manutenção, elas aparecerão aqui."
						/>
					</div>
				{:else}
					<div class="grid grid-cols-1 gap-3 md:grid-cols-2">
						{#each chamados as chamado (chamado.id)}
							<article class="rounded-xl border border-border bg-surface p-4">
								<div class="flex items-start justify-between gap-2">
									<h3 class="font-mono text-xs text-muted">{chamado.codigo}</h3>
									<StatusBadge {...CHAMADO_STATUS_META[chamado.status]} />
								</div>
								<h4 class="mt-1 text-sm font-semibold text-ink">{chamado.classificacao}</h4>
								<p class="mt-1 text-sm leading-relaxed text-muted">{chamado.relato}</p>
								<div class="mt-3 flex flex-wrap items-center gap-2">
									<span class="inline-flex items-center gap-1.5 rounded-full border border-border bg-elevated/40 px-2 py-0.5 text-[11px] text-ink">
										<Avatar name={chamado.responsavel.nome} size="xs" />
										{chamado.responsavel.nome}
									</span>
									{#if pendenteCompra(chamado)}
										<span class="inline-flex items-center gap-1 rounded-full bg-warn/10 px-2 py-0.5 text-[11px] font-medium text-warn">
											<Icon name="shopping-bag" class="h-3 w-3" /> Aguardando compra
										</span>
									{/if}
								</div>
							</article>
						{/each}
					</div>
				{/if}

				{#if !canEditProducao}
					<p class="text-[11px] text-muted">
						Abrir chamados fica disponível apenas para quem pode editar produção.
					</p>
				{/if}
			</section>
		{/if}
	{/if}
</div>

{#if maquina}
	<Modal
		open={modalChamado}
		title="Abrir chamado de manutenção"
		subtitle={maquina.nome}
		onClose={chamadoEnviando ? undefined : () => (modalChamado = false)}
		width="md"
	>
		{#snippet children()}
			<form id="modal-chamado-form" onsubmit={salvarChamado} novalidate>
				{#if chamadoErro}
					<div role="alert" class="mb-4 rounded-xl border border-danger/30 bg-danger/10 px-4 py-3 text-sm text-danger">
						{chamadoErro}
					</div>
				{/if}

				<label class="block">
					<span class="mb-1.5 block text-xs font-medium text-muted">Classificação *</span>
					<select bind:value={chamadoClassificacao} class={inputCls}>
						<option value="" disabled>Selecione…</option>
						{#each classificacoes as cls (cls)}
							<option value={cls}>{cls}</option>
						{/each}
					</select>
				</label>

				<label class="mt-4 block">
					<span class="mb-1.5 block text-xs font-medium text-muted">Relato da ocorrência *</span>
					<textarea
						bind:value={chamadoRelato}
						rows="3"
						placeholder="Descreva o que aconteceu…"
						class={`${inputCls} resize-none`}
					></textarea>
				</label>

				<label class="mt-4 block">
					<span class="mb-1.5 block text-xs font-medium text-muted">Responsável *</span>
					<select bind:value={chamadoResponsavelId} class={inputCls}>
						<option value="" disabled>Selecione…</option>
						{#each responsaveis as resp (resp.id)}
							<option value={resp.id}>{resp.nome}</option>
						{/each}
					</select>
					<p class="mt-1 text-[11px] text-muted">
						Lista limitada ao contexto atual — a busca completa virá do RH (contrato pendente).
					</p>
				</label>
			</form>
		{/snippet}
		{#snippet footer()}
			<button
				type="button"
				onclick={() => (modalChamado = false)}
				disabled={chamadoEnviando}
				class="rounded-md px-4 py-2 text-sm text-muted transition hover:text-ink disabled:opacity-50"
			>
				Cancelar
			</button>
			<button
				type="submit"
				form="modal-chamado-form"
				disabled={chamadoEnviando}
				class="rounded-md bg-brand px-4 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi disabled:cursor-not-allowed disabled:opacity-50"
			>
				{chamadoEnviando ? 'Abrindo…' : 'Abrir chamado'}
			</button>
		{/snippet}
	</Modal>

	<Modal
		open={modalPreventiva}
		title="Agendar manutenção preventiva"
		subtitle={maquina.nome}
		onClose={preventivaEnviando ? undefined : () => (modalPreventiva = false)}
		width="md"
	>
		{#snippet children()}
			<form id="modal-preventiva-form" onsubmit={salvarPreventiva} novalidate>
				{#if preventivaErro}
					<div role="alert" class="mb-4 rounded-xl border border-danger/30 bg-danger/10 px-4 py-3 text-sm text-danger">
						{preventivaErro}
					</div>
				{/if}
				<label class="block">
					<span class="mb-1.5 block text-xs font-medium text-muted">Data programada *</span>
					<input type="date" bind:value={preventivaData} class={inputCls} />
				</label>
			</form>
		{/snippet}
		{#snippet footer()}
			<button
				type="button"
				onclick={() => (modalPreventiva = false)}
				disabled={preventivaEnviando}
				class="rounded-md px-4 py-2 text-sm text-muted transition hover:text-ink disabled:opacity-50"
			>
				Cancelar
			</button>
			<button
				type="submit"
				form="modal-preventiva-form"
				disabled={preventivaEnviando}
				class="rounded-md bg-brand px-4 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi disabled:cursor-not-allowed disabled:opacity-50"
			>
				{preventivaEnviando ? 'Agendando…' : 'Agendar'}
			</button>
		{/snippet}
	</Modal>
{/if}

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