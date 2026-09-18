<script lang="ts">
	interface Props {
		message: string;
		hint?: string;
		onRetry?: () => void;
	}

	let { message, hint, onRetry }: Props = $props();
</script>

<div
	role="alert"
	class="fade-in flex items-start gap-3 rounded-xl border border-danger/30 bg-danger/10 px-4 py-3"
>
	<svg
		class="mt-0.5 h-5 w-5 shrink-0 text-danger"
		fill="none"
		viewBox="0 0 24 24"
		stroke="currentColor"
		stroke-width="2"
		aria-hidden="true"
	>
		<path
			stroke-linecap="round"
			stroke-linejoin="round"
			d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126zM12 15.75h.007v.008H12v-.008z"
		/>
	</svg>
	<div class="min-w-0 flex-1">
		<p class="text-sm font-medium text-danger">{message}</p>
		{#if hint}
			<p class="mt-0.5 text-xs text-muted">{hint}</p>
		{/if}
	</div>
	{#if onRetry}
		<button
			type="button"
			onclick={onRetry}
			class="flex shrink-0 items-center gap-1.5 rounded-md border border-danger/30 bg-danger/15 px-3 py-1.5 text-xs font-medium text-danger transition hover:bg-danger/25"
		>
			<svg
				class="h-3.5 w-3.5"
				fill="none"
				viewBox="0 0 24 24"
				stroke="currentColor"
				stroke-width="2"
				aria-hidden="true"
			>
				<path
					stroke-linecap="round"
					stroke-linejoin="round"
					d="M16.023 9.348h4.992v-.001M2.985 19.644v-4.992m0 0h4.992m-4.993 0l3.181 3.183a8.25 8.25 0 0013.803-3.7M4.031 9.865a8.25 8.25 0 0113.803-3.7l3.181 3.182m0-4.991v4.99"
				/>
			</svg>
			Tentar novamente
		</button>
	{/if}
</div>