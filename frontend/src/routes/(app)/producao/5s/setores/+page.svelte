<script lang="ts">
	import { goto, invalidateAll } from '$app/navigation';
	import type { PageProps } from './$types';
	import type { Cartao5S, NomeFunc, Setor5S } from '$lib/types/producao';
	import { CARTAO_META } from '$lib/utils/producao-status';
	import CartaoBadge from '$lib/components/producao/CartaoBadge.svelte';
	import ModalSetor from '$lib/components/producao/ModalSetor.svelte';
	import ModalChecklistSetor from '$lib/components/producao/ModalChecklistSetor.svelte';
	import PageHeader from '$lib/components/ui/PageHeader.svelte';
	import SearchInput from '$lib/components/ui/SearchInput.svelte';
	import Dropdown from '$lib/components/ui/Dropdown.svelte';
	import Chip from '$lib/components/ui/Chip.svelte';
	import Skeleton from '$lib/components/ui/Skeleton.svelte';
	import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte';
	import EmptyState from '$lib/components/ui/EmptyState.svelte';
	import Avatar from '$lib/components/ui/Avatar.svelte';
	import Modal from '$lib/components/ui/Modal.svelte';
	import Icon from '$lib/components/ui/Icon.svelte';
	import { isResponsavelAtribuido } from '$lib/utils/permissions';

	let { data }: PageProps = $props();

	const canEditProducao = $derived(data.canEditProducao ?? false);

	function podeEditar(setor: Setor5S): boolean {
		if (canEditProducao) return true;
		const ids = setor.responsaveis.map((r) => r.membro.id);
		if (setor.auditor) ids.push(setor.auditor.id);
		return ids.some((id) => isResponsavelAtribuido(data.user, { responsavelId: id }));
	}

	const result = $derived(data.result);
	const error = $derived(data.error);
	const setores = $derived(result?.dados ?? []);
	const loading = $derived(result === null && error === null);
	const hasError = $derived(error !== null && result === null);

	const params = $derived(data.params);

	const hasFilters = $derived(
		params.search !== '' || params.cartao.length > 0 || params.responsavel.length > 0
	);

	const kpiCls =
		'rounded-xl border border-border bg-surface p-4';

	const totalSetores = $derived(setores.length);
	const verdes = $derived(setores.filter((s) => s.cartao === 'Verde').length);
	const amarelos = $derived(setores.filter((s) => s.cartao === 'Amarelo').length);
	const vermelhos = $derived(setores.filter((s) => s.cartao === 'Vermelho').length);

	const pessoas = $derived.by(() => {
		const mapa = new Map<string, NomeFunc>();
		for (const s of setores) {
			for (const r of s.responsaveis) mapa.set(r.membro.id, r.membro);
			if (s.auditor) mapa.set(s.auditor.id, s.auditor);
		}
		return [...mapa.values()];
	});

	const responsaveisOptions = $derived(
		pessoas.map((p) => ({ id: p.id, label: p.nome }))
	);

	const cartaoOptions = $derived(
		(['Verde', 'Amarelo', 'Vermelho'] as Cartao5S[]).map((c) => ({ id: c, label: c }))
	);

	const filtrado = $derived(
		setores.filter((s) => {
			const termo = params.search.toLowerCase().trim();
			const coincideBusca =
				termo === '' ||
				s.nome.toLowerCase().includes(termo) ||
				s.responsaveis.some((r) => r.membro.nome.toLowerCase().includes(termo));
			const coincideCartao =
				params.cartao.length === 0 || params.cartao.includes(s.cartao);
			const coincideResponsavel =
				params.responsavel.length === 0 ||
				s.responsaveis.some((r) => params.responsavel.includes(r.membro.id));
			return coincideBusca && coincideCartao && coincideResponsavel;
		})
	);

	// ---- Modais ----
	let modalNovo = $state(false);
	let modalEditar = $state<Setor5S | null>(null);
	let modalChecklist = $state<Setor5S | null>(null);
	let modalConfig = $state(false);

	function abrirNovoSetor(): void {
		modalNovo = true;
	}

	function abrirEditar(setor: Setor5S): void {
		modalEditar = setor;
	}

	function abrirChecklist(setor: Setor5S): void {
		modalChecklist = setor;
	}

	function aoSalvo(): void {
		void invalidateAll();
	}

	function formatarData(iso: string): string {
		if (!iso) return '—';
		const [a, m, d] = iso.slice(0, 10).split('-');
		return d ? `${d}/${m}/${a}` : '—';
	}

	interface Query {
		[key: string]: string | string[] | number | undefined;
	}

	function navegar(overrides: Query): string {
		const base: Query = {
			search: params.search,
			cartao: params.cartao,
			responsavel: params.responsavel,
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
		return `/producao/5s/setores${qs ? `?${qs}` : ''}`;
	}

	function toggleFiltro(chave: 'cartao' | 'responsavel', id: string): void {
		const atual = chave === 'cartao' ? params.cartao : params.responsavel;
		const proximo = atual.includes(id) ? atual.filter((v) => v !== id) : [...atual, id];
		void goto(navegar({ [chave]: proximo, page: 1 }));
	}

	function limparFiltro(chave: 'cartao' | 'responsavel', id?: string): void {
		const atual = chave === 'cartao' ? params.cartao : params.responsavel;
		const proximo = id ? atual.filter((v) => v !== id) : [];
		void goto(navegar({ [chave]: proximo, page: 1 }));
	}

	function limparTodos(): void {
		void goto(navegar({ search: '', cartao: [], responsavel: [], page: 1 }));
	}
</script>

<svelte:head>
	<title>Setores 5S — Produção — FabLab</title>
</svelte:head>

<div class="space-y-4">
	<PageHeader
		title="Setores 5S"
		subtitle="Responsáveis rotativos, auditorias, cartões e checklists de cada setor."
	>
		{#snippet children()}
			{#if canEditProducao}
				<button
					type="button"
					data-testid="set-config"
					onclick={() => (modalConfig = true)}
					class="inline-flex items-center gap-1.5 rounded-lg border border-border bg-surface px-3 py-2 text-sm font-medium text-ink transition hover:border-brand/50 hover:text-brandhi"
				>
					<Icon name="adjustments" class="h-4 w-4" /> Configurar 5S
				</button>
			{/if}
			<button
				type="button"
				data-testid="set-novo"
				onclick={abrirNovoSetor}
				class="inline-flex items-center gap-1.5 rounded-lg bg-brand px-3 py-2 text-sm font-semibold text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi"
			>
				<Icon name="plus" class="h-4 w-4" /> Novo setor
			</button>
		{/snippet}
	</PageHeader>

	<div class="grid grid-cols-2 gap-3 lg:grid-cols-4">
		<div class={kpiCls}><span class="text-xs text-muted">Setores</span><p class="mt-1 text-2xl font-semibold tabular-nums">{totalSetores}</p></div>
		<div class={kpiCls}><span class="text-xs text-muted">Cartão verde</span><p class="mt-1 text-2xl font-semibold text-success tabular-nums">{verdes}</p></div>
		<div class={kpiCls}><span class="text-xs text-muted">Cartão amarelo</span><p class="mt-1 text-2xl font-semibold text-warn tabular-nums">{amarelos}</p></div>
		<div class={kpiCls}><span class="text-xs text-muted">Cartão vermelho</span><p class="mt-1 text-2xl font-semibold text-danger tabular-nums">{vermelhos}</p></div>
	</div>

	{#if hasError}
		<ErrorBanner
			message="Não foi possível carregar os setores 5S"
			hint={error ?? ''}
			onRetry={() => void goto('/producao/5s/setores', { invalidateAll: true })}
		/>
	{:else if loading}
		<div class="grid grid-cols-1 gap-3 md:grid-cols-2 xl:grid-cols-4">
			{#each [0, 1, 2, 3] as i (i)}
				<div class="space-y-3 rounded-xl border border-border bg-surface p-4">
					<Skeleton class="h-5 w-2/3" />
					<Skeleton class="h-4 w-1/2" />
					<Skeleton class="h-10 w-full" />
					<Skeleton class="h-3 w-3/4" />
				</div>
			{/each}
		</div>
	{:else}
		<!-- Filtros -->
		<div class="flex flex-wrap items-center gap-2">
			<SearchInput
				value={params.search}
				onSearch={(v) => void goto(navegar({ search: v, page: 1 }))}
				placeholder="Buscar setor ou responsável…"
				class="min-w-56 flex-1"
			/>
			<Dropdown
				label="Cartão"
				options={cartaoOptions}
				selected={params.cartao}
				onToggle={(id) => void toggleFiltro('cartao', id)}
				onClear={() => void limparFiltro('cartao')}
			/>
			<Dropdown
				label="Responsável"
				options={responsaveisOptions}
				selected={params.responsavel}
				onToggle={(id) => void toggleFiltro('responsavel', id)}
				onClear={() => void limparFiltro('responsavel')}
			/>
		</div>

		{#if hasFilters}
			<div class="flex flex-wrap items-center gap-1.5">
				{#each params.cartao as c (c)}
					<Chip label={c} onRemove={() => void limparFiltro('cartao', c)} />
				{/each}
				{#each params.responsavel as r (r)}
					<Chip label={pessoas.find((p) => p.id === r)?.nome ?? 'Responsável'} onRemove={() => void limparFiltro('responsavel', r)} />
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
		{#if setores.length === 0}
			<div class="rounded-xl border border-border bg-surface">
				<EmptyState
					icon="squares"
					title="Ainda não há setores 5S"
					description="Crie o primeiro setor para começar o sistema 5S."
				>
					{#snippet children()}
						<button
							data-testid="set-novo"
							onclick={abrirNovoSetor}
							class="inline-flex items-center gap-1.5 rounded-lg bg-brand px-3 py-2 text-sm font-semibold text-white transition hover:bg-brandhi"
						>
							<Icon name="plus" class="h-4 w-4" /> Novo setor
						</button>
					{/snippet}
				</EmptyState>
			</div>
		{:else if filtrado.length === 0}
			<div class="rounded-xl border border-border bg-surface">
				<EmptyState
					icon="filter"
					title="Nenhum setor encontrado"
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
		{:else}
			<div class="grid grid-cols-1 gap-3 md:grid-cols-2 xl:grid-cols-4">
				{#each filtrado as setor (setor.id)}
					<article
						data-testid="set-card"
						class="flex flex-col rounded-xl border border-border bg-surface p-4 transition hover:border-brand/40"
					>
						<div class="flex items-center justify-between gap-2">
							<h3 class="truncate text-sm font-semibold text-ink">{setor.nome}</h3>
							<span class="rounded-full border border-border bg-elevated/60 px-2 py-0.5 text-[10px] font-medium capitalize text-muted">
								{setor.ciclo}
							</span>
						</div>

						<div class="mt-3 flex items-center gap-2">
							<CartaoBadge cartao={setor.cartao} />
							{#if setor.nota !== null && setor.nota !== undefined}
								<span class="font-mono text-sm text-ink tabular-nums">{setor.nota}</span>
							{/if}
						</div>

						<div class="mt-3 space-y-1.5">
							<div class="flex flex-wrap items-center gap-1.5">
								{#each setor.responsaveis as resp (resp.membro.id)}
									<span class="inline-flex items-center gap-1 rounded-full border border-border bg-elevated/40 py-0.5 pl-1 pr-2 text-[11px] text-ink">
										<Avatar name={resp.membro.nome} size="xs" tone={resp.ps ? 'brand' : 'muted'} />
										{resp.membro.nome}
										{#if resp.ps}
											<span class="text-[9px] font-bold text-brandhi">PS</span>
										{/if}
									</span>
								{/each}
							</div>
							<p class="text-[11px] text-muted">
								Auditor: <span class="text-ink">{setor.auditor?.nome ?? '—'}</span>
							</p>
						</div>

						<p class="mt-2 text-[11px] text-muted">
							Próxima auditoria: <span class="tabular-nums">{formatarData(setor.proximaAuditoria)}</span>
						</p>

						{#if podeEditar(setor)}
							<div class="mt-3 flex flex-wrap items-center gap-1.5 border-t border-border pt-3">
								<button
									type="button"
									onclick={() => abrirEditar(setor)}
									class="inline-flex items-center gap-1 rounded-md border border-border bg-elevated/40 px-2 py-1 text-xs font-medium text-ink transition hover:border-brand/50 hover:text-brandhi"
								>
									<Icon name="pencil" class="h-3 w-3" /> Editar
								</button>
								<button
									type="button"
									onclick={() => abrirChecklist(setor)}
									class="rounded-md border border-border bg-elevated/40 px-2 py-1 text-xs font-medium text-ink transition hover:border-brand/50 hover:text-brandhi"
								>
									Checklist
								</button>
							</div>
						{/if}
					</article>
				{/each}
			</div>
		{/if}

		<!-- Checklist padrão (informativo) -->
		<section class="rounded-xl border border-border bg-surface p-4">
			<h2 class="text-sm font-semibold text-ink">Checklist padrão (5 S)</h2>
			<ol class="mt-2 list-inside list-decimal space-y-1 text-sm text-muted">
				<li>Seiri — separar o útil do desnecessário</li>
				<li>Seiton — arrumar e organizar</li>
				<li>Seiso — limpar e inspecionar</li>
				<li>Seiketsu — padronizar</li>
				<li>Shitsuke — disciplina</li>
			</ol>
			<p class="mt-2 text-[11px] text-muted">
				Os 5 S são travados; itens específicos por setor são editáveis (1 item = 1 ponto). Alterações propostas por
				responsável/auditor passam por aprovação do Admin.
			</p>
		</section>
	{/if}

	<ModalSetor
		open={modalNovo}
		setor={null}
		pessoas={pessoas}
		isAdmin={canEditProducao}
		canEdit={canEditProducao}
		onClose={() => (modalNovo = false)}
		onSalvo={aoSalvo}
	/>

	{#if modalEditar}
		<ModalSetor
			open={modalEditar !== null}
			setor={modalEditar}
			pessoas={pessoas}
			isAdmin={podeEditar(modalEditar)}
			canEdit={podeEditar(modalEditar)}
			onClose={() => (modalEditar = null)}
			onSalvo={aoSalvo}
		/>
	{/if}

	{#if modalChecklist}
		<ModalChecklistSetor
			open={modalChecklist !== null}
			setor={modalChecklist}
			itens={[]}
			nota={modalChecklist.nota ?? null}
			isAdmin={podeEditar(modalChecklist)}
			canEdit={podeEditar(modalChecklist)}
			onClose={() => (modalChecklist = null)}
			onSalvo={aoSalvo}
		/>
	{/if}

	<Modal
		open={modalConfig}
		title="Configuração do 5S"
		subtitle="Parâmetros vigentes do sistema 5S"
		onClose={() => (modalConfig = false)}
	>
		{#snippet children()}
			<dl class="space-y-2 text-sm">
				<div class="flex items-center justify-between rounded-lg border border-border bg-elevated/40 px-3 py-2">
					<dt class="text-muted">Rotação de responsáveis</dt>
					<dd class="tabular-nums text-ink">dias (Admin)</dd>
				</div>
				<div class="flex items-center justify-between rounded-lg border border-border bg-elevated/40 px-3 py-2">
					<dt class="text-muted">Nota p/ trocar de cartão</dt>
					<dd class="tabular-nums text-ink">Verde ≥90 · Vermelho &lt;70</dd>
				</div>
				<div class="flex items-center justify-between rounded-lg border border-border bg-elevated/40 px-3 py-2">
					<dt class="text-muted">Prazo p/ resolver pendência grave</dt>
					<dd class="text-ink">dias úteis (Admin)</dd>
				</div>
				<div class="flex items-center justify-between rounded-lg border border-border bg-elevated/40 px-3 py-2">
					<dt class="text-muted">Top 1 do mês</dt>
					<dd class="text-ink">1 semana livre</dd>
				</div>
			</dl>
			<p class="mt-3 rounded-lg border border-warn/30 bg-warn/10 p-3 text-xs text-muted">
				Edição dos parâmetros depende do endpoint <span class="font-mono">/producao/parametros-5s</span> (contrato
				pendente 🟡).
			</p>
		{/snippet}
	</Modal>
</div>