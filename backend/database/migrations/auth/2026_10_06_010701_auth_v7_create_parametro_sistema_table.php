<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

// V7__create_parametro_sistema_table.sql — parâmetros globais (KV) + 4 seeds.
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('auth.parametro_sistema', function (Blueprint $table): void {
            $table->string('chave', 128)->primary('pk_parametro_sistema');
            $table->string('valor', 2048);
        });

        DB::table('auth.parametro_sistema')->insert([
            ['chave' => 'identidade.nomeFablab', 'valor' => 'FabLab IFPR — Curitiba'],
            ['chave' => 'identidade.logo', 'valor' => ''],
            ['chave' => 'cadencia.checklist5S', 'valor' => 'Semanal'],
            ['chave' => 'cadencia.auditoria5S', 'valor' => 'Mensal'],
        ]);
    }

    public function down(): void
    {
        Schema::dropIfExists('auth.parametro_sistema');
    }
};
