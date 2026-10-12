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
	it('listLancamentos monta /financeiro/lancamentos com status/tipo/idCategoria/dataInicio/dataFim UPPER', async () => {
		apiFetchMock.mockResolvedValue({ lancamentos: [] });
		const { listLancamentos } = await freshLancamentos();

		await listLancamentos({
			status: 'Pendente',
			tipo: 'Saída',
			categoria: '3',
			periodo: '7d'
		});

		const hoje = new Date();
		const fim = hoje.toISOString().slice(0, 10);
		const inicio = new Date(hoje);
		inicio.setDate(inicio.getDate() - 7);
		const dataInicio = inicio.toISOString().slice(0, 10);
		expect(chamada().path).toBe(
			`/financeiro/lancamentos?status=PENDENTE&tipo=SAIDA&idCategoria=3&dataInicio=${dataInicio}&dataFim=${fim}`
		);
	});

	it('listLancamentos omite filtros vazios (path puro sem query)', async () => {
		apiFetchMock.mockResolvedValue({ lancamentos: [] });
		const { listLancamentos } = await freshLancamentos();

		await listLancamentos({});

		expect(chamada().path).toBe('/financeiro/lancamentos');
	});

	it('registrarPagamento faz PUT …/{id}/pagamento com dataPagamento', async () => {
		apiFetchMock.mockResolvedValue({ id: 'l1', status: 'Pago' });
		const { registrarPagamento } = await freshLancamentos();

		await registrarPagamento('l1', { dataPagamento: '2026-09-20' });

		expect(chamada().path).toBe('/financeiro/lancamentos/l1/pagamento');
		expect(chamada().init['method']).toBe('PUT');
		expect(chamada().init['body']).toBe(JSON.stringify({ dataPagamento: '2026-09-20' }));
	});
});

describe('financeiro/api — categorias e doações', () => {
	it('listCategorias usa GET /financeiro/categorias sem query', async () => {
		apiFetchMock.mockResolvedValue([]);
		const { listCategorias } = await freshCategorias();

		await listCategorias();

		expect(chamada().path).toBe('/financeiro/categorias');
		expect(chamada().init['method']).toBeUndefined();
	});

	it('listDoacoes monta query (tipo UPPER + dataInicio/dataFim) e omite vazios', async () => {
		apiFetchMock.mockResolvedValue([]);
		const { listDoacoes } = await freshDoacoes();

		await listDoacoes({ tipo: 'Doação', periodo: '30d' });
		const hoje = new Date();
		const fim = hoje.toISOString().slice(0, 10);
		const inicio = new Date(hoje);
		inicio.setDate(inicio.getDate() - 30);
		const dataInicio = inicio.toISOString().slice(0, 10);
		expect(chamada(0).path).toBe(
			`/financeiro/doacoes-recursos?tipo=DOACAO&dataInicio=${dataInicio}&dataFim=${fim}`
		);

		await listDoacoes({});
		expect(chamada(1).path).toBe('/financeiro/doacoes-recursos');
	});
});

describe('financeiro/api — custeio (congelamento, sem cálculo)', () => {
	it('createFechamento congela: idEncomenda int + números coagidos', async () => {
		apiFetchMock.mockResolvedValue({ id: 1, idEncomenda: 2051, horasEstimadas: '12', valorFechado: '1500', dataFechamento: '2026-09-20', status: 'ABERTA' });
		const { createFechamento } = await freshCusteio();
		const payload = {
			idEncomenda: '2051',
			horasEstimadas: 12,
			valorFechado: 1500,
			dataFechamento: '2026-09-20'
		};

		const fechamento = await createFechamento(payload);

		expect(chamada().path).toBe('/financeiro/fechamento-encomenda');
		expect(chamada().init['method']).toBe('POST');
		expect(chamada().init['body']).toBe(
			JSON.stringify({ idEncomenda: 2051, horasEstimadas: 12, valorFechado: 1500, dataFechamento: '2026-09-20' })
		);
		expect(fechamento.horasEstimadas).toBe(12);
		expect(fechamento.status).toBe('Aberta');
	});

	it('getCusto busca o congelado via GET /financeiro/custos-encomenda/{id}', async () => {
		apiFetchMock.mockResolvedValue({ custoTotal: 900 });
		const { getCusto } = await freshCusteio();

		await getCusto('EN-2051');

		expect(chamada().path).toBe('/financeiro/custos-encomenda/EN-2051');
		expect(chamada().init['method']).toBeUndefined();
	});

	it('concluirCompra faz PUT …/{id}/concluir sem body (back ignora corpo)', async () => {
		apiFetchMock.mockResolvedValue({ id: 1, idItemEstoque: 4, quantidade: '2', valorEstimado: null, status: 'CONCLUIDA', dataSolicitacao: '2026-09-21' });
		const { concluirCompra } = await freshCompras();

		const res = await concluirCompra('c1');

		expect(chamada().path).toBe('/financeiro/solicitacoes-compra/c1/concluir');
		expect(chamada().init['method']).toBe('PUT');
		expect(chamada().init['body']).toBeUndefined();
		expect(res.status).toBe('Concluída');
	});
});

