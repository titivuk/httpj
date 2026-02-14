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

class _Headers {
    private final Map<String, List<String>> _headers = new HashMap<>();

    void set(String key, String[] values) {
        this._headers.put(key, Arrays.asList(values));
    }

    void set(String key, String value) {
        List<String> values = new ArrayList<>();
        values.add(value);

        this._headers.put(key, values);
    }

    void add(String key, String[] newValues) {
        _headers.computeIfAbsent(key, k -> new ArrayList<>())
                .addAll(Arrays.asList(newValues));
    }

    void add(String key, String value) {
        _headers.computeIfAbsent(key, k -> new ArrayList<>())
                .add(value);
    }

    String[] get(String key) {
        return _headers.getOrDefault(key, Collections.emptyList()).toArray(String[]::new);
    }

    String getFirst(String key) {
        return _headers.getOrDefault(key, Collections.emptyList()).getFirst();
    }
}

interface HttpContext {
    _Headers getRequestHeaders();

    InputStream getBody();
}

class Http1Context implements HttpContext {
    private static final byte CR = 13;
    private static final byte LF = 10;

    // request stuff
    private final InputStream body;
    private final _Headers requestHeaders;

    public Http1Context(InputStream src) throws IOException {
        src = new BufferedInputStream(src);

        // request line
        String requestLine = readLine(src);
        parseRequestLine(requestLine);

        // headers
        List<String> rawHeaders = new ArrayList<>();
        String header;
        while (!(header = readLine(src)).isEmpty()) {
            rawHeaders.add(header);
        }
        requestHeaders = parseHeaders(rawHeaders);

        // body
        int bodyLen = getContentLength();
        this.body = new FixedSizeInputStream(src, bodyLen);
    }

    @Override
    public _Headers getRequestHeaders() {
        return null;
    }

    @Override
    public InputStream getBody() {
        return body;
    }

    private String readLine(InputStream src) throws IOException {
        // request line
        StringBuilder sb = new StringBuilder();

        int b = src.read();
        while (b != CR) {
            // TODO: b == -1
            sb.append((char) b);
            b = src.read();
        }

        // skip LF
        b = src.read();
        assert b == LF;

        return new String(sb);
    }

    private int getContentLength() {
        if (requestHeaders == null) {
            throw new IllegalStateException("cannot get content length: headers are not set");
        }

        String headerName = "Content-Length";
        return Optional.of(requestHeaders.getFirst(headerName))
                .map(Integer::parseInt)
                .orElse(0);
    }

    private void parseRequestLine(String requestLine) {
        String[] rlParts = requestLine.split(" ");

        if (rlParts.length != 3) {
            throw new IllegalArgumentException(String.format("invalid request line format: %s", requestLine));
        }
        if (!rlParts[2].equals("HTTP/1.1")) {
            throw new IllegalArgumentException(String.format("unsupported protocol: %s", rlParts[2]));
        }
        // TODO: validate method rlParts[1]
    }

    private _Headers parseHeaders(List<String> rawHeaders) {
        _Headers headers = new _Headers();

        for (String rawHeader : rawHeaders) {
            String[] headerParts = rawHeader.split(":\\s*", 2);
            if (headerParts.length != 2) {
                throw new IllegalArgumentException(String.format("invalid request header: %s", rawHeader));
            }

            String headerKey = headerParts[0];
            String[] headerValues = headerParts[1].split(",\\s*");

            headers.set(headerKey, headerValues);
        }

        return headers;
    }

}

class FixedSizeInputStream extends InputStream {
    private int remaining;
    private final InputStream src;

    FixedSizeInputStream(InputStream src, int size) {
        this.remaining = size;
        this.src = src;
    }

    @Override
    public int read() throws IOException {
        if (remaining == 0) {
            return -1;
        }

        int b = src.read();
        // not sure about that
        if (b != -1) {  // Only decrement if we actually read a byte
            remaining--;
        }
        assert remaining >= 0;

        return b;
    }
}
