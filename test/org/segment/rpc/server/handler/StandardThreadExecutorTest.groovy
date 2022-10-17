package org.segment.rpc.server.handler

import java.util.concurrent.RejectedExecutionException

int min = 2
int max = 10
int queueSize = 10
def obj = new Object()
def executor = new StandardThreadExecutor(min, max, queueSize)

assert executor.poolSize == 0

(1..min).each {
    executor.execute(new WaitRunnable(obj, it))
    assert executor.poolSize == it
    assert executor.submittedTasksCount == it
    assert executor.queue.size() == 0
}

((min + 1)..max).each {
    executor.execute(new WaitRunnable(obj, it))
    assert executor.poolSize == it
    assert executor.submittedTasksCount == it
    assert executor.queue.size() == 0
}

(1..queueSize).each {
    executor.execute(new WaitRunnable(obj, max + it))
    assert executor.poolSize == max
    assert executor.submittedTasksCount == it + max
    assert executor.queue.size() == it
}

try {
    executor.execute(new WaitRunnable(obj, max + queueSize + 1))
    // not happen
    assert false
} catch (RejectedExecutionException e) {
    assert true
} catch (Exception e) {
    assert false
}

assert executor.poolSize == max
assert executor.submittedTasksCount == queueSize + max
assert executor.queue.size() == queueSize

Thread.sleep(1000)

synchronized (obj) {
    obj.notifyAll()
}

// wait until all max pool size's task done
Thread.sleep(1000)

println '...'

assert executor.submittedTasksCount == queueSize
assert executor.queue.size() == 0

Thread.sleep(1000)

synchronized (obj) {
    obj.notifyAll()
}

Thread.sleep(1000)

assert executor.submittedTasksCount == 0
assert executor.queue.size() == 0

executor.shutdown()

class WaitRunnable implements Runnable {
    private Object obj

    private int index

    WaitRunnable(Object obj, int index) {
        this.obj = obj
        this.index = index
    }

    @Override
    void run() {
        synchronized (obj) {
            obj.wait()
            println 'done ' + index
        }
    }
}