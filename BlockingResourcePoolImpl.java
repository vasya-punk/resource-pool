import org.apache.log4j.Logger;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class BlockingResourcePoolImpl<R> implements ResourcePool<R> {

    Logger logger = Logger.getLogger(BlockingResourcePoolImpl.class);

    private ExecutorService executor =
            Executors.newCachedThreadPool();

    /** Lock held by acquire, remove, removeNow */
    private ReentrantLock takeLock = new ReentrantLock();

    /** Wait queue for waiting takes */
    private Condition notEmpty = takeLock.newCondition(); // if have some available resources. Signals when

    /** Lock held by add, returnToPool, etc */
    private ReentrantLock putLock = new ReentrantLock();

    /** Wait queue for waiting puts */
    private Condition notFull = putLock.newCondition();


    private ReentrantLock releaseLock = new ReentrantLock();

    /** Wait queue for waiting to release back some resource */
    private Condition released = releaseLock.newCondition(); // Signals when release or returnToPool, remove or removeNow for close, remove

    private BlockingQueue< R> objects;

    private AtomicInteger inUseCnt = new AtomicInteger(0);

    private volatile boolean closeCalled;

    public BlockingResourcePoolImpl(int size) {
        objects = new LinkedBlockingQueue<>(size);

        closeCalled = true;
    }

    /*
        return true if the pool was modified
        as a result of the method call
        or false if the pool was not modified.

        waiting if capacity is out
     */
    @Override
    public boolean add(R resource) throws InterruptedException{
        logger.info("add "+resource);
        putLock.lock();
        try {
            return objects.offer(resource);
        }finally {
            signalNotEmpty();
            putLock.unlock();
        }
    }

    /*
        If the resource that is being removed
        is currently in use, the remove operation
        will block until that resource has been released
     */
    @Override
    public boolean remove(R resource) throws InterruptedException{
        logger.info("Start remove "+resource);
        if(resource == null){
            logger.debug("End remove "+resource+ " - Invalid");
            return false;
        }

        //only for waiting release
        releaseLock.lock();
        try {
            while (!objects.contains(resource)) released.await(); // wait while resource in use get back to the queue

            takeLock.lock();
            try {
                logger.debug("Start remove sync " + resource);
                return objects.remove(resource);
            }finally {
                logger.debug("Finally remove sync"+resource);
                takeLock.unlock();
            }
        }finally {
            if (objects.remainingCapacity() > 0) {
                signalNotFull();
            }
            releaseLock.unlock();
        }
    }

    /*
        Removes the given resource immediately without waiting for it to be released.
        It returns true if the call resulted in the pool being modified and false otherwise.
     */
    public boolean removeNow(R resource){
        logger.info("Start removeNow "+resource);
        if(resource == null){
            logger.debug("End removeNow "+resource+ " - Invalid");
            return false;
        }

        takeLock.lock();
        logger.debug("Start removeNow sync "+resource);

        try {
            logger.debug("End removeNow sync "+resource);
            return objects.remove(resource);
        }finally {
            if (objects.remainingCapacity() > 0) {
                signalNotFull();
            }

            logger.info("Finally removeNow sync "+resource);
            takeLock.unlock();
        }
    }

    /*
        The acquire() method should block until a resource is
        available.
     */
    public R acquire() throws InterruptedException{
        logger.info("Start acquire");

        if(isOpen()) {
            takeLock.lock();
            logger.debug("Start acquire sync");
            try {
                // block until a resource is available
                while (objects.size() == 0) notEmpty.await();

                R t = objects.poll();

                inUseCnt.incrementAndGet();

                return t;
            } finally {
                if(objects.remainingCapacity() > 0){
                    signalNotFull();
                }
                logger.debug("Finally acquire sync");
                takeLock.unlock();
            }

        }

        throw new IllegalStateException(
                "Object pool is already shutdown");
    }

    /*
        If a resource cannot be acquired within the timeout
      interval specified in the acquire(long, TimeUnit) method,
      either a null can be returned or an exception can be
      thrown.
     */
    public R acquire(long timeOut, TimeUnit unit) throws InterruptedException{
        logger.info("Start acquire time");

        if(isOpen()) {
            R t = null;
            takeLock.lock();
            logger.debug("Start acquire time sync");
            try {
                // block until a resource is available
                if (objects.size() == 0) notEmpty.await(timeOut, unit);

                t = objects.poll();

                inUseCnt.incrementAndGet();

                return t;
            } finally {
                if(objects.remainingCapacity() > 0){
                    signalNotFull();
                }
                logger.debug("Finally acquire time sync");
                takeLock.unlock();
            }
        }

        throw new IllegalStateException(
                "Object pool is already shutdown");
    }

    /*
        Release resource at any time
     */
    @Override
    public void release(R resource) throws InterruptedException, IllegalArgumentException {
        logger.info("Start release "+resource);
        if(resource == null){
            logger.debug("End release "+resource+ " - Invalid");
            return;
        }

        putLock.lock();
        releaseLock.lock();
        try {
            logger.debug("Start release sync " + resource);

            if(inUseCnt.get() > 0){

                if(!add(resource)){ // if not add, do it async
                    returnToPool(resource);
                }else {
                    released.signalAll();
                }

                inUseCnt.decrementAndGet();
            }else {
                logger.debug("End release "+resource+ " - Invalid, we don't wait for it");
            }

        } finally {
            logger.debug("Finally release sync " + resource);
            releaseLock.unlock();
            putLock.unlock();
        }
    }

    @Override
    public void open() {
        closeCalled = false;
    }

    @Override
    public boolean isOpen() {
        return !closeCalled;
    }

    @Override
    public void close() throws InterruptedException{
        logger.info("Start close");

        // only for waiting release
        releaseLock.lock();
        try {
            logger.debug("Start close sync");

            // it blocks until all acquired resources are released.
            while(inUseCnt.get() > 0) released.await();

            putLock.lock();
            try {
                closeNow();
            } finally {
                logger.debug("Finally close sync");
                putLock.unlock();
            }
        }finally {
            releaseLock.unlock();
        }
    }

    public void closeNow() {
        closeCalled = true;
    }

    /**
     * Signals a waiting take. Called only from put/offer (which do not
     * otherwise ordinarily lock takeLock.)
     */
    private void signalNotEmpty() {
        takeLock.lock();
        try {
            notEmpty.signalAll();
        } finally {
            takeLock.unlock();
        }
    }

    /**
     * Signals a waiting release. Called only from returnToPool (which do not
     * otherwise ordinarily lock takeLock.)
     */
    private void signalReleased() {
        takeLock.lock();
        try {
            released.signalAll();
        } finally {
            takeLock.unlock();
        }
    }

    /**
     * Signals a waiting put. Called only from acquire/remove.
     */
    private void signalNotFull() {
        putLock.lock();
        try {
            notFull.signalAll();
        } finally {
            putLock.unlock();
        }
    }

    private void returnToPool(R resource)
    {
        executor.submit(new ObjectReturner(objects, resource));
    }

    private class ObjectReturner < E >
            implements Callable< Void > {
        private BlockingQueue<E> queue;
        private E e;

        public ObjectReturner(BlockingQueue<E> queue, E e) {
            this.queue = queue;
            this.e = e;
        }

        public Void call() {
            putLock.lock();
            try {
                while (queue.remainingCapacity() == 0) notFull.await();

                queue.offer(e);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }finally {
                if(queue.size() > 0){
                    signalNotEmpty();
                }
                signalReleased();
                putLock.unlock();
            }

            return null;
        }
    }
}
