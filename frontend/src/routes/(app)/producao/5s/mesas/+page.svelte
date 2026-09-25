<script lang="ts">
	import { goto, invalidateAll } from '$app/navigation';
	import type { PageProps } from './$types';
	import { SITUACOES } from './+page';
	import type { Mesa5S, MesaAuditoriaResultado, NomeFunc, SituacaoMesas } from '$lib/types/producao';
	import { MESA_AUDITORIA_META, MESA_STATUS_META } from '$lib/utils/producao-status';
	import { registrarAuditoriaMesa } from '$lib/api/producao/client';
	import { ApiError, NetworkError } from '$lib/api/client';
	import { toastError, toasts } from '$lib/stores/toast';
	import ModalMesa from '$lib/components/producao/ModalMesa.svelte';
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
	import Icon from '$lib/components/ui/Icon.svelte';

	let { data }: PageProps = $props();

	const canEditProducao = $derived(data.canEditProducao ?? false);

	const result = $derived(data.mesas);
	const error = $derived(data.error);
	const mesas = $derived(result?.dados ?? []);
	const loading = $derived(result === null && error === null);
	const hasError = $derived(error !== null && result === null);

	const projetos = $derived(data.projetos);
	const membros = $derived(data.membros);

	const params = $derived(data.params);

	const hasFilters = $derived(
		params.search !== '' || params.mes !== '' || params.situacao.length > 0
	);

	const kpiCls = 'rounded-xl border border-border bg-surface p-4';

	const totalMesas = $derived(mesas.length);
	const emUso = $derived(mesas.filter((m) => m.status === 'Aprovada').length);
	const pendentes = $derived(mesas.filter((m) => m.status === 'Pendente').length);
	const auditorias = $derived(mesas.filter((m) => m.ultimaAuditoria !== null).length);

	const situacaoOptions = $derived(
		SITUACOES.map((s) => ({ id: s, label: s }))
	);

	const mesOptions = $derived.by(() => {
		const mapa = new Map<string, string>();
		for (const mesa of mesas) {
			const dataAud = mesa.ultimaAuditoria?.data ?? '';
			const chave = dataAud.slice(0, 7);
			if (chave.length === 7 && !mapa.has(chave)) {
				const [a, m] = chave.split('-');
				mapa.set(chave, `${m}/${a}`);
			}
		}
		return [...mapa.entries()].map(([id, label]) => ({ id, label }));
	});

	const filtrado = $derived(
		mesas.filter((m) => {
			const termo = params.search.toLowerCase().trim();
			const coincideBusca =
				termo === '' ||
				m.nome.toLowerCase().includes(termo) ||
				(m.projeto?.codigo.toLowerCase().includes(termo) ?? false) ||
				m.membro.nome.toLowerCase().includes(termo);
			const coincideSituacao =
				params.situacao.length === 0 || params.situacao.includes(m.status);
			const coincideMes = params.mes === '' || (m.ultimaAuditoria?.data ?? '').startsWith(params.mes);
			return coincideBusca && coincideSituacao && coincideMes;
		})
	);

	// ---- modais ----
	let modalVincular = $state(false);
	let modalAuditar = $state<Mesa5S | null>(null);

	// ---- auditar mesa ----
	let auditarResultado = $state<MesaAuditoriaResultado>('ATIVO');
	let auditarAcao = $state('');
	let auditarEnviando = $state(false);
	let auditarErro = $state('');

	$effect(() => {
		if (modalAuditar) {
			auditarResultado = 'ATIVO';
			auditarAcao = '';
			auditarErro = '';
		}
	});

	async function salvarAuditoria(event: SubmitEvent): Promise<void> {
		event.preventDefault();
		if (!modalAuditar) return;
		auditarEnviando = true;
		auditarErro = '';
		try {
			await registrarAuditoriaMesa(modalAuditar.id, {
				resultado: auditarResultado,
				acao: auditarAcao.trim() || undefined
			});
			toasts.success('Auditoria da mesa registrada');
			modalAuditar = null;
			await invalidateAll();
		} catch (err) {
			const msg =
				err instanceof ApiError || err instanceof NetworkError
					? err.message
					: 'Não foi possível registrar a auditoria.';
			auditarErro = msg;
		} finally {
			auditarEnviando = false;
		}
	}

	function aoVincular(_mesa: Mesa5S): void {
		modalVincular = false;
		void invalidateAll();
	}

	function formatarData(iso?: string | null): string {
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
			mes: params.mes,
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
		return `/producao/5s/mesas${qs ? `?${qs}` : ''}`;
	}

	function toggleFiltro(chave: 'situacao', id: string): void {
		const atual = params.situacao;
		const proximo = atual.includes(id as SituacaoMesas)
			? atual.filter((v) => v !== id)
			: ([...atual, id] as SituacaoMesas[]);
		void goto(navegar({ [chave]: proximo, page: 1 }));
	}

	function limparFiltro(chave: 'situacao', id?: string): void {
		const atual = params.situacao;
		const proximo = id
			? atual.filter((v) => v !== id)
			: ([] as SituacaoMesas[]);
		void goto(navegar({ [chave]: proximo, page: 1 }));
	}

	function limparTodos(): void {
		void goto(navegar({ search: '', mes: '', situacao: [], page: 1 }));
	}

	const inputCls =
		'w-full rounded-lg border border-border bg-elevated px-3 py-2.5 text-sm text-ink placeholder:text-muted/60 focus:border-brand focus:outline-none focus:ring-1 focus:ring-brand/40';
