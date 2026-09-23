import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { writable } from 'svelte/store';

const { apiFetchMock } = vi.hoisted(() => ({ apiFetchMock: vi.fn() }));

const authMock = writable<{ user: null; token: string | null; isAuthenticated: boolean }>({
	user: null,
	token: null,
	isAuthenticated: false
});

vi.mock('$lib/api/client', () => ({
	apiFetch: apiFetchMock
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
	it('listPessoas monta /api/rh/pessoas com search/setor/nivel/status/page/pageSize', async () => {
		apiFetchMock.mockResolvedValue({ people: [], pagination: {} });
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
			'/api/rh/pessoas?search=ana&setor=Software&nivel=bolsista&status=ativo&page=2&pageSize=25'
		);
	});

	it('listPessoas omite filtros vazios (path puro sem query)', async () => {
		apiFetchMock.mockResolvedValue({ people: [], pagination: {} });
		const { listPessoas } = await freshPessoas();

		await listPessoas({});

		expect(chamada().path).toBe('/api/rh/pessoas');
	});

	it('getPessoa dispara GET /api/rh/pessoas/{id}', async () => {
		apiFetchMock.mockResolvedValue({ id: 'p1' });
		const { getPessoa } = await freshPessoas();

		await getPessoa('p1');

		expect(chamada().path).toBe('/api/rh/pessoas/p1');
		expect(chamada().init['method']).toBeUndefined();
	});

	it('createPessoa faz POST /api/rh/pessoas com body JSON', async () => {
		apiFetchMock.mockResolvedValue({ id: 'p9' });
		const { createPessoa } = await freshPessoas();
		const payload = { name: 'Ana Souza', email: 'ana@fablab.org', type: 'bolsista', nivel: 'bolsista' } as const;

		await createPessoa(payload);

		expect(chamada().path).toBe('/api/rh/pessoas');
		expect(chamada().init['method']).toBe('POST');
		expect(chamada().init['body']).toBe(JSON.stringify(payload));
	});

	it('updatePessoa faz PUT /api/rh/pessoas/{id} com body JSON', async () => {
		apiFetchMock.mockResolvedValue({ id: 'p1' });
		const { updatePessoa } = await freshPessoas();

		await updatePessoa('p1', { name: 'Ana S.' });

		expect(chamada().path).toBe('/api/rh/pessoas/p1');
		expect(chamada().init['method']).toBe('PUT');
		expect(chamada().init['body']).toBe(JSON.stringify({ name: 'Ana S.' }));
	});
});

describe('rh/api — processo seletivo', () => {
	it('listPS monta /api/rh/processo-seletivo com estagio e omite quando ausente', async () => {
		apiFetchMock.mockResolvedValue({ groups: [], counts: {} });
		const { listPS } = await freshPS();

		await listPS({ estagio: 'triagem' });
		expect(chamada(0).path).toBe('/api/rh/processo-seletivo?estagio=triagem');

		await listPS({});
		expect(chamada(1).path).toBe('/api/rh/processo-seletivo');
	});

	it('createGrupo faz POST com tutorId (nunca instrutor)', async () => {
		apiFetchMock.mockResolvedValue({ id: 'g6' });
		const { createGrupo } = await freshPS();
		const payload = { name: 'G-06', tutorId: 'p1', memberIds: ['p2', 'p3'] };

		await createGrupo(payload);

		const { path, init } = chamada();
		expect(path).toBe('/api/rh/processo-seletivo/grupos');
		expect(init['method']).toBe('POST');
		expect(init['body']).toBe(JSON.stringify(payload));
		expect(String(init['body'])).toContain('tutorId');
		expect(String(init['body'])).not.toContain('instrutor');
	});

	it('moverEstagio faz PATCH …/{id}/estagio com o destino', async () => {
		apiFetchMock.mockResolvedValue({ id: 'g1' });
		const { moverEstagio } = await freshPS();

		await moverEstagio('g1', 'entrevista');

		expect(chamada().path).toBe('/api/rh/processo-seletivo/g1/estagio');
		expect(chamada().init['method']).toBe('PATCH');
		expect(chamada().init['body']).toBe(JSON.stringify({ estagio: 'entrevista' }));
	});

	it('avaliarMembro avalia individual (grupo + pessoa no path)', async () => {
		apiFetchMock.mockResolvedValue({ nota: 8 });
		const { avaliarMembro } = await freshPS();

		await avaliarMembro('g1', 'c9', { nota: 8, feedback: 'Bom potencial.', resultado: 'aprovado' });

		expect(chamada().path).toBe('/api/rh/processo-seletivo/g1/membros/c9/avaliar');
		expect(chamada().init['method']).toBe('POST');
	});
});

