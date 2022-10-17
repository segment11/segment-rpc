package org.segment.rpc.server.handler

import groovy.transform.CompileStatic

import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger

/*
refer Motan StandardThreadExecutor
 */

@CompileStatic
class StandardThreadExecutor extends ThreadPoolExecutor {
    public static final int DEFAULT_MIN_THREADS = 20
    public static final int DEFAULT_MAX_THREADS = 200
    // 1min
    public static final int DEFAULT_MAX_IDLE_TIME = 60 * 1000

    private AtomicInteger submittedTasksCount
    private int maxSubmittedTaskCount

    StandardThreadExecutor() {
        this(DEFAULT_MIN_THREADS, DEFAULT_MAX_THREADS)
    }

    StandardThreadExecutor(int coreThread, int maxThreads) {
        this(coreThread, maxThreads, maxThreads)
    }

    StandardThreadExecutor(int coreThread, int maxThreads, long keepAliveTime, TimeUnit unit) {
        this(coreThread, maxThreads, keepAliveTime, unit, maxThreads)
    }

    StandardThreadExecutor(int coreThreads, int maxThreads, int queueCapacity) {
        this(coreThreads, maxThreads, queueCapacity, Executors.defaultThreadFactory())
    }

    StandardThreadExecutor(int coreThreads, int maxThreads, int queueCapacity, ThreadFactory threadFactory) {
        this(coreThreads, maxThreads, DEFAULT_MAX_IDLE_TIME, TimeUnit.MILLISECONDS, queueCapacity, threadFactory)
    }

    StandardThreadExecutor(int coreThreads, int maxThreads, long keepAliveTime, TimeUnit unit, int queueCapacity) {
        this(coreThreads, maxThreads, keepAliveTime, unit, queueCapacity, Executors.defaultThreadFactory())
    }

    StandardThreadExecutor(int coreThreads, int maxThreads, long keepAliveTime, TimeUnit unit,
                           int queueCapacity, ThreadFactory threadFactory) {
        this(coreThreads, maxThreads, keepAliveTime, unit, queueCapacity, threadFactory, new AbortPolicy())
    }

    StandardThreadExecutor(int coreThreads, int maxThreads, long keepAliveTime, TimeUnit unit,
                           int queueCapacity, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        super(coreThreads, maxThreads, keepAliveTime, unit, new ExecutorQueue(), threadFactory, handler)
        ((ExecutorQueue) getQueue()).setStandardThreadExecutor(this)

        submittedTasksCount = new AtomicInteger(0)
        maxSubmittedTaskCount = queueCapacity + maxThreads
    }

    void execute(Runnable command) {
        int count = submittedTasksCount.incrementAndGet()

        if (count > maxSubmittedTaskCount) {
            submittedTasksCount.decrementAndGet()
            getRejectedExecutionHandler().rejectedExecution(command, this)
        }

        try {
            super.execute(command)
        } catch (RejectedExecutionException rx) {
            if (!((ExecutorQueue) getQueue()).force(command)) {
                submittedTasksCount.decrementAndGet()
                getRejectedExecutionHandler().rejectedExecution(command, this)
            }
        }
    }

    int getSubmittedTasksCount() {
        return this.submittedTasksCount.get()
    }

    int getMaxSubmittedTaskCount() {
        return maxSubmittedTaskCount
    }

    protected void afterExecute(Runnable r, Throwable t) {
        submittedTasksCount.decrementAndGet()
    }
}

@CompileStatic
class ExecutorQueue extends LinkedTransferQueue<Runnable> {
    StandardThreadExecutor threadPoolExecutor

    ExecutorQueue() {
        super()
    }

    void setStandardThreadExecutor(StandardThreadExecutor threadPoolExecutor) {
        this.threadPoolExecutor = threadPoolExecutor
    }

    // from tomcat
    boolean force(Runnable o) {
        if (threadPoolExecutor.isShutdown()) {
            throw new RejectedExecutionException("Executor not running, can't force a command into the queue")
        }
        // forces the item onto the queue, to be used if the task is rejected
        return super.offer(o)
    }

    boolean offer(Runnable o) {
        int poolSize = threadPoolExecutor.getPoolSize()

        // we are maxed out on threads, simply queue the object
        if (poolSize == threadPoolExecutor.getMaximumPoolSize()) {
            return super.offer(o)
        }
        // we have idle threads, just add it to the queue
        // note that we don't use getActiveCount(), see BZ 49730
        if (threadPoolExecutor.getSubmittedTasksCount() <= poolSize) {
            return super.offer(o)
        }
        // if we have less threads than maximum force creation of a new
        // thread
        if (poolSize < threadPoolExecutor.getMaximumPoolSize()) {
            return false
        }
        // if we reached here, we need to add it to the queue
        return super.offer(o)
    }
}
