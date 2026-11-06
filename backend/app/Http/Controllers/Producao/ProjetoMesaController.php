<?php

namespace App\Http\Controllers\Producao;

use App\Http\Controllers\Controller;

use App\Models\Auth\Login;
use App\Enums\Producao\StatusProjetoMesa;
use App\Http\Requests\Producao\ProjetoMesaRequest;
use App\Http\Resources\Producao\AuditoriaResource;
use App\Http\Resources\Producao\ProjetoMesaResource;
use App\Shared\Auth\Caller;
use App\Services\Producao\ProjetoMesaService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/**
 * Projetos de mesa — só HTTP. O QR é servido como string
 * (`fablab://projeto-mesa/{id}`): o PNG (`chillerlan/php-qrcode`) é inviável
 * sem GD/imagick nas imagens PHP 8.3 disponíveis.
 */
class ProjetoMesaController extends Controller
{
    public function __construct(private ProjetoMesaService $mesas) {}

    public function store(ProjetoMesaRequest $request): JsonResponse
    {
        $mesa = $this->mesas->criar($request->validated(), $this->principal($request));

        return response()->json(new ProjetoMesaResource($mesa), 201);
    }

    public function index(Request $request): JsonResponse
    {
        $filtros = $request->validate([
            'status' => ['nullable', 'string', 'in:ATIVO,ABANDONADO,CONCLUIDO'],
        ]);

        $mesas = $this->mesas->listar(
            isset($filtros['status']) ? StatusProjetoMesa::from($filtros['status']) : null,
            $this->principal($request),
        );

        return response()->json(ProjetoMesaResource::collection($mesas));
    }

    public function show(Request $request, int $id): JsonResponse
    {
        return response()->json(new ProjetoMesaResource($this->mesas->detalhar($id, $this->principal($request))));
    }

    public function update(ProjetoMesaRequest $request, int $id): JsonResponse
    {
        $mesa = $this->mesas->atualizar($id, $request->validated(), $this->principal($request));

        return response()->json(new ProjetoMesaResource($mesa));
    }

    public function evolucao(Request $request, int $id): JsonResponse
    {
        $mesa = $this->mesas->registrarEvolucao($id, $this->principal($request));

        return response()->json(new ProjetoMesaResource($mesa));
    }

    public function qrcode(Request $request, int $id): JsonResponse
    {
        return response()->json(['conteudo' => $this->mesas->conteudoQrCode($id, $this->principal($request))]);
    }

    public function auditorias(Request $request, int $id): JsonResponse
    {
        $lista = $this->mesas->listarAuditorias($id, $this->principal($request));

        return response()->json(AuditoriaResource::collection($lista));
    }

    public function destroy(Request $request, int $id): JsonResponse
    {
        $this->mesas->remover($id, $this->principal($request));

        return response()->json(null, 204);
    }

}
