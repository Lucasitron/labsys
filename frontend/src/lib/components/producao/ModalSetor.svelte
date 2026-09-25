<script lang="ts">
	import { onMount } from 'svelte';
	import type { Ciclo5S, NomeFunc, ResponsavelSetor, Setor5S } from '$lib/types/producao';
	import {
		aprovarAlteracaoDoSetor,
		atualizarSetor5S,
		criarSetor5S
	} from '$lib/api/producao/client';
	import { ApiError, NetworkError } from '$lib/api/client';
	import Modal from '$lib/components/ui/Modal.svelte';

	interface Props {
		open: boolean;
		setor?: Setor5S | null;
		pessoas: NomeFunc[];
		isAdmin: boolean;
		canEdit: boolean;
		onClose: () => void;
		onSalvo?: (setor: Setor5S, proposta: boolean) => void;
	}

	let { open, setor = null, pessoas, isAdmin, canEdit, onClose, onSalvo }: Props = $props();

	const ciclos: Ciclo5S[] = ['semanal', 'quinzenal', 'mensal'];

	let nome = $state('');
	let ciclo = $state<Ciclo5S>('semanal');
	let responsaveis = $state<ResponsavelSetor[]>([]);
	let auditorId = $state('');
	let enviando = $state(false);
	let erro = $state('');

	const editando = $derived(setor !== null);

	$effect(() => {
		if (open) {
			nome = setor?.nome ?? '';
			ciclo = setor?.ciclo ?? 'semanal';
			responsaveis = (setor?.responsaveis ?? []).map((r) => ({ ...r }));
			auditorId = setor?.auditor?.id ?? '';
			erro = '';
		}
	});

	onMount(() => {
		const onKey = (e: KeyboardEvent): void => {
			if (open && e.key === 'Escape') onClose();
		};
		document.addEventListener('keydown', onKey);
		return () => document.removeEventListener('keydown', onKey);
	});

	const titulo = $derived(editando ? `Editar setor 5S · ${setor?.nome ?? ''}` : 'Novo setor 5S');

	function toggleResponsavel(membroId: string, incluso: boolean): void {
		if (incluso) {
			responsaveis = [...responsaveis, { membro: pessoas.find((p) => p.id === membroId)!, ps: false }];
		} else {
			responsaveis = responsaveis.filter((r) => r.membro.id !== membroId);
		}
	}

	function setPs(membroId: string, ps: boolean): void {
		responsaveis = responsaveis.map((r) => (r.membro.id === membroId ? { ...r, ps } : r));
	}

	const estaIncluso = $derived((membroId: string) => responsaveis.some((r) => r.membro.id === membroId));

	function mensagemErro(err: unknown): string {
		if (err instanceof ApiError || err instanceof NetworkError) return err.message;
		return 'Não foi possível salvar o setor.';
	}

	async function salvar(event: SubmitEvent): Promise<void> {
		event.preventDefault();
		enviando = true;
		erro = '';
		const auditor = pessoas.find((p) => p.id === auditorId);
		try {
			if (!canEdit) return;
			const payload = {
				nome: nome.trim(),
				ciclo,
				responsaveis: responsaveis.map((r) => ({ membroId: r.membro.id, ps: r.ps })),
				auditorId: auditor?.id ?? ''
			};
			let atualizado: Setor5S;
			if (isAdmin) {
				atualizado = editando
					? await atualizarSetor5S(setor!.id, payload)
					: await criarSetor5S(payload);
				onSalvo?.(atualizado, false);
			} else if (editando) {
				atualizado = await aprovarAlteracaoDoSetor(setor!.id, { aprovado: false });
				onSalvo?.(atualizado, true);
			} else {
				atualizado = await criarSetor5S(payload);
				onSalvo?.(atualizado, true);
			}
		} catch (err) {
			erro = mensagemErro(err);
		} finally {
			enviando = false;
		}
	}

	const labelCta = $derived(
		editando ? (isAdmin ? 'Salvar alterações' : 'Enviar p/ aprovação') : isAdmin ? 'Criar setor' : 'Enviar p/ aprovação'
	);
	const testIdCta = $derived(editando ? 'set-salvar' : 'set-criar');

	const inputCls =
		'w-full rounded-lg border border-border bg-elevated px-3 py-2.5 text-sm text-ink placeholder:text-muted/60 focus:border-brand focus:outline-none focus:ring-1 focus:ring-brand/40';
</script>

