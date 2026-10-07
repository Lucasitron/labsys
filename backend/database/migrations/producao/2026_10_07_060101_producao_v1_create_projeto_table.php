<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

// V1__create_projeto.sql — projetos (1:1 com o upstream, só qualificado `producao.`).
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('producao.projeto', function (Blueprint $table): void {
            $table->id('id_projeto');
            $table->string('nome', 150);
            $table->string('descricao', 1000)->nullable();
            $table->date('data_inicio');
            $table->date('data_fim_prevista')->nullable();
            $table->date('data_fim_real')->nullable();
            $table->string('status', 20);
            $table->bigInteger('id_responsavel');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('producao.projeto');
    }
};
