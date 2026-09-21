<script lang="ts">
	import { goto, invalidateAll } from '$app/navigation';
	import type { PageProps } from './$types';
	import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte';
	import EmptyState from '$lib/components/ui/EmptyState.svelte';
	import PageHeader from '$lib/components/ui/PageHeader.svelte';
	import StatusBadge from '$lib/components/ui/StatusBadge.svelte';
	import Dropdown from '$lib/components/ui/Dropdown.svelte';
	import Avatar from '$lib/components/ui/Avatar.svelte';
	import CartaoBadge from '$lib/components/producao/CartaoBadge.svelte';
	import ModalAuditoria from '$lib/components/producao/ModalAuditoria.svelte';
	import { INSPECAO_STATUS_META } from '$lib/utils/producao-status';
	import { nomearAuditor5S } from '$lib/api/producao/client';
	import type { Inspecao5S } from '$lib/types/producao';

	const PADRAO_5S_ITENS = [
		'Seiri — separar o útil do desnecessário',
		'Seiton — arrumar e organizar',
		'Seiso — limpar e inspecionar',
		'Seiketsu — padronizar',
		'Shitsuke — disciplina e rotina'
	];

	const kpiCls = 'bg-elevated border border-border rounded-xl p-4';
	const cellCls = 'px-5 py-3 text-sm whitespace-nowrap';
	const thCls = 'px-5 py-3 text-left text-[11px] font-semibold uppercase tracking-wider text-muted';

	let { data }: PageProps = $props();

	let auditando = $state<Inspecao5S | null>(null);
	let nomeando = $state(false);
	let nomearAlvo = $state<Inspecao5S | null>(null);
	let salvarNomeacao = $state(false);
	let erroNomeacao = $state<string | null>(null);
	let auditorSelecionado = $state('');

	const params = $derived(data.params);
	const hoje = new Date().toISOString().slice(0, 10);
	const mesAtual = new Date().toISOString().slice(0, 7);

	const loading = $derived(!data.result && !data.error);
	const hasError = $derived(Boolean(data.error));
	const inspecoes = $derived(data.result?.dados ?? []);
	const filtradas = $derived(
		inspecoes.filter((i) => {
			if (params.status.length && !params.status.includes(i.status)) return false;
			if (params.setor.length && !params.setor.includes(i.setor.id)) return false;
			return true;
		})
	);
	const empty = $derived(!loading && !hasError && filtradas.length === 0);
	const emptyFiltrado = $derived(Boolean(data.result && data.result.dados.length > 0 && empty));

	const setoresOpcoes = $derived([
		...new Map(inspecoes.map((i) => [i.setor.id, i.setor])).values()
	]);
	const auditorsOpcoes = $derived(
		[...new Map(inspecoes.map((i) => [i.auditor.id, i.auditor])).values()]
	);
	const pendentes = $derived(inspecoes.filter((i) => i.status !== 'Concluída'));

	const kpis = $derived({
		mes: inspecoes.filter((i) => i.data.startsWith(mesAtual)).length,
		hoje: inspecoes.filter((i) => i.data === hoje).length,
		auditores: auditorsOpcoes.length,
		atrasadas: inspecoes.filter((i) => i.status === 'Atrasada').length
	});

	interface Query {
		[key: string]: string | string[] | number | undefined;
	}

	function navegar(overrides: Query): string {
		const base: Query = {
			status: params.status,
			setor: params.setor,
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
		return `/producao/5s/auditoria${qs ? `?${qs}` : ''}`;
	}

	function toggleFiltro(atual: string[], valor: string): string[] {
		return atual.includes(valor) ? atual.filter((v) => v !== valor) : [...atual, valor];
	}

	function abrirAuditar(inspecao: Inspecao5S) {
		auditando = inspecao;
	}

	function abrirNomear(inspecao: Inspecao5S | null) {
		nomeando = true;
		nomearAlvo = inspecao;
		auditorSelecionado = inspecao ? inspecao.auditor.id : '';
		erroNomeacao = null;
	}

	function fecharNomear() {
		nomeando = false;
		nomearAlvo = null;
		auditorSelecionado = '';
		erroNomeacao = null;
	}

	async function salvarNomeacaoSubmit(event: SubmitEvent) {
		event.preventDefault();
		const alvo = nomearAlvo;
		if (!alvo) {
			erroNomeacao = 'Selecione uma auditoria pendente.';
			return;
		}
		if (!auditorSelecionado) {
			erroNomeacao = 'Selecione um auditor.';
			return;
		}
		salvarNomeacao = true;
		erroNomeacao = null;
		try {
			await nomearAuditor5S(alvo.id, { auditorId: auditorSelecionado });
			fecharNomear();
			await invalidateAll();
		} catch (err) {
			erroNomeacao =
				err instanceof Error ? err.message : 'Não foi possível nomear o auditor. Tente novamente.';
		} finally {
			salvarNomeacao = false;
		}
	}

	function formatarData(iso: string) {
		return new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short' }).format(
			new Date(`${iso}T00:00:00`)
		);
	}
