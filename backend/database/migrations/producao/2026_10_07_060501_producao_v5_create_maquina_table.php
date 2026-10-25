<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

// V5__create_maquina.sql (1:1 com o upstream).
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('producao.maquina', function (Blueprint $table): void {
            $table->id('id_maquina');
            $table->string('nome', 150);
            $table->string('descricao', 1000)->nullable();
            $table->string('status', 20);
            $table->string('localizacao', 150)->nullable();
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('producao.maquina');
    }
};
