import { describe, expect, it } from 'vitest';
import type { Handle } from '@sveltejs/kit';
import { handle } from '../../src/hooks.server';

describe('hooks.server handle', () => {
	it('define cache-control: no-cache para respostas HTML', async () => {
		const resolve = async () => new Response('<html>', { headers: { 'content-type': 'text/html' } });
		const event = {} as unknown as Parameters<Handle>[0]['event'];
		const response = await handle({ event, resolve });
		expect(response.headers.get('cache-control')).toBe('no-cache');
	});

	it('não adiciona cache-control para respostas não-HTML', async () => {
		const resolve = async () => new Response('{}', { headers: { 'content-type': 'application/json' } });
		const event = {} as unknown as Parameters<Handle>[0]['event'];
		const response = await handle({ event, resolve });
		expect(response.headers.get('cache-control')).toBeNull();
	});

	it('sobrescreve cache-control existente apenas quando text/html', async () => {
		// HTML: sobrescreve o valor pré-existente.
		const resolveHtml = async () =>
			new Response('<html>', { headers: { 'content-type': 'text/html', 'cache-control': 'public, max-age=3600' } });
		const event = {} as unknown as Parameters<Handle>[0]['event'];
		const htmlResponse = await handle({ event, resolve: resolveHtml });
		expect(htmlResponse.headers.get('cache-control')).toBe('no-cache');

		// Não-HTML: preserva o cache-control pré-existente.
		const resolveJson = async () =>
			new Response('{}', { headers: { 'content-type': 'application/json', 'cache-control': 'public, max-age=3600' } });
		const jsonResponse = await handle({ event, resolve: resolveJson });
		expect(jsonResponse.headers.get('cache-control')).toBe('public, max-age=3600');
	});
});
