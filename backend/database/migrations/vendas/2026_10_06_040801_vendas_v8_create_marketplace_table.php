<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

// V8__registro_marketplace.sql — vendas manuais externas (UK plataforma+código).
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('vendas.registro_marketplace', function (Blueprint $table): void {
            $table->id('id_registro');
            $table->unsignedBigInteger('id_encomenda');
            $table->string('plataforma', 64);
            $table->string('codigo_externo', 64);
            $table->date('data_venda');
            $table->decimal('valor_taxa', 12, 2);

            $table->foreign('id_encomenda', 'fk_marketplace_encomenda')
                ->references('id_encomenda')->on('vendas.encomenda');

            $table->unique(['plataforma', 'codigo_externo'], 'uq_marketplace_plataforma_codigo');
            $table->index('id_encomenda', 'idx_marketplace_encomenda');
            $table->index('plataforma', 'idx_marketplace_plataforma');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('vendas.registro_marketplace');
    }
};
