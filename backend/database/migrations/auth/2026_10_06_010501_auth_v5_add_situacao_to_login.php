<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

// V5__add_situacao_to_login.sql — situação da conta (gestão em Usuários).
return new class extends Migration
{
    public function up(): void
    {
        Schema::table('auth.login', function (Blueprint $table): void {
            $table->string('situacao', 20)->default('ATIVO');
        });

        DB::statement("ALTER TABLE auth.login ADD CONSTRAINT ck_login_situacao CHECK (situacao IN ('ATIVO', 'PENDENTE', 'DESATIVADO'))");
    }

    public function down(): void
    {
        DB::statement('ALTER TABLE auth.login DROP CONSTRAINT IF EXISTS ck_login_situacao');
        Schema::table('auth.login', function (Blueprint $table): void {
            $table->dropColumn('situacao');
        });
    }
};
