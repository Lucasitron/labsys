import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/svelte';
import { auth } from '$lib/stores/auth';
import LancamentosPage from '../../src/routes/(app)/financeiro/lancamentos/+page.svelte';
import ContasPagarPage from '../../src/routes/(app)/financeiro/contas-pagar/+page.svelte';
import ContasReceberPage from '../../src/routes/(app)/financeiro/contas-receber/+page.svelte';
import DoacoesPage from '../../src/routes/(app)/financeiro/doacoes/+page.svelte';
import CusteioPage from '../../src/routes/(app)/financeiro/custeio/+page.svelte';
import RelatoriosPage from '../../src/routes/(app)/financeiro/relatorios/+page.svelte';
import ResumoPage from '../../src/routes/(app)/financeiro/+page.svelte';
import { load as contasPagarLoad } from '../../src/routes/(app)/financeiro/contas-pagar/+page';
import { load as contasReceberLoad } from '../../src/routes/(app)/financeiro/contas-receber/+page';
import type { LancamentosFilterState } from '../../src/routes/(app)/financeiro/lancamentos/+page';
import type { ContasPagarFilterState } from '../../src/routes/(app)/financeiro/contas-pagar/+page';
import type { ContasReceberFilterState } from '../../src/routes/(app)/financeiro/contas-receber/+page';
import type { DoacoesFilterState } from '../../src/routes/(app)/financeiro/doacoes/+page';
import { load as financeiroLayoutLoad } from '../../src/routes/(app)/financeiro/+layout';
import type { User } from '$lib/types/auth';
import type {
	CategoriaFinanceira,
	CusteioTab,
	CustoEncomenda,
	DoacoesResult,
	FechamentoEncomenda,
	Lancamento,
	LancamentosResult,
	SolicitacaoCompra,
	ValorHoraNivel
} from '$lib/types/financeiro';

const mocks = vi.hoisted(() => {
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
	return {
		ApiError,
		goto: vi.fn(),
		invalidateAll: vi.fn(),
		toastSuccess: vi.fn(),
		toastDanger: vi.fn(),
		toastWarn: vi.fn(),
		listLancamentos: vi.fn(),
		getLancamento: vi.fn(),
		createLancamento: vi.fn(),
		registrarPagamento: vi.fn(),
		listCategorias: vi.fn(),
		createCategoria: vi.fn(),
		listDoacoes: vi.fn(),
		createDoacao: vi.fn(),
		listFechamentos: vi.fn(),
		createFechamento: vi.fn(),
		getCusto: vi.fn(),
		listValoresHora: vi.fn(),
		definirValorHora: vi.fn(),
		definirOverhead: vi.fn(),
		listSolicitacoesCompra: vi.fn(),
		createSolicitacaoCompra: vi.fn(),
		concluirCompra: vi.fn()
	};
});

vi.mock('$app/navigation', () => ({ goto: mocks.goto, invalidateAll: mocks.invalidateAll }));
vi.mock('$app/environment', () => ({ browser: false }));
vi.mock('$lib/stores/toast', () => ({
	toasts: {
		success: mocks.toastSuccess,
		danger: mocks.toastDanger,
		warn: mocks.toastWarn,
		info: vi.fn()
	},
	toastError: vi.fn()
}));
vi.mock('$lib/api/financeiro/lancamentos', () => ({
	listLancamentos: mocks.listLancamentos,
	getLancamento: mocks.getLancamento,
	createLancamento: mocks.createLancamento,
	registrarPagamento: mocks.registrarPagamento
}));
vi.mock('$lib/api/financeiro/categorias', () => ({
	listCategorias: mocks.listCategorias,
	createCategoria: mocks.createCategoria
}));
vi.mock('$lib/api/financeiro/doacoes', () => ({
	listDoacoes: mocks.listDoacoes,
	createDoacao: mocks.createDoacao
}));
vi.mock('$lib/api/financeiro/custeio', () => ({
	listFechamentos: mocks.listFechamentos,
	createFechamento: mocks.createFechamento,
	getCusto: mocks.getCusto,
	listValoresHora: mocks.listValoresHora,
	definirValorHora: mocks.definirValorHora,
	definirOverhead: mocks.definirOverhead
}));
vi.mock('$lib/api/financeiro/solicitacoes-compra', () => ({
	listSolicitacoesCompra: mocks.listSolicitacoesCompra,
	createSolicitacaoCompra: mocks.createSolicitacaoCompra,
	concluirCompra: mocks.concluirCompra
}));

const ADMIN: User = {
	id: 'u0',
	username: 'admin',
	name: 'Admin FabLab',
	email: 'admin@fablab.org',
	role: 0
};

