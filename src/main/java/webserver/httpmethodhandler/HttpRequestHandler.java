package webserver.httpmethodhandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface HttpRequestHandler {
    void handleRequest(InputStream input, OutputStream output) throws IOException;
}
