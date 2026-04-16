using System;
using System.Threading;

class Table {
    private bool[] isEating = new bool[5];
    private readonly object _lock = new object();

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
    private int id;
    private Table table;

    public Philosopher(int id, Table table) {
        this.id = id;
        this.table = table;
        new Thread(Run).Start();
    }

    public void Run() {
        for (int i = 0; i < 10; i++) {
            Console.WriteLine($"Philosopher {id} is thinking {i + 1} times");
            
            table.AskWaiter(id);
            
            Console.WriteLine($"Philosopher {id} is eating {i + 1} times");
            
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