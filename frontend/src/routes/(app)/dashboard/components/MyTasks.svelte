<script lang="ts">
	import { goto } from '$app/navigation';
	import { completeTask } from '$lib/api/dashboard';
	import { moduleLabel } from '$lib/config/menu';
	import type { Task } from '$lib/types/dashboard';

	interface Props {
		tasks: Task[];
		error?: boolean;
		empty?: boolean;
		onRefetch?: () => void;
	}

	let { tasks, error = false, empty = false, onRefetch = (): void => {} }: Props = $props();

	let items = $state<Task[]>([]);
	let pending = $state<Set<string>>(new Set());

	$effect(() => {
		items = tasks;
	});

	async function toggle(task: Task): Promise<void> {
		if (pending.has(task.id)) return;

		pending = new Set(pending).add(task.id);
		items = items.filter((item) => item.id !== task.id);

		try {
			await completeTask(task.id);
		} catch {
			// mantém a tarefa visível e permite nova tentativa
			items = tasks;
		} finally {
			const next = new Set(pending);
			next.delete(task.id);
			pending = next;
		}

		onRefetch();
	}

	const pendingCount = $derived(items.length);

	function badgeClass(task: Task): string {
		return task.urgent
			? 'border border-danger/30 bg-danger/10 font-medium text-danger'
			: 'font-medium text-muted border border-border bg-elevated';
	}

	function subtitle(task: Task): string {
		const base = moduleLabel(task.module);
		return task.urgent ? `${base} · vence hoje` : base;
	}
</script>

<section class="rounded-xl border border-border bg-surface p-5" data-testid="my-tasks">
	<div class="mb-4 flex items-center justify-between">
		<div class="flex items-center gap-2">
			<svg
				class="h-4 w-4 text-brand"
				fill="none"
				viewBox="0 0 24 24"
				stroke="currentColor"
				stroke-width="2"
				aria-hidden="true"
			>
				<path
					stroke-linecap="round"
					stroke-linejoin="round"
					d="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
				/>
			</svg>
			<h2 class="text-sm font-semibold">Minhas tarefas</h2>
			<span
				class="rounded border border-brand/30 bg-brand/10 px-1.5 py-0.5 text-[10px] font-medium text-brand"
			>
				{pendingCount} pendentes
			</span>
		</div>
		<a href="/producao/tarefas" class="text-xs text-muted transition hover:text-brand">
			Ver todas →
		</a>
	</div>

	{#if error}
		<div class="flex flex-col items-center justify-center py-10 text-center">
			<svg
				class="mb-3 h-8 w-8 text-muted"
				fill="none"
				viewBox="0 0 24 24"
				stroke="currentColor"
				stroke-width="1.5"
				aria-hidden="true"
			>
				<path
					stroke-linecap="round"
					stroke-linejoin="round"
					d="M12 9v3.75m9-.75a9 9 0 11-18 0 9 9 0 0118 0zm-9 3.75h.008v.008H12v-.008z"
				/>
			</svg>
			<p class="mb-3 text-xs text-muted">Não foi possível carregar suas tarefas.</p>
			<button type="button" onclick={onRefetch} class="text-xs font-medium text-brand transition hover:text-brandhi">
				Tentar novamente
			</button>
		</div>
	{:else if items.length === 0}
		<div class="flex min-h-[200px] flex-col items-center justify-center text-center">
			<div
				class="mb-4 flex h-12 w-12 items-center justify-center rounded-2xl border border-brand/30 bg-brand/10"
			>
				<svg
					class="h-6 w-6 text-brand"
					fill="none"
					viewBox="0 0 24 24"
					stroke="currentColor"
					stroke-width="2"
					aria-hidden="true"
				>
					<path
						stroke-linecap="round"
						stroke-linejoin="round"
						d="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
					/>
				</svg>
			</div>
			<h2 class="mb-1 text-sm font-semibold">Sem tarefas por aqui</h2>
			<p class="max-w-xs text-xs text-muted">
				Quando algo for atribuído a você, aparecerá nesta lista.
			</p>
			<button
				type="button"
				onclick={() => goto('/producao')}
				class="mt-5 rounded-md bg-brand px-3 py-2 text-xs font-medium text-white shadow-lg shadow-brand/20 transition hover:bg-brandhi"
			>
				Explorar módulos
			</button>
		</div>
	{:else}
		<ul class="-mx-1 divide-y divide-border">
			{#each items as task (task.id)}
				<li data-testid="task-item" class="flex items-start gap-3 px-1 py-3">
					<input
						id={`task-${task.id}`}
						type="checkbox"
						onchange={() => toggle(task)}
						class="mt-0.5 h-4 w-4 shrink-0 cursor-pointer accent-brand"
						aria-label={`Concluir tarefa: ${task.title}`}
					/>
					<label
						for={`task-${task.id}`}
						class="flex min-w-0 flex-1 cursor-pointer items-start gap-3 rounded transition hover:bg-elevated/40"
					>
						<div class="min-w-0 flex-1">
							<p class="text-sm text-ink">{task.title}</p>
							<p class="mt-0.5 text-xs text-muted">{subtitle(task)}</p>
						</div>
						<span class="shrink-0 rounded px-1.5 py-0.5 text-[10px] {badgeClass(task)}">
							{task.dueLabel ?? 'Sem prazo'}
						</span>
					</label>
				</li>
			{/each}
		</ul>
	{/if}
</section>