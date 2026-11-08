<?php

namespace Tests\Feature;

use App\Events\CompraSolicitadaEvent;
use App\Events\EncomendaCriadaEvent;
use App\Events\NivelAlteradoEvent;
use App\Events\ProducaoConcluidaEvent;
use App\Events\ProducaoStatusAlteradoEvent;
use App\Events\RfidAccessEvent;
use Illuminate\Support\Facades\Event;
use Tests\TestCase;

class EventRegistrationTest extends TestCase
{
    public function test_nao_ha_listeners_duplicados(): void
    {
        $raw = Event::getRawListeners();

        foreach ($raw as $event => $listeners) {
            $keys = array_map(static function ($listener): string {
                if (is_array($listener)) {
                    return implode('@', $listener);
                }

                if ($listener instanceof \Closure) {
                    return 'Closure#'.spl_object_id($listener);
                }

                return (string) $listener;
            }, $listeners);

            $this->assertSame(
                count($keys),
                count(array_unique($keys)),
                "Listener duplicado para {$event}"
            );
        }
    }

    public function test_contagens_minimas_de_listeners(): void
    {
        $raw = Event::getRawListeners();

        $this->assertCount(1, $raw[RfidAccessEvent::class] ?? []);
        $this->assertCount(3, $raw[ProducaoConcluidaEvent::class] ?? []);
        $this->assertCount(2, $raw[ProducaoStatusAlteradoEvent::class] ?? []);
        $this->assertCount(2, $raw[CompraSolicitadaEvent::class] ?? []);
        $this->assertCount(2, $raw[NivelAlteradoEvent::class] ?? []);
        $this->assertCount(3, $raw[EncomendaCriadaEvent::class] ?? []);
    }
}
