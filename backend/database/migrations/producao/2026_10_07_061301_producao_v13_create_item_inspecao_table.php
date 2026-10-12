<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

// V13__create_item_inspecao_5s.sql (1:1 com o upstream).
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('producao.item_inspecao_5s', function (Blueprint $table): void {
            $table->id('id_item_inspecao');
            $table->unsignedBigInteger('id_inspecao');
            $table->unsignedBigInteger('id_checklist');
            $table->boolean('conforme');
            $table->string('observacao', 500)->nullable();

            $table->foreign('id_inspecao', 'fk_item_inspecao')
                ->references('id_inspecao')->on('producao.inspecao_5s');
            $table->foreign('id_checklist', 'fk_item_checklist')
                ->references('id_checklist')->on('producao.setor_checklist');

            $table->index('id_inspecao', 'idx_item_inspecao');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('producao.item_inspecao_5s');
    }
};
