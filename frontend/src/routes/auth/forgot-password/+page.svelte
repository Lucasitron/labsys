<script lang="ts">
	import { page } from '$app/state';
	import { sanitizeRedirect } from '$lib/api/client';
</script>

<svelte:head>
	<title>Recuperar senha — FabLab Management</title>
</svelte:head>

<main
	class="relative flex min-h-screen items-center justify-center overflow-hidden bg-base p-4 text-ink"
>
	<div class="pointer-events-none absolute inset-0 glow" aria-hidden="true"></div>

	<section class="relative w-full max-w-sm">
		<div class="mb-8 flex justify-center">
			<div
				class="flex h-16 w-16 items-center justify-center rounded-2xl border border-brand/30 bg-brand/10 shadow-[0_0_40px_-10px_rgba(28,128,222,0.6)]"
			>
				<span class="text-xl font-bold tracking-tight text-brand">FL</span>
			</div>
		</div>

		<h1 class="mb-1 text-center text-2xl font-semibold tracking-tight">Recuperar senha</h1>
		<p class="mb-8 text-center text-sm text-muted">
			A recuperação de senha estará disponível em uma versão futura.
		</p>

		<div
			class="rounded-xl border border-border bg-surface/80 p-6 shadow-2xl shadow-black/40 backdrop-blur"
		>
			<div class="flex flex-col items-center gap-3 py-4 text-center">
				<svg
					xmlns="http://www.w3.org/2000/svg"
					class="h-8 w-8 text-brand"
					fill="none"
					viewBox="0 0 24 24"
					stroke="currentColor"
					stroke-width="1.5"
					aria-hidden="true"
				>
					<path
						stroke-linecap="round"
						stroke-linejoin="round"
						d="M16.5 10.5V6.75a4.5 4.5 0 1 0-9 0v3.75m-.75 11.25h10.5a2.25 2.25 0 0 0 2.25-2.25v-6.75a2.25 2.25 0 0 0-2.25-2.25H6.75a2.25 2.25 0 0 0-2.25 2.25v6.75a2.25 2.25 0 0 0 2.25 2.25Z"
					/>
				</svg>
				<p class="max-w-xs text-xs text-muted">
					Por ora, entre em contato com um administrador do FabLab para redefinir o acesso à sua
					conta.
				</p>
			</div>

			<a
				href="/auth/login?redirect={encodeURIComponent(sanitizeRedirect(page.url.searchParams.get('redirect')))}"
				class="flex w-full items-center justify-center gap-2 rounded-md border border-border bg-elevated py-2.5 text-sm font-medium text-ink transition hover:bg-elevated/70 hover:text-brand focus:outline-none focus:ring-2 focus:ring-brand/40"
			>
				<svg
					xmlns="http://www.w3.org/2000/svg"
					class="h-4 w-4"
					fill="none"
					viewBox="0 0 24 24"
					stroke="currentColor"
					stroke-width="2"
					aria-hidden="true"
				>
					<path stroke-linecap="round" stroke-linejoin="round" d="M10.5 19.5 3 12m0 0 7.5-7.5M3 12h18" />
				</svg>
				Voltar para o login
			</a>
		</div>

		<p class="mt-6 text-center text-[11px] text-muted/60">FabLab Management · v0.1</p>
	</section>
</main>