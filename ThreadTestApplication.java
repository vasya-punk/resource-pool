import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ThreadTestApplication {

    public static void main(String[] args) {
        BlockingResourcePoolImpl<Object> resourcePool = new BlockingResourcePoolImpl<>(new LinkedBlockingQueue<>());
        new Thread(() -> {
            resourcePool.add(new Object());
            resourcePool.add(new Object());
            resourcePool.add(new Object());
        }).start();
        resourcePool.open();


        // TODO: Try to close while some resources in use
        /*new Thread(() -> {
            try {
               obj = resourcePool.acquire();
            }catch (InterruptedException e){

            }

            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                        resourcePool.release(obj);
                }
            }, 10000);

        }).start();

        new Thread(() -> {
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        resourcePool.close();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }, 5000);

        }).start();*/

        // TODO: simultaneously acquired and released
       /* new Thread(() -> {

            try {

                obj = resourcePool.acquire();

            }catch (InterruptedException e){

            }


            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                        resourcePool.release(obj);
                }
            }, 10000);

        }).start();

        new Thread(() -> {
            try {

                obj2 = resourcePool.acquire();

            }catch (InterruptedException e){

            }

            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                        resourcePool.release(obj2);
                }
            }, 10000);

        }).start();*/

        //TODO:  wait to some resource will be available to acquire
   /*     new Thread(() -> {
            try {
                obj = resourcePool.acquire();

                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        resourcePool.release(obj);
                    }
                }, 10000);
            }catch (InterruptedException e) {


            }
        }).start();

        new Thread(() -> {
            try {
                resourcePool.acquire();

            }catch (InterruptedException e){

            }

        }).start();

        new Thread(() -> {
            try {
                resourcePool.acquire();

            }catch (InterruptedException e){

            }

        }).start();

        new Thread(() -> {
            try {
                resourcePool.acquire();

            }catch (InterruptedException e){

            }

        }).start();*/

        //TODO:  wait acquired resource to remove
        /*new Thread(() -> {
            try {

                obj = resourcePool.acquire();
                boolean removed = resourcePool.remove(obj);
                System.out.println("is removed? "+removed);


            }catch (InterruptedException e) {

            }
        }).start();

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                resourcePool.release(obj);
            }
        }, 10000);

        new Thread(() -> {
            try {

                obj2 = resourcePool.acquire();
                boolean removed = resourcePool.remove(obj2);
                System.out.println("is removed? "+removed);


            }catch (InterruptedException e) {

            }
        }).start();

        // or removeNow
        Timer timer2 = new Timer();
        timer2.schedule(new TimerTask() {
            @Override
            public void run() {
                resourcePool.removeNow(obj2);
            }
        }, 10000);*/



        // TODO: Check if can't acquire in time
        /*new Thread(() -> {
            try {
                obj = resourcePool.acquire();
            }catch (InterruptedException e){

            }
        }).start();
        new Thread(() -> {
            try {
                resourcePool.acquire();
            }catch (InterruptedException e){

            }
        }).start();

        new Thread(() -> {
            try {
                resourcePool.acquire();
                resourcePool.acquire(15, TimeUnit.SECONDS);

            }catch (InterruptedException e){

            }
        }).start();

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                resourcePool.release(obj);
            }
        }, 10000);*/
    }

    public static volatile Object obj = null;
    public static volatile Object obj2 = null;
}
