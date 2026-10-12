# FabLab backend — imagem ÚNICA do monólito modular Laravel (API em :8080).
#
# DECISÃO serve vs php-fpm+nginx: `php artisan serve` (servidor embutido PHP,
# processo único no container). Motivos:
#   1. Simplicidade robusta — 1 processo = sem supervisor, sem config dupla;
#      a MESMA imagem roda em dev/testing/prod (prod = ghcr.io/fablab/backend:${TAG}).
#   2. TLS/proxy ficam na borda (nginx do host + Tailscale), não dentro da imagem.
#   3. Carga do MVP cabe no servidor embutido atrás de proxy reverso com
#      keep-alive; se a carga exigir, evoluir p/ estágio fpm+nginx dedicado.
# Boot (entrypoint): espera o Postgres, `migrate --force`, `config:cache`,
# `route:cache`, `queue:restart` e entrega p/ o CMD (serve | queue:work | scheduler).
# Segredos NUNCA entram na imagem: `.env` está no .dockerignore e tudo vem de env.
#
# Build local (dev/testing):
#   docker compose -f infra/docker-compose.yml -f infra/docker-compose.dev.yml up -d --build
# Publicação (CI):
#   docker build -t ghcr.io/fablab/backend:${TAG} backend/ && docker push ghcr.io/fablab/backend:${TAG}
FROM php:8.3-cli

ENV COMPOSER_ALLOW_SUPERUSER=1 \
    APP_URL=http://localhost:8080

# Extensões exigidas pelo Laravel 12 + pgsql (+ curl p/ o healthcheck /up).
RUN apt-get update && apt-get install -y --no-install-recommends \
        git unzip curl libpq-dev libicu-dev libzip-dev \
    && docker-php-ext-install -j"$(nproc)" pdo_pgsql intl zip bcmath pcntl opcache \
    && printf 'opcache.enable_cli=1\nopcache.memory_consumption=128\nopcache.max_accelerated_files=10000\nopcache.validate_timestamps=0\n' \
        > /usr/local/etc/php/conf.d/fablab-opcache.ini \
    && apt-get clean && rm -rf /var/lib/apt/lists/*

COPY --from=composer:2 /usr/bin/composer /usr/bin/composer

WORKDIR /app

# Camada de dependências primeiro (cache de build): .env/VENDOR fora via .dockerignore.
COPY composer.json composer.lock ./
RUN composer install --no-dev --no-scripts --no-autoloader --prefer-dist

COPY . ./
# Build-time precisa bootar o Laravel (post-autoload-dump → package:discover →
# AppServiceProvider::boot com fail-fast de JWT_SECRET >= 32 chars), mas no
# build NÃO há .env (correto: .dockerignore exclui) nem segredo real disponível.
# Forma escolhida: `VAR=...` prefixado inline no RUN (e NÃO `ENV`/`ARG→ENV`).
# Motivo: env inline vale só p/ esse RUN — não persiste no config da imagem
# (`docker inspect` limpo); runtime vem 100% do env do container. O manifest
# gerado aqui (bootstrap/cache/packages.php|services.php) só lista providers,
# sem segredo. O `rm` abaixo apaga eventual cache de config/rotas com dummies
# (o build nunca gera, é defesa); o entrypoint regenera config:cache +
# route:cache no boot com os segredos reais. NENHUM DB_* é injetado: o boot de
# build não conecta no Postgres (Gates são closures lazy; Event::listen não
# toca banco; migrate só roda no entrypoint, com rede de serviços).
RUN JWT_SECRET=dummy-build-only-00000000000000000000 \
    APP_KEY=base64:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA= \
    composer dump-autoload --optimize \
    && rm -f bootstrap/cache/config.php bootstrap/cache/routes-*.php \
    && chown -R www-data:www-data storage bootstrap/cache \
    && chmod -R ug+rw storage bootstrap/cache

COPY docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh
RUN chmod +x /usr/local/bin/docker-entrypoint.sh

USER www-data

EXPOSE 8080

ENTRYPOINT ["docker-entrypoint.sh"]
CMD ["php", "artisan", "serve", "--host=0.0.0.0", "--port=8080"]
