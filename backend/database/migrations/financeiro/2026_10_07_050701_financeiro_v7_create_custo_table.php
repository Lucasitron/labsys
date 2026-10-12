<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

// V7__custo_encomenda.sql — custos congelados por encomenda (Job Order Costing).
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('financeiro.custo_encomenda', function (Blueprint $table): void {
            $table->id('id_custo');
            $table->integer('id_encomenda');
            $table->decimal('custo_materiais', 14, 2);
            $table->decimal('custo_mao_obra', 14, 2);
            $table->decimal('custo_overhead', 14, 2);
            $table->decimal('custo_total', 14, 2);
            $table->decimal('valor_venda', 14, 2);
            $table->decimal('margem_lucro', 14, 2);
            $table->date('data_calculo');

            $table->index('id_encomenda', 'idx_custo_encomenda_id_encomenda');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('financeiro.custo_encomenda');
    }
};
