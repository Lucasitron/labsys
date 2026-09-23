import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/svelte';
import { auth } from '$lib/stores/auth';
import { page as rhPage, applyRhUrl, resetRhPage } from './rh-page-state.svelte.js';
import ListaPage from '../../src/routes/(app)/pessoas/+page.svelte';
import EditarPage from '../../src/routes/(app)/pessoas/[id]/editar/+page.svelte';
import TreinDetPage from '../../src/routes/(app)/pessoas/treinamentos/[id]/+page.svelte';
import HorasPage from '../../src/routes/(app)/pessoas/registro-horas/+page.svelte';
import PsPage from '../../src/routes/(app)/pessoas/processo-seletivo/+page.svelte';
import { load as pessoasLayoutLoad } from '../../src/routes/(app)/pessoas/+layout';
import type { User } from '$lib/types/auth';
import type {
	ExtratoMensalHoras,
	HoraApontamento,
	Person,
	PersonDetail,
	PessoasResult,
	PsCandidate,
	PsGroup,
	TreinamentoDetail,
	TreinamentoSessao
} from '$lib/types/rh';

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
		listPessoas: vi.fn(),
		getPessoa: vi.fn(),
		createPessoa: vi.fn(),
		updatePessoa: vi.fn(),
		listHoras: vi.fn(),
		registrarHoras: vi.fn(),
		validarHoras: vi.fn(),
		rejeitarHoras: vi.fn(),
		getHorasDisponiveis: vi.fn(),
		listApontamentos: vi.fn(),
		createApontamento: vi.fn(),
		validarApontamento: vi.fn(),
		listPS: vi.fn(),
		createGrupo: vi.fn(),
		moverEstagio: vi.fn(),
		avaliarMembro: vi.fn(),
		listTreinamentos: vi.fn(),
		getTreinamento: vi.fn(),
		getSessao: vi.fn(),
		avaliarTreinamento: vi.fn(),
		atribuirTarefaFinal: vi.fn(),
		listAgenda: vi.fn(),
		getDisponibilidade: vi.fn(),
		listGuias: vi.fn(),
		getNiveis: vi.fn(),
		alterarNivel: vi.fn(),
		convidarUsuario: vi.fn()
	};
});

vi.mock('$app/state', () => ({ page: rhPage }));
vi.mock('$app/navigation', () => ({ goto: mocks.goto, invalidateAll: mocks.invalidateAll }));
vi.mock('$app/environment', () => ({ browser: false }));
vi.mock('$lib/stores/toast', () => ({
	toasts: { success: mocks.toastSuccess, danger: mocks.toastDanger },
	toastError: vi.fn()
}));
vi.mock('$lib/api/client', () => ({
	apiFetch: mocks.apiFetch,
	ApiError: mocks.ApiError,
	NetworkError: mocks.NetworkError,
	API_BASE: '',
	sanitizeRedirect: (href: string) => href
}));
vi.mock('$lib/api/rh/pessoas', () => ({
	listPessoas: mocks.listPessoas,
	getPessoa: mocks.getPessoa,
	createPessoa: mocks.createPessoa,
	updatePessoa: mocks.updatePessoa
}));
vi.mock('$lib/api/rh/horas', () => ({
	listHoras: mocks.listHoras,
	registrarHoras: mocks.registrarHoras,
	validarHoras: mocks.validarHoras,
	rejeitarHoras: mocks.rejeitarHoras,
	getHorasDisponiveis: mocks.getHorasDisponiveis,
	listApontamentos: mocks.listApontamentos,
	createApontamento: mocks.createApontamento,
	validarApontamento: mocks.validarApontamento
}));
vi.mock('$lib/api/rh/processo-seletivo', () => ({
	listPS: mocks.listPS,
	createGrupo: mocks.createGrupo,
	moverEstagio: mocks.moverEstagio,
	avaliarMembro: mocks.avaliarMembro
}));
vi.mock('$lib/api/rh/treinamentos', () => ({
	listTreinamentos: mocks.listTreinamentos,
	getTreinamento: mocks.getTreinamento,
	getSessao: mocks.getSessao,
	avaliarTreinamento: mocks.avaliarTreinamento,
	atribuirTarefaFinal: mocks.atribuirTarefaFinal,
	listAgenda: mocks.listAgenda,
	getDisponibilidade: mocks.getDisponibilidade,
	listGuias: mocks.listGuias
}));
vi.mock('$lib/api/rh/niveis', () => ({
	getNiveis: mocks.getNiveis,
	alterarNivel: mocks.alterarNivel,
	convidarUsuario: mocks.convidarUsuario
}));

