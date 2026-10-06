<?php

namespace Tests\Feature\Auth;

use App\Modules\Auth\Models\Login;
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
}
