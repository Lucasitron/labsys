// ---- Primitivas e tipos de exibição (derivados client) ----

export type Tone = 'success' | 'warn' | 'danger' | 'brand' | 'muted' | 'ink';

export interface IdLabel {
	id: string;
	label: string;
}

export interface PageInfo {
	page: number;
	pageSize: number;
	totalItems: number;
	totalPages: number;
}

export interface Pagination<T> {
	items: T[];
	page: number;
	pageSize: number;
	total: number;
}

export interface FilterOption {
	id: string;
	label: string;
	count: number;
}

export type ItemStatus = 'available' | 'low' | 'out' | 'loaned' | 'maintenance';
export type MovementType = 'in' | 'out';
export type EntryKind = 'compra' | 'doacao' | 'devolucao' | 'ajuste';
export type TipoSaida = 'CONSUMO' | 'PERDA' | 'AJUSTE' | 'EMPRESTIMO';
export type ExitReason = TipoSaida;
export type LoanStatus = 'ATIVO' | 'DEVOLVIDO' | 'ATRASADO';
export type LoanComputed = 'no_prazo' | 'vence_hoje' | 'atrasado';
export type LoanCondition = 'bom' | 'avaria' | 'danificado';
export type LoanTab = 'ativos' | 'atrasados' | 'historico';

// ---- Backend Categoria (enum) ----

export type CategoriaEnum = 'INSUMO' | 'FERRAMENTA' | 'PECA';

// ---- Localizações (backend LocalizacaoResponse) ----

export interface LocalizacaoResp {
	id: string;
	armario: string;
	prateleira?: string;
	caixa?: string;
	descricao?: string;
}

// ---- Fornecedores (backend FornecedorResponse) ----

export interface Fornecedor {
	id: string;
	nome: string;
	contato?: string;
	cnpj?: string;
}

// ---- Itens (backend ItemResponse) ----

export interface StockItem {
	id: string;
	nome: string;
	descricao?: string;
	categoria: CategoriaEnum;
	unidadeMedida: string;
	quantidadeAtual: number;
	estoqueMinimo: number;
	localizacao: LocalizacaoResp | null;
	// Derivado client (quantidadeAtual × estoqueMinimo) — backend não expõe `status`.
	status: ItemStatus;
	// UI-only: backend não expõe estoque em empréstimo / data de atualização.
	activeLoans?: number;
	updatedAt?: string;
}

export interface ItemDetail extends StockItem {
	// UI-only (D-3): área de operação derivada das telas atuais (backend não expõe).
}

export interface CreateItemPayload {
	nome: string;
	descricao?: string;
	categoria: CategoriaEnum;
	unidadeMedida: string;
	quantidadeAtual: number;
	estoqueMinimo: number;
	idLocalizacao?: string | null;
}

export type UpdateItemPayload = Partial<CreateItemPayload>;

// ---- Itens: tela lista (filtros/paginação client-side, R-9) ----

export interface ItemFilters {
	categories: FilterOption[];
	locations: FilterOption[];
	statuses: FilterOption[];
}

export interface ItemsResult {
	items: StockItem[];
	pagination: PageInfo;
	filters: ItemFilters;
}

// ---- Entradas / Saídas (backend EntradaResponse / SaidaResponse) ----

export interface EntradaResponse {
	id: string;
	idItem: string;
	nomeItem?: string;
	quantidade: number;
	valorUnitario?: number | null;
	dataEntrada: string;
	idFornecedor?: string | null;
	fornecedor?: Fornecedor | null;
	notaFiscal?: string | null;
	observacao?: string | null;
}

export interface SaidaResponse {
	id: string;
	idItem: string;
	nomeItem?: string;
	quantidade: number;
	tipoSaida: TipoSaida;
	dataSaida: string;
	idReferencia?: string | null;
	observacao?: string | null;
}

export interface CreateEntradaPayload {
	idItem: string;
	idFornecedor?: string | null;
	quantidade: number;
	valorUnitario?: number | null;
	dataEntrada?: string;
	notaFiscal?: string | null;
	observacao?: string | null;
}

export interface CreateSaidaPayload {
	idItem: string;
	quantidade: number;
	tipoSaida: TipoSaida;
	idReferencia?: string | null;
	observacao?: string | null;
}

// ---- Movimentações: view unificada p/ histórico de item (G-7) ----

export interface MovementDetail {
	id: string;
	tipo: 'entrada' | 'saida';
	itemNome: string;
	unidadeMedida: string;
	quantidade: number;
	data: string;
	referencia?: string | null;
	valorUnitario?: number | null;
	fornecedor?: Fornecedor | null;
	tipoSaida?: TipoSaida;
	observacao?: string | null;
}

// View de movimentação para as telas atuais (bloco 2 migra para MovementDetail).
export interface Movement {
	id: string;
	type: MovementType;
	kind?: EntryKind;
	reason?: ExitReason;
	item: { id: string; code?: string; name: string; unit: string };
	quantity: number;
	date: string;
	origin?: string;
	destination?: string;
	reference?: string;
	responsible?: { id: string; name: string; initials: string } | null;
	unitValue?: number | null;
	observation?: string;
}

export interface MovementSummary {
	entries?: { count: number; sum: number };
	exits?: { count: number; sum: number };
}

export interface MovementFilters {
	kinds: FilterOption[];
	reasons: FilterOption[];
	periods: FilterOption[];
}

export interface MovementsResult {
	movements: Movement[];
	pagination: PageInfo;
	filters: MovementFilters;
}

export interface ItemMovementsResult {
	summary: MovementSummary;
	movements: Movement[];
	pagination: PageInfo;
}

// ---- Empréstimos (backend EmprestimoResponse) ----

export interface Emprestimo {
	id: string;
	idItem: string;
	nomeItem: string;
	idPessoa: string;
	quantidade: number;
	dataEmprestimo: string;
	dataDevolucaoPrevista: string;
	dataDevolucaoReal: string | null;
	status: LoanStatus;
	observacao?: string;
	loanComputed: LoanComputed;
}

export interface CreateEmprestimoPayload {
	idItem: string;
	idPessoa: string;
	quantidade: number;
	dataDevolucaoPrevista: string;
	observacao?: string;
}

// ---- BOM (backend BomResponse) ----

export interface BomItemResponse {
	idItem: string;
	nomeItem: string;
	quantidadePrevista: number;
	quantidadeReal: number;
}

export interface BomResponse {
	id: string;
	idProdutoServico: string;
	nome: string;
	versao: string;
	editavel: boolean;
	itens: BomItemResponse[];
}

export interface BomItemPayload {
	idItem: string;
	quantidadePrevista: number;
}

export interface CreateBomPayload {
	idProdutoServico: string;
	nome: string;
	versao: string;
	itens: BomItemPayload[];
}

export type UpdateBomPayload = Partial<CreateBomPayload>;

export interface RegistrarConsumoPayload {
	itens: { idItem: string; quantidade: number }[];
}

// ---- Cadastros ----

export interface CreateSupplierPayload {
	nome: string;
	contato?: string;
	cnpj?: string;
}

export interface CreateLocationPayload {
	armario: string;
	prateleira?: string;
	caixa?: string;
	descricao?: string;
}