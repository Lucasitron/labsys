<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

// V18__create_consumo_encomenda.sql — sem PK composta (upsert por par, 1:1).
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('producao.consumo_encomenda', function (Blueprint $table): void {
            $table->id('id_consumo');
            $table->bigInteger('id_encomenda');
            $table->bigInteger('id_item');
            $table->decimal('quantidade_consumida', 12, 2);

            $table->index('id_encomenda', 'idx_consumo_encomenda');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('producao.consumo_encomenda');
    }
};
