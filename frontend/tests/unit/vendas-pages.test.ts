import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/svelte';
import { auth } from '$lib/stores/auth';
import ClientesPage from '../../src/routes/(app)/vendas/clientes/+page.svelte';
import ClienteDetPage from '../../src/routes/(app)/vendas/clientes/[id]/+page.svelte';
import EncomendasPage from '../../src/routes/(app)/vendas/encomendas/+page.svelte';
import OrcDetPage from '../../src/routes/(app)/vendas/orcamentos/[id]/+page.svelte';
import SolPage from '../../src/routes/(app)/vendas/solicitacoes/+page.svelte';
import { load as vendasLayoutLoad } from '../../src/routes/(app)/vendas/+layout';
import type { User } from '$lib/types/auth';
import type {
	Cliente,
	ClientesResult,
	Encomenda,
	EncomendasResult,
	Orcamento,
	SolicitacaoEdicao,
	SolicitacoesResult
} from '$lib/types/vendas';

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
	class NetworkError extends Error {
		constructor(message = 'Falha de conexão. Verifique sua rede.') {
			super(message);
			this.name = 'NetworkError';
		}
	}
	return {
		ApiError,
		NetworkError,
		apiFetch: vi.fn(),
		goto: vi.fn(),
		invalidateAll: vi.fn(),
		toastSuccess: vi.fn(),
		toastDanger: vi.fn(),
		toastWarn: vi.fn(),
		listClientes: vi.fn(),
		getCliente: vi.fn(),
		createCliente: vi.fn(),
		updateCliente: vi.fn(),
		deleteCliente: vi.fn(),
		listTags: vi.fn(),
		createTag: vi.fn(),
		bulkAdicionarTag: vi.fn(),
		listOrcamentos: vi.fn(),
		getOrcamento: vi.fn(),
		createOrcamento: vi.fn(),
		updateOrcamento: vi.fn(),
		listEncomendas: vi.fn(),
		getEncomenda: vi.fn(),
		createEncomenda: vi.fn(),
		moverKanban: vi.fn(),
		listSolicitacoes: vi.fn(),
		createSolicitacao: vi.fn(),
		decidirSolicitacao: vi.fn(),
		listInteracoes: vi.fn(),
		registrarInteracao: vi.fn()
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
vi.mock('$lib/api/client', () => ({
	apiFetch: mocks.apiFetch,
	ApiError: mocks.ApiError,
	NetworkError: mocks.NetworkError,
	API_BASE: '',
	sanitizeRedirect: (href: string) => href
}));
vi.mock('$lib/api/vendas/clientes', () => ({
	listClientes: mocks.listClientes,
	getCliente: mocks.getCliente,
	createCliente: mocks.createCliente,
	updateCliente: mocks.updateCliente,
	deleteCliente: mocks.deleteCliente,
	listTags: mocks.listTags,
	createTag: mocks.createTag,
	bulkAdicionarTag: mocks.bulkAdicionarTag
}));
vi.mock('$lib/api/vendas/orcamentos', () => ({
	listOrcamentos: mocks.listOrcamentos,
	getOrcamento: mocks.getOrcamento,
	createOrcamento: mocks.createOrcamento,
	updateOrcamento: mocks.updateOrcamento
}));
vi.mock('$lib/api/vendas/encomendas', () => ({
	listEncomendas: mocks.listEncomendas,
	getEncomenda: mocks.getEncomenda,
	createEncomenda: mocks.createEncomenda,
	moverKanban: mocks.moverKanban
}));
vi.mock('$lib/api/vendas/solicitacoes', () => ({
	listSolicitacoes: mocks.listSolicitacoes,
	createSolicitacao: mocks.createSolicitacao,
	decidirSolicitacao: mocks.decidirSolicitacao
}));
vi.mock('$lib/api/vendas/interacoes', () => ({
	listInteracoes: mocks.listInteracoes,
	registrarInteracao: mocks.registrarInteracao
}));

const ADMIN: User = {
	id: 'u0',
	username: 'admin',
	name: 'Admin FabLab',
	email: 'admin@fablab.org',
	role: 0
};

const CRIADOR: User = {
	id: 'u9',
	username: 'criador',
	name: 'Criador Vendas',
	email: 'criador@fablab.org',
	role: 2
};

const OUTRO: User = {
	id: 'u7',
	username: 'outro',
	name: 'Outro Membro',
	email: 'outro@fablab.org',
	role: 2
};

const CLIENTE: Cliente = {
	id: 'c1',
	codigo: 'CLI-0001',
	tipoPessoa: 'pf',
	nome: 'Ana Souza',
	documento: '123.456.789-00',
	email: 'ana@fablab.org',
	telefone: '(11) 99999-9999',
	tags: [],
	dataCadastro: '2026-01-10',
	createdBy: 'u9',
	updatedAt: '2026-09-01'
};

const PARAMS_CLIENTES = { search: '', tipo: '', tags: '', ordenar: '', page: 1, pageSize: 10 };

const RESULTADO_CLIENTES: ClientesResult = {
	clientes: [CLIENTE],
	pagination: { page: 1, pageSize: 10, totalItems: 1, totalPages: 1 }
};

const RESULTADO_VAZIO: ClientesResult = {
	clientes: [],
	pagination: { page: 1, pageSize: 10, totalItems: 0, totalPages: 1 }
};

const ENCOMENDA: Encomenda = {
	id: 'e1',
	codigo: 'EN-0001',
	cliente: { id: 'c1', nome: 'Ana Souza' },
	statusKanban: 'Fila',
	origem: 'Venda direta',
	previsao: '2026-12-31',
	valorFinal: 500,
	itensCount: 2,
	createdBy: 'u9'
};

const ORCAMENTO_BASE: Orcamento = {
	id: 'oc1',
	codigo: 'OC-0001',
	cliente: { id: 'c1', nome: 'Ana Souza' },
	valorTotal: 1240,
	status: 'Aprovado',
	validade: '2026-12-31',
	qtdItens: 2,
	createdBy: 'u9'
};

const SOLICITACAO: SolicitacaoEdicao = {
	id: 's1',
	tipo: 'Alteração de dados',
	alvo: { tipo: 'CLI', id: 'c1', nome: 'Ana Souza' },
	campo: 'telefone',
	valorAtual: '(11) 11111-1111',
	valorProposto: '(11) 99999-9999',
	justificativa: 'Número novo da cliente.',
	status: 'Pendente',
	solicitante: 'Maria Santos',
	createdBy: 'u2'
};

beforeEach(() => {
	for (const fn of Object.values(mocks)) {
		if (typeof fn === 'function' && 'mockReset' in fn) (fn as { mockReset: () => void }).mockReset();
	}
	auth.set({ user: null, token: null, isAuthenticated: false });
	mocks.listTags.mockResolvedValue([]);
	mocks.apiFetch.mockResolvedValue(undefined);
});

afterEach(() => {
	cleanup();
	auth.set({ user: null, token: null, isAuthenticated: false });
});

describe('vendas-pages — lista de clientes (states)', () => {
	it('skeleton quando resultado e erro são nulos (sem rows)', () => {
		render(ClientesPage, {
			// Skeleton (resultado+erro nulos) não é estado do load: cast local.
			props: { data: { user: null, canEdit: false, params: PARAMS_CLIENTES, resultado: null, error: null } as never, params: {} }
		});

		expect(screen.queryByTestId('cliente-row')).toBeNull();
		expect(document.querySelector('.animate-pulse')).toBeTruthy();
	});

	it('sucesso: rows desktop+mobile por cliente', () => {
		render(ClientesPage, {
			props: { data: { user: null, canEdit: false, params: PARAMS_CLIENTES, resultado: RESULTADO_CLIENTES, error: null }, params: {} }
		});

		expect(screen.getAllByTestId('cliente-row')).toHaveLength(2);
		expect(screen.getAllByText('Ana Souza').length).toBeGreaterThanOrEqual(1);
	});

	it('empty sem filtros mostra CTA de cadastro', () => {
		render(ClientesPage, {
			props: { data: { user: null, canEdit: false, params: PARAMS_CLIENTES, resultado: RESULTADO_VAZIO, error: null }, params: {} }
		});

		expect(screen.getByText('Nenhum cliente cadastrado')).toBeTruthy();
		expect(screen.queryByTestId('cliente-row')).toBeNull();
	});

	it('empty filtrado mostra chips e Limpar filtros navega sem filtros', async () => {
		render(ClientesPage, {
			props: {
				data: {
					user: null,
					canEdit: false,
					params: { ...PARAMS_CLIENTES, search: 'zzz' },
					resultado: RESULTADO_VAZIO,
					error: null
				},
				params: {}
			}
		});

		expect(screen.getByText('Nenhum cliente encontrado')).toBeTruthy();
		expect(screen.getByText('Busca: zzz')).toBeTruthy();

		await fireEvent.click(screen.getAllByText('Limpar filtros')[0]!);

		expect(mocks.goto).toHaveBeenCalledWith('/vendas/clientes?page=1');
	});

	it('erro mostra banner e Tentar novamente refaz via goto', async () => {
		render(ClientesPage, {
			props: {
				data: { user: null, canEdit: false, params: PARAMS_CLIENTES, resultado: null, error: 'Falha de rede.' },
				params: {}
			}
		});

		expect(screen.getByText('Não foi possível carregar os clientes')).toBeTruthy();

		await fireEvent.click(screen.getByTestId('cliente-retry'));

		expect(mocks.goto).toHaveBeenCalledWith('/vendas/clientes', { invalidateAll: true });
	});
});

describe('vendas-pages — RBAC hide (Sugerir alteração × Editar)', () => {
	it('!canEdit na lista: menu mostra "Sugerir alteração" e omite Editar do DOM', async () => {
		auth.set({ user: OUTRO, token: 'tk', isAuthenticated: true });
		render(ClientesPage, {
			props: { data: { user: null, canEdit: false, params: PARAMS_CLIENTES, resultado: RESULTADO_CLIENTES, error: null }, params: {} }
		});

		await fireEvent.click(screen.getAllByLabelText('Ações de Ana Souza')[0]!);

		expect(screen.getByText('Sugerir alteração')).toBeTruthy();
		expect(screen.queryByText('Editar')).toBeNull();
	});

	it('canEdit (criador) na lista: menu mostra Editar e omite Sugerir', async () => {
		auth.set({ user: CRIADOR, token: 'tk', isAuthenticated: true });
		render(ClientesPage, {
			props: { data: { user: null, canEdit: false, params: PARAMS_CLIENTES, resultado: RESULTADO_CLIENTES, error: null }, params: {} }
		});

		await fireEvent.click(screen.getAllByLabelText('Ações de Ana Souza')[0]!);

		expect(screen.getByText('Editar')).toBeTruthy();
		expect(screen.queryByText('Sugerir alteração')).toBeNull();
	});

	it('!canEdit no detalhe do cliente: "Sugerir alteração" visível, Editar fora do DOM', () => {
		auth.set({ user: OUTRO, token: 'tk', isAuthenticated: true });
		render(ClienteDetPage, {
			props: {
				data: { user: null, canEdit: false, id: 'c1', tab: 'visao-geral', cliente: CLIENTE, notFound: false, error: null },
				params: { id: 'c1' }
			}
		});

		expect(screen.getByText('Sugerir alteração')).toBeTruthy();
		expect(screen.queryByText('Editar')).toBeNull();
	});

	it('!canEdit no detalhe do orçamento: "Sugerir alteração" visível, Editar fora do DOM', () => {
		auth.set({ user: OUTRO, token: 'tk', isAuthenticated: true });
		render(OrcDetPage, {
			props: {
				data: { user: null, canEdit: false, id: 'oc1', orcamento: ORCAMENTO_BASE, notFound: false, error: null },
				params: { id: 'oc1' }
			}
		});

		expect(screen.getByText('Sugerir alteração')).toBeTruthy();
		expect(screen.queryByText('Editar')).toBeNull();
	});
});

describe('vendas-pages — Kanban de encomendas (moverKanban)', () => {
	function dadosKanban() {
		return {
			user: null,
			canEdit: false,
			params: { status_kanban: '', search: '', clienteId: '' },
			resultado: {
				encomendas: [ENCOMENDA],
				counts: { Produção: 0, Pronto: 0, Atrasadas: 0, 'Entregues no mês': 0 }
			},
			error: null as null
		};
	}

	it('colunas e cards renderizam (kanban-col-producao + en-card)', () => {
		render(EncomendasPage, { props: { data: dadosKanban(), params: {} } });

		expect(screen.getByTestId('kanban-col-producao')).toBeTruthy();
		expect(screen.getAllByTestId('en-card')).toHaveLength(1);
	});

	it('mover chama moverKanban com destino, toast e refetch', async () => {
		mocks.moverKanban.mockResolvedValue({ statusNovo: 'Produção' });
		render(EncomendasPage, { props: { data: dadosKanban(), params: {} } });

		await fireEvent.click(screen.getByTestId('move-ene1'));
		await fireEvent.click(screen.getByText('Mover para Produção'));

		await waitFor(() =>
			expect(mocks.moverKanban).toHaveBeenCalledWith('e1', { statusKanban: 'Produção' })
		);
		expect(mocks.toastSuccess).toHaveBeenCalledWith('Encomenda EN-0001 movida para Produção.');
		await waitFor(() => expect(mocks.invalidateAll).toHaveBeenCalled());
	});

	it('409 no move vira toast "atualize a tela" (sem refetch silencioso)', async () => {
		mocks.moverKanban.mockRejectedValueOnce(new mocks.ApiError(409, 'CONFLITO', 'Versão antiga.'));
		render(EncomendasPage, { props: { data: dadosKanban(), params: {} } });

		await fireEvent.click(screen.getByTestId('move-ene1'));
		await fireEvent.click(screen.getByText('Mover para Produção'));

		await waitFor(() =>
			expect(mocks.toastWarn).toHaveBeenCalledWith('Conflito de versão — atualize a tela.')
		);
	});
});

describe('vendas-pages — detalhe do orçamento (banner de conversão)', () => {
	it('banner só em Aprovado sem encomenda', () => {
		render(OrcDetPage, {
			props: {
				data: { user: null, canEdit: false, id: 'oc1', orcamento: ORCAMENTO_BASE, notFound: false, error: null },
				params: { id: 'oc1' }
			}
		});

		expect(screen.getByTestId('orc-criar-encomenda')).toBeTruthy();
		expect(screen.queryByTestId('orc-encomenda-criada')).toBeNull();
	});

	it('Recusado não mostra banner', () => {
		render(OrcDetPage, {
			props: {
				data: {
					user: null,
					canEdit: false,
					id: 'oc1',
					orcamento: { ...ORCAMENTO_BASE, status: 'Recusado' as const },
					notFound: false,
					error: null
				},
				params: { id: 'oc1' }
			}
		});

		expect(screen.queryByTestId('orc-criar-encomenda')).toBeNull();
		expect(screen.queryByTestId('orc-encomenda-criada')).toBeNull();
	});

	it('Aprovado com encomenda mostra vínculo e omite banner', () => {
		render(OrcDetPage, {
			props: {
				data: {
					user: null,
					canEdit: false,
					id: 'oc1',
					orcamento: { ...ORCAMENTO_BASE, encomendaId: 'e1' },
					notFound: false,
					error: null
				},
				params: { id: 'oc1' }
			}
		});

		expect(screen.queryByTestId('orc-criar-encomenda')).toBeNull();
		expect(screen.getByTestId('orc-encomenda-criada')).toBeTruthy();
	});

	it('converter chama createEncomenda com {idOrcamento} + toast + refetch', async () => {
		mocks.createEncomenda.mockResolvedValue({ ...ENCOMENDA, codigo: 'EN-0001' });
		render(OrcDetPage, {
			props: {
				data: { user: null, canEdit: false, id: 'oc1', orcamento: ORCAMENTO_BASE, notFound: false, error: null },
				params: { id: 'oc1' }
			}
		});

		await fireEvent.click(screen.getByTestId('orc-criar-encomenda'));

		await waitFor(() =>
			expect(mocks.createEncomenda).toHaveBeenCalledWith({ idOrcamento: 'oc1' })
		);
		expect(mocks.toastSuccess).toHaveBeenCalledWith('Encomenda EN-0001 criada na Fila.');
		await waitFor(() => expect(mocks.invalidateAll).toHaveBeenCalled());
	});
});

describe('vendas-pages — solicitações (decisão + erro)', () => {
	function dadosSol() {
		return {
			user: null,
			canEdit: false,
			params: { tab: 'pendentes' },
			resultado: {
				solicitacoes: [SOLICITACAO],
				counts: { Pendentes: 1, 'Aprovadas no mês': 0, 'Rejeitadas no mês': 0, 'Revisar hoje': 1 }
			},
			error: null as null
		};
	}

	it('erro mostra banner e Tentar novamente refaz via goto', async () => {
		render(SolPage, {
			props: {
				data: {
					user: null,
					canEdit: false,
					params: { tab: 'pendentes' },
					resultado: null,
					error: 'Falha de rede.'
				},
				params: {}
			}
		});

		expect(screen.getByText('Não foi possível carregar as solicitações')).toBeTruthy();

		await fireEvent.click(screen.getByTestId('sol-retry'));

		expect(mocks.goto).toHaveBeenCalledWith('/vendas/solicitacoes', { invalidateAll: true });
	});

	it('aprovar chama decidirSolicitacao({aprovada:true}) + toast + refetch', async () => {
		mocks.decidirSolicitacao.mockResolvedValue({ ...SOLICITACAO, status: 'Aprovada' });
		render(SolPage, { props: { data: dadosSol(), params: {} } });

		await fireEvent.click(screen.getByTestId('sol-aprovar'));
		expect(screen.getByTestId('modal-decisao-solicitacao')).toBeTruthy();
		await fireEvent.click(screen.getByTestId('sol-decidir'));

		await waitFor(() =>
			expect(mocks.decidirSolicitacao).toHaveBeenCalledWith('s1', { aprovada: true })
		);
		expect(mocks.toastSuccess).toHaveBeenCalledWith('Solicitação aprovada.');
		await waitFor(() => expect(mocks.invalidateAll).toHaveBeenCalled());
	});

	it('rejeitar sem motivo mostra inline e NÃO chama a API', async () => {
		render(SolPage, { props: { data: dadosSol(), params: {} } });

		await fireEvent.click(screen.getByTestId('sol-rejeitar'));
		await fireEvent.click(screen.getByTestId('sol-decidir'));

		await waitFor(() =>
			expect(screen.getByText('Informe o motivo da rejeição.')).toBeTruthy()
		);
		expect(mocks.decidirSolicitacao).not.toHaveBeenCalled();
	});

	it('rejeitar com motivo chama decidirSolicitacao com motivo', async () => {
		mocks.decidirSolicitacao.mockResolvedValue({ ...SOLICITACAO, status: 'Rejeitada' });
		render(SolPage, { props: { data: dadosSol(), params: {} } });

		await fireEvent.click(screen.getByTestId('sol-rejeitar'));
		await fireEvent.input(screen.getByLabelText(/Motivo/), {
			target: { value: 'Dados divergentes do cadastro.' }
		});
		await fireEvent.click(screen.getByTestId('sol-decidir'));

		await waitFor(() =>
			expect(mocks.decidirSolicitacao).toHaveBeenCalledWith('s1', {
				aprovada: false,
				motivo: 'Dados divergentes do cadastro.'
			})
		);
	});
});

describe('vendas-pages — guarda do módulo (load sem render)', () => {
	it('admin em /vendas/clientes recebe canEdit', () => {
		auth.set({ user: ADMIN, token: 'tk', isAuthenticated: true });

		const saida = vendasLayoutLoad({ url: new URL('http://localhost/vendas/clientes') }) as {
			canEdit: boolean;
		};

		expect(saida).toEqual({ canEdit: true });
	});

	it('responsável de vendas em /vendas/solicitacoes passa (canDecide)', () => {
		auth.set({
			user: { ...OUTRO, responsibilities: { vendas: ['crm'] } },
			token: 'tk',
			isAuthenticated: true
		});

		const saida = vendasLayoutLoad({ url: new URL('http://localhost/vendas/solicitacoes') }) as {
			canEdit: boolean;
		};

		expect(saida).toEqual({ canEdit: false });
	});

	it('!canDecide em /vendas/solicitacoes é redirecionado (303 /vendas/clientes)', () => {
		auth.set({ user: OUTRO, token: 'tk', isAuthenticated: true });

		try {
			vendasLayoutLoad({ url: new URL('http://localhost/vendas/solicitacoes') });
			expect.unreachable('deveria redirecionar !canDecide para /vendas/clientes');
		} catch (e) {
			const redirect = e as { status: number; location: string };
			expect(redirect.status).toBe(303);
			expect(redirect.location).toBe('/vendas/clientes');
		}
	});

	it('recrutando (role 4) em /vendas é barrado (302 com denied)', () => {
		auth.set({
			user: { ...OUTRO, id: 'u4', role: 4 },
			token: 'tk',
			isAuthenticated: true
		});

		try {
			vendasLayoutLoad({ url: new URL('http://localhost/vendas/clientes') });
			expect.unreachable('deveria barrar recrutando com 302');
		} catch (e) {
			const redirect = e as { status: number; location: string };
			expect(redirect.status).toBe(302);
			expect(redirect.location).toContain('/dashboard?denied=');
		}
	});
});
