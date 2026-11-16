# Pessoas & RH — Frontend Requirements

## Routes
- `/(app)/pessoas` (list) + `[id]` (detail com tabs) + `/novo` + `[id]/editar`
- `/(app)/pessoas/processo-seletivo` (Kanban por GRUPOS)
- `/(app)/pessoas/registro-horas`
- `/(app)/pessoas/treinamentos` + `[id]`
- `/(app)/pessoas/certificados` + `[id]` (Dívida 4 — Certificados no PJ&RH)
- `/(app)/pessoas/niveis` (Admin only)

> **Dívida 6 — Tutor × Instrutor (§5.1, definitivo):** Tutor(a) acompanha **grupo/processo-seletivo**; Instrutor(a) **ministra o treinamento**. No spec do front: processo-seletivo/grupos = `Tutor`; treinamentos/avaliação = `Instrutor`. Uma pessoa pode ser ambos, mas os papéis não se sobrepõem.

## Access
- View: Admin, Bolsista, Voluntário; Estagiário/Recrutando **próprio**
- Edit: Admin; Bolsista/Voluntário se **responsável**
- `pessoas/niveis` → Admin only (guard + hide menu)

## Data Requirements
- `GET /api/rh/pessoas?search=&setor=&nivel=&status=&page=`
- `GET /api/rh/pessoas/{id}`
- `POST /api/rh/pessoas` (+ PUT)
- `POST /api/rh/pessoas` → kanban grupos / processo-seletivo: `POST /api/rh/processo-seletivo/grupos` (+ PATCH `/{id}/estagio`, POST `/{id}/membros/{pessoaId}/avaliar`)
- Registro horas: `GET /api/rh/horas?status=&periodo=` (+ POST, PATCH `/{id}/validar|rejeitar`)
- Treinamentos: `GET /api/rh/treinamentos` (+ `/{id}/sessao`, POST `/{id}/avaliar`)
- **Certificados (Dívida 4, v0.2.0 backend):** `POST /api/rh/certificados/solicitar` · `GET /api/rh/certificados/solicitacoes` · `PUT /{id}/aprovar|rejeitar` · `GET /api/rh/certificados/emitidos[/{id}]` — ver `docs/mokups/rh/requisitos.md` quando o mockup de RH/Certificado for gerado pelo Designer (próxima iteração)
- **Horas (nova modelagem, Dívida 4):** `GET /api/rh/horas/disponiveis` + apontamentos `GET/POST /api/rh/apontamentos-horas` (+ `PUT /{id}/validar`) — **extrato mensal servido pelo backend** (`ExtratoMensalHorasEvent`), nunca somado no client

## RBAC
- Kanban: ações escondidas se não responsável pelo grupo
- Registro de horas: Estagiário vê só o próprio
- `hide`, nunca disable

## States
- Loading, Success, Empty, Empty(filtrado), Error, RBAC-restricted
- Kanban: Loading, Success, Empty(kanban vazio), Error

## URL State
- Lista: `?search=&setor=&nivel=&status=&page=`
- Registro horas: `?status=&periodo=`
- Detail: `?tab=`
- Processo seletivo: `?estagio=`

## Mockup
- `docs/mokups/pessoas/`
