import type { Meta } from './stock-status';
import type {
	DisponibilidadeStatus,
	GuideStatus,
	HoraStatus,
	Nivel,
	PersonStatus,
	PsStage,
	TrainingStatus
} from '$lib/types/rh';

const DESCONHECIDO: Meta = { label: 'Desconhecido', color: 'muted' };

export function personStatusMeta(status: PersonStatus): Meta {
	switch (status) {
		case 'ativo':
			return { label: 'Ativo', color: 'success' };
		case 'inativo':
			return { label: 'Inativo', color: 'muted' };
		case 'afastado':
			return { label: 'Afastado', color: 'warn' };
		default:
			return DESCONHECIDO;
	}
}

export function psStageMeta(stage: PsStage): Meta {
	switch (stage) {
		case 'inscrito':
			return { label: 'Inscrito', color: 'muted' };
		case 'triagem':
			return { label: 'Em triagem', color: 'brand' };
		case 'entrevista':
			return { label: 'Em entrevista', color: 'warn' };
		case 'aprovado':
			return { label: 'Aprovado', color: 'success' };
		case 'reprovado':
			return { label: 'Reprovado', color: 'danger' };
		default:
			return DESCONHECIDO;
	}
}

export function horaStatusMeta(status: HoraStatus): Meta {
	switch (status) {
		case 'pendente':
			return { label: 'Pendente', color: 'warn' };
		case 'validada':
			return { label: 'Validada', color: 'success' };
		case 'rejeitada':
			return { label: 'Rejeitada', color: 'danger' };
		default:
			return DESCONHECIDO;
	}
}

export function trainingStatusMeta(status: TrainingStatus): Meta {
	switch (status) {
		case 'aberto':
			return { label: 'Inscrições abertas', color: 'success' };
		case 'agendado':
			return { label: 'Agendado', color: 'success' };
		case 'solicitado':
			return { label: 'Solicitado', color: 'brand' };
		case 'pendente':
			return { label: 'Pendente', color: 'warn' };
		case 'em_andamento':
			return { label: 'Em andamento', color: 'brand' };
		case 'concluido':
			return { label: 'Concluído', color: 'muted' };
		default:
			return DESCONHECIDO;
	}
}

export function guideStatusMeta(status: GuideStatus): Meta {
	switch (status) {
		case 'validado':
			return { label: 'Validado', color: 'success' };
		case 'em_revisao':
			return { label: 'Em revisão', color: 'brand' };
		case 'rascunho':
			return { label: 'Rascunho', color: 'warn' };
		default:
			return DESCONHECIDO;
	}
}

export function disponibilidadeMeta(status: DisponibilidadeStatus): Meta {
	switch (status) {
		case 'livre':
			return { label: 'Livre', color: 'success' };
		case 'ocupada':
			return { label: 'Ocupada', color: 'warn' };
		case 'indisponivel':
			return { label: 'Indisponível', color: 'danger' };
		default:
			return DESCONHECIDO;
	}
}

export function nivelMeta(nivel: Nivel): Meta {
	switch (nivel) {
		case 'admin':
			return { label: 'Admin', color: 'danger' };
		case 'bolsista':
			return { label: 'Bolsista', color: 'brand' };
		case 'voluntario':
			return { label: 'Voluntário', color: 'success' };
		case 'estagiario':
			return { label: 'Estagiário', color: 'warn' };
		case 'recrutando':
			return { label: 'Recrutando', color: 'muted' };
		default:
			return DESCONHECIDO;
	}
}
