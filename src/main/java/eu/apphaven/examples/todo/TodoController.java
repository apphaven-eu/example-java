package eu.apphaven.examples.todo;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

@RestController
public class TodoController {

    private static final String CSS = """
            * { box-sizing: border-box; }
            input[type=text] { min-width: 0; }
            li span { min-width: 0; overflow-wrap: anywhere; }
            li form { flex-shrink: 0; }
            footer { margin-top: 2rem; padding-top: 1rem; border-top: 1px solid #8884; font-size: .875rem; }
            a { color: #2457bd; }
            :focus-visible { outline: 2px solid #2457bd; outline-offset: 3px; }
            body {
              margin: 0; padding: 48px 16px;
              font-family: system-ui, -apple-system, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
              line-height: 1.5; color: #1c1c1e; background: #f6f6f7;
            }
            main { max-width: 640px; margin: 0 auto; }
            h1 { font-size: 1.5rem; margin: 0 0 24px; font-weight: 600; }
            form.add { display: flex; gap: 8px; margin-bottom: 24px; }
            input[type=text] {
              flex: 1; padding: 10px 12px; font: inherit;
              border: 1px solid #c9c9cd; border-radius: 6px; background: #fff;
            }
            input[type=text]:focus { outline: 2px solid #4a6cf7; outline-offset: 1px; }
            button {
              padding: 10px 16px; font: inherit; border-radius: 6px;
              border: 1px solid #c9c9cd; background: #fff; cursor: pointer;
            }
            form.add button { background: #1c1c1e; border-color: #1c1c1e; color: #fff; }
            ul { list-style: none; margin: 0; padding: 0; border-top: 1px solid #e2e2e6; }
            li {
              display: flex; align-items: center; gap: 16px;
              padding: 12px 4px; border-bottom: 1px solid #e2e2e6;
            }
            li span { flex: 1; overflow-wrap: anywhere; }
            li button { padding: 6px 12px; font-size: 0.875rem; color: #8a1c1c; }
            p.empty { color: #6b6b70; padding: 16px 4px; margin: 0; }
            """;

    private final JdbcClient db;

    TodoController(JdbcClient db) {
        this.db = db;
    }

    record Todo(long id, String title) {
    }

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public String index() {
        List<Todo> todos = db.sql("SELECT id, title FROM todos ORDER BY created_at DESC, id DESC")
                .query(Todo.class)
                .list();

        StringBuilder html = new StringBuilder();
        html.append("<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\">")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">")
                .append("<title>Spring Boot Todo | AppHaven</title><style>").append(CSS).append("</style></head><body><main>")
                .append("<h1>Spring Boot Todo</h1><p>A shared task list, built with Spring Boot and PostgreSQL.</p>")
                .append("<form class=\"add\" method=\"post\" action=\"/add\">")
                .append("<input type=\"text\" name=\"title\" aria-label=\"New task\" placeholder=\"What needs doing?\" ")
                .append("maxlength=\"200\" autocomplete=\"off\" autofocus required>")
                .append("<button type=\"submit\">Add</button></form>");

        if (todos.isEmpty()) {
            html.append("<p class=\"empty\">No items yet.</p>");
        } else {
            html.append("<ul>");
            for (Todo todo : todos) {
                html.append("<li><span>").append(HtmlUtils.htmlEscape(todo.title())).append("</span>")
                        .append("<form method=\"post\" action=\"/delete\">")
                        .append("<input type=\"hidden\" name=\"id\" value=\"").append(todo.id()).append("\">")
                        .append("<button type=\"submit\">Delete</button></form></li>");
            }
            html.append("</ul>");
        }

        return html.append("<footer><p>Deploy your own on <a href=\"https://apphaven.eu\">AppHaven</a> · ")
                .append("<a href=\"https://github.com/apphaven-eu/example-java\">Source code</a> · ")
                .append("<a href=\"https://docs.apphaven.eu/getting-started\">Deployment guide</a>")
                .append("</p></footer></main></body></html>").toString();
    }

    @PostMapping("/add")
    public ResponseEntity<Void> add(@RequestParam String title) {
        String trimmed = title.strip();
        if (!trimmed.isEmpty()) {
            db.sql("INSERT INTO todos (title) VALUES (?)")
                    .param(trimmed.codePointCount(0, trimmed.length()) > 200
                            ? trimmed.substring(0, trimmed.offsetByCodePoints(0, 200)) : trimmed)
                    .update();
        }
        return redirectHome();
    }

    @PostMapping("/delete")
    public ResponseEntity<Void> delete(@RequestParam long id) {
        db.sql("DELETE FROM todos WHERE id = ?").param(id).update();
        return redirectHome();
    }

    @GetMapping(value = "/healthz", produces = MediaType.TEXT_PLAIN_VALUE)
    public String healthz() {
        return "ok";
    }

    // 303 after a form post, so a refresh does not resubmit.
    private ResponseEntity<Void> redirectHome() {
        return ResponseEntity.status(HttpStatus.SEE_OTHER).location(URI.create("/")).build();
    }
}
