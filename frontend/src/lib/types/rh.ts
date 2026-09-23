export type PersonType = 'funcionario' | 'bolsista' | 'voluntario' | 'estagiario';

export type PersonStatus = 'ativo' | 'inativo' | 'afastado';

export type Nivel = 'admin' | 'bolsista' | 'voluntario' | 'estagiario' | 'recrutando';

export type PsStage = 'inscrito' | 'triagem' | 'entrevista' | 'aprovado' | 'reprovado';

export type HoraStatus = 'pendente' | 'validada' | 'rejeitada';

export type HoraTipo = 'encomenda' | 'projeto';

export type TrainingStatus =
	| 'aberto'
	| 'agendado'
	| 'solicitado'
	| 'pendente'
	| 'em_andamento'
	| 'concluido';

export type GuideStatus = 'validado' | 'em_revisao' | 'rascunho';

export type DisponibilidadeStatus = 'livre' | 'ocupada' | 'indisponivel';

export interface PageInfo {
	page: number;
	pageSize: number;
	totalItems: number;
	totalPages: number;
}

export interface Paged<T> {
	items: T[];
	pagination: PageInfo;
}

export interface RhListParams {
	search?: string;
	setor?: string;
	nivel?: string;
	status?: string;
	page?: number;
	pageSize?: number;
}

export interface GroupRef {
	id: string;
	label: string;
}

export interface Person {
	id: string;
	matricula: string;
	name: string;
	type: PersonType;
	role?: string;
	isInstrutor?: boolean;
	nivel: Nivel;
	status: PersonStatus;
	group: GroupRef | null;
	email: string;
	phone?: string;
	initials: string;
	joinedAt: string;
	updatedAt?: string;
}

export interface PersonKpis {
	horasMes?: number;
	treinamentosMedia?: number;
	pendencias?: number;
	projetosAtivos?: number;
}

export interface PersonDetail extends Person {
	kpis?: PersonKpis;
	specs?: Record<string, string>;
	qualification?: number;
}

export interface CreatePessoaPayload {
	name: string;
	email: string;
	phone?: string;
	type: PersonType;
	nivel: Nivel;
	grupoId?: string;
	isInstrutor?: boolean;
	qualification?: number;
}

export type UpdatePessoaPayload = Partial<CreatePessoaPayload>;

export interface PessoasResult {
	people: Person[];
	pagination: PageInfo;
}

export interface PsCandidate {
	id: string;
	name: string;
	initials: string;
	stage: PsStage;
	nota?: number | null;
	feedback?: string | null;
}

export interface PsGroup {
	id: string;
	code: string;
	name: string;
	stage: PsStage;
	membersCount: number;
	candidates: PsCandidate[];
}

export interface PsEvaluation {
	nota: number;
	feedback: string;
	resultado?: 'aprovado' | 'reprovado';
}

export interface PsListResult {
	groups: PsGroup[];
	counts: {
		inscritos: number;
		grupos: number;
		em_avaliacao: number;
		aprovados: number;
	};
}

export interface CreateGrupoPayload {
	name: string;
	tutorId: string;
	memberIds: string[];
}

export interface HoraApontamento {
	id: string;
	personId: string;
	person: { name: string; initials: string };
	date: string;
	startTime?: string;
	endTime?: string;
	hours: number;
	type: HoraTipo;
	ref?: string;
	status: HoraStatus;
	reason?: string;
	submittedBy?: string;
}

export interface ExtratoMensalHoras {
	periodo: string;
	validadas: number;
	pendentes: number;
	rejeitadas: number;
}

export interface HorasResult {
	hours: HoraApontamento[];
	counts: { pendentes: number; validadas: number; rejeitadas: number };
}

export interface RegistrarHorasPayload {
	personId: string;
	date: string;
	startTime: string;
	endTime: string;
	hours: number;
	type: HoraTipo;
	projectId?: string | null;
	orderId?: string | null;
	observation?: string;
}

export interface RejeitarHorasPayload {
	reason: string;
}

export interface TreinamentoInstrutor {
	id: string;
	name: string;
	initials: string;
}

export interface Treinamento {
	id: string;
	title: string;
	instructor: TreinamentoInstrutor;
	status: TrainingStatus;
	machine?: string;
	group?: GroupRef;
	doneCount?: number;
	groupSize?: number;
	average?: number;
}

export interface AgendaItem {
	id: string;
	title: string;
	startsAt: string;
	endsAt: string;
	status: TrainingStatus;
	present?: number;
}

export interface EvaluationCriterion {
	criterion: string;
	weight: number;
}

export interface FinalTask {
	title: string;
	dueDate?: string;
	description?: string;
}

export interface TreinamentoDetail extends Treinamento {
	track?: string;
	startsAt?: string;
	load?: string;
	nextClassAt?: string;
	agenda?: AgendaItem[];
	evaluation?: EvaluationCriterion[];
	finalTask?: FinalTask | null;
}

export interface TreinamentoSessao {
	id: string;
	startsAt: string;
	endsAt: string;
	present: number;
	absent?: number;
	materials?: string[];
}

export interface AvaliarTreinamentoPayload {
	nota: number;
	feedback: string;
	instructorId?: string;
	groupId?: string;
}

export interface AtribuirTarefaFinalPayload {
	title: string;
	description?: string;
	groupId?: string;
	modelo?: string;
	dataConclusao: string;
}

export interface TreinamentosResult {
	trainings: Treinamento[];
}

export interface Guia {
	id: string;
	title: string;
	version: string;
	status: GuideStatus;
	author: string;
	pages?: number;
	updatedAt?: string;
	maquinaId?: string;
	trilha?: string;
}

export interface GuiasResult {
	machines: { id: string; label: string; guides: Guia[] }[];
}

export interface InstrutorDisponibilidade {
	id: string;
	name: string;
	matricula?: string;
	maquinas?: string[];
	tarefasHojeHoras?: number;
	disponibilidade: DisponibilidadeStatus;
	nota?: string;
}

export interface NivelMembro {
	id: string;
	name: string;
	matricula: string;
	nivel: Nivel;
	grupo?: string;
}

export interface NivelMatrix {
	modulos: string[];
	niveis: Nivel[];
	matriz: Record<string, Record<Nivel, string>>;
	membros?: NivelMembro[];
}

export interface ConvidarUsuarioPayload {
	name: string;
	email: string;
	nivel: Nivel;
}