describe('financeiro/api — relatórios (1 endpoint por pane + ?periodo=)', () => {
	it('cada relatório tem endpoint próprio (período só onde o back aceita)', async () => {
		apiFetchMock.mockImplementation(async (path: string) => {
			if (String(path).includes('fluxo-caixa')) {
				return { entradas: '100', saidas: '40', saldo: '60', semanas: [{ semana: '2026-W38', entradas: '100', saidas: '40', liquido: '60' }] };
			}
			if (String(path).includes('lucratividade') || String(path).includes('inadimplencia') || String(path).includes('custo-maquina')) {
				return [];
			}
			if (String(path).includes('doacoes-despesas')) return { saldo: '60', meses: [] };
			return {};
		});
		const rel = await freshRelatorios();

		const fluxo = await rel.getFluxoCaixa({ periodo: '2026-09' });
		expect(fluxo.linhas).toEqual([{ periodo: '2026-W38', entradas: 100, saidas: 40, liquido: 60 }]);
		expect(fluxo.total).toBe(60);
		await rel.getDre({ periodo: '2026-09' });
		await rel.getLucratividade({ periodo: '2026-09' });
		await rel.getInadimplencia({ periodo: '2026-09' });
		await rel.getDoacoesDespesas({ periodo: '2026-09' });
		await rel.getCustoMaquina({ periodo: '2026-09' });

		const paths = apiFetchMock.mock.calls.map((c) => String(c[0]));
		expect(paths).toEqual([
			'/financeiro/relatorios/fluxo-caixa?periodo=2026-09',
			'/financeiro/relatorios/dre?periodo=2026-09',
			'/financeiro/relatorios/lucratividade',
			'/financeiro/relatorios/inadimplencia',
			'/financeiro/relatorios/doacoes-despesas?periodo=2026-09',
			'/financeiro/relatorios/custo-maquina'
		]);
	});

	it('sem periodo o path sai sem query', async () => {
		apiFetchMock.mockResolvedValue({});
		const { getFluxoCaixa } = await freshRelatorios();

		await getFluxoCaixa({});

		expect(chamada().path).toBe('/financeiro/relatorios/fluxo-caixa');
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
		apiFetchMock.mockResolvedValue({
			lancamentos: [
				{ id: 1, idCategoria: 2, tipo: 'ENTRADA', valor: '100', dataVencimento: '2026-09-20', status: 'PENDENTE' }
			]
		});
		const { getLancamento } = await freshLancamentos();

		const lancamento = await getLancamento('1');

		expect((chamada().init['headers'] as Record<string, string>).Authorization).toBe(
			'Bearer tk-fin'
		);
		expect(lancamento.valor).toBe(100);
	});
});

describe('financeiro/api — guarda de paths /financeiro/**', () => {
	it('nenhum path fora de /financeiro/ nas chamadas', async () => {
		apiFetchMock.mockImplementation(async (path: string) => {
			if (String(path).includes('doacoes')) return [];
			if (String(path).includes('categorias')) return [];
			if (String(path).includes('fechamento-encomenda') || String(path).includes('custos-encomenda')) return [];
			if (String(path).includes('solicitacoes-compra')) return [];
			if (String(path).includes('lancamentos')) {
				return {
					lancamentos: [
						{ id: 1, idCategoria: 2, tipo: 'ENTRADA', valor: '100', dataVencimento: '2026-09-20', status: 'PENDENTE' }
					]
				};
			}
			return {};
		});
		const lanc = await freshLancamentos();
		const cat = await freshCategorias();
		const doa = await freshDoacoes();
		const cus = await freshCusteio();
		const com = await freshCompras();
		const rel = await freshRelatorios();

		await lanc.listLancamentos({ status: 'Pendente' });
		await lanc.getLancamento('1');
		await cat.listCategorias();
		await doa.listDoacoes({ tipo: 'Projeto' });
		await cus.listFechamentos();
		await cus.getCusto('EN-1');
		await com.listSolicitacoesCompra();
		await rel.getDre({ periodo: '2026-09' });

		const paths = apiFetchMock.mock.calls.map((c) => String(c[0]));
		expect(paths.length).toBeGreaterThan(0);
		for (const path of paths) {
			expect(path.startsWith('/financeiro/')).toBe(true);
		}
	});
});
