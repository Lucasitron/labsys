<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

// V15__create_projeto_mesa.sql — `id_mesa` BIGINT (drift §M6.1.1: o SQL upstream
// prevalece sobre o requirements §3.5, que dizia String).
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('producao.projeto_mesa', function (Blueprint $table): void {
            $table->id('id_projeto_mesa');
            $table->bigInteger('id_funcionario');
            $table->bigInteger('id_mesa');
            $table->string('nome_projeto', 150);
            $table->string('tipo_projeto', 100)->nullable();
            $table->date('prazo_execucao')->nullable();
            $table->date('data_inicio');
            $table->date('data_ultima_evolucao')->nullable();
            $table->string('status', 20);
            $table->string('qr_code_totem', 500)->nullable();
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('producao.projeto_mesa');
    }
};