const ADMIN: User = {
	id: 'u0',
	username: 'admin',
	name: 'Admin FabLab',
	email: 'admin@fablab.org',
	role: 0
};

const PESSOA: Person = {
	id: 'p1',
	matricula: 'FBL-001',
	name: 'Ana Souza',
	type: 'bolsista',
	nivel: 'bolsista',
	status: 'ativo',
	group: { id: 'g1', label: 'G-01' },
	email: 'ana@fablab.org',
	initials: 'AS',
	joinedAt: '2026-01-10'
};

const PESSOA_DETALHE: PersonDetail = {
	...PESSOA,
	kpis: { horasMes: 12, treinamentosMedia: 8.5, pendencias: 1, projetosAtivos: 2 },
	specs: { Departamento: 'Software', Turno: 'Tarde' },
	qualification: 3
};

interface ListaBase {
	user: User | null;
	canEdit: boolean;
	params: { search: string; setor: string; nivel: string; status: string; page: number; pageSize: number };
}

type ListaOk = ListaBase & { resultado: PessoasResult; error: null };
type ListaErro = ListaBase & { resultado: null; error: string };

function baseLista(overrides: Partial<ListaBase> = {}): ListaBase {
	return {
		user: null,
		canEdit: true,
		params: { search: '', setor: '', nivel: '', status: '', page: 1, pageSize: 10 },
		...overrides
	};
}

const RESULTADO_CHEIO: PessoasResult = {
	people: [PESSOA],
	pagination: { page: 1, pageSize: 10, totalItems: 1, totalPages: 3 }
};

const RESULTADO_VAZIO: PessoasResult = {
	people: [],
	pagination: { page: 1, pageSize: 10, totalItems: 0, totalPages: 1 }
};

const APONTAMENTO: HoraApontamento = {
	id: 'h1',
	personId: 'p1',
	person: { name: 'Ana Souza', initials: 'AS' },
	date: '2026-09-20',
	startTime: '08:00',
	endTime: '12:00',
	hours: 4,
	type: 'encomenda',
	ref: 'ENC-12',
	status: 'pendente'
};

const EXTRATO: ExtratoMensalHoras = {
	periodo: '2026-09',
	validadas: 10,
	pendentes: 4,
	rejeitadas: 1
};

const CANDIDATA: PsCandidate = {
	id: 'c1',
	name: 'Ana Souza',
	initials: 'AS',
	stage: 'triagem',
	// Nota pré-existente: o fluxo de salvar reaproveita o valor inicial (string)
	// sem exigir digitação no input numérico (ver desvio D-PS-NOTA no relatório).
	nota: 7
};

const GRUPO: PsGroup = {
	id: 'g1',
	code: 'G-01',
	name: 'Grupo 01',
	stage: 'triagem',
	membersCount: 2,
	candidates: [CANDIDATA]
};

const TREINAMENTO: TreinamentoDetail = {
	id: 't1',
	title: 'Impressão 3D',
	instructor: { id: 'p2', name: 'Bruno Lima', initials: 'BL' },
	status: 'em_andamento',
	machine: 'Impressora Delta',
	group: { id: 'g1', label: 'G-01' },
	doneCount: 6,
	groupSize: 10,
	average: 8.5,
	evaluation: [{ criterion: 'Acabamento', weight: 2 }]
};

const SESSAO: TreinamentoSessao = {
	id: 's1',
	startsAt: '2026-09-21T08:00:00.000Z',
	endsAt: '2026-09-21T12:00:00.000Z',
	present: 7
};

beforeEach(() => {
	for (const fn of Object.values(mocks)) {
		if (typeof fn === 'function' && 'mockReset' in fn) (fn as { mockReset: () => void }).mockReset();
	}
	auth.set({ user: null, token: null, isAuthenticated: false });
	resetRhPage('', {});
	mocks.goto.mockImplementation((href: string) => {
		applyRhUrl(href);
		return Promise.resolve();
	});
	mocks.apiFetch.mockResolvedValue(undefined);
});

afterEach(() => {
	cleanup();
	auth.set({ user: null, token: null, isAuthenticated: false });
});

