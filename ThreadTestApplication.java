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
        BlockingResourcePoolImpl<Object> resourcePool = new BlockingResourcePoolImpl<>(2);
        new Thread(() -> {
            try {
                resourcePool.add(new Object());
                resourcePool.add(new Object());
                resourcePool.add(new Object());

            } catch (InterruptedException e) {

            }
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
                    try {
                        resourcePool.release(obj);
                    }catch (InterruptedException e){

                    }
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
        /*new Thread(() -> {

            try {

                obj = resourcePool.acquire();

            }catch (InterruptedException e){

            }


            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        resourcePool.release(obj);
                    }catch (InterruptedException e){

                    }
                }
            }, 10000);

        }).start();

        new Thread(() -> {
            try {

                obj = resourcePool.acquire();

            }catch (InterruptedException e){

            }

            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        resourcePool.release(obj);
                    }catch (InterruptedException e){

                    }
                }
            }, 10000);

        }).start();*/

        //TODO:  wait to some resource will be available to acquire
        /*new Thread(() -> {
            try {

                    obj = resourcePool.acquire();




                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            resourcePool.release(obj);

                        }catch (InterruptedException e){

                        }
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

        }).start();*/

        //TODO:  wait acquired resource to remove
        /*new Thread(() -> {
            try {

                obj = resourcePool.acquire();
System.out.println("sdfasd");
                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            resourcePool.release(obj);
                        }catch (InterruptedException e){

                        }
                    }
                }, 15000);

                        resourcePool.remove(obj);
            }catch (InterruptedException e){

            }



        }).start();



        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                resourcePool.removeNow(obj);
            }
        }, 10000);*/


        // TODO: Check if can't acquire in time
       /* new Thread(() -> {
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

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {

                    resourcePool.release(obj);
                }catch (InterruptedException e){

                }
            }
        }, 20000);

        new Thread(() -> {
            try {

                Object object = resourcePool.acquire(10, TimeUnit.SECONDS);

            }catch (InterruptedException e){

            }
        }).start();

        new Thread(() -> {
            try {
                resourcePool.acquire();
            }catch (InterruptedException e){

            }
        }).start();*/

    }

    public static volatile Object obj = null;
}
