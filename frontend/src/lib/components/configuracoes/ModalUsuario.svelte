<script lang="ts">
	import Modal from '$lib/components/ui/Modal.svelte';
	import Select from '$lib/components/ui/Select.svelte';
	import { searchPeople, type PersonOption } from '$lib/api/rh';
	import type { UsuarioPayload } from '$lib/api/configuracoes/usuarios';
	import type { Nivel, SituacaoUsuario, Usuario } from '$lib/types/configuracoes';

	interface Props {
		open: boolean;
		admin: boolean;
		usuario?: Usuario | null;
		onClose: () => void;
		onSave: (payload: UsuarioPayload) => void;
	}

	let { open, admin, usuario = null, onClose, onSave }: Props = $props();

	const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

	const niveis = [
		{ id: '0', label: 'Admin (nível 0)' },
		{ id: '1', label: 'Bolsista (nível 1)' },
		{ id: '2', label: 'Voluntário (nível 2)' },
		{ id: '3', label: 'Estagiário (nível 3)' },
		{ id: '4', label: 'Recrutando (nível 4)' }
	];

	const situacoes = [
		{ id: 'Ativo', label: 'Ativo' },
		{ id: 'Pendente', label: 'Pendente' },
		{ id: 'Desativado', label: 'Desativado' }
	];

	interface ErrosForm {
		pessoa?: string;
		nome?: string;
		email?: string;
		senha?: string;
	}

	const edicao = $derived(usuario !== null);

	let buscaPessoa = $state('');
	let opcoesPessoa = $state<PersonOption[]>([]);
	let buscando = $state(false);
	let pessoaSel = $state<PersonOption | null>(null);
	let nome = $state('');
	let email = $state('');
	let senha = $state('');
	let nivel = $state<Nivel>(1);
	let situacao = $state<SituacaoUsuario>('Pendente');
	let tentou = $state(false);

	$effect(() => {
		if (open) {
			buscaPessoa = '';
			opcoesPessoa = [];
			pessoaSel = null;
			nome = usuario?.nome ?? '';
			email = usuario?.email ?? '';
			senha = '';
			nivel = usuario?.nivel ?? 1;
			situacao = usuario?.situacao ?? 'Pendente';
			tentou = false;
		}
	});

	let buscaSeq = 0;

	async function onBuscarPessoa(valor: string): Promise<void> {
		buscaPessoa = valor;
		pessoaSel = null;
		if (valor.trim().length < 2) {
			opcoesPessoa = [];
			return;
		}
		const seq = ++buscaSeq;
		buscando = true;
		try {
			const lista = await searchPeople(valor.trim());
			if (seq === buscaSeq) opcoesPessoa = lista;
		} catch {
			if (seq === buscaSeq) opcoesPessoa = [];
		} finally {
			if (seq === buscaSeq) buscando = false;
		}
	}

	function escolherPessoa(op: PersonOption): void {
		pessoaSel = op;
		buscaPessoa = op.name;
		nome = op.name;
		opcoesPessoa = [];
	}

	function validar(): ErrosForm {
		const erros: ErrosForm = {};
		if (!edicao && !pessoaSel) erros.pessoa = 'Selecione a pessoa do RH vinculada.';
		if (!nome.trim()) erros.nome = 'Informe o nome.';
		if (!email.trim()) {
			erros.email = 'Informe o e-mail.';
		} else if (!EMAIL_RE.test(email.trim())) {
			erros.email = 'E-mail inválido.';
		}
		if (!edicao && senha.length < 8) erros.senha = 'A senha precisa de ao menos 8 caracteres.';
		return erros;
	}

	const erros = $derived(validar());
	const invalido = $derived(Object.keys(erros).length > 0);

	function salvar(event: SubmitEvent): void {
		event.preventDefault();
		tentou = true;
		if (invalido) return;

		onSave({
			nome: nome.trim(),
			email: email.trim(),
			nivel,
			situacao,
			...(!edicao && pessoaSel ? { idUser: Number(pessoaSel.id) } : {}),
			...(!edicao ? { senha } : {})
		});
	}

	const inputCls =
		'w-full rounded-lg border border-border bg-elevated px-3 py-2.5 text-sm text-ink placeholder:text-muted/60 focus:border-brand focus:outline-none focus:ring-1 focus:ring-brand/40';

	const criarAviso =
		'O acesso é vinculado a uma pessoa do RH. Sem pessoa vinculada, o convite é bloqueado.';
