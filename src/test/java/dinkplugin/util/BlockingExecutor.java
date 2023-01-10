package dinkplugin.util;

import lombok.SneakyThrows;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class BlockingExecutor implements ScheduledExecutorService {
    private final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return schedule(() -> {
            command.run();
            return null;
        }, delay, unit);
    }

    @Override
    @SneakyThrows
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        ScheduledFuture<V> fut = exec.schedule(callable, delay, unit);
        fut.get();
        return fut;
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void shutdown() {
        exec.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return exec.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return exec.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return exec.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return exec.awaitTermination(timeout, unit);
    }

    @Override
    @SneakyThrows
    public <T> Future<T> submit(Callable<T> task) {
        Future<T> fut = exec.submit(task);
        fut.get();
        return fut;
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return submit(() -> {
            task.run();
            return result;
        });
    }

    @Override
    public Future<?> submit(Runnable task) {
        return submit(task, null);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return exec.invokeAll(tasks);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return exec.invokeAll(tasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return exec.invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return exec.invokeAny(tasks);
    }

    @Override
    public void execute(Runnable command) {
        submit(command);
    }
}
