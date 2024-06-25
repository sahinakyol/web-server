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

public class HttpServer_Lock_Sync_Mutex_Semaphore {
    private final ExecutorService executorService;
    private volatile int hitCounter = 0;
    private volatile boolean running = true;

    //private final ReentrantLock lock = new ReentrantLock();

    //Object lock = new Object();

    private final Map<String, HttpRequestHandler> handlerMap = new HashMap<>();

    public HttpServer_Lock_Sync_Mutex_Semaphore(int port, String hostname) {
        int threadPoolSize = Runtime.getRuntime().availableProcessors() * 2;

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
                running = false;
                System.out.println("Running Flag is " + running);
                executorService.shutdown();
                System.out.println("Server shut down.");
            }));

            while (running) {
                Socket socket = serverSocket.accept();
                executorService.submit(() -> getRequest(socket));
            }
            System.out.println("Running Flag is " + running);

        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
            executorService.shutdown();
        }
    }

    private void getRequest(Socket socket) {
        try {
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
        hitCounter();

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

    // synchronized directly affecting performance
    /*private synchronized void hitCounter() {
        hitCounter += 1;
        System.out.println("Hit counter: " + hitCounter);
    }*/

    // lock directly affecting performance
    /*private void hitCounter() {
        lock.lock();
        try {
            hitCounter++;
        } finally {
            lock.unlock();
        }
        System.out.println("Hit counter: " + hitCounter);
    }*/


    /*private void hitCounter() {
        synchronized (this) {
            hitCounter++;
        }
        System.out.println("Hit counter: " + hitCounter);
    }*/

   /* private void hitCounter() {
        synchronized (lock) {
            hitCounter++;
        }
        System.out.println("Hit counter: " + hitCounter);
    }*/

    // Hit counter 184 Thread pool-1-thread-7
    // Hit counter 184 Thread pool-1-thread-11
    // Number reflected with same value two diff thread but there is still race condition
    private void hitCounter() {
        hitCounter += 1;
        if ((hitCounter % 2) == 0) {
            System.out.println("Hit counter " + hitCounter + " Thread " + Thread.currentThread().getName());
        }
    }
}
