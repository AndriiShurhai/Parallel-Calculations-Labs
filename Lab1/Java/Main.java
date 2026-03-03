package Lab1.Java;
public class Main {

    static final int  NUM_THREADS = 10;
    static final int  STEP_BASE   = 5;
    static final long TIME_MS     = 1000;

    static class WorkerThread extends Thread {
        private final int id;
        private final int step;
        private volatile boolean running = true;
        private long sum   = 0;
        private long count = 0;

        WorkerThread(int id, int step) {
            this.id   = id;
            this.step = step;
        }

        @Override
        public void run() {
            long value = 0;
            while (running) {
                sum += value;
                value += step;
                count++;
            }
            System.out.printf(
                "Thread %d finished: sum = %d, elements count = %d%n",
                id, sum, count
            );
        }

        public void stopThread() {
            running = false;
        }
    }

    public static void main(String[] args) throws InterruptedException {

        WorkerThread[] threads = new WorkerThread[NUM_THREADS];

        for (int i = 0; i < NUM_THREADS; i++) {
            threads[i] = new WorkerThread(i + 1, STEP_BASE * (i + 1));
            threads[i].start();
            System.out.printf("Thread %d launched (step = %d)%n",
                              i + 1, STEP_BASE * (i + 1));
        }

        Thread controller = new Thread(() -> {
            for (int i = 0; i < NUM_THREADS; i++) {
                try {
                    Thread.sleep(TIME_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                threads[i].stopThread();
                try {
                    threads[i].join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            System.out.println("All threads are finished.");
        });
        controller.setName("Controller");
        controller.start();

        // Main thread simply waits for the controller to complete
        controller.join();
    }
}