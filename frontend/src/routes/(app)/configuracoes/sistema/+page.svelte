<script lang="ts">
	import { invalidateAll } from '$app/navigation';
	import type { PageProps } from './$types';
	import { atualizarSistema, criarToken, revogarToken } from '$lib/api/configuracoes/sistema';
	import type { ParametroSistema, TokenIntegracao } from '$lib/types/configuracoes';
	import { toasts, toastError } from '$lib/stores/toast';
	import Icon from '$lib/components/ui/Icon.svelte';
	import Skeleton from '$lib/components/ui/Skeleton.svelte';
	import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte';
	import StatusBadge from '$lib/components/ui/StatusBadge.svelte';
	import Avatar from '$lib/components/ui/Avatar.svelte';
	import Select from '$lib/components/ui/Select.svelte';
	import Modal from '$lib/components/ui/Modal.svelte';
	import ModalConfirm from '$lib/components/configuracoes/ModalConfirm.svelte';

	let { data }: PageProps = $props();

	const sistema = $derived(data.sistema);
	const tokens = $derived(data.tokens);
	const error = $derived(data.error);
	const loading = $derived(sistema === null && error === null);
	const hasError = $derived(error !== null && sistema === null);

	function formatarData(iso: string): string {
		return new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: 'short', year: 'numeric' }).format(
			new Date(iso)
		);
	}

	function ehUrl(valor: string | null): valor is string {
		if (!valor) return false;
		try {
			const url = new URL(valor);
			return url.protocol === 'http:' || url.protocol === 'https:';
		} catch {
			return false;
		}
	}

	const opcoesCadencia = [
		{ id: 'Diário', label: 'Diário' },
		{ id: 'Semanal', label: 'Semanal' },
		{ id: 'Quinzenal', label: 'Quinzenal' },
		{ id: 'Mensal', label: 'Mensal' }
	];

	let identidadeNome = $state('');
	let logo = $state<string | null>(null);
	let cadChecklist = $state('');
	let cadAuditoria = $state('');
	let sincronizado = $state(false);
	let busy = $state(false);

	$effect(() => {
		if (sistema && !sincronizado) {
			identidadeNome = sistema.identidade.nome ?? '';
			logo = sistema.identidade.logo ?? null;
			cadChecklist = sistema.cadenciaChecklist5S ?? '';
			cadAuditoria = sistema.cadenciaAuditoria5S ?? '';
			sincronizado = true;
		} else if (!sistema && sincronizado) {
			sincronizado = false;
		}
	});

	const dirty = $derived(
		sistema
			? identidadeNome.trim() !== sistema.identidade.nome ||
					(logo ?? null) !== (sistema.identidade.logo ?? null) ||
					cadChecklist !== sistema.cadenciaChecklist5S ||
					cadAuditoria !== (sistema.cadenciaAuditoria5S ?? '')
			: false
	);

	async function salvar(): Promise<void> {
		if (!sistema || busy || !dirty) return;
		busy = true;
		try {
			const payload: ParametroSistema = {
				identidade: { nome: identidadeNome.trim(), logo: logo },
				cadenciaChecklist5S: cadChecklist,
				cadenciaAuditoria5S: cadAuditoria,
				tokens: tokens ?? []
			};
			await atualizarSistema(payload);
			toasts.success('Configurações do sistema salvas com sucesso.');
			await invalidateAll();
		} catch (err) {
			toastError(err, 'Não foi possível salvar as configurações do sistema.');
		} finally {
			busy = false;
		}
	}

	let mostrarNovo = $state(false);
	let novoNome = $state('');
	let criandoToken = $state(false);

	async function criarNovoToken(): Promise<void> {
		if (criandoToken) return;
		const nome = novoNome.trim();
		if (!nome) {
			toasts.warn('Dê um nome ao token antes de gerá-lo.');
			return;
		}
		criandoToken = true;
		try {
			await criarToken({ nome });
			toasts.success(`Token "${nome}" criado com sucesso. Guarde-o em local seguro.`);
			novoNome = '';
			mostrarNovo = false;
			await invalidateAll();
		} catch (err) {
			toastError(err, 'Não foi possível criar o token.');
		} finally {
			criandoToken = false;
		}
	}

	let tokenAlvo = $state<TokenIntegracao | null>(null);

	async function confirmarRevogacao(): Promise<void> {
		const alvo = tokenAlvo;
		if (!alvo || busy) return;
		busy = true;
		try {
			await revogarToken(alvo.id);
			toasts.success(`Token "${alvo.nome}" revogado.`);
			tokenAlvo = null;
			await invalidateAll();
		} catch (err) {
			toastError(err, 'Não foi possível revogar o token.');
		} finally {
			busy = false;
		}
	}
</script>

<svelte:head>
	<title>Sistema — Configurações — FabLab</title>
</svelte:head>