</script>

{#if admin}
	<div data-testid="cfg-user-modal">
		<Modal
			{open}
			title={usuario ? 'Editar usuário' : 'Convidar usuário'}
			subtitle="Conta, nível de acesso e situação"
			{onClose}
			width="md"
		>
			{#snippet children()}
				<form id="cfg-user-form" onsubmit={salvar} novalidate>
					{#if !edicao}
						<label class="relative block">
							<span class="mb-1.5 block text-xs font-medium text-muted">Pessoa do RH *</span>
							<input
								type="text"
								value={buscaPessoa}
								oninput={(e) => void onBuscarPessoa(e.currentTarget.value)}
								placeholder="Buscar por nome…"
								class={inputCls}
								aria-label="Pessoa do RH"
								autocomplete="off"
							/>
							{#if buscando}
								<p class="mt-1 text-xs text-muted">Buscando…</p>
							{:else if opcoesPessoa.length > 0}
								<ul class="absolute z-10 mt-1 max-h-40 w-full overflow-auto rounded-lg border border-border bg-surface shadow-lg">
									{#each opcoesPessoa as op (op.id)}
										<li>
											<button
												type="button"
												onclick={() => escolherPessoa(op)}
												class="block w-full px-3 py-2 text-left text-sm text-ink hover:bg-elevated"
											>
												{op.name}
											</button>
										</li>
									{/each}
								</ul>
							{/if}
							{#if tentou && erros.pessoa}
								<p class="mt-1 text-xs text-danger">{erros.pessoa}</p>
							{/if}
						</label>
					{/if}

					<label class="mt-4 block">
						<span class="mb-1.5 block text-xs font-medium text-muted">Nome *</span>
						<input
							type="text"
							bind:value={nome}
							placeholder="ex.: Maria Souza"
							class={inputCls}
							aria-label="Nome do usuário"
						/>
						{#if tentou && erros.nome}
							<p class="mt-1 text-xs text-danger">{erros.nome}</p>
						{/if}
					</label>

					<label class="mt-4 block">
						<span class="mb-1.5 block text-xs font-medium text-muted">E-mail *</span>
						<input
							type="email"
							bind:value={email}
							placeholder="nome@fablab.org"
							class={inputCls}
							aria-label="E-mail do usuário"
						/>
						{#if tentou && erros.email}
							<p class="mt-1 text-xs text-danger">{erros.email}</p>
						{/if}
					</label>

					{#if !edicao}
						<label class="mt-4 block">
							<span class="mb-1.5 block text-xs font-medium text-muted">Senha inicial *</span>
							<input
								type="password"
								bind:value={senha}
								placeholder="Mínimo 8 caracteres"
								class={inputCls}
								aria-label="Senha inicial"
								autocomplete="new-password"
							/>
							{#if tentou && erros.senha}
								<p class="mt-1 text-xs text-danger">{erros.senha}</p>
							{/if}
						</label>
					{/if}

					<div class="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-2">
						<Select
							id="cfg-user-nivel"
							label="Nível"
							options={niveis}
							value={String(nivel)}
							onChange={(valor) => (nivel = Number(valor) as Nivel)}
							required
						/>
						<Select
							id="cfg-user-situacao"
							label="Situação"
							options={situacoes}
							value={situacao}
							onChange={(valor) => (situacao = valor as SituacaoUsuario)}
							required
						/>
					</div>

					{#if !edicao}
						<p class="mt-4 rounded-lg border border-border bg-elevated/40 px-3 py-2 text-[11px] text-muted">
							{criarAviso}
						</p>
					{/if}
				</form>
			{/snippet}

			{#snippet footer()}
				<button
					type="button"
					data-testid="cfg-user-cancel"
					onclick={onClose}
					class="rounded-lg border border-border bg-surface px-4 py-2 text-sm font-medium text-ink transition-colors hover:bg-border/40"
				>
					Cancelar
				</button>
				<button
					type="submit"
					form="cfg-user-form"
					data-testid="cfg-user-save"
					class="rounded-lg bg-brand px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-brandhi"
				>
					{usuario ? 'Salvar alterações' : 'Enviar convite'}
				</button>
			{/snippet}
		</Modal>
	</div>
{/if}
