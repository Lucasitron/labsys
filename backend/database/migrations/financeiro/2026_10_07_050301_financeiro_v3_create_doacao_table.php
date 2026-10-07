<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

// V3__doacao_recurso.sql — doações e recursos de projetos.
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('financeiro.doacao_recurso', function (Blueprint $table): void {
            $table->id('id_doacao');
            $table->string('tipo', 16);
            $table->string('origem', 200);
            $table->decimal('valor', 14, 2);
            $table->date('data_recebimento');
            $table->integer('id_projeto_associado')->nullable();
        });

        DB::statement("ALTER TABLE financeiro.doacao_recurso ADD CONSTRAINT ck_doacao_tipo CHECK (tipo IN ('DOACAO', 'PROJETO'))");
        DB::statement('ALTER TABLE financeiro.doacao_recurso ADD CONSTRAINT ck_doacao_valor CHECK (valor > 0)');
    }

    public function down(): void
    {
        Schema::dropIfExists('financeiro.doacao_recurso');
    }
};
