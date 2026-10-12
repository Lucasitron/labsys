<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

// V2__create_funcionario_table.sql — vínculo pessoa → nível de acesso.
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('rh.funcionario', function (Blueprint $table): void {
            $table->id();
            $table->bigInteger('id_pessoa')->unique('uk_funcionario_pessoa');
            $table->integer('nivel_acesso');
            $table->string('departamento', 255)->nullable();

            $table->foreign('id_pessoa', 'fk_funcionario_pessoa')
                ->references('id')->on('rh.pessoa');
        });

        DB::statement('ALTER TABLE rh.funcionario ADD CONSTRAINT ck_funcionario_nivel CHECK (nivel_acesso BETWEEN 0 AND 4)');
    }

    public function down(): void
    {
        Schema::dropIfExists('rh.funcionario');
    }
};
