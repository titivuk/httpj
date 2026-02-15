import java.io.InputStream;

interface HttpContext {
    Headers getRequestHeaders();

    Headers getResponseHeaders();

    InputStream getBody();
}