const LANCAMENTO: Lancamento = {
	id: 'l1',
	codigo: 'LAN-0001',
	categoria: { id: 'cat-1', nome: 'Mensalidades' },
	tipo: 'Saída',
	valor: 380,
	dataVencimento: '2026-09-10',
	status: 'Pendente'
};

const RESULTADO: LancamentosResult = {
	lancamentos: [LANCAMENTO],
	pagination: { page: 1, pageSize: 10, totalItems: 1, totalPages: 1 },
	counts: { Pendente: 1, Pago: 0, Atrasado: 0, Cancelado: 0 },
	resumo: { entradas: 1240, saidas: 380, pendente: 380 }
};

const RESULTADO_VAZIO: LancamentosResult = {
	lancamentos: [],
	pagination: { page: 1, pageSize: 10, totalItems: 0, totalPages: 1 },
	counts: { Pendente: 0, Pago: 0, Atrasado: 0, Cancelado: 0 },
	resumo: { entradas: 0, saidas: 0, pendente: 0 }
};

const CATEGORIAS: CategoriaFinanceira[] = [
	{ id: 'cat-1', nome: 'Mensalidades', tipo: 'Despesa' }
];

function paramsLanc(overrides: Partial<LancamentosFilterState> = {}): LancamentosFilterState {
	return {
		search: '',
		status: '',
		tipo: '',
		categoria: '',
		periodo: '',
		ordenar: '',
		page: 1,
		pageSize: 10,
		...overrides
	};
}

const FECHAMENTO: FechamentoEncomenda = {
	id: 'f1',
	idEncomenda: 'EN-2051',
	horasEstimadas: 12,
	valorFechado: 1500,
	dataFechamento: '2026-09-20',
	status: 'Aberta'
};

const CUSTO: CustoEncomenda = {
	custoMateriais: 400,
	custoMaoObra: 350,
	custoOverhead: 150,
	custoTotal: 900,
	valorVenda: 1500,
	margemLucro: 600,
	dataCalculo: '2026-09-20'
};

const VALORES: ValorHoraNivel[] = [{ nivelAcesso: 0, valorHora: 45, dataVigencia: '2026-09-01' }];

const COMPRA: SolicitacaoCompra = {
	id: 'c1',
	item: 'Filamento PLA',
	quantidade: 2,
	valorEstimado: 300,
	status: 'Registrada'
};

function dadosCusteio(tab: CusteioTab): {
	user: null;
	tab: CusteioTab;
	fechamentos: FechamentoEncomenda[];
	fechamentosError: null;
	valoresHora: ValorHoraNivel[];
	valoresError: null;
	compras: SolicitacaoCompra[];
	comprasError: null;
} {
	return {
		user: null,
		tab,
		fechamentos: [FECHAMENTO],
		fechamentosError: null,
		valoresHora: VALORES,
		valoresError: null,
		compras: [COMPRA],
		comprasError: null
	};
}

const DOACOES: DoacoesResult = {
	registros: [
		{
			id: 'd1',
			tipo: 'Doação',
			origem: 'Alumni X',
			valor: 500,
			dataRecebimento: '2026-09-05',
			idProjetoAssociado: null
		}
	],
	resumo: { recebidoAno: 500, totalDoacoes: 1, totalProjetos: 0, mediaDoacao: 500 },
	cobertura: { percentual: 40 }
};

beforeEach(() => {
	for (const fn of Object.values(mocks)) {
		if (typeof fn === 'function' && 'mockReset' in fn) (fn as { mockReset: () => void }).mockReset();
	}
	auth.set({ user: null, token: null, isAuthenticated: false });
	mocks.goto.mockImplementation(() => Promise.resolve());
	mocks.getCusto.mockResolvedValue(CUSTO);
	mocks.registrarPagamento.mockResolvedValue({ ...LANCAMENTO, status: 'Pago' });
});

afterEach(() => {
	cleanup();
	auth.set({ user: null, token: null, isAuthenticated: false });
});

