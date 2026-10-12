<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

// V7__create_treinamento_table.sql — LMS (tutor ministra; vínculo tutor, não texto livre).
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('rh.treinamento', function (Blueprint $table): void {
            $table->id();
            $table->string('titulo', 255);
            $table->text('descricao')->nullable();
            $table->string('url_conteudo', 500)->nullable();
            $table->bigInteger('id_tutor');

            $table->foreign('id_tutor', 'fk_treinamento_tutor')
                ->references('id')->on('rh.funcionario');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('rh.treinamento');
    }
};
