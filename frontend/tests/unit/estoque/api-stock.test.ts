import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { writable } from 'svelte/store';
import type { ApiError } from '$lib/api/client';
import type { CreateItemPayload } from '$lib/types/stock';

const gotoMock = vi.fn();
const logoutMock = vi.fn();
const authMock = writable<{ user: null; token: string | null; isAuthenticated: boolean }>({
	user: null,
	token: null,
	isAuthenticated: false
});

vi.mock('$app/navigation', () => ({ goto: gotoMock }));
vi.mock('$lib/stores/auth', () => ({ auth: authMock, logout: logoutMock }));
vi.mock('$app/environment', () => ({ browser: true }));

function jsonResponse(body: unknown, status = 200): Response {
	return new Response(JSON.stringify(body), {
		status,
		headers: { 'Content-Type': 'application/json' }
	});
}

function mockFetch(body: unknown, status = 200): ReturnType<typeof vi.fn<typeof fetch>> {
	return vi.fn<typeof fetch>().mockResolvedValue(jsonResponse(body, status));
}

async function freshItems() {
	vi.resetModules();
	return await import('$lib/api/stock/items');
}
async function freshMovements() {
	vi.resetModules();
	return await import('$lib/api/stock/movements');
}
async function freshLoans() {
	vi.resetModules();
	return await import('$lib/api/stock/loans');
}
async function freshSuppliers() {
	vi.resetModules();
	return await import('$lib/api/stock/suppliers');
}
async function freshLocations() {
	vi.resetModules();
	return await import('$lib/api/stock/locations');
}

beforeEach(() => {
	vi.stubEnv('VITE_API_BASE_URL', 'http://api.test');
});

afterEach(() => {
	vi.unstubAllEnvs();
	vi.unstubAllGlobals();
});

