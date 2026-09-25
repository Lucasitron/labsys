import type { PageLoad } from './$types';

export const ssr = false;
export const prerender = false;

export const load: PageLoad = () => {
	return { summary: null as null, error: null as null };
};