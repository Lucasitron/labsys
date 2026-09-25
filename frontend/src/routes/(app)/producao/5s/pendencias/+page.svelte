<script lang="ts">
	import { goto, invalidateAll } from '$app/navigation';
	import type { PageProps } from './$types';
	import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte';
	import EmptyState from '$lib/components/ui/EmptyState.svelte';
	import PageHeader from '$lib/components/ui/PageHeader.svelte';
	import StatusBadge from '$lib/components/ui/StatusBadge.svelte';
	import Dropdown from '$lib/components/ui/Dropdown.svelte';
	import ModalDetalhePendencia from '$lib/components/producao/ModalDetalhePendencia.svelte';
	import { GRAVIDADE_META, PENDENCIA_STATUS_META, gravidadeMeta } from '$lib/utils/producao-status';
	import { isResponsavelAtribuido } from '$lib/utils/permissions';
	import type { Pendencia5S } from '$lib/types/producao';

	const kpiCls = 'bg-elevated border border-border rounded-xl p-4';
	const cellCls = 'px-5 py-3 text-sm whitespace-nowrap';
	const thCls = 'px-5 py-3 text-left text-[11px] font-semibold uppercase tracking-wider text-muted';

	let { data }: PageProps = $props();

	let detalhe = $state<Pendencia5S | null>(null);

	const params = $derived(data.params);
	const hoje = new Date().toISOString().slice(0, 10);

	const loading = $derived(!data.result && !data.error);
	const hasError = $derived(Boolean(data.error));
	const pendencias = $derived(data.result?.dados ?? []);
	const filtradas = $derived(
		pendencias.filter((p) => {
			if (params.gravidade.length && !params.gravidade.includes(p.gravidade)) return false;
			if (params.setor.length && !params.setor.includes(p.setor.id)) return false;
			if (params.situacao.length && !params.situacao.includes(p.status)) return false;
			return true;
		})
	);
	const empty = $derived(!loading && !hasError && filtradas.length === 0);
	const emptyFiltrado = $derived(Boolean(data.result && data.result.dados.length > 0 && empty));

	const setoresOpcoes = $derived(
		[...new Map(pendencias.map((p) => [p.setor.id, p.setor])).values()]
	);

	const kpis = $derived({
		total: pendencias.length,
		leves: pendencias.filter((p) => p.gravidade === 'Leve').length,
		moderadas: pendencias.filter((p) => p.gravidade === 'Moderada').length,
		graves: pendencias.filter((p) => p.gravidade === 'Grave').length
	});

	interface Query {
		[key: string]: string | string[] | number | undefined;
	}

	function navegar(overrides: Query): string {
		const base: Query = {
			gravidade: params.gravidade,
			setor: params.setor,
			situacao: params.situacao,
			page: params.page,
			pageSize: params.pageSize
		};
		const merged = { ...base, ...overrides };
		const url = new URLSearchParams();
		for (const [chave, valor] of Object.entries(merged)) {
			if (Array.isArray(valor)) {
				for (const v of valor) if (v) url.append(chave, v);
			} else if (valor !== undefined && valor !== '') {
				url.append(chave, String(valor));
			}
		}
		const qs = url.toString();
		return `/producao/5s/pendencias${qs ? `?${qs}` : ''}`;
	}

	function toggleFiltro(atual: string[], valor: string): string[] {
		return atual.includes(valor) ? atual.filter((v) => v !== valor) : [...atual, valor];
	}

	function canResolve(pendencia: Pendencia5S) {
		return (
			data.canEditProducao ||
			isResponsavelAtribuido(data.user, { responsavelId: pendencia.responsavel.id })
		);
	}

	function formatarData(iso: string) {
		return new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short' }).format(
			new Date(`${iso}T00:00:00`)
		);
	}

	function vencida(pendencia: Pendencia5S) {
		return pendencia.status === 'Aberta' && pendencia.prazo < hoje;
	}
</script>

<svelte:head><title>Pendências 5S · Produção</title></svelte:head>

