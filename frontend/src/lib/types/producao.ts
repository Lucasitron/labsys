export type Tone = 'success' | 'warn' | 'danger' | 'brand' | 'muted' | 'ink';

export interface NomeFunc {
	id: string;
	nome: string;
}

export interface Paginacao {
	page: number;
	pageSize: number;
	totalItems: number;
	totalPages: number;
}

export interface Paginado<T> {
	dados: T[];
	paginacao: Paginacao;
}

// ---- Unions de status/prioridade (strings PT servidas pelo backend) ----

export type ProjetoStatus = 'Planejado' | 'Em andamento' | 'Concluído' | 'Cancelado';

export type TarefaStatus = 'Pendente' | 'Em andamento' | 'Concluída' | 'Atrasada' | 'Bloqueada';

export type Prioridade = 'Baixa' | 'Média' | 'Moderada' | 'Alta';

export type KanbanColuna = 'Fila' | 'Produção' | 'Acabamento' | 'Pronto' | 'Entregue';

export type Cartao5S = 'Verde' | 'Amarelo' | 'Vermelho';

export type MaquinaStatus = 'Disponível' | 'Em manutenção' | 'Suspensa';

export type ChamadoStatus = 'Aberto' | 'Atribuído' | 'Em andamento' | 'Resolvido' | 'Aguardando compra';

export type PreventivaStatus = 'Programada' | 'Concluída' | 'Atrasada';

export type InspecaoStatus = 'Programada' | 'Concluída' | 'Atrasada';

export type Ciclo5S = 'semanal' | 'quinzenal' | 'mensal';

export type GravidadePendencia = 'Leve' | 'Moderada' | 'Grave';

export type SituacaoPendencia = 'Aberta' | 'Resolvida';

export type PenalidadeTipo = 'Aviso' | 'Advertência' | 'Suspensão' | 'Expulsão';

export type PenalidadeStatus = 'Ativa' | 'Cumprida';

export type SituacaoMesas = 'Aprovada' | 'Pendente';

export type MesaStatus = SituacaoMesas;

export type MesaAuditoriaResultado = 'ATIVO' | 'ABANDONADO';

export type ChaveParametro5S =
	| 'rotacaoDias'
	| 'diasParaAuditoriaProjeto'
	| 'diaSemanaInspecao'
	| 'periodoExperimentalAtivo'
	| 'notaCartaoVerde'
	| 'notaCartaoAmarelo'
	| 'prazoPendGrave';

export type TipoDocumentoProjeto = 'arquivo' | 'imagem' | 'link';

// ---- Projetos ----

export interface Projeto {
	id: string;
	codigo: string;
	nome: string;
	cliente?: string;
	descricao?: string;
	responsavel: NomeFunc;
	prazo: string;
	status: ProjetoStatus;
	progresso: number;
}

export interface ProjetoDocumento {
	id: string;
	tipo: TipoDocumentoProjeto;
	nome?: string;
	url: string;
	criadoEm: string;
}

export interface MaterialProjeto {
	item: string;
	codigo: string;
	quantidadeNecessaria: number;
	disponivel: number;
}

export interface SaidaMaterial {
	projetoId: string;
	item: string;
	codigo: string;
	quantidade: number;
	registradoEm: string;
}

// ---- Tarefas ----

export interface Tarefa {
	id: string;
	codigo: string;
	titulo: string;
	descricao?: string;
	projeto?: { id: string; codigo: string; nome: string } | null;
	responsavel: NomeFunc;
	prioridade: Prioridade;
	prazo: string;
	status: TarefaStatus;
	pendencia5s?: { id: string; gravidade: GravidadePendencia; titulo: string } | null;
}

// ---- Kanban de encomendas ----

export interface KanbanItem {
	id: string;
	codigo: string;
	titulo: string;
	coluna: KanbanColuna;
	cliente?: string;
	prioridade?: Prioridade;
	prazo?: string;
	responsavel?: NomeFunc | null;
}

