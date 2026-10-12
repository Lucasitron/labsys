<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

// V2__notificacao_tipo_canal_link.sql 1:1 + `create notificacao_historico`
// (espelho da ativa + revisão do Admin, per docs/07 §4 e architecture V2).
return new class extends Migration
{
    public function up(): void
    {
        Schema::table('notification.notificacao', function (Blueprint $table): void {
            $table->string('tipo', 40)->nullable();
            $table->string('canal', 20)->nullable();
            $table->string('link', 2000)->nullable();
            $table->index(['id_usuario', 'tipo'], 'ix_notificacao_usuario_tipo');
        });

        Schema::create('notification.notificacao_historico', function (Blueprint $table): void {
            $table->id('id_historico');
            $table->unsignedBigInteger('id_notificacao_original');
            $table->unsignedBigInteger('id_usuario');
            $table->string('titulo', 160);
            $table->text('mensagem')->nullable();
            $table->string('tipo', 40)->nullable();
            $table->string('canal', 20)->nullable();
            $table->string('link', 2000)->nullable();
            $table->boolean('lida')->default(false);
            $table->timestampTz('criada_em');
            $table->string('status', 20)->default('ENVIADA');
            $table->timestampTz('data_envio')->nullable();
            $table->timestampTz('data_leitura')->nullable();
            $table->bigInteger('id_referencia')->nullable();
            $table->timestampTz('data_revisao_admin');
            $table->bigInteger('id_admin_revisor');
            $table->index('data_revisao_admin', 'ix_notificacao_historico_revisao');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('notification.notificacao_historico');
        Schema::table('notification.notificacao', function (Blueprint $table): void {
            $table->dropIndex('ix_notificacao_usuario_tipo');
            $table->dropColumn(['tipo', 'canal', 'link']);
        });
    }
};
