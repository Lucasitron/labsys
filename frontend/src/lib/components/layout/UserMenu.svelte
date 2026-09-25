<script lang="ts">
	import { goto } from '$app/navigation';
	import { get } from 'svelte/store';
	import { auth, logout } from '$lib/stores/auth';
	import { initials } from '$lib/utils/format';

	let open = $state(false);
	let rootEl = $state<HTMLDivElement | null>(null);

	const user = $derived(get(auth).user);

	function toggle(): void {
		open = !open;
	}

	function close(): void {
		open = false;
	}

	function handleKeydown(event: KeyboardEvent): void {
		if (event.key === 'Escape') close();
	}

	$effect(() => {
		if (!open) return;

		function onDocumentClick(event: MouseEvent): void {
			if (rootEl && !rootEl.contains(event.target as Node)) close();
		}

		document.addEventListener('click', onDocumentClick);
		return () => document.removeEventListener('click', onDocumentClick);
	});

	function handleLogout(): void {
		logout();
		goto('/auth/login');
	}
</script>

<div class="relative" bind:this={rootEl}>
	<button
		type="button"
		onclick={toggle}
		onkeydown={handleKeydown}
		aria-haspopup="menu"
		aria-expanded={open}
		class="flex items-center gap-2 rounded-md py-1.5 pl-2 pr-3 transition hover:bg-elevated"
	>
		<div
			class="flex h-7 w-7 items-center justify-center rounded-full border border-brand/30 bg-brand/20"
		>
			<span class="text-xs font-semibold text-brand">{user ? initials(user.name) : '--'}</span>
		</div>
		<span class="hidden text-sm text-ink sm:inline">{user?.name ?? 'Usuário'}</span>
		<svg
			class="h-3 w-3 text-muted transition-transform {open ? 'rotate-180' : ''}"
			fill="none"
			viewBox="0 0 24 24"
			stroke="currentColor"
			stroke-width="2.5"
			aria-hidden="true"
		>
			<path stroke-linecap="round" stroke-linejoin="round" d="M19.5 8.25l-7.5 7.5-7.5-7.5" />
		</svg>
	</button>

	{#if open}
		<div
			role="menu"
			tabindex="-1"
			onkeydown={handleKeydown}
			class="absolute right-0 top-full z-50 mt-2 w-48 overflow-hidden rounded-xl border border-border bg-surface shadow-2xl shadow-black/40"
		>
			<button
				type="button"
				role="menuitem"
				onclick={() => {
					close();
					goto('/configuracoes/perfil');
				}}
				class="flex w-full items-center gap-2 px-4 py-2.5 text-left text-sm text-ink transition hover:bg-elevated"
			>
				Perfil
			</button>
			<button
				type="button"
				role="menuitem"
				onclick={() => {
					close();
					goto('/configuracoes');
				}}
				class="flex w-full items-center gap-2 px-4 py-2.5 text-left text-sm text-ink transition hover:bg-elevated"
			>
				Configurações
			</button>
			<div class="border-t border-border"></div>
			<button
				type="button"
				role="menuitem"
				onclick={handleLogout}
				class="flex w-full items-center gap-2 px-4 py-2.5 text-left text-sm text-danger transition hover:bg-danger/10"
			>
				Sair
			</button>
		</div>
	{/if}
</div>