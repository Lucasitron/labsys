<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

// V12__processo_grupos_avaliacao.sql — grupos + nota/feedback por membro.
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('rh.grupo_processo_seletivo', function (Blueprint $table): void {
            $table->id();
            $table->string('nome', 255);
            $table->bigInteger('id_tutor_lider');
            $table->string('etapa', 32)->default('INSCRITO');

            $table->foreign('id_tutor_lider', 'fk_grupo_lider')
                ->references('id')->on('rh.funcionario');
        });

        Schema::table('rh.processo_seletivo', function (Blueprint $table): void {
            $table->bigInteger('id_grupo')->nullable();
            $table->decimal('nota', 4, 2)->nullable();
            $table->text('feedback')->nullable();

            $table->foreign('id_grupo', 'fk_processo_grupo')
                ->references('id')->on('rh.grupo_processo_seletivo');
            $table->index('id_grupo', 'ix_processo_grupo');
        });

        DB::statement("ALTER TABLE rh.grupo_processo_seletivo ADD CONSTRAINT ck_grupo_etapa CHECK (etapa IN ('INSCRITO', 'EM_TRIAGEM', 'ENTREVISTA', 'APROVADO', 'REPROVADO'))");
    }

    public function down(): void
    {
        Schema::table('rh.processo_seletivo', function (Blueprint $table): void {
            $table->dropForeign('fk_processo_grupo');
            $table->dropIndex('ix_processo_grupo');
            $table->dropColumn(['id_grupo', 'nota', 'feedback']);
        });
        Schema::dropIfExists('rh.grupo_processo_seletivo');
    }
};
