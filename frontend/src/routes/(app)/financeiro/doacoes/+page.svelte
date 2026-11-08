<script lang="ts">
	import { goto, invalidateAll } from '$app/navigation';
	import type { PageProps } from './$types';
	import type { DoacaoRecurso, TipoDoacao } from '$lib/types/financeiro';
	import { createDoacao } from '$lib/api/financeiro/doacoes';
	import { doacaoTipoMeta } from '$lib/utils/financeiro-status';
	import { formatDateBR, formatMoneyBRL } from '$lib/utils/vendas-format';
	import { toUserMessage } from '$lib/utils/errors';
	import { toasts } from '$lib/stores/toast';
	import PageHeader from '$lib/components/ui/PageHeader.svelte';
	import SearchInput from '$lib/components/ui/SearchInput.svelte';
	import Dropdown from '$lib/components/ui/Dropdown.svelte';
import Chip from '$lib/components/ui/Chip.svelte';
import TypeBadge from '$lib/components/ui/TypeBadge.svelte';
	import Icon from '$lib/components/ui/Icon.svelte';
	import TableSkeleton from '$lib/components/ui/TableSkeleton.svelte';
	import EmptyState from '$lib/components/ui/EmptyState.svelte';
	import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte';
	import RowActions from '$lib/components/ui/RowActions.svelte';
	import Modal from '$lib/components/ui/Modal.svelte';
	import Select from '$lib/components/ui/Select.svelte';
	import MoneyInput from '$lib/components/ui/MoneyInput.svelte';

	let { data }: PageProps = $props();

	const params = $derived(data.params);
	const resultado = $derived(data.resultado);
	const erro = $derived(data.error);

	const registros = $derived(resultado?.registros ?? []);
	const resumo = $derived(resultado?.resumo);
	const cobertura = $derived(resultado?.cobertura);

	const carregando = $derived(resultado === null && erro === null);
	const comErro = $derived(erro !== null && resultado === null);

	const temFiltros = $derived(
		params.search !== '' || params.tipo !== '' || params.periodo !== ''
	);

	const TIPO_OPCOES = ['Doação', 'Projeto'].map((id) => ({ id, label: id }));
	const PERIODO_OPCOES = [
		{ id: 'mes', label: 'Este mês' },
		{ id: 'ano', label: 'Este ano' },
		{ id: '2026', label: '2026' },
		{ id: '2025', label: '2025' }
	];

	// Contagens de exibição sobre os itens servidos (sem somar valores — R-7).
	const qtdDoacoes = $derived(registros.filter((r) => r.tipo === 'Doação').length);
	const qtdProjetos = $derived(registros.filter((r) => r.tipo === 'Projeto').length);

	interface MesContagem {
		mes: string;
		rotulo: string;
		quantidade: number;
	}

	// Distribuição mensal por contagem de registros servidos (série de valores não servida — D-2).
	const porMes = $derived.by((): MesContagem[] => {
		const mapa = new Map<string, number>();
		for (const r of registros) {
			const chave = r.dataRecebimento.slice(0, 7);
			if (/^\d{4}-\d{2}$/.test(chave)) mapa.set(chave, (mapa.get(chave) ?? 0) + 1);
		}
		return [...mapa.entries()]
			.sort(([a], [b]) => (a < b ? -1 : 1))
			.slice(-6)
			.map(([mes, quantidade]) => {
				const [ano, mm] = mes.split('-');
				return { mes, rotulo: `${mm}/${ano}`, quantidade };
			});
	});
	const maxMes = $derived(Math.max(...porMes.map((m) => m.quantidade), 1));

	function largura(valor: number, maximo: number): number {
		if (maximo <= 0) return 0;
		return Math.max(4, Math.round((valor / maximo) * 100));
	}

	const PROJETO_OPCOES = $derived(
		[...new Set(registros.map((r) => r.idProjetoAssociado).filter((p) => p !== null && p !== ''))].map(
			(p) => ({ id: p as string, label: p as string })
		)
	);

	interface Query {
		[key: string]: string | number | undefined;
	}

	function navegar(overrides: Query): void {
		const base: Query = {
			search: params.search || undefined,
			tipo: params.tipo || undefined,
			periodo: params.periodo || undefined,
			page: params.page
		};
		const merged = { ...base, ...overrides };
		const url = new URLSearchParams();
		for (const [chave, valor] of Object.entries(merged)) {
			if (valor !== undefined && valor !== '') url.append(chave, String(valor));
		}
		const qs = url.toString();
		void goto(`/financeiro/doacoes${qs ? `?${qs}` : ''}`);
	}

	function onSearch(termo: string): void {
		navegar({ search: termo || undefined, page: 1 });
	}

	function alternarFiltro(chave: 'tipo' | 'periodo', id: string): void {
		const atual = params[chave];
		navegar({ [chave]: atual === id ? undefined : id, page: 1 });
	}

	function limparFiltro(chave: 'tipo' | 'periodo'): void {
		navegar({ [chave]: undefined, page: 1 });
	}

	function limparTodos(): void {
		navegar({ search: undefined, tipo: undefined, periodo: undefined, page: 1 });
	}

	function tentarNovamente(): void {
		void goto(`/financeiro/doacoes${window.location.search}`, { invalidateAll: true });
	}

	function projetoDe(r: DoacaoRecurso): string {
		return r.idProjetoAssociado?.trim() ? (r.idProjetoAssociado as string) : '—';
	}

	// ---- Modal: nova doação/recurso ----

	let modalNova = $state(false);
	let novoTipo = $state<TipoDoacao>('Doação');
	let novoOrigem = $state('');
	let novoValor = $state<number | null>(null);
	let novoRecebimento = $state('');
	let novoProjeto = $state('');
	let ocupado = $state(false);

	function hojeISO(): string {
		return new Date().toISOString().slice(0, 10);
	}

	function abrirNova(): void {
		novoTipo = 'Doação';
		novoOrigem = '';
		novoValor = null;
		novoRecebimento = hojeISO();
		novoProjeto = '';
		modalNova = true;
	}

	async function confirmarNova(): Promise<void> {
		if (!novoOrigem.trim()) {
			toasts.warn('Informe a origem da doação ou recurso.');
			return;
		}
		if (novoValor === null || novoValor <= 0) {
			toasts.warn('Informe um valor maior que zero.');
			return;
		}
		if (!novoRecebimento) {
			toasts.warn('Informe a data de recebimento.');
			return;
		}
		ocupado = true;
		try {
			await createDoacao({
				tipo: novoTipo,
				origem: novoOrigem.trim(),
				valor: novoValor,
				dataRecebimento: novoRecebimento,
				idProjetoAssociado: novoProjeto || null
			});
			toasts.success('Doação registrada com sucesso.');
			modalNova = false;
			await invalidateAll();
		} catch (err) {
			toasts.danger(toUserMessage(err).message);
		} finally {
			ocupado = false;
		}
	}

	// ---- Ficha local ----

	let detalhe = $state<DoacaoRecurso | null>(null);
	const detalheTipo = $derived(detalhe ? doacaoTipoMeta(detalhe.tipo) : null);
