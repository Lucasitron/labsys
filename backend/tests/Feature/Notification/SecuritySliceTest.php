<?php

namespace Tests\Feature\Notification;

use App\Modules\Auth\Enums\Role;

/** Slice security: 401 sem token em tudo; B/V/E → 403 no admin; Recrutando inbox 200. */
class SecuritySliceTest extends NotificationTestCase
{
    /** @return list<array{0:string,1:string}> */
    private static function rotas(): array
    {
        return [
            ['get', '/api/notification/notificacoes'],
            ['get', '/api/notification/notificacoes/nao-lidas/contagem'],
            ['patch', '/api/notification/notificacoes/1/ler'],
            ['post', '/api/notification/notificacoes/ler-todas'],
            ['post', '/api/notification/notificacoes/revisar'],
            ['get', '/api/notification/historico'],
            ['get', '/api/notification/preferencias'],
            ['put', '/api/notification/preferencias'],
            ['get', '/api/notification/configuracao-canais'],
            ['put', '/api/notification/configuracao-canais/EMAIL'],
            ['get', '/api/notificacoes'],
            ['get', '/api/notificacoes/nao-lidas/contagem'],
        ];
    }

    public function test_sem_token_401_em_tudo(): void
    {
        foreach (self::rotas() as [$verbo, $uri]) {
            $corpo = in_array($verbo, ['put', 'post', 'patch'], true) ? ['habilitado' => true, 'ids' => [1]] : [];

            $resposta = match ($verbo) {
                'get' => $this->getJson($uri),
                'post' => $this->postJson($uri, $corpo),
                'put' => $this->putJson($uri, $corpo),
                'patch' => $this->patchJson($uri, $corpo),
            };

            $resposta->assertUnauthorized($uri);
        }
    }

    public function test_nao_admin_403_no_admin_e_200_no_inbox(): void
    {
        foreach ([Role::BOLSISTA, Role::VOLUNTARIO, Role::ESTAGIARIO] as $role) {
            $headers = $this->headersPapel($role);

            $this->getJson('/api/notification/notificacoes', $headers)->assertOk();
            $this->getJson('/api/notification/notificacoes/nao-lidas/contagem', $headers)->assertOk();
            $this->postJson('/api/notification/notificacoes/ler-todas', [], $headers)->assertOk();

            $this->getJson('/api/notification/preferencias', $headers)->assertForbidden();
            $this->putJson('/api/notification/preferencias', [], $headers)->assertForbidden();
            $this->getJson('/api/notification/historico', $headers)->assertForbidden();
            $this->postJson('/api/notification/notificacoes/revisar', ['ids' => [1]], $headers)->assertForbidden();
            $this->getJson('/api/notification/configuracao-canais', $headers)->assertForbidden();
            $this->putJson('/api/notification/configuracao-canais/EMAIL', ['habilitado' => true], $headers)->assertForbidden();
        }
    }

    public function test_recrutando_inbox_propria_200_admin_403(): void
    {
        $headers = $this->headersPapel(Role::RECRUTANDO);

        $this->getJson('/api/notification/notificacoes', $headers)->assertOk();
        $this->getJson('/api/notificacoes', $headers)->assertOk();
        $this->getJson('/api/notification/preferencias', $headers)->assertForbidden();
        $this->getJson('/api/notification/historico', $headers)->assertForbidden();
    }
}
