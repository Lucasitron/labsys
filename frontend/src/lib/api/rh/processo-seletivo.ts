import { get } from 'svelte/store';
import { apiFetch } from '$lib/api/client';
import { auth } from '$lib/stores/auth';
import type {
	CreateGrupoPayload,
	PsCandidate,
	PsEvaluation,
	PsGroup,
	PsListResult,
	PsStage
} from '$lib/types/rh';

function bearer(): Record<string, string> {
	const { token } = get(auth);
	return token ? { Authorization: `Bearer ${token}` } : {};
}

const ETAPA_PARA_UPPER: Record<PsStage, string> = {
	inscrito: 'INSCRITO',
	triagem: 'EM_TRIAGEM',
	entrevista: 'ENTREVISTA',
	aprovado: 'APROVADO',
	reprovado: 'REPROVADO'
};

const UPPER_PARA_ETAPA: Record<string, PsStage> = {
	INSCRITO: 'inscrito',
	EM_TRIAGEM: 'triagem',
	ENTREVISTA: 'entrevista',
	APROVADO: 'aprovado',
	REPROVADO: 'reprovado'
};

interface RawCandidato {
	idPessoa: number | string;
	nome: string;
	status: string;
	nota?: number | string | null;
	feedback?: string | null;
}

interface RawGrupo {
	id: number | string | null;
	nome: string;
	etapa: string;
	total: number;
	idLider?: number | string | null;
	nomeLider?: string | null;
	candidatos: RawCandidato[];
}

function mapCandidato(raw: RawCandidato): PsCandidate {
	const nome = raw.nome ?? '';
	const partes = nome.trim().split(/\s+/);
	const initials = partes.length > 1
		? `${partes[0][0] ?? ''}${partes[partes.length - 1][0] ?? ''}`.toUpperCase()
		: (nome.slice(0, 2) || '?').toUpperCase();
	return {
		id: String(raw.idPessoa),
		name: nome,
		initials,
		stage: UPPER_PARA_ETAPA[raw.status] ?? 'inscrito',
		...(raw.nota != null ? { nota: Number(raw.nota) } : {}),
		...(raw.feedback != null ? { feedback: raw.feedback } : {})
	};
}

function mapGrupo(raw: RawGrupo): PsGroup {
	const candidatos = (raw.candidatos ?? []).map(mapCandidato);
	return {
		id: raw.id != null ? String(raw.id) : 'sem-grupo',
		code: raw.id != null ? `G-${raw.id}` : 'SEM-GRUPO',
		name: raw.nome,
		stage: UPPER_PARA_ETAPA[raw.etapa] ?? 'inscrito',
		membersCount: raw.total ?? candidatos.length,
		candidates: candidatos
	};
}

export interface IniciarProcessoPayload {
	nomeCompleto: string;
	matricula: string;
	contato?: string;
	turno?: string;
	idTutor?: number;
}

export interface ProcessoIniciado {
	id: string;
	idCandidato: string;
	nomeCandidato: string;
	statusProcesso: string;
}

interface RawProcesso {
	id: number | string;
	idCandidato: number | string;
	nomeCandidato?: string | null;
	statusProcesso: string;
}

// POST /rh/processo-seletivo — candidato nasce como pessoa + funcionário
// Recrutando e é o único caminho que o insere no Kanban do PS.
export async function iniciarProcesso(
	payload: IniciarProcessoPayload,
	fetchFn: typeof fetch = fetch
): Promise<ProcessoIniciado> {
	const body: Record<string, unknown> = {
		nomeCompleto: payload.nomeCompleto,
		matricula: payload.matricula
	};
	if (payload.contato) body['contato'] = payload.contato;
	if (payload.turno) body['turno'] = payload.turno;
	if (payload.idTutor !== undefined) body['idTutor'] = payload.idTutor;
	const raw = await apiFetch<RawProcesso>(
		'/rh/processo-seletivo',
		{ method: 'POST', headers: bearer(), body: JSON.stringify(body) },
		fetchFn
	);
	return {
		id: String(raw.id),
		idCandidato: String(raw.idCandidato),
		nomeCandidato: raw.nomeCandidato ?? '',
		statusProcesso: raw.statusProcesso
	};
}

