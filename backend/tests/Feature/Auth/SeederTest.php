<?php

namespace Tests\Feature\Auth;

use App\Modules\Auth\Models\Login;
use App\Modules\Rh\Enums\NivelAcesso;
use App\Modules\Rh\Enums\PessoaStatus;
use App\Modules\Rh\Models\Pessoa;
use App\Modules\Rh\Services\PessoaService;
use Database\Seeders\AuthSeeder;
use Illuminate\Support\Facades\Hash;
use Tests\TestCase;

class SeederTest extends TestCase
{
    public function test_sem_flag_nao_semeia(): void
    {
        config()->set('auth.seed.enabled', false);

        (new AuthSeeder)->run();

        $this->assertSame(0, Login::count());
        $this->assertSame(0, Pessoa::count());
    }

    public function test_com_flag_cria_admin_e_login_funciona(): void
    {
        config()->set('auth.seed.enabled', true);

        (new AuthSeeder)->run();

        $admin = Login::where('email', 'admin@fablab.org')->first();
        $this->assertNotNull($admin);
        $this->assertTrue(Hash::check('admin123', $admin->senha_hash));

        $this->postJson('/api/auth/login', [
            'email' => 'admin@fablab.org',
            'senha' => 'admin123',
        ])->assertOk()->assertJson(['role' => 'ADMIN']);
    }

    public function test_roda_2x_sem_duplicar(): void
    {
        config()->set('auth.seed.enabled', true);

        (new AuthSeeder)->run();
        (new AuthSeeder)->run();

        $this->assertSame(1, Login::count());
    }

    /**
     * C-01: o seed cria a `rh.pessoa` do admin (id == id_user) e a primeira
     * pessoa real com acesso NÃO colide com o login do admin (id_user=1).
     */
    public function test_seed_cria_pessoa_do_admin_e_proxima_pessoa_nao_colide(): void
    {
        config()->set('auth.seed.enabled', true);

        (new AuthSeeder)->run();

        $admin = Login::where('email', 'admin@fablab.org')->firstOrFail();

        // Invariante: existe pessoa para o admin com id == id_user.
        $this->assertNotNull(
            Pessoa::find($admin->id_user),
            'seed deve criar a rh.pessoa do admin (pessoa.id == login.id_user)',
        );

        // A primeira pessoa real com acesso deve ser provisionada sem colisão.
        $nova = app(PessoaService::class)->cadastrar(
            ['nome_completo' => 'Primeira Real', 'matricula' => 'MAT-1', 'status' => PessoaStatus::ATIVO],
            ['nivel_acesso' => NivelAcesso::BOLSISTA],
            ['criar' => true, 'email' => 'primeira@fablab.org', 'senha' => 'senha1234'],
        );

        $this->assertNotSame($admin->id_user, (int) $nova->getKey());
        $this->assertTrue(Login::where('id_user', (int) $nova->getKey())->exists());
    }

    /**
     * O admin precisa de vínculo de funcionário: `historico_nivel.id_admin_alterou`
     * é NOT NULL FK→funcionario e o autor da alteração de nível exige vínculo.
     */
    public function test_seed_cria_funcionario_admin(): void
    {
        config()->set('auth.seed.enabled', true);

        (new AuthSeeder)->run();

        $admin = Login::where('email', 'admin@fablab.org')->firstOrFail();

        $this->assertDatabaseHas('rh.funcionario', [
            'id_pessoa' => $admin->id_user,
            'nivel_acesso' => NivelAcesso::ADMIN->value,
            'departamento' => 'Administrativo',
        ]);
    }
}
