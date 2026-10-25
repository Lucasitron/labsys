<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

// V2__create_user_permissions_table.sql — RBAC (papel 0-4). Sem "responsabilidade"
// (conceito do RH, não do Auth).
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('auth.user_permissions', function (Blueprint $table): void {
            $table->id();
            $table->bigInteger('id_user');
            $table->integer('role');
            $table->boolean('active')->default(true);
            $table->index('id_user', 'ix_user_permissions_id_user');
        });

        DB::statement('ALTER TABLE auth.user_permissions ADD CONSTRAINT ck_user_permissions_role CHECK (role BETWEEN 0 AND 4)');
    }

    public function down(): void
    {
        Schema::dropIfExists('auth.user_permissions');
    }
};
