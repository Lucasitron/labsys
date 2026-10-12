<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

// V6__create_permissao_matriz_table.sql — matriz RBAC persistente
// (PUT /api/permissoes sobrevive ao restart) + seed 8 módulos × 5 papéis.
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('auth.permissao_matriz', function (Blueprint $table): void {
            $table->string('modulo', 64);
            $table->integer('role');
            $table->string('nivel', 16);
            $table->primary(['modulo', 'role'], 'pk_permissao_matriz');
        });

        DB::statement('ALTER TABLE auth.permissao_matriz ADD CONSTRAINT ck_permissao_matriz_role CHECK (role BETWEEN 0 AND 4)');
        DB::statement("ALTER TABLE auth.permissao_matriz ADD CONSTRAINT ck_permissao_matriz_nivel CHECK (nivel IN ('VER', 'EDITAR', 'NENHUM'))");

        foreach (['dashboard', 'rh', 'estoque', 'vendas', 'financeiro', 'producao', 'notificacoes', 'configuracoes'] as $modulo) {
            foreach ([0, 1, 2, 3, 4] as $role) {
                DB::table('auth.permissao_matriz')->insert([
                    'modulo' => $modulo,
                    'role' => $role,
                    'nivel' => $role === 0 ? 'EDITAR' : ($role === 4 ? 'NENHUM' : 'VER'),
                ]);
            }
        }
    }

    public function down(): void
    {
        Schema::dropIfExists('auth.permissao_matriz');
    }
};
