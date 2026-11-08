export const page = $state({
	url: {
		searchParams: new URLSearchParams('')
	},
	params: {}
});

/** @param {string} qs */
/** @param {Record<string, string>} params */
export function resetRhPage(qs = '', params = {}) {
	page.url.searchParams = new URLSearchParams(qs);
	page.params = params;
}

/** @param {string} href */
export function applyRhUrl(href) {
	try {
		const u = new URL(href, 'http://localhost');
		page.url.searchParams = u.searchParams;
	} catch {
		page.url.searchParams = new URLSearchParams('');
	}
}
