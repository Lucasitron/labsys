<script lang="ts">
	import { onMount } from 'svelte';
	import { login } from '$lib/stores/auth';
	import { toUserMessage, type UserMessage } from '$lib/utils/errors';

	interface Props {
		onsuccess: () => void;
	}

	const { onsuccess }: Props = $props();

	let username = $state('');
	let password = $state('');
	let showPassword = $state(false);
	let loading = $state(false);
	let fieldErrors = $state<{ username?: string; password?: string }>({});
	let apiError = $state<UserMessage | null>(null);
	let usernameInput = $state<HTMLInputElement | null>(null);

	const inputBase =
		'w-full rounded-md border bg-elevated px-3 py-2.5 text-sm text-ink placeholder-muted/60 transition focus:outline-none';
	const inputOk = 'border-border focus:border-brand focus:ring-2 focus:ring-brand/30';
	const inputBad = 'border-danger/60 focus:border-danger focus:ring-2 focus:ring-danger/30';
	const inputDisabled = 'cursor-not-allowed opacity-70';

	const hasError = $derived(
		apiError !== null || fieldErrors.username !== undefined || fieldErrors.password !== undefined
	);
	const forgotClass = $derived(
		loading
			? 'pointer-events-none cursor-not-allowed text-xs text-muted/50'
			: 'text-xs text-muted transition hover:text-brand'
	);

	onMount(() => {
		usernameInput?.focus();
	});

	async function handleSubmit(event: SubmitEvent): Promise<void> {
		event.preventDefault();
		if (loading) return;

		const trimmed = username.trim();
		const next: { username?: string; password?: string } = {};
		if (!trimmed) next.username = 'Informe seu usuário';
		if (password.length < 6) next.password = 'Informe sua senha';
		fieldErrors = next;

		if (next.username || next.password) return;

		loading = true;
		apiError = null;

		try {
			await login(trimmed, password);
			onsuccess();
		} catch (err) {
			apiError = toUserMessage(err);
			loading = false;
		}
	}
</script>

<form class="space-y-4" onsubmit={handleSubmit} novalidate>
	<div>
		<label for="username" class="mb-1.5 block text-xs font-medium uppercase tracking-wide text-muted">
			Usuário
		</label>
		<input
			id="username"
			name="username"
			type="text"
			autocomplete="username"
			placeholder="Digite seu usuário"
			disabled={loading}
			bind:value={username}
			bind:this={usernameInput}
			class="{inputBase} {hasError ? inputBad : inputOk} {loading ? inputDisabled : ''}"
		/>
		{#if fieldErrors.username}
			<p class="mt-1.5 text-xs text-danger">{fieldErrors.username}</p>
		{/if}
	</div>

	<div>
		<label for="password" class="mb-1.5 block text-xs font-medium uppercase tracking-wide text-muted">
			Senha
		</label>
		<div class="relative">
			<input
				id="password"
				name="password"
				type={showPassword ? 'text' : 'password'}
				autocomplete="current-password"
				placeholder="Digite sua senha"
				disabled={loading}
				bind:value={password}
				class="{inputBase} pr-10 {hasError ? inputBad : inputOk} {loading ? inputDisabled : ''}"
			/>
			<button
				type="button"
				onclick={() => (showPassword = !showPassword)}
				disabled={loading}
				aria-label={showPassword ? 'Ocultar senha' : 'Mostrar senha'}
				class="absolute inset-y-0 right-0 flex items-center pr-3 text-muted transition hover:text-ink"
			>
				{#if showPassword}
					<svg
						xmlns="http://www.w3.org/2000/svg"
						class="h-4 w-4"
						fill="none"
						viewBox="0 0 24 24"
						stroke="currentColor"
						stroke-width="2"
					>
						<path
							stroke-linecap="round"
							stroke-linejoin="round"
							d="M3.98 8.223A10.477 10.477 0 0 0 1.934 12C3.226 16.338 7.244 19.5 12 19.5c.993 0 1.953-.138 2.863-.395M6.228 6.228A10.451 10.451 0 0 1 12 4.5c4.756 0 8.773 3.162 10.065 7.498a10.522 10.522 0 0 1-4.293 5.774M6.228 6.228 3 3m3.228 3.228 3.65 3.65m7.894 7.894L21 21m-3.228-3.228-3.65-3.65m0 0a3 3 0 1 0-4.243-4.243m4.242 4.242L9.88 9.88"
						/>
					</svg>
				{:else}
					<svg
						xmlns="http://www.w3.org/2000/svg"
						class="h-4 w-4"
						fill="none"
						viewBox="0 0 24 24"
						stroke="currentColor"
						stroke-width="2"
					>
						<path
							stroke-linecap="round"
							stroke-linejoin="round"
							d="M2.036 12.322a1.012 1.012 0 0 1 0-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178z"
						/>
						<path
							stroke-linecap="round"
							stroke-linejoin="round"
							d="M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0z"
						/>
					</svg>
				{/if}
			</button>
		</div>
		{#if fieldErrors.password}
			<p class="mt-1.5 text-xs text-danger">{fieldErrors.password}</p>
		{/if}
	</div>

	{#if apiError}
		<div
			class="fade-in flex items-start gap-2 rounded-md border border-danger/30 bg-danger/10 px-3 py-2.5"
			role="alert"
		>
			<svg
				xmlns="http://www.w3.org/2000/svg"
				class="mt-0.5 h-4 w-4 shrink-0 text-danger"
				fill="none"
				viewBox="0 0 24 24"
				stroke="currentColor"
				stroke-width="2"
			>
				<path
					stroke-linecap="round"
					stroke-linejoin="round"
					d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126zM12 15.75h.007v.008H12v-.008z"
				/>
			</svg>
			<div>
				<p class="text-xs font-medium text-danger">{apiError.message}</p>
				<p class="mt-0.5 text-[11px] text-muted">{apiError.hint}</p>
			</div>
		</div>
	{/if}

	<button
		type="submit"
		disabled={loading}
		class="flex w-full items-center justify-center gap-2 rounded-md bg-brand py-2.5 text-sm font-semibold tracking-wide text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi focus:outline-none focus:ring-2 focus:ring-brand/50 focus:ring-offset-2 focus:ring-offset-surface disabled:cursor-wait disabled:bg-brand/70"
	>
		{#if loading}
			<svg class="h-4 w-4 animate-spin" fill="none" viewBox="0 0 24 24" aria-hidden="true">
				<circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="3" />
				<path class="opacity-90" fill="currentColor" d="M4 12a8 8 0 018-8v3a5 5 0 00-5 5H4z" />
			</svg>
			Autenticando...
		{:else}
			Entrar
		{/if}
	</button>
</form>

<div class="mt-5 text-center">
	<a href="/auth/forgot-password" aria-disabled={loading} class={forgotClass}>
		Esqueci minha senha
	</a>
</div>