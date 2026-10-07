<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

// V10__create_setor_checklist.sql (1:1 com o upstream — sem peso/reordenação).
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('producao.setor_checklist', function (Blueprint $table): void {
            $table->id('id_checklist');
            $table->unsignedBigInteger('id_setor');
            $table->string('item', 255);
            $table->boolean('ativo')->default(true);

            $table->foreign('id_setor', 'fk_checklist_setor')
                ->references('id_setor')->on('producao.setor');

            $table->index('id_setor', 'idx_checklist_setor');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('producao.setor_checklist');
    }
};
