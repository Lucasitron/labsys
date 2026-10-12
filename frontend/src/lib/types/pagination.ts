// Tipos de paginação compartilhados (F-11).
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
