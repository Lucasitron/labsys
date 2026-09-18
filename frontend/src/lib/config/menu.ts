import type { Module } from '$lib/types/auth';

export type MenuIcon =
	| 'dashboard'
	| 'rh'
	| 'estoque'
	| 'vendas'
	| 'producao'
	| 'financeiro'
	| 'notificacoes'
	| 'configuracoes';

export interface MenuItem {
	label: string;
	path: string;
	module: Module;
	icon: MenuIcon;
}

export const menuItems: MenuItem[] = [
	{ label: 'Dashboard', path: '/dashboard', module: 'dashboard', icon: 'dashboard' },
	{ label: 'Pessoas & RH', path: '/pessoas', module: 'rh', icon: 'rh' },
	{ label: 'Estoque', path: '/estoque', module: 'estoque', icon: 'estoque' },
	{ label: 'Vendas & CRM', path: '/vendas', module: 'vendas', icon: 'vendas' },
	{ label: 'Produção', path: '/producao', module: 'producao', icon: 'producao' },
	{ label: 'Financeiro', path: '/financeiro', module: 'financeiro', icon: 'financeiro' },
	{ label: 'Notificações', path: '/notificacoes', module: 'notificacoes', icon: 'notificacoes' },
	{ label: 'Configurações', path: '/configuracoes', module: 'configuracoes', icon: 'configuracoes' }
];

export interface Crumb {
	label: string;
	path: string;
	current: boolean;
}

const MODULE_LABELS: Record<string, string> = {
	dashboard: 'Dashboard',
	rh: 'Pessoas & RH',
	estoque: 'Estoque',
	vendas: 'Vendas & CRM',
	financeiro: 'Financeiro',
	producao: 'Produção',
	notificacoes: 'Notificações',
	configuracoes: 'Configurações'
};

export function moduleLabel(module: string): string {
	return MODULE_LABELS[module] ?? beautify(module);
}

function beautify(segment: string): string {
	return segment
		.split('-')
		.map((part) => part.charAt(0).toUpperCase() + part.slice(1))
		.join(' ');
}

export function crumbsFor(pathname: string): Crumb[] {
	const match = menuItems.find(
		(item) => pathname === item.path || pathname.startsWith(item.path + '/')
	);

	if (!match) {
		const first = pathname.split('/').filter(Boolean)[0];
		return first
			? [{ label: beautify(first), path: `/${first}`, current: true }]
			: [];
	}

	if (pathname === match.path) {
		return [{ label: match.label, path: match.path, current: true }];
	}

	const remaining = pathname.slice(match.path.length).split('/').filter(Boolean);
	return [
		{ label: match.label, path: match.path, current: false },
		{ label: beautify(remaining[0]), path: pathname, current: true }
	];
}