describe('estoque/api-stock — items', () => {
	const ITEM_RAW = {
		id: 'it-1',
		nome: 'Parafuso M6',
		categoria: 'INSUMO',
		unidadeMedida: 'un',
		quantidadeAtual: 10,
		estoqueMinimo: 5,
		localizacao: null
	};
	const PAYLOAD: CreateItemPayload = {
		nome: 'Parafuso M6',
		categoria: 'INSUMO',
		unidadeMedida: 'un',
		quantidadeAtual: 10,
		estoqueMinimo: 5
	};

	it('listarItens dispara GET /estoque/itens, parseia List e deriva status', async () => {
		const fetchMock = mockFetch([ITEM_RAW]);
		const { listarItens } = await freshItems();

		const result = await listarItens({}, fetchMock);

		expect(fetchMock).toHaveBeenCalledTimes(1);
		expect(fetchMock.mock.calls[0]![0]).toBe('http://api.test/estoque/itens');
		expect(result).toHaveLength(1);
		expect(result[0]).toMatchObject({ id: 'it-1', status: 'available' });
	});

	it('listarItens serializa filtros categoria, idLocalizacao e baixo na query', async () => {
		const fetchMock = mockFetch([]);
		const { listarItens } = await freshItems();

		await listarItens({ categoria: 'INSUMO', idLocalizacao: 'loc-2', baixo: true }, fetchMock);

		expect(fetchMock.mock.calls[0]![0]).toBe(
			'http://api.test/estoque/itens?categoria=INSUMO&idLocalizacao=loc-2&baixo=1'
		);
	});

	it('listarItens omite baixo quando false', async () => {
		const fetchMock = mockFetch([]);
		const { listarItens } = await freshItems();

		await listarItens({ baixo: false }, fetchMock);

		expect(fetchMock.mock.calls[0]![0]).toBe('http://api.test/estoque/itens');
	});

	it('buscarItem dispara GET /estoque/itens/{id}', async () => {
		const fetchMock = mockFetch({ ...ITEM_RAW, quantidadeAtual: 3 });
		const { buscarItem } = await freshItems();

		const result = await buscarItem('it-9', fetchMock);

		expect(fetchMock.mock.calls[0]![0]).toBe('http://api.test/estoque/itens/it-9');
		expect(result.status).toBe('low');
	});

	it('criarItem faz POST /estoque/itens com body JSON e retorna item com status', async () => {
		const fetchMock = mockFetch(ITEM_RAW, 201);
		vi.stubGlobal('fetch', fetchMock);
		const { criarItem } = await freshItems();

		const result = await criarItem(PAYLOAD);

		const [url, init] = fetchMock.mock.calls[0]!;
		expect(url).toBe('http://api.test/estoque/itens');
		expect(init!.method).toBe('POST');
		expect((init!.headers as Headers).get('Content-Type')).toBe('application/json');
		expect(init!.body).toBe(JSON.stringify(PAYLOAD));
		expect(result.status).toBe('available');
	});

	it('atualizarItem faz PUT /estoque/itens/{id} com body JSON', async () => {
		const fetchMock = mockFetch({ ...ITEM_RAW, estoqueMinimo: 2 });
		vi.stubGlobal('fetch', fetchMock);
		const { atualizarItem } = await freshItems();

		await atualizarItem('it-1', { estoqueMinimo: 2 });

		const [url, init] = fetchMock.mock.calls[0]!;
		expect(url).toBe('http://api.test/estoque/itens/it-1');
		expect(init!.method).toBe('PUT');
		expect(init!.body).toBe(JSON.stringify({ estoqueMinimo: 2 }));
	});

	it('atualizarItem mapeia 409 para ApiError com code e mensagem pt-BR', async () => {
		const fetchMock = mockFetch({ code: 'NOME_DUPLICADO', message: 'Já existe item com esse nome.' }, 409);
		vi.stubGlobal('fetch', fetchMock);
		const { atualizarItem } = await freshItems();
		const { ApiError } = await import('$lib/api/client');

		const err = await atualizarItem('it-1', { nome: 'Duplicado' }).catch((e: unknown) => e);

		expect(err).toBeInstanceOf(ApiError);
		expect((err as ApiError).status).toBe(409);
		expect((err as ApiError).code).toBe('NOME_DUPLICADO');
		expect((err as ApiError).message).toBe('Já existe item com esse nome.');
	});

	it('importarCsv faz POST /estoque/itens/import com FormData (campo arquivo) sem Content-Type manual', async () => {
		const fetchMock = mockFetch([ITEM_RAW]);
		const { importarCsv } = await freshItems();
		const file = new File(['a,b,c'], 'itens.csv', { type: 'text/csv' });

		const result = await importarCsv(file, fetchMock);

		const [url, init] = fetchMock.mock.calls[0]!;
		expect(url).toBe('http://api.test/estoque/itens/import');
		expect(init!.method).toBe('POST');
		expect(init!.body).toBeInstanceOf(FormData);
		expect((init!.body as FormData).get('arquivo')).toBe(file);
		expect((init!.headers as Headers).get('Content-Type')).toBeNull();
		expect(result).toHaveLength(1);
	});

	it('exportarCsv dispara GET /estoque/itens/export e dispara download via blob URL', async () => {
		const originalCreate = URL.createObjectURL;
		const originalRevoke = URL.revokeObjectURL;
		const createObjectURL = vi.fn(() => 'blob:fake');
		const revokeObjectURL = vi.fn();
		const clickMock = vi.fn();
		const createElementSpy = vi
			.spyOn(document, 'createElement')
			.mockImplementation((tag: string) => {
				if (tag.toLowerCase() === 'a') {
					return { href: '', download: '', click: clickMock } as unknown as HTMLElement;
				}
				return document.createElement(tag);
			});
		try {
			Object.defineProperty(URL, 'createObjectURL', { configurable: true, writable: true, value: createObjectURL });
			Object.defineProperty(URL, 'revokeObjectURL', { configurable: true, writable: true, value: revokeObjectURL });
			const fetchMock = vi.fn<typeof fetch>().mockResolvedValue(new Response(new Blob(['a,b,c'])));
			const { exportarCsv } = await freshItems();

			await exportarCsv(fetchMock);

			expect(fetchMock.mock.calls[0]![0]).toBe('http://api.test/estoque/itens/export');
			expect(createObjectURL).toHaveBeenCalledTimes(1);
			expect(clickMock).toHaveBeenCalledTimes(1);
			expect(revokeObjectURL).toHaveBeenCalledWith('blob:fake');
		} finally {
			Object.defineProperty(URL, 'createObjectURL', { configurable: true, writable: true, value: originalCreate });
			Object.defineProperty(URL, 'revokeObjectURL', { configurable: true, writable: true, value: originalRevoke });
			createElementSpy.mockRestore();
		}
	});
});

