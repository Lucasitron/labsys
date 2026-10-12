<script lang="ts">
	import { goto } from '$app/navigation';
	import type { PageProps } from './$types';
	import { get } from 'svelte/store';
	import { auth } from '$lib/stores/auth';
	import type { Nivel, PersonStatus } from '$lib/types/rh';
	import { createPessoa } from '$lib/api/rh/pessoas';
	import { ApiError } from '$lib/api/client';
	import { nivelMeta, personStatusMeta } from '$lib/utils/rh-status';
	import { isAdmin } from '$lib/utils/permissions';
	import { errorMessage } from '$lib/utils/errors';
	import { toasts } from '$lib/stores/toast';
	import PageHeader from '$lib/components/ui/PageHeader.svelte';
	import Select from '$lib/components/ui/Select.svelte';
	import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte';
	import Icon from '$lib/components/ui/Icon.svelte';

	let { data }: PageProps = $props();

	const canEdit = $derived(data.canEdit ?? false);
	const usuarioLogado = $derived(get(auth).user);

	// Rota bloqueada para view-only/Recrutando.
	$effect(() => {
		if (!canEdit) void goto('/pessoas');
	});

	// Dados
	let nome = $state('');
	let matricula = $state('');
	let contato = $state('');
	let cpf = $state('');
	let admissao = $state('');
	let turno = $state('');
	let status = $state<PersonStatus>('ativo');

	// Vínculo (RH) — opcional
	let nivel = $state<Nivel | ''>('');
	let departamento = $state('');

	// Acesso (Auth) — opcional
	let criarAcesso = $state(false);
	let email = $state('');
	let senha = $state('');
	let mostrarSenha = $state(false);

	let ocupado = $state(false);
	let erroTopo = $state<string | null>(null);
	let erros = $state<Record<string, string>>({});

	const NIVEL_OPCOES = $derived(
		(['admin', 'bolsista', 'voluntario', 'estagiario', 'recrutando'] as Nivel[])
			.map((id) => ({ id, label: nivelMeta(id).label }))
			.filter((n) => n.id !== 'admin' || isAdmin(usuarioLogado))
	);

	const STATUS_OPCOES = (['ativo', 'inativo'] as PersonStatus[]).map((s) => ({
		id: s,
		label: personStatusMeta(s).label
	}));

	const DEPARTAMENTO_OPCOES = ['Administrativo', 'Eletrônica', 'Software', 'Mecatrônica', 'Design'];
	const TURNO_OPCOES = ['Manhã', 'Tarde', 'Noite', 'Integral'];

	function validar(): boolean {
		const novos: Record<string, string> = {};
		if (!nome.trim()) novos['nome'] = 'Informe o nome completo.';
		if (!matricula.trim()) novos['matricula'] = 'Informe a matrícula.';
		if (criarAcesso) {
			if (!email.trim()) novos['email'] = 'Informe o e-mail de acesso.';
			else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim()))
				novos['email'] = 'Informe um e-mail válido.';
		}
		if (criarAcesso && senha && senha.length < 8)
			novos['senha'] = 'A senha deve ter ao menos 8 caracteres.';
		erros = novos;
		return Object.keys(novos).length === 0;
	}

	function limpar(): void {
		nome = '';
		matricula = '';
		contato = '';
		cpf = '';
		admissao = '';
		turno = '';
		status = 'ativo';
		nivel = '';
		departamento = '';
		criarAcesso = false;
		email = '';
		senha = '';
		erros = {};
	}

	function gerarSenhaAleatoria(): void {
		const alfabeto = 'abcdefghjkmnpqrstuvwxyzABCDEFGHJKMNPQRSTUVWXYZ23456789!@#';
		const bytes = new Uint32Array(12);
		crypto.getRandomValues(bytes);
		senha = Array.from(bytes, (b) => alfabeto[b % alfabeto.length]).join('');
		erros = { ...erros, senha: '' };
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
			await createPessoa({
				name: nome.trim(),
				matricula: matricula.trim(),
				...(contato.trim() ? { contato: contato.trim() } : {}),
				...(cpf.trim() ? { cpf: cpf.trim() } : {}),
				...(admissao ? { dataAdmissao: admissao } : {}),
				...(turno ? { turno } : {}),
				status,
				...(nivel ? { nivelAcesso: nivel } : {}),
				...(departamento ? { departamento } : {}),
				...(criarAcesso ? { criarAcesso: true, email: email.trim() } : {}),
				...(criarAcesso && senha ? { senha } : {})
			});
			if (adicionarOutro) {
				toasts.success('Pessoa cadastrada. Preencha o próximo cadastro.');
				limpar();
			} else {
				toasts.success(
					criarAcesso && !senha
						? 'Pessoa cadastrada. Convite de acesso enviado.'
						: 'Pessoa cadastrada com sucesso.'
				);
				void goto('/pessoas');
			}
		} catch (err) {
			const msg = errorMessage(err, 'Não foi possível cadastrar a pessoa.');
			const acessoMsg = /e-?mail|acesso|credencial/i.test(msg);
			if (err instanceof ApiError && err.status === 422 && criarAcesso && acessoMsg) {
				erros = { ...erros, email: msg };
				erroTopo = 'Não foi possível provisionar o acesso. Ajuste o campo destacado.';
			} else {
				erroTopo = msg;
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
			subtitle="Cadastre a pessoa, o vínculo e o acesso em uma única operação."
			backHref="/pessoas"
		/>

		{#if erroTopo}
			<ErrorBanner message={erroTopo} testid="person-form-error" />
		{/if}

		<form novalidate class="space-y-5" onsubmit={(e) => e.preventDefault()}>
			<section class="rounded-xl border border-border bg-surface p-5" aria-label="Dados pessoais">
				<h2 class="text-xs font-medium uppercase tracking-wide text-muted">Dados</h2>
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
						<label for="novo-matricula" class={labelCls}>
							Matrícula <span class="text-danger" aria-hidden="true">*</span>
						</label>
						<input
							id="novo-matricula"
							type="text"
							data-testid="person-matricula"
							bind:value={matricula}
							placeholder="Ex.: 2026-001"
							aria-invalid={!!erros['matricula']}
							class="{inputCls} {erros['matricula'] ? inputErroCls : ''} font-mono"
						/>
						{#if erros['matricula']}<p class="mt-1 text-xs text-danger">{erros['matricula']}</p>{/if}
					</div>
					<div>
						<label for="novo-contato" class={labelCls}>Contato (e-mail ou telefone)</label>
						<input
							id="novo-contato"
							type="text"
							bind:value={contato}
							placeholder="nome@email.com ou (48) 99999-0000"
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
						<label for="novo-admissao" class={labelCls}>Data de admissão</label>
						<input id="novo-admissao" type="date" bind:value={admissao} class="{inputCls} tabnums" />
					</div>
					<div>
						<label for="novo-turno" class={labelCls}>Turno</label>
						<select id="novo-turno" bind:value={turno} class={inputCls}>
							<option value="">Selecione…</option>
							{#each TURNO_OPCOES as t (t)}
								<option value={t}>{t}</option>
							{/each}
						</select>
					</div>
					<div>
						<label for="novo-status" class={labelCls}>Status</label>
						<select id="novo-status" bind:value={status} class={inputCls}>
							{#each STATUS_OPCOES as opt (opt.id)}
								<option value={opt.id}>{opt.label}</option>
							{/each}
						</select>
					</div>
				</div>
			</section>

			<section class="rounded-xl border border-border bg-surface p-5" aria-label="Vínculo (RH)">
				<h2 class="text-xs font-medium uppercase tracking-wide text-muted">Vínculo (opcional)</h2>
				<p class="mt-1 text-xs text-muted">
					Informe o nível para criar o vínculo de funcionário. Sem nível, nenhum vínculo é criado.
				</p>
				<div class="mt-4 grid gap-4 sm:grid-cols-2">
					<div>
						<Select
							id="novo-nivel"
							label="Nível de acesso"
							options={NIVEL_OPCOES}
							value={nivel}
							placeholder="Selecione…"
							onChange={(v) => (nivel = v as Nivel)}
						/>
					</div>
					<div>
						<label for="novo-departamento" class={labelCls}>Departamento</label>
						<select id="novo-departamento" bind:value={departamento} class={inputCls}>
							<option value="">Selecione…</option>
							{#each DEPARTAMENTO_OPCOES as dep (dep)}
								<option value={dep}>{dep}</option>
							{/each}
						</select>
					</div>
				</div>
			</section>

			<section class="rounded-xl border border-border bg-surface p-5" aria-label="Acesso (Auth)">
				<h2 class="text-xs font-medium uppercase tracking-wide text-muted">Acesso (opcional)</h2>
				<label class="mt-3 flex cursor-pointer items-center gap-2.5 text-sm text-ink">
					<input
						type="checkbox"
						data-testid="novo-criar-acesso"
						bind:checked={criarAcesso}
						class="h-4 w-4 rounded border-border bg-surface accent-brand"
					/>
					<span>Provisionar acesso ao sistema</span>
				</label>

				{#if criarAcesso}
					<div class="mt-4 grid gap-4 sm:grid-cols-2">
						<div>
							<label for="novo-email" class={labelCls}>
								E-mail de acesso <span class="text-danger" aria-hidden="true">*</span>
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
							<label for="novo-senha" class={labelCls}>Senha inicial</label>
							<div class="flex gap-2">
								<input
									id="novo-senha"
									type={mostrarSenha ? 'text' : 'password'}
									bind:value={senha}
									placeholder="Deixe vazio para enviar convite"
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
									onclick={gerarSenhaAleatoria}
									class="shrink-0 rounded-md border border-border bg-elevated px-3 py-2 text-sm font-medium text-ink transition hover:border-brand/50 hover:text-brandhi"
								>
									Gerar
								</button>
							</div>
							{#if erros['senha']}<p class="mt-1 text-xs text-danger">{erros['senha']}</p>{/if}
							<p class="mt-1 text-xs text-muted">
								Sem senha, a conta é criada como convite (pendente).
							</p>
						</div>
					</div>
				{/if}
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
