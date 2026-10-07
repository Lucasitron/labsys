#!/bin/sh
# Boot do monólito FabLab: espera o Postgres, migra, aquece caches e
# entrega para o CMD (serve | queue:work | loop schedule:run).
# Roda em app/worker/scheduler (migrate é idempotente; sem pendência = no-op).
set -eu

DB_HOST="${DB_HOST:-postgres}"
DB_PORT="${DB_PORT:-5432}"
: "${DB_PASSWORD:?DB_PASSWORD é obrigatório no ambiente do container}"
: "${APP_KEY:?APP_KEY é obrigatório no ambiente do container}"

echo "[entrypoint] aguardando postgres em ${DB_HOST}:${DB_PORT}..."
export WAIT_HOST="$DB_HOST" WAIT_PORT="$DB_PORT"
i=0
until php -r '$c = @fsockopen(getenv("WAIT_HOST"), (int) getenv("WAIT_PORT"), $e, $s, 2); if (!$c) exit(1); fclose($c);'; do
  i=$((i + 1))
  if [ "$i" -ge 30 ]; then
    echo "[entrypoint] postgres inacessível após ~60s em ${DB_HOST}:${DB_PORT}" >&2
    exit 1
  fi
  sleep 2
done

echo "[entrypoint] php artisan migrate --force"
php artisan migrate --force

echo "[entrypoint] caches de config/rotas"
php artisan config:cache
php artisan route:cache

# Sinaliza workers antigos (pós-deploy) — no-op se não houver nenhum.
php artisan queue:restart || true

exec "$@"
