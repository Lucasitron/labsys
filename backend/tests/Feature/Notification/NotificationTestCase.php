<?php

namespace Tests\Feature\Notification;

use App\Modules\Auth\Enums\Role;
use App\Modules\Auth\Models\Login;
use Tests\TestCase;

/** Base dos testes do Notification: papéis + fábricas de destinatários. */
abstract class NotificationTestCase extends TestCase
{
    /** @return array<string, string> */
    protected function headersPapel(Role $role): array
    {
        $login = $this->criarLogin();
        $this->comPermissao($login, $role);

        return $this->authHeader($login, $role);
    }

    /** @return array<string, string> */
    protected function headersAdmin(): array
    {
        return $this->authHeader($this->criarAdmin(), Role::ADMIN);
    }

    /**
     * Funcionário com pessoa vinculada ao login (`contato` = e-mail por padrão).
     *
     * @return array{login:Login,pessoa:\App\Modules\Rh\Models\Pessoa,funcionario:\App\Modules\Rh\Models\Funcionario}
     */
    protected function funcionarioComEmail(Role $role = Role::BOLSISTA, ?string $contato = null): array
    {
        $this->seq++;
        $email = "dest{$this->seq}@fablab.org";

        return $this->criarFuncionario(null, $role, [], [
            'contato' => $contato ?? $email,
        ]);
    }

    /** @return array<string, string> */
    protected function headersFuncionario(array $func, Role $role = Role::BOLSISTA): array
    {
        return $this->authHeader($func['login'], $role);
    }
}
