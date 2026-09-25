<script lang="ts">
	import { goto } from '$app/navigation';
	import { get } from 'svelte/store';
	import type { PageProps } from './$types';
	import type { Perfil } from '$lib/types/configuracoes';
	import type { Role } from '$lib/types/auth';
	import type { Tone } from '$lib/types/stock';
	import { auth } from '$lib/stores/auth';
	import { updatePerfil } from '$lib/api/configuracoes/perfil';
	import { alterarSenha } from '$lib/api/configuracoes/senha';
	import { toasts, toastError } from '$lib/stores/toast';
	import PageHeader from '$lib/components/ui/PageHeader.svelte';
	import Skeleton from '$lib/components/ui/Skeleton.svelte';
	import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte';
	import StatusBadge from '$lib/components/ui/StatusBadge.svelte';
	import Avatar from '$lib/components/ui/Avatar.svelte';
	import Select from '$lib/components/ui/Select.svelte';

	let { data }: PageProps = $props();

	const perfil = $derived(data.perfil);
	const error = $derived(data.error);
	const loading = $derived(perfil === null && error === null);
	const hasError = $derived(error !== null && perfil === null);

	const user = $derived(get(auth).user);

	const NIVEL_LABELS: Record<Role, string> = {
		0: 'Admin',
		1: 'Bolsista',
		2: 'Voluntário',
		3: 'Estagiário',
		4: 'Recrutando'
	};

	const NIVEL_TONES: Record<Role, Tone> = {
		0: 'brand',
		1: 'muted',
		2: 'muted',
		3: 'warn',
		4: 'danger'
	};

	const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

	let nome = $state('');
	let email = $state('');
	let telefone = $state('');
	let funcao = $state('');

	let perfilTentou = $state(false);

	$effect(() => {
		if (perfil) {
			nome = perfil.nome ?? '';
			email = perfil.email ?? '';
			telefone = perfil.telefone ?? '';
			funcao = perfil.funcao ?? '';
			perfilTentou = false;
		}
	});

	const perfilSujo = $derived(
		perfil !== null &&
			(nome !== (perfil.nome ?? '') ||
				email !== (perfil.email ?? '') ||
				telefone !== (perfil.telefone ?? '') ||
				funcao !== (perfil.funcao ?? ''))
	);

	function validarPerfil(): { nome?: string; email?: string } {
		const erros: { nome?: string; email?: string } = {};
		if (!nome.trim()) erros.nome = 'Informe o nome completo.';
		if (!email.trim()) {
			erros.email = 'Informe o e-mail institucional.';
		} else if (!EMAIL_RE.test(email.trim())) {
			erros.email = 'E-mail inválido.';
		}
		return erros;
	}

	const erroPerfilDerivado = $derived(validarPerfil());

	function descartarPerfil(): void {
		if (!perfil) return;
		nome = perfil.nome ?? '';
		email = perfil.email ?? '';
		telefone = perfil.telefone ?? '';
		funcao = perfil.funcao ?? '';
		perfilTentou = false;
	}

	async function salvarPerfil(): Promise<void> {
		perfilTentou = true;
		if (Object.keys(erroPerfilDerivado).length > 0) return;

		const dados: Perfil = {
			nome: nome.trim(),
			email: email.trim(),
			telefone: telefone.trim() || null,
			funcao: funcao.trim() || null,
			avatar: perfil?.avatar ?? null
		};

		try {
			await updatePerfil(dados);
			toasts.success('Perfil atualizado com sucesso.');
		} catch (err) {
			toastError(err, 'Não foi possível salvar o perfil.');
		}
	}

	let senhaAtual = $state('');
	let novaSenha = $state('');
	let confirmacaoSenha = $state('');
	let senhaTentou = $state(false);

	function validarSenha(): { atual?: string; nova?: string; confirmacao?: string } {
		const erros: { atual?: string; nova?: string; confirmacao?: string } = {};
		if (!senhaAtual) erros.atual = 'Informe a senha atual.';
		if (!novaSenha) {
			erros.nova = 'Informe a nova senha.';
		} else if (novaSenha.length < 8) {
			erros.nova = 'A nova senha deve ter pelo menos 8 caracteres.';
		}
		if (!confirmacaoSenha) {
			erros.confirmacao = 'Repita a nova senha.';
		} else if (confirmacaoSenha !== novaSenha) {
			erros.confirmacao = 'As senhas não coincidem.';
		}
		return erros;
	}

	const erroSenhaDerivado = $derived(validarSenha());

	async function atualizarSenha(): Promise<void> {
		senhaTentou = true;
		if (Object.keys(erroSenhaDerivado).length > 0) return;

		try {
			await alterarSenha(senhaAtual, novaSenha, confirmacaoSenha);
			senhaAtual = '';
			novaSenha = '';
			confirmacaoSenha = '';
			senhaTentou = false;
			toasts.success('Senha atualizada com sucesso.');
		} catch (err) {
			toastError(err, 'Não foi possível alterar a senha.');
		}
	}

	let idioma = $state('pt-BR');
	let emailNotificacoes = $state(true);

	const idiomas = [
		{ id: 'pt-BR', label: 'Português (Brasil)' },
		{ id: 'en', label: 'English' },
		{ id: 'es', label: 'Español' }
	];

	const inputCls =
		'w-full rounded-lg border border-border bg-elevated px-3 py-2.5 text-sm text-ink placeholder:text-muted/60 focus:border-brand focus:outline-none focus:ring-1 focus:ring-brand/40';
