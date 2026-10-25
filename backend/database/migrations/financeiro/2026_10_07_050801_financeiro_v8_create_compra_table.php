<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

// V8__solicitacao_compra.sql — solicitações de compra (fluxo informativo).
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('financeiro.solicitacao_compra', function (Blueprint $table): void {
            $table->id('id_solicitacao');
            $table->integer('id_item_estoque');
            $table->decimal('quantidade', 12, 2);
            $table->decimal('valor_estimado', 14, 2)->nullable();
            $table->string('status', 16);
            $table->date('data_solicitacao');
        });

        DB::statement("ALTER TABLE financeiro.solicitacao_compra ADD CONSTRAINT ck_compra_status CHECK (status IN ('REGISTRADA', 'VISUALIZADA', 'CONCLUIDA'))");
        DB::statement('ALTER TABLE financeiro.solicitacao_compra ADD CONSTRAINT ck_compra_quantidade CHECK (quantidade > 0)');
    }

    public function down(): void
    {
        Schema::dropIfExists('financeiro.solicitacao_compra');
    }
};
