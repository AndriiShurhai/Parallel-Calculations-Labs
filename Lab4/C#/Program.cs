using System;
using System.Threading;

class Table {
    private Semaphore[] forks = new Semaphore[5];

    public Table() {
        for (int i = 0; i < 5; i++) {
            forks[i] = new Semaphore(1, 1);
        }
    }

    public void GetFork(int id) {
        forks[id].WaitOne();
    }

    public void PutFork(int id) {
        forks[id].Release();
    }
}

class Philosopher {
    private int id, firstFork, secondFork;
    private Table table;

    public Philosopher(int id, Table table) {
        this.id = id;
        this.table = table;
        
        int rightFork = id;
        int leftFork = (id + 1) % 5;

        if (id == 4) {
            firstFork = leftFork;
            secondFork = rightFork;
        } else {
            firstFork = rightFork;
            secondFork = leftFork;
        }
        new Thread(Run).Start();
    }

    public void Run() {
        for (int i = 0; i < 10; i++) {
            Console.WriteLine($"Philosopher {id} is thinking {i + 1} times");
            table.GetFork(firstFork);
            table.GetFork(secondFork);
            Console.WriteLine($"Philosopher {id} is eating {i + 1} times");
            table.PutFork(secondFork);
            table.PutFork(firstFork);
        }
    }
}

class Program {
    static void Main() {
        Table table = new Table();
        for (int i = 0; i < 5; i++) {
            new Philosopher(i, table);
        }
    }
}