import { describe, expect, it } from 'vitest';
import { ApiError, NetworkError } from '$lib/api/client';
import { toUserMessage } from '$lib/utils/errors';

describe('toUserMessage', () => {
	it('mapeia ApiError 401 INVALID_CREDENTIALS para credenciais inválidas', () => {
		const err = new ApiError(401, 'INVALID_CREDENTIALS', 'usuario invalido');
		expect(toUserMessage(err).message).toBe('Usuário ou senha inválidos');
	});

	it('mapeia ApiError 423 ACCOUNT_LOCKED para conta bloqueada', () => {
		const err = new ApiError(423, 'ACCOUNT_LOCKED', 'conta bloqueada');
		expect(toUserMessage(err).message).toBe('Conta bloqueada. Contate o administrador.');
	});

	it('mapeia ApiError 401 sem code para credenciais inválidas', () => {
		const err = new ApiError(401, 'UNKNOWN', 'sem code');
		expect(toUserMessage(err).message).toBe('Usuário ou senha inválidos');
	});

	it('mapeia ApiError 423 sem code para conta bloqueada', () => {
		const err = new ApiError(423, 'UNKNOWN', 'sem code');
		expect(toUserMessage(err).message).toBe('Conta bloqueada. Contate o administrador.');
	});

	it('mapeia ApiError 5xx com qualquer code para erro ao processar login', () => {
		const err = new ApiError(500, 'SERVER_ERROR', 'boom');
		expect(toUserMessage(err).message).toBe('Erro ao processar login. Tente novamente.');

		const err503 = new ApiError(503, 'UNKNOWN', 'indisponivel');
		expect(toUserMessage(err503).message).toBe('Erro ao processar login. Tente novamente.');
	});

	it('mapeia NetworkError para falha de conexão', () => {
		const err = new NetworkError();
		expect(toUserMessage(err).message).toBe('Falha de conexão. Verifique sua rede.');
	});

	it('mapeia erro desconhecido para erro inesperado', () => {
		expect(toUserMessage(new Error('x')).message).toBe('Ocorreu um erro inesperado.');
	});
});