<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

// V6__create_processo_seletivo_table.sql — base (grupo/nota/feedback vêm na V12).
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('rh.processo_seletivo', function (Blueprint $table): void {
            $table->id();
            $table->bigInteger('id_candidato');
            $table->bigInteger('id_tutor');
            $table->string('status_processo', 32);
            $table->date('data_inscricao');
            $table->string('resultado_final', 255)->nullable();

            $table->foreign('id_candidato', 'fk_processo_candidato')
                ->references('id')->on('rh.pessoa');
            $table->foreign('id_tutor', 'fk_processo_tutor')
                ->references('id')->on('rh.funcionario');
            $table->index('id_candidato', 'ix_processo_candidato');
        });

        DB::statement("ALTER TABLE rh.processo_seletivo ADD CONSTRAINT ck_processo_status CHECK (status_processo IN ('INSCRITO', 'EM_TRIAGEM', 'ENTREVISTA', 'APROVADO', 'REPROVADO'))");
    }

    public function down(): void
    {
        Schema::dropIfExists('rh.processo_seletivo');
    }
};
