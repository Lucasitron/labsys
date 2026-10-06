<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

// V6__encomenda.sql — encomendas + Kanban com lock otimista (versao) e origem legada.
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('vendas.encomenda', function (Blueprint $table): void {
            $table->id('id_encomenda');
            $table->bigInteger('versao')->nullable();
            $table->unsignedBigInteger('id_orcamento')->nullable();
            $table->unsignedBigInteger('id_cliente');
            $table->date('data_criacao');
            $table->date('data_previsao_entrega')->nullable();
            $table->string('status_kanban', 16);
            $table->decimal('valor_final', 12, 2);
            $table->string('observacoes', 1000)->nullable();
            $table->bigInteger('criado_por');
            $table->unsignedBigInteger('encomenda_origem_id')->nullable();

            $table->foreign('id_orcamento', 'fk_encomenda_orcamento')
                ->references('id_orcamento')->on('vendas.orcamento');
            $table->foreign('id_cliente', 'fk_encomenda_cliente')
                ->references('id_cliente')->on('vendas.cliente');
            $table->foreign('encomenda_origem_id', 'fk_encomenda_origem')
                ->references('id_encomenda')->on('vendas.encomenda');

            $table->index('status_kanban', 'idx_encomenda_status');
            $table->index('id_cliente', 'idx_encomenda_cliente');
            $table->index('id_orcamento', 'idx_encomenda_orcamento');
        });

        DB::statement("ALTER TABLE vendas.encomenda ADD CONSTRAINT ck_encomenda_status_kanban CHECK (status_kanban IN ('Fila', 'Produção', 'Acabamento', 'Pronto', 'Entregue'))");
    }

    public function down(): void
    {
        Schema::dropIfExists('vendas.encomenda');
    }
};
