<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

// V7__historico_status_encomenda.sql — auditoria do Kanban (usuário + data).
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('vendas.historico_status_encomenda', function (Blueprint $table): void {
            $table->id('id_historico');
            $table->unsignedBigInteger('id_encomenda');
            $table->string('status_anterior', 16);
            $table->string('status_novo', 16);
            $table->timestamp('data_alteracao');
            $table->bigInteger('id_usuario');
            $table->string('observacao', 1000)->nullable();

            $table->foreign('id_encomenda', 'fk_historico_encomenda')
                ->references('id_encomenda')->on('vendas.encomenda')
                ->onDelete('cascade');

            $table->index(['id_encomenda', 'data_alteracao'], 'idx_historico_encomenda');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('vendas.historico_status_encomenda');
    }
};
