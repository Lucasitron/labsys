<script lang="ts">
	import { goto } from '$app/navigation';
	import { page } from '$app/state';
	import { get } from 'svelte/store';
	import type { PageProps } from './$types';
	import { auth } from '$lib/stores/auth';
	import {
		createApontamento,
		getHorasDisponiveis,
		listApontamentos,
		rejeitarHoras,
		validarApontamento
	} from '$lib/api/rh/horas';
	import type { PersonOption } from '$lib/api/rh';
	import type { ExtratoMensalHoras, HoraApontamento, HoraStatus, HoraTipo } from '$lib/types/rh';
	import { formatNumber } from '$lib/utils/format';
	import { toUserMessage } from '$lib/utils/errors';
	import { horaStatusMeta } from '$lib/utils/rh-status';
	import { toasts } from '$lib/stores/toast';
	import PageHeader from '$lib/components/ui/PageHeader.svelte';
	import Tabs from '$lib/components/ui/Tabs.svelte';
	import TypeBadge from '$lib/components/ui/TypeBadge.svelte';
	import StatusBadge from '$lib/components/ui/StatusBadge.svelte';
	import Avatar from '$lib/components/ui/Avatar.svelte';
	import Modal from '$lib/components/ui/Modal.svelte';
	import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte';
	import EmptyState from '$lib/components/ui/EmptyState.svelte';
	import TableSkeleton from '$lib/components/ui/TableSkeleton.svelte';
	import Icon from '$lib/components/ui/Icon.svelte';
	import PeoplePicker from '$lib/components/estoque/PeoplePicker.svelte';

	let { data }: PageProps = $props();

	const canEdit = $derived(data.canEdit ?? false);
	const usuario = $derived(get(auth).user);
	const ehEstagiario = $derived(usuario?.role === 3);
	const podeValidar = $derived(canEdit && !ehEstagiario);
	const podeRegistrar = $derived(canEdit || ehEstagiario);
	// Estagiário enxerga somente os próprios apontamentos (D-7: token → pessoa).
	const filtroPessoa = $derived(ehEstagiario && usuario ? usuario.id : undefined);

	const ABAS: HoraStatus[] = ['pendente', 'validada', 'rejeitada'];
	const ABAS_LABEL: Record<HoraStatus, string> = {
		pendente: 'Pendentes',
		validada: 'Validadas',
		rejeitada: 'Rejeitadas'
	};

	const query = $derived(page.url.searchParams);

	const abaAtiva = $derived.by((): HoraStatus => {
		const bruto = (query.get('status') ?? '').trim();
		return (ABAS as string[]).includes(bruto) ? (bruto as HoraStatus) : 'pendente';
	});

	const periodo = $derived.by((): string => {
		const bruto = (query.get('periodo') ?? '').trim();
		return /^\d{4}-\d{2}$/.test(bruto) ? bruto : '';
	});

	let apontamentos = $state<HoraApontamento[]>([]);
	let contagens = $state({ pendentes: 0, validadas: 0, rejeitadas: 0 });
	let extrato = $state<ExtratoMensalHoras | null>(null);
	let carregando = $state(true);
	let erro = $state<string | null>(null);
	let erroExtrato = $state<string | null>(null);
	let recarregar = $state(0);
	let mutando = $state(false);

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
		const status = abaAtiva;
		const per = periodo;
		const pessoaId = filtroPessoa;
		void recarregar;

		const ctrl = new AbortController();
		const sinal = ctrl.signal;
		const busca = fetchCancelavel(sinal);
		carregando = true;
		erro = null;
		erroExtrato = null;

		void listApontamentos(
			{
				status,
				...(per ? { periodo: per } : {}),
				...(pessoaId ? { personId: pessoaId } : {})
			},
			busca
		)
			.then((res) => {
				if (sinal.aborted) return;
				apontamentos = res.hours;
				contagens = res.counts;
			})
			.catch((err: unknown) => {
				if (sinal.aborted) return;
				erro = err instanceof Error ? err.message : 'Não foi possível carregar as horas';
			})
			.finally(() => {
				if (!sinal.aborted) carregando = false;
			});

		// KPIs vêm do extrato servido pelo backend (R-5: nunca somar no client).
		void getHorasDisponiveis(busca)
			.then((res) => {
				if (!sinal.aborted) extrato = res;
			})
			.catch((err: unknown) => {
				if (sinal.aborted) return;
				erroExtrato = err instanceof Error ? err.message : 'Não foi possível carregar o extrato';
			});

		return () => ctrl.abort();
	});

	function navegar(opcoes: { status?: HoraStatus; periodo?: string }): void {
		const status = opcoes.status ?? abaAtiva;
		const per = opcoes.periodo ?? periodo;
		const params = new URLSearchParams();
		params.set('status', status);
		if (per) params.set('periodo', per);
		void goto(`/pessoas/registro-horas?${params.toString()}`, { keepFocus: true });
	}

	function tentarNovamente(): void {
		recarregar += 1;
	}

	function atualizar(): void {
		recarregar += 1;
	}

	const abas = $derived(
		ABAS.map((id) => ({
			id,
			label: ABAS_LABEL[id],
			count: id === 'pendente' ? contagens.pendentes : id === 'validada' ? contagens.validadas : contagens.rejeitadas
		}))
	);

	function tipoInfo(tipo: HoraTipo): { label: string; tone: 'brand' | 'success' } {
		return tipo === 'encomenda' ? { label: 'Encomenda', tone: 'brand' } : { label: 'Projeto', tone: 'success' };
	}

	function formatarDataCurta(iso: string): string {
		const data = new Date(`${iso}T12:00:00`);
		if (Number.isNaN(data.getTime())) return iso;
		return new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: '2-digit' }).format(data);
	}

	function formatarPeriodo(per: string): string {
		const data = new Date(`${per}-02T12:00:00`);
		if (Number.isNaN(data.getTime())) return per;
		const texto = new Intl.DateTimeFormat('pt-BR', { month: 'long', year: 'numeric' }).format(data);
		return texto.charAt(0).toUpperCase() + texto.slice(1);
	}

	async function validar(id: string, nome: string): Promise<void> {
		if (mutando) return;
		mutando = true;
		try {
			await validarApontamento(id);
			toasts.success(`Horas de ${nome} validadas.`);
			atualizar();
		} catch (err) {
			toasts.danger(toUserMessage(err).message);
		} finally {
			mutando = false;
		}
	}

	// ---- Modal rejeitar ----

	let alvoRejeicao = $state<HoraApontamento | null>(null);
	let motivo = $state('');
	let erroMotivo = $state<string | null>(null);

	function abrirRejeitar(ap: HoraApontamento): void {
		alvoRejeicao = ap;
		motivo = '';
		erroMotivo = null;
	}

	async function confirmarRejeicao(): Promise<void> {
		if (mutando || !alvoRejeicao) return;
		if (!motivo.trim()) {
			erroMotivo = 'Informe o motivo da rejeição.';
			return;
		}
		mutando = true;
		try {
			await rejeitarHoras(alvoRejeicao.id, { reason: motivo.trim() });
			toasts.success(`Horas de ${alvoRejeicao.person.name} rejeitadas.`);
			alvoRejeicao = null;
			atualizar();
		} catch (err) {
			erroMotivo = toUserMessage(err).message;
		} finally {
			mutando = false;
		}
	}

	// ---- Modal registrar ----

	let modalRegistrar = $state(false);
	let pessoaSel = $state<PersonOption | null>(null);
	let dataReg = $state('');
	let inicioReg = $state('');
	let fimReg = $state('');
	let tipoReg = $state<HoraTipo>('encomenda');
	let refReg = $state('');
	let obsReg = $state('');
	let errosReg = $state<Record<string, string>>({});
	let chavePessoa = $state(0);

	const duracaoHoras = $derived.by((): number => {
		const ini = /^(\d{2}):(\d{2})$/.exec(inicioReg);
		const fim = /^(\d{2}):(\d{2})$/.exec(fimReg);
		if (!ini || !fim) return 0;
		const diff = Number(fim[1]) * 60 + Number(fim[2]) - (Number(ini[1]) * 60 + Number(ini[2]));
		if (diff <= 0) return 0;
		return Math.round((diff / 60) * 100) / 100;
	});

	function abrirRegistrar(): void {
		pessoaSel = null;
		dataReg = '';
		inicioReg = '';
		fimReg = '';
		tipoReg = 'encomenda';
		refReg = '';
		obsReg = '';
		errosReg = {};
		chavePessoa += 1;
		modalRegistrar = true;
	}

	async function salvarRegistro(): Promise<void> {
		if (mutando) return;
		const pessoaId = ehEstagiario && usuario ? usuario.id : pessoaSel?.id;
		const novos: Record<string, string> = {};
		if (!pessoaId) novos['pessoa'] = 'Selecione a pessoa.';
		if (!dataReg) novos['data'] = 'Informe a data.';
		if (!inicioReg) novos['inicio'] = 'Informe o início.';
		if (!fimReg) novos['fim'] = 'Informe o fim.';
		else if (duracaoHoras <= 0) novos['fim'] = 'O fim deve ser após o início.';
		errosReg = novos;
		if (Object.keys(novos).length > 0 || !pessoaId) return;
		mutando = true;
		try {
			await createApontamento({
				personId: pessoaId,
				date: dataReg,
				startTime: inicioReg,
				endTime: fimReg,
				hours: duracaoHoras,
				type: tipoReg,
				...(refReg.trim()
					? tipoReg === 'encomenda'
						? { orderId: refReg.trim() }
						: { projectId: refReg.trim() }
					: {}),
				...(obsReg.trim() ? { observation: obsReg.trim() } : {})
			});
			toasts.success('Horas registradas com sucesso.');
			modalRegistrar = false;
			atualizar();
		} catch (err) {
			toasts.danger(toUserMessage(err).message);
		} finally {
			mutando = false;
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
	<title>Registro de horas — Pessoas — FabLab</title>
</svelte:head>

<div class="space-y-4">
	<PageHeader title="Registro de horas" subtitle="Apontamentos, validação e extrato mensal.">
		{#snippet children()}
			{#if podeRegistrar}
				<button
					type="button"
					onclick={abrirRegistrar}
					class={btnPrimario}
					aria-label="Registrar horas"
				>
					<Icon name="plus" class="h-4 w-4" /> Registrar horas
				</button>
			{/if}
		{/snippet}
	</PageHeader>

	<section aria-label="Extrato mensal">
		{#if carregando && !extrato}
			<div class="grid grid-cols-2 gap-3 lg:grid-cols-4">
				{#each [0, 1, 2, 3] as i (i)}
					<div class="animate-pulse rounded-xl border border-border bg-elevated p-4" aria-hidden="true">
						<div class="h-3 w-24 rounded bg-muted/20"></div>
						<div class="mt-2 h-7 w-14 rounded bg-muted/20"></div>
					</div>
				{/each}
			</div>
		{:else if extrato}
			<div class="grid grid-cols-2 gap-3 lg:grid-cols-4">
				<div class="rounded-xl border border-border bg-elevated p-4">
					<p class="text-xs text-muted">Horas validadas</p>
					<p class="mt-1 font-mono text-2xl font-semibold text-ink tabular-nums">
						{formatNumber(extrato.validadas)}h
					</p>
				</div>
				<div class="rounded-xl border border-border bg-elevated p-4">
					<p class="text-xs text-muted">Horas pendentes</p>
					<p class="mt-1 font-mono text-2xl font-semibold text-ink tabular-nums">
						{formatNumber(extrato.pendentes)}h
					</p>
				</div>
				<div class="rounded-xl border border-border bg-elevated p-4">
					<p class="text-xs text-muted">Horas rejeitadas</p>
					<p class="mt-1 font-mono text-2xl font-semibold text-ink tabular-nums">
						{formatNumber(extrato.rejeitadas)}h
					</p>
				</div>
				<div class="rounded-xl border border-border bg-elevated p-4">
					<p class="text-xs text-muted">Período do extrato</p>
					<p class="mt-1 font-mono text-lg font-semibold text-ink">
						{formatarPeriodo(extrato.periodo)}
					</p>
				</div>
			</div>
		{/if}
		{#if erroExtrato}
			<div
				role="alert"
				class="mt-2 flex flex-wrap items-center gap-2 rounded-xl border border-warn/30 bg-warn/10 px-4 py-2.5"
			>
				<p class="flex-1 text-xs text-warn">Extrato indisponível: {erroExtrato}</p>
				<button
					type="button"
					onclick={tentarNovamente}
					class="rounded-md border border-warn/30 px-2.5 py-1 text-xs font-medium text-warn transition hover:bg-warn/15"
				>
					Tentar novamente
				</button>
			</div>
		{/if}
	</section>

	<div class="flex flex-wrap items-center gap-2">
		<Tabs tabs={abas} active={abaAtiva} onChange={(id) => navegar({ status: id as HoraStatus })} />
		<span class="flex-1"></span>
		<label class="flex items-center gap-2 text-xs text-muted">
			Período
			<input
				type="month"
				value={periodo}
				onchange={(e) => navegar({ periodo: (e.currentTarget as HTMLInputElement).value })}
				aria-label="Filtrar por período"
				class="rounded-md border border-border bg-surface px-2.5 py-1.5 font-mono text-xs text-ink tabular-nums focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/30"
			/>
		</label>
		{#if periodo}
			<button
				type="button"
				onclick={() => navegar({ periodo: '' })}
				aria-label="Limpar período"
				class="rounded-md p-1.5 text-muted transition hover:text-ink"
			>
				<Icon name="x-mark" class="h-4 w-4" />
			</button>
		{/if}
	</div>

	{#if erro && !carregando}
		<ErrorBanner message="Não foi possível carregar as horas" hint={erro} onRetry={tentarNovamente} />
	{:else if carregando}
		<div class="rounded-xl border border-border bg-surface p-4">
			<TableSkeleton rows={5} columns={5} />
		</div>
	{:else if apontamentos.length === 0}
		<div class="rounded-xl border border-border bg-surface">
			<EmptyState
				icon="clock"
				title={`Nenhum apontamento ${ABAS_LABEL[abaAtiva].toLowerCase()}`}
				description={periodo
					? `Sem registros em ${formatarPeriodo(periodo)}.`
					: 'Registre as primeiras horas para começar.'}
			>
				{#snippet children()}
					{#if podeRegistrar}
						<button type="button" onclick={abrirRegistrar} class={btnPrimario}>
							<Icon name="plus" class="h-4 w-4" /> Registrar horas
						</button>
					{/if}
				{/snippet}
			</EmptyState>
		</div>
	{:else}
		<section aria-label={`Apontamentos ${ABAS_LABEL[abaAtiva].toLowerCase()}`} data-testid="hora-tab-{abaAtiva}">
			<div class="overflow-hidden rounded-xl border border-border bg-surface">
				<div class="overflow-x-auto">
					<table class="w-full min-w-[820px] text-sm">
						<thead>
							<tr
								class="border-b border-border bg-elevated/50 text-left text-[11px] uppercase tracking-wide text-muted"
							>
								{#if !ehEstagiario}
									<th class="px-4 py-2.5 font-medium">Pessoa</th>
								{/if}
								<th class="px-4 py-2.5 font-medium">Data</th>
								<th class="px-4 py-2.5 font-medium">Horário</th>
								<th class="px-4 py-2.5 font-medium">Horas</th>
								<th class="px-4 py-2.5 font-medium">Tipo</th>
								<th class="px-4 py-2.5 font-medium">Referência</th>
								<th class="px-4 py-2.5 font-medium">Status</th>
								{#if podeValidar && abaAtiva === 'pendente'}
									<th class="w-40 px-4 py-2.5 text-right font-medium">
										<span class="sr-only">Ações</span>
									</th>
								{/if}
							</tr>
						</thead>
						<tbody>
							{#each apontamentos as ap (ap.id)}
								{@const st = horaStatusMeta(ap.status)}
								{@const tp = tipoInfo(ap.type)}
								<tr class="border-b border-border transition last:border-0 hover:bg-elevated/40">
									{#if !ehEstagiario}
										<td class="px-4 py-3">
											<div class="flex items-center gap-2.5">
												<Avatar name={ap.person.name} initialsOverride={ap.person.initials} size="sm" />
												<span class="font-medium text-ink">{ap.person.name}</span>
											</div>
										</td>
									{/if}
									<td class="px-4 py-3 font-mono text-xs text-muted tabular-nums">
										{formatarDataCurta(ap.date)}
									</td>
									<td class="px-4 py-3 font-mono text-xs text-muted tabular-nums">
										{ap.startTime ?? '—'}–{ap.endTime ?? '—'}
									</td>
									<td class="px-4 py-3 font-mono text-sm text-ink tabular-nums">
										{formatNumber(ap.hours)}h
									</td>
									<td class="px-4 py-3">
										<TypeBadge tone={tp.tone} label={tp.label} />
									</td>
									<td class="px-4 py-3 font-mono text-xs text-muted">{ap.ref ?? '—'}</td>
									<td class="px-4 py-3">
										<StatusBadge label={st.label} color={st.color} />
										{#if ap.status === 'rejeitada' && ap.reason}
											<p class="mt-1 max-w-52 text-xs text-muted">{ap.reason}</p>
										{/if}
									</td>
									{#if podeValidar && abaAtiva === 'pendente'}
										<td class="px-4 py-3">
											<div class="flex items-center justify-end gap-1.5">
												<button
													type="button"
													data-testid="hora-validar"
													disabled={mutando}
													onclick={() => void validar(ap.id, ap.person.name)}
													aria-label={`Validar horas de ${ap.person.name}`}
													class="rounded-md border border-success/40 bg-success/10 px-2.5 py-1.5 text-xs font-medium text-success transition hover:bg-success/20 disabled:opacity-50"
												>
													Validar
												</button>
												<button
													type="button"
													data-testid="hora-rejeitar"
													disabled={mutando}
													onclick={() => abrirRejeitar(ap)}
													aria-label={`Rejeitar horas de ${ap.person.name}`}
													class="rounded-md border border-danger/40 bg-danger/10 px-2.5 py-1.5 text-xs font-medium text-danger transition hover:bg-danger/20 disabled:opacity-50"
												>
													Rejeitar
												</button>
											</div>
										</td>
									{/if}
								</tr>
							{/each}
						</tbody>
					</table>
				</div>
			</div>
		</section>
	{/if}
</div>

<!-- Modal: registrar horas -->
<Modal
	open={modalRegistrar}
	title="Registrar horas"
	subtitle="Apontamento de encomenda ou projeto."
	onClose={() => (modalRegistrar = false)}
>
	{#snippet children()}
		<div data-testid="modal-registrar-horas" class="space-y-4">
			<div>
				{#if ehEstagiario}
					<span class={labelCls}>Pessoa</span>
					<div
						class="flex items-center gap-2 rounded-md border border-border bg-elevated px-3 py-2.5"
					>
						<Avatar name={usuario?.name ?? ''} size="xs" />
						<span class="text-sm font-medium text-ink">{usuario?.name ?? '—'}</span>
					</div>
				{:else}
					<span id="reg-pessoa-label" class={labelCls}>
						Pessoa <span class="text-danger" aria-hidden="true">*</span>
					</span>
					{#key chavePessoa}
						<PeoplePicker
							selected={pessoaSel}
							onSelect={(p) => {
								pessoaSel = p;
								errosReg = { ...errosReg, pessoa: '' };
							}}
							placeholder="Buscar pessoa por nome…"
						/>
					{/key}
					{#if errosReg['pessoa']}<p class="mt-1 text-xs text-danger">{errosReg['pessoa']}</p>{/if}
				{/if}
			</div>
			<div>
				<label for="reg-data" class={labelCls}>
					Data <span class="text-danger" aria-hidden="true">*</span>
				</label>
				<input
					id="reg-data"
					type="date"
					bind:value={dataReg}
					aria-invalid={!!errosReg['data']}
					class="{inputCls} font-mono tabular-nums {errosReg['data'] ? inputErroCls : ''}"
				/>
				{#if errosReg['data']}<p class="mt-1 text-xs text-danger">{errosReg['data']}</p>{/if}
			</div>
			<div class="grid grid-cols-2 gap-4">
				<div>
					<label for="reg-inicio" class={labelCls}>
						Início <span class="text-danger" aria-hidden="true">*</span>
					</label>
					<input
						id="reg-inicio"
						type="time"
						bind:value={inicioReg}
						aria-invalid={!!errosReg['inicio']}
						class="{inputCls} font-mono tabular-nums {errosReg['inicio'] ? inputErroCls : ''}"
					/>
					{#if errosReg['inicio']}<p class="mt-1 text-xs text-danger">{errosReg['inicio']}</p>{/if}
				</div>
				<div>
					<label for="reg-fim" class={labelCls}>
						Fim <span class="text-danger" aria-hidden="true">*</span>
					</label>
					<input
						id="reg-fim"
						type="time"
						bind:value={fimReg}
						aria-invalid={!!errosReg['fim']}
						class="{inputCls} font-mono tabular-nums {errosReg['fim'] ? inputErroCls : ''}"
					/>
					{#if errosReg['fim']}<p class="mt-1 text-xs text-danger">{errosReg['fim']}</p>{/if}
				</div>
			</div>
			{#if duracaoHoras > 0}
				<p class="text-xs text-muted" aria-live="polite">
					Duração: <span class="font-mono text-ink tabular-nums">{formatNumber(duracaoHoras)}h</span>
				</p>
			{/if}
			<fieldset>
				<legend class={labelCls}>Tipo</legend>
				<div class="flex gap-2" role="radiogroup" aria-label="Tipo de apontamento">
					<label
						class="flex flex-1 cursor-pointer items-center gap-2 rounded-md border px-3 py-2.5 text-sm transition {tipoReg ===
						'encomenda'
							? 'border-brand/60 bg-brand/10 text-ink'
							: 'border-border text-muted hover:text-ink'}"
					>
						<input type="radio" name="reg-tipo" value="encomenda" bind:group={tipoReg} class="accent-brand" />
						Encomenda
					</label>
					<label
						class="flex flex-1 cursor-pointer items-center gap-2 rounded-md border px-3 py-2.5 text-sm transition {tipoReg ===
						'projeto'
							? 'border-brand/60 bg-brand/10 text-ink'
							: 'border-border text-muted hover:text-ink'}"
					>
						<input type="radio" name="reg-tipo" value="projeto" bind:group={tipoReg} class="accent-brand" />
						Projeto
					</label>
				</div>
			</fieldset>
			<div>
				<label for="reg-ref" class={labelCls}>Referência</label>
				<input
					id="reg-ref"
					type="search"
					bind:value={refReg}
					placeholder={tipoReg === 'encomenda' ? 'Ex.: ENC-1044' : 'Ex.: PRJ-210'}
					class="{inputCls} font-mono"
				/>
			</div>
			<div>
				<label for="reg-obs" class={labelCls}>Observações</label>
				<textarea
					id="reg-obs"
					bind:value={obsReg}
					rows="2"
					placeholder="Detalhes do trabalho realizado…"
					class="{inputCls} resize-y"
				></textarea>
			</div>
		</div>
	{/snippet}
	{#snippet footer()}
		<button
			type="button"
			onclick={() => (modalRegistrar = false)}
			disabled={mutando}
			class="rounded-md border border-border bg-surface px-4 py-2 text-sm font-medium text-ink transition hover:bg-elevated disabled:opacity-50"
		>
			Cancelar
		</button>
		<button
			type="button"
			onclick={() => void salvarRegistro()}
			disabled={mutando}
			class="rounded-md bg-brand px-4 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi disabled:opacity-50"
		>
			{mutando ? 'Salvando…' : 'Registrar'}
		</button>
	{/snippet}
</Modal>

<!-- Modal: rejeitar com motivo -->
<Modal
	open={alvoRejeicao !== null}
	title="Rejeitar apontamento"
	subtitle={alvoRejeicao ? `${alvoRejeicao.person.name} · ${formatarDataCurta(alvoRejeicao.date)}` : ''}
	onClose={() => (alvoRejeicao = null)}
	width="sm"
>
	{#snippet children()}
		<div data-testid="modal-rejeitar-hora" class="space-y-3">
			{#if erroMotivo}
				<p role="alert" class="rounded-md border border-danger/30 bg-danger/10 px-3 py-2 text-xs text-danger">
					{erroMotivo}
				</p>
			{/if}
			<div>
				<label for="rej-motivo" class={labelCls}>
					Motivo da rejeição <span class="text-danger" aria-hidden="true">*</span>
				</label>
				<textarea
					id="rej-motivo"
					bind:value={motivo}
					rows="3"
					placeholder="Ex.: horário divergente da escala…"
					class={inputCls}
				></textarea>
			</div>
		</div>
	{/snippet}
	{#snippet footer()}
		<button
			type="button"
			onclick={() => (alvoRejeicao = null)}
			disabled={mutando}
			class="rounded-md border border-border bg-surface px-4 py-2 text-sm font-medium text-ink transition hover:bg-elevated disabled:opacity-50"
		>
			Cancelar
		</button>
		<button
			type="button"
			onclick={() => void confirmarRejeicao()}
			disabled={mutando}
			class="rounded-md border border-danger/30 bg-danger/15 px-4 py-2 text-sm font-medium text-danger transition hover:bg-danger/25 disabled:opacity-50"
		>
			{mutando ? 'Rejeitando…' : 'Confirmar rejeição'}
		</button>
	{/snippet}
</Modal>
