<script lang="ts">
	import { getPreferences, savePreferences } from '$lib/api/notifications';
	import type {
		NotificationChannel,
		NotificationPreferences,
		NotificationType,
		Tone
	} from '$lib/types/notifications';
	import { NOTIFICATION_TYPES, TYPE_META } from '$lib/utils/notification-format';
	import { toasts } from '$lib/stores/toast';
	import ErrorBanner from '$lib/components/ui/ErrorBanner.svelte';
	import Icon from '$lib/components/ui/Icon.svelte';
	import PageHeader from '$lib/components/ui/PageHeader.svelte';
	import Skeleton from '$lib/components/ui/Skeleton.svelte';
	import StatusBadge from '$lib/components/ui/StatusBadge.svelte';

	interface ChannelInfo {
		id: NotificationChannel;
		label: string;
		description: string;
	}

	const CHANNELS: ChannelInfo[] = [
		{
			id: 'inapp',
			label: 'In-app',
			description: 'Exibidas na central de notificações e no sino da topbar.'
		},
		{
			id: 'email',
			label: 'E-mail',
			description: 'Enviadas para o endereço de e-mail do destinatário.'
		},
		{
			id: 'push',
			label: 'Push',
			description: 'Enviadas como notificação no dispositivo do destinatário.'
		}
	];

	const ICON_STYLES: Record<Tone, string> = {
		brand: 'bg-brand/15 border-brand/30 text-brand',
		warn: 'bg-warn/15 border-warn/30 text-warn',
		success: 'bg-success/15 border-success/30 text-success',
		danger: 'bg-danger/15 border-danger/30 text-danger',
		ink: 'bg-ink/15 border-ink/30 text-ink',
		muted: 'bg-muted/15 border-muted/30 text-muted'
	};

	function matrizPadrao(): NotificationPreferences {
		const m = {} as NotificationPreferences;
		for (const tipo of NOTIFICATION_TYPES) {
			m[tipo] = { inapp: true, email: true, push: tipo !== 'sistema' };
		}
		return m;
	}

	function clonar(p: NotificationPreferences): NotificationPreferences {
		const m = {} as NotificationPreferences;
		for (const tipo of NOTIFICATION_TYPES) {
			m[tipo] = { ...p[tipo] };
		}
		return m;
	}

	function iguais(a: NotificationPreferences, b: NotificationPreferences): boolean {
		for (const tipo of NOTIFICATION_TYPES) {
			for (const ch of CHANNELS) {
				if (a[tipo][ch.id] !== b[tipo][ch.id]) return false;
			}
		}
		return true;
	}

	let original: NotificationPreferences | null = $state(null);
	let draft: NotificationPreferences | null = $state(null);
	let loading = $state(true);
	let loadError = $state<string | null>(null);
	let saveError = $state<string | null>(null);
	let saving = $state(false);
	let retryTick = $state(0);

	const dirty = $derived(
		original !== null && draft !== null && !iguais(original, draft)
	);

	const contagens = $derived.by(() => {
		const counts = {} as Record<NotificationType, number>;
		if (!draft) return counts;
		const atual = draft;
		for (const tipo of NOTIFICATION_TYPES) {
			counts[tipo] = CHANNELS.reduce(
				(acc, ch) => acc + (atual[tipo][ch.id] ? 1 : 0),
				0
			);
		}
		return counts;
	});

	$effect(() => {
		void retryTick;
		loading = true;
		loadError = null;
		saveError = null;
		void getPreferences()
			.then((p) => {
				original = p;
				draft = clonar(p);
			})
			.catch((err: unknown) => {
				loadError =
					err instanceof Error ? err.message : 'Não foi possível carregar as preferências.';
			})
			.finally(() => {
				loading = false;
			});
	});

	function toggle(tipo: NotificationType, canal: NotificationChannel): void {
		if (!draft) return;
		draft[tipo][canal] = !draft[tipo][canal];
		saveError = null;
	}

	function restaurarPadrao(): void {
		if (!draft) return;
		draft = matrizPadrao();
		saveError = null;
	}

	async function salvar(): Promise<void> {
		if (!draft) return;
		saving = true;
		saveError = null;
		try {
			await savePreferences(draft);
			original = clonar(draft);
			toasts.success('Preferências de canais salvas.');
		} catch (err: unknown) {
			saveError =
				err instanceof Error ? err.message : 'Não foi possível salvar as preferências.';
		} finally {
			saving = false;
		}
	}
</script>

<svelte:head>
	<title>Preferências de canais · Notificações · FabLab</title>
	<meta name="description" content="Configure por quais canais cada tipo de notificação é entregue." />
</svelte:head>

