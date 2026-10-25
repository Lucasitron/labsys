<?php

namespace App\Modules\Producao\Http\Controllers;

use App\Modules\Auth\Models\Login;
use App\Modules\Producao\Http\Requests\ChecklistRequest;
use App\Modules\Producao\Http\Requests\MaterialRequest;
use App\Modules\Producao\Http\Requests\ResponsavelRequest;
use App\Modules\Producao\Http\Requests\SetorRequest;
use App\Modules\Producao\Http\Requests\SinalizacaoRequest;
use App\Modules\Producao\Http\Resources\ChecklistResource;
use App\Modules\Producao\Http\Resources\MaterialResource;
use App\Modules\Producao\Http\Resources\ResponsavelResource;
use App\Modules\Producao\Http\Resources\SetorResource;
use App\Modules\Producao\Http\Resources\SinalizacaoResource;
use App\Modules\Producao\ProducaoPrincipal;
use App\Modules\Producao\Services\SetorService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

/** Setores 5S — só HTTP (rotação e vínculo no service/policy). */
class SetorController
{
    public function __construct(private SetorService $setores) {}

    public function store(SetorRequest $request): JsonResponse
    {
        $setor = $this->setores->criar($request->validated(), $this->principal($request));

        return response()->json(new SetorResource($setor), 201);
    }

    public function index(Request $request): JsonResponse
    {
        $filtros = $request->validate([
            'ativo' => ['nullable', 'boolean'],
        ]);

        $setores = $this->setores->listar(
            array_key_exists('ativo', $filtros) ? (bool) $filtros['ativo'] : null,
            $this->principal($request),
        );

        return response()->json(SetorResource::collection($setores));
    }

    public function show(Request $request, int $id): JsonResponse
    {
        return response()->json(new SetorResource($this->setores->detalhar($id, $this->principal($request))));
    }

    public function update(SetorRequest $request, int $id): JsonResponse
    {
        $setor = $this->setores->atualizar($id, $request->validated(), $this->principal($request));

        return response()->json(new SetorResource($setor));
    }

    public function destroy(Request $request, int $id): JsonResponse
    {
        $this->setores->remover($id, $this->principal($request));

        return response()->json(null, 204);
    }

    public function adicionarMaterial(MaterialRequest $request, int $id): JsonResponse
    {
        $material = $this->setores->adicionarMaterial($id, $request->validated(), $this->principal($request));

        return response()->json(new MaterialResource($material), 201);
    }

    public function removerMaterial(Request $request, int $id, int $idMaterial): JsonResponse
    {
        $this->setores->removerMaterial($id, $idMaterial, $this->principal($request));

        return response()->json(null, 204);
    }

    public function adicionarSinalizacao(SinalizacaoRequest $request, int $id): JsonResponse
    {
        $sinalizacao = $this->setores->adicionarSinalizacao($id, $request->validated(), $this->principal($request));

        return response()->json(new SinalizacaoResource($sinalizacao), 201);
    }

    public function removerSinalizacao(Request $request, int $id, int $idSinalizacao): JsonResponse
    {
        $this->setores->removerSinalizacao($id, $idSinalizacao, $this->principal($request));

        return response()->json(null, 204);
    }

    public function adicionarChecklist(ChecklistRequest $request, int $id): JsonResponse
    {
        $item = $this->setores->adicionarChecklist($id, $request->validated(), $this->principal($request));

        return response()->json(new ChecklistResource($item), 201);
    }

    public function atualizarChecklist(ChecklistRequest $request, int $id, int $idItem): JsonResponse
    {
        $item = $this->setores->atualizarChecklist($id, $idItem, $request->validated(), $this->principal($request));

        return response()->json(new ChecklistResource($item));
    }

    public function removerChecklist(Request $request, int $id, int $idItem): JsonResponse
    {
        $this->setores->removerChecklist($id, $idItem, $this->principal($request));

        return response()->json(null, 204);
    }

    public function adicionarResponsavel(ResponsavelRequest $request, int $id): JsonResponse
    {
        $responsavel = $this->setores->adicionarResponsavel($id, $request->validated(), $this->principal($request));

        return response()->json(new ResponsavelResource($responsavel), 201);
    }

    public function listarResponsaveis(Request $request, int $id): JsonResponse
    {
        $responsaveis = $this->setores->listarResponsaveis($id, $this->principal($request));

        return response()->json(ResponsavelResource::collection($responsaveis));
    }

    public function removerResponsavel(Request $request, int $id, int $idResponsavel): JsonResponse
    {
        $this->setores->removerResponsavel($id, $idResponsavel, $this->principal($request));

        return response()->json(null, 204);
    }

    private function principal(Request $request): ProducaoPrincipal
    {
        /** @var Login $login */
        $login = $request->user();

        return ProducaoPrincipal::from($login);
    }
}
