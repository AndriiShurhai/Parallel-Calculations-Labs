import java.util.Random;

public class ThreadMin {

    private static final int DIM = 100_000_000;
    private static final int THREAD_COUNT = 3;

    private final int[] arr = new int[DIM];

    private long globalMin = Long.MAX_VALUE;
    private int globalMinIndex = -1;

    private final Object minLock = new Object();
    private final Object countLock = new Object();
    private int finishedThreads = 0;

    public static void main(String[] args) {
        ThreadMin program = new ThreadMin();
        program.initArr();

        program.measureAndPrint("Sequential min", () -> program.partMin(0, DIM));

        program.globalMin = Long.MAX_VALUE; // reset before parallel run
        program.globalMinIndex = -1;
        program.measureAndPrint("Parallel min  ", program::parallelMin);
    }

    @FunctionalInterface
    interface MinOperation {
        MinResult run();
    }

    private void measureAndPrint(String label, MinOperation operation) {
        long start = System.currentTimeMillis();
        MinResult result = operation.run();
        long elapsed = System.currentTimeMillis() - start;
        System.out.printf("%s: %d (index: %d)%n", label, result.value, result.index);
        System.out.printf("Result: %d milliseconds%n%n", elapsed);
    }

    // --- Initialization ---
    private void initArr() {
        Random rnd = new Random();
        for (int i = 0; i < DIM; i++)
            arr[i] = rnd.nextInt(DIM);

        // Plant a negative value at a random index
        int randomIndex = rnd.nextInt(DIM);
        arr[randomIndex] = -20;
    }

    // --- Sequential ---
    public MinResult partMin(int startIndex, int endIndex) {
        long localMin = Long.MAX_VALUE;
        int localMinIndex = startIndex;
        for (int i = startIndex; i < endIndex; i++) {
            if (arr[i] < localMin) {
                localMin = arr[i];
                localMinIndex = i;
            }
        }
        return new MinResult(localMin, localMinIndex);
    }

    // --- Parallel ---
    private MinResult parallelMin() {
        int chunkSize = DIM / THREAD_COUNT;

        for (int i = 0; i < THREAD_COUNT; i++) {
            int start = i * chunkSize;
            int end = (i == THREAD_COUNT - 1) ? DIM : start + chunkSize;

            Thread thread = new Thread(new ThreadWorker(start, end));
            thread.start();
        }

        // Wait for all threads to finish
        synchronized (countLock) {
            while (finishedThreads < THREAD_COUNT) {
                try {
                    countLock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        return new MinResult(globalMin, globalMinIndex);
    }

    // --- Thread Worker ---
    private class ThreadWorker implements Runnable {
        private final int start;
        private final int end;

        public ThreadWorker(int start, int end) {
            this.start = start;
            this.end = end;
        }

        @Override
        public void run() {
            MinResult localResult = partMin(start, end);

            // Update shared minimum
            synchronized (minLock) {
                if (localResult.value < globalMin) {
                    globalMin = localResult.value;
                    globalMinIndex = localResult.index;
                }
            }

            // Signal completion
            synchronized (countLock) {
                finishedThreads++;
                countLock.notify();
            }
        }
    }

    // --- Helper record-like class ---
    static class MinResult {
        final long value;
        final int index;

        MinResult(long value, int index) {
            this.value = value;
            this.index = index;
        }
    }
}