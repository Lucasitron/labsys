<?php

namespace Tests\Unit\Producao;

use App\Modules\Auth\Enums\Role;
use App\Modules\Producao\Enums\KanbanStatus;
use App\Modules\Producao\Policies\ProducaoPolicy;
use App\Modules\Producao\ProducaoPrincipal;
use App\Shared\Exceptions\ForbiddenException;
use PHPUnit\Framework\TestCase;

/** Policy + vínculo + rótulos (sem banco). */
class ProducaoPolicyTest extends TestCase
{
    private function principal(Role $role, int $id = 7): ProducaoPrincipal
    {
        return new ProducaoPrincipal($id, $role, $role === Role::ADMIN);
    }

    public function test_leitura_nega_recrutando(): void
    {
        $this->expectException(ForbiddenException::class);
        ProducaoPolicy::exigeLeitura($this->principal(Role::RECRUTANDO));
    }

    public function test_leitura_permite_demais(): void
    {
        foreach ([Role::ADMIN, Role::BOLSISTA, Role::VOLUNTARIO, Role::ESTAGIARIO] as $role) {
            ProducaoPolicy::exigeLeitura($this->principal($role));
        }
        $this->assertTrue(true);
    }

    public function test_responsavel_admin_sempre_e_proprio(): void
    {
        $admin = $this->principal(Role::ADMIN, 1);

        $this->assertTrue(ProducaoPolicy::isResponsavelAtribuido(999, $admin));
        $this->assertTrue(ProducaoPolicy::isResponsavelAtribuido(null, $admin));
        $this->assertTrue(ProducaoPolicy::isResponsavelAtribuido(7, $this->principal(Role::BOLSISTA, 7)));
        $this->assertFalse(ProducaoPolicy::isResponsavelAtribuido(8, $this->principal(Role::BOLSISTA, 7)));
        $this->assertFalse(ProducaoPolicy::isResponsavelAtribuido(null, $this->principal(Role::BOLSISTA, 7)));
    }

    public function test_incluir_kanban_so_admin_bolsista(): void
    {
        ProducaoPolicy::exigeIncluirKanban($this->principal(Role::ADMIN));
        ProducaoPolicy::exigeIncluirKanban($this->principal(Role::BOLSISTA));

        foreach ([Role::VOLUNTARIO, Role::ESTAGIARIO] as $role) {
            try {
                ProducaoPolicy::exigeIncluirKanban($this->principal($role));
                $this->fail("{$role->name} não deveria incluir no Kanban");
            } catch (ForbiddenException) {
            }
        }
        $this->assertTrue(true);
    }

    public function test_rotulo_vendas_do_kanban(): void
    {
        $this->assertSame('Fila', KanbanStatus::FILA->rotuloVendas());
        $this->assertSame('Produção', KanbanStatus::PRODUCAO->rotuloVendas());
        $this->assertSame('Acabamento', KanbanStatus::ACABAMENTO->rotuloVendas());
        $this->assertSame('Pronto', KanbanStatus::PRONTO->rotuloVendas());
        $this->assertSame('Entregue', KanbanStatus::ENTREGUE->rotuloVendas());
    }
}
