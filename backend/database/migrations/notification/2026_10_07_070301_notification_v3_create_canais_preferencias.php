<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

// V3 = `configuracao_canal` (docs/07 §4 1:1) + `create preferencia_notificacao`
// (a matriz do Java era in-memory e não sobrevive em PHP stateless; sem esta
// tabela o `PUT preferences` do contrato é morto).
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('notification.configuracao_canal', function (Blueprint $table): void {
            $table->id('id_configuracao');
            $table->string('canal', 20)->unique();
            $table->boolean('habilitado')->default(false);
            $table->json('parametros')->nullable();
        });

        Schema::create('notification.preferencia_notificacao', function (Blueprint $table): void {
            $table->id();
            $table->string('tipo', 40);
            $table->string('canal', 20);
            $table->boolean('habilitado')->default(true);
            $table->unique(['tipo', 'canal'], 'uk_preferencia_tipo_canal');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('notification.preferencia_notificacao');
        Schema::dropIfExists('notification.configuracao_canal');
    }
};
