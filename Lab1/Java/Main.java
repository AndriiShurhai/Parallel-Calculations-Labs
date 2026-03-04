package Lab1.Java;

import java.util.*;

public class Main {
    static final int NUM_THREADS = 10;
    static final int STEP_BASE = 5;

    static class WorkerThread {
        public final int id;
        private final int step;
        private volatile boolean running = true;
        private long sum = 0;
        private long count = 0;
        public final Thread thread;

        public WorkerThread(int id, int step) {
            this.id = id;
            this.step = step;
            this.thread = new Thread(this::run, "Worker-" + id);
        }

        private void run() {
            long value = 0;
            while (running) {
                sum += value;
                value += step;
                count++;
            }
            System.out.println("Thread " + id + " finished: sum = " + sum + ", elements count = " + count);
        }

        public void start() { thread.start(); }

        public void stop() throws InterruptedException {
            running = false;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        WorkerThread[] workers = new WorkerThread[NUM_THREADS];
        Map<Integer, Integer> order = new HashMap<>(); // worker index -> duration
        Random random = new Random();

        for (int i = 0; i < NUM_THREADS; i++) {
            workers[i] = new WorkerThread(i + 1, STEP_BASE * (i + 1));
            workers[i].start();

            int duration = random.nextInt(9000) + 1000;
            order.put(i, duration);
            System.out.println("Thread " + (i + 1) + " launched (step = " + (STEP_BASE * (i + 1)) + ", duration = " + duration + "ms)");
        }

        List<Map.Entry<Integer, Integer>> sorted = new ArrayList<>(order.entrySet());
        sorted.sort(Map.Entry.comparingByValue());

        WorkerThread[] finalWorkers = workers;
        Thread controller = new Thread(() -> {
            int elapsed = 0;
            try {
                for (Map.Entry<Integer, Integer> entry : sorted) {
                    Thread.sleep(entry.getValue() - elapsed);
                    elapsed = entry.getValue();
                    finalWorkers[entry.getKey()].stop();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        controller.setName("Controller");
        controller.start();
    }
}