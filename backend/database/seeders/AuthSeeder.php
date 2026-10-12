<?php

namespace Database\Seeders;

use App\Modules\Auth\Enums\Role;
use App\Modules\Auth\Enums\SituacaoUsuario;
use App\Modules\Auth\Models\Login;
use App\Modules\Auth\Models\UserPermission;
use App\Modules\Rh\Enums\PessoaStatus;
use App\Modules\Rh\Models\Pessoa;
use Illuminate\Database\Seeder;
use Illuminate\Support\Facades\Hash;

/**
 * Seed do login admin raiz (equivale ao AuthSeedRunner: dev-only, idempotente).
 * Só executa com AUTH_SEED_ENABLED=true e login vazia. Nunca loga credenciais.
 *
 * C-01: cria também a `rh.pessoa` do admin com `id == login.id_user`, honrando a
 * invariante `pessoa.id == id_user`. Sem isso, a primeira pessoa cadastrada (id=1)
 * colidiria com o login do admin (id_user=1) no provisionamento de acesso.
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

        // Pessoa primeiro: seu `id` (auto) é a fonte do `id_user` do login.
        $pessoa = Pessoa::create([
            'nome_completo' => (string) env('AUTH_SEED_NOME', 'Administrador FabLab'),
            'matricula' => (string) env('AUTH_SEED_MATRICULA', 'ADMIN-ROOT'),
            'contato' => (string) env('AUTH_SEED_EMAIL', 'admin@fablab.org'),
            'status' => PessoaStatus::ATIVO,
        ]);

        $login = Login::create([
            'id_user' => (int) $pessoa->getKey(),
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
