import java.util.concurrent.Semaphore;

class Table {
    private final boolean[] isEating = new boolean[5];
    private final Semaphore[] forks = new Semaphore[5];

    public Table() {
        for (int i = 0; i < 5; i++) {
            forks[i] = new Semaphore(1);
        }
    }

    public void getFork(int id) {
        try {
            forks[id].acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void putFork(int id) {
        forks[id].release();
    }

    public synchronized void askWaiter(int id) {
        int leftNeighbor = (id + 4) % 5;
        int rightNeighbor = (id + 1) % 5;

        try {
            while (isEating[leftNeighbor] || isEating[rightNeighbor]) {
                wait(); 
            }
            isEating[id] = true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public synchronized void leaveWaiter(int id) {
        isEating[id] = false;
        notifyAll(); 
    }
}

class Philosopher extends Thread {
    private final Table table;
    private final int id, leftFork, rightFork;

    public Philosopher(int id, Table table) {
        this.id = id;
        this.table = table;
        this.leftFork = id;
        this.rightFork = (id + 1) % 5;
        start();
    }

    @Override
    public void run() {
        for (int i = 0; i < 10; i++) {
            System.out.println("Philosopher " + id + " is thinking " + (i + 1) + " times");
            
            table.askWaiter(id);
            
            table.getFork(leftFork);
            table.getFork(rightFork);
            
            System.out.println("Philosopher " + id + " is eating " + (i + 1) + " times");
            try { Thread.sleep(50); } catch (InterruptedException e) {} // Симуляція їжі
            
            table.putFork(leftFork);
            table.putFork(rightFork);
            
            table.leaveWaiter(id);
        }
    }
}

public class Main {
    public static void main(String[] args) {
        Table table = new Table();
        for (int i = 0; i < 5; i++) {
            new Philosopher(i, table);
        }
    }
}