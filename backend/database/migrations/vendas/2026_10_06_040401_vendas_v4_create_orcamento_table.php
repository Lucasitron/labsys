<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

// V4__orcamento.sql — orçamentos com status PT.
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('vendas.orcamento', function (Blueprint $table): void {
            $table->id('id_orcamento');
            $table->unsignedBigInteger('id_cliente');
            $table->date('data_criacao');
            $table->date('validade')->nullable();
            $table->decimal('valor_total', 12, 2);
            $table->string('status', 16);
            $table->string('observacoes', 1000)->nullable();
            $table->bigInteger('criado_por');

            $table->foreign('id_cliente', 'fk_orcamento_cliente')
                ->references('id_cliente')->on('vendas.cliente');

            $table->index('id_cliente', 'idx_orcamento_cliente');
            $table->index('status', 'idx_orcamento_status');
        });

        DB::statement("ALTER TABLE vendas.orcamento ADD CONSTRAINT ck_orcamento_status CHECK (status IN ('Pendente', 'Aprovado', 'Recusado', 'Ajuste'))");
    }

    public function down(): void
    {
        Schema::dropIfExists('vendas.orcamento');
    }
};
