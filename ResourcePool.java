import java.util.concurrent.TimeUnit;

public interface ResourcePool<R> {
    void open();
    boolean isOpen();
    void close() throws InterruptedException;
    R acquire() throws InterruptedException;
    R acquire(long timeout, TimeUnit timeUnit) throws InterruptedException;
    void release(R resource) throws InterruptedException;
    boolean add(R resource) throws InterruptedException;
    boolean remove(R resource) throws InterruptedException;
}
