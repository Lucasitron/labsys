<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

// V17__create_parametro_5s.sql — tabela + inserts do upstream (viram seed nesta
// migration, padrão Auth V6/V7: sem seeder inventada, P15 verificação OK).
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('producao.parametro_5s', function (Blueprint $table): void {
            $table->id('id_parametro');
            $table->string('chave', 100)->unique();
            $table->string('valor', 255);
            $table->string('descricao', 500)->nullable();
        });

        DB::table('producao.parametro_5s')->insert([
            ['chave' => 'diasParaAuditoriaProjeto', 'valor' => '15', 'descricao' => 'Dias sem evolução para um projeto de mesa entrar em auditoria'],
            ['chave' => 'diaSemanaInspecao', 'valor' => 'SEXTA', 'descricao' => 'Dia da semana em que as inspeções 5S devem ocorrer'],
            ['chave' => 'periodoExperimentalAtivo', 'valor' => 'true', 'descricao' => 'Indica se o período experimental de penalidades está ativo'],
            ['chave' => 'rotacaoDias', 'valor' => '7', 'descricao' => 'Intervalo em dias para rotação de responsáveis pelos setores'],
        ]);
    }

    public function down(): void
    {
        Schema::dropIfExists('producao.parametro_5s');
    }
};
