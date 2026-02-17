import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class HttpServer {
    private ServerSocket serverSocket;
    private final Map<String, HttpHandler> routes;

    public HttpServer() {
        routes = new ConcurrentHashMap<>();
    }

    public void listen(int port) throws IOException {
        try (ExecutorService execSvc = Executors.newVirtualThreadPerTaskExecutor()) {
            try (ServerSocket ss = new ServerSocket(port)) {
                serverSocket = ss;

                while (true) {
                    final Socket conn = serverSocket.accept();

                    execSvc.submit(() -> {
                        try {
                            HttpContext ctx = new Http1Context(conn.getInputStream(), conn.getOutputStream());

                            HttpHandler handler = routes.get(ctx.getMethod() + " " + ctx.getRequestPath());
                            if (handler == null) {
                                // TODO: 404
                                ctx.writeResponse("Not Found".getBytes());
                                return;
                            }

                            handler.handle(ctx);
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            tryCloseConnection(conn);
                        }
                    });
                }
            }
        }
    }

    /**
     * for now single method with http method included
     * for example, "POST /route/path"
     */
    public HttpServer register(String path, HttpHandler handler) {
        routes.put(path, handler);
        return this;
    }

    private void tryCloseConnection(Closeable conn) {
        try {
            conn.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

@FunctionalInterface
interface HttpHandler {
    void handle(HttpContext context);
}
