<script lang="ts">
	import { goto } from '$app/navigation';
	import type { PageProps } from './$types';
	import type { Nivel, PersonStatus } from '$lib/types/rh';
	import { updatePessoa } from '$lib/api/rh/pessoas';
	import { ApiError } from '$lib/api/client';
	import { nivelMeta, personStatusMeta } from '$lib/utils/rh-status';
	import { toUserMessage } from '$lib/utils/errors';
	import { toasts } from '$lib/stores/toast';
	import PageHeader from '$lib/components/ui/PageHeader.svelte';
	import StatusBadge from '$lib/components/ui/StatusBadge.svelte';
	import EmptyState from '$lib/components/ui/EmptyState.svelte';
	import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte';
	import Icon from '$lib/components/ui/Icon.svelte';

	let { data }: PageProps = $props();

	const canEdit = $derived(data.canEdit ?? false);
	const pessoa = $derived(data.pessoa);
	const id = $derived(data.id);

	const carregando = $derived(!pessoa && !data.notFound && !data.error);

	const NIVEL_OPCOES = (['admin', 'bolsista', 'voluntario', 'estagiario', 'recrutando'] as Nivel[]).map(
		(n) => ({ id: n, label: nivelMeta(n).label })
	);
	const STATUS_OPCOES = (['ativo', 'inativo', 'afastado'] as PersonStatus[]).map((s) => ({
		id: s,
		label: personStatusMeta(s).label
	}));
	const DEPARTAMENTO_OPCOES = ['Administrativo', 'Eletrônica', 'Software', 'Mecatrônica', 'Design'];
	const TURNO_OPCOES = ['Manhã', 'Tarde', 'Noite', 'Integral'];

	function spec(chave: string): string {
		return pessoa?.specs?.[chave] ?? '';
	}

	let nome = $state('');
	let email = $state('');
	let telefone = $state('');
	let admissao = $state('');
	let nivel = $state<Nivel>('voluntario');
	let departamento = $state('');
	let turno = $state('');
	let status = $state<PersonStatus>('ativo');
	let ehInstrutor = $state(false);
	let qualificacao = $state(0);
	let inicializado = $state(false);

	$effect(() => {
		if (pessoa && !inicializado) {
			nome = pessoa.name;
			email = pessoa.email;
			telefone = pessoa.phone ?? '';
			admissao = spec('Admissão') || pessoa.joinedAt || '';
			nivel = pessoa.nivel;
			departamento = spec('Departamento');
			turno = spec('Turno');
			status = pessoa.status;
			ehInstrutor = pessoa.isInstrutor === true;
			qualificacao = pessoa.qualification ?? 0;
			inicializado = true;
		}
	});

	let ocupado = $state(false);
	let erroTopo = $state<string | null>(null);
	let erros = $state<Record<string, string>>({});

	function validar(): boolean {
		const novos: Record<string, string> = {};
		if (!nome.trim()) novos['nome'] = 'Informe o nome completo.';
		if (!email.trim()) novos['email'] = 'Informe o e-mail.';
		else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim()))
			novos['email'] = 'Informe um e-mail válido.';
		erros = novos;
		return Object.keys(novos).length === 0;
	}

	async function salvar(): Promise<void> {
		if (ocupado || !canEdit) return;
		erroTopo = null;
		if (!validar()) {
			erroTopo = 'Verifique os campos destacados e tente novamente.';
			return;
		}
		ocupado = true;
		try {
			// department/shift/status/admission seguem o contrato PUT documentado 🟡;
			// os tipos B1 cobrem o núcleo (Partial<CreatePessoaPayload>).
			const extras: Record<string, string> = {
				...(admissao ? { dataAdmissao: admissao } : {}),
				...(departamento ? { department: departamento } : {}),
				...(turno ? { shift: turno } : {}),
				status
			};
			await updatePessoa(id, {
				name: nome.trim(),
				email: email.trim(),
				...(telefone.trim() ? { phone: telefone.trim() } : {}),
				nivel,
				isInstrutor: ehInstrutor,
				...(ehInstrutor && qualificacao > 0 ? { qualification: qualificacao } : {}),
				...extras
			});
			toasts.success('Cadastro atualizado com sucesso.');
			void goto(`/pessoas/${id}`);
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

	const stAtual = $derived(pessoa ? personStatusMeta(pessoa.status) : null);
	const inputCls =
		'w-full rounded-md border border-border bg-elevated px-3 py-2.5 text-sm text-ink placeholder:text-muted/60 focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/30 transition disabled:cursor-not-allowed disabled:opacity-60';
	const inputErroCls = 'border-danger/60 focus:border-danger focus:ring-danger/30';
	const labelCls = 'mb-1 block text-xs font-medium text-muted';
</script>

<svelte:head>
	<title>Editar pessoa — Pessoas — FabLab</title>
</svelte:head>

<div class="space-y-5" data-testid="edit-person">
	<a
		href={`/pessoas/${id}`}
		data-testid="edit-back"
		class="inline-flex w-fit items-center gap-1.5 text-xs font-medium text-muted transition-colors hover:text-ink"
	>
		<Icon name="arrow-left" class="h-3.5 w-3.5" /> Voltar ao perfil
	</a>

	<div class="flex flex-wrap items-start justify-between gap-3">
		<PageHeader
			title="Editar pessoa"
			subtitle={pessoa ? `${pessoa.name} · ${pessoa.matricula}` : 'Carregando…'}
		/>
		{#if stAtual}
			<StatusBadge label={stAtual.label} color={stAtual.color} />
		{/if}
	</div>

	{#if !canEdit}
		<div
			data-testid="edit-rbac-banner"
			role="note"
			class="flex items-start gap-3 rounded-xl border border-brand/30 bg-brand/10 p-3.5"
		>
			<Icon name="lock" class="mt-0.5 h-4 w-4 shrink-0 text-brandhi" />
			<p class="text-xs text-muted">
				Somente <span class="text-ink">Admin</span> ou o <span class="text-ink">responsável</span>
				pelo cadastro podem editar. Seu acesso é de leitura — o botão de edição não é exibido
				para este perfil.
			</p>
		</div>
	{/if}

	{#if data.notFound}
		<div class="rounded-xl border border-border bg-surface">
			<EmptyState
				icon="user"
				title="Pessoa não encontrada"
				description="O cadastro pode ter sido excluído ou o endereço está incorreto."
			>
				{#snippet children()}
					<a
						href="/pessoas"
						class="rounded-md border border-border bg-elevated px-3 py-2 text-sm font-medium text-ink transition hover:border-brand/50 hover:text-brandhi"
					>
						Voltar à lista
					</a>
				{/snippet}
			</EmptyState>
		</div>
	{:else if data.error || (!carregando && !pessoa)}
		<ErrorBanner
			message="Não foi possível carregar a pessoa"
			hint={data.error ?? 'Verifique sua conexão e tente novamente.'}
			onRetry={() => void goto(`/pessoas/${id}/editar`, { invalidateAll: true })}
			testid="edit-person-retry"
		/>
	{:else if carregando || !pessoa}
		<div class="space-y-5" aria-hidden="true">
			{#each [0, 1] as bloco (bloco)}
				<div class="rounded-xl border border-border bg-surface p-5">
					<div class="h-3 w-32 animate-pulse rounded bg-elevated"></div>
					<div class="mt-4 grid gap-4 sm:grid-cols-2">
						{#each [0, 1, 2, 3] as campo (campo)}
							<div class="h-10 animate-pulse rounded-md bg-elevated"></div>
						{/each}
					</div>
				</div>
			{/each}
		</div>
	{:else}
		{#if erroTopo}
			<ErrorBanner message={erroTopo} testid="edit-person-error" />
		{/if}

		<form novalidate class="space-y-5" onsubmit={(e) => e.preventDefault()}>
			<fieldset disabled={!canEdit} class="space-y-5 disabled:opacity-100">
				<section class="rounded-xl border border-border bg-surface p-5" aria-label="Dados pessoais">
					<h2 class="text-xs font-medium uppercase tracking-wide text-muted">Dados pessoais</h2>
					<div class="mt-4 grid gap-4 sm:grid-cols-2">
						<div>
							<label for="editar-nome" class={labelCls}>
								Nome completo <span class="text-danger" aria-hidden="true">*</span>
							</label>
							<input
								id="editar-nome"
								type="text"
								data-testid="person-name"
								bind:value={nome}
								autocomplete="name"
								aria-invalid={!!erros['nome']}
								class="{inputCls} {erros['nome'] ? inputErroCls : ''}"
							/>
							{#if erros['nome']}<p class="mt-1 text-xs text-danger">{erros['nome']}</p>{/if}
						</div>
						<div>
							<label for="editar-matricula" class={labelCls}>
								Matrícula <span class="text-danger" aria-hidden="true">*</span>
							</label>
							<input
								id="editar-matricula"
								type="text"
								data-testid="person-matricula"
								value={pessoa.matricula}
								readonly
								aria-readonly="true"
								title="A matrícula identifica o registro e não pode ser alterada"
								class="{inputCls} cursor-not-allowed font-mono opacity-70"
							/>
						</div>
						<div>
							<label for="editar-admissao" class={labelCls}>Data de admissão</label>
							<input
								id="editar-admissao"
								type="date"
								data-testid="person-admission"
								bind:value={admissao}
								class="{inputCls} tabnums"
							/>
						</div>
						<div>
							<label for="editar-email" class={labelCls}>
								E-mail <span class="text-danger" aria-hidden="true">*</span>
							</label>
							<input
								id="editar-email"
								type="email"
								data-testid="person-email"
								bind:value={email}
								autocomplete="email"
								aria-invalid={!!erros['email']}
								class="{inputCls} {erros['email'] ? inputErroCls : ''}"
							/>
							{#if erros['email']}<p class="mt-1 text-xs text-danger">{erros['email']}</p>{/if}
						</div>
						<div class="sm:col-span-2">
							<label for="editar-telefone" class={labelCls}>Telefone / contato</label>
							<input
								id="editar-telefone"
								type="tel"
								data-testid="person-phone"
								bind:value={telefone}
								autocomplete="tel"
								class={inputCls}
							/>
						</div>
					</div>
				</section>

				<section
					class="rounded-xl border border-border bg-surface p-5"
					aria-label="Função no laboratório"
				>
					<h2 class="text-xs font-medium uppercase tracking-wide text-muted">
						Função no laboratório
					</h2>
					<div class="mt-4 grid gap-4 sm:grid-cols-2">
						<div>
							<label for="editar-nivel" class={labelCls}>Nível de acesso</label>
							<select id="editar-nivel" data-testid="person-level" bind:value={nivel} class={inputCls}>
								{#each NIVEL_OPCOES as opt (opt.id)}
									<option value={opt.id}>{opt.label}</option>
								{/each}
							</select>
						</div>
						<div>
							<label for="editar-departamento" class={labelCls}>Departamento</label>
							<select
								id="editar-departamento"
								data-testid="person-department"
								bind:value={departamento}
								class={inputCls}
							>
								<option value="">Selecione…</option>
								{#each DEPARTAMENTO_OPCOES as dep (dep)}
									<option value={dep}>{dep}</option>
								{/each}
							</select>
						</div>
						<div>
							<label for="editar-turno" class={labelCls}>Turno</label>
							<select id="editar-turno" data-testid="person-shift" bind:value={turno} class={inputCls}>
								<option value="">Selecione…</option>
								{#each TURNO_OPCOES as t (t)}
									<option value={t}>{t}</option>
								{/each}
							</select>
						</div>
						<div>
							<label for="editar-status" class={labelCls}>Status</label>
							<select
								id="editar-status"
								data-testid="person-status"
								bind:value={status}
								class={inputCls}
							>
								{#each STATUS_OPCOES as opt (opt.id)}
									<option value={opt.id}>{opt.label}</option>
								{/each}
							</select>
						</div>
						<div class="sm:col-span-2">
							<label class="flex w-fit cursor-pointer items-center gap-2.5 text-sm text-ink">
								<input
									type="checkbox"
									data-testid="is-instrutor"
									bind:checked={ehInstrutor}
									class="h-4 w-4 rounded border-border bg-surface accent-brand"
								/>
								<span>É instrutor(a)</span>
							</label>
							{#if ehInstrutor}
								<div class="mt-3">
									<p class={labelCls}>Qualificação técnica</p>
									<div
										class="flex flex-wrap gap-1.5"
										data-testid="person-qualification"
										role="group"
										aria-label="Qualificação de 1 a 6"
									>
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
					</div>
				</section>
			</fieldset>

			<div class="flex items-start gap-3 rounded-xl border border-border bg-elevated/50 p-4">
				<Icon name="lock" class="mt-0.5 h-4 w-4 shrink-0 text-muted" />
				<p class="text-xs text-muted">
					Credenciais de acesso não são alteradas aqui; níveis são geridos em
					<a href="/pessoas/niveis" class="font-medium text-brandhi hover:text-brand">
						Níveis &amp; acesso (Admin)
					</a>.
				</p>
			</div>

			{#if canEdit}
				<footer class="flex flex-wrap items-center gap-2">
					<a
						href={`/pessoas/${id}`}
						data-testid="person-cancel"
						class="rounded-md border border-border bg-elevated px-4 py-2 text-sm font-medium text-ink transition hover:bg-elevated/70"
					>
						Cancelar
					</a>
					<button
						type="button"
						data-testid="person-save"
						onclick={() => void salvar()}
						disabled={ocupado}
						class="rounded-md bg-brand px-4 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi disabled:opacity-50"
					>
						{ocupado ? 'Salvando…' : 'Salvar alterações'}
					</button>
				</footer>
			{/if}
		</form>
	{/if}
</div>
