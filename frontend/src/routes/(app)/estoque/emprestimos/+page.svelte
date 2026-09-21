<script lang="ts">
	import { goto, invalidateAll } from '$app/navigation';
	import type { PageProps } from './$types';
	import type { Emprestimo, LoanTab, Tone } from '$lib/types/stock';
	import { fmtDate, fmtQty, daysFromToday } from '$lib/utils/stock-format';
import { loanComputed, loanComputedMeta, loanStatusMeta, LOAN_TABS } from '$lib/utils/stock-status';
	import PageHeader from '$lib/components/ui/PageHeader.svelte';
	import Icon from '$lib/components/ui/Icon.svelte';
	import Avatar from '$lib/components/ui/Avatar.svelte';
	import StatusBadge from '$lib/components/ui/StatusBadge.svelte';
	import EmptyState from '$lib/components/ui/EmptyState.svelte';
	import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte';

	let { data }: PageProps = $props();

	const canEdit = $derived(data.canEdit ?? false);
	const tab = $derived(data.tab);
	const error = $derived(data.error);
	const atrasados = $derived(data.atrasados);
	const pessoas = $derived(data.pessoas ?? {});

	function selectTab(id: LoanTab): void {
		void goto(`/estoque/emprestimos?tab=${id}`, { invalidateAll: false });
	}

	function pessoaNome(emprestimo: Emprestimo): string {
		return pessoas[String(emprestimo.idPessoa)] ?? `Pessoa #${emprestimo.idPessoa}`;
	}

	function prazoMeta(emprestimo: Emprestimo): { label: string; color: Tone; chip?: string } {
		const meta = loanComputedMeta(loanComputed(emprestimo));
		const dias = Math.max(0, -daysFromToday(emprestimo.dataDevolucaoPrevista));
		if (emprestimo.status === 'DEVOLVIDO') {
			return { label: fmtDate(emprestimo.dataDevolucaoReal), color: 'muted', chip: 'devolvido' };
		}
		if (dias > 0) return { label: fmtDate(emprestimo.dataDevolucaoPrevista), color: 'danger', chip: `+${dias}d` };
		if (meta.color === 'warn') return { label: fmtDate(emprestimo.dataDevolucaoPrevista), color: 'warn', chip: 'hoje' };
		return { label: fmtDate(emprestimo.dataDevolucaoPrevista), color: meta.color };
	}
</script>

<svelte:head>
	<title>Empréstimos — Estoque — FabLab</title>
</svelte:head>

