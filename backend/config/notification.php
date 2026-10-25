<?php

// Notification: schedulers com kill-switch (padrão M3/E10). SMTP real vem do
// `.env` (`MAIL_*`, nunca versionado); sem SMTP, o e-mail falha fail-soft
// (row `PENDENTE` p/ retry) em vez de estourar o job.
return [
    'reenvio_scheduler_enabled' => env('NOTIFICATION_REENVIO_SCHEDULER_ENABLED', true),
    'expurgo_scheduler_enabled' => env('NOTIFICATION_EXPURGO_SCHEDULER_ENABLED', true),
];
