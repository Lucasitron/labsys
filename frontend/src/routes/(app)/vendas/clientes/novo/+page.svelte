<script lang="ts">
	import { goto } from '$app/navigation';
	import { page } from '$app/state';
	import { get } from 'svelte/store';
	import { auth } from '$lib/stores/auth';
	import type { CreateClientePayload, Tag, TipoPessoa } from '$lib/types/vendas';
	import type { Tone } from '$lib/types/stock';
	import { createCliente, createTag, getCliente, listTags, updateCliente } from '$lib/api/vendas/clientes';
	import { ApiError } from '$lib/api/client';
	import { canEditVendas } from '$lib/utils/permissions';
	import { toUserMessage } from '$lib/utils/errors';
	import { toasts } from '$lib/stores/toast';
	import PageHeader from '$lib/components/ui/PageHeader.svelte';
	import RadioCards from '$lib/components/ui/RadioCards.svelte';
	import Icon from '$lib/components/ui/Icon.svelte';

	const usuario = $derived(get(auth).user);

	const editarId = $derived(page.url.searchParams.get('editar') ?? '');
	const duplicarId = $derived(page.url.searchParams.get('duplicar') ?? '');
	const modoEdicao = $derived(editarId !== '');

	let tipo = $state<TipoPessoa>('pf');
	let nome = $state('');
	let documento = $state('');
	let nascimento = $state('');
	let email = $state('');
	let telefone = $state('');
	let cep = $state('');
	let rua = $state('');
	let numero = $state('');
	let complemento = $state('');
	let bairro = $state('');
	let cidade = $state('');
	let uf = $state('');
	let tagIds = $state<string[]>([]);
	let novaTag = $state('');
	let observacoes = $state('');

	let tagsDisponiveis = $state<Tag[]>([]);
	let carregandoOrigem = $state(false);
	let erroOrigem = $state<string | null>(null);
	let origemPronta = $state(false);
	let origemCreatedBy = $state<string | undefined>(undefined);

	let ocupado = $state(false);
	let tagCriando = $state(false);
	let cepBuscando = $state(false);
	let cepErro = $state<string | null>(null);
	let erroTopo = $state<string | null>(null);
	let erros = $state<Record<string, string>>({});

	let cepCtrl: AbortController | null = null;

	const TAG_TONES: Record<Tone, string> = {
		brand: 'border-brand/40 bg-brand/10 text-brandhi',
		success: 'border-success/40 bg-success/10 text-success',
		warn: 'border-warn/40 bg-warn/10 text-warn',
		danger: 'border-danger/40 bg-danger/10 text-danger',
		muted: 'border-border bg-elevated text-muted',
		ink: 'border-border bg-elevated text-ink'
	};

	const inputCls =
		'w-full rounded-md border border-border bg-elevated px-3 py-2.5 text-sm text-ink placeholder:text-muted/60 focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/30 transition';
	const inputErroCls = 'border-danger/60 focus:border-danger focus:ring-danger/30';
	const labelCls = 'mb-1 block text-xs font-medium text-muted';
	const cardCls = 'rounded-xl border border-border bg-surface p-5';

	function somenteDigitos(valor: string): string {
		return valor.replace(/\D/g, '');
	}

	function mascararDocumento(valor: string, pessoa: TipoPessoa): string {
		const d = somenteDigitos(valor).slice(0, pessoa === 'pf' ? 11 : 14);
		if (pessoa === 'pf') {
			return d
				.replace(/(\d{3})(\d)/, '$1.$2')
				.replace(/(\d{3})(\d)/, '$1.$2')
				.replace(/(\d{3})(\d{1,2})$/, '$1-$2');
		}
		return d
			.replace(/(\d{2})(\d)/, '$1.$2')
			.replace(/(\d{3})(\d)/, '$1.$2')
			.replace(/(\d{3})(\d)/, '$1/$2')
			.replace(/(\d{4})(\d{1,2})$/, '$1-$2');
	}

	function validarCPF(doc: string): boolean {
		const d = somenteDigitos(doc);
		if (d.length !== 11 || /^(\d)\1{10}$/.test(d)) return false;
		let soma = 0;
		for (let i = 0; i < 9; i++) soma += Number(d[i]) * (10 - i);
		let resto = (soma * 10) % 11;
		if (resto === 10) resto = 0;
		if (resto !== Number(d[9])) return false;
		soma = 0;
		for (let i = 0; i < 10; i++) soma += Number(d[i]) * (11 - i);
		resto = (soma * 10) % 11;
		if (resto === 10) resto = 0;
		return resto === Number(d[10]);
	}

	function validarCNPJ(doc: string): boolean {
		const d = somenteDigitos(doc);
		if (d.length !== 14 || /^(\d)\1{13}$/.test(d)) return false;
		const pesos1 = [5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2];
		const pesos2 = [6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2];
		let soma = 0;
		for (let i = 0; i < 12; i++) soma += Number(d[i]) * (pesos1[i] ?? 0);
		let resto = soma % 11;
		const dig1 = resto < 2 ? 0 : 11 - resto;
		if (dig1 !== Number(d[12])) return false;
		soma = 0;
		for (let i = 0; i < 13; i++) soma += Number(d[i]) * (pesos2[i] ?? 0);
		resto = soma % 11;
		const dig2 = resto < 2 ? 0 : 11 - resto;
		return dig2 === Number(d[13]);
	}

	function onDocumentoInput(event: Event): void {
		documento = mascararDocumento((event.currentTarget as HTMLInputElement).value, tipo);
	}

	function trocarTipo(novo: TipoPessoa): void {
		tipo = novo;
		documento = mascararDocumento(documento, novo);
		if (novo === 'pj') nascimento = '';
	}

	function mascararTelefone(valor: string): string {
		const d = somenteDigitos(valor).slice(0, 11);
		if (d.length <= 2) return d.length > 0 ? `(${d}` : '';
		if (d.length <= 6) return `(${d.slice(0, 2)}) ${d.slice(2)}`;
		if (d.length <= 10) return `(${d.slice(0, 2)}) ${d.slice(2, 6)}-${d.slice(6)}`;
		return `(${d.slice(0, 2)}) ${d.slice(2, 7)}-${d.slice(7)}`;
	}

	function onTelefoneInput(event: Event): void {
		telefone = mascararTelefone((event.currentTarget as HTMLInputElement).value);
	}

	function mascararCep(valor: string): string {
		const d = somenteDigitos(valor).slice(0, 8);
		return d.length > 5 ? `${d.slice(0, 5)}-${d.slice(5)}` : d;
	}

	function onCepInput(event: Event): void {
		cep = mascararCep((event.currentTarget as HTMLInputElement).value);
	}

	async function buscarCep(): Promise<void> {
		const d = somenteDigitos(cep);
		if (d.length !== 8) {
			cepErro = 'Informe um CEP com 8 dígitos.';
			return;
		}
		cepErro = null;
		cepBuscando = true;
		cepCtrl?.abort();
		const ctrl = new AbortController();
		cepCtrl = ctrl;
		const timerCep = setTimeout(() => ctrl.abort(), 8000);
		try {
			const res = await fetch(`https://viacep.com.br/ws/${d}/json/`, { signal: ctrl.signal });
			const dados = (await res.json()) as {
				erro?: boolean;
				logradouro?: string;
				bairro?: string;
				localidade?: string;
				uf?: string;
			};
			if (ctrl.signal.aborted) return;
			if (dados.erro) {
				cepErro = 'CEP não encontrado. Preencha o endereço manualmente.';
				return;
			}
			if (dados.logradouro) rua = dados.logradouro;
			if (dados.bairro) bairro = dados.bairro;
			if (dados.localidade) cidade = dados.localidade;
			if (dados.uf) uf = dados.uf;
		} catch {
			if (cepCtrl === ctrl)
				cepErro = ctrl.signal.aborted
					? 'Tempo esgotado ao buscar o CEP. Preencha manualmente.'
					: 'Não foi possível buscar o CEP. Preencha manualmente.';
		} finally {
			clearTimeout(timerCep);
			if (cepCtrl === ctrl) cepBuscando = false;
		}
	}

	function alternarTag(id: string): void {
		tagIds = tagIds.includes(id) ? tagIds.filter((t) => t !== id) : [...tagIds, id];
	}

	function validar(): boolean {
		const novos: Record<string, string> = {};
		if (!nome.trim()) novos['nome'] = 'Informe o nome ou a razão social.';
		const docValido = tipo === 'pf' ? validarCPF(documento) : validarCNPJ(documento);
		if (!somenteDigitos(documento)) novos['documento'] = `Informe o ${tipo === 'pf' ? 'CPF' : 'CNPJ'}.`;
		else if (!docValido) novos['documento'] = `${tipo === 'pf' ? 'CPF' : 'CNPJ'} inválido.`;
		if (!email.trim()) novos['email'] = 'Informe o e-mail.';
		else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim()))
			novos['email'] = 'Informe um e-mail válido.';
		if (somenteDigitos(telefone).length < 10) novos['telefone'] = 'Informe um telefone válido com DDD.';
		if (cep && somenteDigitos(cep).length !== 8) novos['cep'] = 'CEP deve ter 8 dígitos.';
		if (tipo === 'pf' && nascimento && Number.isNaN(new Date(`${nascimento}T12:00:00`).getTime()))
			novos['nascimento'] = 'Data inválida.';
		erros = novos;
		return Object.keys(novos).length === 0;
	}

	function montarEndereco(): string | undefined {
		const linhaRua = [rua.trim(), numero.trim() ? `nº ${numero.trim()}` : '']
			.filter(Boolean)
			.join(' ');
		const cidadeUf = [cidade.trim(), uf.trim()].filter(Boolean).join('/');
		const partes = [
			linhaRua,
			complemento.trim(),
			bairro.trim(),
			cidadeUf,
			somenteDigitos(cep) ? `CEP ${mascararCep(cep)}` : ''
		].filter(Boolean);
		const endereco = partes.join(', ');
		return endereco || undefined;
	}

	function limpar(): void {
		if (modoEdicao) return;
		nome = '';
		documento = '';
		nascimento = '';
		email = '';
		telefone = '';
		cep = '';
		rua = '';
		numero = '';
		complemento = '';
		bairro = '';
		cidade = '';
		uf = '';
		tagIds = [];
		novaTag = '';
		observacoes = '';
		erros = {};
		erroTopo = null;
		cepErro = null;
	}

	async function salvar(adicionarOutro: boolean): Promise<void> {
		if (ocupado) return;
		erroTopo = null;
		if (!validar()) {
			erroTopo = 'Verifique os campos destacados e tente novamente.';
			return;
		}
		if (modoEdicao && !canEditVendas(usuario, { createdBy: origemCreatedBy })) {
			toasts.warn('Sem permissão para esta ação.');
			return;
		}
		ocupado = true;
		try {
			const endereco = montarEndereco();
			// `nascimento` é coletado (mockup §8.3) mas ainda não consta no DTO
			// CreateClientePayload — segue como extensão para confirmação (D-2).
			const payload: CreateClientePayload & { nascimento?: string } = {
				tipoPessoa: tipo,
				nome: nome.trim(),
				documento: somenteDigitos(documento),
				email: email.trim(),
				telefone: telefone.trim(),
				...(endereco ? { endereco } : {}),
				...(tagIds.length > 0 ? { tagIds: [...tagIds] } : {}),
				...(tipo === 'pf' && nascimento ? { nascimento } : {}),
				...(observacoes.trim() ? { observacoes: observacoes.trim() } : {})
			};
			if (modoEdicao) {
				await updateCliente(editarId, payload);
				toasts.success('Cliente atualizado com sucesso.');
				void goto(`/vendas/clientes/${editarId}`);
			} else if (adicionarOutro) {
				await createCliente(payload);
				toasts.success('Cliente cadastrado. Preencha o próximo cadastro.');
				limpar();
			} else {
				const criado = await createCliente(payload);
				toasts.success('Cliente cadastrado com sucesso.');
				void goto(`/vendas/clientes/${criado.id}`);
			}
		} catch (err) {
			if (err instanceof ApiError && err.status === 409) {
				const codigo = `${err.code} ${err.message}`.toUpperCase();
				if (codigo.includes('EMAIL')) {
					erros = { ...erros, email: 'Este e-mail já está em uso por outro cliente.' };
				} else {
					erros = { ...erros, documento: 'Este documento já está cadastrado.' };
				}
				erroTopo = 'Registro duplicado. Ajuste o campo destacado.';
			} else if (err instanceof ApiError && err.status === 403) {
				toasts.warn('Sem permissão para esta ação.');
			} else {
				erroTopo = toUserMessage(err).message;
			}
		} finally {
			ocupado = false;
		}
	}

	async function criarNovaTag(): Promise<void> {
		const nomeTag = novaTag.trim();
		if (!nomeTag || tagCriando) return;
		tagCriando = true;
		try {
			const criada = await createTag({ nome: nomeTag, cor: 'brand' });
			tagsDisponiveis = [...tagsDisponiveis, criada];
			tagIds = [...tagIds, criada.id];
			novaTag = '';
			toasts.success(`Tag "${criada.nome}" criada.`);
		} catch (err) {
			toasts.danger(toUserMessage(err).message);
		} finally {
			tagCriando = false;
		}
	}

	function voltar(): void {
		if (modoEdicao) void goto(`/vendas/clientes/${editarId}`);
		else if (window.history.length > 1) window.history.back();
		else void goto('/vendas/clientes');
	}

	// ---- Origem: editar (?editar=) ou duplicar (?duplicar=) ----

	$effect(() => {
		const origemId = editarId || duplicarId;
		// Tags servidas para segmentação (lista + criação 🟡).
		const ctrlTags = new AbortController();
		listTags((input, init) => fetch(input, { ...init, signal: ctrlTags.signal }))
			.then((tags) => {
				if (!ctrlTags.signal.aborted) tagsDisponiveis = tags;
			})
			.catch(() => {});
		if (!origemId) {
			origemPronta = true;
			return () => ctrlTags.abort();
		}
		const ctrl = new AbortController();
		const sinal = ctrl.signal;
		carregandoOrigem = true;
		erroOrigem = null;
		getCliente(origemId, (input, init) => fetch(input, { ...init, signal: sinal }))
			.then((origem) => {
				if (sinal.aborted) return;
				if (editarId && !canEditVendas(usuario, { createdBy: origem.createdBy })) {
					toasts.warn('Você não tem permissão para editar este cliente.');
					void goto(`/vendas/clientes/${origemId}`);
					return;
				}
				tipo = origem.tipoPessoa;
				nome = editarId ? origem.nome : `${origem.nome} (cópia)`;
				documento = editarId ? mascararDocumento(origem.documento, origem.tipoPessoa) : '';
				email = editarId ? origem.email : '';
				telefone = editarId ? mascararTelefone(origem.telefone) : '';
				rua = editarId && origem.endereco ? origem.endereco : '';
				origemCreatedBy = origem.createdBy;
				tagIds = origem.tags.map((t) => t.id);
				origemPronta = true;
			})
			.catch((err: unknown) => {
				if (!sinal.aborted) erroOrigem = toUserMessage(err).message;
			})
			.finally(() => {
				if (!sinal.aborted) carregandoOrigem = false;
			});
		return () => {
			ctrl.abort();
			ctrlTags.abort();
		};
	});