describe('rh/api — horas', () => {
	it('listHoras monta query com status/periodo', async () => {
		apiFetchMock.mockResolvedValue({ hours: [], counts: {} });
		const { listHoras } = await freshHoras();

		await listHoras({ status: 'pendente', periodo: '2026-09' });

		expect(chamada().path).toBe('/api/rh/horas?status=pendente&periodo=2026-09');
	});

	it('validarHoras e rejeitarHoras usam PATCH com motivo na rejeição', async () => {
		apiFetchMock.mockResolvedValue({ id: 'h1' });
		const { validarHoras, rejeitarHoras } = await freshHoras();

		await validarHoras('h1');
		expect(chamada(0).path).toBe('/api/rh/horas/h1/validar');
		expect(chamada(0).init['method']).toBe('PATCH');

		await rejeitarHoras('h1', { reason: 'Horário divergente da escala.' });
		expect(chamada(1).path).toBe('/api/rh/horas/h1/rejeitar');
		expect(chamada(1).init['method']).toBe('PATCH');
		expect(chamada(1).init['body']).toBe(
			JSON.stringify({ reason: 'Horário divergente da escala.' })
		);
	});

	it('getHorasDisponiveis dispara GET /api/rh/horas/disponiveis (KPIs do extrato)', async () => {
		apiFetchMock.mockResolvedValue({ periodo: '2026-09', validadas: 10, pendentes: 4, rejeitadas: 1 });
		const { getHorasDisponiveis } = await freshHoras();

		const extrato = await getHorasDisponiveis();

		expect(chamada().path).toBe('/api/rh/horas/disponiveis');
		expect(extrato.validadas).toBe(10);
	});

	it('nova modelagem: listApontamentos/createApontamento/validarApontamento (PUT …/validar)', async () => {
		apiFetchMock.mockResolvedValue({ hours: [], counts: {} });
		const { listApontamentos, createApontamento, validarApontamento } = await freshHoras();

		await listApontamentos({ status: 'pendente', periodo: '2026-09', personId: 'p1' });
		expect(chamada(0).path).toBe(
			'/api/rh/apontamentos-horas?status=pendente&periodo=2026-09&personId=p1'
		);

		await createApontamento({
			personId: 'p1',
			date: '2026-09-20',
			startTime: '08:00',
			endTime: '12:00',
			hours: 4,
			type: 'encomenda'
		});
		expect(chamada(1).path).toBe('/api/rh/apontamentos-horas');
		expect(chamada(1).init['method']).toBe('POST');

		await validarApontamento('h1');
		expect(chamada(2).path).toBe('/api/rh/apontamentos-horas/h1/validar');
		expect(chamada(2).init['method']).toBe('PUT');
	});
});

describe('rh/api — treinamentos', () => {
	it('listTreinamentos monta query com status/search', async () => {
		apiFetchMock.mockResolvedValue({ trainings: [] });
		const { listTreinamentos } = await freshTreinamentos();

		await listTreinamentos({ status: 'aberto', search: 'laser' });

		expect(chamada().path).toBe('/api/rh/treinamentos?status=aberto&search=laser');
	});

	it('getTreinamento/getSessao disparam GET detalhe e sessão', async () => {
		apiFetchMock.mockResolvedValue({ id: 't1' });
		const { getTreinamento, getSessao } = await freshTreinamentos();

		await getTreinamento('t1');
		expect(chamada(0).path).toBe('/api/rh/treinamentos/t1');

		await getSessao('t1');
		expect(chamada(1).path).toBe('/api/rh/treinamentos/t1/sessao');
	});

	it('avaliarTreinamento e atribuirTarefaFinal usam POST nos sub-paths', async () => {
		apiFetchMock.mockResolvedValue({});
		const { avaliarTreinamento, atribuirTarefaFinal } = await freshTreinamentos();

		await avaliarTreinamento('t1', { nota: 9, feedback: 'Ótima turma.' });
		expect(chamada(0).path).toBe('/api/rh/treinamentos/t1/avaliar');
		expect(chamada(0).init['method']).toBe('POST');

		await atribuirTarefaFinal('t1', { title: 'Peça final', dataConclusao: '2026-10-01' });
		expect(chamada(1).path).toBe('/api/rh/treinamentos/t1/tarefa-final');
		expect(chamada(1).init['method']).toBe('POST');
	});

	it('agenda/disponibilidade/guias usam paths agregados', async () => {
		apiFetchMock.mockResolvedValue({ trainings: [] });
		const { listAgenda, getDisponibilidade, listGuias } = await freshTreinamentos();

		await listAgenda({ tab: 'solicitados' });
		expect(chamada(0).path).toBe('/api/rh/treinamentos/agenda?tab=solicitados');

		await getDisponibilidade();
		expect(chamada(1).path).toBe('/api/rh/treinamentos/disponibilidade');

		await listGuias({ maquinaId: 'm1' });
		expect(chamada(2).path).toBe('/api/rh/treinamentos/guias?maquinaId=m1');
	});
});