</script>

<svelte:head>
	<title>Doações & recursos — Financeiro — FabLab</title>
</svelte:head>

<div class="space-y-4">
	<PageHeader
		title="Doações & recursos"
		subtitle="Doações e recursos de projetos universitários do laboratório."
	>
		{#snippet children()}
			<button
				type="button"
				data-testid="doa-nova"
				onclick={abrirNova}
				aria-label="Nova doação ou recurso"
				class="inline-flex items-center gap-1.5 rounded-md bg-brand px-3.5 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi"
			>
				<Icon name="plus" class="h-4 w-4" /> Nova doação/recurso
			</button>
		{/snippet}
	</PageHeader>

	{#if comErro}
		<ErrorBanner
			message="Não foi possível carregar as doações e recursos"
			hint="Verifique sua conexão e tente novamente. Se persistir, contate o suporte."
			onRetry={tentarNovamente}
			testid="doa-retry"
		/>
	{:else if carregando}
		<div class="grid gap-3 sm:grid-cols-2 lg:grid-cols-4" aria-hidden="true">
			{#each Array(4) as _, i (i)}
				<div class="h-20 animate-pulse rounded-xl bg-elevated"></div>
			{/each}
		</div>
		<div class="rounded-xl border border-border bg-surface p-4">
			<TableSkeleton rows={6} columns={5} />
		</div>
	{:else}
		{#if resumo}
			<div class="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
				<div class="rounded-xl border border-border bg-elevated p-4">
					<p class="text-xs text-muted">Recebido no ano</p>
					<p class="mt-1 font-mono text-lg font-semibold text-success tabular-nums">
						{formatMoneyBRL(resumo.recebidoAno)}
					</p>
					<p class="mt-0.5 text-xs text-muted">no período</p>
				</div>
				<div class="rounded-xl border border-border bg-elevated p-4">
					<p class="text-xs text-muted">Doações</p>
					<p class="mt-1 font-mono text-lg font-semibold text-ink tabular-nums">
						{formatMoneyBRL(resumo.totalDoacoes)}
					</p>
					<p class="mt-0.5 text-xs text-muted">{qtdDoacoes} doações</p>
				</div>
				<div class="rounded-xl border border-border bg-elevated p-4">
					<p class="text-xs text-muted">Recursos de projetos</p>
					<p class="mt-1 font-mono text-lg font-semibold text-ink tabular-nums">
						{formatMoneyBRL(resumo.totalProjetos)}
					</p>
					<p class="mt-0.5 text-xs text-muted">{qtdProjetos} projetos</p>
				</div>
				<div class="rounded-xl border border-border bg-elevated p-4">
					<p class="text-xs text-muted">Doação média</p>
					<p class="mt-1 font-mono text-lg font-semibold text-ink tabular-nums">
						{formatMoneyBRL(resumo.mediaDoacao)}
					</p>
					<p class="mt-0.5 text-xs text-muted">por registro</p>
				</div>
			</div>
		{/if}

		{#if cobertura}
			<section
				aria-label="Doações versus despesas"
				class="rounded-xl border border-border bg-surface p-5"
			>
				<div class="flex items-center justify-between gap-2">
					<h2 class="text-sm font-semibold text-ink">Doações vs. Despesas</h2>
					<span
						class="inline-flex items-center rounded-full bg-success/10 px-2.5 py-0.5 text-xs font-medium text-success"
					>
						Cobertura {cobertura.percentual}%
					</span>
				</div>
				<div
					class="mt-3 h-2 rounded bg-elevated"
					role="img"
					aria-label="Cobertura de despesas: {cobertura.percentual} por cento"
				>
					<div
						class="h-2 rounded bg-success"
						style="width: {Math.max(0, Math.min(100, cobertura.percentual))}%"
					></div>
				</div>
				{#if porMes.length > 0}
					<div class="mt-4 flex items-end gap-2" aria-hidden="true">
						{#each porMes as m (m.mes)}
							<div class="flex flex-1 flex-col items-center gap-1">
								<div class="flex h-20 w-full items-end rounded bg-elevated/60 p-1">
									<div
										class="w-full rounded bg-brand"
										style="height: {largura(m.quantidade, maxMes)}%"
									></div>
								</div>
								<span class="text-[10px] text-muted">{m.rotulo}</span>
							</div>
						{/each}
					</div>
					<p class="mt-2 text-[10px] text-muted">
						Registros por mês (contagem da página servida — série de valores não servida).
					</p>
				{:else}
					<p class="mt-3 text-xs text-muted">Sem registros para exibir a distribuição mensal.</p>
				{/if}
			</section>
		{/if}

		<div class="flex flex-wrap items-center gap-2">
			<div class="min-w-56 flex-1">
				<SearchInput
					value={params.search}
					onSearch={onSearch}
					placeholder="Buscar por origem ou projeto…"
					delay={300}
					label="Buscar doações e recursos"
				/>
			</div>
			<div data-testid="fpanel-doa-tipo">
				<Dropdown
					label="Tipo"
					options={TIPO_OPCOES}
					selected={params.tipo ? [params.tipo] : []}
					onToggle={(id) => alternarFiltro('tipo', id)}
					onClear={() => limparFiltro('tipo')}
					search={false}
				/>
			</div>
			<div data-testid="fpanel-doa-periodo">
				<Dropdown
					label="Período"
					options={PERIODO_OPCOES}
					selected={params.periodo ? [params.periodo] : []}
					onToggle={(id) => alternarFiltro('periodo', id)}
					onClear={() => limparFiltro('periodo')}
					search={false}
				/>
			</div>
		</div>

		{#if temFiltros}
			<div data-testid="fin-chips" class="flex flex-wrap items-center gap-1.5">
				{#if params.search}
					<Chip label={`Busca: ${params.search}`} onRemove={() => onSearch('')} />
				{/if}
				{#if params.tipo}
					<Chip label={params.tipo} onRemove={() => limparFiltro('tipo')} />
				{/if}
				{#if params.periodo}
					<Chip
						label={PERIODO_OPCOES.find((o) => o.id === params.periodo)?.label ?? params.periodo}
						onRemove={() => limparFiltro('periodo')}
					/>
				{/if}
				<button
					type="button"
					onclick={limparTodos}
					class="text-xs font-medium text-brandhi transition-colors hover:text-brand"
				>
					Limpar filtros
				</button>
			</div>
		{/if}

		{#if registros.length === 0}
			<div class="rounded-xl border border-border bg-surface">
				{#if temFiltros}
					<EmptyState
						icon="search"
						title="Nenhum registro encontrado"
						description="Nenhuma doação ou recurso combina com os filtros aplicados."
					>
						{#snippet children()}
							<button
								type="button"
								onclick={limparTodos}
								class="rounded-md border border-border bg-elevated px-3 py-2 text-sm font-medium text-ink transition hover:border-brand/50 hover:text-brandhi"
							>
								Limpar filtros
							</button>
						{/snippet}
					</EmptyState>
				{:else}
					<EmptyState
						icon="document"
						title="Nenhuma doação registrada"
						description="Registre a primeira doação ou recurso de projeto do laboratório."
					>
						{#snippet children()}
							<button
								type="button"
								onclick={abrirNova}
								class="inline-flex items-center gap-1.5 rounded-md bg-brand px-3.5 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi"
							>
								<Icon name="plus" class="h-4 w-4" /> Nova doação/recurso
							</button>
						{/snippet}
					</EmptyState>
				{/if}
			</div>
		{:else}
			<div class="hidden overflow-hidden rounded-xl border border-border bg-surface md:block">
				<div class="overflow-x-auto">
					<table class="w-full min-w-[760px] text-sm">
						<thead>
							<tr
								class="border-b border-border bg-elevated/50 text-left text-[11px] uppercase tracking-wide text-muted"
							>
								<th class="px-4 py-2.5 font-medium">Tipo</th>
								<th class="px-4 py-2.5 font-medium">Origem</th>
								<th class="px-4 py-2.5 font-medium">Projeto associado</th>
								<th class="px-4 py-2.5 font-medium">Recebimento</th>
								<th class="px-4 py-2.5 text-right font-medium">Valor</th>
								<th class="w-12 px-4 py-2.5 text-right font-medium">
									<span class="sr-only">Ações</span>
								</th>
							</tr>
						</thead>
						<tbody>
							{#each registros as r (r.id)}
								{@const tm = doacaoTipoMeta(r.tipo)}
								<tr
									data-testid="doa-row"
									onclick={() => (detalhe = r)}
									class="cursor-pointer border-b border-border transition last:border-0 hover:bg-elevated/40"
								>
									<td class="px-4 py-3">
										<TypeBadge tone={tm.color === 'success' ? 'success' : 'brand'} label={tm.label} />
									</td>
									<td class="px-4 py-3 font-medium text-ink">{r.origem}</td>
									<td class="px-4 py-3 font-mono text-xs text-muted">{projetoDe(r)}</td>
									<td class="px-4 py-3 font-mono text-xs text-muted tabular-nums">
										{formatDateBR(r.dataRecebimento)}
									</td>
									<td class="px-4 py-3 text-right font-mono text-sm text-success tabular-nums">
										{formatMoneyBRL(r.valor)}
									</td>
									<td class="px-4 py-3 text-right" onclick={(e) => e.stopPropagation()}>
										<RowActions
											label={`Ações de ${r.origem}`}
											actions={[{ id: 'detalhe', label: 'Ver ficha', icon: 'eye' }]}
											onSelect={() => (detalhe = r)}
										/>
									</td>
								</tr>
							{/each}
						</tbody>
					</table>
				</div>
				<div class="border-t border-border px-4 py-3">
					<p class="text-xs text-muted">{registros.length} registros no período</p>
				</div>
			</div>

			<div class="space-y-2 md:hidden">
				{#each registros as r (r.id)}
					{@const tm = doacaoTipoMeta(r.tipo)}
					<article data-testid="doa-row" class="rounded-xl border border-border bg-surface p-4">
						<div class="flex items-start justify-between gap-2">
							<div class="min-w-0">
								<p class="truncate text-sm font-medium text-ink">{r.origem}</p>
								<p class="font-mono text-xs text-muted">
									{projetoDe(r)} · {formatDateBR(r.dataRecebimento)}
								</p>
							</div>
							<TypeBadge tone={tm.color === 'success' ? 'success' : 'brand'} label={tm.label} />
						</div>
						<div class="mt-2 flex items-center justify-between gap-2">
							<span class="font-mono text-sm text-success tabular-nums">
								{formatMoneyBRL(r.valor)}
							</span>
							<RowActions
								label={`Ações de ${r.origem}`}
								actions={[{ id: 'detalhe', label: 'Ver ficha', icon: 'eye' }]}
								onSelect={() => (detalhe = r)}
							/>
						</div>
					</article>
				{/each}
				<div class="rounded-xl border border-border bg-surface px-4 py-3">
					<p class="text-xs text-muted">{registros.length} registros no período</p>
				</div>
			</div>
		{/if}
	{/if}
</div>

<Modal
	open={modalNova}
	title="Nova doação/recurso"
	subtitle="Registre uma doação ou recurso de projeto"
	onClose={() => (modalNova = false)}
	width="sm"
>
	{#snippet children()}
		<div class="space-y-3">
			<fieldset>
				<legend class="mb-1 block text-xs font-medium text-muted">
					Tipo <span class="text-danger">*</span>
				</legend>
				<div class="flex gap-2">
					{#each [{ id: 'Doação', label: 'Doação' }, { id: 'Projeto', label: 'Projeto' }] as opt (opt.id)}
						<label
							class="flex flex-1 cursor-pointer items-center gap-2 rounded-md border px-3 py-2.5 text-sm transition {novoTipo === opt.id
								? 'border-brand/60 bg-brand/10 text-ink'
								: 'border-border bg-elevated text-muted'}"
						>
							<input
								type="radio"
								name="doa-novo-tipo"
								value={opt.id}
								checked={novoTipo === opt.id}
								onchange={() => (novoTipo = opt.id as TipoDoacao)}
								class="h-4 w-4 accent-brand"
							/>
							{opt.label}
						</label>
					{/each}
				</div>
			</fieldset>
			<div>
				<label for="doa-nova-origem" class="mb-1 block text-xs font-medium text-muted">
					Origem <span class="text-danger">*</span>
				</label>
				<input
					id="doa-nova-origem"
					type="text"
					bind:value={novoOrigem}
					placeholder="Ex.: Grupo Mãos que Criam"
					class="w-full rounded-md border border-border bg-elevated px-3 py-2.5 text-sm text-ink placeholder:text-muted/60 focus:border-brand focus:ring-2 focus:ring-brand/30 focus:outline-none"
				/>
			</div>
			<MoneyInput bind:value={novoValor} label="Valor" />
			<div>
				<label for="doa-novo-recebimento" class="mb-1 block text-xs font-medium text-muted">
					Recebimento <span class="text-danger">*</span>
				</label>
				<input
					id="doa-novo-recebimento"
					type="date"
					bind:value={novoRecebimento}
					class="w-full rounded-md border border-border bg-elevated px-3 py-2.5 text-sm text-ink focus:border-brand focus:ring-2 focus:ring-brand/30 focus:outline-none"
				/>
			</div>
			<Select
				id="doa-novo-projeto"
				label="Projeto universitário"
				options={PROJETO_OPCOES}
				value={novoProjeto}
				onChange={(v) => (novoProjeto = v)}
				placeholder="—"
				hint={PROJETO_OPCOES.length === 0
					? 'Nenhum projeto nos registros — ficará sem vínculo.'
					: 'Opcional — vincule a um projeto existente.'}
			/>
			<p class="rounded-lg border border-brand/30 bg-brand/10 px-3 py-2 text-xs text-brandhi">
				O registro gera uma entrada separada das receitas operacionais.
			</p>
		</div>
	{/snippet}
	{#snippet footer()}
		<button
			type="button"
			onclick={() => (modalNova = false)}
			disabled={ocupado}
			class="rounded-md border border-border bg-surface px-4 py-2 text-sm font-medium text-ink transition hover:bg-elevated disabled:opacity-50"
		>
			Cancelar
		</button>
		<button
			type="button"
			onclick={() => void confirmarNova()}
			disabled={ocupado}
			class="rounded-md bg-brand px-4 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi disabled:opacity-50"
		>
			{ocupado ? 'Salvando…' : 'Registrar doação'}
		</button>
	{/snippet}
</Modal>

<Modal
	open={detalhe !== null}
	title="Ficha do registro"
	subtitle={detalhe ? detalhe.origem : ''}
	onClose={() => (detalhe = null)}
	width="sm"
>
	{#snippet children()}
		{#if detalhe && detalheTipo}
			<dl class="space-y-2.5 text-sm">
				<div class="flex items-center justify-between gap-2">
					<dt class="text-xs text-muted">Tipo</dt>
					<dd>
						<TypeBadge
							tone={detalheTipo.color === 'success' ? 'success' : 'brand'}
							label={detalheTipo.label}
						/>
					</dd>
				</div>
				<div class="flex items-center justify-between gap-2">
					<dt class="text-xs text-muted">Origem</dt>
					<dd class="text-ink">{detalhe.origem}</dd>
				</div>
				<div class="flex items-center justify-between gap-2">
					<dt class="text-xs text-muted">Valor</dt>
					<dd class="font-mono font-semibold text-success tabular-nums">
						{formatMoneyBRL(detalhe.valor)}
					</dd>
				</div>
				<div class="flex items-center justify-between gap-2">
					<dt class="text-xs text-muted">Recebimento</dt>
					<dd class="font-mono text-xs text-ink">{formatDateBR(detalhe.dataRecebimento)}</dd>
				</div>
				<div class="flex items-center justify-between gap-2">
					<dt class="text-xs text-muted">Projeto associado</dt>
					<dd class="font-mono text-xs text-ink">{projetoDe(detalhe)}</dd>
				</div>
			</dl>
		{/if}
	{/snippet}
	{#snippet footer()}
		<button
			type="button"
			onclick={() => (detalhe = null)}
			class="rounded-md border border-border bg-surface px-4 py-2 text-sm font-medium text-ink transition hover:bg-elevated"
		>
			Fechar
		</button>
	{/snippet}
</Modal>
