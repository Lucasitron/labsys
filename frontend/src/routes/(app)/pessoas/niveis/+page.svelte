<script lang="ts">
	import { getNiveis, alterarNivel, convidarUsuario } from '$lib/api/rh/niveis';
	import { get } from 'svelte/store';
	import { auth } from '$lib/stores/auth';
	import type { Nivel, NivelMatrix } from '$lib/types/rh';
	import { nivelMeta } from '$lib/utils/rh-status';
	import { isAdmin } from '$lib/utils/permissions';
	import { toUserMessage } from '$lib/utils/errors';
	import { toasts } from '$lib/stores/toast';
	import PageHeader from '$lib/components/ui/PageHeader.svelte';
	import Avatar from '$lib/components/ui/Avatar.svelte';
	import StatusBadge from '$lib/components/ui/StatusBadge.svelte';
	import Modal from '$lib/components/ui/Modal.svelte';
	import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte';
	import EmptyState from '$lib/components/ui/EmptyState.svelte';
	import TableSkeleton from '$lib/components/ui/TableSkeleton.svelte';
	import Icon from '$lib/components/ui/Icon.svelte';

	const NIVEIS_FALLBACK: Nivel[] = ['admin', 'bolsista', 'voluntario', 'estagiario', 'recrutando'];

	const usuario = $derived(get(auth).user);

	let dados = $state<NivelMatrix | null>(null);
	let carregando = $state(true);
	let erro = $state<string | null>(null);
	let recarregar = $state(0);
	let trocandoId = $state<string | null>(null);

	const niveis = $derived(
		(dados?.niveis?.length ? dados.niveis : NIVEIS_FALLBACK).filter(
			(n) => n !== 'admin' || isAdmin(usuario)
		)
	);
	const NIVEIS_CONVITE = $derived(
		NIVEIS_FALLBACK.filter((n) => n !== 'admin' || isAdmin(usuario))
	);
	const modulos = $derived(dados?.modulos ?? []);
	const membros = $derived(dados?.membros ?? []);

	function fetchCancelavel(sinal: AbortSignal): typeof fetch {
		return (input, init) => {
			if (sinal.aborted) return Promise.reject(new DOMException('Abortada', 'AbortError'));
			return new Promise<Response>((resolve, reject) => {
				const aoAbortar = (): void => reject(new DOMException('Abortada', 'AbortError'));
				sinal.addEventListener('abort', aoAbortar, { once: true });
				fetch(input, init).then(
					(res) => {
						sinal.removeEventListener('abort', aoAbortar);
						resolve(res);
					},
					(err: unknown) => {
						sinal.removeEventListener('abort', aoAbortar);
						reject(err);
					}
				);
			});
		};
	}

	$effect(() => {
		void recarregar;
		const ctrl = new AbortController();
		const sinal = ctrl.signal;
		carregando = true;
		erro = null;

		void getNiveis(fetchCancelavel(sinal))
			.then((res) => {
				if (!sinal.aborted) dados = res;
			})
			.catch((err: unknown) => {
				if (sinal.aborted) return;
				erro = err instanceof Error ? err.message : 'Não foi possível carregar os níveis';
			})
			.finally(() => {
				if (!sinal.aborted) carregando = false;
			});

		return () => ctrl.abort();
	});

	function tentarNovamente(): void {
		recarregar += 1;
	}

	function celula(modulo: string, nivel: Nivel): string {
		const valor = dados?.matriz?.[modulo]?.[nivel];
		if (valor === undefined || valor === null || String(valor).trim() === '') return '—';
		return String(valor);
	}

	async function trocarNivel(membroId: string, nome: string, novo: Nivel): Promise<void> {
		if (trocandoId) return;
		trocandoId = membroId;
		try {
			await alterarNivel(membroId, novo);
			toasts.success(`Nível de ${nome} atualizado.`);
			recarregar += 1;
		} catch (err) {
			toasts.danger(toUserMessage(err).message);
		} finally {
			trocandoId = null;
		}
	}

	// ---- Modal convidar ----

	let modalConvidar = $state(false);
	let nomeConvite = $state('');
	let emailConvite = $state('');
	let nivelConvite = $state<Nivel | ''>('');
	let errosConvite = $state<Record<string, string>>({});
	let convidando = $state(false);

	function abrirConvidar(): void {
		nomeConvite = '';
		emailConvite = '';
		nivelConvite = '';
		errosConvite = {};
		modalConvidar = true;
	}

	async function salvarConvite(): Promise<void> {
		if (convidando) return;
		const novos: Record<string, string> = {};
		if (!nomeConvite.trim()) novos['nome'] = 'Informe o nome.';
		if (!emailConvite.trim()) novos['email'] = 'Informe o e-mail.';
		else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(emailConvite.trim()))
			novos['email'] = 'Informe um e-mail válido.';
		if (!nivelConvite) novos['nivel'] = 'Selecione o nível.';
		errosConvite = novos;
		if (Object.keys(novos).length > 0) return;
		convidando = true;
		try {
			await convidarUsuario({
				name: nomeConvite.trim(),
				email: emailConvite.trim(),
				nivel: nivelConvite as Nivel
			});
			toasts.success(`Convite enviado para ${nomeConvite.trim()}.`);
			modalConvidar = false;
			recarregar += 1;
		} catch (err) {
			toasts.danger(toUserMessage(err).message);
		} finally {
			convidando = false;
		}
	}

	const inputCls =
		'w-full rounded-md border border-border bg-elevated px-3 py-2.5 text-sm text-ink placeholder:text-muted/60 focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/30 transition';
	const inputErroCls = 'border-danger/60 focus:border-danger focus:ring-danger/30';
	const labelCls = 'mb-1 block text-xs font-medium text-muted';
	const btnPrimario =
		'inline-flex items-center gap-1.5 rounded-md bg-brand px-3.5 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi disabled:opacity-50';
