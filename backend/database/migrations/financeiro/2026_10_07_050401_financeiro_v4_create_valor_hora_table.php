<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

// V4__valor_hora_nivel.sql — valor/hora por nível de acesso (0-3), com vigência.
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('financeiro.valor_hora_nivel', function (Blueprint $table): void {
            $table->id('id_valor_hora');
            $table->integer('nivel_acesso');
            $table->decimal('valor_hora', 12, 2);
            $table->date('data_vigencia');

            $table->index(['nivel_acesso', 'data_vigencia'], 'idx_valor_hora_nivel_vigencia');
        });

        DB::statement('ALTER TABLE financeiro.valor_hora_nivel ADD CONSTRAINT ck_valor_hora_nivel CHECK (nivel_acesso BETWEEN 0 AND 3)');
        DB::statement('ALTER TABLE financeiro.valor_hora_nivel ADD CONSTRAINT ck_valor_hora_valor CHECK (valor_hora >= 0)');
    }

    public function down(): void
    {
        Schema::dropIfExists('financeiro.valor_hora_nivel');
    }
};
