import { bearer } from '$lib/api/bearer';
import { apiFetch } from '$lib/api/client';
import type {
	ExtratoMensalHoras,
	HoraApontamento,
	HorasResult,
	HoraStatus,
	HoraTipo,
	RegistrarHorasPayload,
	RejeitarHorasPayload
} from '$lib/types/rh';


const STATUS_PARA_UPPER: Record<HoraStatus, string> = {
	pendente: 'PENDENTE',
	validada: 'VALIDADO',
	rejeitada: 'REJEITADO'
};

const UPPER_PARA_STATUS: Record<string, HoraStatus> = {
	PENDENTE: 'pendente',
	VALIDADO: 'validada',
	REJEITADO: 'rejeitada'
};

const TIPO_PARA_UPPER: Record<HoraTipo, string> = {
	encomenda: 'ENCOMENDA',
	projeto: 'PROJETO'
};

const UPPER_PARA_TIPO: Record<string, HoraTipo> = {
	ENCOMENDA: 'encomenda',
	PROJETO: 'projeto'
};

export interface RawApontamento {
	id: number | string;
	idFuncionario: number | string;
	tipo: string;
	idReferencia: number | string;
	data: string;
	horasTrabalhadas?: number | string | null;
	horaInicio?: string | null;
	horaFim?: string | null;
	descricaoAtividade?: string | null;
	status: string;
	motivoRejeicao?: string | null;
}

export function mapApontamento(raw: RawApontamento): HoraApontamento {
	return {
		id: String(raw.id),
		personId: String(raw.idFuncionario),
		person: { name: '', initials: '?' },
		date: raw.data,
		...(raw.horaInicio ? { startTime: raw.horaInicio } : {}),
		...(raw.horaFim ? { endTime: raw.horaFim } : {}),
		hours: Number(raw.horasTrabalhadas ?? 0),
		type: UPPER_PARA_TIPO[raw.tipo] ?? 'encomenda',
		ref: String(raw.idReferencia),
		status: UPPER_PARA_STATUS[raw.status] ?? 'pendente',
		...(raw.motivoRejeicao ? { reason: raw.motivoRejeicao } : {})
	};
}

function contar(hours: HoraApontamento[]): HorasResult['counts'] {
	return {
		pendentes: hours.filter((h) => h.status === 'pendente').length,
		validadas: hours.filter((h) => h.status === 'validada').length,
		rejeitadas: hours.filter((h) => h.status === 'rejeitada').length
	};
}

export async function listApontamentos(
	params: { status?: HoraStatus; periodo?: string; personId?: string } = {},
	fetchFn: typeof fetch = fetch
): Promise<HorasResult> {
	const query = new URLSearchParams();
	if (params.status) {
		const upper = STATUS_PARA_UPPER[params.status];
		if (upper) query.set('status', upper);
	}
	if (params.periodo) query.set('periodo', params.periodo);
	const qs = query.toString();
	const raw = await apiFetch<RawApontamento[]>(
		`/rh/apontamentos-horas${qs ? `?${qs}` : ''}`,
		{ headers: bearer() },
		fetchFn
	);
	let hours = (raw ?? []).map(mapApontamento);
	if (params.personId) {
		hours = hours.filter((h) => h.personId === params.personId);
	}
	return { hours, counts: contar(hours) };
}

export async function createApontamento(
	payload: RegistrarHorasPayload,
	fetchFn: typeof fetch = fetch
): Promise<HoraApontamento> {
	const idReferencia = Number(payload.projectId ?? payload.orderId);
	if (!payload.personId || !Number.isInteger(idReferencia)) {
		throw new Error('Informe a pessoa e a referência para registrar as horas.');
	}
	const detalhe = await apiFetch<{ idFuncionario?: number | string | null }>(
		`/rh/pessoas/${payload.personId}/detalhe`,
		{ headers: bearer() },
		fetchFn
	);
	if (detalhe.idFuncionario == null) {
		throw new Error('Pessoa sem vínculo de funcionário: não é possível apontar horas.');
	}
	const body: Record<string, unknown> = {
		idFuncionario: Number(detalhe.idFuncionario),
		tipo: TIPO_PARA_UPPER[payload.type],
		idReferencia,
		data: payload.date,
		horaInicio: payload.startTime,
		horaFim: payload.endTime,
		horasTrabalhadas: payload.hours
	};
	if (payload.observation?.trim()) body['descricaoAtividade'] = payload.observation.trim();
	const raw = await apiFetch<RawApontamento>(
		'/rh/apontamentos-horas',
		{ method: 'POST', headers: bearer(), body: JSON.stringify(body) },
		fetchFn
	);
	return mapApontamento(raw);
}

export async function validarApontamento(
	id: string,
	fetchFn: typeof fetch = fetch
): Promise<HoraApontamento> {
	const raw = await apiFetch<RawApontamento>(
		`/rh/apontamentos-horas/${id}/validar`,
		{ method: 'PUT', headers: bearer(), body: JSON.stringify({ status: 'VALIDADO' }) },
		fetchFn
	);
	return mapApontamento(raw);
}

export async function rejeitarHoras(
	id: string,
	payload: RejeitarHorasPayload,
	fetchFn: typeof fetch = fetch
): Promise<HoraApontamento> {
	const raw = await apiFetch<RawApontamento>(
		`/rh/horas/${id}/rejeitar`,
		{ method: 'PATCH', headers: bearer(), body: JSON.stringify({ motivo: payload.reason }) },
		fetchFn
	);
	return mapApontamento(raw);
}

export async function getHorasDisponiveis(
	fetchFn: typeof fetch = fetch
): Promise<ExtratoMensalHoras> {
	const raw = await apiFetch<{ idFuncionario?: number | string | null; totalHorasDisponiveis?: number | string | null }>(
		'/rh/horas/disponiveis',
		{ headers: bearer() },
		fetchFn
	);
	// O back serve só o total validado não consolidado (sem quebra por status).
	return {
		periodo: '',
		validadas: Number(raw.totalHorasDisponiveis ?? 0),
		pendentes: 0,
		rejeitadas: 0
	};
}
