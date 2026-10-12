<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

// V5__create_apontamento_horas_table.sql — horas em ENCOMENDA/PROJETO.
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('rh.apontamento_horas', function (Blueprint $table): void {
            $table->id();
            $table->bigInteger('id_funcionario');
            $table->string('tipo', 32);
            $table->bigInteger('id_referencia');
            $table->date('data');
            $table->decimal('horas_trabalhadas', 5, 2);
            $table->text('descricao_atividade')->nullable();
            $table->string('status', 32);
            $table->bigInteger('id_admin_validador')->nullable();
            $table->timestampTz('data_validacao')->nullable();

            $table->foreign('id_funcionario', 'fk_apontamento_funcionario')
                ->references('id')->on('rh.funcionario');
            $table->foreign('id_admin_validador', 'fk_apontamento_validador')
                ->references('id')->on('rh.funcionario');
            $table->index(['id_funcionario', 'data'], 'ix_apontamento_funcionario');
        });

        DB::statement("ALTER TABLE rh.apontamento_horas ADD CONSTRAINT ck_apontamento_tipo CHECK (tipo IN ('ENCOMENDA', 'PROJETO'))");
        DB::statement("ALTER TABLE rh.apontamento_horas ADD CONSTRAINT ck_apontamento_status CHECK (status IN ('PENDENTE', 'VALIDADO', 'REJEITADO'))");
    }

    public function down(): void
    {
        Schema::dropIfExists('rh.apontamento_horas');
    }
};