<Modal {open} title={titulo} {onClose} width="md">
	{#snippet children()}
		<form id="modal-setor-form" onsubmit={salvar} novalidate>
			{#if erro}
				<div role="alert" class="mb-4 rounded-xl border border-danger/30 bg-danger/10 px-4 py-3 text-sm text-danger">
					{erro}
				</div>
			{/if}

			<div class="mb-4 flex items-center justify-between gap-2 rounded-lg border border-border bg-elevated/30 p-3">
				<span class="text-[11px] text-muted">Salvando como</span>
				<span class="inline-flex items-center gap-1.5 text-xs font-medium text-ink">
					<span class="h-1.5 w-1.5 rounded-full {isAdmin ? 'bg-brand' : 'bg-warn'}" aria-hidden="true"></span>
					{isAdmin ? 'Admin (salva direto)' : 'Responsável / Auditor (envia p/ aprovação)'}
				</span>
			</div>

			<div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
				<label class="block">
					<span class="mb-1.5 block text-xs font-medium text-muted">Nome do setor *</span>
					<input
						type="text"
						bind:value={nome}
						required
						maxlength="80"
						placeholder="ex.: Bancada de eletrônica"
						class={inputCls}
					/>
				</label>

				<fieldset>
					<legend class="mb-1.5 block text-xs font-medium text-muted">Cadência de auditoria *</legend>
					<div class="flex flex-wrap gap-1.5">
						{#each ciclos as c}
							<label
								class="inline-flex cursor-pointer items-center gap-1.5 rounded-full border px-3 py-1.5 text-xs font-medium transition {ciclo === c
									? 'border-brand bg-brand/10 text-brandhi'
									: 'border-border bg-elevated text-muted hover:border-brand/40'}"
							>
								<input
									type="radio"
									name="setor-ciclo"
									value={c}
									checked={ciclo === c}
									onchange={() => (ciclo = c)}
									class="h-3.5 w-3.5 accent-brand"
								/>
								{c === 'semanal' ? 'Semanal' : c === 'quinzenal' ? 'Quinzenal' : 'Mensal'}
							</label>
						{/each}
					</div>
				</fieldset>
			</div>

			<div class="mt-4 rounded-xl border border-border bg-elevated/30 p-3">
				<p class="mb-2 text-xs font-medium text-muted">Responsáveis (N:N) *</p>
				{#if pessoas.length === 0}
					<p class="text-xs text-muted">Nenhum integrante disponível.</p>
				{:else}
					<div class="space-y-1.5">
						{#each pessoas as p (p.id)}
							<div class="flex flex-wrap items-center justify-between gap-2 rounded-lg border border-border bg-elevated/50 px-3 py-2">
								<label class="inline-flex cursor-pointer items-center gap-2 text-sm text-ink">
									<input
										type="checkbox"
										checked={estaIncluso(p.id)}
										onchange={(e) => toggleResponsavel(p.id, (e.currentTarget as HTMLInputElement).checked)}
										class="h-4 w-4 rounded border-border accent-brand"
									/>
									{p.nome}
								</label>
								{#if estaIncluso(p.id)}
									<label class="inline-flex cursor-pointer items-center gap-1.5 text-xs text-muted">
										<input
											type="checkbox"
											checked={responsaveis.find((r) => r.membro.id === p.id)?.ps ?? false}
											onchange={(e) => setPs(p.id, (e.currentTarget as HTMLInputElement).checked)}
											class="h-3.5 w-3.5 rounded accent-brand"
										/>
										PS
									</label>
								{/if}
							</div>
						{/each}
					</div>
					<p class="mt-2 text-[11px] text-muted">PS = integrante de processo seletivo, atua sob um membro responsável.</p>
				{/if}
			</div>

			<label class="mt-4 block">
				<span class="mb-1.5 block text-xs font-medium text-muted">Auditor do setor *</span>
				<select bind:value={auditorId} required class={inputCls}>
					<option value="" disabled>Selecione o auditor…</option>
					{#each pessoas as p (p.id)}
						<option value={p.id}>{p.nome}</option>
					{/each}
				</select>
			</label>

			<div class="mt-4 rounded-lg border border-border bg-elevated/30 p-3 text-xs text-muted">
				{editando
					? 'No modo editar, as alterações valem para o cartão vigente.'
					: 'Setor novo começa com cartão Amarelo até a 1ª auditoria.'}
				{#if !isAdmin}
					<br />Alterações propostas por responsável/auditor seguem para o fluxo <span class="text-ink">Aprovar alteração</span> (Admin valida e aplica).
				{/if}
			</div>
		</form>
	{/snippet}

	{#snippet footer()}
		<button type="button" onclick={onClose} class="rounded-md px-4 py-2 text-sm text-muted transition hover:text-ink">
			Cancelar
		</button>
		{#if canEdit}
			<button
				type="submit"
				form="modal-setor-form"
				data-testid={testIdCta}
				disabled={enviando || nome.trim().length === 0 || auditorId === ''}
				class="rounded-md bg-brand px-4 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi disabled:cursor-not-allowed disabled:opacity-50"
			>
				{enviando ? 'Enviando…' : labelCta}
			</button>
		{/if}
	{/snippet}
</Modal>