</script>

<svelte:head>
	<title>Mesas de projeto — Sistema 5S — Produção — FabLab</title>
</svelte:head>

<div class="space-y-4">
	<PageHeader
		title="Mesas de projeto"
		subtitle="Projetos universitários vinculados às mesas com QR no totem e auditoria periódica."
	>
		{#snippet children()}
			{#if canEditProducao}
				<button
					type="button"
					data-testid="mesa-vincular"
					onclick={() => (modalVincular = true)}
					class="inline-flex items-center gap-1.5 rounded-lg bg-brand px-3 py-2 text-sm font-semibold text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi"
				>
					<Icon name="plus" class="h-4 w-4" /> Vincular projeto
				</button>
			{/if}
		{/snippet}
	</PageHeader>

	<div class="grid grid-cols-2 gap-3 lg:grid-cols-4">
		<div class={kpiCls}>
			<span class="text-xs text-muted">Mesas</span>
			<p class="mt-1 text-2xl font-semibold text-ink tabular-nums">{totalMesas}</p>
		</div>
		<div class={kpiCls}>
			<span class="text-xs text-muted">Em uso</span>
			<p class="mt-1 text-2xl font-semibold text-success tabular-nums">{emUso}</p>
		</div>
		<div class={kpiCls}>
			<span class="text-xs text-muted">Pendentes</span>
			<p class="mt-1 text-2xl font-semibold text-warn tabular-nums">{pendentes}</p>
		</div>
		<div class={kpiCls}>
			<span class="text-xs text-muted">Auditorias (período)</span>
			<p class="mt-1 text-2xl font-semibold text-ink tabular-nums">{auditorias}</p>
		</div>
	</div>

	{#if hasError}
		<ErrorBanner
			message="Não foi possível carregar as mesas de projeto"
			hint={error ?? ''}
			onRetry={() => void goto('/producao/5s/mesas', { invalidateAll: true })}
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
				placeholder="Buscar mesa, projeto ou membro…"
				class="min-w-56 flex-1"
			/>
			<Dropdown
				label="Mês (auditoria)"
				options={mesOptions}
				selected={params.mes ? [params.mes] : []}
				onToggle={(id) => void goto(navegar({ mes: params.mes === id ? '' : id, page: 1 }))}
				onClear={() => void goto(navegar({ mes: '', page: 1 }))}
				search={false}
			/>
			<Dropdown
				label="Situação"
				options={situacaoOptions}
				selected={params.situacao}
				onToggle={(id) => void toggleFiltro('situacao', id)}
				onClear={() => void limparFiltro('situacao')}
				search={false}
			/>
		</div>

		{#if hasFilters}
			<div class="flex flex-wrap items-center gap-1.5">
				{#if params.mes}
					<Chip
						label={`Mês ${mesOptions.find((o) => o.id === params.mes)?.label ?? params.mes}`}
						onRemove={() => void goto(navegar({ mes: '', page: 1 }))}
					/>
				{/if}
				{#each params.situacao as s (s)}
					<Chip label={s} onRemove={() => void limparFiltro('situacao', s)} />
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
		{#if mesas.length === 0}
			<div class="rounded-xl border border-border bg-surface">
				<EmptyState
					icon="squares"
					title="Ainda não há mesas de projeto"
					description="Vincule um projeto universitário à mesa para iniciar."
				>
					{#snippet children()}
						{#if canEditProducao}
							<button
								type="button"
								data-testid="mesa-vincular"
								onclick={() => (modalVincular = true)}
								class="inline-flex items-center gap-1.5 rounded-lg bg-brand px-3 py-2 text-sm font-semibold text-white transition hover:bg-brandhi"
							>
								<Icon name="plus" class="h-4 w-4" /> Vincular projeto
							</button>
						{/if}
					{/snippet}
				</EmptyState>
			</div>
		{:else if filtrado.length === 0}
			<div class="rounded-xl border border-border bg-surface">
				<EmptyState
					icon="filter"
					title="Nenhuma mesa encontrada"
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
				{#each filtrado as mesa (mesa.id)}
					<article
						data-testid="mesa-card"
						class="flex flex-col rounded-xl border border-border bg-surface p-4 transition hover:border-brand/40"
					>
						<div class="flex items-start justify-between gap-2">
							<h3 class="truncate text-sm font-semibold text-ink">{mesa.nome}</h3>
							{#if mesa.periodoExperimental}
								<span class="rounded-full border border-brand/30 bg-brand/10 px-2 py-0.5 text-[10px] font-medium text-brandhi">
									Período experimental
								</span>
							{/if}
						</div>

						<p class="mt-1 truncate font-mono text-xs text-muted">
							{mesa.projeto?.codigo ?? 'Sem projeto'}
						</p>

						<div class="mt-3 flex items-center gap-2">
							<Avatar name={mesa.membro.nome} size="xs" />
							<span class="truncate text-sm text-ink">{mesa.membro.nome}</span>
						</div>

						<div class="mt-3 rounded-lg border border-dashed border-brand/40 bg-brand/5 px-3 py-2">
							<p class="font-mono text-[11px] break-all text-brandhi">{mesa.qrTotem}</p>
							<p class="mt-0.5 text-[10px] text-muted">QR totem (placeholder)</p>
						</div>

						<div class="mt-3 flex items-center justify-between gap-2">
							<StatusBadge {...MESA_STATUS_META[mesa.status]} />
							{#if mesa.ultimaAuditoria}
								<span title={`Última auditoria ${formatarData(mesa.ultimaAuditoria.data)}`}>
									<StatusBadge {...MESA_AUDITORIA_META[mesa.ultimaAuditoria.resultado]} />
								</span>
							{:else}
								<span class="text-[11px] text-muted">sem auditoria</span>
							{/if}
						</div>

						{#if canEditProducao}
							<div class="mt-3 flex flex-wrap items-center gap-1.5 border-t border-border pt-3">
								<button
									type="button"
									data-testid="mesa-auditar"
									onclick={() => (modalAuditar = mesa)}
									class="inline-flex items-center gap-1 rounded-md border border-border bg-elevated/40 px-2 py-1 text-xs font-medium text-ink transition hover:border-brand/50 hover:text-brandhi"
								>
									<Icon name="check" class="h-3 w-3" /> Auditar mesa
								</button>
							</div>
						{/if}
					</article>
				{/each}
			</div>
		{/if}
	{/if}

	<ModalMesa
		open={modalVincular}
		projetos={projetos}
		membros={membros as NomeFunc[]}
		onClose={() => (modalVincular = false)}
		onCriada={aoVincular}
	/>
</div>

<Modal
	open={modalAuditar !== null}
	title={modalAuditar ? `Auditar mesa · ${modalAuditar.nome}` : 'Auditar mesa'}
	subtitle="Resultado da auditoria periódica da mesa"
	onClose={auditarEnviando ? undefined : () => (modalAuditar = null)}
	width="md"
>
	{#snippet children()}
		<form id="modal-auditar-mesa-form" data-testid="mesa-auditar" onsubmit={salvarAuditoria} novalidate>
			{#if auditarErro}
				<div role="alert" class="mb-4 rounded-xl border border-danger/30 bg-danger/10 px-4 py-3 text-sm text-danger">
					Não foi possível registrar a auditoria. {auditarErro}
				</div>
			{/if}

			<label class="block">
				<span class="mb-1.5 block text-xs font-medium text-muted">Resultado *</span>
				<select bind:value={auditarResultado} class={inputCls}>
					<option value="ATIVO">ATIVO — projeto em uso e organizado</option>
					<option value="ABANDONADO">ABANDONADO — mesa sem uso / materiais recolhidos</option>
				</select>
			</label>

			<label class="mt-4 block">
				<span class="mb-1.5 block text-xs font-medium text-muted">Ação / ocorrência</span>
				<textarea
					bind:value={auditarAcao}
					rows="3"
					placeholder="ex.: totem removido, materiais recolhidos…"
					class={`${inputCls} resize-none`}
				></textarea>
			</label>

			<div class="mt-4 rounded-lg border border-border bg-elevated/30 p-3 text-xs text-muted">
				Resultado ABANDONADO notifica o membro responsável e o Admin (Notification Service).
			</div>
		</form>
	{/snippet}
	{#snippet footer()}
		<button
			type="button"
			onclick={() => (modalAuditar = null)}
			disabled={auditarEnviando}
			class="rounded-md px-4 py-2 text-sm text-muted transition hover:text-ink disabled:opacity-50"
		>
			Cancelar
		</button>
		<button
			type="submit"
			form="modal-auditar-mesa-form"
			data-testid="mesa-auditar"
			disabled={auditarEnviando}
			class="rounded-md bg-brand px-4 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi disabled:cursor-not-allowed disabled:opacity-50"
		>
			{auditarEnviando ? 'Registrando…' : 'Registrar auditoria'}
		</button>
	{/snippet}
</Modal>