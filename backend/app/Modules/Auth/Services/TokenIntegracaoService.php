<?php

namespace App\Modules\Auth\Services;

use App\Modules\Auth\Models\TokenIntegracao;
use App\Shared\Exceptions\ConfiguracaoInvalidaException;
use App\Shared\Exceptions\ResourceNotFoundException;
use Illuminate\Support\Str;

/**
 * Tokens de integração: lista sem segredo, criação exibe a chave UMA vez
 * (persiste só hash SHA-256 + prefixo), revogação é soft (404 se inexistente).
 */
class TokenIntegracaoService
{
    /** @return list<array{id:int,nome:string,prefixo:string,criadoEm:?string,ultimoUso:?string,revogado:bool}> */
    public function listarAtivos(): array
    {
        return TokenIntegracao::where('revogado', false)->orderByDesc('criado_em')->get()
            ->map(fn (TokenIntegracao $t) => $this->resumo($t))->all();
    }

    /** @return array{id:int,nome:string,prefixo:string,criadoEm:?string,chave:string} */
    public function criar(string $nomeRaw): array
    {
        $nome = trim($nomeRaw);

        if ($nome === '') {
            throw new ConfiguracaoInvalidaException('Nome da integração é obrigatório');
        }
        if (mb_strlen($nome) > 128) {
            throw new ConfiguracaoInvalidaException('Nome da integração deve ter no máximo 128 caracteres');
        }
        if (TokenIntegracao::where('nome', $nome)->where('revogado', false)->exists()) {
            throw new ConfiguracaoInvalidaException('Já existe um token ativo com este nome');
        }

        $chave = 'fablab_'.Str::random(40);
        $prefixo = substr($chave, 0, 12);

        $token = TokenIntegracao::create([
            'nome' => $nome,
            'prefixo' => $prefixo,
            'hash' => hash('sha256', $chave),
            'criado_em' => now(),
            'revogado' => false,
        ]);

        return [
            'id' => $token->id,
            'nome' => $token->nome,
            'prefixo' => $token->prefixo,
            'criadoEm' => $token->criado_em->toIso8601String(),
            'chave' => $chave,
        ];
    }

    public function revogar(int $id): void
    {
        $token = TokenIntegracao::find($id);

        if ($token === null || $token->revogado) {
            throw new ResourceNotFoundException('Token não encontrado');
        }

        $token->revogado = true;
        $token->save();
    }

    /** @return array{id:int,nome:string,prefixo:string,criadoEm:?string,ultimoUso:?string,revogado:bool} */
    public function resumo(TokenIntegracao $token): array
    {
        return [
            'id' => $token->id,
            'nome' => $token->nome,
            'prefixo' => $token->prefixo,
            'criadoEm' => $token->criado_em?->toIso8601String(),
            'ultimoUso' => $token->ultimo_uso?->toIso8601String(),
            'revogado' => $token->revogado,
        ];
    }
}
