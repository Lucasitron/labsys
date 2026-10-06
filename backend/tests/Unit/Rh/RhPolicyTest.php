<?php

namespace Tests\Unit\Rh;

use App\Modules\Auth\Enums\Role;
use App\Modules\Rh\Enums\NivelAcesso;
use App\Modules\Rh\Models\Funcionario;
use App\Modules\Rh\Policies\RhPolicy;
use App\Modules\Rh\RhPrincipal;
use App\Modules\Rh\Rules\CpfRule;
use App\Shared\Exceptions\ForbiddenException;
use Tests\TestCase;

class RhPolicyTest extends TestCase
{
    public function test_eh_admin_ou_proprio(): void
    {
        $admin = $this->criarAdminRh();
        $a = $this->criarFuncionario(null, Role::BOLSISTA);
        $b = $this->criarFuncionario(null, Role::BOLSISTA);

        $pAdmin = RhPrincipal::from($admin['login']);
        $pA = RhPrincipal::from($a['login']);

        $this->assertTrue(RhPolicy::ehAdminOuProprio($pAdmin, $b['funcionario']));
        $this->assertTrue(RhPolicy::ehAdminOuProprio($pA, $a['funcionario']));
        $this->assertFalse(RhPolicy::ehAdminOuProprio($pA, $b['funcionario']));
    }

    public function test_exige_vinculo_ou_admin(): void
    {
        $adminSemVinculo = $this->criarAdmin();
        $this->assertSame(0, RhPolicy::exigeVinculoOuAdmin(RhPrincipal::from($adminSemVinculo)));

        $solto = $this->criarLogin();
        $this->comPermissao($solto, Role::BOLSISTA);

        $this->expectException(ForbiddenException::class);
        RhPolicy::exigeVinculoOuAdmin(RhPrincipal::from($solto));
    }

    public function test_pode_ver_todos_e_tutor(): void
    {
        $bolsista = $this->criarFuncionario(null, Role::BOLSISTA);
        $estagiario = $this->criarFuncionario(null, Role::ESTAGIARIO);

        $this->assertTrue(RhPolicy::podeVerTodos(RhPrincipal::from($bolsista['login'])));
        $this->assertFalse(RhPolicy::podeVerTodos(RhPrincipal::from($estagiario['login'])));
        $this->assertFalse(RhPolicy::ehTutor(RhPrincipal::from($bolsista['login'])));

        $this->criarTutor($bolsista['funcionario']);
        $this->assertTrue(RhPolicy::ehTutor(RhPrincipal::from($bolsista['login'])));
    }

    public function test_principal_sem_permissao_usa_nivel_do_funcionario(): void
    {
        $login = $this->criarLogin();
        $pessoa = $this->criarPessoa(['id' => $login->id_user]);
        $func = Funcionario::create([
            'id_pessoa' => $pessoa->getKey(), 'nivel_acesso' => NivelAcesso::VOLUNTARIO,
        ]);

        $principal = RhPrincipal::from($login);

        $this->assertSame((int) $func->getKey(), $principal->idFuncionario);
        $this->assertSame(NivelAcesso::VOLUNTARIO, $principal->nivel);
        $this->assertFalse($principal->isAdmin());
    }

    public function test_cpf_rule_falha_no_validate(): void
    {
        $falhas = [];
        (new CpfRule)->validate('cpf', '111.111.111-11', function ($msg) use (&$falhas): void {
            $falhas[] = $msg;
        });

        $this->assertCount(1, $falhas);

        (new CpfRule)->validate('cpf', null, function () use (&$falhas): void {
            $falhas[] = 'x';
        });
        $this->assertCount(1, $falhas);
    }
}
