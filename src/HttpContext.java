import java.io.IOException;
import java.io.InputStream;

interface HttpContext {
    Headers getRequestHeaders();

    Headers getResponseHeaders();

    InputStream getBody();

    String getMethod();

    String getRequestPath();

    void setStatus(int statusCode);

    void writeResponse(InputStream src) throws IOException;

    void writeResponse(byte[] body) throws IOException;

    void noResponse() throws IOException;
}