</script>

<svelte:head>
	<title>Níveis & acesso — Pessoas — FabLab</title>
</svelte:head>

<div class="space-y-4">
	<PageHeader
		title="Níveis & acesso"
		subtitle="Matriz de permissões e gestão de níveis dos membros."
	>
		{#snippet children()}
			<StatusBadge label="Restrito ao Admin" color="warn" />
			<button
				type="button"
				data-testid="nivel-convidar"
				onclick={abrirConvidar}
				aria-label="Convidar novo usuário"
				class={btnPrimario}
			>
				<Icon name="plus" class="h-4 w-4" /> Novo usuário
			</button>
		{/snippet}
	</PageHeader>

	<div
		role="note"
		aria-label="Aviso sobre autoridade dos níveis"
		class="flex items-start gap-3 rounded-xl border border-brand/30 bg-brand/10 px-4 py-3"
	>
		<Icon name="info" class="mt-0.5 h-5 w-5 shrink-0 text-brandhi" />
		<p class="text-sm text-ink">
			O nível de acesso é autoritativo no backend: a matriz abaixo reflete as permissões
			servidas pelo serviço e as alterações são aplicadas no ato.
		</p>
	</div>

	{#if erro && !carregando}
		<ErrorBanner
			message="Não foi possível carregar os níveis"
			hint="Verifique sua conexão e tente novamente. Se persistir, contate o suporte."
			onRetry={tentarNovamente}
		/>
	{:else if carregando}
		<div class="rounded-xl border border-border bg-surface p-4">
			<TableSkeleton rows={5} columns={4} />
		</div>
		<div class="rounded-xl border border-border bg-surface p-4">
			<TableSkeleton rows={4} columns={4} />
		</div>
	{:else if dados}
		<section aria-label="Matriz de níveis por módulo">
			<div class="overflow-hidden rounded-xl border border-border bg-surface">
				<div class="border-b border-border px-4 py-3">
					<h2 class="text-sm font-semibold text-ink">Matriz de permissões</h2>
					<p class="mt-0.5 text-xs text-muted">
						Nível × módulo, conforme servido pelo backend.
					</p>
				</div>
				<div class="overflow-x-auto">
					<table class="w-full min-w-[720px] text-sm">
						<thead>
							<tr
								class="border-b border-border bg-elevated/50 text-left text-[11px] uppercase tracking-wide text-muted"
							>
								<th scope="col" class="px-4 py-2.5 font-medium">Módulo</th>
								{#each niveis as nivel (nivel)}
									{@const meta = nivelMeta(nivel)}
									<th scope="col" class="px-4 py-2.5 font-medium">{meta.label}</th>
								{/each}
							</tr>
						</thead>
						<tbody>
							{#each modulos as modulo (modulo)}
								<tr class="border-b border-border transition last:border-0 hover:bg-elevated/40">
									<th scope="row" class="px-4 py-3 text-left font-medium text-ink">
										{modulo}
									</th>
									{#each niveis as nivel (nivel)}
										<td class="px-4 py-3 text-sm text-muted">{celula(modulo, nivel)}</td>
									{/each}
								</tr>
							{/each}
						</tbody>
					</table>
				</div>
				{#if modulos.length === 0}
					<EmptyState
						icon="lock"
						title="Matriz indisponível"
						description="O backend não retornou módulos para a matriz de permissões."
					/>
				{/if}
			</div>
		</section>

		<section aria-label="Membros e níveis">
			<div class="overflow-hidden rounded-xl border border-border bg-surface">
				<div class="border-b border-border px-4 py-3">
					<h2 class="text-sm font-semibold text-ink">Membros</h2>
					<p class="mt-0.5 text-xs text-muted">
						Troque o nível pelo seletor; a alteração é aplicada de imediato.
					</p>
				</div>
				{#if membros.length === 0}
					<EmptyState
						icon="users"
						title="Nenhum membro listado"
						description="Ainda não há membros retornados para gestão de níveis."
					>
						{#snippet children()}
							<button type="button" onclick={abrirConvidar} class={btnPrimario}>
								<Icon name="plus" class="h-4 w-4" /> Novo usuário
							</button>
						{/snippet}
					</EmptyState>
				{:else}
					<div class="overflow-x-auto">
						<table class="w-full min-w-[720px] text-sm">
							<thead>
								<tr
									class="border-b border-border bg-elevated/50 text-left text-[11px] uppercase tracking-wide text-muted"
								>
									<th scope="col" class="px-4 py-2.5 font-medium">Pessoa</th>
									<th scope="col" class="px-4 py-2.5 font-medium">Nível atual</th>
									<th scope="col" class="px-4 py-2.5 font-medium">Grupo</th>
									<th scope="col" class="px-4 py-2.5 font-medium">Alterar nível</th>
								</tr>
							</thead>
							<tbody>
								{#each membros as membro (membro.id)}
									{@const meta = nivelMeta(membro.nivel)}
									<tr
										data-testid="nivel-membro"
										class="border-b border-border transition last:border-0 hover:bg-elevated/40"
									>
										<td class="px-4 py-3">
											<div class="flex min-w-0 items-center gap-3">
												<Avatar name={membro.name} size="sm" />
												<div class="min-w-0">
													<p class="truncate font-medium text-ink">{membro.name}</p>
													<p class="truncate font-mono text-xs text-muted tabular-nums">
														{membro.matricula}
													</p>
												</div>
											</div>
										</td>
										<td class="px-4 py-3">
											<StatusBadge label={meta.label} color={meta.color} />
										</td>
										<td class="px-4 py-3 text-muted">{membro.grupo ?? '—'}</td>
										<td class="px-4 py-3">
											<select
												value={membro.nivel}
												disabled={trocandoId === membro.id}
												onchange={(e) =>
													void trocarNivel(
														membro.id,
														membro.name,
														(e.currentTarget as HTMLSelectElement).value as Nivel
													)}
												aria-label={`Alterar nível de ${membro.name}`}
												class="rounded-md border border-border bg-elevated px-2.5 py-1.5 text-sm text-ink focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/30 disabled:opacity-50"
											>
												{#each niveis as nivel (nivel)}
													<option value={nivel}>{nivelMeta(nivel).label}</option>
												{/each}
											</select>
										</td>
									</tr>
								{/each}
							</tbody>
						</table>
					</div>
				{/if}
			</div>
		</section>
	{/if}
</div>

<Modal
	open={modalConvidar}
	title="Convidar usuário"
	subtitle="Envie um convite com o nível inicial de acesso."
	onClose={() => (modalConvidar = false)}
	width="sm"
>
	{#snippet children()}
		<div data-testid="modal-convidar" class="space-y-4">
			<div>
				<label for="convite-nome" class={labelCls}>
					Nome <span class="text-danger" aria-hidden="true">*</span>
				</label>
				<input
					id="convite-nome"
					type="text"
					bind:value={nomeConvite}
					placeholder="Ex.: Ana Souza"
					autocomplete="name"
					aria-invalid={!!errosConvite['nome']}
					class="{inputCls} {errosConvite['nome'] ? inputErroCls : ''}"
				/>
				{#if errosConvite['nome']}<p class="mt-1 text-xs text-danger">{errosConvite['nome']}</p>{/if}
			</div>
			<div>
				<label for="convite-email" class={labelCls}>
					E-mail <span class="text-danger" aria-hidden="true">*</span>
				</label>
				<input
					id="convite-email"
					type="email"
					bind:value={emailConvite}
					placeholder="Ex.: ana.souza@fablab.ifsc.edu.br"
					autocomplete="email"
					aria-invalid={!!errosConvite['email']}
					class="{inputCls} {errosConvite['email'] ? inputErroCls : ''}"
				/>
				{#if errosConvite['email']}<p class="mt-1 text-xs text-danger">{errosConvite['email']}</p>{/if}
			</div>
			<div>
				<label for="convite-nivel" class={labelCls}>
					Nível <span class="text-danger" aria-hidden="true">*</span>
				</label>
				<select
					id="convite-nivel"
					bind:value={nivelConvite}
					aria-invalid={!!errosConvite['nivel']}
					class="{inputCls} {errosConvite['nivel'] ? inputErroCls : ''}"
				>
					<option value="" disabled>Selecione…</option>
					{#each NIVEIS_CONVITE as nivel (nivel)}
						<option value={nivel}>{nivelMeta(nivel).label}</option>
					{/each}
				</select>
				{#if errosConvite['nivel']}<p class="mt-1 text-xs text-danger">{errosConvite['nivel']}</p>{/if}
			</div>
		</div>
	{/snippet}
	{#snippet footer()}
		<button
			type="button"
			onclick={() => (modalConvidar = false)}
			disabled={convidando}
			class="rounded-md border border-border bg-surface px-4 py-2 text-sm font-medium text-ink transition hover:bg-elevated disabled:opacity-50"
		>
			Cancelar
		</button>
		<button
			type="button"
			onclick={() => void salvarConvite()}
			disabled={convidando}
			aria-label="Enviar convite"
			class="rounded-md bg-brand px-4 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi disabled:opacity-50"
		>
			{convidando ? 'Enviando…' : 'Enviar convite'}
		</button>
	{/snippet}
</Modal>
