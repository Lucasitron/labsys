<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

// V8__create_item_bom_table.sql — itens da Lista de Materiais (BOM).
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('estoque.item_bom', function (Blueprint $table): void {
            $table->id('id_item_bom');
            $table->unsignedBigInteger('id_bom');
            $table->unsignedBigInteger('id_item');
            $table->decimal('quantidade_prevista', 12, 2);
            $table->decimal('quantidade_real', 12, 2)->nullable();

            $table->unique(['id_bom', 'id_item'], 'uk_item_bom_bom_item');

            $table->foreign('id_bom', 'fk_item_bom_bom')
                ->references('id_bom')->on('estoque.lista_materiais')
                ->cascadeOnDelete();
            $table->foreign('id_item', 'fk_item_bom_item')
                ->references('id_item')->on('estoque.item');

            $table->index('id_item', 'idx_item_bom_item');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('estoque.item_bom');
    }
};