describe('estoque/api-stock — movements (entradas/saídas PT)', () => {
	const entrada = {
		id: 'e-1',
		idItem: 'it-1',
		quantidade: 10,
		dataEntrada: '2026-09-21'
	};
	const saida = {
		id: 's-1',
		idItem: 'it-1',
		quantidade: 2,
		tipoSaida: 'CONSUMO',
		dataSaida: '2026-09-21'
	};

	it('listarEntradasPorItem usa GET /estoque/entradas?idItem=', async () => {
		const fetchMock = mockFetch([entrada]);
		const { listarEntradasPorItem } = await freshMovements();

		const result = await listarEntradasPorItem('it-1', fetchMock);

		expect(fetchMock.mock.calls[0]![0]).toBe('http://api.test/estoque/entradas?idItem=it-1');
		expect(result[0].id).toBe('e-1');
	});

	it('buscarEntrada usa GET /estoque/entradas/{id}', async () => {
		const fetchMock = mockFetch(entrada);
		const { buscarEntrada } = await freshMovements();

		await buscarEntrada('e-7', fetchMock);

		expect(fetchMock.mock.calls[0]![0]).toBe('http://api.test/estoque/entradas/e-7');
	});

	it('criarEntrada faz POST /estoque/entradas', async () => {
		const fetchMock = mockFetch(entrada, 201);
		vi.stubGlobal('fetch', fetchMock);
		const { criarEntrada } = await freshMovements();

		await criarEntrada({ idItem: 'it-1', quantidade: 10 });

		const [url, init] = fetchMock.mock.calls[0]!;
		expect(url).toBe('http://api.test/estoque/entradas');
		expect(init!.method).toBe('POST');
		expect(init!.body).toBe(JSON.stringify({ idItem: 'it-1', quantidade: 10 }));
	});

	it('listarSaidasPorItem usa GET /estoque/saidas?idItem=', async () => {
		const fetchMock = mockFetch([saida]);
		const { listarSaidasPorItem } = await freshMovements();

		await listarSaidasPorItem('it-2', fetchMock);

		expect(fetchMock.mock.calls[0]![0]).toBe('http://api.test/estoque/saidas?idItem=it-2');
	});

	it('buscarSaida usa GET /estoque/saidas/{id}', async () => {
		const fetchMock = mockFetch(saida);
		const { buscarSaida } = await freshMovements();

		const result = await buscarSaida('s-9', fetchMock);

		expect(fetchMock.mock.calls[0]![0]).toBe('http://api.test/estoque/saidas/s-9');
		expect(result.tipoSaida).toBe('CONSUMO');
	});

	it('criarSaida faz POST /estoque/saidas', async () => {
		const fetchMock = mockFetch(saida, 201);
		vi.stubGlobal('fetch', fetchMock);
		const { criarSaida } = await freshMovements();

		await criarSaida({ idItem: 'it-1', quantidade: 2, tipoSaida: 'CONSUMO' });

		const [url, init] = fetchMock.mock.calls[0]!;
		expect(url).toBe('http://api.test/estoque/saidas');
		expect(init!.method).toBe('POST');
	});
});

describe('estoque/api-stock — loans (empréstimos)', () => {
	const emprestimo = {
		id: 'l-1',
		idItem: 'it-1',
		nomeItem: 'Serra',
		idPessoa: 'p-1',
		quantidade: 1,
		dataEmprestimo: '2026-09-14',
		dataDevolucaoPrevista: '2026-09-28',
		dataDevolucaoReal: null,
		status: 'ATIVO',
		loanComputed: 'no_prazo'
	};
	const LOAN_PAYLOAD = {
		idItem: 'it-1',
		idPessoa: 'p-1',
		quantidade: 1,
		dataDevolucaoPrevista: '2026-09-28'
	};

	it('listarAtrasados usa GET /estoque/emprestimos/atrasados', async () => {
		const fetchMock = mockFetch([emprestimo]);
		const { listarAtrasados } = await freshLoans();

		const result = await listarAtrasados(fetchMock);

		expect(fetchMock.mock.calls[0]![0]).toBe('http://api.test/estoque/emprestimos/atrasados');
		expect(result[0].status).toBe('ATIVO');
	});

	it('buscarEmprestimo usa GET /estoque/emprestimos/{id}', async () => {
		const fetchMock = mockFetch(emprestimo);
		const { buscarEmprestimo } = await freshLoans();

		await buscarEmprestimo('l-3', fetchMock);

		expect(fetchMock.mock.calls[0]![0]).toBe('http://api.test/estoque/emprestimos/l-3');
	});

	it('criarEmprestimo faz POST /estoque/emprestimos com body JSON', async () => {
		const fetchMock = mockFetch(emprestimo, 201);
		vi.stubGlobal('fetch', fetchMock);
		const { criarEmprestimo } = await freshLoans();

		await criarEmprestimo(LOAN_PAYLOAD);

		const [url, init] = fetchMock.mock.calls[0]!;
		expect(url).toBe('http://api.test/estoque/emprestimos');
		expect(init!.method).toBe('POST');
		expect(init!.body).toBe(JSON.stringify(LOAN_PAYLOAD));
	});

	it('devolverEmprestimo faz PUT /estoque/emprestimos/{id}/devolucao sem payload real', async () => {
		const fetchMock = mockFetch({ ...emprestimo, status: 'DEVOLVIDO', dataDevolucaoReal: '2026-09-21' });
		vi.stubGlobal('fetch', fetchMock);
		const { devolverEmprestimo } = await freshLoans();

		const result = await devolverEmprestimo('l-1');

		const [url, init] = fetchMock.mock.calls[0]!;
		expect(url).toBe('http://api.test/estoque/emprestimos/l-1/devolucao');
		expect(init!.method).toBe('PUT');
		expect(init!.body).toBeUndefined();
		expect(result.status).toBe('DEVOLVIDO');
	});
});

