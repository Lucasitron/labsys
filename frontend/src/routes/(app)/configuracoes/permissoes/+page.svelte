<script lang="ts">
	import { goto, invalidateAll } from '$app/navigation';
	import type { PageProps } from './$types';
	import type { Nivel, PermissaoMatriz, PermissaoNivel } from '$lib/types/configuracoes';
	import type { Module } from '$lib/types/auth';
	import { atualizarPermissao } from '$lib/api/configuracoes/permissoes';
	import { toasts, toastError } from '$lib/stores/toast';
	import Icon from '$lib/components/ui/Icon.svelte';
	import Skeleton from '$lib/components/ui/Skeleton.svelte';
	import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte';
	import StatusBadge from '$lib/components/ui/StatusBadge.svelte';
	import ModalConfirm from '$lib/components/configuracoes/ModalConfirm.svelte';
	import PermCell from '$lib/components/configuracoes/PermCell.svelte';

	let { data }: PageProps = $props();

	const matriz = $derived(data.matriz);
	const error = $derived(data.error);
	const loading = $derived(matriz === null && error === null);
	const hasError = $derived(error !== null && matriz === null);

	const MODULOS: { id: Module; label: string }[] = [
		{ id: 'dashboard', label: 'Dashboard' },
		{ id: 'rh', label: 'Pessoas & RH' },
		{ id: 'estoque', label: 'Estoque' },
		{ id: 'financeiro', label: 'Financeiro' },
		{ id: 'vendas', label: 'Vendas & CRM' },
		{ id: 'producao', label: 'Produção' },
		{ id: 'notificacoes', label: 'Notificações' },
		{ id: 'configuracoes', label: 'Configurações' }
	];

	const NIVEL_ORDEM: Nivel[] = [0, 1, 2, 3, 4];
	const NIVEL_LABELS: Record<Nivel, string> = {
		0: 'Admin',
		1: 'Bolsista',
		2: 'Voluntário',
		3: 'Estagiário',
		4: 'Recrutando'
	};

	// alinhado a .opencode/specs/system/rbac-matrix.md (defaults conservadores)
	const DEFAULT_MATRIZ: PermissaoMatriz = {
		dashboard: { 0: 'Editar', 1: 'Ver', 2: 'Ver', 3: 'Ver', 4: 'Nenhum' },
		rh: { 0: 'Editar', 1: 'Ver', 2: 'Ver', 3: 'Ver', 4: 'Ver' },
		estoque: { 0: 'Editar', 1: 'Ver', 2: 'Ver', 3: 'Ver', 4: 'Nenhum' },
		financeiro: { 0: 'Editar', 1: 'Nenhum', 2: 'Nenhum', 3: 'Nenhum', 4: 'Nenhum' },
		vendas: { 0: 'Editar', 1: 'Ver', 2: 'Ver', 3: 'Ver', 4: 'Nenhum' },
		producao: { 0: 'Editar', 1: 'Ver', 2: 'Ver', 3: 'Ver', 4: 'Nenhum' },
		notificacoes: { 0: 'Editar', 1: 'Ver', 2: 'Ver', 3: 'Ver', 4: 'Ver' },
		configuracoes: { 0: 'Editar', 1: 'Nenhum', 2: 'Nenhum', 3: 'Nenhum', 4: 'Nenhum' }
	};

	function clonar(matrizOriginal: PermissaoMatriz): PermissaoMatriz {
		const copia = {} as Record<Module, Record<Nivel, PermissaoNivel>>;
		for (const m of MODULOS) {
			copia[m.id] = { ...(matrizOriginal[m.id] ?? {}) } as Record<Nivel, PermissaoNivel>;
		}
		return copia as PermissaoMatriz;
	}

	let trabalho = $state<PermissaoMatriz | null>(null);
	let busy = $state(false);

	$effect(() => {
		if (matriz) {
			trabalho = clonar(matriz);
		} else {
			trabalho = null;
		}
	});

	const pendentes = $derived.by((): Record<string, PermissaoNivel> => {
		const dif = {} as Record<string, PermissaoNivel>;
		if (!matriz || !trabalho) return dif;
		for (const m of MODULOS) {
			for (const n of NIVEL_ORDEM) {
				if (trabalho[m.id][n] !== matriz[m.id][n]) {
					dif[`${m.id}:${n}`] = trabalho[m.id][n];
				}
			}
		}
		return dif;
	});

	const totalPendentes = $derived(Object.keys(pendentes).length);
	const sujo = $derived(totalPendentes > 0);

	function mudarCelula(modulo: Module, nivel: Nivel, valor: PermissaoNivel): void {
		if (!trabalho) return;
		trabalho[modulo] = { ...trabalho[modulo], [nivel]: valor };
	}

	let confirmar = $state(false);

	function pedirRestaurar(): void {
		confirmar = true;
	}

	function restaurarPadrao(): void {
		if (!matriz) return;
		trabalho = clonar(DEFAULT_MATRIZ);
		confirmar = false;
		toasts.info('Matriz restaurada ao padrão. Salve para aplicar.');
	}

	async function salvar(): Promise<void> {
		if (totalPendentes === 0 || busy) return;
		busy = true;
		try {
			for (const [chave, valor] of Object.entries(pendentes)) {
				const [modulo, nivel] = chave.split(':');
				await atualizarPermissao(modulo as Module, Number(nivel) as Nivel, valor);
			}
			toasts.success('Permissões atualizadas com sucesso.');
			await invalidateAll();
		} catch (err) {
			toastError(err, 'Não foi possível salvar as permissões.');
		} finally {
			busy = false;
		}
	}
