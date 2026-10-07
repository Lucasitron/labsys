<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

// V6__fechamento_encomenda.sql — congelamento de valor/horas por encomenda (D-5).
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('financeiro.fechamento_encomenda', function (Blueprint $table): void {
            $table->id('id_fechamento');
            $table->integer('id_encomenda')->unique();
            $table->decimal('horas_estimadas', 14, 2);
            $table->decimal('valor_fechado', 14, 2);
            $table->date('data_fechamento');
            $table->string('status', 16);
            $table->decimal('horas_validadas', 14, 2)->default(0);
        });

        DB::statement("ALTER TABLE financeiro.fechamento_encomenda ADD CONSTRAINT ck_fechamento_status CHECK (status IN ('ABERTA', 'CONCLUIDA', 'CANCELADA'))");
        DB::statement('ALTER TABLE financeiro.fechamento_encomenda ADD CONSTRAINT ck_fechamento_horas CHECK (horas_estimadas >= 0)');
        DB::statement('ALTER TABLE financeiro.fechamento_encomenda ADD CONSTRAINT ck_fechamento_valor CHECK (valor_fechado >= 0)');
    }

    public function down(): void
    {
        Schema::dropIfExists('financeiro.fechamento_encomenda');
    }
};