<div class="space-y-4">
	<div class="flex flex-wrap items-start justify-between gap-4">
		<div class="flex items-center gap-3">
			<div>
				<h1 class="text-xl font-semibold tracking-tight text-ink">Sistema</h1>
				<p class="mt-0.5 text-sm text-muted">
					Identidade do laboratório, cadências 5S e tokens de integração.
				</p>
			</div>
			<StatusBadge label="Restrito ao Admin" color="warn" />
		</div>
		<button
			type="button"
			data-testid="cfg-system-save"
			onclick={salvar}
			disabled={busy || !dirty}
			class="inline-flex items-center gap-2 rounded-md border px-4 py-2 text-sm font-medium transition disabled:cursor-not-allowed disabled:opacity-50 {dirty
				? 'border-brand bg-brand text-white shadow-lg shadow-brand/20 hover:bg-brandhi'
				: 'border-border bg-surface text-muted hover:border-brand/50 hover:text-brandhi'}"
		>
			Salvar alterações
		</button>
	</div>

	{#if hasError}
		<ErrorBanner
			message="Não foi possível carregar os dados do sistema"
			hint={error ?? ''}
			onRetry={() => void invalidateAll()}
		/>
	{:else if loading}
		<div class="grid grid-cols-1 gap-4 lg:grid-cols-2">
			<Skeleton class="h-44 w-full" />
			<Skeleton class="h-44 w-full" />
			<Skeleton class="h-52 w-full lg:col-span-2" />
		</div>
	{:else}
		<div
			class="grid grid-cols-1 gap-4 lg:grid-cols-2"
			data-testid="cfg-system-section"
		>
			<div class="rounded-xl border {dirty ? 'border-brand/50 ring-1 ring-brand/40' : 'border-border'} bg-surface p-5">
				<div class="mb-4 flex items-center justify-between">
					<h2 class="text-sm font-semibold text-ink">Identidade</h2>
					{#if dirty}
						<StatusBadge label="Alterado (não salvo)" color="warn" />
					{/if}
				</div>
				<div class="flex items-center gap-3">
					{#if ehUrl(logo)}
						<img
							src={logo}
							alt="Logo do laboratório"
							class="h-14 w-14 rounded-full border border-border object-cover"
							data-testid="cfg-system-logo"
						/>
					{:else if logo}
						<span
							class="flex h-14 w-14 items-center justify-center rounded-full border border-warn/30 bg-warn/10 text-warn"
						>
							<Icon name="photo" class="h-5 w-5" />
						</span>
					{:else}
						<Avatar name={identidadeNome || 'Laboratório'} size="md" />
					{/if}
					<div class="flex-1 space-y-3">
						<label class="block">
							<span class="mb-1 block text-xs font-medium text-muted">URL do logo (opcional)</span>
							<input
								type="url"
								class="w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm text-ink focus:border-brand focus:outline-none focus:ring-1 focus:ring-brand/40"
								placeholder="https://…/logo.png"
								aria-label="URL do logo do laboratório"
								data-testid="cfg-system-logo-url"
								bind:value={logo}
							/>
						</label>
						<label class="block">
							<span class="mb-1 block text-xs font-medium text-muted">
								Nome do laboratório <span class="text-danger">*</span>
							</span>
							<input
								type="text"
								class="w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm text-ink focus:border-brand focus:outline-none focus:ring-1 focus:ring-brand/40"
								placeholder="Ex.: FabLab UNIFAP"
								aria-label="Nome do laboratório"
								data-testid="cfg-system-nome"
								bind:value={identidadeNome}
							/>
						</label>
					</div>
				</div>
			</div>

			<div class="rounded-xl border {dirty ? 'border-brand/50 ring-1 ring-brand/40' : 'border-border'} bg-surface p-5">
				<div class="mb-3 flex items-center justify-between">
					<h2 class="text-sm font-semibold text-ink">Cadência 5S</h2>
					{#if dirty}
						<StatusBadge label="Alterado (não salvo)" color="warn" />
					{/if}
				</div>
				<div class="flex items-start gap-2 rounded-lg border border-warn/30 bg-warn/10 px-3 py-2 text-xs text-muted">
					<Icon name="warning" class="mt-0.5 h-4 w-4 shrink-0 text-warn" />
					<p>
						A cadência 5S é compartilhada com a <strong class="text-ink">Produção</strong> —
						alterar aqui afeta as rotinas de checklist e auditoria 5S.
					</p>
				</div>
				<div class="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2">
					<Select
						id="cfg-system-cad-checklist"
						label="Cadência do checklist 5S"
						value={cadChecklist}
						options={opcoesCadencia}
						placeholder="Selecione…"
						onChange={(v) => (cadChecklist = v)}
					/>
					<Select
						id="cfg-system-cad-auditoria"
						label="Cadência da auditoria 5S"
						value={cadAuditoria}
						options={opcoesCadencia}
						placeholder="Selecione…"
						onChange={(v) => (cadAuditoria = v)}
					/>
				</div>
			</div>

			<div class="rounded-xl border border-border bg-surface lg:col-span-2">
				<div class="flex flex-wrap items-center justify-between gap-3 border-b border-border px-5 py-4">
					<div>
						<h2 class="text-sm font-semibold text-ink">Tokens de integração</h2>
						<p class="mt-0.5 text-xs text-muted">
							Chaves de API usadas por serviços externos (robôs, IoT, integrações).
						</p>
					</div>
					<button
						type="button"
						data-testid="cfg-token-new"
						onclick={() => (mostrarNovo = true)}
						class="inline-flex items-center gap-2 rounded-md border border-brand bg-brand px-3 py-2 text-sm font-medium text-white transition-colors hover:bg-brandhi"
					>
						<Icon name="plus" class="h-4 w-4" />
						Gerar novo token
					</button>
				</div>

				{#if (tokens ?? []).length === 0}
					<div class="flex flex-col items-center gap-2 px-5 py-10 text-center" data-testid="cfg-tokens-empty">
						<Icon name="lock" class="h-8 w-8 text-muted/50" />
						<p class="text-sm text-muted">Nenhum token de integração cadastrado.</p>
						<button
							type="button"
							data-testid="cfg-token-new-empty"
							onclick={() => (mostrarNovo = true)}
							class="mt-1 text-sm font-medium text-brandhi underline-offset-4 hover:underline"
						>
							Gerar o primeiro token
						</button>
					</div>
				{:else}
					<div class="divide-y divide-border" data-testid="cfg-token-list">
						{#each tokens ?? [] as token (token.id)}
							<div class="flex flex-wrap items-center gap-3 px-5 py-3">
								<Icon name="lock" class="h-4 w-4 text-muted" />
								<div class="min-w-40 flex-1">
									<p class="text-sm font-medium text-ink">{token.nome}</p>
									<p class="text-xs text-muted">
										<span class="font-mono">{token.prefixo}</span>
										<span class="mx-1">·</span>
										criado em {formatarData(token.criadoEm)}
									</p>
								</div>
								<p class="text-xs text-muted">
									{#if token.revogado}
										—
									{:else if token.ultimoUso}
										Último uso há {formatarData(token.ultimoUso)}
									{:else}
										Nunca usado
									{/if}
								</p>
								{#if token.revogado}
									<StatusBadge label="Revogado" color="muted" />
								{:else}
									<StatusBadge label="Ativo" color="success" />
									<button
										type="button"
										data-testid="cfg-token-revoke"
										onclick={() => (tokenAlvo = token)}
										class="inline-flex items-center gap-1.5 rounded-md px-2 py-1 text-xs font-medium text-danger transition-colors hover:bg-danger/10"
									>
										<Icon name="trash" class="h-3.5 w-3.5" />
										Revogar
									</button>
								{/if}
							</div>
						{/each}
					</div>
				{/if}

				<div class="border-t border-border bg-elevated/50 px-5 py-3 text-xs text-muted">
					Tokens revogados não podem ser reutilizados. Contrato de endpoints pendente 🟡.
				</div>
			</div>
		</div>
	{/if}
</div>

<Modal open={mostrarNovo} title="Gerar novo token" subtitle="O valor completo aparece apenas uma vez." width="sm" onClose={() => (mostrarNovo = false)}>
	{#snippet children()}
		<label class="block">
			<span class="mb-1 block text-xs font-medium text-muted">Nome do token <span class="text-danger">*</span></span>
			<input
				type="text"
				class="w-full rounded-lg border border-border bg-surface px-3 py-2 text-sm text-ink focus:border-brand focus:outline-none focus:ring-1 focus:ring-brand/40"
				placeholder="Ex.: Robô do checklist 5S"
				aria-label="Nome do token"
				data-testid="cfg-token-nome"
				bind:value={novoNome}
			/>
		</label>
	{/snippet}
	{#snippet footer()}
		<button
			type="button"
			onclick={() => (mostrarNovo = false)}
			disabled={criandoToken}
			class="rounded-lg border border-border bg-surface px-4 py-2 text-sm font-medium text-ink transition-colors hover:bg-border/40 disabled:cursor-not-allowed disabled:opacity-50"
		>
			Cancelar
		</button>
		<button
			type="button"
			data-testid="cfg-token-create"
			onclick={criarNovoToken}
			disabled={criandoToken}
			class="rounded-lg bg-brand px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-brandhi disabled:cursor-not-allowed disabled:opacity-50"
		>
			{criandoToken ? 'Gerando…' : 'Gerar token'}
		</button>
	{/snippet}
</Modal>

<ModalConfirm
	open={tokenAlvo !== null}
	titulo="Revogar token"
	mensagem={tokenAlvo ? `O token "${tokenAlvo.nome}" será revogado imediatamente e deixará de funcionar.` : ''}
	confirmLabel="Revogar"
	tone="danger"
	onConfirm={() => void confirmarRevogacao()}
	onClose={() => (tokenAlvo = null)}
/>