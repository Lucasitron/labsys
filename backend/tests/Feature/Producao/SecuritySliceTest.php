<?php

namespace Tests\Feature\Producao;

use App\Modules\Auth\Enums\Role;

/** Slice 401/403 do Producao (tudo autenticado; Recrutando fora; Admin-only e vínculos). */
class SecuritySliceTest extends ProducaoTestCase
{
    /** @return list<string> */
    private static function rotasGet(): array
    {
        return [
            '/api/producao/projetos',
            '/api/producao/projetos/1',
            '/api/producao/tarefas',
            '/api/producao/tarefas/1',
            '/api/producao/kanban',
            '/api/producao/kanban/encomenda/1',
            '/api/producao/kanban/encomenda/1/historico',
            '/api/producao/kanban/encomenda/1/consumo',
            '/api/producao/maquinas',
            '/api/producao/maquinas/1',
            '/api/producao/maquinas/1/historico',
            '/api/producao/setores',
            '/api/producao/setores/1',
            '/api/producao/setores/1/responsaveis',
            '/api/producao/inspecoes-5s',
            '/api/producao/inspecoes-5s/1',
            '/api/producao/advertencias',
            '/api/producao/advertencias/1',
            '/api/producao/projetos-mesa',
            '/api/producao/projetos-mesa/1',
            '/api/producao/projetos-mesa/1/qrcode',
            '/api/producao/projetos-mesa/1/auditorias',
            '/api/producao/auditorias-projeto-mesa/1',
            '/api/producao/parametros-5s',
            '/api/producao/parametros-5s/1',
        ];
    }

    /** @return list<array{0:string,1:string}> */
    private static function rotasEscrita(): array
    {
        return [
            ['POST', '/api/producao/projetos'],
            ['PUT', '/api/producao/projetos/1'],
            ['PUT', '/api/producao/projetos/1/status'],
            ['DELETE', '/api/producao/projetos/1'],
            ['POST', '/api/producao/tarefas'],
            ['PUT', '/api/producao/tarefas/1'],
            ['PUT', '/api/producao/tarefas/1/status'],
            ['PATCH', '/api/producao/tarefas/1'],
            ['DELETE', '/api/producao/tarefas/1'],
            ['POST', '/api/producao/kanban'],
            ['PUT', '/api/producao/kanban/1/mover'],
            ['DELETE', '/api/producao/kanban/1'],
            ['POST', '/api/producao/kanban/encomenda/1/consumo'],
            ['POST', '/api/producao/maquinas'],
            ['PUT', '/api/producao/maquinas/1'],
            ['PUT', '/api/producao/maquinas/1/status'],
            ['POST', '/api/producao/maquinas/1/uso'],
            ['PUT', '/api/producao/maquinas/1/uso/1/fim'],
            ['DELETE', '/api/producao/maquinas/1'],
            ['POST', '/api/producao/setores'],
            ['PUT', '/api/producao/setores/1'],
            ['DELETE', '/api/producao/setores/1'],
            ['POST', '/api/producao/setores/1/materiais'],
            ['DELETE', '/api/producao/setores/1/materiais/1'],
            ['POST', '/api/producao/setores/1/sinalizacoes'],
            ['DELETE', '/api/producao/setores/1/sinalizacoes/1'],
            ['POST', '/api/producao/setores/1/checklist'],
            ['PUT', '/api/producao/setores/1/checklist/1'],
            ['DELETE', '/api/producao/setores/1/checklist/1'],
            ['POST', '/api/producao/setores/1/responsaveis'],
            ['DELETE', '/api/producao/setores/1/responsaveis/1'],
            ['POST', '/api/producao/inspecoes-5s'],
            ['DELETE', '/api/producao/inspecoes-5s/1'],
            ['POST', '/api/producao/advertencias'],
            ['POST', '/api/producao/projetos-mesa'],
            ['PUT', '/api/producao/projetos-mesa/1'],
            ['PUT', '/api/producao/projetos-mesa/1/evolucao'],
            ['DELETE', '/api/producao/projetos-mesa/1'],
            ['POST', '/api/producao/auditorias-projeto-mesa'],
            ['PUT', '/api/producao/parametros-5s/1'],
        ];
    }

    public function test_sem_token_da_401(): void
    {
        foreach (self::rotasGet() as $uri) {
            $this->getJson($uri)->assertUnauthorized($uri);
        }

        foreach (self::rotasEscrita() as [$verbo, $uri]) {
            $res = match ($verbo) {
                'POST' => $this->postJson($uri),
                'PUT' => $this->putJson($uri),
                'PATCH' => $this->patchJson($uri),
                default => $this->deleteJson($uri),
            };
            $res->assertUnauthorized($uri);
        }
    }

    public function test_recrutando_da_403_em_tudo(): void
    {
        $headers = $this->headersPapel(Role::RECRUTANDO);

        foreach (self::rotasGet() as $uri) {
            $this->getJson($uri, $headers)->assertForbidden($uri);
        }

        foreach (self::rotasEscrita() as [$verbo, $uri]) {
            $res = match ($verbo) {
                'POST' => $this->postJson($uri, [], $headers),
                'PUT' => $this->putJson($uri, [], $headers),
                'PATCH' => $this->patchJson($uri, [], $headers),
                default => $this->deleteJson($uri, [], $headers),
            };
            $res->assertForbidden($uri);
        }
    }

    public function test_admin_only_e_estagiario_sem_escrita(): void
    {
        $bolsista = $this->headersPapel(Role::BOLSISTA);
        $estagiario = $this->headersPapel(Role::ESTAGIARIO);

        // Admin-only via can:admin.
        $this->postJson('/api/producao/maquinas', ['nome' => 'X'], $bolsista)->assertForbidden();
        $this->postJson('/api/producao/advertencias', [], $bolsista)->assertForbidden();
        $this->postJson('/api/producao/auditorias-projeto-mesa', [], $bolsista)->assertForbidden();
        $this->putJson('/api/producao/parametros-5s/1', ['valor' => 'x'], $bolsista)->assertForbidden();
        $this->postJson('/api/producao/setores/1/responsaveis', [], $bolsista)->assertForbidden();

        // Estagiário não escreve (sem vínculo possível).
        $this->postJson('/api/producao/projetos',
            ['nome' => 'X', 'dataInicio' => today()->toDateString(), 'idResponsavel' => 1], $estagiario)
            ->assertForbidden();
        $this->postJson('/api/producao/kanban', ['idEncomenda' => 1], $estagiario)->assertForbidden();
    }
}
