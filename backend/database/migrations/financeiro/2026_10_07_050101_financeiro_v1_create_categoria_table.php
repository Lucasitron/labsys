<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

// V1__categoria_financeira.sql — categorias de receita/despesa.
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('financeiro.categoria_financeira', function (Blueprint $table): void {
            $table->id('id_categoria');
            $table->string('nome', 120);
            $table->string('tipo', 16);
            $table->string('descricao', 500)->nullable();
        });

        DB::statement("ALTER TABLE financeiro.categoria_financeira ADD CONSTRAINT ck_categoria_tipo CHECK (tipo IN ('RECEITA', 'DESPESA'))");
    }

    public function down(): void
    {
        Schema::dropIfExists('financeiro.categoria_financeira');
    }
};
