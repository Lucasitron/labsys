<?php

namespace Tests;

use App\Modules\Auth\Enums\Role;
use App\Modules\Auth\Enums\SituacaoUsuario;
use App\Modules\Auth\Models\Login;
use App\Modules\Auth\Models\UserPermission;
use App\Modules\Auth\Services\TokenService;
use App\Modules\Rh\Enums\NivelAcesso;
use App\Modules\Rh\Enums\PessoaStatus;
use App\Modules\Rh\Models\Funcionario;
use App\Modules\Rh\Models\Pessoa;
use App\Modules\Rh\Models\Tutor;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Foundation\Testing\TestCase as BaseTestCase;
use Illuminate\Support\Facades\Hash;

/** Base dos testes M1: banco limpo (migrate:fresh) + fábricas mínimas do Auth. */
abstract class TestCase extends BaseTestCase
{
    use RefreshDatabase;

    protected int $seq = 0;

    protected function criarLogin(array $over = []): Login
    {
        $this->seq++;

        return Login::create(array_merge([
            'id_user' => 1000 + $this->seq,
            'uuid' => 'rfid-'.$this->seq,
            'email' => "user{$this->seq}@fablab.org",
            'nome_usuario' => 'user'.$this->seq,
            'senha_hash' => Hash::make('senha123'),
            'setor' => 'PRODUCAO',
            'situacao' => SituacaoUsuario::ATIVO,
        ], $over));
    }

    protected function comPermissao(Login $login, Role $role = Role::ADMIN, bool $active = true): UserPermission
    {
        return UserPermission::create([
            'id_user' => $login->id_user,
            'role' => $role,
            'active' => $active,
        ]);
    }

    protected function criarAdmin(array $over = []): Login
    {
        $login = $this->criarLogin($over);
        $this->comPermissao($login, Role::ADMIN);

        return $login;
    }

    protected function tokenPara(Login $login, Role $role = Role::ADMIN, string $type = TokenService::TYPE_ACCESS): string
    {
        /** @var TokenService $tokens */
        $tokens = app(TokenService::class);

        $token = $type === TokenService::TYPE_REFRESH
            ? $tokens->generateRefreshToken($login, $role)
            : $tokens->generateAccessToken($login, $role);

        // generate() autentica o login no guard compartilhado (efeito colateral
        // de `login($login)`, que cacheia o usuário no processo de teste).
        // Descarta os guards para cada request resolver o usuário pelo Bearer.
        auth()->forgetGuards();

        return $token;
    }

    /** @return array<string, string> */
    protected function authHeader(Login $login, Role $role = Role::ADMIN): array
    {
        return ['Authorization' => 'Bearer '.$this->tokenPara($login, $role)];
    }

    /**
     * O guard JWT e o manager `tymon.jwt` cacheiam usuário/token no processo de
     * teste; sem isso, um request com o Bearer de B resolveria o usuário/token
     * de A (autenticado antes). Produção não é afetada (app novo por request).
     */
    public function call($method, $uri, $parameters = [], $cookies = [], $files = [], $server = [], $content = null)
    {
        auth()->forgetGuards();
        app('tymon.jwt')->unsetToken();

        return parent::call($method, $uri, $parameters, $cookies, $files, $server, $content);
    }

    // -- Fábrica RH (M2): pessoa.id = login.id_user (vínculo do RhPrincipal). --

    protected function criarPessoa(array $over = []): Pessoa
    {
        $this->seq++;

        return Pessoa::create(array_merge([
            'nome_completo' => "Pessoa {$this->seq}",
            'matricula' => 'MAT-'.$this->seq,
            'contato' => "pessoa{$this->seq}@fablab.org",
            'turno' => 'MANHA',
            'status' => PessoaStatus::ATIVO,
        ], $over));
    }

    /**
     * @return array{login:Login,pessoa:Pessoa,funcionario:Funcionario}
     */
    protected function criarFuncionario(
        ?Login $login = null,
        Role $role = Role::BOLSISTA,
        array $overFunc = [],
        array $overPessoa = [],
    ): array {
        $login ??= $this->criarLogin();
        $this->comPermissao($login, $role);

        $pessoa = $this->criarPessoa(array_merge(['id' => $login->id_user], $overPessoa));

        $funcionario = Funcionario::create(array_merge([
            'id_pessoa' => $pessoa->getKey(),
            'nivel_acesso' => NivelAcesso::from($role->value),
            'departamento' => 'ELETRONICA',
        ], $overFunc));

        return ['login' => $login, 'pessoa' => $pessoa, 'funcionario' => $funcionario];
    }

    /** @return array{login:Login,pessoa:Pessoa,funcionario:Funcionario} */
    protected function criarAdminRh(array $overFunc = []): array
    {
        return $this->criarFuncionario(null, Role::ADMIN, $overFunc);
    }

    protected function criarTutor(Funcionario $funcionario, array $over = []): Tutor
    {
        return Tutor::create(array_merge([
            'id_funcionario' => $funcionario->getKey(),
            'turno' => 'MANHA',
            'qualificacao' => 3,
        ], $over));
    }
}
