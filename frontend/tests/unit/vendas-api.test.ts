import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { writable } from 'svelte/store';

const { apiFetchMock, ApiError } = vi.hoisted(() => {
	class ApiError extends Error {
		status: number;
		code?: string;
		constructor(status = 500, message = 'Erro inesperado.', code?: string) {
			super(message);
			this.name = 'ApiError';
			this.status = status;
			this.code = code;
		}
	}
	return { apiFetchMock: vi.fn(), ApiError };
});

const authMock = writable<{ user: null; token: string | null; isAuthenticated: boolean }>({
	user: null,
	token: null,
	isAuthenticated: false
});

vi.mock('$lib/api/client', () => ({
	apiFetch: apiFetchMock,
	ApiError
}));

vi.mock('$lib/stores/auth', () => ({
	auth: authMock,
	logout: vi.fn()
}));

vi.mock('$app/navigation', () => ({ goto: vi.fn() }));
vi.mock('$app/environment', () => ({ browser: false }));

async function freshClientes() {
	vi.resetModules();
	return await import('$lib/api/vendas/clientes');
}

async function freshEncomendas() {
	vi.resetModules();
	return await import('$lib/api/vendas/encomendas');
}

async function freshMarketplace() {
	vi.resetModules();
	return await import('$lib/api/vendas/marketplace');
}

async function freshSolicitacoes() {
	vi.resetModules();
	return await import('$lib/api/vendas/solicitacoes');
}

beforeEach(() => {
	apiFetchMock.mockReset();
	authMock.set({ user: null, token: null, isAuthenticated: false });
});

afterEach(() => {
	authMock.set({ user: null, token: null, isAuthenticated: false });
});

function chamada(indice = 0): { path: string; init: Record<string, unknown> } {
	const [path, init] = apiFetchMock.mock.calls[indice] as [string, Record<string, unknown>];
	return { path, init: init ?? {} };
}

describe('vendas/api — clientes (query PT)', () => {
	it('listClientes monta /api/vendas/clientes com search/tipo/tags/ordenar/page/pageSize', async () => {
		apiFetchMock.mockResolvedValue({ clientes: [], pagination: {} });
		const { listClientes } = await freshClientes();

		await listClientes({
			search: 'ana',
			tipo: 'pf',
			tags: 'vip',
			ordenar: 'nome',
			page: 2,
			pageSize: 25
		});

		expect(chamada().path).toBe(
			'/api/vendas/clientes?search=ana&tipo=pf&tags=vip&ordenar=nome&page=2&pageSize=25'
		);
	});

	it('listClientes omite filtros vazios (path puro sem query)', async () => {
		apiFetchMock.mockResolvedValue({ clientes: [], pagination: {} });
		const { listClientes } = await freshClientes();

		await listClientes({});

		expect(chamada().path).toBe('/api/vendas/clientes');
	});
});

describe('vendas/api — encomendas (Kanban)', () => {
	it('listEncomendas usa status_kanban PT e omite quando ausente', async () => {
		apiFetchMock.mockResolvedValue({ encomendas: [], counts: {} });
		const { listEncomendas } = await freshEncomendas();

		await listEncomendas({ status_kanban: 'Produção', search: 'mesa' });
		expect(chamada(0).path).toBe('/api/vendas/encomendas?status_kanban=Produ%C3%A7%C3%A3o&search=mesa');

		await listEncomendas({});
		expect(chamada(1).path).toBe('/api/vendas/encomendas');
	});

	it('moverKanban faz PUT …/{id}/kanban com destino', async () => {
		apiFetchMock.mockResolvedValue({ statusNovo: 'Produção' });
		const { moverKanban } = await freshEncomendas();

		await moverKanban('e1', { statusKanban: 'Produção' });

		expect(chamada().path).toBe('/api/vendas/encomendas/e1/kanban');
		expect(chamada().init['method']).toBe('PUT');
		expect(chamada().init['body']).toBe(JSON.stringify({ statusKanban: 'Produção' }));
	});

	it('createEncomenda converte orçamento via {idOrcamento}', async () => {
		apiFetchMock.mockResolvedValue({ id: 'e9' });
		const { createEncomenda } = await freshEncomendas();

		await createEncomenda({ idOrcamento: 'oc1' });

		expect(chamada().path).toBe('/api/vendas/encomendas');
		expect(chamada().init['method']).toBe('POST');
		expect(chamada().init['body']).toBe(JSON.stringify({ idOrcamento: 'oc1' }));
	});
});