describe('rh/api — níveis', () => {
	it('getNiveis/alterarNivel/convidarUsuario usam paths e métodos PT', async () => {
		apiFetchMock.mockResolvedValue({});
		const { getNiveis, alterarNivel, convidarUsuario } = await freshNiveis();

		await getNiveis();
		expect(chamada(0).path).toBe('/api/rh/niveis');

		await alterarNivel('m1', 'bolsista');
		expect(chamada(1).path).toBe('/api/rh/niveis/m1/membros');
		expect(chamada(1).init['method']).toBe('PATCH');
		expect(chamada(1).init['body']).toBe(JSON.stringify({ nivel: 'bolsista' }));

		await convidarUsuario({ name: 'Novo', email: 'novo@fablab.org', nivel: 'voluntario' });
		expect(chamada(2).path).toBe('/api/rh/niveis/convites');
		expect(chamada(2).init['method']).toBe('POST');
	});
});

describe('rh/api — bearer', () => {
	it('envia Authorization Bearer quando há token na store', async () => {
		authMock.set({ user: null, token: 'tk-rh', isAuthenticated: false });
		apiFetchMock.mockResolvedValue({ id: 'p1' });
		const { getPessoa } = await freshPessoas();

		await getPessoa('p1');

		expect((chamada().init['headers'] as Record<string, string>).Authorization).toBe(
			'Bearer tk-rh'
		);
	});
});

describe('rh/api — guardas de naming PT e R-5', () => {
	it('nenhum path /rh/people novo e nenhum /stock/ em paths das chamadas', async () => {
		apiFetchMock.mockImplementation(async (path: string) => {
			if (String(path).includes('/pessoas')) return { people: [], pagination: {} };
			if (String(path).includes('processo-seletivo')) return { groups: [], counts: {} };
			if (String(path).includes('horas')) return { hours: [], counts: {} };
			if (String(path).includes('treinamentos')) return { trainings: [] };
			if (String(path).includes('niveis')) return {};
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
		await ps.listPS({ estagio: 'triagem' });
		await ps.createGrupo({ name: 'G', tutorId: 'p1', memberIds: [] });
		await ps.moverEstagio('g1', 'entrevista');
		await ps.avaliarMembro('g1', 'c1', { nota: 7, feedback: 'Ok' });
		await horas.listHoras({ status: 'pendente' });
		await horas.validarHoras('h1');
		await horas.rejeitarHoras('h1', { reason: 'x' });
		await horas.getHorasDisponiveis();
		await horas.listApontamentos({ status: 'pendente' });
		await horas.createApontamento({
			personId: 'p1',
			date: '2026-09-20',
			startTime: '08:00',
			endTime: '12:00',
			hours: 4,
			type: 'projeto'
		});
		await horas.validarApontamento('h1');
		await treinamentos.listTreinamentos({ status: 'aberto' });
		await treinamentos.getTreinamento('t1');
		await treinamentos.getSessao('t1');
		await treinamentos.avaliarTreinamento('t1', { nota: 8, feedback: 'Ok' });
		await treinamentos.atribuirTarefaFinal('t1', { title: 'T', dataConclusao: '2026-10-01' });
		await treinamentos.listAgenda({});
		await treinamentos.getDisponibilidade();
		await treinamentos.listGuias({});
		await niveis.getNiveis();
		await niveis.alterarNivel('m1', 'voluntario');
		await niveis.convidarUsuario({ name: 'N', email: 'n@fablab.org', nivel: 'voluntario' });

		const paths = apiFetchMock.mock.calls.map((c) => String(c[0]));
		expect(paths.length).toBeGreaterThan(0);
		for (const path of paths) {
			expect(path.startsWith('/api/rh/')).toBe(true);
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
