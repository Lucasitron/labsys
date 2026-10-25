<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

// V5__item_orcamento.sql — itens do orçamento (extensão D-5: material/horas/compra).
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('vendas.item_orcamento', function (Blueprint $table): void {
            $table->id('id_item_orcamento');
            $table->unsignedBigInteger('id_orcamento');
            $table->string('descricao', 500);
            $table->decimal('quantidade', 12, 2);
            $table->decimal('valor_unitario', 12, 2);
            $table->string('material_tipo', 64)->nullable();
            $table->decimal('material_quantidade', 12, 3)->nullable();
            $table->string('material_unidade', 16)->nullable();
            $table->decimal('horas', 10, 2)->nullable();
            $table->boolean('compra')->default(false);

            $table->foreign('id_orcamento', 'fk_item_orcamento_orcamento')
                ->references('id_orcamento')->on('vendas.orcamento')
                ->onDelete('cascade');

            $table->index('id_orcamento', 'idx_item_orcamento_orcamento');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('vendas.item_orcamento');
    }
};
