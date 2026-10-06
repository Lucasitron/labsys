<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

// V1__create_login_table.sql — credenciais e identidade do usuário.
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('auth.login', function (Blueprint $table): void {
            $table->id();
            $table->bigInteger('id_user');
            $table->string('uuid')->unique('uk_login_uuid');
            $table->string('email')->unique('uk_login_email');
            $table->string('nome_usuario')->unique('uk_login_nome_usuario');
            $table->string('senha_hash');
            $table->string('setor')->nullable();
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('auth.login');
    }
};
