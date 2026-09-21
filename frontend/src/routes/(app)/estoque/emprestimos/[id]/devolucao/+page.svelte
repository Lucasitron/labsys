<script lang="ts">
	import { goto, invalidateAll } from '$app/navigation';
	import type { PageProps } from './$types';
	import { ApiError } from '$lib/api/client';
	import { devolverEmprestimo } from '$lib/api/stock/loans';
	import { toasts, toastError } from '$lib/stores/toast';
	import { fmtDate, fmtQty, toDateInputValue, daysFromToday } from '$lib/utils/stock-format';
	import { loanComputed } from '$lib/utils/stock-status';
	import PageHeader from '$lib/components/ui/PageHeader.svelte';
	import Icon from '$lib/components/ui/Icon.svelte';
	import Avatar from '$lib/components/ui/Avatar.svelte';
	import ConfirmDialog from '$lib/components/ui/ConfirmDialog.svelte';
	import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte';
	import Skeleton from '$lib/components/ui/Skeleton.svelte';

	let { data }: PageProps = $props();

	const emprestimo = $derived(data.emprestimo);
	const loadError = $derived(data.error);
	const notFound = $derived(data.notFound);

	const tomador = $derived(data.pessoaNome || data.pessoaFallback);
	const hoje = $derived(toDateInputValue());
	const atrasoDias = $derived(
		emprestimo && loanComputed(emprestimo) === 'atrasado'
			? Math.max(0, -daysFromToday(emprestimo.dataDevolucaoPrevista))
			: 0
	);

	let openConfirm = $state(false);
	let submitting = $state(false);

	async function confirmarDevolucao(): Promise<void> {
		if (!emprestimo) return;
		submitting = true;
		try {
			await devolverEmprestimo(emprestimo.id);
			toasts.success(
				`Devolução registrada — ${fmtQty(emprestimo.quantidade)} voltou ao estoque`
			);
			await goto('/estoque/emprestimos?tab=historico', { invalidateAll: true });
		} catch (err) {
			openConfirm = false;
			if (err instanceof ApiError && err.status === 409) {
				toastError(err, 'Não foi possível registrar a devolução');
			} else {
				toastError(err, 'Não foi possível registrar a devolução');
			}
		} finally {
			submitting = false;
		}
	}
</script>

<svelte:head>
	<title>Devolução — Estoque — FabLab</title>
</svelte:head>

