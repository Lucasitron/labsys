<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

// V11__create_setor_responsavel.sql (1:1 com o upstream).
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('producao.setor_responsavel', function (Blueprint $table): void {
            $table->id('id_responsavel');
            $table->unsignedBigInteger('id_setor');
            $table->bigInteger('id_funcionario');
            $table->date('data_inicio');
            $table->date('data_fim')->nullable();
            $table->boolean('ativo')->default(true);

            $table->foreign('id_setor', 'fk_responsavel_setor')
                ->references('id_setor')->on('producao.setor');

            $table->index('id_setor', 'idx_responsavel_setor');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('producao.setor_responsavel');
    }
};
