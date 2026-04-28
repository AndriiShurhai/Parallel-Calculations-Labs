using System;
using System.Threading;

class Table {
    private bool[] isEating = new bool[5];
    private Semaphore[] forks = new Semaphore[5];
    private readonly object _lock = new object();

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

    public void AskWaiter(int id) {
        int leftNeighbor = (id + 4) % 5;
        int rightNeighbor = (id + 1) % 5;

        lock (_lock) {
            while (isEating[leftNeighbor] || isEating[rightNeighbor]) {
                Monitor.Wait(_lock);
            }
            isEating[id] = true; 
        }
    }

    public void LeaveWaiter(int id) {
        lock (_lock) {
            isEating[id] = false;
            Monitor.PulseAll(_lock);
        }
    }
}

class Philosopher {
    private int id, leftFork, rightFork;
    private Table table;

    public Philosopher(int id, Table table) {
        this.id = id;
        this.table = table;

        leftFork = id;
        rightFork = (id + 1) % 5;

        new Thread(Run).Start();
    }

    public void Run() {
        for (int i = 0; i < 10; i++) {
            Console.WriteLine($"Philosopher {id} is thinking {i + 1} times");
            
            table.AskWaiter(id);
            
            table.GetFork(leftFork);
            table.GetFork(rightFork);
            
            Console.WriteLine($"Philosopher {id} is eating {i + 1} times");
            
            table.PutFork(leftFork);
            table.PutFork(rightFork);

            table.LeaveWaiter(id);
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