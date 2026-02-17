import java.io.IOException;

import static java.lang.System.exit;

public class Main {
    public static void main(String[] args) {
        HttpServer server = new HttpServer();

        server.register("POST /mirror", (HttpContext ctx) -> {
                    try {
                        String contentType = ctx.getRequestHeaders().getFirst("Content-Type");
                        if (contentType != null) {
                            ctx.getResponseHeaders().set("Content-Type", contentType);
                        }

                        ctx.writeResponse(ctx.getBody());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .register("POST /hello", (HttpContext ctx) -> {
                    try {
                        ctx.getResponseHeaders().set("Content-Type", "application/json");
                        ctx.writeResponse("{\"hello\": \"world\"}".getBytes());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

        try {
            server.listen(3000);
        } catch (Exception e) {
            e.printStackTrace();
            exit(1);
        }
    }
}
