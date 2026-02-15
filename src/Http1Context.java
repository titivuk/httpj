import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

class Http1Context implements HttpContext {
    private static final byte CR = 13, LF = 10;

    private String protocol;
    private String method;
    private String path;
    private final InputStream body;
    private final Headers requestHeaders;

    private final Headers responseHeaders;

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

        responseHeaders = new Headers();
    }

    @Override
    public Headers getRequestHeaders() {
        return requestHeaders;
    }

    @Override
    public Headers getResponseHeaders() {
        return responseHeaders;
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

        method = rlParts[0];
        path = rlParts[1];
        protocol = rlParts[2];
    }

    private Headers parseHeaders(List<String> rawHeaders) {
        Headers headers = new Headers();

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
