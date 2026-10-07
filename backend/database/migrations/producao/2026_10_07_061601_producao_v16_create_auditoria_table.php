<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

// V16__create_auditoria_projeto_mesa.sql (1:1 com o upstream).
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('producao.auditoria_projeto_mesa', function (Blueprint $table): void {
            $table->id('id_auditoria');
            $table->unsignedBigInteger('id_projeto_mesa');
            $table->date('data_auditoria');
            $table->string('resultado', 20);
            $table->string('acao_tomada', 500)->nullable();
            $table->bigInteger('id_admin_responsavel');

            $table->foreign('id_projeto_mesa', 'fk_auditoria_projeto_mesa')
                ->references('id_projeto_mesa')->on('producao.projeto_mesa');

            $table->index('id_projeto_mesa', 'idx_auditoria_projeto_mesa');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('producao.auditoria_projeto_mesa');
    }
};
