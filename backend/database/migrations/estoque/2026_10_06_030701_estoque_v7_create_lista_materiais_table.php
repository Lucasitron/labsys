<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

// V7__create_lista_materiais_table.sql — Listas de Materiais (BOM).
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('estoque.lista_materiais', function (Blueprint $table): void {
            $table->id('id_bom');
            // Referência externa à Produção (produto/serviço): sem FK cross-schema.
            $table->bigInteger('id_produto_servico');
            $table->string('nome', 255);
            $table->integer('versao');
            $table->boolean('editavel');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('estoque.lista_materiais');
    }
};
