<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

// V10__tarefa_marketing.sql — tarefas internas (responsável = id, sem FK cross-schema).
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('vendas.tarefa_marketing', function (Blueprint $table): void {
            $table->id('id_tarefa');
            $table->string('titulo', 255);
            $table->string('descricao', 2000)->nullable();
            $table->bigInteger('id_responsavel');
            $table->date('data_inicio')->nullable();
            $table->date('data_fim')->nullable();
            $table->string('status', 16);
            $table->string('prioridade', 16);
            $table->bigInteger('criado_por');

            $table->index('id_responsavel', 'idx_tarefa_responsavel');
            $table->index('status', 'idx_tarefa_status');
        });

        DB::statement("ALTER TABLE vendas.tarefa_marketing ADD CONSTRAINT ck_tarefa_status CHECK (status IN ('Pendente', 'Em Andamento', 'Concluída'))");
        DB::statement("ALTER TABLE vendas.tarefa_marketing ADD CONSTRAINT ck_tarefa_prioridade CHECK (prioridade IN ('Baixa', 'Média', 'Alta'))");
    }

    public function down(): void
    {
        Schema::dropIfExists('vendas.tarefa_marketing');
    }
};