</script>

<svelte:head>
	<title>Permissões — Configurações — FabLab</title>
</svelte:head>

<div class="space-y-4">
	<div class="flex flex-wrap items-start justify-between gap-4">
		<div class="flex items-center gap-3">
			<div>
				<h1 class="text-xl font-semibold tracking-tight text-ink">Permissões</h1>
				<p class="mt-0.5 text-sm text-muted">
					Matriz RBAC — o que cada nível de integrante pode fazer por módulo.
				</p>
			</div>
			<StatusBadge label="Restrito ao Admin" color="warn" />
		</div>
		<div class="flex items-center gap-3">
			<button
				type="button"
				data-testid="cfg-perm-reset"
				onclick={pedirRestaurar}
				class="px-3 py-2 text-sm text-muted transition-colors hover:text-ink"
			>
				Restaurar padrão
			</button>
			<button
				type="button"
				data-testid="cfg-perm-save"
				onclick={salvar}
				disabled={busy || !sujo}
				class="inline-flex items-center gap-2 rounded-md border px-4 py-2 text-sm font-medium transition disabled:cursor-not-allowed disabled:opacity-50 {sujo
					? 'border-brand bg-brand text-white shadow-lg shadow-brand/20 hover:bg-brandhi'
					: 'border-border bg-surface text-muted hover:border-brand/50 hover:text-brandhi'}"
			>
				Salvar alterações
				{#if sujo}
					<span class="inline-flex h-5 min-w-5 items-center justify-center rounded-full bg-white/20 px-1 text-[10px] font-bold text-white">
						{totalPendentes}
					</span>
				{/if}
			</button>
		</div>
	</div>

	{#if hasError}
		<ErrorBanner
			message="Não foi possível carregar a matriz de permissões"
			hint={error ?? ''}
			onRetry={() => void goto('/configuracoes/permissoes', { invalidateAll: true })}
		/>
	{:else if loading || !trabalho}
		<div class="rounded-xl border border-border bg-surface p-5">
			<Skeleton class="h-6 w-48" />
			<div class="mt-4 overflow-hidden rounded-lg border border-border">
				{#each [0, 1, 2, 3, 4] as i (i)}
					<div class="flex items-center gap-3 border-b border-border px-4 py-3">
						<Skeleton class="h-3 w-32" />
						<div class="flex flex-1 gap-3">
							{#each [0, 1, 2, 3, 4] as j (j)}
								<Skeleton class="h-8 flex-1" />
							{/each}
						</div>
					</div>
				{/each}
			</div>
		</div>
	{:else}
		{#if sujo}
			<div
				class="flex items-center gap-2 rounded-lg border border-warn/30 bg-warn/10 px-3 py-2 text-xs text-muted"
				data-testid="cfg-perm-dirty"
			>
				<Icon name="warning" class="h-4 w-4 shrink-0 text-warn" />
				<span>
					<b class="text-ink">{totalPendentes}</b> alteração(ões) não salva(s). Clique em
					"Salvar alterações" para persistir.
				</span>
			</div>
		{/if}

		<div
			data-testid="cfg-perm-matrix"
			class="overflow-hidden rounded-xl border {sujo ? 'border-brand/50 ring-1 ring-brand/40' : 'border-border'} bg-surface"
		>
			<div class="overflow-x-auto">
				<table class="w-full min-w-[820px] text-sm">
					<thead>
						<tr class="border-b border-border text-left text-xs text-muted">
							<th class="px-4 py-3 font-medium">Módulo</th>
							{#each NIVEL_ORDEM as nivel (nivel)}
								<th class="px-4 py-3 text-center font-medium">{NIVEL_LABELS[nivel]}</th>
							{/each}
						</tr>
					</thead>
					<tbody>
						{#each MODULOS as modulo (modulo.id)}
							<tr class="border-b border-border transition-colors hover:bg-elevated/40">
								<td class="px-4 py-3 font-medium text-ink {modulo.id === 'configuracoes' ? 'bg-brand/5' : ''}">
									{modulo.label}
								</td>
								{#each NIVEL_ORDEM as nivel (nivel)}
									<td class="px-4 py-3 text-center {modulo.id === 'configuracoes' ? 'bg-brand/5' : ''}">
										<PermCell
											modulo={modulo.id}
											{nivel}
											valor={trabalho[modulo.id][nivel]}
											admin={data.isAdmin}
											onChange={(valor) => mudarCelula(modulo.id, nivel, valor)}
										/>
									</td>
								{/each}
							</tr>
						{/each}
					</tbody>
				</table>
			</div>
			<div class="flex flex-wrap items-start gap-6 border-t border-border px-4 py-3">
				<p class="text-xs text-muted">
					Legenda: <span class="text-ink">✎ Editar</span> · <span class="text-ink">👁 Ver</span> ·
					<span class="text-muted">— sem acesso</span>
				</p>
				<p class="ml-auto text-xs text-muted">
					Alterações são enviadas célula a célula via <span class="font-mono">PUT /api/permissoes/:modulo/:nivel</span>.
				</p>
			</div>
		</div>

		<div class="mt-5 grid grid-cols-1 gap-4 md:grid-cols-3">
			<div class="rounded-xl border border-border bg-surface p-4">
				<h3 class="mb-2 text-sm font-medium text-ink">Item sem permissão</h3>
				<p class="text-xs text-muted">
					É <strong class="text-ink">removido da sidebar</strong> (nunca desabilitado). Níveis sem
					edição não veem ações de escrita.
				</p>
			</div>
			<div class="rounded-xl border border-border bg-surface p-4">
				<h3 class="mb-2 text-sm font-medium text-ink">Guarda de rota</h3>
				<p class="text-xs text-muted">
					Rotas Admin-only redirecionam para
					<span class="font-mono">/configuracoes/perfil</span> quando o usuário não é Admin.
				</p>
			</div>
			<div class="rounded-xl border border-border bg-surface p-4">
				<h3 class="mb-2 text-sm font-medium text-ink">Assumido (sem spec)</h3>
				<p class="text-xs text-muted">
					Endpoints de permissões não constam nos serviços atuais — contrato pendente (🟡).
				</p>
			</div>
		</div>
	{/if}
</div>

<ModalConfirm
	open={confirmar}
	titulo="Restaurar padrão"
	mensagem="A matriz de permissões voltará aos valores padrão. As alterações atuais serão substituídas — salve para aplicar."
	confirmLabel="Restaurar padrão"
	tone="warn"
	onConfirm={restaurarPadrao}
	onClose={() => (confirmar = false)}
/>