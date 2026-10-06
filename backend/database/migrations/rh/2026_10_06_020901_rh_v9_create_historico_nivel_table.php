<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

// V9__create_historico_nivel_table.sql — auditoria de evolução de nível.
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('rh.historico_nivel', function (Blueprint $table): void {
            $table->id();
            $table->bigInteger('id_funcionario');
            $table->integer('nivel_antigo');
            $table->integer('nivel_novo');
            $table->bigInteger('id_admin_alterou');
            $table->timestampTz('data_alteracao');

            $table->foreign('id_funcionario', 'fk_historico_funcionario')
                ->references('id')->on('rh.funcionario');
            $table->foreign('id_admin_alterou', 'fk_historico_admin')
                ->references('id')->on('rh.funcionario');
            $table->index('id_funcionario', 'ix_historico_funcionario');
        });

        DB::statement('ALTER TABLE rh.historico_nivel ADD CONSTRAINT ck_historico_nivel_antigo CHECK (nivel_antigo BETWEEN 0 AND 4)');
        DB::statement('ALTER TABLE rh.historico_nivel ADD CONSTRAINT ck_historico_nivel_novo CHECK (nivel_novo BETWEEN 0 AND 4)');
    }

    public function down(): void
    {
        Schema::dropIfExists('rh.historico_nivel');
    }
};
