import { writable } from 'svelte/store';

export type ToastTone = 'success' | 'danger' | 'warn' | 'info';

export interface ToastItem {
	id: number;
	tone: ToastTone;
	message: string;
}

function createToasts() {
	const { subscribe, update } = writable<ToastItem[]>([]);

	let counter = 0;

	function push(tone: ToastTone, message: string, duration = 3200): void {
		const id = ++counter;
		update((items) => [...items, { id, tone, message }]);
		setTimeout(() => {
			update((items) => items.filter((t) => t.id !== id));
		}, duration);
	}

	return {
		subscribe,
		success(message: string) {
			push('success', message);
		},
		danger(message: string) {
			push('danger', message, 4500);
		},
		warn(message: string) {
			push('warn', message);
		},
		info(message: string) {
			push('info', message);
		},
		dismiss(id: number) {
			update((items) => items.filter((t) => t.id !== id));
		}
	};
}

export const toasts = createToasts();

export function toastError(err: unknown, fallback = 'Ocorreu um erro. Tente novamente.'): void {
	const message = err instanceof Error && err.message ? err.message : fallback;
	toasts.danger(message);
}