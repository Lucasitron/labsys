export const API_BASE: string =
	import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api';

const TIMEOUT_MS = 15_000;

export class ApiError extends Error {
	readonly status: number;
	readonly code: string;

	constructor(status: number, code: string, message: string) {
		super(message);
		this.name = 'ApiError';
		this.status = status;
		this.code = code;
	}
}

export class NetworkError extends Error {
	constructor(message = 'Falha de conexão. Verifique sua rede.') {
		super(message);
		this.name = 'NetworkError';
	}
}

interface ErrorPayload {
	error?: string;
	code?: string;
	message?: string;
}

export async function apiFetch<T>(path: string, init: RequestInit = {}): Promise<T> {
	const controller = new AbortController();
	const timeout = setTimeout(() => controller.abort(), TIMEOUT_MS);

	const headers = new Headers(init.headers);
	if (!headers.has('Content-Type')) {
		headers.set('Content-Type', 'application/json');
	}

	let response: Response;
	try {
		response = await fetch(`${API_BASE}${path}`, {
			...init,
			headers,
			signal: controller.signal
		});
	} catch (err) {
		if (err instanceof DOMException && err.name === 'AbortError') {
			throw new NetworkError('Tempo limite excedido. Verifique sua rede.');
		}
		throw new NetworkError();
	} finally {
		clearTimeout(timeout);
	}

	if (!response.ok) {
		const payload = await readPayload(response);
		const code = payload.code ?? payload.error ?? 'UNKNOWN';
		throw new ApiError(response.status, code, payload.message ?? `Erro ${response.status}`);
	}

	return (await response.json()) as T;
}

async function readPayload(response: Response): Promise<ErrorPayload> {
	try {
		return (await response.json()) as ErrorPayload;
	} catch {
		return {};
	}
}