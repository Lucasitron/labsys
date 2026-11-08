export const page = $state({
	url: {
		searchParams: new URLSearchParams('')
	}
});

export function resetUrl(qs = '') {
	page.url.searchParams = new URLSearchParams(qs);
}

/** @param {string} href */
export function applyUrl(href) {
	try {
		const u = new URL(href, 'http://localhost');
		page.url.searchParams = u.searchParams;
	} catch {
		page.url.searchParams = new URLSearchParams('');
	}
}