export async function listPS(
	params: { estagio?: PsStage } = {},
	fetchFn: typeof fetch = fetch
): Promise<PsListResult> {
	const query = new URLSearchParams();
	if (params.estagio) query.set('estagio', ETAPA_PARA_UPPER[params.estagio]);
	const qs = query.toString();
	const res = await apiFetch<{
		grupos: RawGrupo[];
		totais: { processos: number; grupos: number; emAvaliacao: number; aprovados: number };
	}>(
		`/rh/processo-seletivo${qs ? `?${qs}` : ''}`,
		{ headers: bearer() },
		fetchFn
	);
	return {
		groups: (res.grupos ?? []).map(mapGrupo),
		counts: {
			inscritos: res.totais?.processos ?? 0,
			grupos: res.totais?.grupos ?? 0,
			em_avaliacao: res.totais?.emAvaliacao ?? 0,
			aprovados: res.totais?.aprovados ?? 0
		}
	};
}

export async function createGrupo(
	payload: CreateGrupoPayload,
	fetchFn: typeof fetch = fetch
): Promise<PsGroup> {
	const idLider = Number(payload.tutorId);
	if (!Number.isInteger(idLider)) throw new Error('Selecione o tutor líder do grupo.');
	const body: Record<string, unknown> = { nome: payload.name, idLider };
	if (payload.memberIds?.length) body['membroIds'] = payload.memberIds.map(Number);
	const raw = await apiFetch<RawGrupo>(
		'/rh/processo-seletivo/grupos',
		{ method: 'POST', headers: bearer(), body: JSON.stringify(body) },
		fetchFn
	);
	return mapGrupo(raw);
}

export async function moverEstagio(
	id: string,
	estagio: PsStage,
	fetchFn: typeof fetch = fetch
): Promise<PsGroup> {
	// O back move o PROCESSO do candidato (sem estágio de grupo); a resposta é
	// o processo — converte p/ grupo de 1 membro p/ manter o kanban da tela.
	const raw = await apiFetch<{
		id: number | string;
		idGrupo?: number | string | null;
		nomeCandidato?: string | null;
		idCandidato?: number | string | null;
		statusProcesso: string;
		nota?: number | string | null;
		feedback?: string | null;
	}>(
		`/rh/processo-seletivo/${id}/estagio`,
		{
			method: 'PATCH',
			headers: bearer(),
			body: JSON.stringify({ etapa: ETAPA_PARA_UPPER[estagio] })
		},
		fetchFn
	);
	const candidato: RawCandidato = {
		idPessoa: raw.idCandidato ?? raw.id,
		nome: raw.nomeCandidato ?? '',
		status: raw.statusProcesso,
		...(raw.nota != null ? { nota: raw.nota } : {}),
		...(raw.feedback != null ? { feedback: raw.feedback } : {})
	};
	return {
		id: String(raw.id),
		code: `P-${raw.id}`,
		name: raw.nomeCandidato ?? '',
		stage: estagio,
		membersCount: 1,
		candidates: [mapCandidato(candidato)]
	};
}

export async function avaliarMembro(
	id: string,
	pessoaId: string,
	payload: PsEvaluation,
	fetchFn: typeof fetch = fetch
): Promise<PsEvaluation> {
	return await apiFetch<PsEvaluation>(
		`/rh/processo-seletivo/${id}/membros/${pessoaId}/avaliar`,
		{
			method: 'POST',
			headers: bearer(),
			body: JSON.stringify({ nota: Number(payload.nota), feedback: payload.feedback })
		},
		fetchFn
	);
}
