<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Support\Facades\DB;

// Schema do módulo Producao (idempotente, padrão M1).
return new class extends Migration
{
    public function up(): void
    {
        DB::statement('CREATE SCHEMA IF NOT EXISTS producao');
    }

    public function down(): void
    {
        DB::statement('DROP SCHEMA IF EXISTS producao CASCADE');
    }
};
