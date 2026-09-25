<script lang="ts">
	import { invalidateAll } from '$app/navigation';
	import type { PageProps } from './$types';
	import type { BomItemResponse, BomResponse, StockItem } from '$lib/types/stock';
	import { registrarConsumo as registrarConsumoApi } from '$lib/api/stock/bom';
	import { toasts, toastError } from '$lib/stores/toast';
	import { fmtQty } from '$lib/utils/stock-format';
	import PageHeader from '$lib/components/ui/PageHeader.svelte';
	import TableSkeleton from '$lib/components/ui/TableSkeleton.svelte';
	import EmptyState from '$lib/components/ui/EmptyState.svelte';
	import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte';
	import StatusBadge from '$lib/components/ui/StatusBadge.svelte';
	import ConfirmDialog from '$lib/components/ui/ConfirmDialog.svelte';
	import Icon from '$lib/components/ui/Icon.svelte';

	let { data }: PageProps = $props();

	interface Disponibilidade {
		label: string;
		status: 'ok' | 'faltam' | 'indefinido';
	}

	const loaded = $derived(!!data);
	const canEdit = $derived(data?.canEdit ?? false);
	const bomId = $derived(data?.bomId ?? '');
	const bom = $derived<BomResponse | null>(data?.bom ?? null);
	const itens = $derived<StockItem[]>(data?.itens ?? []);
	const error = $derived(data?.error ?? null);

	function disponibilidade(item: BomItemResponse): Disponibilidade {
		const estoque = itens.find((i) => i.id === item.idItem);
		if (!estoque) {
			return { label: 'Item não cadastrado', status: 'indefinido' };
		}
		const saldo = estoque.quantidadeAtual - item.quantidadePrevista;
		if (saldo >= 0) {
			return { label: `OK · sobra ${fmtQty(saldo, estoque.unidadeMedida)}`, status: 'ok' };
		}
		return { label: `Faltam ${fmtQty(-saldo, estoque.unidadeMedida)}`, status: 'faltam' };
	}

	const linhas = $derived(
		(bom?.itens ?? []).map((item) => ({ item, disponibilidade: disponibilidade(item) }))
	);
	const faltando = $derived(linhas.filter((l) => l.disponibilidade.status === 'faltam').length);

	function badgeStatus(status: Disponibilidade['status']): 'success' | 'danger' | 'muted' {
		if (status === 'ok') return 'success';
		if (status === 'faltam') return 'danger';
		return 'muted';
	}

	function exportarCsv(): void {
		if (!bom) return;
		const sanitizeCell = (value: unknown): string => {
			const text = String(value);
			return /^[=+\-@\t\r]/.test(text) ? `'${text}` : text;
		};
		const header = ['Item', 'Quantidade prevista', 'Quantidade real', 'Unidade', 'Disponibilidade'];
		const rows = bom.itens.map((item) => {
			const estoque = itens.find((i) => i.id === item.idItem);
			return [
				item.nomeItem,
				String(item.quantidadePrevista),
				String(item.quantidadeReal),
				estoque?.unidadeMedida ?? '',
				disponibilidade(item).label
			];
		});
		const csv = [header, ...rows]
			.map((row) => row.map((cell) => `"${sanitizeCell(cell).replace(/"/g, '""')}"`).join(';'))
			.join('\n');
		const blob = new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8' });
		const url = URL.createObjectURL(blob);
		const link = document.createElement('a');
		link.href = url;
		const base = String(bom.nome ?? '').toLowerCase().replace(/[^a-z0-9-_]/g, '-');
		const fileName = base || 'bom';
		link.download = `${fileName}.csv`;
		link.click();
		URL.revokeObjectURL(url);
		toasts.info('Exportação CSV iniciada');
	}

	// Escrita canEdit: consumo da BOM registrado via POST /estoque/boms/{id}/consumo (contrato R-8).
	let confirmConsumo = $state(false);
	let consumindo = $state(false);

	async function registrarConsumo(): Promise<void> {
		if (!bom) return;
		consumindo = true;
		try {
			await registrarConsumoApi(bom.id, {
				itens: bom.itens.map((i) => ({ idItem: i.idItem, quantidade: i.quantidadePrevista }))
			});
			confirmConsumo = false;
			toasts.success('Consumo da lista de materiais registrado');
			await invalidateAll();
		} catch (err) {
			toastError(err, 'Não foi possível registrar o consumo');
		} finally {
			consumindo = false;
		}
	}
</script>

<svelte:head>
	<title>Lista de materiais (BOM) — Estoque — FabLab</title>
</svelte:head>

