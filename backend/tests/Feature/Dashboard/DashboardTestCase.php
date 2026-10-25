<?php

namespace Tests\Feature\Dashboard;

use App\Modules\Auth\Enums\Role;
use App\Modules\Auth\Models\Login;
use App\Modules\Estoque\Enums\Categoria;
use App\Modules\Estoque\Enums\StatusEmprestimo;
use App\Modules\Estoque\Models\Emprestimo;
use App\Modules\Estoque\Models\Item;
use App\Modules\Notification\Models\Notificacao;
use App\Modules\Producao\Enums\KanbanStatus;
use App\Modules\Producao\Enums\MaquinaStatus;
use App\Modules\Producao\Models\EncomendaKanban;
use App\Modules\Producao\Models\Maquina;
use App\Modules\Producao\Models\Projeto;
use App\Modules\Producao\Models\Tarefa;
use App\Modules\Rh\Models\CertificadoEmitido;
use App\Modules\Rh\Models\Funcionario;
use App\Modules\Rh\Models\SolicitacaoCertificado;
use Illuminate\Support\Str;
use Tests\TestCase;

/** Base dos testes do Dashboard: papéis + fábricas mínimas dos 5 domínios agregados. */
abstract class DashboardTestCase extends TestCase
{
    /** @return array<string, string> */
    protected function headersPapel(Role $role): array
    {
        $login = $this->criarLogin();
        $this->comPermissao($login, $role);

        return $this->authHeader($login, $role);
    }

    /** @return array{login:Login,headers:array<string,string>,funcionario:Funcionario} */
    protected function funcionarioHeaders(Role $role = Role::BOLSISTA): array
    {
        $func = $this->criarFuncionario(null, $role);

        return ['login' => $func['login'], 'headers' => $this->authHeader($func['login'], $role), 'funcionario' => $func['funcionario']];
    }

    /** @return array{login:Login,headers:array<string,string>,funcionario:Funcionario} */
    protected function adminHeaders(): array
    {
        $func = $this->criarFuncionario(null, Role::ADMIN);

        return ['login' => $func['login'], 'headers' => $this->authHeader($func['login'], Role::ADMIN), 'funcionario' => $func['funcionario']];
    }

    protected function criarProjeto(int $idResponsavel, array $over = []): Projeto
    {
        return Projeto::create(array_merge([
            'nome' => 'Mesa CNC',
            'descricao' => 'Construir mesa',
            'data_inicio' => today()->toDateString(),
            'status' => 'PLANEJADO',
            'id_responsavel' => $idResponsavel,
        ], $over));
    }

    protected function criarTarefa(int $idProjeto, int $idResponsavel, array $over = []): Tarefa
    {
        return Tarefa::create(array_merge([
            'id_projeto' => $idProjeto,
            'titulo' => 'Cortar MDF',
            'id_responsavel' => $idResponsavel,
            'data_fim_prevista' => today()->toDateString(),
            'status' => 'PENDENTE',
            'prioridade' => 'MEDIA',
        ], $over));
    }

    protected function criarCartao(int $idEncomenda, KanbanStatus $status = KanbanStatus::FILA): EncomendaKanban
    {
        return EncomendaKanban::create([
            'id_encomenda' => $idEncomenda,
            'status' => $status->value,
            'data_entrada_status' => now()->toDateTimeString(),
            'ordem' => 0,
            'version' => 0,
        ]);
    }

    protected function criarMaquina(MaquinaStatus $status = MaquinaStatus::DISPONIVEL): Maquina
    {
        $this->seq++;

        return Maquina::create(['nome' => "Laser {$this->seq}", 'status' => $status]);
    }

    protected function criarEmprestimoAberto(): Emprestimo
    {
        $item = Item::create([
            'nome' => 'Furadeira',
            'categoria' => Categoria::FERRAMENTA,
            'unidade_medida' => 'UN',
            'quantidade_atual' => '10.00',
            'estoque_minimo' => '1.00',
            'versao' => 0,
        ]);

        return Emprestimo::create([
            'id_item' => $item->getKey(),
            'id_pessoa' => 4242,
            'quantidade' => '1.00',
            'data_emprestimo' => today()->toDateString(),
            'data_devolucao_prevista' => today()->addWeek()->toDateString(),
            'status' => StatusEmprestimo::ATIVO->value,
        ]);
    }

    protected function criarSolicitacao(int $idFuncionario, string $status = 'PENDENTE'): SolicitacaoCertificado
    {
        return SolicitacaoCertificado::create([
            'id_funcionario' => $idFuncionario,
            'tipo_certificado' => 'EXTENSAO',
            'data_solicitacao' => now()->toDateTimeString(),
            'horas_solicitadas' => '10.00',
            'status' => $status,
        ]);
    }

    protected function criarEmitido(SolicitacaoCertificado $solicitacao): CertificadoEmitido
    {
        return CertificadoEmitido::create([
            'id_solicitacao' => $solicitacao->getKey(),
            'id_funcionario' => $solicitacao->id_funcionario,
            'tipo_certificado' => 'EXTENSAO',
            'horas_certificadas' => '10.00',
            'data_emissao' => now()->toDateString(),
            'codigo_verificacao' => (string) Str::uuid(),
        ]);
    }

    protected function criarNotificacaoNaoLida(int $idUsuario): Notificacao
    {
        return Notificacao::create([
            'id_usuario' => $idUsuario,
            'titulo' => 'Lembrete',
            'tipo' => 'pessoas',
            'canal' => 'inapp',
            'lida' => false,
            'criada_em' => now()->toDateTimeString(),
        ]);
    }
}