<div class="space-y-4">
	<PageHeader
		title="Preferências de canais"
		subtitle="Matriz de canais por tipo de notificação no sistema."
	>
		{#snippet children()}
			<StatusBadge label="Restrito ao Admin" color="warn" icon="lock" />
		{/snippet}
	</PageHeader>

	{#if loading}
		<div class="grid gap-6 lg:grid-cols-[280px_1fr]">
			<Skeleton class="aspect-[1/1.2] rounded-xl lg:aspect-auto lg:min-h-56" />
			<div class="rounded-xl border border-border bg-surface p-5">
				<Skeleton class="h-5 w-44" />
				<div class="mt-4 space-y-5">
					{#each NOTIFICATION_TYPES as tipo (tipo)}
						<div class="flex items-center justify-between gap-4">
							<Skeleton class="h-4 w-32" />
							<div class="flex gap-8">
								{#each CHANNELS as ch (ch.id)}
									<Skeleton class="h-6 w-11 rounded-full" />
								{/each}
							</div>
						</div>
					{/each}
				</div>
			</div>
		</div>
	{:else if loadError}
		<div data-testid="notif-pref-error">
			<ErrorBanner
				message={loadError}
				hint="Não foi possível carregar as preferências de canais. Verifique sua conexão e tente novamente."
				onRetry={() => {
					retryTick += 1;
				}}
			/>
		</div>
	{:else if original && draft}
		<div class="grid gap-6 lg:grid-cols-[280px_1fr]">
			<aside class="rounded-xl border border-border bg-surface p-5">
				<h2 class="text-sm font-semibold text-ink">Canais disponíveis</h2>
				<p class="mt-1 text-xs text-muted">
					Cada tipo de notificação pode ser entregue por um ou mais canais abaixo.
				</p>
				<ul class="mt-4 space-y-4" data-testid="notif-pref-list">
					{#each CHANNELS as ch (ch.id)}
						<li>
							<p class="flex items-center gap-1.5 text-sm font-medium text-ink">
								<span class="flex h-5 w-5 items-center justify-center rounded-full border border-border bg-elevated/80">
									<Icon name={ch.id === 'inapp' ? 'bell' : 'document'} class="h-3 w-3 text-muted" />
								</span>
								{ch.label}
							</p>
							<p class="mt-1 text-xs text-muted">{ch.description}</p>
						</li>
					{/each}
				</ul>
			</aside>

			<div>
				{#if saveError}
					<div class="mb-4" data-testid="notif-pref-save-error">
						<ErrorBanner
							message={saveError}
							hint="Suas alterações foram mantidas. Tente salvar novamente."
						/>
					</div>
				{/if}

				<div class="overflow-hidden rounded-xl border border-border bg-surface">
					<div class="grid min-w-[600px] grid-cols-[minmax(160px,1.6fr)_1fr_1fr_1fr] items-center gap-3 border-b border-border bg-elevated/50 px-4 py-2.5 text-[11px] font-medium uppercase tracking-wide text-muted">
						<span>Tipo de notificação</span>
						{#each CHANNELS as ch (ch.id)}
							<span class="text-center">{ch.label}</span>
						{/each}
					</div>
					<div class="overflow-x-auto">
						{#each NOTIFICATION_TYPES as tipo (tipo)}
							<div class="grid min-w-[600px] grid-cols-[minmax(160px,1.6fr)_1fr_1fr_1fr] items-center gap-3 border-b border-border px-4 py-3 transition-colors hover:bg-elevated/40">
								<div class="flex min-w-0 items-center gap-2.5">
									<span
										class="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg border {ICON_STYLES[TYPE_META[tipo].tone]}"
										aria-hidden="true"
									>
										<Icon name={TYPE_META[tipo].icon} class="h-4 w-4" />
									</span>
									<div class="min-w-0">
										<p class="truncate text-sm font-medium text-ink">{TYPE_META[tipo].label}</p>
										<span class="mt-0.5 inline-flex rounded-full border border-border bg-elevated/80 px-2 py-0.5 text-[10px] font-medium text-muted">
											{contagens[tipo]} de {CHANNELS.length} canais
										</span>
									</div>
								</div>
								{#each CHANNELS as ch (ch.id)}
									<div class="flex items-center justify-center">
										<label
											class="relative inline-flex cursor-pointer items-center"
										>
											<input
												type="checkbox"
												data-testid="notif-pref-toggle"
												class="peer sr-only"
												checked={draft[tipo][ch.id]}
												onchange={() => toggle(tipo, ch.id)}
												aria-label={`${draft[tipo][ch.id] ? 'Desativar' : 'Ativar'} ${TYPE_META[tipo].label} por ${ch.label}`}
											/>
											<span
												aria-hidden="true"
												class="inline-flex h-6 w-11 items-center rounded-full border border-border bg-elevated px-0.5 transition peer-checked:border-brand/40 peer-checked:bg-brand"
											>
												<span class="h-[18px] w-[18px] rounded-full bg-muted/40 shadow transition peer-checked:translate-x-5 peer-checked:bg-white"></span>
											</span>
										</label>
									</div>
								{/each}
							</div>
						{/each}
					</div>
				</div>

				<div class="mt-4 flex flex-wrap items-center justify-end gap-2">
					<button
						type="button"
						data-testid="notif-pref-reset"
						onclick={restaurarPadrao}
						class="inline-flex items-center gap-1.5 rounded-md border border-border bg-elevated px-3 py-2 text-sm font-medium text-ink transition hover:bg-elevated/70 hover:text-brandhi"
					>
						<Icon name="arrow-uturn-left" class="h-4 w-4" />
						Restaurar padrão
					</button>
					<button
						type="button"
						data-testid="notif-pref-save"
						disabled={!dirty || saving}
						onclick={() => void salvar()}
						class="inline-flex items-center gap-1.5 rounded-md bg-brand px-3.5 py-2 text-sm font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi disabled:cursor-not-allowed disabled:opacity-40"
					>
						<Icon name="check" class="h-4 w-4" />
						{saving ? 'Salvando…' : 'Salvar preferências'}
					</button>
				</div>
			</div>
		</div>
	{/if}
</div>