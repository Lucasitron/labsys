<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

// V4__create_historico_kanban.sql — trilha de movimentações (1:1 com o upstream).
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('producao.historico_kanban', function (Blueprint $table): void {
            $table->id('id_historico');
            $table->bigInteger('id_encomenda');
            $table->string('status_anterior', 20)->nullable();
            $table->string('status_novo', 20);
            $table->dateTime('data_alteracao');
            $table->bigInteger('id_usuario')->nullable();
            $table->string('observacao', 500)->nullable();

            $table->index('id_encomenda', 'idx_historico_kanban_encomenda');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('producao.historico_kanban');
    }
};
