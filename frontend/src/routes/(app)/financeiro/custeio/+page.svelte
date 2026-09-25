<script lang="ts">
	import { goto, invalidateAll } from '$app/navigation';
	import type { PageProps } from './$types';
	import type {
		CustoEncomenda,
		CusteioTab,
		FechamentoEncomenda,
		NivelAcesso,
		SolicitacaoCompra
	} from '$lib/types/financeiro';
	import {
		createFechamento,
		definirOverhead,
		definirValorHora,
		getCusto
	} from '$lib/api/financeiro/custeio';
	import { concluirCompra } from '$lib/api/financeiro/solicitacoes-compra';
	import { compraStatusMeta, fechamentoStatusMeta } from '$lib/utils/financeiro-status';
	import { formatDateBR, formatMoneyBRL } from '$lib/utils/vendas-format';
	import { toUserMessage } from '$lib/utils/errors';
	import { toasts } from '$lib/stores/toast';
	import PageHeader from '$lib/components/ui/PageHeader.svelte';
	import Tabs from '$lib/components/ui/Tabs.svelte';
	import StatusBadge from '$lib/components/ui/StatusBadge.svelte';
	import Icon from '$lib/components/ui/Icon.svelte';
	import TableSkeleton from '$lib/components/ui/TableSkeleton.svelte';
	import EmptyState from '$lib/components/ui/EmptyState.svelte';
	import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte';
	import Modal from '$lib/components/ui/Modal.svelte';
	import Select from '$lib/components/ui/Select.svelte';
	import MoneyInput from '$lib/components/ui/MoneyInput.svelte';

	let { data }: PageProps = $props();

	const tab = $derived<CusteioTab>(data.tab);
	const fechamentos = $derived<FechamentoEncomenda[]>(data.fechamentos);
	const valoresHora = $derived(data.valoresHora);
	const compras = $derived<SolicitacaoCompra[]>(data.compras);
	const fechamentosError = $derived(data.fechamentosError);
	const valoresError = $derived(data.valoresError);
	const comprasError = $derived(data.comprasError);

	const TABS = [
		{ id: 'fechamentos', label: 'Fechamentos' },
		{ id: 'custos', label: 'Custo por encomenda' },
		{ id: 'valores', label: 'Valores/hora & Overhead' },
		{ id: 'compras', label: 'Compras' }
	];

	const NIVEL_NOMES: Record<NivelAcesso, string> = {
		0: 'Admin',
		1: 'Bolsista',
		2: 'Voluntário',
		3: 'Estagiário'
	};

	const abertas = $derived(fechamentos.filter((f) => f.status === 'Aberta'));
	const concluidas = $derived(fechamentos.filter((f) => f.status === 'Concluída').length);

	function irParaTab(id: string): void {
		void goto(`/financeiro/custeio?tab=${id}`);
	}

	function tentarNovamente(): void {
		void invalidateAll();
	}

	function formatarHoras(horas: number): string {
		return `${new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 2 }).format(horas)} h`;
	}

	/** Largura de barra CSS (só apresentação — nunca totaliza valores). */
	function largura(valor: number, maximo: number): number {
		if (maximo <= 0) return 0;
		return Math.max(4, Math.round((valor / maximo) * 100));
	}

	function hojeISO(): string {
		return new Date().toISOString().slice(0, 10);
	}

	// ---- Aba Custos: detalhe servido da encomenda selecionada ----

	let custoId = $state<string | null>(null);
	let custo = $state<CustoEncomenda | null>(null);
	let custoCarregando = $state(false);
	let custoErro = $state<string | null>(null);
	let custoInicializado = $state(false);

	$effect(() => {
		if (!custoInicializado && fechamentos.length > 0) {
			custoInicializado = true;
			custoId = fechamentos[0].idEncomenda;
		}
	});

	$effect(() => {
		const id = custoId;
		if (!id) return;
		recarregarCusto(id);
	});

	function recarregarCusto(id: string): void {
		custoCarregando = true;
		custoErro = null;
		custo = null;
		getCusto(id)
			.then((valor) => {
				if (custoId !== id) return;
				custo = valor;
				custoCarregando = false;
			})
			.catch((err) => {
				if (custoId !== id) return;
				custoErro = toUserMessage(err).message;
				custoCarregando = false;
			});
	}

	const custoMax = $derived(
		Math.max(custo?.custoMateriais ?? 0, custo?.custoMaoObra ?? 0, custo?.custoOverhead ?? 0, 1)
	);
	const margemNegativa = $derived((custo?.margemLucro ?? 0) < 0);

	// ---- Modal: custo detalhe (leitura do servido) ----

	let modalCustoAberto = $state(false);
	let modalCustoTitulo = $state('');
	let modalCustoDado = $state<CustoEncomenda | null>(null);
	let modalCustoCarregando = $state(false);
	let modalCustoErro = $state<string | null>(null);

	async function abrirCustoDetalhe(f: FechamentoEncomenda): Promise<void> {
		modalCustoTitulo = `Encomenda ${f.idEncomenda}`;
		modalCustoAberto = true;
		modalCustoCarregando = true;
		modalCustoErro = null;
		modalCustoDado = null;
		try {
			modalCustoDado = await getCusto(f.idEncomenda);
		} catch (err) {
			modalCustoErro = toUserMessage(err).message;
		} finally {
			modalCustoCarregando = false;
		}
	}

	// ---- Modal: novo fechamento ----

	let modalFechamento = $state(false);
	let novoIdEncomenda = $state('');
	let novoValor = $state<number | null>(null);
	let novoHoras = $state('');
	let novoData = $state('');
	let salvandoFechamento = $state(false);

	const abertasOpcoes = $derived(
		abertas.map((f) => ({ id: f.idEncomenda, label: `Encomenda ${f.idEncomenda}` }))
	);

	function abrirNovoFechamento(): void {
		novoIdEncomenda = '';
		novoValor = null;
		novoHoras = '';
		novoData = hojeISO();
		modalFechamento = true;
	}

	async function confirmarFechamento(): Promise<void> {
		if (!novoIdEncomenda) {
			toasts.warn('Selecione uma encomenda com status Aberta.');
			return;
		}
		if (novoValor === null || novoValor <= 0) {
			toasts.warn('Informe um valor fechado maior que zero.');
			return;
		}
		const horas = Number(novoHoras.replace(',', '.'));
		if (!Number.isFinite(horas) || horas <= 0) {
			toasts.warn('Informe as horas estimadas (maior que zero).');
			return;
		}
		if (!novoData) {
			toasts.warn('Informe a data do fechamento.');
			return;
		}
		salvandoFechamento = true;
		try {
			await createFechamento({
				idEncomenda: novoIdEncomenda,
				horasEstimadas: horas,
				valorFechado: novoValor,
				dataFechamento: novoData
			});
			toasts.success('Fechamento criado — valores congelados.');
			modalFechamento = false;
			await invalidateAll();
		} catch (err) {
			toasts.danger(toUserMessage(err).message);
		} finally {
			salvandoFechamento = false;
		}
	}

	// ---- Modal: valor/hora ----

	let modalValorHora = $state(false);
	let vhNivel = $state<NivelAcesso>(0);
	let vhValor = $state<number | null>(null);
	let vhVigencia = $state('');
	let salvandoVh = $state(false);

	const NIVEL_OPCOES = ([0, 1, 2, 3] as NivelAcesso[]).map((n) => ({
		id: String(n),
		label: `${n} — ${NIVEL_NOMES[n]}`
	}));

	function abrirValorHora(nivel: NivelAcesso): void {
		vhNivel = nivel;
		vhValor = null;
		vhVigencia = hojeISO();
		modalValorHora = true;
	}

	async function confirmarValorHora(): Promise<void> {
		if (vhValor === null || vhValor < 0) {
			toasts.warn('Informe um valor/hora válido (zero ou maior).');
			return;
		}
		if (!vhVigencia) {
			toasts.warn('Informe a data de vigência.');
			return;
		}
		salvandoVh = true;
		try {
			await definirValorHora({ nivelAcesso: vhNivel, valorHora: vhValor, dataVigencia: vhVigencia });
			toasts.success('Valor/hora atualizado com auditoria.');
			modalValorHora = false;
			await invalidateAll();
		} catch (err) {
			toasts.danger(toUserMessage(err).message);
		} finally {
			salvandoVh = false;
		}
	}

	// ---- Modal: overhead ----

	let modalOverhead = $state(false);
	let ohTaxa = $state<number | null>(null);
	let ohVigencia = $state('');
	let salvandoOh = $state(false);
	let taxaVigente = $state<number | null>(null);

	function abrirOverhead(): void {
		ohTaxa = null;
		ohVigencia = hojeISO();
		modalOverhead = true;
	}

	async function confirmarOverhead(): Promise<void> {
		if (ohTaxa === null || ohTaxa < 0) {
			toasts.warn('Informe uma taxa de overhead válida (zero ou maior).');
			return;
		}
		if (!ohVigencia) {
			toasts.warn('Informe a data de vigência.');
			return;
		}
		salvandoOh = true;
		try {
			const retorno = await definirOverhead({ valorTaxaHora: ohTaxa, dataVigencia: ohVigencia });
			taxaVigente = retorno.valorTaxaHora;
			toasts.success('Taxa de overhead atualizada com auditoria.');
			modalOverhead = false;
			await invalidateAll();
		} catch (err) {
			toasts.danger(toUserMessage(err).message);
		} finally {
			salvandoOh = false;
		}
	}

	// ---- Modal: concluir compra ----

	let modalConcluir = $state(false);
	let compraAlvo = $state<SolicitacaoCompra | null>(null);
	let cpData = $state('');
	let cpValor = $state<number | null>(null);
	let cpNota = $state('');
	let salvandoCompra = $state(false);

	function abrirConcluir(c: SolicitacaoCompra): void {
		compraAlvo = c;
		cpData = hojeISO();
		cpValor = null;
		cpNota = '';
		modalConcluir = true;
	}

	async function confirmarConcluir(): Promise<void> {
		if (!compraAlvo) return;
		if (!cpData) {
			toasts.warn('Informe a data de conclusão.');
			return;
		}
		if (cpValor === null || cpValor <= 0) {
			toasts.warn('Informe o valor real maior que zero.');
			return;
		}
		salvandoCompra = true;
		try {
			const retorno = await concluirCompra(compraAlvo.id, {
				dataConclusao: cpData,
				valorReal: cpValor,
				notaFiscal: cpNota || undefined
			});
			toasts.success(`Compra concluída — saída ${retorno.lancamentoGerado} lançada.`);
			modalConcluir = false;
			compraAlvo = null;
			await invalidateAll();
		} catch (err) {
			toasts.danger(toUserMessage(err).message);
		} finally {
			salvandoCompra = false;
		}
	}
