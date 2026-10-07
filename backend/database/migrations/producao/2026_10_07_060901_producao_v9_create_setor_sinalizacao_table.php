<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

// V9__create_setor_sinalizacao.sql (1:1 com o upstream).
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('producao.setor_sinalizacao', function (Blueprint $table): void {
            $table->id('id_sinalizacao');
            $table->unsignedBigInteger('id_setor');
            $table->string('texto', 255);

            $table->foreign('id_setor', 'fk_sinalizacao_setor')
                ->references('id_setor')->on('producao.setor');

            $table->index('id_setor', 'idx_sinalizacao_setor');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('producao.setor_sinalizacao');
    }
};
