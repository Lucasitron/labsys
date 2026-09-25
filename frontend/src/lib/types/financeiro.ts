export type TipoLancamento = 'Entrada' | 'Saída';
export type LancamentoStatus = 'Pendente' | 'Pago' | 'Atrasado' | 'Cancelado';
export type FechamentoStatus = 'Aberta' | 'Concluída' | 'Cancelada';
export type CompraStatus = 'Registrada' | 'Visualizada' | 'Concluída';
export type TipoDoacao = 'Doação' | 'Projeto';
export type CusteioTab = 'fechamentos' | 'custos' | 'valores' | 'compras';
export type RelatorioId =
	| 'fluxo'
	| 'dre'
	| 'lucratividade'
	| 'inadimplencia'
	| 'doacoes'
	| 'custo-maquina';

export type CategoriaTipo = 'Receita' | 'Despesa';
export type NivelAcesso = 0 | 1 | 2 | 3;

export interface Paginacao {
	page: number;
	pageSize: number;
	totalItems: number;
	totalPages: number;
}

export interface Paged<T> {
	items: T[];
	pagination: Paginacao;
}

export interface CategoriaFinanceira {
	id: string;
	nome: string;
	tipo: CategoriaTipo;
	descricao?: string;
}

export interface CreateCategoriaPayload {
	nome: string;
	tipo: CategoriaTipo;
	descricao?: string;
}

export interface CategoriaResumo {
	id: string;
	nome: string;
}

export interface Lancamento {
	id: string;
	codigo: string;
	categoria: CategoriaResumo;
	tipo: TipoLancamento;
	valor: number;
	dataVencimento: string;
	dataPagamento?: string;
	status: LancamentoStatus;
	idReferenciaExterna?: string;
	observacao?: string;
}

export interface LancamentoResumo {
	entradas: number;
	saidas: number;
	pendente: number;
}

export interface LancamentosResult {
	lancamentos: Lancamento[];
	pagination: Paginacao;
	counts: Record<LancamentoStatus, number>;
	resumo: LancamentoResumo;
}

export interface ListLancamentosParams {
	search?: string;
	status?: string;
	tipo?: TipoLancamento;
	categoria?: string;
	periodo?: string;
	ordenar?: string;
	page?: number;
	pageSize?: number;
}

export interface CreateLancamentoPayload {
	idCategoria: string;
	tipo: TipoLancamento;
	valor: number;
	dataVencimento: string;
	dataPagamento?: string | null;
	idReferenciaExterna?: string;
	observacao?: string;
}

export interface RegistrarPagamentoPayload {
	dataPagamento: string;
	observacao?: string;
}

export interface TotaisContas {
	mes: number;
	vencidas: number;
	pagasMes: number;
	proximos30d: number;
}

export interface DoacaoRecurso {
	id: string;
	tipo: TipoDoacao;
	origem: string;
	valor: number;
	dataRecebimento: string;
	idProjetoAssociado?: string | null;
}

export interface DoacoesResumo {
	recebidoAno: number;
	totalDoacoes: number;
	totalProjetos: number;
	mediaDoacao: number;
}

export interface DoacoesCobertura {
	percentual: number;
}

export interface DoacoesResult {
	registros: DoacaoRecurso[];
	resumo: DoacoesResumo;
	cobertura: DoacoesCobertura;
}

export interface ListDoacoesParams {
	search?: string;
	tipo?: TipoDoacao;
	periodo?: string;
	page?: number;
}

export interface CreateDoacaoPayload {
	tipo: TipoDoacao;
	origem: string;
	valor: number;
	dataRecebimento: string;
	idProjetoAssociado?: string | null;
}

export interface ValorHoraNivel {
	nivelAcesso: NivelAcesso;
	valorHora: number;
	dataVigencia: string;
}

export interface DefinirValorHoraPayload {
	nivelAcesso: NivelAcesso;
	valorHora: number;
	dataVigencia: string;
}

export interface ParametroOverhead {
	valorTaxaHora: number;
	dataVigencia: string;
}

export interface DefinirOverheadPayload {
	valorTaxaHora: number;
	dataVigencia: string;
}

export interface FechamentoEncomenda {
	id: string;
	idEncomenda: string;
	horasEstimadas: number;
	valorFechado: number;
	dataFechamento: string;
	status: FechamentoStatus;
}

export interface CreateFechamentoPayload {
	idEncomenda: string;
	horasEstimadas: number;
	valorFechado: number;
	dataFechamento: string;
}

export interface CustoEncomenda {
	custoMateriais: number;
	custoMaoObra: number;
	custoOverhead: number;
	custoTotal: number;
	valorVenda: number;
	margemLucro: number;
	dataCalculo: string;
}

export interface SolicitacaoCompra {
	id: string;
	item: string;
	quantidade: number;
	valorEstimado: number;
	status: CompraStatus;
	lancamentoGerado?: string;
}

export interface CreateSolicitacaoCompraPayload {
	item: string;
	quantidade: number;
	valorEstimado: number;
	observacao?: string;
}

export interface ConcluirCompraPayload {
	dataConclusao: string;
	valorReal: number;
	notaFiscal?: string;
}

export interface ConcluirCompraResult {
	status: CompraStatus;
	lancamentoGerado: string;
}

export interface ResumoFinanceiro {
	saldoCaixa: number;
	aReceber: number;
	inadimplentes: number;
	aPagar: number;
	vencem7d: number;
	custoMedio: number;
	margemMedia: number;
}

export interface FluxoCaixaLinha {
	periodo: string;
	entradas: number;
	saidas: number;
	liquido: number;
}

export interface FluxoCaixa {
	linhas: FluxoCaixaLinha[];
	total: number;
}

export interface Dre {
	receitaOperacional: number;
	custosDiretos: number;
	despesasOperacionais: number;
	doacoesRecursos: number;
	resultado: number;
}

export interface LucratividadeLinha {
	idEncomenda: string;
	valorVenda: number;
	custoTotal: number;
	margemReais: number;
	margemPercentual: number;
}

export interface Lucratividade {
	linhas: LucratividadeLinha[];
}

export interface InadimplenciaLinha {
	cliente: string;
	referencia: string;
	vencimento: string;
	diasAtraso: number;
	valor: number;
}

export interface Inadimplencia {
	linhas: InadimplenciaLinha[];
}

export interface DoacoesDespesasLinha {
	mes: string;
	doacoes: number;
	despesas: number;
	saldo: number;
}

export interface DoacoesDespesas {
	linhas: DoacoesDespesasLinha[];
	acumulado: number;
}

export interface CustoMaquinaLinha {
	maquina: string;
	horas: number;
	custo: number;
	percentual: number;
}

export interface CustoMaquina {
	linhas: CustoMaquinaLinha[];
	total: number;
}

export interface RelatorioPeriodoParams {
	periodo?: string;
}
