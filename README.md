# resource-pool


If I had more time, I would resolve some problems as:
 We should have some job to clean inUseCnt ( counter of resources in use) if some thread, which took that resource, had interrupted and didn’t release that resource. If we will not do that we can have such scenario: T1 acquired resource, inUseCnt++ and interrupted, T2 try to close pool, but will be held forever.

To know if my pool is thread-safe we can only run it through some intensive threaded tests designed to catch things like race conditions and dropped values.
I used some tests in **ThreadTestApplication**

I'm using LinkedBlockingQueue as main queue of available resources, because we have often acquire\release, but not add,remove(we need more like buffer but not dynamic array).
Thread-safety conditions was written in test. And for these exactly pool for those porpoises I added some more complex locking and conditions.

# Author
Vasyl Pryimak
