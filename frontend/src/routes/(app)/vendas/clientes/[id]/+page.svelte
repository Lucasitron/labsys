<script lang="ts">
	import { goto } from '$app/navigation';
	import type { PageProps } from './$types';
	import { get } from 'svelte/store';
	import { auth } from '$lib/stores/auth';
	import type { Encomenda, Interacao, Orcamento, TipoInteracao } from '$lib/types/vendas';
	import type { Tone } from '$lib/types/stock';
	import { deleteCliente } from '$lib/api/vendas/clientes';
	import { ApiError } from '$lib/api/client';
	import { listInteracoes, registrarInteracao } from '$lib/api/vendas/interacoes';
	import { listOrcamentos } from '$lib/api/vendas/orcamentos';
	import { listEncomendas } from '$lib/api/vendas/encomendas';
	import { canEditVendas } from '$lib/utils/permissions';
	import { toUserMessage } from '$lib/utils/errors';
	import { toasts } from '$lib/stores/toast';
	import { formatMoneyBRL, formatDateBR, maskDocumento } from '$lib/utils/vendas-format';
	import { formatNumber } from '$lib/utils/format';
	import {
		interacaoTipoMeta,
		kanbanStatusMeta,
		orcamentoStatusMeta
	} from '$lib/utils/vendas-status';
	import Avatar from '$lib/components/ui/Avatar.svelte';
	import StatusBadge from '$lib/components/ui/StatusBadge.svelte';
	import TypeBadge from '$lib/components/ui/TypeBadge.svelte';
	import Tabs from '$lib/components/ui/Tabs.svelte';
	import Timeline, { type TimelineItem } from '$lib/components/ui/Timeline.svelte';
	import EmptyState from '$lib/components/ui/EmptyState.svelte';
	import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte';
	import TableSkeleton from '$lib/components/ui/TableSkeleton.svelte';
	import Icon from '$lib/components/ui/Icon.svelte';
	import RowActions from '$lib/components/ui/RowActions.svelte';
	import ConfirmDialog from '$lib/components/ui/ConfirmDialog.svelte';
	import Modal from '$lib/components/ui/Modal.svelte';
	import RadioCards from '$lib/components/ui/RadioCards.svelte';
	import SolicitarEdicaoModal from '../../components/SolicitarEdicaoModal.svelte';

	let { data }: PageProps = $props();

	const usuario = $derived(get(auth).user);
	const cliente = $derived(data.cliente);
	const naoEncontrado = $derived(data.notFound);
	const erroPagina = $derived(data.error);
	const id = $derived(data.id);
	const aba = $derived(data.tab);

	const podeEditar = $derived(
		cliente ? canEditVendas(usuario, { createdBy: cliente.createdBy }) : false
	);

	function voltar(): void {
		if (window.history.length > 1) window.history.back();
		else void goto('/vendas/clientes');
	}

	function trocarAba(nova: string): void {
		const qs = new URLSearchParams(window.location.search);
		qs.set('tab', nova);
		void goto(`/vendas/clientes/${id}?${qs.toString()}`);
	}

	function dataBR(iso: string | null | undefined): string {
		if (!iso) return '—';
		if (Number.isNaN(new Date(iso).getTime())) return '—';
		return formatDateBR(iso);
	}

	function tipoPessoaLabel(tipo: 'pf' | 'pj'): string {
		return tipo === 'pf' ? 'Pessoa física' : 'Pessoa jurídica';
	}

	function diasRelacionamento(iso: string): string {
		const tempo = new Date(iso).getTime();
		if (Number.isNaN(tempo)) return '—';
		const dias = Math.max(0, Math.floor((Date.now() - tempo) / 86_400_000));
		return `${formatNumber(dias)} ${dias === 1 ? 'dia' : 'dias'}`;
	}

	function tagTone(cor: Tone): 'brand' | 'success' | 'warn' | 'danger' | 'muted' {
		return cor === 'ink' ? 'muted' : cor;
	}

	function telefoneHref(telefone: string): string {
		const digitos = telefone.replace(/\D/g, '');
		return digitos ? `tel:+${digitos}` : '';
	}

	const emailValido = $derived(
		cliente ? /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(cliente.email.trim()) : false
	);
	const telefoneDigitos = $derived(cliente ? cliente.telefone.replace(/\D/g, '') : '');

	const abas = [
		{ id: 'visao-geral', label: 'Visão geral' },
		{ id: 'interacoes', label: 'Interações' },
		{ id: 'orcamentos', label: 'Orçamentos' },
		{ id: 'encomendas', label: 'Encomendas' }
	];

	// ---- Excluir (DELETE /api/vendas/clientes/{id} 🔴) ----

	let confirmarExclusao = $state(false);
	let ocupado = $state(false);

	async function confirmarExcluir(): Promise<void> {
		if (!cliente || !canEditVendas(usuario, { createdBy: cliente.createdBy })) {
			toasts.warn('Sem permissão para esta ação.');
			return;
		}
		ocupado = true;
		try {
			await deleteCliente(id);
			toasts.success('Cliente excluído com sucesso.');
			void goto('/vendas/clientes');
		} catch (err) {
			if (err instanceof ApiError && err.status === 403) {
				toasts.warn('Sem permissão para esta ação.');
			} else {
				toasts.danger(toUserMessage(err).message);
			}
		} finally {
			ocupado = false;
		}
	}

	// ---- Sugerir alteração (modal 🟡) ----

	let sugerirAberto = $state(false);

	// ---- Interações (GET /api/vendas/clientes/{id}/interacoes 🟡; erro parcial) ----

	let interacoes = $state<Interacao[]>([]);
	let interacoesErro = $state<string | null>(null);
	let interacoesCarregando = $state(false);

	function carregarInteracoes(): void {
		interacoesCarregando = true;
		interacoesErro = null;
		const ctrl = new AbortController();
		const sinal = ctrl.signal;
		listInteracoes(id, (input, init) => fetch(input, { ...init, signal: sinal }))
			.then((res) => {
				if (!sinal.aborted) interacoes = res;
			})
			.catch((err: unknown) => {
				if (!sinal.aborted) interacoesErro = toUserMessage(err).message;
			})
			.finally(() => {
				if (!sinal.aborted) interacoesCarregando = false;
			});
	}

	$effect(() => {
		if (aba === 'interacoes' && cliente) carregarInteracoes();
	});

	const timelineItens = $derived<TimelineItem[]>(
		interacoes.map((i) => ({
			id: i.id,
			tone: interacaoTipoMeta(i.tipo).color,
			title: `${i.tipo} · ${i.responsavel}`,
			body: i.descricao,
			meta: dataBR(i.dataInteracao)
		}))
	);

	// ---- Registrar interação (POST /api/vendas/interacoes 🟡; modal local) ----

	let modalInteracao = $state(false);
	let interTipo = $state<TipoInteracao>('E-mail');
	let interData = $state('');
	let interDescricao = $state('');
	let interOcupado = $state(false);
	let interErros = $state<Record<string, string>>({});

	const INTER_TIPOS: TipoInteracao[] = ['E-mail', 'Telefone', 'Reunião', 'WhatsApp'];

	function abrirModalInteracao(): void {
		interTipo = 'E-mail';
		interData = new Date().toISOString().slice(0, 16);
		interDescricao = '';
		interErros = {};
		modalInteracao = true;
	}

	async function salvarInteracao(): Promise<void> {
		if (interOcupado) return;
		const novos: Record<string, string> = {};
		if (!interData) novos['data'] = 'Informe a data da interação.';
		if (!interDescricao.trim()) novos['descricao'] = 'Informe a descrição.';
		interErros = novos;
		if (Object.keys(novos).length > 0) return;
		interOcupado = true;
		try {
			await registrarInteracao({
				clienteId: id,
				tipo: interTipo,
				descricao: interDescricao.trim(),
				dataInteracao: new Date(interData).toISOString(),
				...(usuario?.id ? { responsavelId: usuario.id } : {})
			});
			toasts.success('Interação registrada.');
			modalInteracao = false;
			carregarInteracoes();
		} catch (err) {
			toasts.danger(toUserMessage(err).message);
		} finally {
			interOcupado = false;
		}
	}

	// ---- Orçamentos do cliente (filtro por clienteId; erro parcial) ----

	let orcamentos = $state<Orcamento[]>([]);
	let orcamentosErro = $state<string | null>(null);
	let orcamentosCarregando = $state(false);

	function carregarOrcamentos(): void {
		orcamentosCarregando = true;
		orcamentosErro = null;
		const ctrl = new AbortController();
		const sinal = ctrl.signal;
		listOrcamentos({ clienteId: id }, (input, init) => fetch(input, { ...init, signal: sinal }))
			.then((res) => {
				if (!sinal.aborted) orcamentos = res.orcamentos;
			})
			.catch((err: unknown) => {
				if (!sinal.aborted) orcamentosErro = toUserMessage(err).message;
			})
			.finally(() => {
				if (!sinal.aborted) orcamentosCarregando = false;
			});
	}

	$effect(() => {
		if (aba === 'orcamentos' && cliente) carregarOrcamentos();
	});

	// ---- Encomendas do cliente (filtro por clienteId; erro parcial) ----

	let encomendas = $state<Encomenda[]>([]);
	let encomendasErro = $state<string | null>(null);
	let encomendasCarregando = $state(false);

	function carregarEncomendas(): void {
		encomendasCarregando = true;
		encomendasErro = null;
		const ctrl = new AbortController();
		const sinal = ctrl.signal;
		listEncomendas({ clienteId: id }, (input, init) => fetch(input, { ...init, signal: sinal }))
			.then((res) => {
				if (!sinal.aborted) encomendas = res.encomendas;
			})
			.catch((err: unknown) => {
				if (!sinal.aborted) encomendasErro = toUserMessage(err).message;
			})
			.finally(() => {
				if (!sinal.aborted) encomendasCarregando = false;
			});
	}

	$effect(() => {
		if (aba === 'encomendas' && cliente) carregarEncomendas();
	});

	const inputCls =
		'w-full rounded-md border border-border bg-elevated px-3 py-2.5 text-sm text-ink placeholder:text-muted/60 focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/30 transition';
	const inputErroCls = 'border-danger/60 focus:border-danger focus:ring-danger/30';
	const labelCls = 'mb-1 block text-xs font-medium text-muted';
