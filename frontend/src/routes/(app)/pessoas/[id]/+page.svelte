<script lang="ts">
	import { goto } from '$app/navigation';
	import type { PageProps } from './$types';
	import { get } from 'svelte/store';
	import { auth } from '$lib/stores/auth';
	import type { ExtratoMensalHoras, HoraApontamento } from '$lib/types/rh';
	import { getHorasDisponiveis, listApontamentos } from '$lib/api/rh/horas';
	import { horaStatusMeta, nivelMeta, personStatusMeta } from '$lib/utils/rh-status';
	import { isProprioRegistro } from '$lib/utils/permissions';
	import { toasts } from '$lib/stores/toast';
	import Avatar from '$lib/components/ui/Avatar.svelte';
	import StatusBadge from '$lib/components/ui/StatusBadge.svelte';
	import TypeBadge from '$lib/components/ui/TypeBadge.svelte';
	import Tabs from '$lib/components/ui/Tabs.svelte';
	import EmptyState from '$lib/components/ui/EmptyState.svelte';
	import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte';
	import TableSkeleton from '$lib/components/ui/TableSkeleton.svelte';
	import Icon from '$lib/components/ui/Icon.svelte';

	let { data }: PageProps = $props();

	const canEdit = $derived(data.canEdit ?? false);
	const usuario = $derived(get(auth).user);
	const pessoa = $derived(data.pessoa);
	const naoEncontrada = $derived(data.notFound);
	const erroPagina = $derived(data.error);
	const id = $derived(data.id);
	const aba = $derived(data.tab);

	function voltar(): void {
		if (window.history.length > 1) window.history.back();
		else void goto('/pessoas');
	}

	function trocarAba(nova: string): void {
		const url = new URLSearchParams(window.location.search);
		url.set('tab', nova);
		void goto(`/pessoas/${id}?${url.toString()}`);
	}

	function formatarData(iso: string | undefined): string {
		if (!iso) return '—';
		const data = new Date(iso.includes('T') ? iso : `${iso}T12:00:00`);
		if (Number.isNaN(data.getTime())) return iso;
		return new Intl.DateTimeFormat('pt-BR', {
			day: '2-digit',
			month: '2-digit',
			year: 'numeric'
		}).format(data);
	}

	// ---- Extrato mensal de horas (backend; nunca somado no client — R-5) ----

	let extrato = $state<ExtratoMensalHoras | null>(null);
	let extratoErro = $state<string | null>(null);

	$effect(() => {
		const ctrl = new AbortController();
		const sinal = ctrl.signal;
		extratoErro = null;
		getHorasDisponiveis((input, init) => fetch(input, { ...init, signal: sinal }))
			.then((res) => {
				if (!sinal.aborted) extrato = res;
			})
			.catch((err: unknown) => {
				if (!sinal.aborted) extratoErro = err instanceof Error ? err.message : 'Erro';
			});
		return () => ctrl.abort();
	});

	// ---- Apontamentos da pessoa (aba Horas; erro parcial por aba) ----

	let apontamentos = $state<HoraApontamento[]>([]);
	let horasErro = $state<string | null>(null);
	let horasCarregando = $state(false);
	let subAba = $state('pendente');

	$effect(() => {
		if (aba !== 'horas' || !pessoa) return;
		const ctrl = new AbortController();
		const sinal = ctrl.signal;
		horasCarregando = true;
		horasErro = null;
		listApontamentos({ personId: id }, (input, init) => fetch(input, { ...init, signal: sinal }))
			.then((res) => {
				if (!sinal.aborted) apontamentos = res.hours;
			})
			.catch((err: unknown) => {
				if (!sinal.aborted) horasErro = err instanceof Error ? err.message : 'Erro';
			})
			.finally(() => {
				if (!sinal.aborted) horasCarregando = false;
			});
		return () => ctrl.abort();
	});

	function recarregarHoras(): void {
		subAba = 'pendente';
		apontamentos = [];
		horasErro = null;
		horasCarregando = true;
		listApontamentos({ personId: id })
			.then((res) => (apontamentos = res.hours))
			.catch((err: unknown) => {
				horasErro = err instanceof Error ? err.message : 'Erro';
				toasts.danger('Não foi possível recarregar as horas.');
			})
			.finally(() => (horasCarregando = false));
	}

	const apontamentosFiltrados = $derived(
		apontamentos.filter((a) => (subAba === 'todas' ? true : a.status === subAba))
	);

	const kpis = $derived(pessoa?.kpis);
	const specs = $derived(pessoa?.specs ?? {});
	const specEntries = $derived(Object.entries(specs));
	const qualificacao = $derived(pessoa?.qualification ?? 0);
	const ehInstrutor = $derived(pessoa?.isInstrutor === true);

	// Estagiário/Recrutando (roles 3/4) vê Horas/Treinamentos/Histórico só do próprio
	// registro (D-7: sem redirect p/ não mascarar mapeamento token→pessoa ausente).
	const verSensivel = $derived(
		!((usuario?.role === 3 || usuario?.role === 4) && !isProprioRegistro(usuario, id))
	);

	const abas = $derived(
		[
			{ id: 'visao-geral', label: 'Visão geral' },
			{ id: 'horas', label: 'Horas' },
			{ id: 'treinamentos', label: 'Treinamentos' },
			{ id: 'historico', label: 'Histórico de nível' }
		].filter((t) => verSensivel || t.id === 'visao-geral')
	);

	const nivelAtual = $derived(pessoa ? nivelMeta(pessoa.nivel) : null);
	const statusAtual = $derived(pessoa ? personStatusMeta(pessoa.status) : null);
