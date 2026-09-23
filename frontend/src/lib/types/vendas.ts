import type { Tone } from '$lib/types/stock';

export type TipoPessoa = 'pf' | 'pj';
export type OrcamentoStatus = 'Pendente' | 'Aprovado' | 'Recusado' | 'Ajuste';
export type KanbanStatus = 'Fila' | 'Produção' | 'Acabamento' | 'Pronto' | 'Entregue';
export type OrdemOrigem = 'Orçamento' | 'Venda direta' | 'Marketplace';
export type Plataforma = 'Mercado Livre' | 'Shopee' | 'Elo7';
export type TipoInteracao = 'E-mail' | 'Telefone' | 'Reunião' | 'WhatsApp';
export type TarefaStatus = 'Pendente' | 'Em Andamento' | 'Concluída';
export type Prioridade = 'Baixa' | 'Média' | 'Alta';
export type TipoSolicitacao =
	| 'Alteração de dados'
	| 'Mudança de status'
	| 'Mover encomenda'
	| 'Outra';
export type SolicitacaoStatus = 'Pendente' | 'Aprovada' | 'Rejeitada';
export type SolicitacaoAlvoTipo = 'CLI' | 'OC' | 'EN';

export interface Tag {
	id: string;
	nome: string;
	cor: Tone;
}

export interface ClienteResumo {
	id: string;
	nome: string;
}

export interface Cliente {
	id: string;
	codigo: string;
	tipoPessoa: TipoPessoa;
	nome: string;
	documento: string;
	email: string;
	telefone: string;
	endereco?: string;
	tags: Tag[];
	dataCadastro: string;
	createdBy: string;
	updatedAt: string;
}

export interface CreateClientePayload {
	tipoPessoa: TipoPessoa;
	nome: string;
	documento: string;
	email: string;
	telefone: string;
	endereco?: string;
	tagIds?: string[];
	observacoes?: string;
}

export type UpdateClientePayload = Partial<CreateClientePayload>;

export interface CreateTagPayload {
	nome: string;
	cor: Tone;
}

export interface ItemMaterial {
	tipo: string;
	quantidade: number;
	unidade: string;
}

export interface ItemOrcamento {
	descricao: string;
	quantidade: number;
	valorUnitario: number;
	material?: ItemMaterial;
	horas?: number;
	compra?: boolean;
}

export interface Orcamento {
	id: string;
	codigo: string;
	cliente: ClienteResumo;
	valorTotal: number;
	status: OrcamentoStatus;
	validade: string;
	qtdItens: number;
	createdBy: string;
	itens?: ItemOrcamento[];
	observacoes?: string;
	encomendaId?: string;
}

export interface CreateOrcamentoPayload {
	clienteId: string;
	itens: ItemOrcamento[];
	validade: string;
	observacoes?: string;
	desconto?: number;
}

export type UpdateOrcamentoPayload = Partial<CreateOrcamentoPayload>;

export interface Encomenda {
	id: string;
	codigo: string;
	cliente: ClienteResumo;
	statusKanban: KanbanStatus;
	origem: OrdemOrigem;
	previsao: string;
	valorFinal: number;
	itensCount: number;
	createdBy: string;
}

export interface CreateEncomendaPayload {
	idOrcamento?: string;
	clienteId?: string;
	origem?: OrdemOrigem;
	valorFinal?: number;
	previsao?: string;
	observacoes?: string;
}

export interface MoverKanbanPayload {
	statusKanban: KanbanStatus;
	observacao?: string;
}

export interface HistoricoEncomenda {
	statusAnterior: KanbanStatus;
	statusNovo: KanbanStatus;
	dataAlteracao: string;
	responsavel: string;
	observacao?: string;
}

export interface RegistroMarketplace {
	id: string;
	plataforma: Plataforma;
	codigoExterno: string;
	encomenda: { codigo: string; valor: number };
	clienteNome: string;
	valorBruto: number;
	valorTaxa: number;
	valorLiquido: number;
	dataVenda: string;
	createdBy: string;
}

export interface RegistrarVendaPayload {
	encomendaId: string;
	plataforma: Plataforma;
	codigoExterno: string;
	dataVenda: string;
	valorTaxa: number;
}

export interface Interacao {
	id: string;
	clienteId: string;
	tipo: TipoInteracao;
	descricao: string;
	dataInteracao: string;
	responsavel: string;
}

export interface RegistrarInteracaoPayload {
	clienteId: string;
	tipo: TipoInteracao;
	descricao: string;
	dataInteracao: string;
	responsavelId?: string;
}

export interface TarefaMarketing {
	id: string;
	titulo: string;
	descricao?: string;
	responsavelId: string;
	dataInicio?: string;
	dataFim?: string;
	status: TarefaStatus;
	prioridade: Prioridade;
	createdBy: string;
}

export interface CreateTarefaPayload {
	titulo: string;
	descricao?: string;
	responsavelId: string;
	dataInicio?: string;
	dataFim: string;
	prioridade: Prioridade;
	status?: TarefaStatus;
}

export type UpdateTarefaPayload = Partial<CreateTarefaPayload>;

export interface SolicitacaoAlvo {
	tipo: SolicitacaoAlvoTipo;
	id: string;
	nome: string;
}

export interface SolicitacaoEdicao {
	id: string;
	tipo: TipoSolicitacao;
	alvo: SolicitacaoAlvo;
	campo?: string;
	valorAtual?: string;
	valorProposto?: string;
	justificativa: string;
	status: SolicitacaoStatus;
	solicitante: string;
	motivo?: string;
	createdBy: string;
}

export interface CreateSolicitacaoPayload {
	tipo: TipoSolicitacao;
	alvo: SolicitacaoAlvo;
	campo?: string;
	valorAtual?: string;
	valorProposto?: string;
	justificativa: string;
}

export interface DecidirSolicitacaoPayload {
	aprovada: boolean;
	motivo?: string;
}

export interface Pagination {
	page: number;
	pageSize: number;
	totalItems: number;
	totalPages: number;
}

export interface Paged<T> {
	items: T[];
	pagination: Pagination;
}

export interface ClientesResult {
	clientes: Cliente[];
	pagination: Pagination;
}

export interface OrcamentosResult {
	orcamentos: Orcamento[];
	counts: Record<string, number>;
}

export interface EncomendasResult {
	encomendas: Encomenda[];
	counts: Record<string, number>;
}

export interface MarketplaceTotais {
	bruto: number;
	taxas: number;
	liquido: number;
}

export interface MarketplaceResult {
	registros: RegistroMarketplace[];
	totais: MarketplaceTotais;
}

export interface TarefasResult {
	tarefas: TarefaMarketing[];
	counts: Record<string, number>;
}

export interface SolicitacoesResult {
	solicitacoes: SolicitacaoEdicao[];
	counts: Record<string, number>;
}

export interface ListClientesParams {
	search?: string;
	tipo?: TipoPessoa;
	tags?: string;
	ordenar?: string;
	page?: number;
	pageSize?: number;
}

export interface ListOrcamentosParams {
	status?: OrcamentoStatus;
	search?: string;
	clienteId?: string;
	page?: number;
	pageSize?: number;
}

export interface ListEncomendasParams {
	status_kanban?: KanbanStatus;
	search?: string;
	clienteId?: string;
}

export interface ListMarketplaceParams {
	plataforma?: Plataforma;
	page?: number;
	pageSize?: number;
}

export interface ListTarefasParams {
	search?: string;
	status?: TarefaStatus;
	prioridade?: Prioridade;
	page?: number;
	pageSize?: number;
}

export interface ListSolicitacoesParams {
	status?: SolicitacaoStatus;
}
