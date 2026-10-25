<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

// V9: uma credencial por pessoa — `login.id_user` referencia `rh.pessoa.id` (1:1).
// Requer backfill/dedup prévio se houver contas duplicadas legadas (ver §2.5 da varredura).
return new class extends Migration
{
    public function up(): void
    {
        Schema::table('auth.login', function (Blueprint $table): void {
            $table->unique('id_user', 'uk_login_id_user');
        });
    }

    public function down(): void
    {
        Schema::table('auth.login', function (Blueprint $table): void {
            $table->dropUnique('uk_login_id_user');
        });
    }
};
