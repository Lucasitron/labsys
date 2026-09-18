import type { PageLoad } from './$types';
import { fetchSummary } from '$lib/api/dashboard';

export const ssr = false;
export const prerender = false;

export const load: PageLoad = async ({ fetch }) => {
	try {
		const summary = await fetchSummary(fetch);
		return { summary, error: null as string | null };
	} catch {
		return { summary: null, error: 'Não foi possível carregar o dashboard' };
	}
};