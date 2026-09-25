<script lang="ts">
	import Breadcrumb from './Breadcrumb.svelte';
	import NotificationsButton from './NotificationsButton.svelte';
	import UserMenu from './UserMenu.svelte';

	interface Props {
		onToggleMenu: () => void;
	}

	let { onToggleMenu }: Props = $props();
</script>

<header
	class="sticky top-0 z-30 flex h-14 shrink-0 items-center gap-4 border-b border-border bg-surface/60 px-6 backdrop-blur"
>
	<button
		type="button"
		onclick={onToggleMenu}
		aria-label="Abrir menu"
		class="-ml-2 rounded-md p-2 text-muted transition hover:bg-elevated hover:text-ink lg:hidden"
	>
		<svg
			class="h-5 w-5"
			fill="none"
			viewBox="0 0 24 24"
			stroke="currentColor"
			stroke-width="2"
			aria-hidden="true"
		>
			<path stroke-linecap="round" stroke-linejoin="round" d="M3.75 6.75h16.5M3.75 12h16.5m-16.5 5.25h16.5" />
		</svg>
	</button>

	<Breadcrumb />

	<div class="ml-auto flex items-center gap-2">
		<NotificationsButton />
		<UserMenu />
	</div>
</header>