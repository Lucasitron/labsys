import { writable } from 'svelte/store';
import { browser } from '$app/environment';
import { fetchUnreadCount } from '$lib/api/dashboard';

const POLL_MS = 60_000;

export const unreadCount = writable(0);

let timer: ReturnType<typeof setInterval> | null = null;
let polling = false;

async function refresh(): Promise<void> {
	if (!browser) return;

	try {
		const result = await fetchUnreadCount();
		unreadCount.set(result.count);
	} catch {
		// mantém contador atual em caso de falha
	}
}

function schedule(): void {
	if (timer) clearInterval(timer);
	timer = setInterval(() => {
		if (!document.hidden) refresh();
	}, POLL_MS);
}

function handleVisibility(): void {
	if (document.hidden) return;
	refresh();
	schedule();
}

export function startPolling(): void {
	if (polling || !browser) return;
	polling = true;

	refresh();
	schedule();
	document.addEventListener('visibilitychange', handleVisibility);
}

export function stopPolling(): void {
	if (!polling || !browser) return;
	polling = false;

	if (timer) clearInterval(timer);
	timer = null;
	document.removeEventListener('visibilitychange', handleVisibility);
}