describe('rh-pages — lista de pessoas (server-side)', () => {
	it('skeleton quando resultado e erro são nulos (sem rows)', () => {
		render(ListaPage, {
			// Skeleton (resultado+erro nulos) não é estado do load: cast local.
			props: { data: { ...baseLista(), resultado: null, error: null } as never, params: {} }
		});

		expect(screen.queryByTestId('person-row')).toBeNull();
		expect(document.querySelector('.animate-pulse')).toBeTruthy();
	});

	it('sucesso: rows desktop+mobile, checkbox e Novo cadastro', () => {
		render(ListaPage, {
			props: { data: { ...baseLista(), resultado: RESULTADO_CHEIO, error: null }, params: {} }
		});

		expect(screen.getAllByTestId('person-row')).toHaveLength(2);
		expect(screen.getAllByTestId('person-checkbox')).toHaveLength(2);
		expect(screen.getByText('Novo cadastro')).toBeTruthy();
		expect(screen.getAllByText('Ana Souza').length).toBeGreaterThanOrEqual(1);
	});

	it('paginação server-side: próxima página navega via goto com page=2', async () => {
		render(ListaPage, {
			props: { data: { ...baseLista(), resultado: RESULTADO_CHEIO, error: null }, params: {} }
		});

		await fireEvent.click(screen.getAllByLabelText('Próxima página')[0]!);

		expect(mocks.goto).toHaveBeenCalledWith('/pessoas?page=2');
	});

	it('empty sem filtros mostra CTA de cadastro', () => {
		render(ListaPage, {
			props: {
				data: { ...baseLista(), resultado: RESULTADO_VAZIO, error: null },
				params: {}
			}
		});

		expect(screen.getByText('Nenhuma pessoa cadastrada')).toBeTruthy();
		expect(screen.queryByTestId('person-row')).toBeNull();
	});

	it('empty filtrado mostra chips e Limpar filtros navega sem filtros', async () => {
		render(ListaPage, {
			props: {
				data: {
					...baseLista({
						params: { search: 'zzz', setor: '', nivel: '', status: '', page: 1, pageSize: 10 }
					}),
					resultado: RESULTADO_VAZIO,
					error: null
				},
				params: {}
			}
		});

		expect(screen.getByText('Nenhuma pessoa encontrada')).toBeTruthy();
		expect(screen.getByText('Busca: zzz')).toBeTruthy();

		await fireEvent.click(screen.getAllByText('Limpar filtros')[0]!);

		expect(mocks.goto).toHaveBeenCalledWith('/pessoas?page=1');
	});

	it('erro mostra banner e Tentar novamente refaz via goto', async () => {
		render(ListaPage, {
			props: {
				data: { ...baseLista(), resultado: null, error: 'Falha de rede.' },
				params: {}
			}
		});

		expect(screen.getByText('Não foi possível carregar as pessoas')).toBeTruthy();

		await fireEvent.click(screen.getByTestId('person-retry'));

		expect(mocks.goto).toHaveBeenCalledWith('/pessoas', { invalidateAll: true });
	});

	it('!canEdit esconde checkbox, select-all, ações e Novo cadastro (RBAC hide)', () => {
		render(ListaPage, {
			props: {
				data: { ...baseLista({ canEdit: false }), resultado: RESULTADO_CHEIO, error: null },
				params: {}
			}
		});

		expect(screen.getAllByTestId('person-row')).toHaveLength(2);
		expect(screen.queryByTestId('select-all')).toBeNull();
		expect(screen.queryByTestId('person-checkbox')).toBeNull();
		expect(screen.queryByTestId('bulk-bar')).toBeNull();
		expect(screen.queryByText('Novo cadastro')).toBeNull();
	});

	it('excluir inline chama DELETE /api/rh/pessoas/{id} (contrato assumido)', async () => {
		render(ListaPage, {
			props: { data: { ...baseLista(), resultado: RESULTADO_CHEIO, error: null }, params: {} }
		});

		await fireEvent.click(screen.getAllByTestId('person-checkbox')[0]!);
		expect(screen.getByTestId('bulk-bar')).toBeTruthy();

		await fireEvent.click(screen.getByText('Excluir'));
		expect(screen.getByText('Excluir pessoa')).toBeTruthy();

		await fireEvent.click(screen.getAllByText('Excluir').at(-1)!);

		await waitFor(() => expect(mocks.apiFetch).toHaveBeenCalledTimes(1));
		const [path, init] = mocks.apiFetch.mock.calls[0] as [string, { method: string }];
		expect(path).toBe('/api/rh/pessoas/p1');
		expect(init.method).toBe('DELETE');
		await waitFor(() =>
			expect(mocks.toastSuccess).toHaveBeenCalledWith('Pessoa excluída com sucesso.')
		);
		expect(mocks.invalidateAll).toHaveBeenCalled();
	});
});

