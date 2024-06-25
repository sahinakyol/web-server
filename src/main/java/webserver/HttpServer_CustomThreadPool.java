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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class HttpServer_CustomThreadPool {
    private final ExecutorService executorService;
    private final AtomicInteger hitCounter = new AtomicInteger(0);
    private final Map<String, HttpRequestHandler> handlerMap = new HashMap<>();

    public HttpServer_CustomThreadPool(int port, String hostname) {
        int threadPoolSize = Runtime.getRuntime().availableProcessors() * 2;
        // corePoolSize – the number of threads to keep in the pool, even if they are idle, unless allowCoreThreadTimeOut is set
        // maximumPoolSize – the maximum number of threads to allow in the pool
        // keepAliveTime – when the number of threads is greater than the core, this is the maximum time that excess idle threads will wait for new tasks before terminating.
        // unit – the time unit for the keepAliveTime argument
        // workQueue – the queue to use for holding tasks before they are executed. This queue will hold only the Runnable tasks submitted by the execute method.
        // handler – the handler to use when execution is blocked because the thread bounds and queue capacities are reached
        // new ThreadPoolExecutor.CallerRunsPolicy() if all threads are busy then caller thread run the task
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
            output.write(httpResponse.getBytes("UTF-8"));
        }
        output.flush();
    }

    private void hitCounter() {
        int count = hitCounter.incrementAndGet();
        System.out.println("Hit counter: " + count);
    }
}
