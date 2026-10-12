<?php

namespace App\Modules\Rh\Http\Controllers;

use App\Modules\Auth\Models\Login;
use App\Modules\Rh\Enums\NivelAcesso;
use App\Modules\Rh\Enums\PessoaStatus;
use App\Modules\Rh\Http\Requests\PessoaRequest;
use App\Modules\Rh\Http\Resources\AvaliacaoResource;
use App\Modules\Rh\Http\Resources\PessoaResource;
use App\Modules\Rh\RhPrincipal;
use App\Modules\Rh\Services\PessoaService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/** Pessoas — só HTTP: FormRequest valida, service decide, Resource dá o shape. */
class PessoaController
{
    public function __construct(private PessoaService $pessoas) {}

    public function store(PessoaRequest $request): JsonResponse
    {
        $pessoa = $this->pessoas->cadastrar(
            $this->dados($request),
            $this->vinculo($request),
            $this->acesso($request),
        );

        return response()->json((new PessoaResource($pessoa))->toArray($request), 201);
    }

    public function index(Request $request): JsonResponse
    {
        $filtros = $request->validate([
            'search' => ['nullable', 'string', 'max:255'],
            'setor' => ['nullable', 'string', 'max:255'],
            'nivel' => ['nullable'],
            'status' => ['nullable'],
            'page' => ['nullable', 'integer', 'min:1'],
            'pageSize' => ['nullable', 'integer', 'min:1', 'max:100'],
        ]);

        $principal = $this->principal($request);
        $pagina = $this->pessoas->listar(
            $filtros['search'] ?? null,
            $filtros['setor'] ?? null,
            isset($filtros['nivel']) ? NivelAcesso::parse($filtros['nivel']) : null,
            isset($filtros['status']) ? PessoaStatus::parse($filtros['status']) : null,
            (int) ($filtros['page'] ?? 1),
            (int) ($filtros['pageSize'] ?? 10),
            $principal,
        );

        return response()->json([
            'pessoas' => $pagina->getCollection()->map(
                fn ($p) => (new PessoaResource($p))->toArray($request)
            )->all(),
            'paginacao' => [
                'page' => $pagina->currentPage(), 'pageSize' => $pagina->perPage(),
                'total' => $pagina->total(), 'totalPages' => $pagina->lastPage(),
            ],
            'filtros' => $this->pessoas->facetas($filtros['search'] ?? null),
        ]);
    }

    public function show(Request $request, int $id): PessoaResource
    {
        return new PessoaResource($this->pessoas->buscar($id, $this->principal($request)));
    }

    public function detalhe(Request $request, int $id): JsonResponse
    {
        $dados = $this->pessoas->detalhe($id, $this->principal($request));

        return response()->json([
            'pessoa' => (new PessoaResource($dados['pessoa']))->toArray($request),
            'idFuncionario' => $dados['idFuncionario'],
            'nivel' => $dados['nivel'],
            'departamento' => $dados['departamento'],
            'horasMes' => $dados['horasMes'],
            'treinamentos' => $dados['treinamentos'],
            'pendencias' => $dados['pendencias'],
            'horasPorStatus' => $dados['horasPorStatus'],
            'avaliacoes' => collect($dados['avaliacoes'])->map(
                fn ($a) => (new AvaliacaoResource($a))->toArray($request)
            )->all(),
            'historicoNivel' => $dados['historicoNivel'],
        ]);
    }

    public function update(PessoaRequest $request, int $id): JsonResponse
    {
        $pessoa = $this->pessoas->atualizar($id, $this->dados($request), $this->principal($request));

        return response()->json((new PessoaResource($pessoa))->toArray($request));
    }

    public function destroy(int $id): JsonResponse
    {
        $this->pessoas->excluir($id);

        return response()->json(['excluido' => true, 'id' => $id]);
    }

    private function dados(PessoaRequest $request): array
    {
        return [
            'nome_completo' => $request->input('nomeCompleto'),
            'matricula' => $request->input('matricula'),
            'data_admissao' => $request->input('dataAdmissao'),
            'contato' => $request->input('contato'),
            'turno' => $request->input('turno'),
            'status' => $request->input('status') !== null ? PessoaStatus::from((int) $request->input('status')) : null,
            'cpf' => $request->input('cpf'),
        ];
    }

    /** @return array{nivel_acesso?:?NivelAcesso,departamento?:?string} */
    private function vinculo(PessoaRequest $request): array
    {
        $nivelRaw = $request->input('nivelAcesso');

        return array_filter([
            'nivel_acesso' => $nivelRaw !== null ? NivelAcesso::from((int) $nivelRaw) : null,
            'departamento' => $request->input('departamento'),
        ], fn ($valor) => $valor !== null);
    }

    /** @return array{criar?:bool,email?:?string,senha?:?string,nome_usuario?:?string,setor?:?string} */
    private function acesso(PessoaRequest $request): array
    {
        if (! $request->boolean('criarAcesso')) {
            return [];
        }

        return [
            'criar' => true,
            'email' => $request->input('email'),
            'senha' => $request->input('senha'),
            'nome_usuario' => $request->input('nomeUsuario'),
            'setor' => $request->input('setor'),
        ];
    }

    private function principal(Request $request): RhPrincipal
    {
        /** @var Login $login */
        $login = $request->user();

        return RhPrincipal::from($login);
    }
}
