import type { Handle } from '@sveltejs/kit';

// Evita que o browser cacheie o shell HTML de forma heurística e exiba versão
// antiga do SPA após deploy: força revalidação pelo etag (304 quando nada mudou).
export const handle: Handle = async ({ event, resolve }) => {
	const response = await resolve(event);
	if ((response.headers.get('content-type') ?? '').includes('text/html')) {
		response.headers.set('cache-control', 'no-cache');
	}
	return response;
};