</script>

<svelte:head>
	<title>Custeio — Financeiro — FabLab</title>
</svelte:head>

<div class="space-y-4">
	<PageHeader
		title="Custeio"
		subtitle="Fechamento por encomenda, valores/hora, overhead e compras (informativo)."
	>
		{#snippet children()}
			<button
				type="button"
				onclick={() => abrirValorHora(0)}
				aria-label="Definir valores por hora e overhead"
				class="inline-flex items-center gap-1.5 rounded-md border border-border bg-elevated px-3.5 py-2 text-sm font-medium text-ink transition hover:border-brand/50 hover:text-brandhi"
			>
				<Icon name="clock" class="h-4 w-4" /> Valores/hora & overhead
			</button>
			<button
				type="button"
				onclick={abrirNovoFechamento}
				aria-label="Criar novo fechamento de encomenda"
				class="inline-flex items-center gap-1.5 rounded-md bg-brand px-3.5 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi"
			>
				<Icon name="plus" class="h-4 w-4" /> Novo fechamento
			</button>
		{/snippet}
	</PageHeader>

	<div class="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
		<div class="rounded-xl border border-border bg-elevated p-4">
			<p class="text-xs text-muted">Custo médio/encomenda</p>
			<p class="mt-1 font-mono text-lg font-semibold text-ink tabular-nums">—</p>
			<p class="mt-0.5 text-xs text-muted">{fechamentos.length} fechamentos servidos</p>
		</div>
		<div class="rounded-xl border border-border bg-elevated p-4">
			<p class="text-xs text-muted">Margem média</p>
			<p class="mt-1 font-mono text-lg font-semibold text-ink tabular-nums">—</p>
			<p class="mt-0.5 text-xs text-muted">média servida indisponível (D-2)</p>
		</div>
		<div class="rounded-xl border border-border bg-elevated p-4">
			<p class="text-xs text-muted">Overhead acumulado</p>
			<p class="mt-1 font-mono text-lg font-semibold text-ink tabular-nums">
				{taxaVigente !== null ? formatMoneyBRL(taxaVigente) : '—'}
			</p>
			<p class="mt-0.5 text-xs text-muted">taxa/hora vigente (D-5)</p>
		</div>
		<div class="rounded-xl border border-border bg-elevated p-4">
			<p class="text-xs text-muted">Horas validadas</p>
			<p class="mt-1 font-mono text-lg font-semibold text-ink tabular-nums">
				{concluidas} concluídas
			</p>
			<p class="mt-0.5 inline-flex items-center gap-1 text-xs text-success">
				<Icon name="check" class="h-3.5 w-3.5" /> coerência de horas ok ✓
			</p>
		</div>
	</div>

	<div data-testid="custeio-tabs">
		<Tabs tabs={TABS} active={tab} onChange={irParaTab} />
	</div>

	{#if tab === 'fechamentos'}
		<section aria-label="Fechamentos de encomenda" class="space-y-3">
			{#if fechamentosError}
				<ErrorBanner
					message="Não foi possível carregar os fechamentos"
					hint="Verifique sua conexão e tente novamente."
					onRetry={tentarNovamente}
					testid="cus-fech-retry"
				/>
			{:else if fechamentos.length === 0}
				<div class="rounded-xl border border-border bg-surface">
					<EmptyState
						icon="document"
						title="Nenhum fechamento registrado"
						description="Crie o primeiro fechamento para congelar valor e horas estimadas da encomenda."
					>
						{#snippet children()}
							<button
								type="button"
								onclick={abrirNovoFechamento}
								class="inline-flex items-center gap-1.5 rounded-md bg-brand px-3.5 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi"
							>
								<Icon name="plus" class="h-4 w-4" /> Novo fechamento
							</button>
						{/snippet}
					</EmptyState>
				</div>
			{:else}
				<div class="hidden overflow-hidden rounded-xl border border-border bg-surface md:block">
					<div class="overflow-x-auto">
						<table class="w-full min-w-[820px] text-sm">
							<thead>
								<tr
									class="border-b border-border bg-elevated/50 text-left text-[11px] uppercase tracking-wide text-muted"
								>
									<th class="px-4 py-2.5 font-medium">Encomenda</th>
									<th class="px-4 py-2.5 font-medium">Cliente</th>
									<th class="px-4 py-2.5 text-right font-medium">Valor fechado</th>
									<th class="px-4 py-2.5 text-right font-medium">Horas estimadas</th>
									<th class="px-4 py-2.5 font-medium">Fechamento</th>
									<th class="px-4 py-2.5 font-medium">Status</th>
								</tr>
							</thead>
							<tbody>
								{#each fechamentos as f (f.id)}
									{@const st = fechamentoStatusMeta(f.status)}
									<tr
										onclick={() => void abrirCustoDetalhe(f)}
										class="cursor-pointer border-b border-border transition last:border-0 hover:bg-elevated/40"
									>
										<td class="px-4 py-3 font-mono text-xs text-ink">{f.idEncomenda}</td>
										<td class="px-4 py-3 text-xs text-muted" title="Cliente não servido pelo contrato">
											—
										</td>
										<td class="px-4 py-3 text-right font-mono text-sm text-ink tabular-nums">
											{formatMoneyBRL(f.valorFechado)}
										</td>
										<td class="px-4 py-3 text-right font-mono text-xs text-muted tabular-nums">
											{formatarHoras(f.horasEstimadas)}
										</td>
										<td class="px-4 py-3 font-mono text-xs text-muted tabular-nums">
											{formatDateBR(f.dataFechamento)}
										</td>
										<td class="px-4 py-3">
											<StatusBadge label={st.label} color={st.color} />
										</td>
									</tr>
								{/each}
							</tbody>
						</table>
					</div>
				</div>
				<div class="space-y-2 md:hidden">
					{#each fechamentos as f (f.id)}
						{@const st = fechamentoStatusMeta(f.status)}
						<article class="rounded-xl border border-border bg-surface p-4">
							<div class="flex items-start justify-between gap-2">
								<div class="min-w-0">
									<p class="font-mono text-sm font-medium text-ink">{f.idEncomenda}</p>
									<p class="font-mono text-xs text-muted tabular-nums">
										{formatDateBR(f.dataFechamento)} · {formatarHoras(f.horasEstimadas)}
									</p>
								</div>
								<StatusBadge label={st.label} color={st.color} />
							</div>
							<div class="mt-2 flex items-center justify-between gap-2">
								<span class="font-mono text-sm text-ink tabular-nums">
									{formatMoneyBRL(f.valorFechado)}
								</span>
								<button
									type="button"
									onclick={() => void abrirCustoDetalhe(f)}
									aria-label="Ver custo da encomenda {f.idEncomenda}"
									class="rounded-md border border-border bg-elevated px-3 py-1.5 text-xs font-medium text-ink transition hover:border-brand/50 hover:text-brandhi"
								>
									Ver custo
								</button>
							</div>
						</article>
					{/each}
				</div>
			{/if}
			<p class="rounded-lg border border-warn/30 bg-warn/10 px-3 py-2 text-xs text-warn">
				Os valores e horas de um fechamento ficam congelados após a criação. Alterações na
				encomenda encerram a ordem atual e exigem nova ordem + novo fechamento.
			</p>
		</section>
	{:else if tab === 'custos'}
		<section
			aria-label="Custo por encomenda"
			data-testid="cus-tab-custos"
			class="space-y-3 rounded-xl border border-border bg-surface p-5"
		>
			{#if fechamentosError}
				<ErrorBanner
					message="Não foi possível carregar os fechamentos"
					hint="Verifique sua conexão e tente novamente."
					onRetry={tentarNovamente}
					testid="cus-custos-retry"
				/>
			{:else if fechamentos.length === 0}
				<EmptyState
					icon="cube"
					title="Nenhuma encomenda com fechamento"
					description="Crie um fechamento para consultar o custo congelado da encomenda."
				/>
			{:else}
				<div class="max-w-xs">
					<Select
						id="cus-encomenda"
						label="Encomenda"
						options={fechamentos.map((f) => ({ id: f.idEncomenda, label: `Encomenda ${f.idEncomenda}` }))}
						value={custoId ?? ''}
						onChange={(v) => (custoId = v)}
					/>
				</div>
				{#if custoCarregando}
					<TableSkeleton rows={4} columns={3} />
				{:else if custoErro}
					<ErrorBanner
						message="Não foi possível carregar o custo da encomenda"
						hint={custoErro}
						onRetry={() => {
							if (custoId) recarregarCusto(custoId);
						}}
						testid="cus-custo-retry"
					/>
				{:else if custo}
					<div class="grid gap-3 sm:grid-cols-3">
						<div class="rounded-xl border border-border bg-elevated p-4">
							<p class="text-xs text-muted">Materiais</p>
							<p class="mt-1 font-mono text-lg font-semibold text-ink tabular-nums">
								{formatMoneyBRL(custo.custoMateriais)}
							</p>
							<div
								class="mt-2 h-1.5 rounded bg-surface"
								role="img"
								aria-label="Materiais {formatMoneyBRL(custo.custoMateriais)}"
							>
								<div
									class="h-1.5 rounded bg-brand"
									style="width: {largura(custo.custoMateriais, custoMax)}%"
								></div>
							</div>
						</div>
						<div class="rounded-xl border border-border bg-elevated p-4">
							<p class="text-xs text-muted">Mão de obra</p>
							<p class="mt-1 font-mono text-lg font-semibold text-ink tabular-nums">
								{formatMoneyBRL(custo.custoMaoObra)}
							</p>
							<div
								class="mt-2 h-1.5 rounded bg-surface"
								role="img"
								aria-label="Mão de obra {formatMoneyBRL(custo.custoMaoObra)}"
							>
								<div
									class="h-1.5 rounded bg-brandhi"
									style="width: {largura(custo.custoMaoObra, custoMax)}%"
								></div>
							</div>
						</div>
						<div class="rounded-xl border border-border bg-elevated p-4">
							<p class="text-xs text-muted">Overhead</p>
							<p class="mt-1 font-mono text-lg font-semibold text-ink tabular-nums">
								{formatMoneyBRL(custo.custoOverhead)}
							</p>
							<div
								class="mt-2 h-1.5 rounded bg-surface"
								role="img"
								aria-label="Overhead {formatMoneyBRL(custo.custoOverhead)}"
							>
								<div
									class="h-1.5 rounded bg-warn"
									style="width: {largura(custo.custoOverhead, custoMax)}%"
								></div>
							</div>
						</div>
					</div>
					<div class="grid gap-3 sm:grid-cols-2">
						<div class="rounded-xl border border-border bg-elevated p-4">
							<p class="text-xs text-muted">Custo total (servido)</p>
							<p class="mt-1 font-mono text-lg font-semibold text-ink tabular-nums">
								{formatMoneyBRL(custo.custoTotal)}
							</p>
							<p class="mt-0.5 text-xs text-muted">
								Venda {formatMoneyBRL(custo.valorVenda)} · calculado em {formatDateBR(
									custo.dataCalculo
								)}
							</p>
						</div>
						<div class="rounded-xl border border-border bg-elevated p-4">
							<p class="text-xs text-muted">Margem (R$ servida)</p>
							<p
								class="mt-1 font-mono text-lg font-semibold tabular-nums {margemNegativa
									? 'text-danger'
									: 'text-success'}"
							>
								{formatMoneyBRL(custo.margemLucro)}
							</p>
							<p class="mt-0.5 text-xs text-muted">percentual não servido pelo contrato</p>
						</div>
					</div>
				{/if}
			{/if}
		</section>
	{:else if tab === 'valores'}
		<section aria-label="Valores por hora e overhead" class="space-y-3">
			{#if valoresError}
				<ErrorBanner
					message="Não foi possível carregar os valores/hora"
					hint="Verifique sua conexão e tente novamente."
					onRetry={tentarNovamente}
					testid="cus-valores-retry"
				/>
			{:else if valoresHora.length === 0}
				<div class="rounded-xl border border-border bg-surface">
					<EmptyState
						icon="clock"
						title="Nenhum valor/hora vigente"
						description="Defina o valor/hora por nível de acesso para alimentar o custeio."
					>
						{#snippet children()}
							<button
								type="button"
								onclick={() => abrirValorHora(0)}
								class="inline-flex items-center gap-1.5 rounded-md bg-brand px-3.5 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi"
							>
								<Icon name="plus" class="h-4 w-4" /> Definir valor/hora
							</button>
						{/snippet}
					</EmptyState>
				</div>
			{:else}
				<div class="overflow-hidden rounded-xl border border-border bg-surface">
					<div class="overflow-x-auto">
						<table class="w-full min-w-[560px] text-sm">
							<thead>
								<tr
									class="border-b border-border bg-elevated/50 text-left text-[11px] uppercase tracking-wide text-muted"
								>
									<th class="px-4 py-2.5 font-medium">Nível</th>
									<th class="px-4 py-2.5 text-right font-medium">Valor/hora</th>
									<th class="px-4 py-2.5 font-medium">Vigência</th>
									<th class="w-24 px-4 py-2.5 text-right font-medium">
										<span class="sr-only">Ações</span>
									</th>
								</tr>
							</thead>
							<tbody>
								{#each valoresHora as v (v.nivelAcesso)}
									<tr class="border-b border-border transition last:border-0 hover:bg-elevated/40">
										<td class="px-4 py-3 font-medium text-ink">
											{v.nivelAcesso} — {NIVEL_NOMES[v.nivelAcesso]}
										</td>
										<td class="px-4 py-3 text-right font-mono text-sm text-ink tabular-nums">
											{formatMoneyBRL(v.valorHora)}
										</td>
										<td class="px-4 py-3 font-mono text-xs text-muted tabular-nums">
											{formatDateBR(v.dataVigencia)}
										</td>
										<td class="px-4 py-3 text-right">
											<button
												type="button"
												onclick={() => abrirValorHora(v.nivelAcesso)}
												aria-label="Editar valor/hora do nível {v.nivelAcesso}"
												class="inline-flex items-center gap-1 rounded-md border border-border bg-elevated px-2.5 py-1.5 text-xs font-medium text-ink transition hover:border-brand/50 hover:text-brandhi"
											>
												<Icon name="pencil" class="h-3.5 w-3.5" /> Editar
											</button>
										</td>
									</tr>
								{/each}
							</tbody>
						</table>
					</div>
				</div>
			{/if}
			<div class="rounded-xl border border-border bg-surface p-5">
				<div class="flex flex-wrap items-center justify-between gap-2">
					<div>
						<h2 class="text-sm font-semibold text-ink">Overhead por hora</h2>
						<p class="mt-0.5 font-mono text-xs text-muted">overhead = Σ horas × taxa</p>
					</div>
					<button
						type="button"
						onclick={abrirOverhead}
						aria-label="Editar taxa de overhead"
						class="inline-flex items-center gap-1 rounded-md border border-border bg-elevated px-3 py-1.5 text-xs font-medium text-ink transition hover:border-brand/50 hover:text-brandhi"
					>
						<Icon name="pencil" class="h-3.5 w-3.5" /> Editar taxa
					</button>
				</div>
				<p class="mt-2 font-mono text-lg font-semibold text-ink tabular-nums">
					{taxaVigente !== null ? `${formatMoneyBRL(taxaVigente)}/h` : 'R$ —/h'}
				</p>
				<p class="mt-0.5 text-xs text-muted">
					{taxaVigente !== null
						? 'Taxa definida nesta sessão (servida pelo backend).'
						: 'Taxa vigente não servida — sem endpoint de consulta (D-5).'}
				</p>
			</div>
			<p class="rounded-lg border border-border bg-elevated px-3 py-2 text-xs text-muted">
				Alterações de valores/hora e overhead entram em vigor na data informada e ficam
				registradas com trilha de auditoria no backend (docs/05 §8).
			</p>
		</section>
	{:else}
		<section aria-label="Compras (informativo)" class="space-y-3">
			<p class="rounded-lg border border-brand/30 bg-brand/10 px-3 py-2 text-xs text-brandhi">
				Fluxo informativo — não há bloqueio: concluir uma compra apenas registra a saída
				correspondente no financeiro.
			</p>
			{#if comprasError}
				<ErrorBanner
					message="Não foi possível carregar as solicitações de compra"
					hint="Verifique sua conexão e tente novamente."
					onRetry={tentarNovamente}
					testid="cus-compras-retry"
				/>
			{:else if compras.length === 0}
				<div class="rounded-xl border border-border bg-surface">
					<EmptyState
						icon="shopping-bag"
						title="Nenhuma solicitação de compra"
						description="As solicitações registradas pela operação aparecem aqui para acompanhamento."
					/>
				</div>
			{:else}
				<div class="overflow-hidden rounded-xl border border-border bg-surface">
					<div class="overflow-x-auto">
						<table class="w-full min-w-[760px] text-sm">
							<thead>
								<tr
									class="border-b border-border bg-elevated/50 text-left text-[11px] uppercase tracking-wide text-muted"
								>
									<th class="px-4 py-2.5 font-medium">Item</th>
									<th class="px-4 py-2.5 text-right font-medium">Qtd</th>
									<th class="px-4 py-2.5 text-right font-medium">Valor estimado</th>
									<th class="px-4 py-2.5 font-medium">Solicitação</th>
									<th class="px-4 py-2.5 font-medium">Status</th>
									<th class="w-40 px-4 py-2.5 text-right font-medium">
										<span class="sr-only">Ações</span>
									</th>
								</tr>
							</thead>
							<tbody>
								{#each compras as c (c.id)}
									{@const st = compraStatusMeta(c.status)}
									<tr class="border-b border-border transition last:border-0 hover:bg-elevated/40">
										<td class="px-4 py-3 font-medium text-ink">{c.item}</td>
										<td class="px-4 py-3 text-right font-mono text-sm text-ink tabular-nums">
											{c.quantidade}
										</td>
										<td class="px-4 py-3 text-right font-mono text-sm text-ink tabular-nums">
											{formatMoneyBRL(c.valorEstimado)}
										</td>
										<td class="px-4 py-3 font-mono text-xs text-muted">{c.id}</td>
										<td class="px-4 py-3">
											<StatusBadge label={st.label} color={st.color} />
										</td>
										<td class="px-4 py-3 text-right">
											{#if c.status === 'Concluída'}
												<span class="inline-flex items-center gap-1 text-xs text-success">
													<Icon name="check" class="h-3.5 w-3.5" /> saída lançada
													{#if c.lancamentoGerado}
														<span class="font-mono text-[11px] text-muted">{c.lancamentoGerado}</span>
													{/if}
												</span>
											{:else}
												<button
													type="button"
													data-testid="cus-concluir"
													onclick={() => abrirConcluir(c)}
													aria-label="Concluir compra {c.item}"
													class="inline-flex items-center gap-1 rounded-md bg-success/15 px-2.5 py-1.5 text-xs font-medium text-success transition hover:bg-success/25"
												>
													<Icon name="check" class="h-3.5 w-3.5" /> Concluir
												</button>
											{/if}
										</td>
									</tr>
								{/each}
							</tbody>
						</table>
					</div>
				</div>
			{/if}
		</section>
	{/if}
</div>

<Modal
	open={modalFechamento}
	title="Novo fechamento"
	subtitle="Congela valor e horas estimadas da encomenda"
	onClose={() => (modalFechamento = false)}
	width="sm"
>
	{#snippet children()}
		<div class="space-y-3">
			<p class="rounded-lg border border-warn/30 bg-warn/10 px-3 py-2 text-xs text-warn">
				Atenção: o fechamento congela horas e valor — após criado, só uma nova ordem + novo
				fechamento alteram a encomenda.
			</p>
			<Select
				id="cus-novo-encomenda"
				label="Encomenda"
				options={abertasOpcoes}
				value={novoIdEncomenda}
				onChange={(v) => (novoIdEncomenda = v)}
				placeholder="Selecione…"
				required
				hint={abertasOpcoes.length === 0
					? 'Nenhuma encomenda com status Aberta.'
					: 'Somente encomendas com status Aberta.'}
			/>
			<MoneyInput bind:value={novoValor} label="Valor fechado" />
			<div>
				<label for="cus-novo-horas" class="mb-1 block text-xs font-medium text-muted">
					Horas estimadas <span class="text-danger">*</span>
				</label>
				<input
					id="cus-novo-horas"
					type="text"
					inputmode="decimal"
					bind:value={novoHoras}
					placeholder="Ex.: 8,0"
					class="w-full rounded-md border border-border bg-elevated px-3 py-2.5 font-mono text-sm text-ink placeholder:text-muted/60 focus:border-brand focus:ring-2 focus:ring-brand/30 focus:outline-none"
				/>
			</div>
			<div>
				<label for="cus-novo-data" class="mb-1 block text-xs font-medium text-muted">
					Data do fechamento <span class="text-danger">*</span>
				</label>
				<input
					id="cus-novo-data"
					type="date"
					bind:value={novoData}
					class="w-full rounded-md border border-border bg-elevated px-3 py-2.5 text-sm text-ink focus:border-brand focus:ring-2 focus:ring-brand/30 focus:outline-none"
				/>
			</div>
		</div>
	{/snippet}
	{#snippet footer()}
		<button
			type="button"
			onclick={() => (modalFechamento = false)}
			disabled={salvandoFechamento}
			class="rounded-md border border-border bg-surface px-4 py-2 text-sm font-medium text-ink transition hover:bg-elevated disabled:opacity-50"
		>
			Cancelar
		</button>
		<button
			type="button"
			onclick={() => void confirmarFechamento()}
			disabled={salvandoFechamento}
			class="rounded-md bg-brand px-4 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi disabled:opacity-50"
		>
			{salvandoFechamento ? 'Salvando…' : 'Criar fechamento'}
		</button>
	{/snippet}
</Modal>

<Modal
	open={modalValorHora}
	title="Definir valor/hora"
	subtitle="Valor por nível de acesso, com vigência"
	onClose={() => (modalValorHora = false)}
	width="sm"
>
	{#snippet children()}
		<div class="space-y-3">
			<Select
				id="cus-vh-nivel"
				label="Nível de acesso"
				options={NIVEL_OPCOES}
				value={String(vhNivel)}
				onChange={(v) => (vhNivel = Number(v) as NivelAcesso)}
				required
			/>
			<MoneyInput bind:value={vhValor} label="Valor/hora" />
			<div>
				<label for="cus-vh-vigencia" class="mb-1 block text-xs font-medium text-muted">
					Data de vigência <span class="text-danger">*</span>
				</label>
				<input
					id="cus-vh-vigencia"
					type="date"
					bind:value={vhVigencia}
					class="w-full rounded-md border border-border bg-elevated px-3 py-2.5 text-sm text-ink focus:border-brand focus:ring-2 focus:ring-brand/30 focus:outline-none"
				/>
			</div>
		</div>
	{/snippet}
	{#snippet footer()}
		<button
			type="button"
			onclick={() => (modalValorHora = false)}
			disabled={salvandoVh}
			class="rounded-md border border-border bg-surface px-4 py-2 text-sm font-medium text-ink transition hover:bg-elevated disabled:opacity-50"
		>
			Cancelar
		</button>
		<button
			type="button"
			onclick={() => void confirmarValorHora()}
			disabled={salvandoVh}
			class="rounded-md bg-brand px-4 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi disabled:opacity-50"
		>
			{salvandoVh ? 'Salvando…' : 'Definir valor/hora'}
		</button>
	{/snippet}
</Modal>

<Modal
	open={modalOverhead}
	title="Editar taxa de overhead"
	subtitle="Taxa por hora, com vigência e auditoria"
	onClose={() => (modalOverhead = false)}
	width="sm"
>
	{#snippet children()}
		<div class="space-y-3">
			<MoneyInput bind:value={ohTaxa} label="Taxa por hora" />
			<div>
				<label for="cus-oh-vigencia" class="mb-1 block text-xs font-medium text-muted">
					Data de vigência <span class="text-danger">*</span>
				</label>
				<input
					id="cus-oh-vigencia"
					type="date"
					bind:value={ohVigencia}
					class="w-full rounded-md border border-border bg-elevated px-3 py-2.5 text-sm text-ink focus:border-brand focus:ring-2 focus:ring-brand/30 focus:outline-none"
				/>
			</div>
			<p class="rounded-lg border border-border bg-elevated px-3 py-2 text-xs text-muted">
				Fórmula servida: overhead = Σ horas × taxa. O cálculo é do backend.
			</p>
		</div>
	{/snippet}
	{#snippet footer()}
		<button
			type="button"
			onclick={() => (modalOverhead = false)}
			disabled={salvandoOh}
			class="rounded-md border border-border bg-surface px-4 py-2 text-sm font-medium text-ink transition hover:bg-elevated disabled:opacity-50"
		>
			Cancelar
		</button>
		<button
			type="button"
			onclick={() => void confirmarOverhead()}
			disabled={salvandoOh}
			class="rounded-md bg-brand px-4 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi disabled:opacity-50"
		>
			{salvandoOh ? 'Salvando…' : 'Definir taxa'}
		</button>
	{/snippet}
</Modal>

<Modal
	open={modalConcluir}
	title="Concluir compra"
	subtitle={compraAlvo ? compraAlvo.item : ''}
	onClose={() => (modalConcluir = false)}
	width="sm"
>
	{#snippet children()}
		<div class="space-y-3">
			<div>
				<label for="cus-cp-data" class="mb-1 block text-xs font-medium text-muted">
					Data de conclusão <span class="text-danger">*</span>
				</label>
				<input
					id="cus-cp-data"
					type="date"
					bind:value={cpData}
					class="w-full rounded-md border border-border bg-elevated px-3 py-2.5 text-sm text-ink focus:border-brand focus:ring-2 focus:ring-brand/30 focus:outline-none"
				/>
			</div>
			<MoneyInput bind:value={cpValor} label="Valor real" />
			<div>
				<label for="cus-cp-nota" class="mb-1 block text-xs font-medium text-muted">
					Nota fiscal
				</label>
				<input
					id="cus-cp-nota"
					type="text"
					bind:value={cpNota}
					placeholder="Opcional"
					class="w-full rounded-md border border-border bg-elevated px-3 py-2.5 text-sm text-ink placeholder:text-muted/60 focus:border-brand focus:ring-2 focus:ring-brand/30 focus:outline-none"
				/>
			</div>
			<p class="rounded-lg border border-brand/30 bg-brand/10 px-3 py-2 text-xs text-brandhi">
				A conclusão gera uma saída (lançamento) correspondente. Fluxo informativo, sem bloqueio.
			</p>
		</div>
	{/snippet}
	{#snippet footer()}
		<button
			type="button"
			onclick={() => (modalConcluir = false)}
			disabled={salvandoCompra}
			class="rounded-md border border-border bg-surface px-4 py-2 text-sm font-medium text-ink transition hover:bg-elevated disabled:opacity-50"
		>
			Cancelar
		</button>
		<button
			type="button"
			onclick={() => void confirmarConcluir()}
			disabled={salvandoCompra}
			class="rounded-md bg-brand px-4 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi disabled:opacity-50"
		>
			{salvandoCompra ? 'Salvando…' : 'Concluir compra'}
		</button>
	{/snippet}
</Modal>

<Modal
	open={modalCustoAberto}
	title="Custo da encomenda"
	subtitle={modalCustoTitulo}
	onClose={() => (modalCustoAberto = false)}
	width="sm"
>
	{#snippet children()}
		{#if modalCustoCarregando}
			<TableSkeleton rows={5} columns={2} />
		{:else if modalCustoErro}
			<ErrorBanner
				message="Não foi possível carregar o custo"
				hint={modalCustoErro}
				testid="cus-custo-retry"
			/>
		{:else if modalCustoDado}
			{@const mc = modalCustoDado}
			<dl class="space-y-2.5 text-sm">
				<div class="flex items-center justify-between gap-2">
					<dt class="text-xs text-muted">Materiais</dt>
					<dd class="font-mono font-medium text-ink tabular-nums">
						{formatMoneyBRL(mc.custoMateriais)}
					</dd>
				</div>
				<div class="flex items-center justify-between gap-2">
					<dt class="text-xs text-muted">Mão de obra</dt>
					<dd class="font-mono font-medium text-ink tabular-nums">
						{formatMoneyBRL(mc.custoMaoObra)}
					</dd>
				</div>
				<div class="flex items-center justify-between gap-2">
					<dt class="text-xs text-muted">Overhead</dt>
					<dd class="font-mono font-medium text-ink tabular-nums">
						{formatMoneyBRL(mc.custoOverhead)}
					</dd>
				</div>
				<div class="flex items-center justify-between gap-2 border-t border-border pt-2.5">
					<dt class="text-xs text-muted">Custo total</dt>
					<dd class="font-mono font-semibold text-ink tabular-nums">
						{formatMoneyBRL(mc.custoTotal)}
					</dd>
				</div>
				<div class="flex items-center justify-between gap-2">
					<dt class="text-xs text-muted">Margem (R$)</dt>
					<dd
						class="font-mono font-semibold tabular-nums {mc.margemLucro < 0
							? 'text-danger'
							: 'text-success'}"
					>
						{formatMoneyBRL(mc.margemLucro)}
					</dd>
				</div>
				<div class="flex items-center justify-between gap-2">
					<dt class="text-xs text-muted">Calculado em</dt>
					<dd class="font-mono text-xs text-muted">{formatDateBR(mc.dataCalculo)}</dd>
				</div>
			</dl>
			<p class="mt-3 rounded-lg border border-warn/30 bg-warn/10 px-3 py-2 text-xs text-warn">
				Custo congelado na entrega — nunca recalculado no client.
			</p>
		{/if}
	{/snippet}
	{#snippet footer()}
		<button
			type="button"
			onclick={() => (modalCustoAberto = false)}
			class="rounded-md border border-border bg-surface px-4 py-2 text-sm font-medium text-ink transition hover:bg-elevated"
		>
			Fechar
		</button>
	{/snippet}
</Modal>