describe('rh-pages — editar pessoa', () => {
	function dadosEditar(canEdit: boolean): {
		user: User | null;
		id: string;
		pessoa: PersonDetail | null;
		notFound: boolean;
		error: string | null;
		canEdit: boolean;
	} {
		return { user: null, id: 'p1', pessoa: PESSOA_DETALHE, notFound: false, error: null, canEdit };
	}

	it('banner edit-rbac-banner sem ações quando !canEdit', () => {
		render(EditarPage, { props: { data: dadosEditar(false), params: { id: 'p1' } } });

		expect(screen.getByTestId('edit-rbac-banner')).toBeTruthy();
		expect(screen.queryByTestId('person-save')).toBeNull();
	});

	it('matrícula é read-only (sem interação)', async () => {
		render(EditarPage, { props: { data: dadosEditar(true), params: { id: 'p1' } } });

		const matricula = screen.getByTestId('person-matricula') as HTMLInputElement;
		expect(matricula.hasAttribute('readonly')).toBe(true);
		expect(matricula.readOnly).toBe(true);
		expect(matricula.getAttribute('aria-readonly')).toBe('true');
		await waitFor(() => expect(matricula.value).toBe('FBL-001'));
	});

	it('409 de e-mail duplicado vira erro inline no campo', async () => {
		mocks.updatePessoa.mockRejectedValueOnce(new mocks.ApiError(409, 'Conflito.'));
		render(EditarPage, { props: { data: dadosEditar(true), params: { id: 'p1' } } });

		await waitFor(() =>
			expect((screen.getByTestId('person-name') as HTMLInputElement).value).toBe('Ana Souza')
		);
		await fireEvent.click(screen.getByTestId('person-save'));

		await waitFor(() =>
			expect(screen.getByText('Este e-mail já está em uso por outra pessoa.')).toBeTruthy()
		);
		expect(mocks.updatePessoa).toHaveBeenCalledWith(
			'p1',
			expect.objectContaining({ name: 'Ana Souza' })
		);
	});
});

describe('rh-pages — detalhe do treinamento', () => {
	beforeEach(() => {
		resetRhPage('', { id: 't1' });
	});

	it('CTA de sessão só aparece com sessão ativa', async () => {
		auth.set({ user: ADMIN, token: 'tk', isAuthenticated: true });
		mocks.getTreinamento.mockResolvedValue(TREINAMENTO);
		mocks.getSessao.mockResolvedValue(SESSAO);
		render(TreinDetPage);

		await waitFor(() => expect(screen.getByTestId('training-detail-title')).toBeTruthy());
		expect(screen.getByTestId('training-open-session')).toBeTruthy();
		expect(screen.getByTestId('training-detail-session').textContent).toContain(
			'sessão em andamento'
		);
	});

	it('sem sessão ativa o CTA some e o indicador informa', async () => {
		auth.set({ user: ADMIN, token: 'tk', isAuthenticated: true });
		mocks.getTreinamento.mockResolvedValue(TREINAMENTO);
		mocks.getSessao.mockRejectedValueOnce(new Error('Sem sessão.'));
		render(TreinDetPage);

		await waitFor(() => expect(screen.getByTestId('training-detail-title')).toBeTruthy());
		expect(screen.queryByTestId('training-open-session')).toBeNull();
		expect(screen.getByTestId('training-detail-session').textContent).toContain('sem sessão ativa');
	});

	it('ações de avaliação escondidas quando !isInstrutor', async () => {
		auth.set({
			user: { ...ADMIN, id: 'u2', role: 2 },
			token: 'tk',
			isAuthenticated: true
		});
		mocks.getTreinamento.mockResolvedValue(TREINAMENTO);
		mocks.getSessao.mockRejectedValueOnce(new Error('Sem sessão.'));
		render(TreinDetPage);

		await waitFor(() => expect(screen.getByTestId('training-detail-title')).toBeTruthy());
		expect(
			screen.queryByRole('button', { name: 'Avaliar treinamento Impressão 3D' })
		).toBeNull();
	});

	it('instrutor vê Avaliar treinamento e Atribuir tarefa final', async () => {
		auth.set({
			user: { ...ADMIN, id: 'u3', role: 1, responsibilities: { rh: ['instrutor-laser'] } },
			token: 'tk',
			isAuthenticated: true
		});
		mocks.getTreinamento.mockResolvedValue(TREINAMENTO);
		mocks.getSessao.mockRejectedValueOnce(new Error('Sem sessão.'));
		render(TreinDetPage);

		await waitFor(() => expect(screen.getByTestId('training-detail-title')).toBeTruthy());
		expect(
			screen.getByRole('button', { name: 'Avaliar treinamento Impressão 3D' })
		).toBeTruthy();
		expect(
			screen.getByRole('button', { name: 'Atribuir tarefa final em Impressão 3D' })
		).toBeTruthy();
	});
});

