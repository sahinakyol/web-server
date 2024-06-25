package webserver.httpmethodhandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class GetRequestHandler implements HttpRequestHandler {
    @Override
    public void handleRequest(InputStream input, OutputStream output) throws IOException {
        String httpResponse = "HTTP/1.1 200 OK\r\nContent-Length: 13\r\n\r\nHello, World!";
        output.write(httpResponse.getBytes("UTF-8"));
        output.flush();
    }
}
