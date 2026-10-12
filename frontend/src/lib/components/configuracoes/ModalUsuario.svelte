<script lang="ts">
	import Modal from '$lib/components/ui/Modal.svelte';
	import Select from '$lib/components/ui/Select.svelte';
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
		nome?: string;
		email?: string;
	}

	let nome = $state('');
	let email = $state('');
	let nivel = $state<Nivel>(1);
	let situacao = $state<SituacaoUsuario>('Pendente');
	let tentou = $state(false);

	$effect(() => {
		if (open) {
			nome = usuario?.nome ?? '';
			email = usuario?.email ?? '';
			nivel = usuario?.nivel ?? 1;
			situacao = usuario?.situacao ?? 'Pendente';
			tentou = false;
		}
	});

	function validar(): ErrosForm {
		const erros: ErrosForm = {};
		if (!usuario) return erros;
		if (!nome.trim()) erros.nome = 'Informe o nome.';
		if (!email.trim()) {
			erros.email = 'Informe o e-mail.';
		} else if (!EMAIL_RE.test(email)) {
			erros.email = 'E-mail inválido.';
		}
		return erros;
	}

	function salvar(event: SubmitEvent): void {
		event.preventDefault();
		tentou = true;
		if (!usuario) return;
		const erros = validar();
		if (Object.keys(erros).length > 0) return;
		onSave({ nome: nome.trim(), email: email.trim(), nivel, situacao });
	}

	const inputCls =
		'w-full rounded-lg border border-border bg-elevated px-3 py-2.5 text-sm text-ink placeholder:text-muted/60 focus:border-brand focus:outline-none focus:ring-1 focus:ring-brand/40';
</script>

{#if admin}
	<div data-testid="cfg-user-modal">
		<Modal
			{open}
			title="Editar usuário"
			subtitle="Situação e nível de acesso da conta"
			{onClose}
			width="md"
		>
			{#snippet children()}
				<form id="cfg-user-form" onsubmit={salvar} novalidate>
					<label class="block">
						<span class="mb-1.5 block text-xs font-medium text-muted">Nome *</span>
						<input
							type="text"
							bind:value={nome}
							placeholder="Nome do usuário"
							class={inputCls}
							aria-label="Nome"
						/>
						{#if tentou && validar().nome}
							<p class="mt-1 text-xs text-danger">{validar().nome}</p>
						{/if}
					</label>

					<label class="mt-4 block">
						<span class="mb-1.5 block text-xs font-medium text-muted">E-mail *</span>
						<input
							type="email"
							bind:value={email}
							placeholder="usuario@fablab.org"
							class={inputCls}
							aria-label="E-mail"
						/>
						{#if tentou && validar().email}
							<p class="mt-1 text-xs text-danger">{validar().email}</p>
						{/if}
					</label>

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

					<p class="mt-4 rounded-lg border border-border bg-elevated/40 px-3 py-2 text-[11px] text-muted">
						Para criar um novo acesso, cadastre a pessoa em Pessoas → Nova pessoa, marcando
						"Criar acesso".
					</p>
				</form>
			{/snippet}
			{#snippet footer()}
				<button
					type="button"
					onclick={onClose}
					class="rounded-lg border border-border bg-surface px-4 py-2 text-sm font-medium text-ink transition-colors hover:bg-elevated/40"
				>
					Cancelar
				</button>
				<button
					type="submit"
					form="cfg-user-form"
					class="rounded-lg bg-brand px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-brandhi"
				>
					Salvar
				</button>
			{/snippet}
		</Modal>
	</div>
{/if}