<div class="space-y-4">
	<PageHeader
		title="Lista de materiais (BOM)"
		subtitle={bom ? `${bom.nome} · versão ${bom.versao}` : 'Controle de materiais por produto'}
	>
		{#snippet children()}
			{#if bom}
				<button
					data-testid="bom-exportar"
					onclick={exportarCsv}
					class="inline-flex items-center gap-1.5 rounded-lg border border-border bg-surface px-3 py-2 text-sm font-medium text-ink transition-colors hover:border-brand/50 hover:text-brandhi"
				>
					<Icon name="arrow-down-tray" class="h-4 w-4" /> Exportar
				</button>
			{/if}
			{#if canEdit && bom?.editavel && bom.itens.length > 0}
				<button
					data-testid="bom-registrar-consumo"
					onclick={() => (confirmConsumo = true)}
					class="inline-flex items-center gap-1.5 rounded-lg bg-brand px-3 py-2 text-sm font-semibold text-white shadow-lg shadow-brand/20 transition-colors hover:bg-brandhi"
				>
					<Icon name="check" class="h-4 w-4" /> Registrar consumo
				</button>
			{/if}
		{/snippet}
	</PageHeader>

	<div
		data-testid="bom-aviso-r8"
		class="rounded-xl border border-warn/30 bg-warn/5 px-4 py-3 text-sm text-ink"
	>
		<strong class="font-semibold">R-8</strong> O contrato atual expõe somente a leitura de uma BOM
		por id ({`buscarBom(id)`}) — não há listagem por projeto no backend. A tela é carregada pela
		URL (<code class="font-mono">?id=…</code>) e a disponibilidade é calculada localmente a partir
		do estoque atual.
	</div>

	{#if !loaded}
		<div class="overflow-hidden rounded-xl border border-border bg-surface">
			<div class="border-b border-border bg-elevated/50 px-4 py-2.5">
				<TableSkeleton rows={1} columns={4} class="!space-y-0" />
			</div>
			<div class="p-4" data-testid="bom-carregando">
				<TableSkeleton rows={6} columns={4} />
			</div>
		</div>
	{:else if !bomId}
		<div class="rounded-xl border border-border bg-surface" data-testid="bom-sem-id">
			<EmptyState
				icon="document"
				title="Selecione uma lista de materiais"
				description="Sem listagem por projeto no contrato atual (R-8), informe o id da BOM na URL, por exemplo /estoque/bom?id=1, para carregar a tabela."
			/>
		</div>
	{:else if error}
		<div data-testid="bom-erro">
			<ErrorBanner
				message="Não foi possível carregar a lista de materiais"
				hint={error}
				onRetry={() => void invalidateAll()}
			/>
		</div>
	{:else if !bom}
		<div class="rounded-xl border border-border bg-surface" data-testid="bom-nao-encontrada">
			<EmptyState
				icon="document"
				title="Lista de materiais não encontrada"
				description="Não foi possível carregar a BOM solicitada. Confira o id informado na URL."
			/>
		</div>
	{:else if linhas.length === 0}
		<div class="rounded-xl border border-border bg-surface" data-testid="bom-vazio">
			<EmptyState
				icon="document"
				title="Lista de materiais sem itens"
				description="Esta BOM não possui itens cadastrados."
			/>
		</div>
	{:else}
		{#if faltando > 0}
			<div
				data-testid="bom-faltando"
				class="rounded-xl border border-danger/30 bg-danger/5 px-4 py-3 text-sm text-danger"
			>
				<Icon name="alert-triangle" class="mr-1 inline h-4 w-4" />
				Estoque insuficiente — {faltando} {faltando === 1 ? 'item precisa' : 'itens precisam'} de
				reposição para a quantidade prevista.
			</div>
		{/if}

		<div class="overflow-hidden rounded-xl border border-border bg-surface" data-testid="bom-tabela">
			<div class="overflow-x-auto">
				<table class="w-full text-sm">
					<thead>
						<tr class="border-b border-border text-left text-[11px] uppercase tracking-wide text-muted">
							<th class="px-4 py-2.5 font-medium">Item</th>
							<th class="px-4 py-2.5 text-right font-medium">Qtd prevista</th>
							<th class="px-4 py-2.5 text-right font-medium">Qtd real</th>
							<th class="px-4 py-2.5 font-medium">Disponibilidade</th>
						</tr>
					</thead>
					<tbody class="divide-y divide-border">
						{#each linhas as linha (linha.item.idItem)}
							{@const disponivel = linha.disponibilidade}
							<tr class="transition-colors hover:bg-elevated/40">
								<td class="px-4 py-3">
									<p class="text-sm font-medium text-ink">{linha.item.nomeItem}</p>
									<p class="font-mono text-xs text-muted">{linha.item.idItem}</p>
								</td>
								<td class="px-4 py-3 text-right font-mono text-ink">
									{fmtQty(linha.item.quantidadePrevista)}
								</td>
								<td class="px-4 py-3 text-right font-mono text-ink">
									{fmtQty(linha.item.quantidadeReal)}
								</td>
								<td class="px-4 py-3" data-testid="bom-disponibilidade">
									<StatusBadge label={disponivel.label} color={badgeStatus(disponivel.status)} />
								</td>
							</tr>
						{/each}
					</tbody>
				</table>
			</div>
			<div class="border-t border-border px-4 py-3" data-testid="bom-contagem">
				<span class="text-xs text-muted">
					{linhas.length} {linhas.length === 1 ? 'item' : 'itens'} na lista
				</span>
			</div>
		</div>
	{/if}

	<ConfirmDialog
		open={confirmConsumo}
		title="Registrar consumo"
		message="O consumo será registrado para cada item da lista na quantidade prevista. A disponibilidade da tela será atualizada."
		confirmLabel="Registrar consumo"
		danger={false}
		loading={consumindo}
		onCancel={() => (confirmConsumo = false)}
		onConfirm={() => void registrarConsumo()}
		icon="check"
	/>
</div>