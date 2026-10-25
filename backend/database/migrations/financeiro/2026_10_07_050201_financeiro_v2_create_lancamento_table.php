<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

// V2__lancamento_financeiro.sql — contas a pagar/receber.
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('financeiro.lancamento_financeiro', function (Blueprint $table): void {
            $table->id('id_lancamento');
            $table->unsignedBigInteger('id_categoria');
            $table->string('tipo', 16);
            $table->decimal('valor', 14, 2);
            $table->date('data_vencimento');
            $table->date('data_pagamento')->nullable();
            $table->string('status', 16);
            $table->string('id_referencia_externa', 100)->nullable();
            $table->string('observacao', 1000)->nullable();

            $table->foreign('id_categoria', 'fk_lancamento_categoria')
                ->references('id_categoria')->on('financeiro.categoria_financeira');

            $table->index(['status', 'data_vencimento'], 'idx_lancamento_status_vencimento');
            $table->index('id_referencia_externa', 'idx_lancamento_referencia');
        });

        DB::statement("ALTER TABLE financeiro.lancamento_financeiro ADD CONSTRAINT ck_lancamento_tipo CHECK (tipo IN ('ENTRADA', 'SAIDA'))");
        DB::statement("ALTER TABLE financeiro.lancamento_financeiro ADD CONSTRAINT ck_lancamento_status CHECK (status IN ('PENDENTE', 'PAGO', 'ATRASADO', 'CANCELADO'))");
        DB::statement('ALTER TABLE financeiro.lancamento_financeiro ADD CONSTRAINT ck_lancamento_valor CHECK (valor > 0)');
    }

    public function down(): void
    {
        Schema::dropIfExists('financeiro.lancamento_financeiro');
    }
};
