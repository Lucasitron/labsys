<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

// V10__certificado_horas.sql — consolidação + certificado_emitido/hora_consolidada.
return new class extends Migration
{
    public function up(): void
    {
        Schema::table('rh.apontamento_horas', function (Blueprint $table): void {
            $table->boolean('consolidado')->default(false);
        });

        Schema::create('rh.solicitacao_certificado', function (Blueprint $table): void {
            $table->id();
            $table->bigInteger('id_funcionario');
            $table->string('tipo_certificado', 32);
            $table->timestampTz('data_solicitacao');
            $table->decimal('horas_solicitadas', 7, 2);
            $table->string('status', 32);
            $table->bigInteger('id_admin_aprovador')->nullable();
            $table->timestampTz('data_decisao')->nullable();
            $table->text('observacao')->nullable();

            $table->foreign('id_funcionario', 'fk_solicitacao_funcionario')
                ->references('id')->on('rh.funcionario');
            $table->foreign('id_admin_aprovador', 'fk_solicitacao_aprovador')
                ->references('id')->on('rh.funcionario');
            $table->index('id_funcionario', 'ix_solicitacao_funcionario');
        });

        Schema::create('rh.certificado_emitido', function (Blueprint $table): void {
            $table->id();
            $table->bigInteger('id_solicitacao')->unique('uk_certificado_solicitacao');
            $table->bigInteger('id_funcionario');
            $table->string('tipo_certificado', 32);
            $table->decimal('horas_certificadas', 7, 2);
            $table->timestampTz('data_emissao');
            $table->uuid('codigo_verificacao')->unique('uk_certificado_codigo');

            $table->foreign('id_solicitacao', 'fk_certificado_solicitacao')
                ->references('id')->on('rh.solicitacao_certificado');
            $table->foreign('id_funcionario', 'fk_certificado_funcionario')
                ->references('id')->on('rh.funcionario');
            $table->index('id_funcionario', 'ix_certificado_funcionario');
        });

        Schema::create('rh.hora_consolidada', function (Blueprint $table): void {
            $table->id();
            $table->bigInteger('id_certificado');
            $table->bigInteger('id_apontamento');
            $table->decimal('horas', 5, 2);
            $table->timestampTz('data_consolidacao');

            $table->foreign('id_certificado', 'fk_consolidada_certificado')
                ->references('id')->on('rh.certificado_emitido');
            $table->foreign('id_apontamento', 'fk_consolidada_apontamento')
                ->references('id')->on('rh.apontamento_horas');
            $table->index('id_certificado', 'ix_hora_consolidada_certificado');
        });

        DB::statement("ALTER TABLE rh.solicitacao_certificado ADD CONSTRAINT ck_solicitacao_tipo CHECK (tipo_certificado IN ('EXTENSAO', 'COMPLEMENTAR', 'ESTAGIO'))");
        DB::statement("ALTER TABLE rh.solicitacao_certificado ADD CONSTRAINT ck_solicitacao_status CHECK (status IN ('PENDENTE', 'APROVADO', 'REJEITADO'))");
        DB::statement("ALTER TABLE rh.certificado_emitido ADD CONSTRAINT ck_certificado_tipo CHECK (tipo_certificado IN ('EXTENSAO', 'COMPLEMENTAR', 'ESTAGIO'))");
    }

    public function down(): void
    {
        Schema::dropIfExists('rh.hora_consolidada');
        Schema::dropIfExists('rh.certificado_emitido');
        Schema::dropIfExists('rh.solicitacao_certificado');
        Schema::table('rh.apontamento_horas', function (Blueprint $table): void {
            $table->dropColumn('consolidado');
        });
    }
};