</script>

<svelte:head><title>Auditoria 5S · Produção</title></svelte:head>

<section class="space-y-5">
	<PageHeader
		title="Auditoria 5S"
		subtitle="Auditorias programadas, realizadas e atrasadas dos setores."
	>
		{#if data.canAuditar5S}
			<button
				type="button"
				data-testid="aud-nomear"
				class="flex items-center gap-2 bg-elevated hover:bg-elevated/70 border border-border rounded-md px-3 py-2 text-sm transition"
				onclick={() => abrirNomear(null)}
			>
				Nomear auditor
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
			<span class="sr-only">Carregando auditorias...</span>
		</div>
	{:else if hasError}
		<ErrorBanner
			message={data.error ?? 'Não foi possível carregar as auditorias'}
			onRetry={() => invalidateAll()}
		/>
	{:else}
		<div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4" data-testid="aud-kpis">
			<div class={kpiCls}>
				<span class="text-xs text-muted">Auditorias do mês</span>
				<div class="mt-1 text-2xl font-semibold tabnums">{kpis.mes}</div>
			</div>
			<div class={kpiCls}>
				<span class="text-xs text-muted">Hoje</span>
				<div class="mt-1 text-2xl font-semibold tabnums">{kpis.hoje}</div>
			</div>
			<div class={kpiCls}>
				<span class="text-xs text-muted">Auditores nomeados</span>
				<div class="mt-1 text-2xl font-semibold tabnums">{kpis.auditores}</div>
			</div>
			<div class={`${kpiCls} ${kpis.atrasadas ? 'border-danger/40' : ''}`}>
				<span class="text-xs text-muted">Atrasadas</span>
				<div class="mt-1 text-2xl font-semibold tabnums {kpis.atrasadas ? 'text-danger' : ''}">
					{kpis.atrasadas}
				</div>
			</div>
		</div>

		<div class="flex items-center gap-3 flex-wrap">
			<Dropdown
				label="Situação"
				options={Object.entries(INSPECAO_STATUS_META).map(([id, m]) => ({
					id,
					label: m.label,
					count: inspecoes.filter((i) => i.status === id).length
				}))}
				selected={params.status}
				onToggle={(id) => goto(navegar({ status: toggleFiltro(params.status, id), page: 1 }))}
				onClear={() => goto(navegar({ status: [], page: 1 }))}
			/>
			<Dropdown
				label="Setor"
				options={setoresOpcoes.map((s) => ({
					id: s.id,
					label: s.nome,
					count: inspecoes.filter((i) => i.setor.id === s.id).length
				}))}
				selected={params.setor}
				onToggle={(id) => goto(navegar({ setor: toggleFiltro(params.setor, id), page: 1 }))}
				onClear={() => goto(navegar({ setor: [], page: 1 }))}
			/>
		</div>

		{#if empty}
			{#if emptyFiltrado}
				<EmptyState
					icon="search"
					title="Nenhuma auditoria para esses filtros"
					description="Ajuste os filtros para ver outras auditorias."
				>
					<button
						type="button"
						class="bg-brand hover:bg-brandhi text-white px-4 py-2 rounded-md text-sm font-medium transition"
						onclick={() => goto(navegar({ status: [], setor: [], page: 1 }))}
					>
						Limpar filtros
					</button>
				</EmptyState>
			{:else}
				<EmptyState
					icon="document"
					title="Nenhuma auditoria"
					description="Quando houver auditorias programadas para os setores, elas aparecerão aqui."
				/>
			{/if}
		{:else}
			<div class="bg-elevated border border-border rounded-xl overflow-x-auto">
				<table class="w-full text-sm">
					<thead>
						<tr class="border-b border-border">
							<th class={thCls}>Setor</th>
							<th class={thCls}>Auditor</th>
							<th class={thCls}>Data</th>
							<th class={thCls}>Nota</th>
							<th class={thCls}>Situação</th>
							<th class={thCls}><span class="sr-only">Ações</span></th>
						</tr>
					</thead>
					<tbody class="divide-y divide-border">
						{#each filtradas as inspecao (inspecao.id)}
							<tr data-testid="aud-row">
								<td class={cellCls}>
									<div class="font-medium">{inspecao.setor.nome}</div>
								</td>
								<td class={cellCls}>
									<div class="flex items-center gap-2">
										<Avatar name={inspecao.auditor.nome} size="xs" tone="brand" />
										{inspecao.auditor.nome}
									</div>
								</td>
								<td class={cellCls}>{formatarData(inspecao.data)}</td>
								<td class={cellCls}>
									{#if inspecao.cartao && inspecao.nota}
										<div class="flex items-center gap-2">
											<span class="font-semibold tabnums">{inspecao.nota}</span>
											<CartaoBadge cartao={inspecao.cartao} />
										</div>
									{:else}
										<span class="text-muted">—</span>
									{/if}
								</td>
								<td class={cellCls}>
									<StatusBadge
										label={INSPECAO_STATUS_META[inspecao.status].label}
										color={INSPECAO_STATUS_META[inspecao.status].color}
									/>
								</td>
								<td class={`${cellCls} text-right`}>
									<div class="flex items-center justify-end gap-3">
										{#if inspecao.status !== 'Concluída'}
											<button
												type="button"
												data-testid="aud-concluir"
												class="text-brand hover:text-brandhi text-xs font-medium transition"
												onclick={() => abrirAuditar(inspecao)}
											>
												Auditar
											</button>
										{:else}
											<span class="text-[11px] text-success">Concluída</span>
										{/if}
										{#if data.canAuditar5S && inspecao.status !== 'Concluída'}
											<button
												type="button"
												class="text-muted hover:text-ink text-xs transition"
												onclick={() => abrirNomear(inspecao)}
											>
												Nomear
											</button>
										{/if}
									</div>
								</td>
							</tr>
						{/each}
					</tbody>
				</table>
			</div>
			<p class="text-xs text-muted">{filtradas.length} de {inspecoes.length} auditorias</p>
		{/if}
	{/if}
</section>

{#if auditando}
	<ModalAuditoria
		open={true}
		inspecao={auditando}
		itens={PADRAO_5S_ITENS}
		onClose={() => (auditando = null)}
		onConcluida={() => {
			auditando = null;
			invalidateAll();
		}}
	/>
{/if}

{#if nomeando}
	<div
		class="fixed inset-0 z-50 bg-black/60 flex items-center justify-center p-4"
		role="dialog"
		aria-modal="true"
		aria-label="Nomear auditor"
	>
		<div
			class="bg-surface border border-border rounded-xl w-full max-w-md max-h-[90vh] overflow-y-auto shadow-2xl shadow-black/60"
		>
			<div
				class="flex items-center justify-between px-5 py-4 border-b border-border sticky top-0 bg-surface"
			>
				<h2 class="text-sm font-semibold">Nomear auditor</h2>
				<button
					type="button"
					onclick={fecharNomear}
					class="p-1 rounded hover:bg-elevated text-muted hover:text-ink transition"
					aria-label="Fechar"
				>
					×
				</button>
			</div>
			<form class="p-5 space-y-4" onsubmit={salvarNomeacaoSubmit}>
				<label class="block">
					<span class="text-xs font-medium text-muted uppercase tracking-wide mb-1.5 block"
						>Auditoria*</span
					>
					{#if nomearAlvo}
						<div
							class="rounded-lg bg-elevated/40 border border-border px-3 py-2.5 text-sm flex items-center justify-between gap-2"
						>
							<span class="font-medium">{nomearAlvo.setor.nome}</span>
							<span class="text-xs text-muted tabnums">{formatarData(nomearAlvo.data)}</span>
						</div>
					{:else if pendentes.length}
						<select
							class="w-full bg-elevated border border-border rounded-md px-3 py-2.5 text-sm focus:outline-none focus:border-brand focus:ring-2 focus:ring-brand/30"
							onchange={(e) => {
								const id = e.currentTarget.value;
								nomearAlvo = pendentes.find((i) => i.id === id) ?? null;
								auditorSelecionado = nomearAlvo?.auditor.id ?? '';
							}}
							value=""
						>
							<option value="" disabled>Selecione uma auditoria...</option>
							{#each pendentes as inspecao (inspecao.id)}
								<option value={inspecao.id}>
									{inspecao.setor.nome} — {formatarData(inspecao.data)}
								</option>
							{/each}
						</select>
					{:else}
						<div
							class="rounded-lg bg-elevated/40 border border-border px-3 py-2.5 text-sm text-muted"
						>
							Nenhuma auditoria pendente para nomear.
						</div>
					{/if}
				</label>
				<label class="block">
					<span class="text-xs font-medium text-muted uppercase tracking-wide mb-1.5 block"
						>Auditor*</span
					>
					<select
						class="w-full bg-elevated border border-border rounded-md px-3 py-2.5 text-sm focus:outline-none focus:border-brand focus:ring-2 focus:ring-brand/30"
						onchange={(e) => (auditorSelecionado = e.currentTarget.value)}
						value={auditorSelecionado}
					>
						<option value="" disabled>Selecione um auditor...</option>
						{#each auditorsOpcoes as auditor (auditor.id)}
							<option value={auditor.id}>{auditor.nome}</option>
						{/each}
					</select>
				</label>
				<div class="rounded-lg bg-elevated/30 border border-border p-3 text-xs text-muted">
					Auditores podem atuar em qualquer setor.
					<b class="text-ink">Alterações de setor propostas pelo auditor</b> (cadastro, checklist,
					responsáveis) seguem para <b class="text-ink">aprovação do Admin</b>.
				</div>
				{#if erroNomeacao}
					<p class="text-xs text-danger" role="alert">{erroNomeacao}</p>
				{/if}
				<div class="flex items-center justify-end gap-3 border-t border-border pt-4">
					<button
						type="button"
						onclick={fecharNomear}
						class="bg-transparent text-muted hover:text-ink px-4 py-2 text-sm transition"
					>
						Cancelar
					</button>
					<button
						type="submit"
						disabled={salvarNomeacao || pendentes.length === 0}
						class="bg-brand hover:bg-brandhi text-white px-4 py-2 rounded-md text-sm font-medium transition shadow-lg shadow-brand/20 disabled:opacity-50 disabled:pointer-events-none"
					>
						{salvarNomeacao ? 'Nomeando...' : 'Nomear auditor'}
					</button>
				</div>
			</form>
		</div>
	</div>
{/if}