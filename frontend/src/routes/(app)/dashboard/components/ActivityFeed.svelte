<script lang="ts">
	import { relativeTime } from '$lib/utils/format';
	import type { ActivityItem } from '$lib/types/dashboard';

	interface Props {
		items: ActivityItem[];
		error?: boolean;
		empty?: boolean;
	}

	let { items, error = false, empty = false }: Props = $props();

	const MODULE_TONES: Record<string, string> = {
		rh: 'bg-brand',
		estoque: 'bg-warn',
		vendas: 'bg-brand',
		producao: 'bg-success',
		financeiro: 'bg-danger',
		notificacoes: 'bg-warn'
	};

	const tone = (module: string) => MODULE_TONES[module] ?? 'bg-brand';
</script>

<section class="rounded-xl border border-border bg-surface p-5">
	<div class="mb-4 flex items-center justify-between">
		<h2 class="text-sm font-semibold">Atividade recente</h2>
		<a href="/notificacoes/revisao" class="text-xs text-muted transition hover:text-brand">
			Ver histórico →
		</a>
	</div>

	{#if error}
		<div class="py-8 text-center">
			<p class="text-xs text-muted">Falha ao carregar atividade recente.</p>
		</div>
	{:else if empty || items.length === 0}
		<div class="py-8 text-center">
			<p class="text-sm text-muted">Nenhuma atividade recente.</p>
		</div>
	{:else}
		<ul class="space-y-3 text-sm">
			{#each items as item (item.id)}
				<li class="flex items-start gap-3">
					<span class="mt-2 h-1.5 w-1.5 shrink-0 rounded-full {tone(item.module)}"></span>
					<div class="flex-1">
						<p class="text-ink">
							{item.actor} {item.verb}
							{#if item.target}<span class="font-medium text-brand">{item.target}</span>{/if}
						</p>
						<p class="text-xs text-muted" data-testid="activity-time">
							{relativeTime(item.at)}
						</p>
					</div>
				</li>
			{/each}
		</ul>
	{/if}
</section>