export type ContagemKanban = Record<KanbanColuna, number>;

// ---- Máquinas ----

export interface MaquinaResumo {
	id: string;
	codigo?: string;
	nome: string;
}

export interface Maquina {
	id: string;
	codigo: string;
	nome: string;
	categoria: string;
	responsavelManutencao: NomeFunc;
	status: MaquinaStatus;
	cartao: Cartao5S;
}

export interface ChamadoMaquina {
	id: string;
	codigo: string;
	maquina: MaquinaResumo;
	classificacao: string;
	relato: string;
	responsavel: NomeFunc;
	status: ChamadoStatus;
	pendenteCompra?: boolean;
}

export interface Preventiva {
	id: string;
	codigo: string;
	maquina: MaquinaResumo;
	dataProgramada: string;
	status: PreventivaStatus;
}

// ---- Sistema 5S ----

export interface SetorRef {
	id: string;
	nome: string;
}

export interface ResponsavelSetor {
	membro: NomeFunc;
	ps: boolean;
}

export interface Setor5S {
	id: string;
	nome: string;
	ciclo: Ciclo5S;
	responsaveis: ResponsavelSetor[];
	auditor: NomeFunc;
	nota?: number | null;
	cartao: Cartao5S;
	ultimaAuditoria?: string | null;
	proximaAuditoria: string;
}

export interface ItemInspecao5S {
	s: number;
	conforme: boolean;
}

export interface Inspecao5S {
	id: string;
	setor: SetorRef;
	auditor: NomeFunc;
	data: string;
	status: InspecaoStatus;
	nota?: number | null;
	cartao?: Cartao5S | null;
	itens: ItemInspecao5S[];
}

export interface Pendencia5S {
	id: string;
	setor: SetorRef;
	titulo: string;
	gravidade: GravidadePendencia;
	responsavel: NomeFunc;
	prazo: string;
	status: SituacaoPendencia;
	resolvidaEm?: string | null;
	origem?: { inspecaoId: string; data: string; auditor: NomeFunc } | null;
}

export interface Penalidade5S {
	id: string;
	membro: NomeFunc;
	tipo: PenalidadeTipo;
	status: PenalidadeStatus;
	data: string;
	motivo: string;
}

export interface RankingItem {
	mes: string;
	posicao: number;
	responsavel: NomeFunc;
	nota: number;
	cartao: Cartao5S;
	top1: boolean;
	semanaLivre: boolean;
}

export interface Parametro5S {
	id: string;
	chave: ChaveParametro5S;
	valor: number | string | boolean;
	descricao?: string;
}

// ---- Mesas de projeto ----

export interface AuditoriaMesa {
	resultado: MesaAuditoriaResultado;
	data: string;
	acao?: string;
}

export interface Mesa5S {
	id: string;
	nome: string;
	projeto: { id: string; codigo: string } | null;
	membro: NomeFunc;
	status: SituacaoMesas;
	qrTotem: string;
	periodoExperimental: boolean;
	ultimaAuditoria?: AuditoriaMesa | null;
}

// ---- Resumo (tela raiz /producao) ----

export interface CartoesCount {
	verde: number;
	amarelo: number;
	vermelho: number;
}

export interface KpisProducao {
	projetosAtivos: number;
	tarefasPendentes: number;
	tarefasAtrasadas: number;
	maquinas: number;
	maquinasForaVerde: number;
	cartoes: CartoesCount;
	kanban: ContagemKanban;
}

export interface AlertaProducao {
	id: string;
	severidade: 'info' | 'warn' | 'danger' | 'success';
	titulo: string;
	descricao?: string;
	dataGoto?: string;
}

export interface ResumoProducao {
	kpis: KpisProducao;
	alertas: AlertaProducao[];
	rankingTop: RankingItem[];
	setores: Setor5S[];
	maquinas: Maquina[];
}