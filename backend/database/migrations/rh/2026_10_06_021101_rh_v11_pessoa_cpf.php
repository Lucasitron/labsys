<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

// V11__pessoa_cpf.sql — CPF opcional + UK (LGPD: API sempre responde mascarado).
return new class extends Migration
{
    public function up(): void
    {
        Schema::table('rh.pessoa', function (Blueprint $table): void {
            $table->string('cpf', 11)->nullable()->unique('uk_pessoa_cpf');
        });
    }

    public function down(): void
    {
        Schema::table('rh.pessoa', function (Blueprint $table): void {
            $table->dropUnique('uk_pessoa_cpf');
            $table->dropColumn('cpf');
        });
    }
};