describe('estoque/api-stock — fornecedores e localizações', () => {
	it('listarFornecedores usa GET /estoque/fornecedores', async () => {
		const fetchMock = mockFetch([{ id: 'f-1', nome: 'Loja X', contato: 'c', cnpj: '123' }]);
		const { listarFornecedores } = await freshSuppliers();

		const result = await listarFornecedores(fetchMock);

		expect(fetchMock.mock.calls[0]![0]).toBe('http://api.test/estoque/fornecedores');
		expect(result[0].nome).toBe('Loja X');
	});

	it('criarFornecedor faz POST /estoque/fornecedores com {nome,contato,cnpj}', async () => {
		const fetchMock = mockFetch({ id: 'f-2', nome: 'Ferreira', contato: '11', cnpj: '999' }, 201);
		vi.stubGlobal('fetch', fetchMock);
		const { criarFornecedor } = await freshSuppliers();

		await criarFornecedor({ nome: 'Ferreira', contato: '11', cnpj: '999' });

		const [url, init] = fetchMock.mock.calls[0]!;
		expect(url).toBe('http://api.test/estoque/fornecedores');
		expect(init!.method).toBe('POST');
		expect(init!.body).toBe(JSON.stringify({ nome: 'Ferreira', contato: '11', cnpj: '999' }));
	});

	it('listarLocalizacoes usa GET /estoque/localizacoes (array puro)', async () => {
		const fetchMock = mockFetch([{ id: 'loc-1', armario: 'A1' }]);
		const { listarLocalizacoes } = await freshLocations();

		const result = await listarLocalizacoes(fetchMock);

		expect(fetchMock.mock.calls[0]![0]).toBe('http://api.test/estoque/localizacoes');
		expect(Array.isArray(result)).toBe(true);
		expect(result[0].armario).toBe('A1');
	});

	it('criarLocalizacao faz POST /estoque/localizacoes com {armario,prateleira,caixa,descricao}', async () => {
		const fetchMock = mockFetch({ id: 'loc-2', armario: 'B7', prateleira: 'P1', caixa: 'C2', descricao: 'g' }, 201);
		vi.stubGlobal('fetch', fetchMock);
		const { criarLocalizacao } = await freshLocations();

		await criarLocalizacao({ armario: 'B7', prateleira: 'P1', caixa: 'C2', descricao: 'g' });

		const [url, init] = fetchMock.mock.calls[0]!;
		expect(url).toBe('http://api.test/estoque/localizacoes');
		expect(init!.method).toBe('POST');
		expect(init!.body).toBe(JSON.stringify({ armario: 'B7', prateleira: 'P1', caixa: 'C2', descricao: 'g' }));
	});
});

describe('estoque/api-stock — nenhum path /stock/', () => {
	it('todas as chamadas usam exclusivamente /estoque/** (nunca /stock/)', async () => {
		const raw = {
			id: 'a',
			nome: 'A',
			categoria: 'INSUMO',
			unidadeMedida: 'un',
			quantidadeAtual: 5,
			estoqueMinimo: 1,
			localizacao: null
		};
		const urls: string[] = [];
		const fetchMock = vi.fn<typeof fetch>().mockImplementation(async (input) => {
			urls.push(String(input));
			return jsonResponse([raw]);
		});

		const items = await freshItems();
		const movements = await freshMovements();
		const loans = await freshLoans();
		const suppliers = await freshSuppliers();
		const locations = await freshLocations();

		await items.listarItens({}, fetchMock);
		await items.buscarItem('x', fetchMock);
		await movements.listarEntradasPorItem('x', fetchMock);
		await movements.buscarSaida('x', fetchMock);
		await loans.listarAtrasados(fetchMock);
		await loans.buscarEmprestimo('x', fetchMock);
		await suppliers.listarFornecedores(fetchMock);
		await locations.listarLocalizacoes(fetchMock);

		expect(urls.length).toBeGreaterThan(0);
		for (const url of urls) {
			expect(url).toContain('/estoque/');
			expect(url).not.toMatch(/\/stock\//);
		}
	});
});