<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

// V2__tag_cliente.sql — tags de segmentação de marketing.
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('vendas.tag_cliente', function (Blueprint $table): void {
            $table->id('id_tag');
            $table->string('nome', 64)->unique('uk_tag_nome');
            $table->string('cor', 16)->nullable();
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('vendas.tag_cliente');
    }
};
