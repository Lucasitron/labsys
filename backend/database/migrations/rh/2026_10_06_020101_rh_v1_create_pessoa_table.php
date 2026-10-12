<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

// V1__create_pessoa_table.sql — pessoas do Fab Lab (id = referência externa do Auth).
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('rh.pessoa', function (Blueprint $table): void {
            $table->id();
            $table->string('nome_completo', 255);
            $table->string('matricula', 64)->unique('uk_pessoa_matricula');
            $table->date('data_admissao')->nullable();
            $table->string('contato', 255)->nullable();
            $table->string('turno', 32)->nullable();
            $table->integer('status');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('rh.pessoa');
    }
};
