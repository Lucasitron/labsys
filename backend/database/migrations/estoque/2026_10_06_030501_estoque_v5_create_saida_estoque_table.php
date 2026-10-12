<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

// V5__create_saida_estoque_table.sql — saídas (consumo, perda, ajuste, empréstimo).
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('estoque.saida_estoque', function (Blueprint $table): void {
            $table->id('id_saida');
            $table->unsignedBigInteger('id_item');
            $table->decimal('quantidade', 12, 2);
            $table->string('tipo_saida', 32);
            $table->bigInteger('id_referencia')->nullable();
            $table->timestamp('data_saida');
            $table->string('observacao', 255)->nullable();

            $table->foreign('id_item', 'fk_saida_item')
                ->references('id_item')->on('estoque.item');

            $table->index('id_item', 'idx_saida_item');
        });

        DB::statement("ALTER TABLE estoque.saida_estoque ADD CONSTRAINT ck_saida_tipo CHECK (tipo_saida IN ('CONSUMO', 'PERDA', 'AJUSTE', 'EMPRESTIMO'))");
    }

    public function down(): void
    {
        Schema::dropIfExists('estoque.saida_estoque');
    }
};