</script>

<svelte:head>
	<title>{cliente ? `${cliente.nome} — Clientes` : 'Cliente — FabLab'}</title>
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
		{#if cliente && !naoEncontrado}
			<div class="flex items-center gap-2">
				{#if podeEditar}
					<a
						href={`/vendas/clientes/novo?editar=${id}`}
						class="inline-flex items-center gap-1.5 rounded-md border border-border bg-elevated px-3 py-2 text-sm transition hover:border-brand/50 hover:text-brandhi"
					>
						<Icon name="pencil" class="h-3.5 w-3.5" /> Editar
					</a>
				{:else}
					<button
						type="button"
						onclick={() => (sugerirAberto = true)}
						class="inline-flex items-center gap-1.5 rounded-md border border-border bg-elevated px-3 py-2 text-sm transition hover:border-brand/50 hover:text-brandhi"
					>
						<Icon name="document" class="h-3.5 w-3.5" /> Sugerir alteração
					</button>
				{/if}
				<RowActions
					label={`Ações de ${cliente.nome}`}
					actions={[
						{ id: 'duplicar', label: 'Duplicar', icon: 'duplicate' },
						{
							id: 'excluir',
							label: 'Excluir',
							icon: 'trash',
							tone: 'danger',
							hidden: !podeEditar
						}
					]}
					onSelect={(acao) => {
						if (acao === 'duplicar') void goto(`/vendas/clientes/novo?duplicar=${id}`);
						else if (acao === 'excluir') confirmarExclusao = true;
					}}
				/>
			</div>
		{/if}
	</div>

	{#if naoEncontrado}
		<div class="rounded-xl border border-border bg-surface">
			<EmptyState
				icon="user"
				title="Cliente não encontrado"
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
	{:else if erroPagina || !cliente}
		<ErrorBanner
			message="Não foi possível carregar o cliente"
			hint="Verifique sua conexão e tente novamente. Se persistir, contate o suporte."
			onRetry={() => void goto(`/vendas/clientes/${id}${window.location.search}`, { invalidateAll: true })}
			testid="cliente-detalhe-retry"
		/>
	{:else}
		<!-- Hero -->
		<section class="overflow-hidden rounded-xl border border-border bg-surface" aria-label="Perfil do cliente">
			<div class="flex flex-col gap-4 p-5 sm:flex-row sm:items-center">
				<Avatar name={cliente.nome} size="md" tone="brand" />
				<div class="min-w-0 flex-1">
					<div class="flex flex-wrap items-center gap-2">
						<h1 class="text-2xl font-semibold tracking-tight text-ink">{cliente.nome}</h1>
					</div>
					<p class="mt-1.5 font-mono text-xs text-muted">
						{cliente.codigo} · {tipoPessoaLabel(cliente.tipoPessoa)} · desde {dataBR(
							cliente.dataCadastro
						)}
					</p>
					{#if cliente.tags.length > 0}
						<div class="mt-2 flex flex-wrap items-center gap-1.5">
							{#each cliente.tags as tag (tag.id)}
								<TypeBadge tone={tagTone(tag.cor)} label={tag.nome} />
							{/each}
						</div>
					{/if}
				</div>
				<div class="flex shrink-0 items-center gap-2">
					{#if emailValido}
						<a
							href={`mailto:${encodeURIComponent(cliente.email)}`}
							aria-label={`Enviar e-mail para ${cliente.nome}`}
							title={cliente.email}
							class="inline-flex h-10 w-10 items-center justify-center rounded-md border border-border bg-elevated text-base transition hover:border-brand/50 hover:text-brandhi"
						>
							<span aria-hidden="true">✉</span>
						</a>
					{:else}
						<span
							title="E-mail inválido"
							class="inline-flex h-10 w-10 items-center justify-center rounded-md border border-border bg-elevated text-base opacity-50"
						>
							<span aria-hidden="true">✉</span>
						</span>
					{/if}
					{#if telefoneDigitos}
						<a
							href={telefoneHref(cliente.telefone)}
							aria-label={`Ligar para ${cliente.nome}`}
							title={cliente.telefone}
							class="inline-flex h-10 w-10 items-center justify-center rounded-md border border-border bg-elevated text-base transition hover:border-brand/50 hover:text-brandhi"
						>
							<span aria-hidden="true">📞</span>
						</a>
					{:else}
						<span
							title="Telefone inválido"
							class="inline-flex h-10 w-10 items-center justify-center rounded-md border border-border bg-elevated text-base text-muted"
						>
							<span aria-hidden="true">—</span>
						</span>
					{/if}
				</div>
			</div>
		</section>

		<!-- KPIs (valores servidos pelo backend; nunca somados no client) -->
		<section class="grid grid-cols-2 gap-4 lg:grid-cols-4" aria-label="Indicadores">
			<div class="rounded-xl border border-border bg-elevated p-4">
				<p class="text-xs text-muted">Total encomendado</p>
				<p class="mt-1 font-mono text-2xl font-semibold tabnums text-ink">
					{typeof cliente.kpis?.totalEncomendado === 'number'
						? formatMoneyBRL(cliente.kpis.totalEncomendado)
						: '—'}
				</p>
				<p class="mt-0.5 text-xs text-muted">acumulado</p>
			</div>
			<div class="rounded-xl border border-border bg-elevated p-4">
				<p class="text-xs text-muted">Orçamentos em aberto</p>
				<p class="mt-1 font-mono text-2xl font-semibold tabnums text-ink">
					{typeof cliente.kpis?.orcamentosAbertos === 'number'
						? formatNumber(cliente.kpis.orcamentosAbertos)
						: '—'}
				</p>
				<p class="mt-0.5 text-xs text-muted">aguardando decisão</p>
			</div>
			<div class="rounded-xl border border-border bg-elevated p-4">
				<p class="text-xs text-muted">Encomendas em produção</p>
				<p class="mt-1 font-mono text-2xl font-semibold tabnums text-ink">
					{typeof cliente.kpis?.encomendasProducao === 'number'
						? formatNumber(cliente.kpis.encomendasProducao)
						: '—'}
				</p>
				<p class="mt-0.5 text-xs text-muted">em andamento</p>
			</div>
			<div class="rounded-xl border border-border bg-elevated p-4">
				<p class="text-xs text-muted">Última interação</p>
				<p class="mt-1 font-mono text-2xl font-semibold tabnums text-ink">
					{cliente.kpis?.ultimaInteracao ? dataBR(cliente.kpis.ultimaInteracao) : '—'}
				</p>
				<p class="mt-0.5 text-xs text-muted">último contato</p>
			</div>
		</section>

		<!-- Abas -->
		<section class="overflow-hidden rounded-xl border border-border bg-surface" aria-label="Detalhes do cliente">
			<div class="border-b border-border px-3 py-2" data-testid="cliente-tabs">
				<Tabs tabs={abas} active={aba} onChange={trocarAba} />
			</div>

			{#if aba === 'visao-geral'}
				<div class="grid gap-6 p-5 lg:grid-cols-2">
					<div>
						<h2 class="text-xs font-medium uppercase tracking-wide text-muted">Informações</h2>
						<dl class="mt-3 grid grid-cols-2 gap-x-6 gap-y-3 text-sm">
							<div>
								<dt class="text-xs text-muted">Nome / Razão social</dt>
								<dd class="mt-0.5 text-ink">{cliente.nome}</dd>
							</div>
							<div>
								<dt class="text-xs text-muted">Código</dt>
								<dd class="mt-0.5 font-mono text-xs text-muted">{cliente.codigo}</dd>
							</div>
							<div>
								<dt class="text-xs text-muted">{cliente.tipoPessoa === 'pf' ? 'CPF' : 'CNPJ'}</dt>
								<dd class="mt-0.5 font-mono text-xs text-muted">
									{#if podeEditar}{cliente.documento}{:else}{maskDocumento(cliente.documento)}{/if}
								</dd>
							</div>
							<div>
								<dt class="text-xs text-muted">Tipo</dt>
								<dd class="mt-0.5 text-ink">{tipoPessoaLabel(cliente.tipoPessoa)}</dd>
							</div>
							<div>
								<dt class="text-xs text-muted">E-mail</dt>
								<dd class="mt-0.5 break-all text-ink">{cliente.email}</dd>
							</div>
							<div>
								<dt class="text-xs text-muted">Telefone</dt>
								<dd class="mt-0.5 text-ink">{cliente.telefone}</dd>
							</div>
							<div class="col-span-2">
								<dt class="text-xs text-muted">Endereço</dt>
								<dd class="mt-0.5 text-ink">{cliente.endereco || '—'}</dd>
							</div>
							<div>
								<dt class="text-xs text-muted">Cliente desde</dt>
								<dd class="mt-0.5 text-ink">{dataBR(cliente.dataCadastro)}</dd>
							</div>
							<div>
								<dt class="text-xs text-muted">Tags</dt>
								<dd class="mt-0.5">
									{#if cliente.tags.length > 0}
										<span class="flex flex-wrap gap-1">
											{#each cliente.tags as tag (tag.id)}
												<TypeBadge tone={tagTone(tag.cor)} label={tag.nome} />
											{/each}
										</span>
									{:else}
										<span class="text-ink">—</span>
									{/if}
								</dd>
							</div>
						</dl>
					</div>
					<div>
						<h2 class="text-xs font-medium uppercase tracking-wide text-muted">Relacionamento</h2>
						<div class="mt-3 rounded-xl border border-border bg-elevated/50 p-4">
							<p class="text-xs text-muted">Idade do relacionamento</p>
							<p class="mt-1 font-mono text-2xl font-semibold tabnums text-ink">
								{diasRelacionamento(cliente.dataCadastro)}
							</p>
							<p class="mt-0.5 text-xs text-muted">desde {dataBR(cliente.dataCadastro)}</p>
						</div>
					</div>
				</div>
			{:else if aba === 'interacoes'}
				<div class="space-y-4 p-5">
					<div class="flex flex-wrap items-center justify-between gap-2">
						<h2 class="text-sm font-semibold text-ink">Timeline de interações</h2>
						<button
							type="button"
							onclick={abrirModalInteracao}
							class="inline-flex items-center gap-1.5 rounded-md bg-brand px-3 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi"
						>
							<Icon name="plus" class="h-4 w-4" /> Registrar interação
						</button>
					</div>
					{#if interacoesCarregando}
						<TableSkeleton rows={4} columns={3} />
					{:else if interacoesErro}
						<ErrorBanner
							message="Não foi possível carregar as interações"
							hint="Verifique sua conexão e tente novamente."
							onRetry={carregarInteracoes}
						/>
					{:else if interacoes.length === 0}
						<EmptyState
							icon="clock"
							title="Sem interações"
							description="Nenhuma interação registrada com este cliente ainda."
						>
							{#snippet children()}
								<button
									type="button"
									onclick={abrirModalInteracao}
									class="inline-flex items-center gap-1.5 rounded-md bg-brand px-3.5 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi"
								>
									<Icon name="plus" class="h-4 w-4" /> Registrar interação
								</button>
							{/snippet}
						</EmptyState>
					{:else}
						<Timeline items={timelineItens} label="Interações com o cliente" />
					{/if}
				</div>
			{:else if aba === 'orcamentos'}
				<div class="space-y-3 p-5">
					<h2 class="text-sm font-semibold text-ink">Orçamentos do cliente</h2>
					{#if orcamentosCarregando}
						<TableSkeleton rows={4} columns={4} />
					{:else if orcamentosErro}
						<ErrorBanner
							message="Não foi possível carregar os orçamentos"
							hint="Verifique sua conexão e tente novamente."
							onRetry={carregarOrcamentos}
						/>
					{:else if orcamentos.length === 0}
						<EmptyState
							icon="document"
							title="Sem orçamentos"
							description="Nenhum orçamento vinculado a este cliente."
						/>
					{:else}
						<div class="overflow-x-auto rounded-lg border border-border">
							<table class="w-full min-w-[560px] text-sm">
								<thead>
									<tr class="border-b border-border bg-elevated/50 text-left text-[11px] uppercase tracking-wide text-muted">
										<th class="px-4 py-2.5 font-medium">Código</th>
										<th class="px-4 py-2.5 text-right font-medium">Valor</th>
										<th class="px-4 py-2.5 font-medium">Validade</th>
										<th class="px-4 py-2.5 font-medium">Status</th>
									</tr>
								</thead>
								<tbody>
									{#each orcamentos as orc (orc.id)}
										{@const meta = orcamentoStatusMeta(orc.status)}
										<tr class="border-b border-border transition last:border-0 hover:bg-elevated/40">
											<td class="px-4 py-3 font-mono text-xs text-muted">{orc.codigo}</td>
											<td class="px-4 py-3 text-right font-mono text-sm tabnums text-ink">
												{formatMoneyBRL(orc.valorTotal)}
											</td>
											<td class="px-4 py-3 font-mono text-xs text-muted tabnums">{dataBR(orc.validade)}</td>
											<td class="px-4 py-3">
												<StatusBadge label={meta.label} color={meta.color} />
											</td>
										</tr>
									{/each}
								</tbody>
							</table>
						</div>
					{/if}
				</div>
			{:else}
				<div class="space-y-3 p-5">
					<h2 class="text-sm font-semibold text-ink">Encomendas do cliente</h2>
					{#if encomendasCarregando}
						<TableSkeleton rows={4} columns={4} />
					{:else if encomendasErro}
						<ErrorBanner
							message="Não foi possível carregar as encomendas"
							hint="Verifique sua conexão e tente novamente."
							onRetry={carregarEncomendas}
						/>
					{:else if encomendas.length === 0}
						<EmptyState
							icon="box"
							title="Sem encomendas"
							description="Nenhuma encomenda vinculada a este cliente."
						/>
					{:else}
						<div class="overflow-x-auto rounded-lg border border-border">
							<table class="w-full min-w-[560px] text-sm">
								<thead>
									<tr class="border-b border-border bg-elevated/50 text-left text-[11px] uppercase tracking-wide text-muted">
										<th class="px-4 py-2.5 font-medium">Código</th>
										<th class="px-4 py-2.5 font-medium">Previsão</th>
										<th class="px-4 py-2.5 text-right font-medium">Valor</th>
										<th class="px-4 py-2.5 font-medium">Etapa</th>
									</tr>
								</thead>
								<tbody>
									{#each encomendas as enc (enc.id)}
										{@const meta = kanbanStatusMeta(enc.statusKanban)}
										<tr class="border-b border-border transition last:border-0 hover:bg-elevated/40">
											<td class="px-4 py-3 font-mono text-xs text-muted">{enc.codigo}</td>
											<td class="px-4 py-3 font-mono text-xs text-muted tabnums">{dataBR(enc.previsao)}</td>
											<td class="px-4 py-3 text-right font-mono text-sm tabnums text-ink">
												{formatMoneyBRL(enc.valorFinal)}
											</td>
											<td class="px-4 py-3">
												<StatusBadge label={meta.label} color={meta.color} />
											</td>
										</tr>
									{/each}
								</tbody>
							</table>
						</div>
					{/if}
				</div>
			{/if}
		</section>
	{/if}
</div>

<ConfirmDialog
	open={confirmarExclusao}
	title="Excluir cliente"
	message="Tem certeza que deseja excluir este cliente? Esta ação não pode ser desfeita."
	confirmLabel="Excluir"
	loading={ocupado}
	onCancel={() => (confirmarExclusao = false)}
	onConfirm={() => void confirmarExcluir()}
/>

{#if sugerirAberto && cliente}
	<SolicitarEdicaoModal
		alvo={{ tipo: 'CLI', id, nome: cliente.nome }}
		onClose={() => (sugerirAberto = false)}
	/>
{/if}

<Modal
	open={modalInteracao}
	title="Registrar interação"
	subtitle={cliente?.nome ?? ''}
	onClose={() => (modalInteracao = false)}
	width="md"
>
	{#snippet children()}
		<div class="space-y-4">
			<RadioCards
				name="inter-tipo"
				options={INTER_TIPOS.map((t) => ({
					id: t,
					label: t,
					color: interacaoTipoMeta(t).color
				}))}
				value={interTipo}
				onChange={(v) => (interTipo = v as TipoInteracao)}
			/>
			<div>
				<label for="inter-data" class={labelCls}>
					Data e hora <span class="text-danger">*</span>
				</label>
				<input
					id="inter-data"
					type="datetime-local"
					bind:value={interData}
					disabled={interOcupado}
					aria-invalid={interErros['data'] ? 'true' : undefined}
					class="{inputCls} {interErros['data'] ? inputErroCls : ''}"
				/>
				{#if interErros['data']}
					<p role="alert" class="mt-1 text-xs text-danger">{interErros['data']}</p>
				{/if}
			</div>
			<div>
				<label for="inter-descricao" class={labelCls}>
					Descrição <span class="text-danger">*</span>
				</label>
				<textarea
					id="inter-descricao"
					bind:value={interDescricao}
					rows={3}
					placeholder="Resumo da conversa ou do contato"
					disabled={interOcupado}
					aria-invalid={interErros['descricao'] ? 'true' : undefined}
					class="{inputCls} resize-y {interErros['descricao'] ? inputErroCls : ''}"
				></textarea>
				{#if interErros['descricao']}
					<p role="alert" class="mt-1 text-xs text-danger">{interErros['descricao']}</p>
				{/if}
			</div>
			<p class="text-xs text-muted">
				Responsável: {usuario?.name ?? 'usuário atual'}
			</p>
		</div>
	{/snippet}
	{#snippet footer()}
		<button
			type="button"
			onclick={() => (modalInteracao = false)}
			disabled={interOcupado}
			class="rounded-md border border-border bg-surface px-4 py-2 text-sm font-medium text-ink transition hover:bg-elevated disabled:opacity-50"
		>
			Cancelar
		</button>
		<button
			type="button"
			onclick={() => void salvarInteracao()}
			disabled={interOcupado}
			class="rounded-md bg-brand px-4 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi disabled:opacity-50"
		>
			{interOcupado ? 'Salvando…' : 'Salvar interação'}
		</button>
	{/snippet}
</Modal>
