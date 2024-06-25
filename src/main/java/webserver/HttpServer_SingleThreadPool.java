package webserver;

import webserver.httpmethodhandler.GetRequestHandler;
import webserver.httpmethodhandler.HttpRequestHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class HttpServer_SingleThreadPool {
    private final ExecutorService executorService;
    private final AtomicInteger hitCounter = new AtomicInteger(0);
    private final Map<String, HttpRequestHandler> handlerMap = new HashMap<>();

    public HttpServer_SingleThreadPool(int port, String hostname) {

        executorService = Executors.newSingleThreadExecutor();

        handlerMap.put("GET", new GetRequestHandler());

        try(ServerSocket serverSocket = new ServerSocket(port, 100, InetAddress.getByName(hostname))) {
            // backlog – requested maximum length of the queue of incoming connections.

            System.out.println("Server is listening on port " + port);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutting down server...");
                executorService.shutdown();
                System.out.println("Server shut down.");
            }));

            while (true) {
                Socket socket = serverSocket.accept();
                executorService.submit(() -> getRequest(socket));
            }

        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void getRequest(Socket socket) {
        try  {
            OutputStream outputStream = socket.getOutputStream();
            InputStream inputStream = socket.getInputStream();

            handleRequest(inputStream, outputStream);
        } catch (IOException e) {
            System.out.println("Client handling exception: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.out.println("Failed to close socket: " + e.getMessage());
            }
        }
    }

    private void handleRequest(InputStream input, OutputStream output) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        String requestLine = reader.readLine();
        if (requestLine == null || requestLine.isEmpty()) {
            return;
        }
        System.out.println("Thread : " + Thread.currentThread().getName());
        hitCounter();
        System.out.println("Request line: " + requestLine);

        String[] requestParts = requestLine.split(" ");

        if (requestParts.length < 2) {
            System.out.println("Invalid request line: " + requestLine);
            return;
        }

        String method = requestParts[0];
        HttpRequestHandler handler = handlerMap.get(method);

        if (handler != null) {
            handler.handleRequest(input, output);
        } else {
            String httpResponse = "HTTP/1.1 400 Bad Request\r\nContent-Length: 11\r\n\r\nBad Request";
            output.write(httpResponse.getBytes("UTF-8"));
        }
        output.flush();
    }

    private void hitCounter() {
        int count = hitCounter.incrementAndGet();
        System.out.println("Hit counter: " + count);
    }
}
