<script lang="ts">
	import { goto } from '$app/navigation';
	import LoginForm from './login-form.svelte';

	let authed = $state(false);

	function handleSuccess(): void {
		authed = true;
		setTimeout(() => goto('/dashboard'), 600);
	}
</script>

<svelte:head>
	<title>Login — FabLab Management</title>
</svelte:head>

<main
	class="relative flex min-h-screen items-center justify-center overflow-hidden bg-base p-4 text-ink"
>
	<div
		class={`pointer-events-none absolute inset-0 ${authed ? 'glow-success' : 'glow'}`}
		aria-hidden="true"
	></div>

	<section class="relative w-full max-w-sm">
		{#if authed}
			<div class="fade-in mb-8 flex justify-center">
				<div
					class="flex h-16 w-16 items-center justify-center rounded-2xl border border-success/40 bg-success/10 shadow-[0_0_40px_-10px_rgba(14,166,65,0.7)]"
				>
					<svg
						xmlns="http://www.w3.org/2000/svg"
						class="h-7 w-7 text-success"
						fill="none"
						viewBox="0 0 24 24"
						stroke="currentColor"
						stroke-width="2.5"
					>
						<path stroke-linecap="round" stroke-linejoin="round" d="M4.5 12.75l6 6 9-13.5" />
					</svg>
				</div>
			</div>

			<h1 class="fade-in mb-1 text-center text-2xl font-semibold tracking-tight">Autenticado</h1>
			<p class="fade-in mb-8 text-center text-sm text-muted">Redirecionando para o painel...</p>

			<div
				class="rounded-xl border border-success/20 bg-surface/80 p-6 shadow-2xl shadow-black/40 backdrop-blur"
			>
				<div class="flex flex-col items-center gap-3 py-4">
					<svg class="h-6 w-6 animate-spin text-success" fill="none" viewBox="0 0 24 24" aria-hidden="true">
						<circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="3" />
						<path class="opacity-90" fill="currentColor" d="M4 12a8 8 0 018-8v3a5 5 0 00-5 5H4z" />
					</svg>
					<p class="text-xs text-muted">Carregando dashboard…</p>
				</div>
			</div>
		{:else}
			<div class="mb-8 flex justify-center">
				<div
					class="flex h-16 w-16 items-center justify-center rounded-2xl border border-brand/30 bg-brand/10 shadow-[0_0_40px_-10px_rgba(28,128,222,0.6)]"
				>
					<span class="text-xl font-bold tracking-tight text-brand">FL</span>
				</div>
			</div>

			<h1 class="mb-1 text-center text-2xl font-semibold tracking-tight">Bem-vindo de volta</h1>
			<p class="mb-8 text-center text-sm text-muted">Acesse o painel do FabLab</p>

			<div
				class="rounded-xl border border-border bg-surface/80 p-6 shadow-2xl shadow-black/40 backdrop-blur"
			>
				<LoginForm onsuccess={handleSuccess} />
			</div>
		{/if}

		<p class="mt-6 text-center text-[11px] text-muted/60">FabLab Management · v0.1</p>
	</section>
</main>