describe('financeiro-pages — lançamentos (states + bulk)', () => {
	it('skeleton quando resultado e erro são nulos (sem rows)', () => {
		render(LancamentosPage, {
			props: {
				data: { user: null, params: paramsLanc(), resultado: null, error: null, categorias: [], categoriasError: null },
				params: {}
			}
		});

		expect(screen.queryByTestId('lan-row')).toBeNull();
		expect(document.querySelector('.animate-pulse')).toBeTruthy();
	});

	it('sucesso: rows, Novo lançamento e rodapé servido', () => {
		render(LancamentosPage, {
			props: {
				data: { user: null, params: paramsLanc(), resultado: RESULTADO, error: null, categorias: CATEGORIAS, categoriasError: null },
				params: {}
			}
		});

		expect(screen.getAllByTestId('lan-row').length).toBeGreaterThanOrEqual(1);
		expect(screen.getByTestId('lan-novo')).toBeTruthy();
		expect(screen.getAllByText('Mostrando 1 de 1 lançamentos')).toHaveLength(2);
	});

	it('empty sem filtros mostra CTA de criar', () => {
		render(LancamentosPage, {
			props: {
				data: { user: null, params: paramsLanc(), resultado: RESULTADO_VAZIO, error: null, categorias: [], categoriasError: null },
				params: {}
			}
		});

		expect(screen.getByText('Nenhum lançamento registrado')).toBeTruthy();
		expect(screen.queryByTestId('lan-row')).toBeNull();
	});

	it('empty filtrado mostra chips e Limpar filtros navega sem filtros', async () => {
		render(LancamentosPage, {
			props: {
				data: {
					user: null,
					params: paramsLanc({ search: 'zzz' }),
					resultado: RESULTADO_VAZIO,
					error: null,
					categorias: [],
					categoriasError: null
				},
				params: {}
			}
		});

		expect(screen.getByText('Nenhum lançamento encontrado')).toBeTruthy();
		expect(screen.getByTestId('fin-chips')).toBeTruthy();

		await fireEvent.click(screen.getAllByText('Limpar filtros')[0]!);

		expect(mocks.goto).toHaveBeenCalledWith('/financeiro/lancamentos?page=1');
	});

	it('erro mostra banner e retry refaz via goto com invalidateAll', async () => {
		render(LancamentosPage, {
			props: {
				data: {
					user: null,
					params: paramsLanc(),
					resultado: null,
					error: 'Não foi possível carregar os lançamentos',
					categorias: [],
					categoriasError: null
				},
				params: {}
			}
		});

		expect(screen.getByText('Não foi possível carregar os lançamentos')).toBeTruthy();

		await fireEvent.click(screen.getByTestId('lan-retry'));

		expect(mocks.goto).toHaveBeenCalledWith(expect.stringContaining('/financeiro/lancamentos'), {
			invalidateAll: true
		});
	});

	it('bulk-bar só com ≥1 seleção; registrar pagamento chama API + refetch', async () => {
		render(LancamentosPage, {
			props: {
				data: { user: null, params: paramsLanc(), resultado: RESULTADO, error: null, categorias: CATEGORIAS, categoriasError: null },
				params: {}
			}
		});

		expect(screen.queryByTestId('bulk-bar')).toBeNull();

		await fireEvent.click(screen.getAllByTestId('lan-checkbox')[0]!);
		expect(screen.getByTestId('bulk-bar')).toBeTruthy();
		expect(screen.getByTestId('bulk-count').textContent).toContain('1 selecionados');

		await fireEvent.click(screen.getByText('Registrar pagamento/recebimento'));

		const dataInput = screen.getByLabelText(/Data/) as HTMLInputElement;
		await fireEvent.input(dataInput, { target: { value: '2026-09-20' } });
		await fireEvent.click(screen.getByText('Registrar'));

		await waitFor(() => expect(mocks.registrarPagamento).toHaveBeenCalledTimes(1));
		expect(mocks.registrarPagamento).toHaveBeenCalledWith(
			'l1',
			expect.objectContaining({ dataPagamento: '2026-09-20' })
		);
		await waitFor(() => expect(mocks.invalidateAll).toHaveBeenCalled());
	});

	it('checkbox com stopPropagation não abre o detalhe; linha abre', async () => {
		render(LancamentosPage, {
			props: {
				data: { user: null, params: paramsLanc(), resultado: RESULTADO, error: null, categorias: CATEGORIAS, categoriasError: null },
				params: {}
			}
		});

		await fireEvent.click(screen.getAllByTestId('lan-checkbox')[0]!);
		expect(screen.queryByText('Ficha do lançamento')).toBeNull();

		await fireEvent.click(screen.getAllByTestId('lan-row')[0]!);
		expect(screen.getByText('Ficha do lançamento')).toBeTruthy();
		expect(screen.getAllByText('LAN-0001').length).toBeGreaterThanOrEqual(1);
	});
});

