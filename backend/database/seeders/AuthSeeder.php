<?php

namespace Database\Seeders;

use App\Modules\Auth\Enums\Role;
use App\Modules\Auth\Enums\SituacaoUsuario;
use App\Modules\Auth\Models\Login;
use App\Modules\Auth\Models\UserPermission;
use Illuminate\Database\Seeder;
use Illuminate\Support\Facades\Hash;

/**
 * Seed do login admin raiz (equivale ao AuthSeedRunner: dev-only, idempotente).
 * Só executa com AUTH_SEED_ENABLED=true e login vazia. Nunca loga credenciais.
 */
class AuthSeeder extends Seeder
{
    public function run(): void
    {
        if (! config('auth.seed.enabled', false)) {
            return;
        }

        if (Login::query()->exists()) {
            return;
        }

        $login = Login::create([
            'id_user' => (int) env('AUTH_SEED_ID_USER', 1),
            'uuid' => (string) env('AUTH_SEED_UUID', 'admin-root-rfid'),
            'email' => (string) env('AUTH_SEED_EMAIL', 'admin@fablab.org'),
            'nome_usuario' => (string) env('AUTH_SEED_USERNAME', 'admin'),
            'senha_hash' => Hash::make((string) env('AUTH_SEED_PASSWORD', 'admin123')),
            'setor' => (string) env('AUTH_SEED_SETOR', 'ADMINISTRACAO'),
            'situacao' => SituacaoUsuario::ATIVO,
        ]);

        UserPermission::create([
            'id_user' => $login->id_user,
            'role' => Role::ADMIN,
            'active' => true,
        ]);
    }
}
