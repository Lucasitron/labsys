<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

// V8__create_token_integracao_table.sql — tokens de integração: SOMENTE
// hash SHA-256 + prefixo. A chave em claro é exibida 1× e nunca persistida.
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('auth.token_integracao', function (Blueprint $table): void {
            $table->id();
            $table->string('nome', 128);
            $table->string('prefixo', 32);
            $table->string('hash', 64)->unique('ix_token_integracao_hash');
            $table->timestampTz('criado_em')->useCurrent();
            $table->timestampTz('ultimo_uso')->nullable();
            $table->boolean('revogado')->default(false);
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('auth.token_integracao');
    }
};
