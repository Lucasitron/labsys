<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

// V7__create_setor.sql (1:1 com o upstream).
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('producao.setor', function (Blueprint $table): void {
            $table->id('id_setor');
            $table->integer('numero');
            $table->string('nome', 150);
            $table->string('descricao', 1000)->nullable();
            $table->string('observacoes', 1000)->nullable();
            $table->string('foto_correto_url', 500)->nullable();
            $table->string('foto_incorreto_url', 500)->nullable();
            $table->boolean('ativo')->default(true);
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('producao.setor');
    }
};
