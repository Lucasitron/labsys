<?php

use App\Modules\Auth\Services\LoginIdUserDedupService;
use Illuminate\Database\Migrations\Migration;

// V9a — backfill/dedup ANTES da UNIQUE de `auth.login.id_user` (V9).
// Arquiva duplicados em `auth.login_dedup_backup` (reversível) e os remove,
// preservando `user_permissions` (papel é da pessoa, por `id_user`).
// Ordem do arquivo (< v9) garante execução prévia; rollback em lote reverte a
// UNIQUE (V9) antes de restaurar (V9a).
return new class extends Migration
{
    public function up(): void
    {
        app(LoginIdUserDedupService::class)->executar();
    }

    public function down(): void
    {
        app(LoginIdUserDedupService::class)->reverter();
    }
};
