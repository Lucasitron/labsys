<main class="flex min-h-screen items-center justify-center">
	<section class="w-full max-w-sm rounded-2xl bg-white p-8 shadow">
		<h1 class="mb-2 text-2xl font-semibold">Entrar</h1>
		<p class="mb-6 text-sm text-slate-500">Autenticação via <code>auth-service</code>.</p>

		<form class="flex flex-col gap-4" method="post" action="/api/auth/login">
			<label class="flex flex-col gap-1 text-sm font-medium">
				E-mail
				<input
					class="rounded-lg border border-slate-300 px-3 py-2"
					type="email"
					name="email"
					required
				/>
			</label>
			<label class="flex flex-col gap-1 text-sm font-medium">
				Senha
				<input
					class="rounded-lg border border-slate-300 px-3 py-2"
					type="password"
					name="password"
					required
				/>
			</label>
			<button class="rounded-lg bg-blue-600 px-4 py-2 text-white hover:bg-blue-700" type="submit">
				Entrar
			</button>
		</form>
	</section>
</main>