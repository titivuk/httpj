@FunctionalInterface
interface HttpHandler {
    void handle(HttpContext context);
}