</script>

<svelte:head>
	<title>{modoEdicao ? 'Editar cliente' : 'Novo cliente'} — Vendas — FabLab</title>
</svelte:head>

<div class="space-y-4">
	<button
		type="button"
		onclick={voltar}
		class="inline-flex items-center gap-1.5 text-xs font-medium text-muted transition-colors hover:text-ink"
	>
		<Icon name="arrow-left" class="h-3.5 w-3.5" />
		{modoEdicao ? 'Voltar ao cliente' : 'Voltar à lista'}
	</button>

	<PageHeader
		title={modoEdicao ? 'Editar cliente' : 'Novo cliente'}
		subtitle={duplicarId
			? 'Duplicando cadastro existente — ajuste os campos e salve.'
			: 'Preencha os dados para cadastrar um cliente.'}
	/>

	{#if erroTopo}
		<p role="alert" class="rounded-xl border border-danger/30 bg-danger/10 px-4 py-3 text-sm text-danger">
			{erroTopo}
		</p>
	{/if}

	{#if carregandoOrigem}
		<div class="{cardCls} space-y-3" aria-hidden="true">
			<div class="h-5 w-40 animate-pulse rounded bg-elevated"></div>
			<div class="h-10 w-full animate-pulse rounded-md bg-elevated"></div>
			<div class="h-10 w-full animate-pulse rounded-md bg-elevated"></div>
		</div>
	{:else if erroOrigem}
		<div class="{cardCls}">
			<p role="alert" class="text-sm text-danger">
				Não foi possível carregar os dados do cliente. {erroOrigem}
			</p>
			<button
				type="button"
				onclick={voltar}
				class="mt-3 rounded-md border border-border bg-elevated px-3 py-2 text-sm font-medium text-ink transition hover:border-brand/50 hover:text-brandhi"
			>
				Voltar
			</button>
		</div>
	{:else if origemPronta}
		<form novalidate onsubmit={(e) => e.preventDefault()} class="space-y-4">
			<!-- Identificação -->
			<section class={cardCls} aria-label="Identificação">
				<h2 class="text-sm font-semibold text-ink">Identificação</h2>
				<div class="mt-4 space-y-4">
					<RadioCards
						name="tipo-pessoa"
						options={[
							{ id: 'pf', label: 'Pessoa física', description: 'CPF', color: 'brand' },
							{ id: 'pj', label: 'Pessoa jurídica', description: 'CNPJ', color: 'success' }
						]}
						value={tipo}
						onChange={(v) => trocarTipo(v as TipoPessoa)}
					/>
					<div class="grid gap-4 sm:grid-cols-2">
						<div>
							<label for="cliente-nome" class={labelCls}>
								{tipo === 'pf' ? 'Nome completo' : 'Razão social'} <span class="text-danger">*</span>
							</label>
							<input
								id="cliente-nome"
								data-testid="cliente-nome"
								type="text"
								bind:value={nome}
								placeholder={tipo === 'pf' ? 'Ex.: Maria Oliveira' : 'Ex.: Ateliê 3D LTDA'}
								disabled={ocupado}
								autocomplete="off"
								aria-invalid={erros['nome'] ? 'true' : undefined}
								class="{inputCls} {erros['nome'] ? inputErroCls : ''}"
							/>
							{#if erros['nome']}
								<p role="alert" class="mt-1 text-xs text-danger">{erros['nome']}</p>
							{/if}
						</div>
						<div>
							<label for="cliente-doc" class={labelCls}>
								{tipo === 'pf' ? 'CPF' : 'CNPJ'} <span class="text-danger">*</span>
							</label>
							<input
								id="cliente-doc"
								data-testid="cliente-cpf-cnpj"
								type="text"
								value={documento}
								oninput={onDocumentoInput}
								placeholder={tipo === 'pf' ? '000.000.000-00' : '00.000.000/0000-00'}
								disabled={ocupado}
								inputmode="numeric"
								autocomplete="off"
								aria-invalid={erros['documento'] ? 'true' : undefined}
								class="{inputCls} font-mono {erros['documento'] ? inputErroCls : ''}"
							/>
							{#if erros['documento']}
								<p role="alert" class="mt-1 text-xs text-danger">{erros['documento']}</p>
							{/if}
						</div>
					</div>
					{#if tipo === 'pf'}
						<div class="grid gap-4 sm:grid-cols-2">
							<div>
								<label for="cliente-nascimento" class={labelCls}>Data de nascimento</label>
								<input
									id="cliente-nascimento"
									type="date"
									bind:value={nascimento}
									disabled={ocupado}
									aria-invalid={erros['nascimento'] ? 'true' : undefined}
									class="{inputCls} {erros['nascimento'] ? inputErroCls : ''}"
								/>
								{#if erros['nascimento']}
									<p role="alert" class="mt-1 text-xs text-danger">{erros['nascimento']}</p>
								{/if}
							</div>
						</div>
					{/if}
				</div>
			</section>

			<!-- Contato -->
			<section class={cardCls} aria-label="Contato">
				<h2 class="text-sm font-semibold text-ink">Contato</h2>
				<div class="mt-4 grid gap-4 sm:grid-cols-2">
					<div>
						<label for="cliente-email" class={labelCls}>
							E-mail <span class="text-danger">*</span>
						</label>
						<input
							id="cliente-email"
							type="email"
							bind:value={email}
							placeholder="cliente@email.com"
							disabled={ocupado}
							autocomplete="off"
							aria-invalid={erros['email'] ? 'true' : undefined}
							class="{inputCls} {erros['email'] ? inputErroCls : ''}"
						/>
						{#if erros['email']}
							<p role="alert" class="mt-1 text-xs text-danger">{erros['email']}</p>
						{/if}
					</div>
					<div>
						<label for="cliente-tel" class={labelCls}>
							Telefone <span class="text-danger">*</span>
						</label>
						<input
							id="cliente-tel"
							type="tel"
							value={telefone}
							oninput={onTelefoneInput}
							placeholder="(48) 99999-0000"
							disabled={ocupado}
							inputmode="tel"
							autocomplete="off"
							aria-invalid={erros['telefone'] ? 'true' : undefined}
							class="{inputCls} font-mono {erros['telefone'] ? inputErroCls : ''}"
						/>
						{#if erros['telefone']}
							<p role="alert" class="mt-1 text-xs text-danger">{erros['telefone']}</p>
						{/if}
					</div>
				</div>
				<div class="mt-4 grid gap-4 sm:grid-cols-2">
					<div>
						<label for="cliente-cep" class={labelCls}>CEP</label>
						<div class="flex gap-2">
							<input
								id="cliente-cep"
								type="text"
								value={cep}
								oninput={onCepInput}
								placeholder="00000-000"
								disabled={ocupado || cepBuscando}
								inputmode="numeric"
								autocomplete="off"
								aria-invalid={erros['cep'] ? 'true' : undefined}
								class="{inputCls} font-mono {erros['cep'] ? inputErroCls : ''}"
							/>
							<button
								type="button"
								onclick={() => void buscarCep()}
								disabled={ocupado || cepBuscando}
								class="shrink-0 rounded-md border border-border bg-elevated px-3 py-2 text-sm font-medium text-ink transition hover:border-brand/50 hover:text-brandhi disabled:opacity-50"
							>
								{cepBuscando ? 'Buscando…' : 'Buscar'}
							</button>
						</div>
						{#if erros['cep']}
							<p role="alert" class="mt-1 text-xs text-danger">{erros['cep']}</p>
						{:else if cepErro}
							<p role="alert" class="mt-1 text-xs text-warn">{cepErro}</p>
						{:else}
							<p class="mt-1 text-xs text-muted">
								CEP usado apenas para autocompletar o endereço (ViaCEP).
							</p>
						{/if}
					</div>
					<div>
						<label for="cliente-rua" class={labelCls}>Rua / Avenida</label>
						<input
							id="cliente-rua"
							type="text"
							bind:value={rua}
							placeholder="Ex.: Rua das Palmeiras"
							disabled={ocupado}
							autocomplete="off"
							class={inputCls}
						/>
					</div>
					<div>
						<label for="cliente-numero" class={labelCls}>Número</label>
						<input
							id="cliente-numero"
							type="text"
							bind:value={numero}
							placeholder="Ex.: 123"
							disabled={ocupado}
							autocomplete="off"
							class={inputCls}
						/>
					</div>
					<div>
						<label for="cliente-compl" class={labelCls}>Complemento</label>
						<input
							id="cliente-compl"
							type="text"
							bind:value={complemento}
							placeholder="Ex.: Sala 2"
							disabled={ocupado}
							autocomplete="off"
							class={inputCls}
						/>
					</div>
					<div>
						<label for="cliente-bairro" class={labelCls}>Bairro</label>
						<input
							id="cliente-bairro"
							type="text"
							bind:value={bairro}
							disabled={ocupado}
							autocomplete="off"
							class={inputCls}
						/>
					</div>
					<div class="grid grid-cols-2 gap-4">
						<div>
							<label for="cliente-cidade" class={labelCls}>Cidade</label>
							<input
								id="cliente-cidade"
								type="text"
								bind:value={cidade}
								disabled={ocupado}
								autocomplete="off"
								class={inputCls}
							/>
						</div>
						<div>
							<label for="cliente-uf" class={labelCls}>UF</label>
							<input
								id="cliente-uf"
								type="text"
								bind:value={uf}
								maxlength={2}
								placeholder="SC"
								disabled={ocupado}
								autocomplete="off"
								class="{inputCls} uppercase"
							/>
						</div>
					</div>
				</div>
			</section>

			<!-- Segmentação -->
			<section class={cardCls} aria-label="Segmentação">
				<h2 class="text-sm font-semibold text-ink">Segmentação</h2>
				<div class="mt-4 space-y-4">
					<div>
						<p class={labelCls} id="tags-label">Tags</p>
						<div class="flex flex-wrap gap-1.5" role="group" aria-labelledby="tags-label">
							{#each tagsDisponiveis as tag (tag.id)}
								{@const ativa = tagIds.includes(tag.id)}
								<button
									type="button"
									onclick={() => alternarTag(tag.id)}
									aria-pressed={ativa}
									disabled={ocupado}
									class="rounded-full border px-3 py-1.5 text-xs font-medium transition {TAG_TONES[tag.cor]} {ativa
										? 'ring-2 ring-current'
										: 'opacity-60 hover:opacity-100'} disabled:opacity-50"
								>
									{tag.nome}
								</button>
							{:else}
								<p class="text-xs text-muted">Nenhuma tag cadastrada. Crie a primeira abaixo.</p>
							{/each}
						</div>
						<div class="mt-3 flex gap-2">
							<input
								id="nova-tag"
								type="text"
								bind:value={novaTag}
								placeholder="Nova tag (ex.: VIP)"
								disabled={ocupado || tagCriando}
								aria-label="Nome da nova tag"
								class={inputCls}
							/>
							<button
								type="button"
								onclick={() => void criarNovaTag()}
								disabled={ocupado || tagCriando || !novaTag.trim()}
								class="shrink-0 rounded-md border border-border bg-elevated px-3 py-2 text-sm font-medium text-ink transition hover:border-brand/50 hover:text-brandhi disabled:opacity-50"
							>
								{tagCriando ? 'Criando…' : 'Criar tag'}
							</button>
						</div>
					</div>
					<div>
						<label for="cliente-obs" class={labelCls}>Observações</label>
						<textarea
							id="cliente-obs"
							bind:value={observacoes}
							rows={3}
							placeholder="Informações adicionais sobre o cliente"
							disabled={ocupado}
							class="{inputCls} resize-y"
						></textarea>
					</div>
				</div>
			</section>

			<!-- Rodapé -->
			<div class="flex flex-wrap items-center justify-end gap-2">
				<button
					type="button"
					onclick={voltar}
					disabled={ocupado}
					class="rounded-md border border-border bg-surface px-4 py-2 text-sm font-medium text-ink transition hover:bg-elevated disabled:opacity-50"
				>
					Cancelar
				</button>
				{#if !modoEdicao}
					<button
						type="button"
						onclick={() => void salvar(true)}
						disabled={ocupado}
						class="rounded-md border border-brand/40 bg-brand/10 px-4 py-2 text-sm font-medium text-brandhi transition hover:bg-brand/20 disabled:opacity-50"
					>
						{ocupado ? 'Salvando…' : 'Salvar e adicionar outro'}
					</button>
				{/if}
				<button
					type="button"
					data-testid="submit-cliente"
					onclick={() => void salvar(false)}
					disabled={ocupado}
					class="rounded-md bg-brand px-4 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi disabled:opacity-50"
				>
					{ocupado ? 'Salvando…' : modoEdicao ? 'Salvar alterações' : 'Salvar cliente'}
				</button>
			</div>
		</form>
	{/if}
</div>
