import org.apache.log4j.Logger;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class BlockingResourcePoolImpl<R> implements ResourcePool<R> {

    Logger logger = Logger.getLogger(BlockingResourcePoolImpl.class);

    private ReentrantLock takeLock = new ReentrantLock();
    private Condition inUseNotEmpty = takeLock.newCondition();

    private ReentrantLock putLock = new ReentrantLock();
    private Condition newNotEmpty = putLock.newCondition();
    private Condition newNotFull = putLock.newCondition();

    private BlockingQueue< R> newObjects;
    private BlockingQueue< R> inUseObjects;

    private volatile boolean closeCalled;

    public BlockingResourcePoolImpl(int size) {
        newObjects = new LinkedBlockingQueue<>(size);
        inUseObjects = new LinkedBlockingQueue<>(size);

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
        if(resource == null){
            logger.debug("End add "+resource+ " - Invalid");
            return false;
        }

        putLock.lock();
        try {
            while(newObjects.remainingCapacity() == 0) newNotFull.await();

            boolean added = newObjects.offer(resource);
            if(added){
                newNotEmpty.signalAll();
            }
            return added;
        }finally {
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

        takeLock.lock();
        try {
            logger.debug("Start remove sync "+resource);

            if(inUseObjects.contains(resource)){
                try {
                    /*
                    check if the resource that is being removed
                    is currently in use, the remove operation
                    will block until that resource has been released
                     */
                    while (inUseObjects.contains(resource)) inUseNotEmpty.await();

                    return newObjects.remove(resource);
                }finally {
                    if(inUseObjects.size() == 0){
                        inUseNotEmpty.signalAll();
                    }

                    logger.debug("End remove sync "+resource+" - Was used");
                }
            }else {
                logger.debug("End remove sync "+resource+" - Not used");

                return newObjects.remove(resource);
            }
        }finally {
            takeLock.unlock();
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
            boolean result = false;
            if(newObjects.contains(resource))
                result = newObjects.remove(resource);

            if(inUseObjects.contains(resource)){
                result = inUseObjects.remove(resource);

                if(inUseObjects.size() == 0) {
                    inUseNotEmpty.signalAll();
                }
            }
            return result;
        }finally {
            logger.info("End removeNow "+resource);
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
            putLock.lock();
            logger.debug("Start acquire sync");
            try {
                // block until a resource is available
                while (newObjects.size() == 0) newNotEmpty.await();

                R t = newObjects.poll();

                if(t!=null)
                    inUseObjects.put(t);

                return t;
            } finally {
                logger.debug("End acquire sync");
                putLock.unlock();
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
            putLock.lock();
            logger.debug("Start acquire time sync");
            try {
                // block until a resource is available
                if (newObjects.size() == 0) newNotEmpty.await(timeOut, unit);

                t = newObjects.poll();

                if(t!=null)
                    inUseObjects.put(t);

                return t;
            } finally {
                logger.debug("End acquire time sync");
                putLock.unlock();
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

        takeLock.lock();
        putLock.lock();
        try {
            logger.debug("Start release sync " + resource);

            if (inUseObjects.contains(resource)) {
                newObjects.put(resource);
                inUseObjects.remove(resource);

                newNotEmpty.signalAll();

                if (inUseObjects.size() == 0) {
                    inUseNotEmpty.signalAll();
                }
            } else {
                // TODO: some logic if we received some resource not needed more or some invalid resource
                logger.debug("End release sync " + resource + " - Invalid");
            }
        } finally {
            logger.debug("End release sync " + resource);

            putLock.unlock();
            takeLock.unlock();
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
        takeLock.lock();
        logger.debug("Start close sync");

        // it blocks until all acquired resources are released.
        while(inUseObjects.size() > 0) inUseNotEmpty.await();

        try {
            closeNow();
        } finally {
            logger.debug("End close sync");
            takeLock.unlock();
        }
    }

    public void closeNow() {
        closeCalled = true;
    }
}
