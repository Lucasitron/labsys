<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

// V8__create_setor_material.sql (1:1 com o upstream).
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('producao.setor_material', function (Blueprint $table): void {
            $table->id('id_material');
            $table->unsignedBigInteger('id_setor');
            $table->string('descricao', 255);
            $table->decimal('quantidade', 12, 2);

            $table->foreign('id_setor', 'fk_material_setor')
                ->references('id_setor')->on('producao.setor');

            $table->index('id_setor', 'idx_material_setor');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('producao.setor_material');
    }
};
