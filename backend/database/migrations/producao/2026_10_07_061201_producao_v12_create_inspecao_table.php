<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

// V12__create_inspecao_5s.sql (1:1 com o upstream).
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('producao.inspecao_5s', function (Blueprint $table): void {
            $table->id('id_inspecao');
            $table->unsignedBigInteger('id_setor');
            $table->bigInteger('id_inspetor');
            $table->date('data_inspecao');
            $table->string('turno', 10);
            $table->string('status', 20);
            $table->string('observacoes', 1000)->nullable();

            $table->foreign('id_setor', 'fk_inspecao_setor')
                ->references('id_setor')->on('producao.setor');

            $table->index('id_setor', 'idx_inspecao_setor');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('producao.inspecao_5s');
    }
};
