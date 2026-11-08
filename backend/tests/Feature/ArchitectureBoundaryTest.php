<?php

namespace Tests\Feature;

use PHPUnit\Framework\TestCase;

/**
 * Guardrails de fronteira entre módulos (remap fase 8).
 *
 * Sem banco: apenas inspeção do filesystem.
 */
class ArchitectureBoundaryTest extends TestCase
{
    private string $root;

    protected function setUp(): void
    {
        parent::setUp();
        $this->root = dirname(__DIR__, 2);
    }

    public function test_modules_do_not_import_other_modules_models_or_services(): void
    {
        $violations = [];

        foreach ($this->phpFiles($this->root.'/app') as $file) {
            $content = (string) file_get_contents($file);
            if (! preg_match('/namespace\s+([^;]+);/', $content, $namespaceMatch)) {
                continue;
            }

            $namespace = trim($namespaceMatch[1]);
            $module = $this->moduleOf($namespace, $file);
            if ($module === null) {
                continue;
            }

            preg_match_all('/^\s*use\s+(App\\\\(?:Models|Services)\\\\[^;]+);/m', $content, $importMatches);
            foreach ($importMatches[1] as $import) {
                if (! preg_match('#^App\\\\(Models|Services)\\\\([^\\\\]+)#', $import, $importModuleMatch)) {
                    continue;
                }

                $otherModule = $importModuleMatch[2];

                // Auth e a fronteira de identidade compartilhada; contratos ficam em
                // App\Contracts (nao sao Models/Services de modulo).
                if ($otherModule === 'Auth') {
                    continue;
                }

                if ($otherModule !== $module) {
                    $violations[] = sprintf(
                        '%s (modulo %s) importa %s',
                        $this->relative($file),
                        $module,
                        $import,
                    );
                }
            }
        }

        $this->assertSame([], $violations, implode("\n", $violations));
    }

    public function test_no_app_modules_reference_remains(): void
    {
        $needle = 'App'.'\\Modules';
        $violations = [];

        foreach (['app', 'routes', 'bootstrap', 'config', 'database', 'tests'] as $directory) {
            $path = $this->root.'/'.$directory;
            if (! is_dir($path)) {
                continue;
            }

            foreach ($this->allFiles($path) as $file) {
                $content = @file_get_contents($file);
                if ($content === false) {
                    continue;
                }

                if (str_contains($content, $needle)) {
                    $violations[] = $this->relative($file);
                }
            }
        }

        $this->assertSame([], $violations, implode("\n", $violations));
    }

    private function moduleOf(string $namespace, string $file): ?string
    {
        if (preg_match('#^App\\\\(Models|Services|Enums|Listeners)\\\\([^\\\\]+)#', $namespace, $match)) {
            return $match[2];
        }

        if (preg_match('#^App\\\\Http\\\\(Controllers|Requests|Resources)\\\\([^\\\\]+)#', $namespace, $match)) {
            return $match[2];
        }

        if ($namespace === 'App\\Policies' || str_starts_with($namespace, 'App\\Policies\\')) {
            $base = basename($file, '.php');
            if (preg_match('/^([A-Z][A-Za-z0-9]+)Policy$/', $base, $match)) {
                return $match[1];
            }
        }

        if (preg_match('#^App\\\\(Shared|Support)\\\\([^\\\\]+)#', $namespace, $match)) {
            return $match[2];
        }

        return null;
    }

    /**
     * @return list<string>
     */
    private function phpFiles(string $directory): array
    {
        if (! is_dir($directory)) {
            return [];
        }

        $files = [];
        $iterator = new \RecursiveIteratorIterator(
            new \RecursiveDirectoryIterator($directory, \FilesystemIterator::SKIP_DOTS)
        );

        foreach ($iterator as $file) {
            if ($file->isFile() && $file->getExtension() === 'php') {
                $files[] = $file->getPathname();
            }
        }

        return $files;
    }

    /**
     * @return list<string>
     */
    private function allFiles(string $directory): array
    {
        $files = [];
        $iterator = new \RecursiveIteratorIterator(
            new \RecursiveDirectoryIterator($directory, \FilesystemIterator::SKIP_DOTS)
        );

        foreach ($iterator as $file) {
            if ($file->isFile()) {
                $files[] = $file->getPathname();
            }
        }

        return $files;
    }

    private function relative(string $file): string
    {
        return ltrim(str_replace($this->root, '', $file), DIRECTORY_SEPARATOR);
    }
}
