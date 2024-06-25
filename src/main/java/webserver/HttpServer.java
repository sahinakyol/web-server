package webserver;

import webserver.httpmethodhandler.GetRequestHandler;
import webserver.httpmethodhandler.HttpRequestHandler;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class HttpServer {
    private final ExecutorService executorService;
    private final AtomicInteger hitCounter = new AtomicInteger(0);
    private final Map<String, HttpRequestHandler> handlerMap = new HashMap<>();

    public HttpServer(int port, String hostname) {
        // Set the thread pool size to twice the number of available processors
        // We set the double size of physical processor because this code is IO intensive
        int threadPoolSize = Runtime.getRuntime().availableProcessors() * 2;

        // Initialize the executor service with a fixed thread pool size,
        // a keep-alive time of 60 seconds, a LinkedBlockingQueue with a capacity of 100,
        // and a CallerRunsPolicy to run the task in the calling thread if the pool is saturated
        executorService = new ThreadPoolExecutor(
                threadPoolSize,
                threadPoolSize,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100),
                new ThreadPoolExecutor.CallerRunsPolicy());

        handlerMap.put("GET", new GetRequestHandler());

        try {
            InetAddress host = InetAddress.getByName(hostname);
            // backlog – requested maximum length of the queue of incoming connections.
            ServerSocket serverSocket = new ServerSocket(port, 100, host);
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
            executorService.shutdown();
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
            output.write(httpResponse.getBytes(StandardCharsets.UTF_8));
        }
        output.flush();
    }

    private void hitCounter() {
        int count = hitCounter.incrementAndGet();
        System.out.println("Hit counter: " + count);
    }
}
