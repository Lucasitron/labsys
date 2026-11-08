<script lang="ts">
	import { goto } from '$app/navigation';
	import { page } from '$app/state';
	import type { Snippet } from 'svelte';
	import Icon, { type IconName } from '$lib/components/ui/Icon.svelte';

	let { children }: { children: Snippet } = $props();

	const BASE = '/pessoas/treinamentos';

	interface Item {
		href: string;
		label: string;
		icon: IconName;
	}

	const ITENS: Item[] = [
		{ href: '/pessoas/treinamentos', label: 'Visão geral', icon: 'squares' },
		{ href: '/pessoas/treinamentos/agenda', label: 'Agenda', icon: 'calendar' },
		{ href: '/pessoas/treinamentos/documentacao', label: 'Documentação', icon: 'document' },
		{ href: '/pessoas/treinamentos/sessao', label: 'Sessão', icon: 'clock' }
	];

	function ehAtivo(href: string): boolean {
		const path = page.url.pathname;
		if (href !== BASE) return path === href || path.startsWith(`${href}/`);
		return (
			path === BASE ||
			(path.startsWith(`${BASE}/`) &&
				!ITENS.some((i) => i.href !== BASE && (path === i.href || path.startsWith(`${i.href}/`))))
		);
	}

	function fechar(): void {
		void goto('/pessoas');
	}

	function aoTeclar(evento: KeyboardEvent): void {
		if (evento.key === 'Escape') fechar();
	}
</script>

<svelte:window onkeydown={aoTeclar} />

<div class="flex flex-col gap-4 md:flex-row">
	<aside
		aria-label="Navegação de treinamentos"
		class="w-full shrink-0 rounded-xl border border-border bg-surface shadow-2xl shadow-black/40 md:sticky md:top-4 md:w-60 md:self-start"
	>
		<div class="flex items-center justify-between gap-2 border-b border-border px-4 py-3">
			<h2 class="text-sm font-semibold text-ink">Treinamentos</h2>
			<button
				type="button"
				onclick={fechar}
				aria-label="Fechar treinamentos e voltar para pessoas"
				title="Voltar para pessoas"
				class="inline-flex h-8 w-8 items-center justify-center rounded-md text-muted transition hover:bg-elevated hover:text-ink"
			>
				<Icon name="chevron-left" class="h-4 w-4 rotate-180" />
			</button>
		</div>
		<nav aria-label="Seções de treinamentos" class="flex gap-1 overflow-x-auto p-2 md:flex-col">
			{#each ITENS as item (item.href)}
				{@const ativo = ehAtivo(item.href)}
				<a
					href={item.href}
					aria-current={ativo ? 'page' : undefined}
					aria-label={item.label}
					class="flex items-center gap-3 rounded-md border-l-2 px-3 py-2 text-sm font-medium transition {ativo
						? 'border-brand bg-brand/10 text-brand'
						: 'border-transparent text-muted hover:bg-elevated hover:text-ink'}"
				>
					<Icon name={item.icon} class="h-4 w-4 shrink-0" />
					{item.label}
				</a>
			{/each}
		</nav>
	</aside>
	<div class="min-w-0 flex-1">
		{@render children()}
	</div>
</div>