describe('rh-pages — registro de horas (extrato backend)', () => {
	beforeEach(() => {
		resetRhPage('status=pendente', {});
		auth.set({ user: ADMIN, token: 'tk', isAuthenticated: true });
		mocks.listApontamentos.mockResolvedValue({
			hours: [APONTAMENTO],
			counts: { pendentes: 1, validadas: 2, rejeitadas: 0 }
		});
		mocks.getHorasDisponiveis.mockResolvedValue(EXTRATO);
	});

	it('KPIs vêm do extrato mockado (sem soma no client)', async () => {
		render(HorasPage, { props: { data: { user: ADMIN, canEdit: true }, params: {} } });

		await waitFor(() => expect(mocks.getHorasDisponiveis).toHaveBeenCalledTimes(1));
		await waitFor(() => expect(screen.getByText('Horas validadas')).toBeTruthy());
		expect(screen.getByText('10h')).toBeTruthy();
		expect(screen.getByText('Período do extrato')).toBeTruthy();
		expect(screen.getByText('Setembro de 2026')).toBeTruthy();
	});

	it('validar chama validarApontamento e refaz a lista', async () => {
		mocks.validarApontamento.mockResolvedValue({ ...APONTAMENTO, status: 'validada' });
		render(HorasPage, { props: { data: { user: ADMIN, canEdit: true }, params: {} } });

		await waitFor(() => expect(screen.getByTestId('hora-tab-pendente')).toBeTruthy());
		await fireEvent.click(screen.getByTestId('hora-validar'));

		await waitFor(() => expect(mocks.validarApontamento).toHaveBeenCalledWith('h1'));
		expect(mocks.toastSuccess).toHaveBeenCalledWith('Horas de Ana Souza validadas.');
		await waitFor(() => expect(mocks.listApontamentos).toHaveBeenCalledTimes(2));
	});

	it('rejeitar exige motivo, chama rejeitarHoras legado e refaz a lista', async () => {
		mocks.rejeitarHoras.mockResolvedValue({ ...APONTAMENTO, status: 'rejeitada' });
		render(HorasPage, { props: { data: { user: ADMIN, canEdit: true }, params: {} } });

		await waitFor(() => expect(screen.getByTestId('hora-tab-pendente')).toBeTruthy());
		await fireEvent.click(screen.getByTestId('hora-rejeitar'));

		expect(screen.getByTestId('modal-rejeitar-hora')).toBeTruthy();
		await fireEvent.input(screen.getByLabelText(/Motivo da rejeição/), {
			target: { value: 'Horário divergente da escala.' }
		});
		await fireEvent.click(screen.getByText('Confirmar rejeição'));

		await waitFor(() =>
			expect(mocks.rejeitarHoras).toHaveBeenCalledWith('h1', {
				reason: 'Horário divergente da escala.'
			})
		);
		await waitFor(() => expect(mocks.listApontamentos).toHaveBeenCalledTimes(2));
	});

	it('apontamento rejeitado exibe o motivo em texto', async () => {
		resetRhPage('status=rejeitada', {});
		mocks.listApontamentos.mockResolvedValue({
			hours: [{ ...APONTAMENTO, status: 'rejeitada', reason: 'Horário divergente da escala.' }],
			counts: { pendentes: 0, validadas: 0, rejeitadas: 1 }
		});
		render(HorasPage, { props: { data: { user: ADMIN, canEdit: true }, params: {} } });

		await waitFor(() => expect(screen.getByTestId('hora-tab-rejeitada')).toBeTruthy());
		expect(screen.getByText('Horário divergente da escala.')).toBeTruthy();
	});
});

