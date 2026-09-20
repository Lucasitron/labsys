export type ItemStatus = 'available' | 'low' | 'out' | 'loaned' | 'maintenance';
export type MovementType = 'in' | 'out';
export type EntryKind = 'compra' | 'doacao' | 'devolucao' | 'ajuste';
export type ExitReason = 'projeto' | 'consumo_interno' | 'perda' | 'descarte';
export type LoanStatus = 'ativo' | 'devolvido';
export type LoanComputed = 'no_prazo' | 'vence_hoje' | 'atrasado';
export type LoanCondition = 'bom' | 'avaria' | 'danificado';
export type Tone = 'success' | 'warn' | 'danger' | 'brand' | 'muted' | 'ink';

export interface IdLabel {
	id: string;
	label: string;
}

export type Category = IdLabel;
export type Location = IdLabel;

export interface PageInfo {
	page: number;
	pageSize: number;
	totalItems: number;
	totalPages: number;
}

export interface FilterOption {
	id: string;
	label: string;
	count: number;
}

// ---- Itens ----

export interface StockItem {
	id: string;
	code: string;
	name: string;
	category: Category;
	location: Location;
	quantity: {
		current: number;
		minimum: number;
		unit: string;
	};
	status: ItemStatus;
	activeLoans: number;
	updatedAt: string;
}

export interface StockParams {
	page: number;
	pageSize: number;
	search: string;
	categories: string[];
	locations: string[];
	statuses: string[];
	sort?: string;
}

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

export interface CreateItemPayload {
	code: string;
	name: string;
	description?: string;
	categoryId: string;
	unit: string;
	locationId: string;
	initialQuantity?: number;
	minimumQuantity?: number;
	maximumQuantity?: number;
	reorderPoint?: number;
	leadTimeDays?: number;
	unitValue?: number | null;
}

export interface ItemDetail {
	id: string;
	code: string;
	name: string;
	description?: string;
	category: Category;
	location: Location;
	unit: string;
	current: number;
	minimum: number;
	maximum?: number | null;
	reorderPoint?: number | null;
	leadTimeDays?: number | null;
	unitValue?: number | null;
	status: ItemStatus;
	activeLoans: number;
	updatedBy?: string | null;
	updatedRelative?: string | null;
	lastEntry?: { at: string; quantity: number; kind: EntryKind } | null;
	lastLoan?: { at: string } | null;
	bomUsage?: { projectId: string; projectName: string; qty: number; unit: string }[];
}

export interface HistoryEntry {
	id: string;
	title: string;
	by: string;
	at: string;
	tone: Tone;
}

export interface ItemHistoryResult {
	history: HistoryEntry[];
}

// ---- Movimentações (Entradas/Saídas) ----

export interface Movement {
	id: string;
	type: MovementType;
	kind?: EntryKind;
	reason?: ExitReason;
	item: { id: string; code: string; name: string; unit: string };
	quantity: number;
	date: string;
	origin?: string;
	destination?: string;
	reference?: string;
	responsible?: { id: string; name: string; initials: string } | null;
	unitValue?: number | null;
	observation?: string;
}

export interface MovementParams {
	type: MovementType;
	page: number;
	pageSize: number;
	search: string;
	kinds: string[];
	reasons: string[];
	period: string;
	itemId?: string;
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

export interface CreateEntryPayload {
	type: 'in';
	kind: EntryKind;
	itemId: string;
	quantity: number;
	unit: string;
	date: string;
	supplierId: string;
	notaFiscal?: string;
	unitValue?: number | null;
	observation?: string;
}

export interface CreateExitPayload {
	type: 'out';
	reason: ExitReason;
	itemId: string;
	quantity: number;
	unit: string;
	date: string;
	responsibleId: string;
	projectId?: string | null;
	observation?: string;
}

export type CreateMovementPayload = CreateEntryPayload | CreateExitPayload;

export interface MovementSummary {
	entries?: { count: number; sum: number };
	exits?: { count: number; sum: number };
}

export interface ItemMovementsResult {
	summary: MovementSummary;
	movements: Movement[];
	pagination: PageInfo;
}

// ---- Empréstimos ----

export type LoanTab = 'ativos' | 'atrasados' | 'historico';

export interface Loan {
	id: string;
	item: { id: string; code: string; name: string; unit: string };
	borrower: { id: string; name: string; initials: string };
	quantity: number;
	borrowDate: string;
	dueDate: string;
	status: LoanStatus;
	computed: LoanComputed;
	overdueDays: number;
	purpose?: string;
	condition?: LoanCondition | null;
	returnDate?: string | null;
	returnedQuantity?: number;
	responsible?: string;
}

export interface LoanParams {
	tab: LoanTab;
	page: number;
	pageSize: number;
	search: string;
}

export interface LoansResult {
	loans: Loan[];
	pagination: PageInfo;
	counts: { ativos: number; atrasados: number };
}

export interface CreateLoanPayload {
	itemId: string;
	borrowerId: string;
	quantity: number;
	borrowDate: string;
	dueDate: string;
	purpose?: string;
	observation?: string;
}

export interface ReturnLoanPayload {
	returnDate: string;
	quantityReturned: number;
	condition: LoanCondition;
	observation?: string;
}

// ---- Fornecedores ----

export interface Supplier {
	id: string;
	name: string;
	email?: string;
	cnpj?: string;
	contactPhone?: string;
	itemsCount: number;
	rating: number;
	lastPurchase?: {
		date: string;
		value?: number | null;
		notaFiscal?: string;
	} | null;
}

export interface SuppliersResult {
	suppliers: Supplier[];
	pagination: PageInfo;
}

// ---- Localizações ----

export interface StockLocation {
	id: string;
	code: string;
	name: string;
	description?: string;
	itemsCount: number;
	unitsCount: number;
	capacityPercent: number;
	capacityVariant: Tone;
}

export interface LocationsResult {
	locations: StockLocation[];
}

// ---- BOM ----

export interface BomRow {
	item: { code: string; name: string };
	qtyPerUnit: number;
	unit: string;
	units: number;
	totalNeed: number;
	available: number;
	missing: number;
	status: 'ok' | 'faltam';
}

export interface BomReport {
	bomId: string;
	projectName: string;
	units: number;
	short: string[];
	items: BomRow[];
	generated: boolean;
}

export interface BomOrderResult {
	orderCode: string;
	items: string[];
}

export interface ProjectOption {
	id: string;
	code: string;
	name: string;
}