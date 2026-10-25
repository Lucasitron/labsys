<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

// V5__parametro_overhead.sql — taxa de overhead por hora, com vigência.
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('financeiro.parametro_overhead', function (Blueprint $table): void {
            $table->id('id_parametro');
            $table->decimal('valor_taxa_hora', 12, 4);
            $table->date('data_vigencia');
        });

        DB::statement('ALTER TABLE financeiro.parametro_overhead ADD CONSTRAINT ck_overhead_taxa CHECK (valor_taxa_hora >= 0)');
    }

    public function down(): void
    {
        Schema::dropIfExists('financeiro.parametro_overhead');
    }
};
