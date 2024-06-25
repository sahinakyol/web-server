# Simple Http Server for educational purposes.

# Concurrency
 TODO

# Executors
TODO

----
### Sync
CountDownLatch: Used to wait for a fixed number of events to occur.

```java
    CountDownLatch latch=new CountDownLatch(3);
        latch.countDown();
        latch.await();
```

CyclicBarrier: Used to synchronize a fixed number of threads at a common barrier point.

```java
    CyclicBarrier barrier=new CyclicBarrier(3,()->System.out.println("All tasks finished, barrier broken"));
        barrier.await();
```

Semaphore: Used to limit the number of threads that can access a resource simultaneously.

```java
    Semaphore semaphore=new Semaphore(2); // Only 2 permits available
        semaphore.acquire();
        semaphore.release();
```

```java
    Phaser phaser=new Phaser(1);
        phaser.register();
        ()->{phaser.arriveAndAwaitAdvance();}
        
        phaser.arriveAndDeregister();
```
Phaser:
- register(): Adds a new thread to the phaser.
- arriveAndAwaitAdvance(): Marks a thread's arrival at the phaser and waits for other threads to arrive.
- arriveAndDeregister(): Marks a thread's arrival and removes it from the phaser.

Try volatile object
```shell
jps
kill -15 <PID>
-- -15 is SIGTERM
```