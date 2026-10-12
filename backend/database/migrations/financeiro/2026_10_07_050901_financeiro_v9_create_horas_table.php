<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

// V9__horas_encomenda.sql — horas validadas do RH por (encomenda, funcionário, data).
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('financeiro.horas_encomenda', function (Blueprint $table): void {
            $table->id('id_horas');
            $table->integer('id_encomenda');
            $table->integer('id_funcionario');
            $table->integer('nivel_acesso')->nullable();
            $table->decimal('horas', 12, 2);
            $table->date('data_registro');

            $table->unique(['id_encomenda', 'id_funcionario', 'data_registro'], 'uq_horas_encomenda');
            $table->index('id_encomenda', 'idx_horas_encomenda_id_encomenda');
        });

        DB::statement('ALTER TABLE financeiro.horas_encomenda ADD CONSTRAINT ck_horas_nivel CHECK (nivel_acesso IS NULL OR nivel_acesso BETWEEN 0 AND 3)');
        DB::statement('ALTER TABLE financeiro.horas_encomenda ADD CONSTRAINT ck_horas_valor CHECK (horas > 0)');
    }

    public function down(): void
    {
        Schema::dropIfExists('financeiro.horas_encomenda');
    }
};
