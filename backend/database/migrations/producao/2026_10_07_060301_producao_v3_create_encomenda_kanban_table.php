<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

// V3__create_encomenda_kanban.sql — cartão por encomenda (UNIQUE) + lock otimista `version` (1:1).
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('producao.encomenda_kanban', function (Blueprint $table): void {
            $table->id('id_kanban');
            $table->bigInteger('id_encomenda')->unique();
            $table->string('status', 20);
            $table->dateTime('data_entrada_status');
            $table->bigInteger('id_responsavel')->nullable();
            $table->integer('ordem');
            $table->bigInteger('version')->default(0);
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('producao.encomenda_kanban');
    }
};
