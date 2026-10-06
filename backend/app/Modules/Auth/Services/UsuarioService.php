<?php

namespace App\Modules\Auth\Services;

use App\Modules\Auth\Enums\Role;
use App\Modules\Auth\Enums\SituacaoUsuario;
use App\Modules\Auth\Models\Login;
use App\Modules\Auth\Models\UserPermission;
use App\Shared\Exceptions\ResourceNotFoundException;
use Illuminate\Contracts\Pagination\LengthAwarePaginator;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Str;

/**
 * Gestão de contas de acesso (opera conta/nível/situação sobre login +
 * user_permissions). Identidade física é do RH — aqui só o acesso.
 */
class UsuarioService
{
    /**
     * @param  list<int>|null  $niveis
     * @param  list<string>|null  $situacoes
     * @return array{data:LengthAwarePaginator<int,Login>,contagens:array{total:int,ativos:int,pendentes:int,desativados:int,porNivel:array<string,int>}}
     */
    public function listar(?string $search = null, ?array $niveis = null, ?array $situacoes = null, int $page = 1, int $perPage = 20): array
    {
        $query = Login::query()->orderBy('id');

        if ($search !== null && trim($search) !== '') {
            $term = '%'.trim($search).'%';
            $query->where(fn ($q) => $q->where('email', 'ilike', $term)
                ->orWhere('nome_usuario', 'ilike', $term));
        }

        $logins = $query->get();

        $situacoesFiltro = [];
        foreach ($situacoes ?? [] as $raw) {
            try {
                $situacoesFiltro[] = SituacaoUsuario::parse($raw)->value;
            } catch (\InvalidArgumentException) {
            }
        }

        $filtradas = $logins->filter(function (Login $login) use ($niveis, $situacoesFiltro): bool {
            if ($situacoesFiltro !== [] && ! in_array($login->situacao->value, $situacoesFiltro, true)) {
                return false;
            }
            if ($niveis !== [] && $niveis !== null && ! in_array($this->roleOf($login)?->value, $niveis, true)) {
                return false;
            }

            return true;
        })->values();

        $total = $filtradas->count();
        $items = $filtradas->forPage($page, $perPage)->values();

        $porNivel = [];
        foreach (Role::cases() as $role) {
            $porNivel[$role->name] = $filtradas->filter(
                fn (Login $l) => $this->roleOf($l) === $role
            )->count();
        }

        $paginator = new \Illuminate\Pagination\LengthAwarePaginator(
            $items, $total, $perPage, $page, ['path' => request()->url(), 'query' => request()->query()]
        );

        return [
            'data' => $paginator,
            'contagens' => [
                'total' => $total,
                'ativos' => $filtradas->filter(fn (Login $l) => $l->situacao === SituacaoUsuario::ATIVO)->count(),
                'pendentes' => $filtradas->filter(fn (Login $l) => $l->situacao === SituacaoUsuario::PENDENTE)->count(),
                'desativados' => $filtradas->filter(fn (Login $l) => $l->situacao === SituacaoUsuario::DESATIVADO)->count(),
                'porNivel' => $porNivel,
            ],
        ];
    }

    /**
     * Cria conta + permissão (convite). Situação ausente = PENDENTE;
     * nível ausente = RECRUTANDO (4).
     *
     * @param  array{idUser:int,email:string,nomeUsuario:string,senha:string,uuid?:?string,setor?:?string,nivel?:?int,situacao?:?string}  $data
     */
    public function criar(array $data): Login
    {
        $email = trim($data['email']);
        $nomeUsuario = trim($data['nomeUsuario']);

        if (Login::where('email', $email)->exists()) {
            throw new \InvalidArgumentException('E-mail já cadastrado');
        }
        if (Login::where('nome_usuario', $nomeUsuario)->exists()) {
            throw new \InvalidArgumentException('Nome de usuário já cadastrado');
        }

        $role = $data['nivel'] ?? null;
        $role = $role === null ? Role::RECRUTANDO : Role::parse($role);
        $situacao = blank($data['situacao'] ?? null)
            ? SituacaoUsuario::PENDENTE
            : SituacaoUsuario::parse($data['situacao']);

        $login = Login::create([
            'id_user' => $data['idUser'],
            'uuid' => blank($data['uuid'] ?? null) ? (string) Str::uuid() : trim($data['uuid']),
            'email' => $email,
            'nome_usuario' => $nomeUsuario,
            'senha_hash' => Hash::make($data['senha']),
            'setor' => $data['setor'] ?? null,
            'situacao' => $situacao,
        ]);

        UserPermission::create([
            'id_user' => $login->id_user,
            'role' => $role,
            'active' => true,
        ]);

        return $login;
    }

    /**
     * Atualiza conta (e-mail, nome de usuário, setor, nível, situação).
     *
     * @param  array{email?:?string,nomeUsuario?:?string,setor?:?string,nivel?:?int,situacao?:?string}  $data
     */
    public function atualizar(int $id, array $data): Login
    {
        $login = Login::find($id);

        if ($login === null) {
            throw new ResourceNotFoundException('Usuário não encontrado');
        }

        if (! blank($data['email'] ?? null)) {
            $email = trim($data['email']);
            if (strcasecmp($email, $login->email) !== 0 && Login::where('email', $email)->exists()) {
                throw new \InvalidArgumentException('E-mail já cadastrado');
            }
            $login->email = $email;
        }

        if (! blank($data['nomeUsuario'] ?? null)) {
            $nome = trim($data['nomeUsuario']);
            if (strcasecmp($nome, $login->nome_usuario) !== 0 && Login::where('nome_usuario', $nome)->exists()) {
                throw new \InvalidArgumentException('Nome de usuário já cadastrado');
            }
            $login->nome_usuario = $nome;
        }

        if (array_key_exists('setor', $data)) {
            $login->setor = $data['setor'];
        }

        if (! blank($data['situacao'] ?? null)) {
            $login->situacao = SituacaoUsuario::parse($data['situacao']);
        }

        $login->save();

        if (array_key_exists('nivel', $data) && $data['nivel'] !== null) {
            $this->upsertRole($login->id_user, Role::parse($data['nivel']));
        }

        return $login->refresh();
    }

    public function alterarStatus(int $id, string $situacaoRaw): Login
    {
        $login = Login::find($id);

        if ($login === null) {
            throw new ResourceNotFoundException('Usuário não encontrado');
        }

        $login->situacao = SituacaoUsuario::parse($situacaoRaw);
        $login->save();

        return $login->refresh();
    }

    public function roleOf(Login $login): ?Role
    {
        $permission = UserPermission::where('id_user', $login->id_user)
            ->where('active', true)
            ->first();

        return $permission?->role;
    }

    private function upsertRole(int $idUser, Role $role): void
    {
        $permission = UserPermission::where('id_user', $idUser)->where('active', true)->first();

        if ($permission === null) {
            UserPermission::create(['id_user' => $idUser, 'role' => $role, 'active' => true]);

            return;
        }

        $permission->role = $role;
        $permission->save();
    }
}
