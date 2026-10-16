package com.banking.demo.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Página de erro amigável no lugar da "Whitelabel Error Page".
 *
 * O Spring Boot mostra um erro genérico e técnico quando não há mapeamento
 * para `/error`. Aqui devolvemos uma página em português com o status, o
 * caminho tentado e um caminho de volta — e preservamos o JSON para quem
 * consome a API, então os clientes programáticos continuam recebendo JSON.
 *
 * Implementar `ErrorController` é o que desliga o `BasicErrorController` do
 * auto-config (ele é `@ConditionalOnMissingBean(ErrorController.class)`); sem
 * isso os dois mapeariam `/error` e a aplicação não subiria.
 */
@RestController
public class ErrorPageController implements ErrorController {

    /** Erros chegam por forward, que pode ser de qualquer método HTTP. */
    @RequestMapping("/error")
    public ResponseEntity<String> erro(HttpServletRequest requisicao) {
        int status = status(requisicao);
        String caminho = caminho(requisicao);
        boolean prefereJson = prefereJson(requisicao);

        if (prefereJson) {
            return ResponseEntity.status(status)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("""
                            {
                              "success": false,
                              "mensagem": "%s",
                              "status": %d,
                              "path": "%s"
                            }
                            """.formatted(titulo(status), status, caminho));
        }

        return ResponseEntity.status(status)
                .contentType(MediaType.TEXT_HTML)
                .body(pagina(status, caminho));
    }

    private String pagina(int status, String caminho) {
        return """
                <!doctype html>
                <html lang="pt-BR">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>%d — %s</title>
                  <style>
                    :root { color-scheme: dark; }
                    * { box-sizing: border-box; }
                    body {
                      margin: 0; min-height: 100vh; display: grid; place-items: center;
                      padding: 2rem 1.25rem;
                      font-family: system-ui, -apple-system, "Segoe UI", Roboto, sans-serif;
                      background: radial-gradient(1200px 600px at 20% -10%, #1e3a5f 0%, transparent 60%), #0b1120;
                      color: #e5e9f0; line-height: 1.55; text-align: center;
                    }
                    main { width: min(34rem, 100%); }
                    .codigo { font-size: clamp(3rem, 12vw, 5rem); font-weight: 700; color: #60a5fa; margin: 0; letter-spacing: -.03em; }
                    h1 { font-size: 1.35rem; margin: .25rem 0 .75rem; }
                    p { color: #9aa7bd; margin: 0 0 1.5rem; }
                    code {
                      background: #101a2e; border: 1px solid #233047; border-radius: .35rem;
                      padding: .15rem .45rem; color: #93c5fd; word-break: break-all;
                    }
                    nav { display: flex; gap: .6rem; justify-content: center; flex-wrap: wrap; }
                    a {
                      display: inline-block; padding: .7rem 1.1rem; border-radius: .6rem;
                      text-decoration: none; font-weight: 600; font-size: .95rem;
                    }
                    a.primario { background: #3b82f6; color: #04101f; }
                    a.secundario { background: #101a2e; color: #e5e9f0; border: 1px solid #233047; }
                    a:hover { filter: brightness(1.1); }
                    a:focus-visible { outline: 3px solid #60a5fa; outline-offset: 2px; }
                  </style>
                </head>
                <body>
                  <main role="main">
                    <p class="codigo">%d</p>
                    <h1>%s</h1>
                    <p>Não encontramos <code>%s</code>.</p>
                    <nav aria-label="Navegação de recuperação">
                      <a class="primario" href="/">Ir para o início</a>
                      <a class="secundario" href="/swagger-ui/index.html">Abrir o Swagger</a>
                      <a class="secundario" href="/actuator/health">Ver o status</a>
                    </nav>
                  </main>
                </body>
                </html>
                """.formatted(status, titulo(status), status, titulo(status), caminho);
    }

    private int status(HttpServletRequest requisicao) {
        Object valor = requisicao.getAttribute("jakarta.servlet.error.status_code");
        return valor instanceof Integer ? (Integer) valor : 500;
    }

    private String caminho(HttpServletRequest requisicao) {
        Object valor = requisicao.getAttribute("jakarta.servlet.error.request_uri");
        String caminho = valor instanceof String ? (String) valor : requisicao.getRequestURI();
        return caminho == null || caminho.isBlank() ? "/" : caminho;
    }

    /** Cliente de API pede JSON; navegador pede HTML. */
    private boolean prefereJson(HttpServletRequest requisicao) {
        String aceite = requisicao.getHeader("Accept");
        return aceite != null && aceite.contains("application/json") && !aceite.contains("text/html");
    }

    private String titulo(int status) {
        return switch (status) {
            case 400 -> "Requisição inválida";
            case 401 -> "Não autorizado";
            case 403 -> "Acesso negado";
            case 404 -> "Página não encontrada";
            case 405 -> "Método não permitido";
            case 500 -> "Erro interno no servidor";
            case 502 -> "Serviço externo indisponível";
            case 503 -> "Serviço temporariamente indisponível";
            default -> status >= 400 && status < 500
                    ? "Não conseguimos atender a solicitação"
                    : "Algo deu errado no servidor";
        };
    }
}
