<script lang="ts">
	import { goto } from '$app/navigation';
	import type { PageProps } from './$types';
	import type { Nivel, PersonType } from '$lib/types/rh';
	import { createPessoa } from '$lib/api/rh/pessoas';
	import { ApiError } from '$lib/api/client';
	import { nivelMeta } from '$lib/utils/rh-status';
	import { toUserMessage } from '$lib/utils/errors';
	import { toasts } from '$lib/stores/toast';
	import PageHeader from '$lib/components/ui/PageHeader.svelte';
	import RadioCards from '$lib/components/ui/RadioCards.svelte';
	import Select from '$lib/components/ui/Select.svelte';
	import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte';
	import Icon from '$lib/components/ui/Icon.svelte';

	let { data }: PageProps = $props();

	const canEdit = $derived(data.canEdit ?? false);

	// Rota bloqueada para view-only/Recrutando.
	$effect(() => {
		if (!canEdit) void goto('/pessoas');
	});

	let nome = $state('');
	let email = $state('');
	let telefone = $state('');
	let cpf = $state('');
	let nascimento = $state('');
	let tipo = $state<PersonType>('voluntario');
	let ehInstrutor = $state(false);
	let qualificacao = $state(0);
	let nivel = $state<Nivel | ''>('');
	let grupo = $state('');
	let cursoTurma = $state('');
	let senha = $state('');
	let mostrarSenha = $state(false);

	let ocupado = $state(false);
	let erroTopo = $state<string | null>(null);
	let erros = $state<Record<string, string>>({});

	const TIPO_OPCOES = [
		{ id: 'bolsista', label: 'Bolsista', description: 'Bolsa ativa no laboratório', color: 'brand' as const },
		{ id: 'voluntario', label: 'Voluntário', description: 'Atuação voluntária', color: 'success' as const },
		{ id: 'estagiario', label: 'Estagiário', description: 'Estágio curricular', color: 'warn' as const }
	];

	const NIVEL_OPCOES = (['admin', 'bolsista', 'voluntario', 'estagiario', 'recrutando'] as Nivel[]).map(
		(id) => ({ id, label: nivelMeta(id).label })
	);

	const GRUPO_OPCOES = ['G-01', 'G-02', 'G-03', 'G-04', 'G-05'].map((id) => ({
		id,
		label: `${id}`
	}));

	const usuario = $derived(email.includes('@') ? email.split('@')[0].trim() : '');

	function gerarSenha(): void {
		const alfabeto = 'abcdefghjkmnpqrstuvwxyzABCDEFGHJKMNPQRSTUVWXYZ23456789!@#';
		const bytes = new Uint32Array(12);
		crypto.getRandomValues(bytes);
		senha = Array.from(bytes, (b) => alfabeto[b % alfabeto.length]).join('');
		erros = { ...erros, senha: '' };
	}

	function validar(): boolean {
		const novos: Record<string, string> = {};
		if (!nome.trim()) novos['nome'] = 'Informe o nome completo.';
		if (!email.trim()) novos['email'] = 'Informe o e-mail.';
		else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim()))
			novos['email'] = 'Informe um e-mail válido.';
		if (!nivel) novos['nivel'] = 'Selecione o nível inicial.';
		if (!grupo) novos['grupo'] = 'Selecione o grupo.';
		if (!senha) novos['senha'] = 'Informe a senha inicial ou use Gerar.';
		else if (senha.length < 6) novos['senha'] = 'A senha deve ter ao menos 6 caracteres.';
		erros = novos;
		return Object.keys(novos).length === 0;
	}

	function limpar(): void {
		nome = '';
		email = '';
		telefone = '';
		cpf = '';
		nascimento = '';
		tipo = 'voluntario';
		ehInstrutor = false;
		qualificacao = 0;
		nivel = '';
		grupo = '';
		cursoTurma = '';
		senha = '';
		erros = {};
		erroTopo = null;
	}

	async function salvar(adicionarOutro: boolean): Promise<void> {
		if (ocupado) return;
		erroTopo = null;
		if (!validar()) {
			erroTopo = 'Verifique os campos destacados e tente novamente.';
			return;
		}
		ocupado = true;
		try {
			// Campos extras (cpf/nascimento/curso/credencial) seguem o contrato PUT/POST
			// documentado 🟡 (D-2); o vínculo de credencial com /api/auth/users será confirmado.
			const extras: Record<string, string> = {
				...(cpf.trim() ? { cpf: cpf.trim() } : {}),
				...(nascimento ? { dataNascimento: nascimento } : {}),
				...(cursoTurma.trim() ? { cursoTurma: cursoTurma.trim() } : {}),
				...(usuario ? { username: usuario } : {}),
				...(senha ? { senha } : {})
			};
			await createPessoa({
				name: nome.trim(),
				email: email.trim(),
				...(telefone.trim() ? { phone: telefone.trim() } : {}),
				type: tipo,
				nivel: nivel as Nivel,
				grupoId: grupo,
				isInstrutor: ehInstrutor,
				...(ehInstrutor && qualificacao > 0 ? { qualification: qualificacao } : {}),
				...extras
			});
			if (adicionarOutro) {
				toasts.success('Pessoa cadastrada. Preencha o próximo cadastro.');
				limpar();
			} else {
				toasts.success('Pessoa cadastrada com sucesso.');
				void goto('/pessoas');
			}
		} catch (err) {
			if (err instanceof ApiError && err.status === 409) {
				erros = { ...erros, email: 'Este e-mail já está em uso por outra pessoa.' };
				erroTopo = 'E-mail duplicado. Ajuste o campo destacado.';
			} else {
				erroTopo = toUserMessage(err).message;
			}
		} finally {
			ocupado = false;
		}
	}

	const inputCls =
		'w-full rounded-md border border-border bg-elevated px-3 py-2.5 text-sm text-ink placeholder:text-muted/60 focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/30 transition';
	const inputErroCls = 'border-danger/60 focus:border-danger focus:ring-danger/30';
	const labelCls = 'mb-1 block text-xs font-medium text-muted';