describe('financeiro-pages — contas derivadas (sem endpoint próprio)', () => {
	it('contas-pagar carrega com tipo Saída + status Pendente,Atrasado', async () => {
		mocks.listLancamentos.mockResolvedValue(RESULTADO);
		mocks.listCategorias.mockResolvedValue([]);

		const saida = (await contasPagarLoad({
			url: new URL('http://localhost/financeiro/contas-pagar'),
			fetch
		} as never) as unknown as { params: unknown; resultado: unknown });

		expect(mocks.listLancamentos).toHaveBeenCalledTimes(1);
		expect(mocks.listLancamentos).toHaveBeenCalledWith(
			expect.objectContaining({ tipo: 'Saída', status: 'Pendente,Atrasado' }),
			expect.anything()
		);
		expect(saida.resultado).toBe(RESULTADO);
	});

	it('contas-receber carrega com tipo Entrada + status Pendente,Atrasado', async () => {
		mocks.listLancamentos.mockResolvedValue(RESULTADO);
		mocks.listCategorias.mockResolvedValue([]);

		await contasReceberLoad({
			url: new URL('http://localhost/financeiro/contas-receber'),
			fetch
		} as never);

		expect(mocks.listLancamentos).toHaveBeenCalledTimes(1);
		expect(mocks.listLancamentos).toHaveBeenCalledWith(
			expect.objectContaining({ tipo: 'Entrada', status: 'Pendente,Atrasado' }),
			expect.anything()
		);
	});

	it('contas-pagar renderiza cp-row com ação de pagamento', () => {
		render(ContasPagarPage, {
			props: {
				data: {
					user: null,
					params: { search: '', status: '', vencimento: '', page: 1 },
					resultado: RESULTADO,
					error: null,
					categorias: CATEGORIAS,
					categoriasError: null
				},
				params: {}
			}
		});

		expect(screen.getAllByTestId('cp-row').length).toBeGreaterThanOrEqual(1);
	});

	it('contas-receber renderiza cr-row com ação de recebimento', () => {
		render(ContasReceberPage, {
			props: {
				data: {
					user: null,
					params: { search: '', status: '', origem: '', page: 1 },
					resultado: {
						...RESULTADO,
						lancamentos: [{ ...LANCAMENTO, tipo: 'Entrada' as const }]
					},
					error: null,
					categorias: CATEGORIAS,
					categoriasError: null
				},
				params: {}
			}
		});

		expect(screen.getAllByTestId('cr-row').length).toBeGreaterThanOrEqual(1);
	});
});

describe('financeiro-pages — doações e resumo', () => {
	it('doações: sucesso renderiza doa-row servida', () => {
		render(DoacoesPage, {
			props: {
				data: {
					user: null,
					params: { search: '', tipo: '', periodo: '', page: 1 },
					resultado: DOACOES,
					error: null
				},
				params: {}
			}
		});

		expect(screen.getAllByTestId('doa-row').length).toBeGreaterThanOrEqual(1);
		expect(screen.getAllByText('Alumni X').length).toBeGreaterThanOrEqual(1);
	});

	it('doações: erro mostra banner da tela', () => {
		render(DoacoesPage, {
			props: {
				data: {
					user: null,
					params: { search: '', tipo: '', periodo: '', page: 1 },
					resultado: null,
					error: 'Não foi possível carregar as doações e recursos'
				},
				params: {}
			}
		});

		expect(screen.getByText('Não foi possível carregar as doações e recursos')).toBeTruthy();
	});

	it('resumo: sucesso exibe fin-saldo servido; erro exibe banner', () => {
		const { unmount } = render(ResumoPage, {
			props: {
				data: { user: null, resultado: RESULTADO, error: null, categorias: [], categoriasError: null },
				params: {}
			}
		});

		expect(screen.getByTestId('fin-saldo')).toBeTruthy();
		unmount();
		cleanup();

		render(ResumoPage, {
			props: {
				data: {
					user: null,
					resultado: null,
					error: 'Não foi possível carregar o resumo financeiro',
					categorias: [],
					categoriasError: null
				},
				params: {}
			}
		});

		expect(screen.getByText('Não foi possível carregar o resumo financeiro')).toBeTruthy();
	});
});

