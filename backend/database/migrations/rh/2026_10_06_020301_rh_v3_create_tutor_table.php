<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

// V3__create_tutor_table.sql — tutores (qualificação 0-6; distinto de "instrutor").
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('rh.tutor', function (Blueprint $table): void {
            $table->id();
            $table->bigInteger('id_funcionario')->unique('uk_tutor_funcionario');
            $table->string('turno', 32)->nullable();
            $table->integer('qualificacao')->nullable();

            $table->foreign('id_funcionario', 'fk_tutor_funcionario')
                ->references('id')->on('rh.funcionario');
        });

        DB::statement('ALTER TABLE rh.tutor ADD CONSTRAINT ck_tutor_qualificacao CHECK (qualificacao BETWEEN 0 AND 6)');
    }

    public function down(): void
    {
        Schema::dropIfExists('rh.tutor');
    }
};