</script>

<svelte:head>
	<title>Novo cadastro — Pessoas — FabLab</title>
</svelte:head>

{#if canEdit}
	<div class="space-y-5">
		<PageHeader
			title="Novo cadastro"
			subtitle="Cadastre uma pessoa no laboratório."
			backHref="/pessoas"
		/>

		{#if erroTopo}
			<ErrorBanner message={erroTopo} testid="person-form-error" />
		{/if}

		<form novalidate class="space-y-5" onsubmit={(e) => e.preventDefault()}>
			<section class="rounded-xl border border-border bg-surface p-5" aria-label="Dados pessoais">
				<h2 class="text-xs font-medium uppercase tracking-wide text-muted">Dados pessoais</h2>
				<div class="mt-4 grid gap-4 sm:grid-cols-2">
					<div>
						<label for="novo-nome" class={labelCls}>
							Nome completo <span class="text-danger" aria-hidden="true">*</span>
						</label>
						<input
							id="novo-nome"
							type="text"
							data-testid="person-name"
							bind:value={nome}
							placeholder="Nome e sobrenome"
							autocomplete="name"
							aria-invalid={!!erros['nome']}
							class="{inputCls} {erros['nome'] ? inputErroCls : ''}"
						/>
						{#if erros['nome']}<p class="mt-1 text-xs text-danger">{erros['nome']}</p>{/if}
					</div>
					<div>
						<label for="novo-email" class={labelCls}>
							E-mail <span class="text-danger" aria-hidden="true">*</span>
						</label>
						<input
							id="novo-email"
							type="email"
							data-testid="person-email"
							bind:value={email}
							placeholder="nome@email.com"
							autocomplete="email"
							aria-invalid={!!erros['email']}
							class="{inputCls} {erros['email'] ? inputErroCls : ''}"
						/>
						{#if erros['email']}<p class="mt-1 text-xs text-danger">{erros['email']}</p>{/if}
					</div>
					<div>
						<label for="novo-telefone" class={labelCls}>Telefone</label>
						<input
							id="novo-telefone"
							type="tel"
							bind:value={telefone}
							placeholder="(48) 99999-0000"
							autocomplete="tel"
							class={inputCls}
						/>
					</div>
					<div>
						<label for="novo-cpf" class={labelCls}>CPF</label>
						<input
							id="novo-cpf"
							type="text"
							bind:value={cpf}
							placeholder="000.000.000-00"
							inputmode="numeric"
							class="{inputCls} font-mono"
						/>
					</div>
					<div>
						<label for="novo-nascimento" class={labelCls}>Data de nascimento</label>
						<input id="novo-nascimento" type="date" bind:value={nascimento} class="{inputCls} tabnums" />
					</div>
				</div>
			</section>

			<section class="rounded-xl border border-border bg-surface p-5" aria-label="Função no laboratório">
				<h2 class="text-xs font-medium uppercase tracking-wide text-muted">Função no laboratório</h2>
				<div class="mt-4 space-y-4">
					<div>
						<p id="novo-tipo-label" class="{labelCls} text-xs font-medium text-muted">Tipo</p>
						<RadioCards
							name="novo-tipo"
							options={TIPO_OPCOES}
							value={tipo}
							onChange={(v) => (tipo = v as PersonType)}
						/>
					</div>
					<div>
						<label class="flex cursor-pointer items-center gap-2.5 text-sm text-ink">
							<input
								type="checkbox"
								data-testid="is-instrutor"
								bind:checked={ehInstrutor}
								class="h-4 w-4 rounded border-border bg-surface accent-brand"
							/>
							<span>É instrutor(a)?</span>
						</label>
						{#if ehInstrutor}
							<div class="mt-3">
								<p class={labelCls}>Qualificação técnica</p>
								<div class="flex flex-wrap gap-1.5" data-testid="person-qualification" role="group" aria-label="Qualificação de 1 a 6">
									{#each [1, 2, 3, 4, 5, 6] as n (n)}
										<button
											type="button"
											onclick={() => (qualificacao = qualificacao === n ? 0 : n)}
											aria-pressed={qualificacao === n}
											aria-label={`Qualificação ${n}`}
											class="inline-flex h-8 w-8 items-center justify-center rounded-md border font-mono text-xs transition {qualificacao === n
												? 'border-brand/60 bg-brand/15 text-brandhi'
												: 'border-border bg-elevated text-muted hover:border-brand/40 hover:text-ink'}"
										>
											{n}
										</button>
									{/each}
								</div>
							</div>
						{/if}
					</div>
					<div class="grid gap-4 sm:grid-cols-2">
						<div>
							<Select
								id="novo-nivel"
								label="Nível inicial"
								options={NIVEL_OPCOES}
								value={nivel}
								placeholder="Selecione…"
								onChange={(v) => {
									nivel = v as Nivel;
									erros = { ...erros, nivel: '' };
								}}
								required
							/>
							{#if erros['nivel']}<p class="mt-1 text-xs text-danger">{erros['nivel']}</p>{/if}
						</div>
						<div>
							<Select
								id="novo-grupo"
								label="Grupo"
								options={GRUPO_OPCOES}
								value={grupo}
								placeholder="Selecione…"
								onChange={(v) => {
									grupo = v;
									erros = { ...erros, grupo: '' };
								}}
								required
							/>
							{#if erros['grupo']}<p class="mt-1 text-xs text-danger">{erros['grupo']}</p>{/if}
						</div>
						<div class="sm:col-span-2">
							<label for="novo-curso" class={labelCls}>Curso / turma</label>
							<input
								id="novo-curso"
								type="text"
								bind:value={cursoTurma}
								placeholder="Ex.: Engenharia Mecatrônica · Turma 2026-1"
								class={inputCls}
							/>
						</div>
					</div>
				</div>
			</section>

			<section class="rounded-xl border border-border bg-surface p-5" aria-label="Credenciais">
				<h2 class="text-xs font-medium uppercase tracking-wide text-muted">Credenciais</h2>
				<div class="mt-4 grid gap-4 sm:grid-cols-2">
					<div>
						<label for="novo-usuario" class={labelCls}>Usuário</label>
						<input
							id="novo-usuario"
							type="text"
							value={usuario}
							readonly
							placeholder="Gerado a partir do e-mail"
							aria-describedby="novo-usuario-ajuda"
							class="{inputCls} cursor-not-allowed opacity-70"
						/>
						<p id="novo-usuario-ajuda" class="mt-1 text-xs text-muted">
							Gerado automaticamente a partir do e-mail.
						</p>
					</div>
					<div>
						<label for="novo-senha" class={labelCls}>
							Senha inicial <span class="text-danger" aria-hidden="true">*</span>
						</label>
						<div class="flex gap-2">
							<input
								id="novo-senha"
								type={mostrarSenha ? 'text' : 'password'}
								bind:value={senha}
								placeholder="Senha inicial"
								autocomplete="new-password"
								aria-invalid={!!erros['senha']}
								class="{inputCls} {erros['senha'] ? inputErroCls : ''} font-mono"
							/>
							<button
								type="button"
								onclick={() => (mostrarSenha = !mostrarSenha)}
								aria-label={mostrarSenha ? 'Ocultar senha' : 'Mostrar senha'}
								class="shrink-0 rounded-md border border-border bg-elevated px-3 text-muted transition hover:text-ink"
							>
								<Icon name="eye" class="h-4 w-4" />
							</button>
							<button
								type="button"
								onclick={gerarSenha}
								class="shrink-0 rounded-md border border-border bg-elevated px-3 py-2 text-sm font-medium text-ink transition hover:border-brand/50 hover:text-brandhi"
							>
								Gerar
							</button>
						</div>
						{#if erros['senha']}<p class="mt-1 text-xs text-danger">{erros['senha']}</p>{/if}
					</div>
				</div>
			</section>

			<footer class="flex flex-wrap items-center gap-2">
				<a
					href="/pessoas"
					class="rounded-md border border-border bg-elevated px-4 py-2 text-sm font-medium text-ink transition hover:bg-elevated/70"
				>
					Cancelar
				</a>
				<span class="flex-1"></span>
				<button
					type="button"
					onclick={() => void salvar(true)}
					disabled={ocupado}
					class="rounded-md border border-border bg-surface px-4 py-2 text-sm font-medium text-ink transition hover:border-brand/50 hover:text-brandhi disabled:opacity-50"
				>
					{ocupado ? 'Salvando…' : 'Salvar e adicionar outro'}
				</button>
				<button
					type="button"
					data-testid="submit-person"
					onclick={() => void salvar(false)}
					disabled={ocupado}
					class="rounded-md bg-brand px-4 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi disabled:opacity-50"
				>
					{ocupado ? 'Salvando…' : 'Salvar cadastro'}
				</button>
			</footer>
		</form>
	</div>
{/if}
