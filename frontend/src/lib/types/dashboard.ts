export interface Task {
	id: string;
	title: string;
	module: string;
	due?: string | null;
	dueLabel?: string;
	urgent?: boolean;
}

export interface Kpi {
	value: number;
	total?: number;
	delta?: string;
	deltaTone?: 'success' | 'danger' | 'warn' | 'muted';
	restricted?: boolean;
}

export interface DashboardKpis {
	ordersActive: Kpi;
	loansOpen?: Kpi;
	machinesActive?: Kpi;
	notificationsUnread: Kpi;
}

export interface StatusSlice {
	status: string;
	label: string;
	count: number;
	color: string;
}

export interface ActivityItem {
	id: string;
	actor: string;
	verb: string;
	target: string;
	module: string;
	at: string;
}

export interface DashboardSummary {
	tasks: Task[];
	kpis: DashboardKpis;
	ordersByStatus: StatusSlice[];
	machinesByStatus: StatusSlice[];
	activity: ActivityItem[];
}

export interface UnreadCount {
	count: number;
}