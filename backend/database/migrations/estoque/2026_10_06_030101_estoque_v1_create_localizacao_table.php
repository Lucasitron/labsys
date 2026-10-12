<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

// V1__create_localizacao_table.sql — localizações físicas dos itens.
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('estoque.localizacao', function (Blueprint $table): void {
            $table->id('id_localizacao');
            $table->string('armario', 255)->nullable();
            $table->string('prateleira', 255)->nullable();
            $table->string('caixa', 255)->nullable();
            $table->string('descricao', 255)->nullable();
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('estoque.localizacao');
    }
};
