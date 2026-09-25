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

async function freshLancamentos(): Promise<typeof import('$lib/api/financeiro/lancamentos')> {
	vi.resetModules();
	return await import('$lib/api/financeiro/lancamentos');
}

async function freshCategorias(): Promise<typeof import('$lib/api/financeiro/categorias')> {
	vi.resetModules();
	return await import('$lib/api/financeiro/categorias');
}

async function freshDoacoes(): Promise<typeof import('$lib/api/financeiro/doacoes')> {
	vi.resetModules();
	return await import('$lib/api/financeiro/doacoes');
}

async function freshCusteio(): Promise<typeof import('$lib/api/financeiro/custeio')> {
	vi.resetModules();
	return await import('$lib/api/financeiro/custeio');
}

async function freshCompras(): Promise<
	typeof import('$lib/api/financeiro/solicitacoes-compra')
> {
	vi.resetModules();
	return await import('$lib/api/financeiro/solicitacoes-compra');
}

async function freshRelatorios(): Promise<typeof import('$lib/api/financeiro/relatorios')> {
	vi.resetModules();
	return await import('$lib/api/financeiro/relatorios');
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

describe('financeiro/api — lançamentos (query PT)', () => {
	it('listLancamentos monta /api/financeiro/lancamentos com status/tipo/categoria/periodo', async () => {
		apiFetchMock.mockResolvedValue({ lancamentos: [], pagination: {} });
		const { listLancamentos } = await freshLancamentos();

		await listLancamentos({
			search: 'aluguel',
			status: 'Pendente',
			tipo: 'Saída',
			categoria: 'cat-1',
			periodo: 'mes',
			ordenar: 'vencimento',
			page: 1,
			pageSize: 10
		});

		expect(chamada().path).toBe(
			'/api/financeiro/lancamentos?search=aluguel&status=Pendente&tipo=Sa%C3%ADda&categoria=cat-1&periodo=mes&ordenar=vencimento&page=1&pageSize=10'
		);
	});

	it('listLancamentos omite filtros vazios (path puro sem query)', async () => {
		apiFetchMock.mockResolvedValue({ lancamentos: [], pagination: {} });
		const { listLancamentos } = await freshLancamentos();

		await listLancamentos({});

		expect(chamada().path).toBe('/api/financeiro/lancamentos');
	});

	it('registrarPagamento faz PUT …/{id}/pagamento com dataPagamento', async () => {
		apiFetchMock.mockResolvedValue({ id: 'l1', status: 'Pago' });
		const { registrarPagamento } = await freshLancamentos();

		await registrarPagamento('l1', { dataPagamento: '2026-09-20' });

		expect(chamada().path).toBe('/api/financeiro/lancamentos/l1/pagamento');
		expect(chamada().init['method']).toBe('PUT');
		expect(chamada().init['body']).toBe(JSON.stringify({ dataPagamento: '2026-09-20' }));
	});
});

describe('financeiro/api — categorias e doações', () => {
	it('listCategorias usa GET /api/financeiro/categorias sem query', async () => {
		apiFetchMock.mockResolvedValue([]);
		const { listCategorias } = await freshCategorias();

		await listCategorias();

		expect(chamada().path).toBe('/api/financeiro/categorias');
		expect(chamada().init['method']).toBeUndefined();
	});

	it('listDoacoes monta query PT (tipo/periodo) e omite vazios', async () => {
		apiFetchMock.mockResolvedValue({ registros: [], resumo: {}, cobertura: {} });
		const { listDoacoes } = await freshDoacoes();

		await listDoacoes({ search: 'alumni', tipo: 'Doação', periodo: '2026', page: 2 });
		expect(chamada(0).path).toBe(
			'/api/financeiro/doacoes-recursos?search=alumni&tipo=Doa%C3%A7%C3%A3o&periodo=2026&page=2'
		);

		await listDoacoes({});
		expect(chamada(1).path).toBe('/api/financeiro/doacoes-recursos');
	});
});

describe('financeiro/api — custeio (congelamento, sem cálculo)', () => {
	it('createFechamento congela: payload só com idEncomenda/horas/valor/data', async () => {
		apiFetchMock.mockResolvedValue({ id: 'f1' });
		const { createFechamento } = await freshCusteio();
		const payload = {
			idEncomenda: 'EN-2051',
			horasEstimadas: 12,
			valorFechado: 1500,
			dataFechamento: '2026-09-20'
		};

		await createFechamento(payload);

		expect(chamada().path).toBe('/api/financeiro/fechamento-encomenda');
		expect(chamada().init['method']).toBe('POST');
		expect(chamada().init['body']).toBe(JSON.stringify(payload));
		const enviado = JSON.parse(String(chamada().init['body'])) as Record<string, unknown>;
		expect(Object.keys(enviado).sort()).toEqual(
			['dataFechamento', 'horasEstimadas', 'idEncomenda', 'valorFechado'].sort()
		);
	});

	it('getCusto busca o congelado via GET /api/financeiro/custos-encomenda/{id}', async () => {
		apiFetchMock.mockResolvedValue({ custoTotal: 900 });
		const { getCusto } = await freshCusteio();

		await getCusto('EN-2051');

		expect(chamada().path).toBe('/api/financeiro/custos-encomenda/EN-2051');
		expect(chamada().init['method']).toBeUndefined();
	});

	it('concluirCompra faz PUT …/{id}/concluir com dataConclusao/valorReal', async () => {
		apiFetchMock.mockResolvedValue({ status: 'Concluída', lancamentoGerado: 'LAN-0009' });
		const { concluirCompra } = await freshCompras();

		await concluirCompra('c1', { dataConclusao: '2026-09-21', valorReal: 320.5 });

		expect(chamada().path).toBe('/api/financeiro/solicitacoes-compra/c1/concluir');
		expect(chamada().init['method']).toBe('PUT');
		expect(chamada().init['body']).toBe(
			JSON.stringify({ dataConclusao: '2026-09-21', valorReal: 320.5 })
		);
	});
});

describe('financeiro/api — relatórios (1 endpoint por pane + ?periodo=)', () => {
	it('cada relatório tem endpoint próprio com ?periodo=', async () => {
		apiFetchMock.mockResolvedValue({});
		const rel = await freshRelatorios();

		await rel.getFluxoCaixa({ periodo: '2026-09' });
		await rel.getDre({ periodo: '2026-09' });
		await rel.getLucratividade({ periodo: '2026-09' });
		await rel.getInadimplencia({ periodo: '2026-09' });
		await rel.getDoacoesDespesas({ periodo: '2026-09' });
		await rel.getCustoMaquina({ periodo: '2026-09' });

		const paths = apiFetchMock.mock.calls.map((c) => String(c[0]));
		expect(paths).toEqual([
			'/api/financeiro/relatorios/fluxo-caixa?periodo=2026-09',
			'/api/financeiro/relatorios/dre?periodo=2026-09',
			'/api/financeiro/relatorios/lucratividade?periodo=2026-09',
			'/api/financeiro/relatorios/inadimplencia?periodo=2026-09',
			'/api/financeiro/relatorios/doacoes-despesas?periodo=2026-09',
			'/api/financeiro/relatorios/custo-maquina?periodo=2026-09'
		]);
	});

	it('sem periodo o path sai sem query', async () => {
		apiFetchMock.mockResolvedValue({});
		const { getFluxoCaixa } = await freshRelatorios();

		await getFluxoCaixa({});

		expect(chamada().path).toBe('/api/financeiro/relatorios/fluxo-caixa');
	});
});

describe('financeiro/api — erros e bearer', () => {
	it('409 (ex.: pagamento duplicado) propaga ApiError', async () => {
		apiFetchMock.mockRejectedValueOnce(new ApiError(409, 'CONFLITO', 'Baixa já registrada.'));
		const { registrarPagamento } = await freshLancamentos();

		await expect(registrarPagamento('l1', { dataPagamento: '2026-09-20' })).rejects.toMatchObject(
			{ status: 409 }
		);
	});

	it('422 (ex.: fechamento inválido) propaga ApiError', async () => {
		apiFetchMock.mockRejectedValueOnce(new ApiError(422, 'VALIDACAO', 'Horas inválidas.'));
		const { createFechamento } = await freshCusteio();

		await expect(
			createFechamento({
				idEncomenda: 'EN-2051',
				horasEstimadas: -1,
				valorFechado: 1500,
				dataFechamento: '2026-09-20'
			})
		).rejects.toMatchObject({ status: 422 });
	});

	it('envia Authorization Bearer quando há token na store', async () => {
		authMock.set({ user: null, token: 'tk-fin', isAuthenticated: false });
		apiFetchMock.mockResolvedValue({ id: 'l1' });
		const { getLancamento } = await freshLancamentos();

		await getLancamento('l1');

		expect((chamada().init['headers'] as Record<string, string>).Authorization).toBe(
			'Bearer tk-fin'
		);
	});
});

describe('financeiro/api — guarda de paths /api/financeiro/**', () => {
	it('nenhum path fora de /api/financeiro/ nas chamadas', async () => {
		apiFetchMock.mockResolvedValue({});
		const lanc = await freshLancamentos();
		const cat = await freshCategorias();
		const doa = await freshDoacoes();
		const cus = await freshCusteio();
		const com = await freshCompras();
		const rel = await freshRelatorios();

		await lanc.listLancamentos({ status: 'Pendente' });
		await lanc.getLancamento('l1');
		await cat.listCategorias();
		await doa.listDoacoes({ tipo: 'Projeto' });
		await cus.listFechamentos();
		await cus.getCusto('EN-1');
		await com.listSolicitacoesCompra();
		await rel.getDre({ periodo: '2026-09' });

		const paths = apiFetchMock.mock.calls.map((c) => String(c[0]));
		expect(paths.length).toBeGreaterThan(0);
		for (const path of paths) {
			expect(path.startsWith('/api/financeiro/')).toBe(true);
		}
	});
});