<section class="space-y-5">
	<PageHeader
		title="Pendências 5S"
		subtitle="Não conformidades apontadas nas auditorias, com responsável e prazo."
	/>

	{#if loading}
		<div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
			{#each Array(4) as _, i}
				<div
					class="h-24 bg-elevated border border-border rounded-xl animate-pulse"
					role="status"
					aria-label="Carregando indicadores {i + 1} de 4"
				></div>
			{/each}
		</div>
		<div class="h-52 bg-elevated border border-border rounded-xl animate-pulse" role="status">
			<span class="sr-only">Carregando pendências...</span>
		</div>
	{:else if hasError}
		<ErrorBanner
			message={data.error ?? 'Não foi possível carregar as pendências'}
			onRetry={() => invalidateAll()}
		/>
	{:else}
		<div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4" data-testid="pend-kpis">
			<div class={kpiCls}>
				<span class="text-xs text-muted">Pendências</span>
				<div class="mt-1 text-2xl font-semibold tabnums">{kpis.total}</div>
			</div>
			<div class={kpiCls}>
				<span class="text-xs text-muted">Leves</span>
				<div class="mt-1 text-2xl font-semibold tabnums text-warn">{kpis.leves}</div>
			</div>
			<div class={kpiCls}>
				<span class="text-xs text-muted">Moderadas</span>
				<div class="mt-1 text-2xl font-semibold tabnums text-brand">{kpis.moderadas}</div>
			</div>
			<div class={`${kpiCls} ${kpis.graves ? 'border-danger/40' : ''}`}>
				<span class="text-xs text-muted">Graves</span>
				<div class="mt-1 text-2xl font-semibold tabnums {kpis.graves ? 'text-danger' : ''}">
					{kpis.graves}
				</div>
			</div>
		</div>

		<div class="rounded-lg bg-elevated/40 border border-border p-3 text-xs text-muted">
			<b class="text-ink">Regra</b>
			pendência grave não resolvida no prazo → <b class="text-ink">aviso</b> após 5 dias úteis; segunda
			ocorrência grava uma <b class="text-ink">penalidade</b>.
		</div>

		<div class="flex items-center gap-3 flex-wrap">
			<Dropdown
				label="Gravidade"
				options={Object.entries(GRAVIDADE_META).map(([id, m]) => ({
					id,
					label: m.label,
					count: pendencias.filter((p) => p.gravidade === id).length
				}))}
				selected={params.gravidade}
				onToggle={(id) => goto(navegar({ gravidade: toggleFiltro(params.gravidade, id), page: 1 }))}
				onClear={() => goto(navegar({ gravidade: [], page: 1 }))}
			/>
			<Dropdown
				label="Setor"
				options={setoresOpcoes.map((s) => ({
					id: s.id,
					label: s.nome,
					count: pendencias.filter((p) => p.setor.id === s.id).length
				}))}
				selected={params.setor}
				onToggle={(id) => goto(navegar({ setor: toggleFiltro(params.setor, id), page: 1 }))}
				onClear={() => goto(navegar({ setor: [], page: 1 }))}
			/>
			<Dropdown
				label="Situação"
				options={Object.entries(PENDENCIA_STATUS_META).map(([id, m]) => ({
					id,
					label: m.label,
					count: pendencias.filter((p) => p.status === id).length
				}))}
				selected={params.situacao}
				onToggle={(id) => goto(navegar({ situacao: toggleFiltro(params.situacao, id), page: 1 }))}
				onClear={() => goto(navegar({ situacao: [], page: 1 }))}
			/>
		</div>

		{#if empty}
			{#if emptyFiltrado}
				<EmptyState
					icon="search"
					title="Nenhuma pendência para esses filtros"
					description="Ajuste os filtros para ver outras pendências."
				>
					<button
						type="button"
						class="bg-brand hover:bg-brandhi text-white px-4 py-2 rounded-md text-sm font-medium transition"
						onclick={() => goto(navegar({ gravidade: [], setor: [], situacao: [], page: 1 }))}
					>
						Limpar filtros
					</button>
				</EmptyState>
			{:else}
				<EmptyState
					icon="document"
					title="Nenhuma pendência"
					description="Quando as auditorias apontarem não conformidades, elas aparecerão aqui."
				/>
			{/if}
		{:else}
			<div class="bg-elevated border border-border rounded-xl overflow-x-auto">
				<table class="w-full text-sm">
					<thead>
						<tr class="border-b border-border">
							<th class={thCls}>Gravidade</th>
							<th class={thCls}>Pendência</th>
							<th class={thCls}>Responsável</th>
							<th class={thCls}>Prazo</th>
							<th class={thCls}>Situação</th>
							<th class={thCls}><span class="sr-only">Ações</span></th>
						</tr>
					</thead>
					<tbody class="divide-y divide-border">
						{#each filtradas as pendencia (pendencia.id)}
							{@const grave = gravidadeMeta(pendencia.gravidade)}
							<tr data-testid="pend-row">
								<td class={cellCls}>
									<span
										class={`inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-medium ${
											pendencia.gravidade === 'Leve'
												? 'border-warn/30 bg-warn/15 text-warn'
												: pendencia.gravidade === 'Moderada'
													? 'border-brand/30 bg-brand/15 text-brandhi'
													: 'border-danger/30 bg-danger/15 text-danger'
										}`}
									>
										{grave.label}
									</span>
								</td>
								<td class={cellCls}>
									<button
										type="button"
										class="text-brand hover:text-brandhi transition text-left font-medium"
										onclick={() => (detalhe = pendencia)}
									>
										{pendencia.titulo}
									</button>
									<div class="text-[11px] text-muted">{pendencia.setor.nome}</div>
								</td>
								<td class={cellCls}>{pendencia.responsavel.nome}</td>
								<td class={cellCls}>
									<span class={vencida(pendencia) ? 'text-danger font-medium' : ''}>
										{formatarData(pendencia.prazo)}
									</span>
									{#if vencida(pendencia)}
										<span class="text-[11px] text-danger block">prazo vencido</span>
									{/if}
								</td>
								<td class={cellCls}>
									<StatusBadge
										label={PENDENCIA_STATUS_META[pendencia.status].label}
										color={PENDENCIA_STATUS_META[pendencia.status].color}
									/>
								</td>
								<td class={`${cellCls} text-right`}>
									<div class="flex items-center justify-end gap-3">
										<button
											type="button"
											class="text-muted hover:text-ink text-xs transition"
											onclick={() => (detalhe = pendencia)}
										>
											Detalhes
										</button>
										{#if pendencia.status === 'Aberta' && canResolve(pendencia)}
											<button
												type="button"
												data-testid="pend-resolver"
												class="text-brand hover:text-brandhi text-xs font-medium transition"
												onclick={() => (detalhe = pendencia)}
											>
												Resolver
											</button>
										{/if}
										{#if data.canEditProducao && pendencia.status === 'Aberta'}
											<button
												type="button"
												data-testid="pend-penalidade"
												class="text-danger hover:text-danger/70 text-xs font-medium transition"
												onclick={() => (detalhe = pendencia)}
											>
												Penalidade
											</button>
										{/if}
									</div>
								</td>
							</tr>
						{/each}
					</tbody>
				</table>
			</div>
			<p class="text-xs text-muted">{filtradas.length} de {pendencias.length} pendências</p>
		{/if}
	{/if}
</section>

{#if detalhe}
	<ModalDetalhePendencia
		open={true}
		pendencia={detalhe}
		isAdmin={data.canEditProducao}
		canResolve={canResolve(detalhe)}
		onClose={() => (detalhe = null)}
		onResolvida={() => {
			detalhe = null;
			invalidateAll();
		}}
		onPenalidadeRegistrada={() => {
			detalhe = null;
			invalidateAll();
		}}
	/>
{/if}