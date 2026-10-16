package com.banking.demo.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Página inicial da API.
 *
 * Sem um mapeamento para `/`, o Spring Boot responde com a "Whitelabel Error
 * Page" (404) — a primeira impressão de quem acessa o deploy. Aqui a raiz vira
 * uma landing simples em português com os atalhos que importam: a aplicação do
 * banco (Angular), a documentação do Swagger e o status do serviço.
 *
 * A URL da aplicação vem de `APP_URL` (variável de ambiente) para não amarrar
 * o domínio do frontend dentro do código.
 */
@RestController
public class HomeController {

    private final String appUrl;
    private final String apiUrl;

    public HomeController(
            @Value("${app.url:https://zecki1.github.io/angular-ledger-bank/}") String appUrl,
            @Value("${api.url:https://java-spring-boot-banking-demo-production.up.railway.app}") String apiUrl) {
        this.appUrl = appUrl;
        this.apiUrl = apiUrl;
    }

    /** Landing page da API (HTML). */
    @GetMapping(produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> inicial() {
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body("""
                <!doctype html>
                <html lang="pt-BR">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>Banking Demo API — Spring Boot</title>
                  <style>
                    :root { color-scheme: dark; }
                    * { box-sizing: border-box; }
                    body {
                      margin: 0; min-height: 100vh; display: grid; place-items: center;
                      padding: 2rem 1.25rem;
                      font-family: system-ui, -apple-system, "Segoe UI", Roboto, sans-serif;
                      background: radial-gradient(1200px 600px at 20% -10%, #1e3a5f 0%, transparent 60%),
                                  radial-gradient(900px 500px at 110% 10%, #14532d55 0%, transparent 55%), #0b1120;
                      color: #e5e9f0; line-height: 1.55;
                    }
                    main { width: min(46rem, 100%); }
                    .selo {
                      display: inline-block; padding: .25rem .7rem; border-radius: 999px;
                      background: #14532d; color: #bbf7d0; font-size: .8rem; font-weight: 600;
                      border: 1px solid #166534;
                    }
                    h1 { font-size: clamp(1.7rem, 4vw, 2.4rem); margin: .9rem 0 .5rem; letter-spacing: -.02em; }
                    p.lead { margin: 0 0 1.75rem; color: #9aa7bd; max-width: 46ch; }
                    nav { display: grid; gap: .85rem; }
                    a.card {
                      display: flex; flex-direction: column; gap: .2rem;
                      padding: 1rem 1.15rem; border-radius: .85rem; text-decoration: none;
                      background: #101a2e; border: 1px solid #233047; color: inherit;
                      transition: border-color .15s, transform .15s, background .15s;
                    }
                    a.card:hover, a.card:focus-visible {
                      border-color: #3b82f6; background: #14203a; transform: translateY(-2px);
                    }
                    a.card:focus-visible { outline: 3px solid #60a5fa; outline-offset: 2px; }
                    a.card b { font-size: 1.05rem; }
                    a.card span { color: #9aa7bd; font-size: .88rem; word-break: break-all; }
                    ul { margin: 1.75rem 0 0; padding-left: 1.15rem; color: #9aa7bd; font-size: .92rem; }
                    li { margin-bottom: .3rem; }
                    code {
                      background: #101a2e; border: 1px solid #233047; border-radius: .35rem;
                      padding: .1rem .38rem; color: #93c5fd; font-size: .86em;
                    }
                    footer { margin-top: 2rem; color: #64748b; font-size: .8rem; }
                    @media (prefers-reduced-motion: reduce) { * { transition: none !important; } }
                  </style>
                </head>
                <body>
                  <main>
                    <span class="selo">Spring Boot 3 · Java 21</span>
                    <h1>Banking Demo API</h1>
                    <p class="lead">
                      Backend que alimenta a aplicação de banco em Angular. Acesse a aplicação
                      para usar o sistema, ou a documentação interativa para testar os endpoints.
                    </p>

                    <nav aria-label="Atalhos">
                      <a class="card" href="%s">
                        <b>Abrir a aplicação do banco →</b>
                        <span>%s</span>
                      </a>
                      <a class="card" href="/swagger-ui/index.html">
                        <b>Documentação da API (Swagger)</b>
                        <span>%s/swagger-ui/index.html</span>
                      </a>
                      <a class="card" href="/v3/api-docs">
                        <b>Especificação OpenAPI (JSON)</b>
                        <span>%s/v3/api-docs</span>
                      </a>
                      <a class="card" href="/actuator/health">
                        <b>Status do serviço</b>
                        <span>%s/actuator/health</span>
                      </a>
                    </nav>

                    <h2 style="font-size:1rem;color:#9aa7bd;margin:2rem 0 .5rem">Principais endpoints</h2>
                    <ul>
                      <li><code>GET /api/contas</code> — lista de contas</li>
                      <li><code>POST /api/transferencias</code> — transfere entre contas</li>
                      <li><code>GET /api/mercado/cotacoes?tickers=PETR4,VALE3</code> — cotações da B3</li>
                      <li><code>GET /api/mercado/series?ticker=PETR4&amp;range=1mo</code> — série histórica</li>
                      <li><code>GET /actuator/health</code> — saúde da aplicação</li>
                    </ul>

                    <footer>
                      Todas as respostas usam o envelope
                      <code>{ "success", "mensagem", "data", "count", "timestamp" }</code>.
                    </footer>
                  </main>
                </body>
                </html>
                """.formatted(appUrl, appUrl, apiUrl, apiUrl, apiUrl));
    }
}
