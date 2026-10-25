<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

// V2__create_item_table.sql — itens de inventário (insumos, ferramentas, peças).
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('estoque.item', function (Blueprint $table): void {
            $table->id('id_item');
            $table->string('nome', 255);
            $table->string('descricao', 255)->nullable();
            $table->string('categoria', 32);
            $table->string('unidade_medida', 32);
            $table->decimal('quantidade_atual', 12, 2);
            $table->decimal('estoque_minimo', 12, 2);
            $table->bigInteger('versao')->default(0);
            $table->unsignedBigInteger('localizacao_id')->nullable();

            $table->foreign('localizacao_id', 'fk_item_localizacao')
                ->references('id_localizacao')->on('estoque.localizacao');

            $table->index('localizacao_id', 'idx_item_localizacao');
        });

        DB::statement("ALTER TABLE estoque.item ADD CONSTRAINT ck_item_categoria CHECK (categoria IN ('INSUMO', 'FERRAMENTA', 'PECA'))");
    }

    public function down(): void
    {
        Schema::dropIfExists('estoque.item');
    }
};
