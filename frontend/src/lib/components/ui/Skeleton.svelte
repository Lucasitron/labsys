<script lang="ts">
	interface Props {
		class?: string;
	}

	let { class: className = '' }: Props = $props();
</script>

<div class="animate-pulse rounded bg-elevated {className}" aria-hidden="true"></div>