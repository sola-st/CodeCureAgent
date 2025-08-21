```java
package com.github.vincentrussell.query.mongodb.sql.converter;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * {@link InputStream} that will time out (return -1) if no data is returned
 * when calling read within the indicated timeout.
 */
public class TimeoutInputStream extends InputStream {

    private ExecutorService executorService = Executors.newFixedThreadPool(1);

    private final InputStream inputStream;
    private final long timeout;
    private final TimeUnit timeUnit;
    private boolean receivedData = false;


    /**
     * Default constructor.
     * @param inputStream the linked {@link InputStream}
     * @param timeout the timeout amount
     * @param timeUnit the unit for the timeout
     */
    public TimeoutInputStream(final InputStream inputStream, final long timeout, final TimeUnit timeUnit) {
        this.inputStream = inputStream;
        this.timeout = timeout;
        this.timeUnit = timeUnit;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int read() throws IOException {

        int result = -1;
        Future<Integer> future = executorService.submit(new ReadNext(inputStream));
        try {
            result = getInteger(future);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            result = -1;
        }

        receivedData = true;
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        }
        if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        }
        if (len == 0) {
            return 0;
        }
        int firstByte = read();
        if (firstByte == -1) {
            return -1;
        }
        b[off] = (byte) firstByte;
        int bytesRead = 1;
        try {
            for (; bytesRead < len; bytesRead++) {
                int nextByte = read();
                if (nextByte == -1) {
                    break;
                }
                b[off + bytesRead] = (byte) nextByte;
            }
        } catch (IOException e) {
            // Return bytes read so far before the exception
        }
        return bytesRead;
    }

    private Integer getInteger(final Future<Integer> future)
            throws InterruptedException, ExecutionException, TimeoutException {
        if (!receivedData) {
            return future.get(1, TimeUnit.MINUTES);
        } else {
            return future.get(timeout, timeUnit);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        executorService.shutdownNow();
    }

    private static final class ReadNext implements Callable<Integer> {

        private final InputStream inputStream;

        private ReadNext(final InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public Integer call() throws Exception {
            return inputStream.read();
        }
    }
}
