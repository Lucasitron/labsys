import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/svelte';
import MyTasks from '../../src/routes/(app)/dashboard/components/MyTasks.svelte';
import type { Task } from '$lib/types/dashboard';

const { completeTaskMock } = vi.hoisted(() => ({
	completeTaskMock: vi.fn()
}));

vi.mock('$lib/api/dashboard', () => ({
	completeTask: completeTaskMock
}));

vi.mock('$app/navigation', () => ({
	goto: vi.fn()
}));

const TASKS: Task[] = [
	{
		id: 't1',
		title: 'Revisar estoque de filamentos',
		module: 'estoque',
		dueLabel: 'Amanhã',
		urgent: true
	},
	{ id: 't2', title: 'Preparar corte a laser', module: 'producao', dueLabel: 'Em 3 dias' }
];

beforeEach(() => {
	completeTaskMock.mockReset();
	completeTaskMock.mockResolvedValue(undefined);
});

afterEach(() => {
	cleanup();
	completeTaskMock.mockReset();
});

describe('MyTasks (toggle otimista + PATCH)', () => {
	it('marcar a tarefa remove otimista, chama completeTask(id) e dispara onRefetch', async () => {
		const onRefetch = vi.fn();
		render(MyTasks, { props: { tasks: TASKS, onRefetch } });

		await fireEvent.click(
			screen.getByLabelText('Concluir tarefa: Revisar estoque de filamentos')
		);

		expect(completeTaskMock).toHaveBeenCalledTimes(1);
		expect(completeTaskMock).toHaveBeenCalledWith('t1');
		expect(screen.queryByText('Revisar estoque de filamentos')).toBeNull();
		expect(screen.getByText('1 pendentes')).toBeTruthy();

		await waitFor(() => expect(onRefetch).toHaveBeenCalledTimes(1));
	});

	it('cada tarefa possui aria-label "Concluir tarefa: ..."', () => {
		render(MyTasks, { props: { tasks: TASKS } });

		expect(screen.getAllByLabelText(/^Concluir tarefa: /)).toHaveLength(2);
		expect(screen.getByLabelText('Concluir tarefa: Preparar corte a laser')).toBeTruthy();
	});

	it('falha no completeTask restaura o item à lista e ainda dispara onRefetch', async () => {
		completeTaskMock.mockRejectedValueOnce(new Error('boom'));
		const onRefetch = vi.fn();
		render(MyTasks, { props: { tasks: TASKS, onRefetch } });

		await fireEvent.click(
			screen.getByLabelText('Concluir tarefa: Revisar estoque de filamentos')
		);

		await waitFor(() =>
			expect(screen.getByText('Revisar estoque de filamentos')).toBeTruthy()
		);
		expect(screen.getByText('2 pendentes')).toBeTruthy();
		expect(screen.getByLabelText('Concluir tarefa: Revisar estoque de filamentos')).toBeTruthy();

		await waitFor(() => expect(onRefetch).toHaveBeenCalledTimes(1));
	});

	it('enquanto completeTask está em voo a tarefa some e não permite novo toggle', async () => {
		let resolveToggle: (() => void) | undefined;
		completeTaskMock.mockImplementationOnce(
			() => new Promise<void>((resolve) => (resolveToggle = resolve))
		);
		const onRefetch = vi.fn();
		render(MyTasks, { props: { tasks: TASKS, onRefetch } });

		await fireEvent.click(
			screen.getByLabelText('Concluir tarefa: Revisar estoque de filamentos')
		);

		expect(completeTaskMock).toHaveBeenCalledTimes(1);
		expect(screen.queryByText('Revisar estoque de filamentos')).toBeNull();
		expect(screen.queryAllByLabelText(/^Concluir tarefa: /)).toHaveLength(1);

		resolveToggle?.();
		await waitFor(() => expect(onRefetch).toHaveBeenCalledTimes(1));
	});

	it('empty exibe onboarding com CTA "Explorar módulos"', () => {
		render(MyTasks, { props: { tasks: [] } });

		expect(screen.getByText('Sem tarefas por aqui')).toBeTruthy();
		expect(screen.getByRole('button', { name: 'Explorar módulos' })).toBeTruthy();
	});

	it('error exibe mensagem e "Tentar novamente" aciona onRefetch', async () => {
		const onRefetch = vi.fn();
		render(MyTasks, { props: { tasks: [], error: true, onRefetch } });

		expect(screen.getByText('Não foi possível carregar suas tarefas.')).toBeTruthy();
		await fireEvent.click(screen.getByText('Tentar novamente'));
		expect(onRefetch).toHaveBeenCalledTimes(1);
	});
});