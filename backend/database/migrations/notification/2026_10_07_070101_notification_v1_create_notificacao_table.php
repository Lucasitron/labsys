<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

// V1__notificacao.sql 1:1 (só qualificado `notification.`) + lifecycle de
// docs/07 §4 (`status` governa a ENTREGA; `lida` do Java governa a LEITURA).
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('notification.notificacao', function (Blueprint $table): void {
            $table->id();
            $table->unsignedBigInteger('id_usuario');
            $table->string('titulo', 160);
            $table->text('mensagem')->nullable();
            $table->boolean('lida')->default(false);
            $table->timestampTz('criada_em');
            $table->string('status', 20)->default('PENDENTE');
            $table->timestampTz('data_envio')->nullable();
            $table->timestampTz('data_leitura')->nullable();
            $table->bigInteger('id_referencia')->nullable();
            $table->index(['id_usuario', 'lida'], 'ix_notificacao_usuario');
        });

        DB::statement("ALTER TABLE notification.notificacao ADD CONSTRAINT ck_notificacao_status CHECK (status IN ('PENDENTE', 'ENVIADA'))");
    }

    public function down(): void
    {
        Schema::dropIfExists('notification.notificacao');
    }
};
