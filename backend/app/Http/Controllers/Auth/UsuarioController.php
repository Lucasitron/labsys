<?php

namespace App\Http\Controllers\Auth;

use App\Http\Controllers\Controller;
use App\Http\Requests\Auth\AlterarStatusRequest;
use App\Http\Requests\Auth\AtualizarUsuarioRequest;
use App\Http\Requests\Auth\CriarUsuarioRequest;
use App\Http\Resources\Auth\UsuarioResource;
use App\Models\Auth\Login;
use App\Services\Auth\UsuarioService;
use Illuminate\Contracts\Pagination\LengthAwarePaginator;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/** Gestão de contas de acesso (Admin-only) — só HTTP. */
class UsuarioController extends Controller
{
    public function __construct(private UsuarioService $usuarios) {}

    public function index(Request $request): JsonResponse
    {
        $niveis = $request->query('nivel');
        $situacoes = $request->query('situacao');

        // O front pagina de 0; o Laravel de 1.
        $page = max(1, (int) $request->query('page', 0) + 1);
        $perPage = max(1, (int) $request->query('size', 20));

        $result = $this->usuarios->listar(
            search: $request->query('search'),
            niveis: $niveis === null ? null : array_map('intval', (array) $niveis),
            situacoes: $situacoes === null ? null : (array) $situacoes,
            page: $page,
            perPage: $perPage,
        );

        /** @var LengthAwarePaginator<int, Login> $paginator */
        $paginator = $result['data'];

        $data = $paginator->getCollection()->map(
            fn (Login $login) => (new UsuarioResource($login, $this->usuarios->roleOf($login)))->toArray($request)
        )->all();

        return response()->json([
            'content' => $data,
            'page' => $paginator->currentPage() - 1,
            'size' => $paginator->perPage(),
            'total' => $paginator->total(),
            'totalPages' => $paginator->lastPage(),
            'contagens' => $result['contagens'],
        ]);
    }

    public function store(CriarUsuarioRequest $request): JsonResponse
    {
        $login = $this->usuarios->criar($request->only([
            'idUser', 'email', 'nomeUsuario', 'senha', 'uuid', 'setor', 'nivel', 'situacao',
        ]));

        return response()->json(
            (new UsuarioResource($login, $this->usuarios->roleOf($login)))->toArray($request),
            201
        );
    }

    public function update(AtualizarUsuarioRequest $request, int $id): JsonResponse
    {
        $login = $this->usuarios->atualizar($id, $request->only([
            'email', 'nomeUsuario', 'setor', 'nivel', 'situacao',
        ]));

        return response()->json(
            (new UsuarioResource($login, $this->usuarios->roleOf($login)))->toArray($request)
        );
    }

    public function alterarStatus(AlterarStatusRequest $request, int $id): JsonResponse
    {
        $login = $this->usuarios->alterarStatus($id, $request->input('situacao'));

        return response()->json(
            (new UsuarioResource($login, $this->usuarios->roleOf($login)))->toArray($request)
        );
    }
}
