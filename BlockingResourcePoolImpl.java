import org.apache.log4j.Logger;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class BlockingResourcePoolImpl<R> implements ResourcePool<R> {

    Logger logger = Logger.getLogger(BlockingResourcePoolImpl.class);

    /** Lock held by acquire, remove, removeNow */
    private final ReentrantLock takeLock = new ReentrantLock();

    /** Wait queue for waiting takes */
    private final Condition notEmpty = takeLock.newCondition();

    /** Lock held by add, releaseAsync, etc */
    private final ReentrantLock putLock = new ReentrantLock();

    /** Wait queue for waiting to release back some resource */
    private final Condition released = putLock.newCondition();

    private final Queue<R> objects;
    private final Set<R> acquiredObjects = new HashSet<>();

    private volatile boolean isOpened;

    public BlockingResourcePoolImpl() {
        this(new LinkedList<>());
    }

    public BlockingResourcePoolImpl(Queue<R> resourceQueue) {
        objects = resourceQueue;

        isOpened = false;
    }

    /*
        return true if the pool was modified
        as a result of the method call
        or false if the pool was not modified.
     */
    @Override
    public boolean add(R resource){
        logger.info("add "+resource);
        putLock.lock();
        try {
            return objects.offer(resource);
        }finally {
            putLock.unlock();

            signalNotEmpty();
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

        putLock.lock();
        try {
            logger.debug("Start remove sync " + resource);

            while (acquiredObjects.contains(resource)) released.await(); // wait while resource in use get back to the queue

            takeLock.lock();
            try {
                return objects.remove(resource);
            }finally {
                takeLock.unlock();
            }
        }finally {
            logger.debug("Finally remove sync"+resource);
            putLock.unlock();
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
        try {
            logger.debug("Start removeNow sync "+resource);

            if(acquiredObjects.contains(resource)){
                return acquiredObjects.remove(resource);
            }else {
                return objects.remove(resource);
            }
        }finally {
            logger.info("Finally removeNow sync "+resource);
            takeLock.unlock();

            signalReleased();
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
            try {
                logger.debug("Start acquire sync");
                // block until a resource is available
                while (objects.size() == 0) notEmpty.await();

                R resource = objects.poll();
                acquiredObjects.add(resource);

                return resource;
            } finally {
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
            takeLock.lock();
            try {
                logger.debug("Start acquire time sync");
                // block until a resource is available
                if (objects.size() == 0) notEmpty.await(timeOut, unit);

                if (objects.size() == 0) {
                    return null;
                }else {
                    R resource = objects.poll();
                    acquiredObjects.add(resource);

                    return resource;
                }

            } finally {
                logger.debug("Finally acquire time sync");
                takeLock.unlock();
            }
        }

        throw new IllegalStateException(
                "Object pool is already shutdown");
    }

    /*
        Release resource
     */
    @Override
    public void release(R resource) {
        logger.info("Start release "+resource);
        if(resource == null){
            logger.debug("End release "+resource+ " - Invalid or not acquired");
            return;
        }

        putLock.lock();
        try {
            logger.debug("Start release sync " + resource);

            if(acquiredObjects.remove(resource)){
                objects.add(resource);
            }
        } finally {
            logger.debug("Finally release sync " + resource);
            released.signal();
            putLock.unlock();

            signalNotEmpty();
        }
    }

    @Override
    public void open() {
        isOpened = true;
    }

    @Override
    public boolean isOpen() {
        return isOpened;
    }

    @Override
    public void close() throws InterruptedException{
        logger.info("Start close");

        putLock.lock();
        try {
            logger.debug("Start close sync");

            // it blocks until all acquired resources are released.
            while(acquiredObjects.size() > 0) released.await();

            closeNow();
        }finally {
            logger.debug("Finally close sync");
            putLock.unlock();
        }
    }

    public void closeNow() {
        isOpened = false;
    }

    /**
     * Signals a waiting take. Called only from add (which do not
     * otherwise ordinarily lock takeLock.)
     */
    private void signalNotEmpty() {
        takeLock.lock();
        try {
            if (objects.size() > 0)
                notEmpty.signal();
        } finally {
            takeLock.unlock();
        }
    }

    /**
     * Signals a waiting put. Called only from removeNow (which do not
     * otherwise ordinarily lock putLock.)
     */
    private void signalReleased() {
        putLock.lock();
        try {
            released.signal();
        } finally {
            putLock.unlock();
        }
    }
}