describe('vendas/api — marketplace', () => {
	it('listMarketplace usa plataforma PT e omite quando ausente', async () => {
		apiFetchMock.mockResolvedValue({ registros: [], totais: {} });
		const { listMarketplace } = await freshMarketplace();

		await listMarketplace({ plataforma: 'Shopee', page: 1 });
		expect(chamada(0).path).toBe('/api/vendas/marketplace?plataforma=Shopee&page=1');

		await listMarketplace({});
		expect(chamada(1).path).toBe('/api/vendas/marketplace');
	});

	it('registrarVenda envia valorTaxa numérico (R$, nunca %)', async () => {
		apiFetchMock.mockResolvedValue({ id: 'm1' });
		const { registrarVenda } = await freshMarketplace();
		const payload = {
			encomendaId: 'e1',
			plataforma: 'Shopee' as const,
			codigoExterno: 'SHP-001',
			dataVenda: '2026-09-18',
			valorTaxa: 12.5
		};

		await registrarVenda(payload);

		expect(chamada().path).toBe('/api/vendas/marketplace');
		expect(chamada().init['method']).toBe('POST');
		const enviado = JSON.parse(String(chamada().init['body'])) as { valorTaxa: unknown };
		expect(typeof enviado.valorTaxa).toBe('number');
		expect(enviado.valorTaxa).toBe(12.5);
	});
});

describe('vendas/api — solicitações (decisão com motivo)', () => {
	it('decidirSolicitacao aprova com {aprovada:true}', async () => {
		apiFetchMock.mockResolvedValue({ id: 's1' });
		const { decidirSolicitacao } = await freshSolicitacoes();

		await decidirSolicitacao('s1', { aprovada: true });

		expect(chamada().path).toBe('/api/vendas/solicitacoes/s1');
		expect(chamada().init['method']).toBe('PUT');
		expect(chamada().init['body']).toBe(JSON.stringify({ aprovada: true }));
	});

	it('decidirSolicitacao rejeita com motivo obrigatório', async () => {
		apiFetchMock.mockResolvedValue({ id: 's1' });
		const { decidirSolicitacao } = await freshSolicitacoes();

		await decidirSolicitacao('s1', { aprovada: false, motivo: 'Dados divergentes.' });

		expect(chamada().init['body']).toBe(
			JSON.stringify({ aprovada: false, motivo: 'Dados divergentes.' })
		);
	});

	it('rejeitar sem motivo falha local (400) sem chamar apiFetch', async () => {
		const { decidirSolicitacao } = await freshSolicitacoes();

		await expect(decidirSolicitacao('s1', { aprovada: false })).rejects.toMatchObject({
			status: 400
		});
		expect(apiFetchMock).not.toHaveBeenCalled();
	});
});

describe('vendas/api — erros e bearer', () => {
	it('409 (ex.: cliente duplicado) propaga ApiError', async () => {
		apiFetchMock.mockRejectedValueOnce(new ApiError(409, 'DUPLICADO', 'Cliente já cadastrado.'));
		const { createCliente } = await freshClientes();

		await expect(
			createCliente({
				tipoPessoa: 'pf',
				nome: 'Ana Souza',
				documento: '123',
				email: 'ana@fablab.org',
				telefone: '11999999999'
			})
		).rejects.toMatchObject({ status: 409 });
	});

	it('envia Authorization Bearer quando há token na store', async () => {
		authMock.set({ user: null, token: 'tk-vendas', isAuthenticated: false });
		apiFetchMock.mockResolvedValue({ id: 'c1' });
		const { getCliente } = await freshClientes();

		await getCliente('c1');

		expect((chamada().init['headers'] as Record<string, string>).Authorization).toBe(
			'Bearer tk-vendas'
		);
	});
});

describe('vendas/api — guarda de paths /api/vendas/**', () => {
	it('nenhum path fora de /api/vendas/ nas chamadas', async () => {
		apiFetchMock.mockImplementation(async (path: string) => {
			if (String(path).includes('clientes')) return { clientes: [], pagination: {} };
			if (String(path).includes('encomendas')) return { encomendas: [], counts: {} };
			if (String(path).includes('marketplace')) return { registros: [], totais: {} };
			if (String(path).includes('solicitacoes')) return { solicitacoes: [], counts: {} };
			return {};
		});
		const clientes = await freshClientes();
		const encomendas = await freshEncomendas();
		const marketplace = await freshMarketplace();
		const solicitacoes = await freshSolicitacoes();

		await clientes.listClientes({ search: 'ana' });
		await clientes.getCliente('c1');
		await encomendas.listEncomendas({ status_kanban: 'Fila' });
		await encomendas.moverKanban('e1', { statusKanban: 'Produção' });
		await encomendas.createEncomenda({ idOrcamento: 'oc1' });
		await marketplace.listMarketplace({ plataforma: 'Elo7' });
		await marketplace.registrarVenda({
			encomendaId: 'e1',
			plataforma: 'Elo7',
			codigoExterno: 'E7-1',
			dataVenda: '2026-09-18',
			valorTaxa: 5
		});
		await solicitacoes.listSolicitacoes({ status: 'Pendente' });
		await solicitacoes.decidirSolicitacao('s1', { aprovada: true });

		const paths = apiFetchMock.mock.calls.map((c) => String(c[0]));
		expect(paths.length).toBeGreaterThan(0);
		for (const path of paths) {
			expect(path.startsWith('/api/vendas/')).toBe(true);
		}
	});
});