<div class="space-y-4">
	<PageHeader title="Empréstimos" subtitle="Acompanhe itens emprestados e prazos de devolução">
		{#snippet children()}
			{#if canEdit}
				<a
					href="/estoque/emprestimos/novo"
					class="inline-flex items-center gap-1.5 rounded-lg bg-brand px-3 py-2 text-sm font-semibold text-white shadow-lg shadow-brand/20 transition-colors hover:bg-brandhi"
				>
					<Icon name="plus" class="h-4 w-4" /> Novo empréstimo
				</a>
			{/if}
		{/snippet}
	</PageHeader>

	<div
		data-testid="emp-aviso"
		class="rounded-xl border border-warn/30 bg-warn/5 px-4 py-3 text-sm text-ink"
	>
		<strong class="font-semibold">R-9</strong> O contrato atual expõe empréstimos somente por item e
		atrasados ({`/atrasados`}). As abas <strong class="font-medium">Ativos</strong> e
		<strong class="font-medium">Histórico</strong> ficam disponíveis no detalhe de cada item.
	</div>

	<div role="tablist" aria-label="Abas de empréstimos">
		<div class="inline-flex items-center gap-0.5 rounded-lg border border-border bg-surface p-0.5">
			{#each LOAN_TABS as tabDef (tabDef.key)}
				<button
					data-testid="loan-tab-{tabDef.key}"
					role="tab"
					aria-selected={tab === tabDef.key}
					onclick={() => selectTab(tabDef.key as LoanTab)}
					class="inline-flex items-center gap-1.5 rounded-md px-3 py-1.5 text-sm font-medium transition-colors {tab ===
					tabDef.key
						? 'bg-brand/15 text-brandhi'
						: 'text-muted hover:text-ink'}"
				>
					{tabDef.label}
				</button>
			{/each}
		</div>
	</div>

	{#if tab === 'atrasados'}
		{#if error}
			<div data-testid="emprestimo-error">
				<ErrorBanner
					message="Não foi possível carregar os empréstimos atrasados"
					hint={error}
					onRetry={() => void invalidateAll()}
				/>
			</div>
		{:else if atrasados.length === 0}
			<div data-testid="emprestimo-empty">
				<div class="rounded-xl border border-border bg-surface">
					<EmptyState
						icon="arrow-uturn-left"
						title="Nenhum empréstimo atrasado"
						description="Todos os empréstimos ativos estão dentro do prazo de devolução."
					/>
				</div>
			</div>
		{:else}
			<div class="overflow-hidden rounded-xl border border-border bg-surface">
				<div class="overflow-x-auto">
					<table class="w-full text-sm">
						<thead>
							<tr
								class="border-b border-border text-left text-[11px] uppercase tracking-wide text-muted"
							>
								<th class="px-4 py-2.5 font-medium">Item</th>
								<th class="px-4 py-2.5 font-medium">Tomador</th>
								<th class="px-4 py-2.5 text-right font-medium">Qtd</th>
								<th class="px-4 py-2.5 font-medium">Emprestado em</th>
								<th class="px-4 py-2.5 font-medium">Prazo</th>
								<th class="px-4 py-2.5 font-medium">Status</th>
								{#if canEdit}
									<th class="px-4 py-2.5 text-right font-medium">Ação</th>
								{/if}
							</tr>
						</thead>
						<tbody class="divide-y divide-border">
							{#each atrasados as emprestimo (emprestimo.id)}
								{@const prazo = prazoMeta(emprestimo)}
								{@const status = loanStatusMeta(emprestimo.status)}
								<tr class="transition-colors hover:bg-elevated/40">
									<td class="px-4 py-3">
										<p class="text-sm font-medium text-ink">{emprestimo.nomeItem}</p>
										<p class="font-mono text-xs text-muted">{emprestimo.idItem}</p>
									</td>
									<td class="px-4 py-3">
										<span class="flex items-center gap-2 text-sm text-ink">
											<Avatar name={pessoaNome(emprestimo)} size="xs" />
											<span class="truncate">{pessoaNome(emprestimo)}</span>
										</span>
									</td>
									<td class="px-4 py-3 text-right font-mono text-ink">
										{fmtQty(emprestimo.quantidade)}
									</td>
									<td class="px-4 py-3 font-mono text-muted">{fmtDate(emprestimo.dataEmprestimo)}</td>
									<td class="px-4 py-3">
										<span class="font-mono {prazo.color === 'danger' ? 'text-danger' : prazo.color === 'warn' ? 'text-warn' : 'text-ink'}">
											{prazo.label}
										</span>
										{#if prazo.chip}
											<span
												class="ml-1 rounded px-1.5 py-0.5 text-[10px] font-medium {prazo.color === 'danger'
													? 'bg-danger/10 text-danger border border-danger/30'
													: prazo.color === 'warn'
														? 'bg-warn/10 text-warn border border-warn/30'
														: 'bg-muted/10 text-muted border border-border'}"
											>
												{prazo.chip}
											</span>
										{/if}
									</td>
									<td class="px-4 py-3">
										<StatusBadge label={status.label} color={status.color} />
									</td>
									{#if canEdit}
										<td class="px-4 py-3 text-right">
											<a
												data-testid="loan-return"
												href={`/estoque/emprestimos/${emprestimo.id}/devolucao`}
												class="inline-flex items-center gap-1.5 rounded-lg border border-border bg-surface px-3 py-1.5 text-xs font-medium text-ink transition-colors hover:border-brand/50 hover:text-brandhi"
											>
												<Icon name="arrow-uturn-left" class="h-3.5 w-3.5" /> Registrar devolução
											</a>
										</td>
									{/if}
								</tr>
							{/each}
						</tbody>
					</table>
				</div>
			</div>
		{/if}
	{:else}
		<!-- Ativos / Histórico → R-9: sem endpoint global; orientar para o detalhe do item -->
		<div data-testid="emprestimo-r9" class="rounded-xl border border-border bg-surface">
			<EmptyState
				icon="arrow-uturn-left"
				title={tab === 'ativos' ? 'Empréstimos ativos' : 'Histórico de empréstimos'}
				description="Por contrato (R-9) não existe listagem global de empréstimos. Consulte a aba Empréstimos no detalhe de cada item para ver ativos e o histórico relacionados."
			>
				{#snippet children()}
					<a
						href="/estoque/itens"
						class="inline-flex items-center gap-1.5 rounded-lg bg-brand px-3 py-2 text-sm font-semibold text-white shadow-lg shadow-brand/20 transition-colors hover:bg-brandhi"
					>
						<Icon name="box" class="h-4 w-4" /> Consultar por item
					</a>
				{/snippet}
			</EmptyState>
		</div>
	{/if}
</div>