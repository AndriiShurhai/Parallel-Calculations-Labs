import java.util.concurrent.Semaphore;

class Table {
    private final Semaphore[] forks = new Semaphore[5];

    public Table() {
        for (int i = 0; i < forks.length; i++) {
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
}

class Philosopher extends Thread {
    private final Table table;
    private final int firstFork, secondFork;
    private final int id;

    public Philosopher(int id, Table table) {
        this.id = id;
        this.table = table;

        int rightFork = id;
        int leftFork = (id + 1) % 5;

        // Зміна порядку для останнього філософа
        if (id == 4) {
            firstFork = leftFork;
            secondFork = rightFork;
        } else {
            firstFork = rightFork;
            secondFork = leftFork;
        }
        start();
    }

    @Override
    public void run() {
        for (int i = 0; i < 10; i++) {
            System.out.println("Philosopher " + id + " is thinking " + (i + 1) + " times");
            table.getFork(firstFork);
            table.getFork(secondFork);
            System.out.println("Philosopher " + id + " is eating " + (i + 1) + " times");
            table.putFork(secondFork);
            table.putFork(firstFork);
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