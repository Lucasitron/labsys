<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

// V1__cliente.sql — clientes PF/PJ (cpf_cnpj único).
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('vendas.cliente', function (Blueprint $table): void {
            $table->id('id_cliente');
            $table->string('tipo_pessoa', 8);
            $table->string('nome_razao_social', 255);
            $table->string('cpf_cnpj', 20)->unique('uk_cliente_cpf_cnpj');
            $table->string('email', 255)->nullable();
            $table->string('telefone', 32)->nullable();
            $table->string('endereco', 500)->nullable();
            $table->date('data_cadastro');
            $table->bigInteger('criado_por');

            $table->index('nome_razao_social', 'idx_cliente_nome');
            $table->index('tipo_pessoa', 'idx_cliente_tipo');
        });

        DB::statement("ALTER TABLE vendas.cliente ADD CONSTRAINT ck_cliente_tipo_pessoa CHECK (tipo_pessoa IN ('PF', 'PJ'))");
    }

    public function down(): void
    {
        Schema::dropIfExists('vendas.cliente');
    }
};
