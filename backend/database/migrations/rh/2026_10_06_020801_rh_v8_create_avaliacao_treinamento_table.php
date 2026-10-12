<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

// V8__create_avaliacao_treinamento_table.sql — nota 0-10 + feedback.
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('rh.avaliacao_treinamento', function (Blueprint $table): void {
            $table->id();
            $table->bigInteger('id_treinamento');
            $table->bigInteger('id_funcionario');
            $table->decimal('nota', 4, 2);
            $table->text('feedback')->nullable();
            $table->date('data_avaliacao')->nullable();

            $table->foreign('id_treinamento', 'fk_avaliacao_treinamento')
                ->references('id')->on('rh.treinamento');
            $table->foreign('id_funcionario', 'fk_avaliacao_funcionario')
                ->references('id')->on('rh.funcionario');
            $table->index('id_treinamento', 'ix_avaliacao_treinamento');
        });

        DB::statement('ALTER TABLE rh.avaliacao_treinamento ADD CONSTRAINT ck_avaliacao_nota CHECK (nota BETWEEN 0 AND 10)');
    }

    public function down(): void
    {
        Schema::dropIfExists('rh.avaliacao_treinamento');
    }
};
