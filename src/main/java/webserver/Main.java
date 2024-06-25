package webserver;

public class Main {


    public static void main(String[] args) {
        //new HttpServer(8084, "localhost");

        //-----------------------------------
        //new HttpServer_CachedThreadPool(8084, "localhost");
        // It is good for the short running tasks
        // there is no limit for new thread

        //new HttpServer_CustomThreadPool(8084, "localhost");

        //new HttpServer_FixedThreadPool(8084, "localhost");
        // It is ideal for limited resource usage needs

        //new HttpServer_SingleThreadPool(8084, "localhost");
        // it is ideal for sequential running tasks

        new HttpServer_Lock_Sync_Mutex_Semaphore(8084, "localhost");
    }
}
