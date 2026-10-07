<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

// V2__create_tarefa.sql — tarefas vinculadas a projeto (1:1 com o upstream).
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('producao.tarefa', function (Blueprint $table): void {
            $table->id('id_tarefa');
            $table->unsignedBigInteger('id_projeto');
            $table->string('titulo', 150);
            $table->string('descricao', 1000)->nullable();
            $table->bigInteger('id_responsavel')->nullable();
            $table->date('data_inicio')->nullable();
            $table->date('data_fim_prevista')->nullable();
            $table->date('data_conclusao')->nullable();
            $table->string('status', 20);
            $table->string('prioridade', 20);

            $table->foreign('id_projeto', 'fk_tarefa_projeto')
                ->references('id_projeto')->on('producao.projeto');

            $table->index('id_projeto', 'idx_tarefa_projeto');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('producao.tarefa');
    }
};
