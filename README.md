# resource-pool

If I had more time, I would resolve some problems as:
 We should have some job to clean resources in use if some thread, which took that resource, had interrupted and dodn’t released that resource. If we will not do that we can have such scenario: T1 acquired resource,  and interrupted, T2 try to close pool, but will be held forever.

To know if my pool is thread-safe we can only run it through some intensive threaded tests designed to catch things like race conditions and dropped values.
I used some tests in **ThreadTestApplication**

I'm using Queue<R> objects for holding available resources and HashSet acquiredObjects for holding objects that are acquired.
acquire() method gets object from objects queue and puts it in acuiredObjects, release(R resource) method gets object from acquiredObjects and put it back to objects queue.
I did like that because we need to have to logics: FIFO queue to have some priority and posibility to getFirst, putLast and HashSet to have fast get/remove methods(complexity O(1)).

# Author
Vasyl Pryimak
