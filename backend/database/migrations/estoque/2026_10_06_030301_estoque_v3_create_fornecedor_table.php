<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

// V3__create_fornecedor_table.sql — fornecedores.
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('estoque.fornecedor', function (Blueprint $table): void {
            $table->id('id_fornecedor');
            $table->string('nome', 255);
            $table->string('contato', 255)->nullable();
            $table->string('cnpj', 32)->nullable();
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('estoque.fornecedor');
    }
};
