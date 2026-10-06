<?php

namespace App\Modules\Auth\Services;

use App\Modules\Auth\Enums\AccessLogType;
use App\Modules\Auth\Enums\Role;
use App\Modules\Auth\Enums\SituacaoUsuario;
use App\Modules\Auth\Events\RfidAccessEvent;
use App\Modules\Auth\Models\Login;
use App\Modules\Auth\Models\UserPermission;
use App\Shared\Exceptions\ForbiddenException;
use App\Shared\Exceptions\InvalidCredentialsException;
use App\Shared\Exceptions\ResourceNotFoundException;
use App\Shared\Exceptions\TokenBlacklistedException;
use App\Shared\Exceptions\TokenInvalidException;
use Illuminate\Support\Collection;
use Illuminate\Support\Facades\Hash;

/**
 * Fluxos de autenticação (F1–F8): login/logout/refresh/me/RFID/permissions/senha.
 * Regra de negócio só aqui — controllers apenas delegam.
 */
class AuthService
{
    public function __construct(
        private TokenService $tokens,
        private TokenBlacklistService $blacklist,
        private AccessLogService $accessLog,
        private RbacService $rbac,
    ) {}

    /** @return array{accessToken:string,refreshToken:string,tokenType:string,expiresIn:int,idUser:int,role:string,setor:?string,nomeUsuario:string} */
    public function login(?string $email, ?string $nomeUsuario, string $senha): array
    {
        if (blank($email) && blank($nomeUsuario)) {
            throw new InvalidCredentialsException('Informe e-mail ou nome de usuário');
        }

        $login = blank($email)
            ? Login::where('nome_usuario', $nomeUsuario)->first()
            : Login::where('email', $email)->first();

        if ($login === null || ! Hash::check($senha, $login->senha_hash)) {
            throw new InvalidCredentialsException;
        }

        if ($login->situacao !== SituacaoUsuario::ATIVO) {
            throw new ForbiddenException('Conta sem acesso: situação '.$login->situacao->value);
        }

        $role = $this->activeRole($login);

        return [
            'accessToken' => $this->tokens->generateAccessToken($login, $role),
            'refreshToken' => $this->tokens->generateRefreshToken($login, $role),
            'tokenType' => 'Bearer',
            'expiresIn' => TokenService::ACCESS_TTL_SECONDS,
            'idUser' => $login->id_user,
            'role' => $role->name,
            'setor' => $login->setor,
            'nomeUsuario' => $login->nome_usuario,
        ];
    }

    public function logout(?string $token): void
    {
        if (blank($token)) {
            throw new TokenInvalidException;
        }

        $claims = $this->tokens->parse($token);
        $this->blacklist->blacklist($token, $this->tokens->expiry($claims));
    }

    /** @return array{accessToken:string,refreshToken:string,tokenType:string,expiresIn:int} */
    public function refresh(string $refreshToken): array
    {
        $claims = $this->tokens->parse($refreshToken);

        if (! $this->tokens->isRefreshToken($claims)) {
            throw new TokenInvalidException;
        }

        if ($this->blacklist->isBlacklisted($refreshToken)) {
            throw new TokenBlacklistedException;
        }

        $login = Login::find($claims['sub'] ?? null);

        if ($login === null) {
            throw new ResourceNotFoundException('Usuário não encontrado');
        }

        $role = $this->activeRole($login);

        // Rotação: o refresh antigo é invalidado.
        $this->blacklist->blacklist($refreshToken, $this->tokens->expiry($claims));

        return [
            'accessToken' => $this->tokens->generateAccessToken($login, $role),
            'refreshToken' => $this->tokens->generateRefreshToken($login, $role),
            'tokenType' => 'Bearer',
            'expiresIn' => TokenService::ACCESS_TTL_SECONDS,
        ];
    }

    /** @return array{login:Login,permissions:Collection<int,UserPermission>} */
    public function me(Login $login): array
    {
        $fresh = Login::find($login->getKey());

        if ($fresh === null) {
            throw new ResourceNotFoundException('Usuário não encontrado');
        }

        return [
            'login' => $fresh,
            'permissions' => UserPermission::where('id_user', $fresh->id_user)->get(),
        ];
    }

    /** @return array{allowed:bool,message:string,idUser:?int,uuidRfid:string,nomeUsuario:?string,setor:?string,type:string,timestamp:string} */
    public function validateRfid(string $uuid): array
    {
        $login = Login::where('uuid', $uuid)->first();

        if ($login === null) {
            $denied = $this->accessLog->record(null, $uuid, AccessLogType::ACESSO_NEGADO);

            return [
                'allowed' => false,
                'message' => 'Cartão RFID não reconhecido',
                'idUser' => null,
                'uuidRfid' => $uuid,
                'nomeUsuario' => null,
                'setor' => null,
                'type' => $denied->type->value,
                'timestamp' => $denied->timestamp->toIso8601String(),
            ];
        }

        $type = $this->accessLog->resolveType($uuid);
        $entry = $this->accessLog->record($login->id_user, $uuid, $type);

        event(new RfidAccessEvent(
            idUser: $login->id_user,
            uuidRfid: $uuid,
            timestamp: $entry->timestamp,
            type: $type,
        ));

        return [
            'allowed' => true,
            'message' => 'Acesso registrado',
            'idUser' => $login->id_user,
            'uuidRfid' => $uuid,
            'nomeUsuario' => $login->nome_usuario,
            'setor' => $login->setor,
            'type' => $type->value,
            'timestamp' => $entry->timestamp->toIso8601String(),
        ];
    }

    /** @return array{role:string,code:int,label:string,permissions:list<string>} */
    public function permissions(Role $role): array
    {
        return $this->rbac->getMatrix($role);
    }

    /**
     * Altera a senha do próprio usuário; revoga o Bearer atual (força re-login).
     */
    public function alterarSenha(Login $login, string $senhaAtual, string $novaSenha, string $confirmacaoSenha, ?string $tokenAtual = null): void
    {
        $fresh = Login::find($login->getKey());

        if ($fresh === null) {
            throw new ResourceNotFoundException('Usuário não encontrado');
        }

        if (! Hash::check($senhaAtual, $fresh->senha_hash)) {
            throw new InvalidCredentialsException('Senha atual incorreta');
        }

        if ($novaSenha !== $confirmacaoSenha) {
            throw new \InvalidArgumentException('Nova senha e confirmação não conferem');
        }

        $fresh->senha_hash = Hash::make($novaSenha);
        $fresh->save();

        if (! blank($tokenAtual)) {
            try {
                $claims = $this->tokens->parse($tokenAtual);
                $this->blacklist->blacklist($tokenAtual, $this->tokens->expiry($claims));
            } catch (TokenInvalidException) {
                // token já inválido ou expirado: nada a revogar
            }
        }
    }

    private function activeRole(Login $login): Role
    {
        $permission = UserPermission::where('id_user', $login->id_user)
            ->where('active', true)
            ->first();

        if ($permission === null) {
            throw new ForbiddenException('Usuário não possui permissão ativa');
        }

        return $permission->role;
    }
}
