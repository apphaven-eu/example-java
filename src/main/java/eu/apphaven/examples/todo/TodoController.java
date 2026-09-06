package eu.apphaven.examples.todo;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class TodoController {

    private final JdbcClient db;

    TodoController(JdbcClient db) {
        this.db = db;
    }

    public record Todo(long id, String title) {
    }

    // Renders src/main/resources/templates/index.html. Thymeleaf escapes th:text, so a
    // title containing markup is shown as text rather than interpreted.
    @GetMapping("/")
    public String index(Model model) {
        List<Todo> todos = db.sql("SELECT id, title FROM todos ORDER BY created_at DESC, id DESC")
                .query(Todo.class)
                .list();

        model.addAttribute("todos", todos);
        return "index";
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
    @ResponseBody
    public String healthz() {
        return "ok";
    }

    // 303 after a form post, so a refresh does not resubmit.
    private ResponseEntity<Void> redirectHome() {
        return ResponseEntity.status(HttpStatus.SEE_OTHER).location(URI.create("/")).build();
    }
}
