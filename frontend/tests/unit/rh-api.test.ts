import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { writable } from 'svelte/store';

const { apiFetchMock, ApiError } = vi.hoisted(() => {
	class ApiError extends Error {
		status: number;
		code: string;
		constructor(status: number, code: string, message: string) {
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

async function freshPessoas() {
	vi.resetModules();
	return await import('$lib/api/rh/pessoas');
}

async function freshPS() {
	vi.resetModules();
	return await import('$lib/api/rh/processo-seletivo');
}

async function freshHoras() {
	vi.resetModules();
	return await import('$lib/api/rh/horas');
}

async function freshTreinamentos() {
	vi.resetModules();
	return await import('$lib/api/rh/treinamentos');
}

async function freshNiveis() {
	vi.resetModules();
	return await import('$lib/api/rh/niveis');
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

describe('rh/api — pessoas', () => {
	it('listPessoas monta /rh/pessoas com search/setor/nivel/status UPPER/page/pageSize', async () => {
		apiFetchMock.mockResolvedValue({ pessoas: [], paginacao: { page: 1, pageSize: 10, total: 0, totalPages: 1 } });
		const { listPessoas } = await freshPessoas();

		await listPessoas({
			search: 'ana',
			setor: 'Software',
			nivel: 'bolsista',
			status: 'ativo',
			page: 2,
			pageSize: 25
		});

		expect(chamada().path).toBe(
			'/rh/pessoas?search=ana&setor=Software&nivel=BOLSISTA&status=ATIVO&page=2&pageSize=25'
		);
	});

	it('listPessoas omite filtros vazios (path puro sem query)', async () => {
		apiFetchMock.mockResolvedValue({ pessoas: [], paginacao: { page: 1, pageSize: 10, total: 0, totalPages: 1 } });
		const { listPessoas } = await freshPessoas();

		await listPessoas({});

		expect(chamada().path).toBe('/rh/pessoas');
	});

	it('getPessoa dispara GET /rh/pessoas/{id}', async () => {
		apiFetchMock.mockResolvedValue({ id: 1, nomeCompleto: 'Ana', matricula: 'M1' });
		const { getPessoa } = await freshPessoas();

		await getPessoa('p1');

		expect(chamada().path).toBe('/rh/pessoas/p1');
		expect(chamada().init['method']).toBeUndefined();
	});

	it('createPessoa faz POST /rh/pessoas com body JSON (inclui nivelAcesso)', async () => {
		apiFetchMock.mockResolvedValue({ id: 1, nomeCompleto: 'Ana Souza', matricula: 'M9' });
		const { createPessoa } = await freshPessoas();
		const payload = { name: 'Ana Souza', matricula: 'M9', email: 'ana@fablab.org', type: 'bolsista', nivel: 'bolsista' } as const;

		await createPessoa(payload);

		expect(chamada().path).toBe('/rh/pessoas');
		expect(chamada().init['method']).toBe('POST');
		expect(chamada().init['body']).toBe(
			JSON.stringify({
				nomeCompleto: 'Ana Souza',
				matricula: 'M9',
				contato: 'ana@fablab.org',
				nivelAcesso: 1
			})
		);
	});

	it('updatePessoa faz PUT /rh/pessoas/{id} com body JSON', async () => {
		apiFetchMock.mockResolvedValue({ id: 1, nomeCompleto: 'Ana S.' });
		const { updatePessoa } = await freshPessoas();

		await updatePessoa('p1', { name: 'Ana S.' });

		expect(chamada().path).toBe('/rh/pessoas/p1');
		expect(chamada().init['method']).toBe('PUT');
		expect(chamada().init['body']).toBe(JSON.stringify({ nomeCompleto: 'Ana S.' }));
	});

	it("updatePessoa envia departamento vazio ('') para limpar o campo", async () => {
		apiFetchMock.mockResolvedValue({ id: 1, nomeCompleto: 'Ana S.' });
		const { updatePessoa } = await freshPessoas();

		await updatePessoa('p1', { name: 'Ana S.', department: '' });

		expect(chamada().init['body']).toBe(
			JSON.stringify({ nomeCompleto: 'Ana S.', departamento: '' })
		);
	});

	it('updatePessoa omite departamento quando a chave não é definida', async () => {
		apiFetchMock.mockResolvedValue({ id: 1, nomeCompleto: 'Ana S.' });
		const { updatePessoa } = await freshPessoas();

		await updatePessoa('p1', { name: 'Ana S.' });

		const body = JSON.parse(String(chamada().init['body'])) as Record<string, unknown>;
		expect(body).not.toHaveProperty('departamento');
	});

	it("updatePessoa envia status numérico 2 para 'afastado' (RECRUTANDO no back)", async () => {
		apiFetchMock.mockResolvedValue({ id: 1, nomeCompleto: 'Ana S.' });
		const { updatePessoa, createPessoa } = await freshPessoas();

		await updatePessoa('p1', { status: 'afastado' });
		expect(chamada(0).init['body']).toBe(JSON.stringify({ status: 2 }));

		await createPessoa({ name: 'Ana', status: 'afastado' });
		expect(chamada(1).init['body']).toBe(JSON.stringify({ nomeCompleto: 'Ana', status: 2 }));

		await updatePessoa('p1', { status: 'ativo' });
		expect(chamada(2).init['body']).toBe(JSON.stringify({ status: 0 }));

		await updatePessoa('p1', { status: 'inativo' });
		expect(chamada(3).init['body']).toBe(JSON.stringify({ status: 1 }));
	});

	it('deletePessoa faz DELETE /rh/pessoas/{id}', async () => {
		apiFetchMock.mockResolvedValue(undefined);
		const { deletePessoa } = await freshPessoas();

		await deletePessoa('p1');

		expect(chamada().path).toBe('/rh/pessoas/p1');
		expect(chamada().init['method']).toBe('DELETE');
	});
});

describe('rh/api — processo seletivo', () => {
	it('listPS monta /rh/processo-seletivo com estagio UPPER e omite quando ausente', async () => {
		apiFetchMock.mockResolvedValue({ grupos: [], totais: { processos: 0, grupos: 0, emAvaliacao: 0, aprovados: 0 } });
		const { listPS } = await freshPS();

		const res = await listPS({ estagio: 'triagem' });
		expect(chamada(0).path).toBe('/rh/processo-seletivo?estagio=EM_TRIAGEM');
		expect(res.groups).toEqual([]);

		await listPS({});
		expect(chamada(1).path).toBe('/rh/processo-seletivo');
	});

	it('createGrupo faz POST {nome,idLider,membroIds} numéricos', async () => {
		apiFetchMock.mockResolvedValue({ id: 6, nome: 'G-06', etapa: 'INSCRITO', total: 0, candidatos: [] });
		const { createGrupo } = await freshPS();

		await createGrupo({ name: 'G-06', tutorId: '1', memberIds: ['2', '3'] });

		const { path, init } = chamada();
		expect(path).toBe('/rh/processo-seletivo/grupos');
		expect(init['method']).toBe('POST');
		expect(init['body']).toBe(JSON.stringify({ nome: 'G-06', idLider: 1, membroIds: [2, 3] }));
	});

	it('iniciarProcesso faz POST /rh/processo-seletivo com nome+matricula e idTutor opcional', async () => {
		apiFetchMock.mockResolvedValue({
			id: 9,
			idCandidato: 3,
			nomeCandidato: 'Ana',
			statusProcesso: 'INSCRITO'
		});
		const { iniciarProcesso } = await freshPS();

		const res = await iniciarProcesso({
			nomeCompleto: 'Ana Souza',
			matricula: '2026-001',
			contato: 'ana@fablab.org',
			idTutor: 4
		});

		const { path, init } = chamada();
		expect(path).toBe('/rh/processo-seletivo');
		expect(init['method']).toBe('POST');
		expect(init['body']).toBe(
			JSON.stringify({
				nomeCompleto: 'Ana Souza',
				matricula: '2026-001',
				contato: 'ana@fablab.org',
				idTutor: 4
			})
		);
		expect(res.idCandidato).toBe('3');
		expect(res.statusProcesso).toBe('INSCRITO');
	});

	it('moverEstagio faz PATCH …/{id}/estagio {etapa: UPPER}', async () => {
		apiFetchMock.mockResolvedValue({ id: 9, idCandidato: 3, nomeCandidato: 'C', statusProcesso: 'ENTREVISTA', nota: null, feedback: null });
		const { moverEstagio } = await freshPS();

		await moverEstagio('9', 'entrevista');

		expect(chamada().path).toBe('/rh/processo-seletivo/9/estagio');
		expect(chamada().init['method']).toBe('PATCH');
		expect(chamada().init['body']).toBe(JSON.stringify({ etapa: 'ENTREVISTA' }));
	});

	it('avaliarMembro avalia individual (grupo + pessoa no path)', async () => {
		apiFetchMock.mockResolvedValue({ nota: 8 });
		const { avaliarMembro } = await freshPS();

		await avaliarMembro('g1', 'c9', { nota: 8, feedback: 'Bom potencial.', resultado: 'aprovado' });

		expect(chamada().path).toBe('/rh/processo-seletivo/g1/membros/c9/avaliar');
		expect(chamada().init['method']).toBe('POST');
	});
});

describe('rh/api — horas', () => {
	it('rejeitarHoras usa PATCH com motivo na rejeição', async () => {
		apiFetchMock.mockResolvedValue({ id: 'h1' });
		const { rejeitarHoras } = await freshHoras();

		await rejeitarHoras('h1', { reason: 'Horário divergente da escala.' });
		expect(chamada(0).path).toBe('/rh/horas/h1/rejeitar');
		expect(chamada(0).init['method']).toBe('PATCH');
		expect(chamada(0).init['body']).toBe(
			JSON.stringify({ motivo: 'Horário divergente da escala.' })
		);
	});

	it('getHorasDisponiveis dispara GET /rh/horas/disponiveis (só total validado)', async () => {
		apiFetchMock.mockResolvedValue({ idFuncionario: 7, totalHorasDisponiveis: '10.50' });
		const { getHorasDisponiveis } = await freshHoras();

		const extrato = await getHorasDisponiveis();

		expect(chamada().path).toBe('/rh/horas/disponiveis');
		expect(extrato.validadas).toBe(10.5);
		expect(extrato.pendentes).toBe(0);
	});

	it('getHorasDisponiveis: 403 vira estado neutro "sem vínculo" (não propaga)', async () => {
		apiFetchMock.mockRejectedValueOnce(new ApiError(403, 'FORBIDDEN', 'Acesso negado.'));
		const { getHorasDisponiveis } = await freshHoras();

		const extrato = await getHorasDisponiveis();

		expect(extrato.semVinculo).toBe(true);
		expect(extrato.validadas).toBe(0);
		expect(extrato.pendentes).toBe(0);
		expect(extrato.rejeitadas).toBe(0);
	});

	it('getHorasDisponiveis: outros erros continuam propagando', async () => {
		apiFetchMock.mockRejectedValueOnce(new ApiError(500, 'INTERNAL', 'Erro inesperado.'));
		const { getHorasDisponiveis } = await freshHoras();

		await expect(getHorasDisponiveis()).rejects.toBeInstanceOf(ApiError);
	});

	it('nova modelagem: listApontamentos/createApontamento/validarApontamento (PUT …/validar)', async () => {
		apiFetchMock.mockImplementation(async (path: string, init?: RequestInit) => {
			if (String(path).includes('/detalhe')) return { idFuncionario: 7 };
			if (String(path) === '/rh/pessoas' && init?.method === 'POST') {
				return { id: 1, nomeCompleto: 'Ana', matricula: 'M1' };
			}
			if (String(path).includes('apontamentos')) return [];
			return {};
		});
		const { listApontamentos, createApontamento, validarApontamento } = await freshHoras();

		await listApontamentos({ status: 'pendente', periodo: '2026-09', personId: 'p1' });
		expect(chamada(0).path).toBe('/rh/apontamentos-horas?status=PENDENTE&periodo=2026-09');

		await createApontamento({
			personId: 'p1',
			date: '2026-09-20',
			startTime: '08:00',
			endTime: '12:00',
			hours: 4,
			type: 'encomenda',
			orderId: '12'
		});
		expect(chamada(1).path).toBe('/rh/pessoas/p1/detalhe');
		expect(chamada(2).path).toBe('/rh/apontamentos-horas');
		expect(chamada(2).init['method']).toBe('POST');
		expect(chamada(2).init['body']).toBe(
			JSON.stringify({
				idFuncionario: 7,
				tipo: 'ENCOMENDA',
				idReferencia: 12,
				data: '2026-09-20',
				horaInicio: '08:00',
				horaFim: '12:00',
				horasTrabalhadas: 4
			})
		);

		await validarApontamento('h1');
		expect(chamada(3).path).toBe('/rh/apontamentos-horas/h1/validar');
		expect(chamada(3).init['method']).toBe('PUT');
		expect(chamada(3).init['body']).toBe(JSON.stringify({ status: 'VALIDADO' }));
	});
});

describe('rh/api — treinamentos', () => {
	it('list/get de treinamentos gateados (Lote 3: sem par no backend)', async () => {
		const { listTreinamentos } = await freshTreinamentos();

		await expect(listTreinamentos({ status: 'aberto', search: 'laser' })).rejects.toThrow(
			'Lote 3'
		);
		expect(apiFetchMock).not.toHaveBeenCalled();
	});

	it('getTreinamento gateado; sessão/tarefa-final/agenda/disponibilidade/guias gateadas', async () => {
		apiFetchMock.mockResolvedValue({ id: 't1' });
		const { getTreinamento, getSessao, atribuirTarefaFinal, listAgenda, getDisponibilidade, listGuias } =
			await freshTreinamentos();

		await expect(getTreinamento('t1')).rejects.toThrow('Lote 3');

		await expect(getSessao()).rejects.toThrow('Lote 3');
		await expect(atribuirTarefaFinal()).rejects.toThrow('Lote 3');
		await expect(listAgenda()).rejects.toThrow('Lote 3');
		await expect(getDisponibilidade()).rejects.toThrow('Lote 3');
		await expect(listGuias()).rejects.toThrow('Lote 3');
		expect(apiFetchMock).not.toHaveBeenCalled();
	});

	it('avaliarTreinamento usa POST …/avaliacoes {idFuncionario,nota,feedback}', async () => {
		apiFetchMock.mockImplementation(async (path: string) => {
			if (String(path).includes('/detalhe')) return { idFuncionario: 7 };
			return {};
		});
		const { avaliarTreinamento } = await freshTreinamentos();

		await avaliarTreinamento('t1', { nota: 9, feedback: 'Ótima turma.', pessoaId: 'p1' });
		expect(chamada(0).path).toBe('/rh/pessoas/p1/detalhe');
		expect(chamada(1).path).toBe('/rh/treinamentos/t1/avaliacoes');
		expect(chamada(1).init['method']).toBe('POST');
		expect(chamada(1).init['body']).toBe(
			JSON.stringify({ idFuncionario: 7, nota: 9, feedback: 'Ótima turma.' })
		);

		await expect(avaliarTreinamento('t1', { nota: 9, feedback: 'x' })).rejects.toThrow(
			'funcionário avaliado'
		);
	});
});

describe('rh/api — níveis', () => {
	it('getNiveis adapta {niveis,permissoes}; alterarNivel envia int', async () => {
		apiFetchMock.mockResolvedValue({
			niveis: [{ id: 'admin', codigo: 0, label: 'Admin', total: 1 }],
			permissoes: [
				{ id: 'pessoas', rotulo: 'Pessoas', admin: 'TOTAL', bolsista: 'TOTAL', voluntario: 'TOTAL', estagiario: 'PROPRIO', recrutando: 'NAO' }
			]
		});
		const { getNiveis, alterarNivel } = await freshNiveis();

		const matriz = await getNiveis();
		expect(chamada(0).path).toBe('/rh/niveis');
		expect(matriz.niveis).toEqual(['admin']);
		expect(matriz.modulos).toEqual(['pessoas']);

		await alterarNivel('m1', 'bolsista');
		expect(chamada(1).path).toBe('/rh/niveis/m1/membros');
		expect(chamada(1).init['method']).toBe('PATCH');
		expect(chamada(1).init['body']).toBe(JSON.stringify({ nivel: 1 }));
	});
});

describe('rh/api — bearer', () => {
	it('envia Authorization Bearer quando há token na store', async () => {
		authMock.set({ user: null, token: 'tk-rh', isAuthenticated: false });
		apiFetchMock.mockResolvedValue({ id: 1, nomeCompleto: 'Ana', matricula: 'M1' });
		const { getPessoa } = await freshPessoas();

		await getPessoa('p1');

		expect((chamada().init['headers'] as Record<string, string>).Authorization).toBe(
			'Bearer tk-rh'
		);
	});
});

describe('rh/api — guardas de naming PT e R-5', () => {
	it('nenhum path /rh/people novo e nenhum /stock/ em paths das chamadas', async () => {
		apiFetchMock.mockImplementation(async (path: string, init?: RequestInit) => {
			if (String(path).includes('/detalhe')) return { idFuncionario: 7 };
			if (String(path) === '/rh/pessoas' && init?.method === 'POST') {
				return { id: 1, nomeCompleto: 'Ana', matricula: 'M1' };
			}
			if (String(path).includes('/pessoas/')) {
				return { id: 1, nomeCompleto: 'Ana', matricula: 'M1' };
			}
			if (String(path).includes('/pessoas')) {
				return { pessoas: [], paginacao: { page: 1, pageSize: 10, total: 0, totalPages: 1 } };
			}
		if (String(path).includes('processo-seletivo')) {
			if (String(path).includes('/estagio')) {
				return { id: 9, idCandidato: 3, nomeCandidato: 'C', statusProcesso: 'ENTREVISTA', nota: null, feedback: null };
			}
			if (String(path).includes('/grupos')) {
				return { id: 1, nome: 'G', etapa: 'INSCRITO', total: 0, candidatos: [] };
			}
			return { grupos: [], totais: { processos: 0, grupos: 0, emAvaliacao: 0, aprovados: 0 } };
		}
		if (String(path).includes('apontamentos')) return [];
		if (String(path).includes('horas')) return { idFuncionario: 7, totalHorasDisponiveis: '0.00' };
		if (String(path).includes('treinamentos')) return { trainings: [] };
		if (String(path).includes('niveis')) {
			if (String(path).includes('/membros')) return {};
			return { niveis: [], permissoes: [] };
		}
		return {};
		});
		const pessoas = await freshPessoas();
		const ps = await freshPS();
		const horas = await freshHoras();
		const treinamentos = await freshTreinamentos();
		const niveis = await freshNiveis();

		await pessoas.listPessoas({ search: 'ana', page: 1 });
		await pessoas.getPessoa('p1');
		await pessoas.createPessoa({
			name: 'A',
			email: 'a@fablab.org',
			type: 'bolsista',
			nivel: 'bolsista'
		});
		await pessoas.updatePessoa('p1', { name: 'B' });
		await pessoas.deletePessoa('p1');
		await ps.listPS({ estagio: 'triagem' });
		await ps.createGrupo({ name: 'G', tutorId: '1', memberIds: [] });
		await ps.moverEstagio('9', 'entrevista');
		await ps.avaliarMembro('g1', 'c1', { nota: 7, feedback: 'Ok' });
		await horas.rejeitarHoras('h1', { reason: 'x' });
		await horas.getHorasDisponiveis();
		await horas.listApontamentos({ status: 'pendente' });
		await horas.createApontamento({
			personId: 'p1',
			date: '2026-09-20',
			startTime: '08:00',
			endTime: '12:00',
			hours: 4,
			type: 'projeto',
			projectId: '5'
		});
		await horas.validarApontamento('h1');
		await expect(treinamentos.listTreinamentos({ status: 'aberto' })).rejects.toThrow('Lote 3');
		await expect(treinamentos.getTreinamento('t1')).rejects.toThrow('Lote 3');
		await treinamentos.avaliarTreinamento('t1', { nota: 8, feedback: 'Ok', pessoaId: 'p1' });
		await niveis.getNiveis();
		await niveis.alterarNivel('m1', 'voluntario');

		const paths = apiFetchMock.mock.calls.map((c) => String(c[0]));
		expect(paths.length).toBeGreaterThan(0);
		for (const path of paths) {
			expect(path.startsWith('/rh/')).toBe(true);
			expect(path).not.toMatch(/\/rh\/people/);
			expect(path).not.toMatch(/\/stock\//);
		}
	});

	it('horas.ts não exporta helper de soma (R-5: KPIs vêm do backend)', async () => {
		const horasNs = (await freshHoras()) as unknown as Record<string, unknown>;
		const somadores = Object.keys(horasNs).filter((nome) =>
			/somar|totalizar|consolidar/i.test(nome)
		);
		expect(somadores).toEqual([]);
	});
});
