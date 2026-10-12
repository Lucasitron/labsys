<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

// V9__add_responsavel_movimentacoes.sql — responsável pelo registro das movimentações.
return new class extends Migration
{
    public function up(): void
    {
        Schema::table('estoque.entrada_estoque', function (Blueprint $table): void {
            $table->string('responsavel', 255)->nullable();
        });

        Schema::table('estoque.saida_estoque', function (Blueprint $table): void {
            $table->string('responsavel', 255)->nullable();
        });

        Schema::table('estoque.emprestimo', function (Blueprint $table): void {
            $table->string('responsavel', 255)->nullable();
        });
    }

    public function down(): void
    {
        Schema::table('estoque.emprestimo', function (Blueprint $table): void {
            $table->dropColumn('responsavel');
        });

        Schema::table('estoque.saida_estoque', function (Blueprint $table): void {
            $table->dropColumn('responsavel');
        });

        Schema::table('estoque.entrada_estoque', function (Blueprint $table): void {
            $table->dropColumn('responsavel');
        });
    }
};