describe('rh-pages — processo seletivo (avaliação individual)', () => {
	beforeEach(() => {
		auth.set({ user: ADMIN, token: 'tk', isAuthenticated: true });
		mocks.listPS.mockResolvedValue({
			groups: [GRUPO],
			counts: { inscritos: 2, grupos: 1, em_avaliacao: 1, aprovados: 0 }
		});
	});

	it('KPIs de grupos vêm dos counts do backend', async () => {
		render(PsPage, { props: { data: { user: ADMIN, canEdit: true }, params: {} } });

		await waitFor(() => expect(screen.getByTestId('ps-kpi-grupos')).toBeTruthy());
		expect(screen.getByTestId('ps-kpi-grupos').textContent).toContain('1');
	});

	it('avaliar chama avaliarMembro com grupo + candidato e refaz a lista', async () => {
		mocks.avaliarMembro.mockResolvedValue({ nota: 7, feedback: 'Bom potencial.' });
		render(PsPage, { props: { data: { user: ADMIN, canEdit: true }, params: {} } });

		await waitFor(() => expect(screen.getByLabelText('Ver grupo Grupo 01')).toBeTruthy());
		await fireEvent.click(screen.getByLabelText('Ver grupo Grupo 01'));

		await waitFor(() => expect(screen.getByTestId('modal-grupo')).toBeTruthy());
		expect(screen.getByText('Nota 7')).toBeTruthy();
		await fireEvent.click(screen.getByLabelText('Avaliar Ana Souza'));

		await waitFor(() => expect(screen.getByTestId('modal-avaliar-ps')).toBeTruthy());
		await fireEvent.input(screen.getByLabelText('Feedback'), {
			target: { value: 'Bom potencial.' }
		});
		await fireEvent.click(screen.getByText('Salvar avaliação'));

		await waitFor(() =>
			expect(mocks.avaliarMembro).toHaveBeenCalledWith('g1', 'c1', {
				nota: 7,
				feedback: 'Bom potencial.',
				resultado: 'aprovado'
			})
		);
		expect(mocks.toastSuccess).toHaveBeenCalledWith('Avaliação de Ana Souza registrada.');
		await waitFor(() => expect(mocks.listPS).toHaveBeenCalledTimes(2));
	});
});

describe('rh-pages — guarda de níveis (load sem render)', () => {
	it('admin em /pessoas/niveis recebe canEdit', () => {
		auth.set({ user: ADMIN, token: 'tk', isAuthenticated: true });

		const saida = pessoasLayoutLoad({ url: new URL('http://localhost/pessoas/niveis') }) as {
			canEdit: boolean;
		};

		expect(saida).toEqual({ canEdit: true });
	});

	it('não-admin em /pessoas/niveis é redirecionado (303 /pessoas)', () => {
		auth.set({
			user: { ...ADMIN, id: 'u1', role: 1, responsibilities: { rh: ['g-01'] } },
			token: 'tk',
			isAuthenticated: true
		});

		try {
			pessoasLayoutLoad({ url: new URL('http://localhost/pessoas/niveis') });
			expect.unreachable('deveria redirecionar não-admin para /pessoas');
		} catch (e) {
			const redirect = e as { status: number; location: string };
			expect(redirect.status).toBe(303);
			expect(redirect.location).toBe('/pessoas');
		}
	});

	it('usuário sem view em RH é redirecionado (302 com denied)', () => {
		auth.set({ user: null, token: null, isAuthenticated: false });

		try {
			pessoasLayoutLoad({ url: new URL('http://localhost/pessoas') });
			expect.unreachable('deveria redirecionar sem view para o dashboard');
		} catch (e) {
			const redirect = e as { status: number; location: string };
			expect(redirect.status).toBe(302);
			expect(redirect.location).toContain('/dashboard?denied=');
		}
	});

	it('não-admin fora de /pessoas/niveis passa com canEdit do responsável', () => {
		auth.set({
			user: { ...ADMIN, id: 'u1', role: 1, responsibilities: { rh: ['g-01'] } },
			token: 'tk',
			isAuthenticated: true
		});

		const saida = pessoasLayoutLoad({ url: new URL('http://localhost/pessoas') }) as {
			canEdit: boolean;
		};

		expect(saida).toEqual({ canEdit: true });
	});
});