{#if loadError && !emprestimo}
	{#if notFound}
		<div data-testid="emp-devolucao-empty" class="rounded-xl border border-border bg-surface">
			<div class="flex flex-col items-center justify-center px-6 py-16 text-center">
				<span
					class="mb-4 flex h-12 w-12 items-center justify-center rounded-xl border border-brand/30 bg-brand/10"
				>
					<Icon name="arrow-uturn-left" class="h-5 w-5 text-brand" />
				</span>
				<h3 class="mb-1 text-sm font-semibold text-ink">Empréstimo não encontrado</h3>
				<p class="mb-5 max-w-xs text-xs text-muted">
					O empréstimo pode ter sido removido ou o endereço está incorreto.
				</p>
				<a
					href="/estoque/emprestimos"
					class="inline-flex items-center gap-1.5 rounded-md border border-border bg-elevated px-3.5 py-2 text-xs font-medium text-ink transition-colors hover:bg-elevated/70"
				>
					<Icon name="chevron-left" class="h-3.5 w-3.5" /> Voltar para empréstimos
				</a>
			</div>
		</div>
	{:else}
		<div data-testid="emp-devolucao-error">
			<ErrorBanner
				message="Não foi possível carregar o empréstimo"
				hint={loadError}
				onRetry={() => void invalidateAll()}
			/>
		</div>
	{/if}
{:else if !emprestimo}
	<div data-testid="emp-devolucao-loading" class="space-y-4">
		<div class="rounded-xl border border-border bg-surface p-6">
			<Skeleton class="h-5 w-40" />
			<Skeleton class="mt-3 h-8 w-72" />
			<Skeleton class="mt-2 h-3 w-64 opacity-60" />
		</div>
		<div class="rounded-xl border border-border bg-surface p-6">
			<Skeleton class="h-3 w-36" />
			<Skeleton class="mt-4 h-10 w-full" />
		</div>
	</div>
{:else}
	<div class="space-y-4">
		<PageHeader title="Devolução" subtitle="Registre o retorno do item ao estoque">
			{#snippet children()}
				<a
					href={`/estoque/emprestimos/${emprestimo.id}`}
					class="rounded-lg border border-border bg-surface px-3 py-2 text-sm font-medium text-ink transition-colors hover:bg-border/40"
				>
					Cancelar
				</a>
			{/snippet}
		</PageHeader>

		{#if atrasoDias > 0}
			<div
				data-testid="emp-devolucao-atraso"
				role="alert"
				class="flex items-start gap-3 rounded-xl border border-danger/30 bg-danger/5 px-4 py-3 text-sm text-danger"
			>
				<Icon name="alert-triangle" class="mt-0.5 h-5 w-5 shrink-0" />
				<p>
					Este empréstimo está <strong class="font-semibold">atrasado</strong> há
					{atrasoDias} {atrasoDias === 1 ? 'dia' : 'dias'} (previsto para {fmtDate(
						emprestimo.dataDevolucaoPrevista
					)}).
				</p>
			</div>
		{/if}

		<!-- Resumo -->
		<section class="rounded-xl border border-border bg-surface p-6">
			<div class="flex flex-wrap items-center gap-5">
				<div
					class="flex h-16 w-16 shrink-0 items-center justify-center rounded-xl border border-border bg-elevated"
				>
					<Icon name="arrow-uturn-left" class="h-8 w-8 text-brand" />
				</div>
				<div class="min-w-0 flex-1">
					<p class="font-mono text-xs text-muted">EMPRESTIMO-{emprestimo.id}</p>
					<p class="mt-0.5 text-base font-semibold text-ink">{emprestimo.nomeItem}</p>
					<p class="mt-0.5 text-sm text-muted">
						<span class="font-mono text-brand">{emprestimo.idItem}</span>
						<span class="mx-1.5 text-muted/50">·</span>
						<span class="inline-flex items-center gap-1.5">
							<Avatar name={tomador} size="xs" />
							{tomador}
						</span>
					</p>
				</div>
				<div class="text-right">
					<p class="text-xs text-muted">Quantidade emprestada</p>
					<p class="mt-1 font-mono text-lg font-semibold text-ink">
						{fmtQty(emprestimo.quantidade)}
					</p>
				</div>
			</div>
		</section>

		<!-- Form de devolução -->
		<section data-testid="emp-devolucao-form" class="rounded-xl border border-border bg-surface p-6">
			<h2 class="mb-1 text-xs font-medium uppercase tracking-wide text-muted">
				Registro de devolução
			</h2>
			<p class="mb-4 text-sm text-muted">
				Ao confirmar, o item volta ao estoque e o empréstimo passa a constar como devolvido.
			</p>

			<div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
				<div>
					<label for="emp-devolucao-data" class="mb-1 block text-xs font-medium text-muted">
						Data da devolução
					</label>
					<input
						id="emp-devolucao-data"
						type="date"
						value={hoje}
						disabled
						class="w-full rounded-lg border border-border bg-elevated/50 px-3 py-2.5 text-sm text-muted opacity-70 focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/30"
					/>
					<p class="mt-1 text-xs text-muted">
						A data será registrada automaticamente como hoje ({fmtDate(hoje)}).
					</p>
				</div>

				<div>
					<span class="mb-1 block text-xs font-medium text-muted">Status após a devolução</span>
					<p class="rounded-lg border border-border bg-elevated/50 px-3 py-2.5 text-sm text-ink">
						Devolvido · item disponível no estoque
					</p>
				</div>
			</div>

			<div class="mt-6 flex justify-end gap-2">
				<button
					data-testid="abrir-confirmacao-devolucao"
					type="button"
					onclick={() => (openConfirm = true)}
					class="inline-flex items-center gap-1.5 rounded-lg bg-brand px-4 py-2.5 text-sm font-semibold text-white shadow-lg shadow-brand/20 transition-colors hover:bg-brandhi"
				>
					<Icon name="check" class="h-4 w-4" /> Confirmar devolução
				</button>
			</div>
		</section>

		<div data-testid="confirmar-devolucao">
			<ConfirmDialog
				open={openConfirm}
				title="Confirmar devolução"
				message={`Confirma a devolução de ${fmtQty(emprestimo.quantidade)} de ${emprestimo.nomeItem} por ${tomador}? O item retornará ao estoque.`}
				confirmLabel="Confirmar devolução"
				danger={false}
				loading={submitting}
				onCancel={() => {
					if (!submitting) openConfirm = false;
				}}
				onConfirm={() => void confirmarDevolucao()}
			/>
		</div>
	</div>
{/if}