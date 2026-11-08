const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function isEmail(value: string): boolean {
	const v = value.trim();
	if (!v) return false;
	return EMAIL_RE.test(v);
}