describe('financeiro-pages — custeio (?tab= troca pane + notas)', () => {
	it('aba fechamentos exibe nota de congelamento', () => {
		render(CusteioPage, { props: { data: dadosCusteio('fechamentos'), params: {} } });

		expect(screen.getByTestId('custeio-tabs')).toBeTruthy();
		expect(screen.getByText(/ficam congelados após a criação/)).toBeTruthy();
		expect(screen.getAllByText('EN-2051').length).toBeGreaterThanOrEqual(1);
	});

	it('aba custos exibe pane cus-tab-custos com custo servido (sem recálculo)', async () => {
		render(CusteioPage, { props: { data: dadosCusteio('custos'), params: {} } });

		expect(screen.getByTestId('cus-tab-custos')).toBeTruthy();
		await waitFor(() => expect(mocks.getCusto).toHaveBeenCalledWith('EN-2051'));
	});

	it('aba compras exibe aviso informativo de ausência de bloqueio', () => {
		render(CusteioPage, { props: { data: dadosCusteio('compras'), params: {} } });

		expect(screen.getByText(/não há bloqueio/)).toBeTruthy();
		expect(screen.getByText('Filamento PLA')).toBeTruthy();
	});

	it('trocar de aba navega via ?tab=', async () => {
		render(CusteioPage, { props: { data: dadosCusteio('fechamentos'), params: {} } });

		await fireEvent.click(screen.getByRole('tab', { name: 'Custo por encomenda' }));

		expect(mocks.goto).toHaveBeenCalledWith('/financeiro/custeio?tab=custos');
	});

	it('erro em fechamentos mostra banner da aba', () => {
		render(CusteioPage, {
			props: { data: { ...dadosCusteio('fechamentos'), fechamentos: [], fechamentosError: 'falha' }, params: {} }
		});

		expect(screen.getByText('Não foi possível carregar os fechamentos')).toBeTruthy();
	});
});

describe('financeiro-pages — relatórios (?relatorio= troca pane, sem recálculo)', () => {
	it('pane fluxo renderiza o servido (linhas + total)', () => {
		render(RelatoriosPage, {
			props: {
				data: {
					user: null,
					relatorio: 'fluxo',
					periodo: '2026-09',
					dados: {
						linhas: [{ periodo: 'Semana 1', entradas: 1000, saidas: 400, liquido: 600 }],
						total: 600
					},
					error: null
				},
				params: {}
			}
		});

		expect(screen.getByTestId('rel-table')).toBeTruthy();
		expect(screen.getByText('Semana 1')).toBeTruthy();
		expect(
			screen.getByRole('button', { name: 'Ver relatório Fluxo de caixa' }).getAttribute('aria-pressed')
		).toBe('true');
	});

	it('pane dre reflete a URL (?relatorio=dre) com resultado servido', () => {
		render(RelatoriosPage, {
			props: {
				data: {
					user: null,
					relatorio: 'dre',
					periodo: '2026-09',
					dados: {
						receitaOperacional: 5000,
						custosDiretos: 2000,
						despesasOperacionais: 800,
						doacoesRecursos: 1200,
						resultado: 3400
					},
					error: null
				},
				params: {}
			}
		});

		expect(screen.getByText('Receita operacional (+)')).toBeTruthy();
		expect(screen.getByText('Resultado do período')).toBeTruthy();
		expect(
			screen.getByRole('button', { name: 'Ver relatório DRE' }).getAttribute('aria-pressed')
		).toBe('true');
	});

	it('trocar de relatório navega com ?relatorio= + ?periodo=', async () => {
		render(RelatoriosPage, {
			props: {
				data: {
					user: null,
					relatorio: 'fluxo',
					periodo: '2026-09',
					dados: { linhas: [], total: 0 },
					error: null
				},
				params: {}
			}
		});

		await fireEvent.click(screen.getByRole('button', { name: 'Ver relatório DRE' }));

		expect(mocks.goto).toHaveBeenCalledWith('/financeiro/relatorios?relatorio=dre&periodo=2026-09');
	});

	it('erro mostra banner e retry usa invalidateAll', async () => {
		render(RelatoriosPage, {
			props: {
				data: {
					user: null,
					relatorio: 'fluxo',
					periodo: '2026-09',
					dados: { linhas: [], total: 0 },
					error: 'Não foi possível carregar o relatório'
				},
				params: {}
			}
		});

		expect(screen.getByText('Não foi possível carregar o relatório')).toBeTruthy();

		await fireEvent.click(screen.getByTestId('rel-retry'));

		expect(mocks.invalidateAll).toHaveBeenCalled();
	});
});

describe('financeiro-pages — guarda redireciona não-Admin', () => {
	it('não-Admin recebe 303 para /dashboard sem renderizar', () => {
		auth.set({ user: { ...ADMIN, id: 'u1', role: 2 }, token: 'tk', isAuthenticated: true });

		try {
			financeiroLayoutLoad();
			expect.unreachable('deveria redirecionar não-admin para /dashboard');
		} catch (e) {
			const redirect = e as { status: number; location: string };
			expect(redirect.status).toBe(303);
			expect(redirect.location).toBe('/dashboard');
		}
	});
});