</script>

<svelte:head>
	<title>{pessoa ? `${pessoa.name} — Pessoas` : 'Pessoa — FabLab'}</title>
</svelte:head>

<div class="space-y-4">
	<div class="flex flex-wrap items-center justify-between gap-2">
		<button
			type="button"
			onclick={voltar}
			class="inline-flex items-center gap-1.5 text-xs font-medium text-muted transition-colors hover:text-ink"
		>
			<Icon name="arrow-left" class="h-3.5 w-3.5" /> Voltar à lista
		</button>
		{#if pessoa && canEdit}
			<div class="flex items-center gap-2">
				<a
					href={`/pessoas/${id}/editar`}
					data-testid="edit-person-button"
					class="inline-flex items-center gap-1.5 rounded-md border border-border bg-elevated px-3 py-2 text-sm transition hover:border-brand/50 hover:text-brandhi"
				>
					<Icon name="pencil" class="h-3.5 w-3.5" /> Editar
				</a>
			</div>
		{/if}
	</div>

	{#if naoEncontrada}
		<div class="rounded-xl border border-border bg-surface">
			<EmptyState
				icon="user"
				title="Pessoa não encontrada"
				description="O cadastro pode ter sido excluído ou o endereço está incorreto."
			>
				{#snippet children()}
					<button
						type="button"
						onclick={voltar}
						class="rounded-md border border-border bg-elevated px-3 py-2 text-sm font-medium text-ink transition hover:border-brand/50 hover:text-brandhi"
					>
						Voltar à lista
					</button>
				{/snippet}
			</EmptyState>
		</div>
	{:else if erroPagina || !pessoa}
		<ErrorBanner
			message="Não foi possível carregar a pessoa"
			hint="Verifique sua conexão e tente novamente. Se persistir, contate o suporte."
			onRetry={() => void goto(`/pessoas/${id}${window.location.search}`, { invalidateAll: true })}
			testid="person-detail-retry"
		/>
	{:else}
		<!-- Hero -->
		<section class="overflow-hidden rounded-xl border border-border bg-surface" aria-label="Perfil">
			<div class="flex flex-col gap-4 p-5 sm:flex-row sm:items-center">
				<Avatar name={pessoa.name} initialsOverride={pessoa.initials} size="md" tone="brand" />
				<div class="min-w-0 flex-1">
					<div class="flex flex-wrap items-center gap-2">
						<h1 class="text-2xl font-semibold tracking-tight text-ink">{pessoa.name}</h1>
						<span class="font-mono text-xs text-muted">{pessoa.matricula}</span>
					</div>
					<p class="mt-1.5 text-xs text-muted">
						{pessoa.group?.label ?? 'Sem grupo'} ·
						{nivelAtual?.label ?? pessoa.nivel} ·
						{statusAtual?.label ?? pessoa.status}
					</p>
					<div class="mt-2 flex flex-wrap items-center gap-2">
						{#if statusAtual}
							<StatusBadge label={statusAtual.label} color={statusAtual.color} />
						{/if}
						{#if ehInstrutor}
							<TypeBadge
								tone="brand"
								label={qualificacao > 0
									? `Instrutor(a) · Qualificação ${qualificacao}`
									: 'Instrutor(a)'}
							/>
						{/if}
					</div>
				</div>
			</div>
		</section>

		<!-- KPIs (valores servidos pelo backend; sem soma no client) -->
		<section class="grid grid-cols-2 gap-4 lg:grid-cols-4" aria-label="Indicadores">
			<div class="rounded-xl border border-border bg-elevated p-4">
				<p class="text-xs text-muted">Horas validadas · {extrato?.periodo ?? 'mês'}</p>
				{#if extrato}
					<p class="mt-1 font-mono text-2xl font-semibold tabnums text-ink">
						{new Intl.NumberFormat('pt-BR').format(extrato.validadas)}h
					</p>
					<p class="mt-0.5 text-xs text-muted">
						{new Intl.NumberFormat('pt-BR').format(extrato.pendentes)} pendente(s)
					</p>
				{:else if extratoErro}
					<p class="mt-1 text-xs text-danger">Indisponível</p>
				{:else}
					<div class="mt-2 h-7 w-20 animate-pulse rounded bg-surface" aria-hidden="true"></div>
				{/if}
			</div>
			<div class="rounded-xl border border-border bg-elevated p-4">
				<p class="text-xs text-muted">Treinamentos · média</p>
				<p class="mt-1 font-mono text-2xl font-semibold tabnums text-ink">
					{kpis?.treinamentosMedia !== undefined
						? new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 1 }).format(
								kpis.treinamentosMedia
							)
						: '—'}
				</p>
				<p class="mt-0.5 text-xs text-muted">nota média</p>
			</div>
			<div class="rounded-xl border border-border bg-elevated p-4">
				<p class="text-xs text-muted">Pendências</p>
				<p class="mt-1 font-mono text-2xl font-semibold tabnums text-ink">
					{kpis?.pendencias !== undefined
						? new Intl.NumberFormat('pt-BR').format(kpis.pendencias)
						: '—'}
				</p>
				<p class="mt-0.5 text-xs text-muted">horas e avaliações</p>
			</div>
			<div class="rounded-xl border border-border bg-elevated p-4">
				<p class="text-xs text-muted">Projetos ativos</p>
				<p class="mt-1 font-mono text-2xl font-semibold tabnums text-ink">
					{kpis?.projetosAtivos !== undefined
						? new Intl.NumberFormat('pt-BR').format(kpis.projetosAtivos)
						: '—'}
				</p>
				<p class="mt-0.5 text-xs text-muted">em andamento</p>
			</div>
		</section>

		<!-- Abas -->
		<section class="overflow-hidden rounded-xl border border-border bg-surface" aria-label="Detalhes">
			<div class="border-b border-border px-3 py-2">
				<Tabs tabs={abas} active={aba} onChange={trocarAba} />
			</div>

			{#if aba === 'visao-geral'}
				<div class="grid gap-6 p-5 lg:grid-cols-2">
					<div>
						<h2 class="text-xs font-medium uppercase tracking-wide text-muted">Informações</h2>
						<dl class="mt-3 grid grid-cols-2 gap-x-6 gap-y-3 text-sm">
							<div>
								<dt class="text-xs text-muted">Nome completo</dt>
								<dd class="mt-0.5 text-ink">{pessoa.name}</dd>
							</div>
							<div>
								<dt class="text-xs text-muted">Matrícula</dt>
								<dd class="mt-0.5 font-mono text-xs text-muted">{pessoa.matricula}</dd>
							</div>
							<div>
								<dt class="text-xs text-muted">E-mail</dt>
								<dd class="mt-0.5 break-all text-ink">{pessoa.email}</dd>
							</div>
							<div>
								<dt class="text-xs text-muted">Telefone</dt>
								<dd class="mt-0.5 text-ink">{pessoa.phone ?? '—'}</dd>
							</div>
							<div>
								<dt class="text-xs text-muted">Grupo</dt>
								<dd class="mt-0.5 text-ink">{pessoa.group?.label ?? '—'}</dd>
							</div>
							<div>
								<dt class="text-xs text-muted">Admissão</dt>
								<dd class="mt-0.5 text-ink">{formatarData(pessoa.joinedAt)}</dd>
							</div>
							{#each specEntries as [chave, valor] (chave)}
								<div>
									<dt class="text-xs text-muted">{chave}</dt>
									<dd class="mt-0.5 text-ink">{valor}</dd>
								</div>
							{/each}
						</dl>
					</div>
					<div>
						<h2 class="text-xs font-medium uppercase tracking-wide text-muted">Função</h2>
						<div class="mt-3 rounded-xl border border-border bg-elevated/50 p-4">
							<div class="flex flex-wrap items-center justify-between gap-2">
								<p class="text-sm font-medium text-ink">
									{ehInstrutor ? 'Instrutor(a)' : nivelAtual?.label ?? pessoa.nivel}
								</p>
								{#if qualificacao > 0}
									<span class="text-xs text-brandhi">Qualificação {qualificacao}/6</span>
								{/if}
							</div>
							<div class="mt-2 flex flex-wrap gap-1.5" aria-label="Qualificação técnica">
								{#each [1, 2, 3, 4, 5, 6] as n (n)}
									<span
										class="inline-flex h-7 w-7 items-center justify-center rounded-md border font-mono text-xs {n <= qualificacao
											? 'border-brand/50 bg-brand/15 text-brandhi'
											: 'border-border bg-surface text-muted'}"
									>
										{n}
									</span>
								{/each}
							</div>
						</div>
					</div>
				</div>
			{:else if aba === 'horas'}
				<div class="space-y-3 p-5">
					{#if horasCarregando}
						<TableSkeleton rows={4} columns={4} />
					{:else if horasErro}
						<ErrorBanner
							message="Não foi possível carregar as horas"
							hint="Verifique sua conexão e tente novamente. Se persistir, contate o suporte."
							onRetry={recarregarHoras}
							testid="person-horas-retry"
						/>
					{:else if apontamentos.length === 0}
						<EmptyState
							icon="clock"
							title="Sem apontamentos"
							description="Nenhuma hora registrada para esta pessoa no período."
						/>
					{:else}
						<Tabs
							tabs={[
								{ id: 'pendente', label: 'Pendentes' },
								{ id: 'validada', label: 'Validadas' },
								{ id: 'rejeitada', label: 'Rejeitadas' }
							]}
							active={subAba}
							onChange={(v) => (subAba = v)}
						/>
						{#if apontamentosFiltrados.length === 0}
							<EmptyState
								icon="clock"
								title="Nada por aqui"
								description="Nenhum apontamento com este status."
							/>
						{:else}
							<div class="overflow-x-auto rounded-lg border border-border">
								<table class="w-full min-w-[640px] text-sm">
									<thead>
										<tr class="border-b border-border bg-elevated/50 text-left text-[11px] uppercase tracking-wide text-muted">
											<th class="px-4 py-2.5 font-medium">Data</th>
											<th class="px-4 py-2.5 text-right font-medium">Horas</th>
											<th class="px-4 py-2.5 font-medium">Tipo</th>
											<th class="px-4 py-2.5 font-medium">Referência</th>
											<th class="px-4 py-2.5 font-medium">Status</th>
											<th class="px-4 py-2.5 font-medium">Motivo</th>
										</tr>
									</thead>
									<tbody>
										{#each apontamentosFiltrados as ap (ap.id)}
											{@const hm = horaStatusMeta(ap.status)}
											<tr class="border-b border-border transition last:border-0 hover:bg-elevated/40">
												<td class="px-4 py-3 font-mono text-xs text-muted tabnums">
													{formatarData(ap.date)}
												</td>
												<td class="px-4 py-3 text-right font-mono text-sm tabnums text-ink">
													{new Intl.NumberFormat('pt-BR').format(ap.hours)}h
												</td>
												<td class="px-4 py-3">
													<TypeBadge
														tone={ap.type === 'encomenda' ? 'brand' : 'success'}
														label={ap.type === 'encomenda' ? 'Encomenda' : 'Projeto'}
													/>
												</td>
												<td class="px-4 py-3 font-mono text-xs text-muted">{ap.ref ?? '—'}</td>
												<td class="px-4 py-3">
													<StatusBadge label={hm.label} color={hm.color} />
												</td>
												<td class="max-w-48 truncate px-4 py-3 text-xs text-muted">
													{ap.reason ?? '—'}
												</td>
											</tr>
										{/each}
									</tbody>
								</table>
							</div>
						{/if}
					{/if}
				</div>
			{:else if aba === 'treinamentos'}
				<div class="space-y-3 p-5">
					<div class="rounded-xl border border-border bg-elevated/50 p-4">
						<p class="text-xs text-muted">Nota média em treinamentos</p>
						<p class="mt-1 font-mono text-2xl font-semibold tabnums text-ink">
							{kpis?.treinamentosMedia !== undefined
								? new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 1 }).format(
										kpis.treinamentosMedia
									)
								: '—'}
						</p>
					</div>
					<EmptyState
						icon="document"
						title="Trilhas detalhadas indisponíveis"
						description="O detalhamento de trilhas, notas e guias por pessoa ainda não é servido pelo backend."
					/>
				</div>
			{:else}
				<div class="p-5">
					<ol class="space-y-4" aria-label="Histórico de nível">
						<li class="flex gap-3">
							<span class="mt-1.5 h-2 w-2 shrink-0 rounded-full bg-success" aria-hidden="true"></span>
							<div>
								<p class="text-sm font-medium text-ink">
									{nivelAtual?.label ?? pessoa.nivel} · {statusAtual?.label ?? pessoa.status}
								</p>
								<p class="text-xs text-muted">Nível atual · desde {formatarData(pessoa.joinedAt)}</p>
							</div>
						</li>
						<li class="flex gap-3">
							<span class="mt-1.5 h-2 w-2 shrink-0 rounded-full bg-muted" aria-hidden="true"></span>
							<div>
								<p class="text-sm font-medium text-ink">Admissão no laboratório</p>
								<p class="text-xs text-muted">{formatarData(pessoa.joinedAt)}</p>
							</div>
						</li>
					</ol>
				</div>
			{/if}
		</section>
	{/if}
</div>