</script>

<svelte:head>
	<title>Meu perfil — Configurações — FabLab</title>
</svelte:head>

<div class="space-y-5">
	<PageHeader
		title="Meu perfil"
		subtitle="Seus dados de identificação, senha de acesso e preferências."
	>
		{#snippet children()}
			<StatusBadge label="Acesso liberado para todos os níveis" color="success" />
		{/snippet}
	</PageHeader>

	{#if hasError}
		<ErrorBanner
			message="Não foi possível carregar o seu perfil"
			hint={error ?? ''}
			onRetry={() => void goto('/configuracoes/perfil', { invalidateAll: true })}
		/>
	{:else if loading}
		<div class="grid grid-cols-1 gap-5 lg:grid-cols-3">
			<div class="space-y-5 lg:col-span-2">
				<div class="rounded-xl border border-border bg-surface p-5">
					<div class="flex items-center gap-4">
						<Skeleton class="h-16 w-16 rounded-2xl" />
						<div class="flex-1 space-y-2">
							<Skeleton class="h-4 w-40" />
							<Skeleton class="h-3 w-64" />
						</div>
					</div>
				</div>
				<div class="space-y-4 rounded-xl border border-border bg-surface p-5">
					<Skeleton class="h-4 w-32" />
					<div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
						<Skeleton class="h-10 w-full" />
						<Skeleton class="h-10 w-full" />
						<Skeleton class="h-10 w-full" />
						<Skeleton class="h-10 w-full" />
					</div>
					<Skeleton class="h-9 w-36" />
				</div>
			</div>
			<div class="space-y-5">
				<div class="space-y-4 rounded-xl border border-border bg-surface p-5">
					<Skeleton class="h-4 w-28" />
					<Skeleton class="h-10 w-full" />
					<Skeleton class="h-10 w-full" />
					<Skeleton class="h-10 w-full" />
					<Skeleton class="h-9 w-full" />
				</div>
			</div>
		</div>
	{:else}
		<div class="grid grid-cols-1 gap-5 lg:grid-cols-3">
			<div class="space-y-5 lg:col-span-2">
				<!-- Identificação -->
				<div class="rounded-xl border border-border bg-surface p-5" data-testid="cfg-profile-card">
					<div class="flex items-center gap-4">
						<div class="flex h-16 w-16 shrink-0 items-center justify-center rounded-2xl border border-brand/30 bg-brand/20">
							<Avatar name={perfil?.nome ?? ''} size="md" tone="brand" />
						</div>
						<div class="min-w-0">
							<h2 class="truncate text-base font-semibold text-ink">{perfil?.nome ?? '—'}</h2>
							<p class="truncate text-sm text-muted">{perfil?.email ?? '—'}</p>
							<div class="mt-1.5 flex items-center gap-2">
								{#if user}
									<StatusBadge
										label={NIVEL_LABELS[user.role] ?? 'Integrante'}
										color={NIVEL_TONES[user.role] ?? 'muted'}
									/>
								{/if}
								<StatusBadge label="Ativo" color="success" />
							</div>
						</div>
					</div>
				</div>

				<!-- Dados pessoais -->
				<div class="rounded-xl border border-border bg-surface p-5">
					<div class="mb-4 flex items-center justify-between">
						<h2 class="text-base font-semibold text-ink">Dados pessoais</h2>
						<button
							type="button"
							data-testid="cfg-profile-reset"
							onclick={descartarPerfil}
							disabled={!perfilSujo}
							class="text-xs font-medium text-muted transition-colors hover:text-ink disabled:cursor-not-allowed disabled:opacity-50"
						>
							Descartar alterações
						</button>
					</div>
					<div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
						<label class="block">
							<span class="mb-1.5 block text-xs font-medium text-muted">Nome completo *</span>
							<input
								type="text"
								bind:value={nome}
								class={inputCls}
								aria-label="Nome completo"
								data-testid="cfg-profile-nome"
							/>
							{#if perfilTentou && erroPerfilDerivado.nome}
								<p class="mt-1 text-xs text-danger">{erroPerfilDerivado.nome}</p>
							{/if}
						</label>
						<label class="block">
							<span class="mb-1.5 block text-xs font-medium text-muted">E-mail institucional *</span>
							<input
								type="email"
								bind:value={email}
								class={inputCls}
								aria-label="E-mail institucional"
								data-testid="cfg-profile-email"
							/>
							{#if perfilTentou && erroPerfilDerivado.email}
								<p class="mt-1 text-xs text-danger">{erroPerfilDerivado.email}</p>
							{/if}
						</label>
						<label class="block">
							<span class="mb-1.5 block text-xs font-medium text-muted">Telefone</span>
							<input
								type="tel"
								bind:value={telefone}
								placeholder="(00) 00000-0000"
								class={inputCls}
								aria-label="Telefone"
								data-testid="cfg-profile-telefone"
							/>
						</label>
						<label class="block">
							<span class="mb-1.5 block text-xs font-medium text-muted">Curso / função</span>
							<input
								type="text"
								bind:value={funcao}
								placeholder="ex.: Técnico em Informática"
								class={inputCls}
								aria-label="Curso ou função"
								data-testid="cfg-profile-funcao"
							/>
						</label>
					</div>
					<div class="mt-5 flex items-center justify-end gap-3 border-t border-border pt-4">
						<p class="mr-auto text-xs text-muted">Salve para enviar ao servidor.</p>
						<button
							type="button"
							onclick={salvarPerfil}
							disabled={!perfilSujo}
							data-testid="cfg-profile-save"
							class="rounded-lg bg-brand px-4 py-2 text-sm font-semibold text-white shadow-lg shadow-brand/20 transition-colors hover:bg-brandhi disabled:cursor-not-allowed disabled:opacity-50"
						>
							Salvar alterações
						</button>
					</div>
				</div>
			</div>

			<div class="space-y-5">
				<!-- Alterar senha -->
				<div class="rounded-xl border border-border bg-surface p-5">
					<h2 class="mb-4 text-base font-semibold text-ink">Alterar senha</h2>
					<div class="space-y-4">
						<label class="block">
							<span class="mb-1.5 block text-xs font-medium text-muted">Senha atual</span>
							<input
								type="password"
								bind:value={senhaAtual}
								autocomplete="current-password"
								class={inputCls}
								aria-label="Senha atual"
								data-testid="cfg-password-atual"
							/>
							{#if senhaTentou && erroSenhaDerivado.atual}
								<p class="mt-1 text-xs text-danger">{erroSenhaDerivado.atual}</p>
							{/if}
						</label>
						<label class="block">
							<span class="mb-1.5 block text-xs font-medium text-muted">Nova senha</span>
							<input
								type="password"
								bind:value={novaSenha}
								autocomplete="new-password"
								placeholder="Mínimo 8 caracteres"
								class={inputCls}
								aria-label="Nova senha"
								data-testid="cfg-password-nova"
							/>
							{#if senhaTentou && erroSenhaDerivado.nova}
								<p class="mt-1 text-xs text-danger">{erroSenhaDerivado.nova}</p>
							{/if}
						</label>
						<label class="block">
							<span class="mb-1.5 block text-xs font-medium text-muted">Confirmar nova senha</span>
							<input
								type="password"
								bind:value={confirmacaoSenha}
								autocomplete="new-password"
								placeholder="Repita a nova senha"
								class={inputCls}
								aria-label="Confirmar nova senha"
								data-testid="cfg-password-confirmacao"
							/>
							{#if senhaTentou && erroSenhaDerivado.confirmacao}
								<p class="mt-1 text-xs text-danger">{erroSenhaDerivado.confirmacao}</p>
							{/if}
						</label>
					</div>
					<button
						type="button"
						onclick={atualizarSenha}
						data-testid="cfg-password-save"
						class="mt-4 flex w-full items-center justify-center gap-2 rounded-md border border-border bg-elevated px-3 py-2 text-sm text-ink transition-colors hover:bg-elevated/70"
					>
						Atualizar senha
					</button>
				</div>

				<!-- Preferências -->
				<div class="rounded-xl border border-border bg-surface p-5">
					<h2 class="mb-4 text-base font-semibold text-ink">Preferências</h2>
					<div class="space-y-4">
						<Select
							id="cfg-profile-idioma"
							label="Idioma da interface"
							options={idiomas}
							value={idioma}
							onChange={(v) => (idioma = v)}
						/>
						<label class="flex cursor-pointer items-center justify-between gap-3">
							<span class="text-sm text-ink">Notificações por e-mail</span>
							<input
								type="checkbox"
								bind:checked={emailNotificacoes}
								data-testid="cfg-profile-notificacoes"
								class="h-4 w-4 rounded border-border bg-surface text-brand accent-brand"
							/>
						</label>
					</div>
				</div>

				<!-- Sessões ativas -->
				<div class="rounded-xl border border-border bg-surface p-5">
					<h2 class="mb-3 text-base font-semibold text-ink">Sessões ativas</h2>
					<div class="space-y-2 text-sm" data-testid="cfg-sessions-list">
						<div class="flex items-center gap-3 rounded-lg bg-elevated/50 px-3 py-2.5">
							<span class="h-1.5 w-1.5 shrink-0 rounded-full bg-success" aria-hidden="true"></span>
							<span class="truncate text-ink">Este dispositivo</span>
							<span class="ml-auto text-[10px] text-muted">Atual</span>
						</div>
					</div>
					<p class="mt-3 text-xs text-muted">Nenhuma outra sessão ativa.</p>
				</div>
			</div>
		</div>
	{/if}
</div>