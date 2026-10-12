<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

// V4__create_registro_ponto_diario_table.sql — ponto consolidado do RFID (1/dia).
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('rh.registro_ponto_diario', function (Blueprint $table): void {
            $table->id();
            $table->bigInteger('id_funcionario');
            $table->date('data');
            $table->timestampTz('hora_entrada')->nullable();
            $table->timestampTz('hora_saida')->nullable();
            $table->decimal('total_horas', 5, 2)->nullable();

            $table->foreign('id_funcionario', 'fk_ponto_funcionario')
                ->references('id')->on('rh.funcionario');
            $table->unique(['id_funcionario', 'data'], 'uk_ponto_funcionario_data');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('rh.registro_ponto_diario');
    }
};
