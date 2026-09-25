import { ApiError, NetworkError } from '$lib/api/client';

export interface UserMessage {
	message: string;
	hint: string;
}

const INVALID_CREDENTIALS: UserMessage = {
	message: 'Usuário ou senha inválidos',
	hint: 'Verifique suas credenciais e tente novamente.'
};

const ACCOUNT_LOCKED: UserMessage = {
	message: 'Conta bloqueada. Contate o administrador.',
	hint: 'Entre em contato com um administrador para desbloquear o acesso.'
};

const SERVER_ERROR: UserMessage = {
	message: 'Erro ao processar login. Tente novamente.',
	hint: 'Se o problema persistir, contate o suporte.'
};

const NETWORK_ERROR: UserMessage = {
	message: 'Falha de conexão. Verifique sua rede.',
	hint: 'Confirme sua conexão e tente novamente.'
};

const UNEXPECTED: UserMessage = {
	message: 'Ocorreu um erro inesperado.',
	hint: 'Tente novamente em instantes.'
};

export function toUserMessage(err: unknown): UserMessage {
	if (err instanceof NetworkError) {
		return NETWORK_ERROR;
	}

	if (err instanceof ApiError) {
		switch (err.code) {
			case 'INVALID_CREDENTIALS':
				return INVALID_CREDENTIALS;
			case 'ACCOUNT_LOCKED':
				return ACCOUNT_LOCKED;
		}

		if (err.status === 401) return INVALID_CREDENTIALS;
		if (err.status === 423) return ACCOUNT_LOCKED;
		if (err.status >= 500) return SERVER_ERROR;
	}

	return UNEXPECTED;
}