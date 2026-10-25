<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

// V4__create_entrada_estoque_table.sql — entradas (compras simples).
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('estoque.entrada_estoque', function (Blueprint $table): void {
            $table->id('id_entrada');
            $table->unsignedBigInteger('id_item');
            $table->unsignedBigInteger('id_fornecedor');
            $table->decimal('quantidade', 12, 2);
            $table->decimal('valor_unitario', 12, 2);
            $table->decimal('valor_total', 12, 2);
            $table->date('data_entrada');
            $table->string('nota_fiscal', 255)->nullable();
            $table->string('observacao', 255)->nullable();

            $table->foreign('id_item', 'fk_entrada_item')
                ->references('id_item')->on('estoque.item');
            $table->foreign('id_fornecedor', 'fk_entrada_fornecedor')
                ->references('id_fornecedor')->on('estoque.fornecedor');

            $table->index('id_item', 'idx_entrada_item');
            $table->index('id_fornecedor', 'idx_entrada_fornecedor');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('estoque.entrada_estoque');
    }
};
