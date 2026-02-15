import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    public static void main(String[] args) {
        try (ExecutorService execSvc = Executors.newVirtualThreadPerTaskExecutor()) {
            try (ServerSocket ss = new ServerSocket(3000)) {
                while (true) {
                    final Socket conn = ss.accept();

                    execSvc.submit(() -> {
                        try {
                            HttpContext ctx = new Http1Context(conn.getInputStream());
                            writeResponse(conn.getOutputStream(), ctx.getBody());
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            tryCloseConnection(conn);
                        }
                    });
                }
            } catch (IOException e) {
                System.exit(1);
            }
        }
    }

    private static void tryCloseConnection(Closeable conn) {
        try {
            conn.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void writeResponse(OutputStream out, byte[] body) throws IOException {
        writeRequestLine(out);
        writeHeaders(out);
        out.write(body);

        out.flush();
    }

    private static void writeResponse(OutputStream out, InputStream in) throws IOException {
        writeRequestLine(out);
        writeHeaders(out);
        in.transferTo(out);

        out.flush();
    }

    private static void writeRequestLine(OutputStream out) throws IOException {
        out.write("HTTP/1.1 200 OK\r\n".getBytes());
    }

    private static void writeHeaders(OutputStream out) throws IOException {
        out.write("Content-Type: text/plain\r\n".getBytes());
        out.write('\r');
        out.write('\n');
    }
}
