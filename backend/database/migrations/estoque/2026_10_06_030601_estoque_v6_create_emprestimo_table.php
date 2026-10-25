<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

// V6__create_emprestimo_table.sql — empréstimos de equipamentos/ferramentas.
return new class extends Migration
{
    public function up(): void
    {
        Schema::create('estoque.emprestimo', function (Blueprint $table): void {
            $table->id('id_emprestimo');
            $table->unsignedBigInteger('id_item');
            // Referência externa ao RH (id da pessoa): sem FK cross-schema (via RhContract).
            $table->bigInteger('id_pessoa');
            $table->decimal('quantidade', 12, 2);
            $table->date('data_emprestimo');
            $table->date('data_devolucao_prevista');
            $table->date('data_devolucao_real')->nullable();
            $table->string('status', 32);
            $table->string('observacao', 255)->nullable();

            $table->foreign('id_item', 'fk_emprestimo_item')
                ->references('id_item')->on('estoque.item');

            $table->index('id_item', 'idx_emprestimo_item');
            $table->index('status', 'idx_emprestimo_status');
        });

        DB::statement("ALTER TABLE estoque.emprestimo ADD CONSTRAINT ck_emprestimo_status CHECK (status IN ('ATIVO', 'DEVOLVIDO', 'ATRASADO'))");
    }

    public function down(): void
    {
        Schema::dropIfExists('estoque.emprestimo');
    }
};
