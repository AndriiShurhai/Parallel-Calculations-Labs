class Table {
    private final boolean[] isEating = new boolean[5];

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
    private final int id;

    public Philosopher(int id, Table table) {
        this.id = id;
        this.table = table;
        start();
    }

    @Override
    public void run() {
        for (int i = 0; i < 10; i++) {
            System.out.println("Philosopher " + id + " is thinking " + (i + 1) + " times");
            table.askWaiter(id);
            System.out.println("Philosopher " + id + " is eating " + (i + 1) + " times");
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