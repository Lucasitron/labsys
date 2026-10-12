<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

// V11__solicitacao_edicao.sql — contrato mínimo D-4 (decidir só flipa status).
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('vendas.solicitacao_edicao', function (Blueprint $table): void {
            $table->id('id_solicitacao');
            $table->string('tipo', 32);
            $table->string('alvo_tipo', 16);
            $table->bigInteger('alvo_id');
            $table->string('campo', 128);
            $table->string('valor_atual', 1000)->nullable();
            $table->string('valor_proposto', 1000);
            $table->string('justificativa', 1000);
            $table->string('status', 16);
            $table->bigInteger('solicitante_id');
            $table->bigInteger('decidido_por')->nullable();
            $table->string('motivo_decisao', 1000)->nullable();
            $table->timestamp('data_criacao');
            $table->timestamp('data_decisao')->nullable();

            $table->index('status', 'idx_solicitacao_status');
        });

        DB::statement("ALTER TABLE vendas.solicitacao_edicao ADD CONSTRAINT ck_solicitacao_tipo CHECK (tipo IN ('ALTERACAO_DADOS', 'MUDANCA_STATUS', 'MOVER_ENCOMENDA', 'OUTRA'))");
        DB::statement("ALTER TABLE vendas.solicitacao_edicao ADD CONSTRAINT ck_solicitacao_alvo_tipo CHECK (alvo_tipo IN ('CLIENTE', 'ORCAMENTO', 'ENCOMENDA'))");
        DB::statement("ALTER TABLE vendas.solicitacao_edicao ADD CONSTRAINT ck_solicitacao_status CHECK (status IN ('Pendente', 'Aprovada', 'Rejeitada'))");
    }

    public function down(): void
    {
        Schema::dropIfExists('vendas.solicitacao_edicao');
    }
};
