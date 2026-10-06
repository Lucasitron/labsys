<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

// V9__interacao_cliente.sql — timeline do CRM.
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('vendas.interacao_cliente', function (Blueprint $table): void {
            $table->id('id_interacao');
            $table->unsignedBigInteger('id_cliente');
            $table->timestamp('data_interacao');
            $table->string('tipo', 16);
            $table->string('descricao', 2000);
            $table->bigInteger('id_usuario');

            $table->foreign('id_cliente', 'fk_interacao_cliente')
                ->references('id_cliente')->on('vendas.cliente')
                ->onDelete('cascade');

            $table->index(['id_cliente', 'data_interacao'], 'idx_interacao_cliente');
        });

        DB::statement("ALTER TABLE vendas.interacao_cliente ADD CONSTRAINT ck_interacao_tipo CHECK (tipo IN ('E-mail', 'Telefone', 'Reunião', 'WhatsApp'))");
    }

    public function down(): void
    {
        Schema::dropIfExists('vendas.interacao_cliente');
    }
};
