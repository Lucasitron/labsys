<script lang="ts">
	import { goto, invalidateAll } from '$app/navigation';
	import type { PageProps } from './$types';
	import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte';
	import EmptyState from '$lib/components/ui/EmptyState.svelte';
	import PageHeader from '$lib/components/ui/PageHeader.svelte';
	import StatusBadge from '$lib/components/ui/StatusBadge.svelte';
	import Dropdown from '$lib/components/ui/Dropdown.svelte';
	import Avatar from '$lib/components/ui/Avatar.svelte';
	import { PENALIDADE_TIPO_META, PENALIDADE_STATUS_META } from '$lib/utils/producao-status';
	import { criarPenalidade5S } from '$lib/api/producao/client';
	import type { PenalidadeTipo } from '$lib/types/producao';

	const kpiCls = 'bg-elevated border border-border rounded-xl p-4';
	const cellCls = 'px-5 py-3 text-sm whitespace-nowrap';
	const thCls = 'px-5 py-3 text-left text-[11px] font-semibold uppercase tracking-wider text-muted';

	const TIPOS: PenalidadeTipo[] = ['Aviso', 'Advertência', 'Suspensão', 'Expulsão'];

	let { data }: PageProps = $props();

	let registrando = $state(false);
	let salvarPenalidade = $state(false);
	let erroPenalidade = $state<string | null>(null);
	let membroSelecionado = $state('');
	let tipoSelecionado = $state<PenalidadeTipo>('Aviso');
	let motivo = $state('');

	const params = $derived(data.params);

	const loading = $derived(!data.result && !data.error);
	const hasError = $derived(Boolean(data.error));
	const penalidades = $derived(data.result?.dados ?? []);
	const filtradas = $derived(
		penalidades.filter((p) => {
			if (params.tipo.length && !params.tipo.includes(p.tipo)) return false;
			if (params.situacao.length && !params.situacao.includes(p.status)) return false;
			return true;
		})
	);
	const empty = $derived(!loading && !hasError && filtradas.length === 0);
	const emptyFiltrado = $derived(Boolean(data.result && data.result.dados.length > 0 && empty));

	const membros = $derived([...new Map(penalidades.map((p) => [p.membro.id, p.membro])).values()]);

	const kpis = $derived({
		aviso: penalidades.filter((p) => p.tipo === 'Aviso' && p.status === 'Ativa').length,
		advertencia: penalidades.filter((p) => p.tipo === 'Advertência' && p.status === 'Ativa').length,
		suspensao: penalidades.filter((p) => p.tipo === 'Suspensão' && p.status === 'Ativa').length,
		expulsao: penalidades.filter((p) => p.tipo === 'Expulsão' && p.status === 'Ativa').length
	});

	interface Query {
		[key: string]: string | string[] | number | undefined;
	}

	function navegar(overrides: Query): string {
		const base: Query = {
			tipo: params.tipo,
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
		return `/producao/5s/advertencias${qs ? `?${qs}` : ''}`;
	}

	function toggleFiltro(atual: string[], valor: string): string[] {
		return atual.includes(valor) ? atual.filter((v) => v !== valor) : [...atual, valor];
	}

	function abrirRegistrar() {
		registrando = true;
		membroSelecionado = membros[0]?.id ?? '';
		tipoSelecionado = 'Aviso';
		motivo = '';
		erroPenalidade = null;
	}

	function fecharRegistrar() {
		registrando = false;
		erroPenalidade = null;
	}

	async function salvarPenalidadeSubmit(event: SubmitEvent) {
		event.preventDefault();
		if (!membroSelecionado) {
			erroPenalidade = 'Selecione um integrante.';
			return;
		}
		if (!motivo.trim()) {
			erroPenalidade = 'Informe o motivo da penalidade.';
			return;
		}
		salvarPenalidade = true;
		erroPenalidade = null;
		try {
			await criarPenalidade5S({ membroId: membroSelecionado, tipo: tipoSelecionado, motivo: motivo.trim() });
			fecharRegistrar();
			await invalidateAll();
		} catch (err) {
			erroPenalidade =
				err instanceof Error ? err.message : 'Não foi possível registrar a penalidade. Tente novamente.';
		} finally {
			salvarPenalidade = false;
		}
	}

	function formatarData(iso: string) {
		return new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short' }).format(
			new Date(`${iso}T00:00:00`)
		);
	}
</script>

<svelte:head><title>Avisos, advertências e suspensões · Produção</title></svelte:head>

