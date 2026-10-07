<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

// V6__create_historico_uso_maquina.sql (1:1 com o upstream).
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('producao.historico_uso_maquina', function (Blueprint $table): void {
            $table->id('id_uso');
            $table->unsignedBigInteger('id_maquina');
            $table->bigInteger('id_funcionario');
            $table->dateTime('data_inicio');
            $table->dateTime('data_fim')->nullable();
            $table->decimal('horas_uso', 10, 2)->nullable();
            $table->string('observacao', 500)->nullable();

            $table->foreign('id_maquina', 'fk_uso_maquina')
                ->references('id_maquina')->on('producao.maquina');

            $table->index('id_maquina', 'idx_uso_maquina');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('producao.historico_uso_maquina');
    }
};
