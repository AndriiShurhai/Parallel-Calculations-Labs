import java.util.LinkedList;
import java.util.concurrent.Semaphore;

public class ProducerConsumer {

    // --- Config ---
    private static final int STORAGE_SIZE   = 3;
    private static final int TOTAL_ITEMS    = 12;
    private static final int PRODUCER_COUNT = 3;
    private static final int CONSUMER_COUNT = 2;

    // --- Semaphores (matching reference naming) ---
    private static final Semaphore access = new Semaphore(1);                    // mutual exclusion
    private static final Semaphore full   = new Semaphore(STORAGE_SIZE);         // free slots
    private static final Semaphore empty  = new Semaphore(0);                    // filled slots

    // --- Shared storage ---
    private static final LinkedList<String> storage = new LinkedList<>();

    // --- Shared item counter ---
    private static int nextItem = 0;
    private static final Object counterLock = new Object();

    public static void main(String[] args) throws InterruptedException {
        int pBase = TOTAL_ITEMS / PRODUCER_COUNT;
        int pRem  = TOTAL_ITEMS % PRODUCER_COUNT;
        int cBase = TOTAL_ITEMS / CONSUMER_COUNT;
        int cRem  = TOTAL_ITEMS % CONSUMER_COUNT;

        Thread[] threads = new Thread[PRODUCER_COUNT + CONSUMER_COUNT];

        for (int i = 0; i < PRODUCER_COUNT; i++) {
            int share = pBase + (i == PRODUCER_COUNT - 1 ? pRem : 0);
            int id    = i + 1;
            threads[i] = new Thread(() -> produce(id, share), "Producer " + id);
            threads[i].start();
        }

        for (int i = 0; i < CONSUMER_COUNT; i++) {
            int share = cBase + (i == CONSUMER_COUNT - 1 ? cRem : 0);
            int id    = i + 1;
            threads[PRODUCER_COUNT + i] = new Thread(() -> consume(id, share), "Consumer " + id);
            threads[PRODUCER_COUNT + i].start();
        }

    }

    private static void produce(int id, int count) {
        for (int i = 0; i < count; i++) {
            int item;
            synchronized (counterLock) { item = ++nextItem; }

            try {
                full.acquire();    // wait for a free slot
                access.acquire();  // enter critical section

                storage.add("item " + item);
                System.out.printf("  Producer %d added item %3d  | storage: %s%n",
                        id, item, storage);

                access.release();  // leave critical section
                empty.release();   // signal: one more item ready

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static void consume(int id, int count) {
        for (int i = 0; i < count; i++) {
            try {
                empty.acquire();   // wait for an available item
                access.acquire();  // enter critical section

                String item = storage.removeFirst();
                System.out.printf("Consumer %d took  %-8s  | storage: %s%n",
                        id, item, storage);

                access.release();  // leave critical section
                full.release();    // signal: one more slot free

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}