<section class="space-y-5">
	<PageHeader
		title="Avisos, advertências e suspensões"
		subtitle="O Sistema sugere e o Admin aplica a penalidade."
	>
		{#if data.canEditProducao}
			<button
				type="button"
				data-testid="pen-registrar"
				class="flex items-center gap-2 bg-elevated hover:bg-elevated/70 border border-border rounded-md px-3 py-2 text-sm transition"
				onclick={abrirRegistrar}
			>
				Registrar penalidade
			</button>
		{/if}
	</PageHeader>

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
			<span class="sr-only">Carregando penalidades...</span>
		</div>
	{:else if hasError}
		<ErrorBanner
			message={data.error ?? 'Não foi possível carregar as penalidades'}
			onRetry={() => invalidateAll()}
		/>
	{:else}
		<div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4" data-testid="pen-kpis">
			<div class={kpiCls}>
				<span class="text-xs text-muted">Avisos ativos</span>
				<div class="mt-1 text-2xl font-semibold tabnums text-warn">{kpis.aviso}</div>
			</div>
			<div class={kpiCls}>
				<span class="text-xs text-muted">Advertências</span>
				<div class="mt-1 text-2xl font-semibold tabnums text-danger">{kpis.advertencia}</div>
			</div>
			<div class={kpiCls}>
				<span class="text-xs text-muted">Suspensões</span>
				<div class="mt-1 text-2xl font-semibold tabnums text-danger">{kpis.suspensao}</div>
			</div>
			<div class={kpiCls}>
				<span class="text-xs text-muted">Expulsões</span>
				<div class="mt-1 text-2xl font-semibold tabnums text-danger">{kpis.expulsao}</div>
			</div>
		</div>

		<div class="bg-surface border border-border rounded-xl p-4 flex items-start gap-3">
			<span class="text-brand shrink-0 mt-0.5" aria-hidden="true">✓</span>
			<div class="text-xs text-muted leading-relaxed">
				<b class="text-ink">Escala de penalidades:</b> pendência grave não resolvida no prazo →{' '}
				<b class="text-ink">aviso</b> · 2 avisos = <b class="text-ink">advertência</b> · 3 advertências
				= <b class="text-ink">suspensão</b> · expulsão só por <b class="text-ink">decisão do Admin</b> em
				reincidência grave. O sistema sugere cada passo na tela; o Admin confirma a aplicação.
			</div>
		</div>

		<div class="flex items-center gap-3 flex-wrap">
			<Dropdown
				label="Tipo"
				options={TIPOS.map((t) => ({
					id: t,
					label: PENALIDADE_TIPO_META[t].label,
					count: penalidades.filter((p) => p.tipo === t).length
				}))}
				selected={params.tipo}
				onToggle={(id) => goto(navegar({ tipo: toggleFiltro(params.tipo, id), page: 1 }))}
				onClear={() => goto(navegar({ tipo: [], page: 1 }))}
			/>
			<Dropdown
				label="Situação"
				options={Object.entries(PENALIDADE_STATUS_META).map(([id, m]) => ({
					id,
					label: m.label,
					count: penalidades.filter((p) => p.status === id).length
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
					title="Nenhuma penalidade para esses filtros"
					description="Ajuste os filtros para ver outras penalidades."
				>
					<button
						type="button"
						class="bg-brand hover:bg-brandhi text-white px-4 py-2 rounded-md text-sm font-medium transition"
						onclick={() => goto(navegar({ tipo: [], situacao: [], page: 1 }))}
					>
						Limpar filtros
					</button>
				</EmptyState>
			{:else}
				<EmptyState
					icon="alert-triangle"
					title="Nenhuma penalidade"
					description="O sistema sugere penalidades quando pendências graves vencem o prazo."
				/>
			{/if}
		{:else}
			<div class="bg-surface border border-border rounded-xl overflow-hidden">
				<div class="overflow-x-auto">
					<table class="w-full text-sm">
						<thead>
							<tr class="border-b border-border">
								<th class={thCls}>Integrante</th>
								<th class={thCls}>Tipo</th>
								<th class={thCls}>Motivo</th>
								<th class={thCls}>Aplicada em</th>
								<th class={thCls}>Situação</th>
							</tr>
						</thead>
						<tbody class="divide-y divide-border">
							{#each filtradas as penalidade (penalidade.id)}
								<tr data-testid="pen-row">
									<td class={cellCls}>
										<div class="flex items-center gap-2">
											<Avatar name={penalidade.membro.nome} size="xs" tone="brand" />
											<span class="font-medium">{penalidade.membro.nome}</span>
										</div>
									</td>
									<td class={cellCls}>
										<StatusBadge
											label={PENALIDADE_TIPO_META[penalidade.tipo].label}
											color={PENALIDADE_TIPO_META[penalidade.tipo].color}
										/>
									</td>
									<td class={`${cellCls} text-xs text-muted`}>{penalidade.motivo}</td>
									<td class={`${cellCls} text-xs text-muted tabnums`}>
										{formatarData(penalidade.data)}
									</td>
									<td class={cellCls}>
										<StatusBadge
											label={PENALIDADE_STATUS_META[penalidade.status].label}
											color={PENALIDADE_STATUS_META[penalidade.status].color}
										/>
									</td>
								</tr>
							{/each}
						</tbody>
					</table>
				</div>
				<div class="flex items-center justify-between px-5 py-3 border-t border-border">
					<span class="text-xs text-muted">
						2 avisos acumulados geram advertência; 3 advertências geram suspensão — tudo registrado
						no histórico do integrante.
					</span>
					<span class="text-xs text-muted tabnums">{filtradas.length} de {penalidades.length}</span>
				</div>
			</div>
		{/if}
	{/if}
</section>

{#if registrando}
	<div
		class="fixed inset-0 z-50 bg-black/60 flex items-center justify-center p-4"
		role="dialog"
		aria-modal="true"
		aria-label="Registrar penalidade"
	>
		<div
			class="bg-surface border border-border rounded-xl w-full max-w-md max-h-[90vh] overflow-y-auto shadow-2xl shadow-black/60"
		>
			<div
				class="flex items-center justify-between px-5 py-4 border-b border-border sticky top-0 bg-surface"
			>
				<h2 class="text-sm font-semibold">Registrar penalidade</h2>
				<button
					type="button"
					onclick={fecharRegistrar}
					class="p-1 rounded hover:bg-elevated text-muted hover:text-ink transition"
					aria-label="Fechar"
				>
					×
				</button>
			</div>
			<form class="p-5 space-y-4" onsubmit={salvarPenalidadeSubmit}>
				<label class="block">
					<span class="text-xs font-medium text-muted uppercase tracking-wide mb-1.5 block"
						>Integrante*</span
					>
					{#if membros.length}
						<select
							class="w-full bg-elevated border border-border rounded-md px-3 py-2.5 text-sm focus:outline-none focus:border-brand focus:ring-2 focus:ring-brand/30"
							value={membroSelecionado}
							onchange={(e) => (membroSelecionado = e.currentTarget.value)}
						>
							{#each membros as membro (membro.id)}
								<option value={membro.id}>{membro.nome}</option>
							{/each}
						</select>
					{:else}
						<div
							class="rounded-lg bg-elevated/40 border border-border px-3 py-2.5 text-sm text-muted"
						>
							Nenhum integrante disponível.
						</div>
					{/if}
				</label>
				<label class="block">
					<span class="text-xs font-medium text-muted uppercase tracking-wide mb-1.5 block">Tipo*</span>
					<select
						class="w-full bg-elevated border border-border rounded-md px-3 py-2.5 text-sm focus:outline-none focus:border-brand focus:ring-2 focus:ring-brand/30"
						value={tipoSelecionado}
						onchange={(e) => (tipoSelecionado = e.currentTarget.value as PenalidadeTipo)}
					>
						{#each TIPOS as tipo}
							<option value={tipo}>{PENALIDADE_TIPO_META[tipo].label}</option>
						{/each}
					</select>
				</label>
				<label class="block">
					<span class="text-xs font-medium text-muted uppercase tracking-wide mb-1.5 block">Motivo*</span>
					<textarea
						rows="3"
						class="w-full bg-elevated border border-border rounded-md px-3 py-2.5 text-sm focus:outline-none focus:border-brand focus:ring-2 focus:ring-brand/30 resize-none"
						placeholder="Descreva o motivo da penalidade"
						bind:value={motivo}
					></textarea>
				</label>
				{#if erroPenalidade}
					<p class="text-xs text-danger" role="alert">{erroPenalidade}</p>
				{/if}
				<div class="flex items-center justify-end gap-3 border-t border-border pt-4">
					<button
						type="button"
						onclick={fecharRegistrar}
						class="bg-transparent text-muted hover:text-ink px-4 py-2 text-sm transition"
					>
						Cancelar
					</button>
					<button
						type="submit"
						disabled={salvarPenalidade || membros.length === 0}
						class="bg-brand hover:bg-brandhi text-white px-4 py-2 rounded-md text-sm font-medium transition shadow-lg shadow-brand/20 disabled:opacity-50 disabled:pointer-events-none"
					>
						{salvarPenalidade ? 'Registrando...' : 'Registrar penalidade'}
					</button>
				</div>
			</form>
		</div>
	</div>
{/if}