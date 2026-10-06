<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

// V3__cliente_tag.sql — vínculo N:N cliente↔tag (UK par).
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('vendas.cliente_tag', function (Blueprint $table): void {
            $table->id('id_cliente_tag');
            $table->unsignedBigInteger('id_cliente');
            $table->unsignedBigInteger('id_tag');

            $table->foreign('id_cliente', 'fk_cliente_tag_cliente')
                ->references('id_cliente')->on('vendas.cliente');
            $table->foreign('id_tag', 'fk_cliente_tag_tag')
                ->references('id_tag')->on('vendas.tag_cliente');

            $table->unique(['id_cliente', 'id_tag'], 'uq_cliente_tag');
            $table->index('id_cliente', 'idx_cliente_tag_cliente');
            $table->index('id_tag', 'idx_cliente_tag_tag');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('vendas.cliente_tag');
    }
};
