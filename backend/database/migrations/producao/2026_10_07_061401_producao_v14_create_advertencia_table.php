<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

// V14__create_advertencia_membro.sql — `id_inspecao` nullable (1:1 com o upstream).
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('producao.advertencia_membro', function (Blueprint $table): void {
            $table->id('id_advertencia');
            $table->bigInteger('id_funcionario');
            $table->unsignedBigInteger('id_inspecao')->nullable();
            $table->date('data');
            $table->string('motivo', 500);
            $table->string('tipo', 20);
            $table->integer('contador');
            $table->bigInteger('id_admin_registrou')->nullable();

            $table->foreign('id_inspecao', 'fk_advertencia_inspecao')
                ->references('id_inspecao')->on('producao.inspecao_5s');

            $table->index('id_funcionario', 'idx_advertencia_funcionario');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('producao.advertencia_membro');
    }
};
