using System;
using System.Threading;

class Program
{
    const int NUM_THREADS = 10;
    const int STEP_BASE = 5;
    const int TIME_MS = 1000;

    class WorkerThread
    {
        public readonly int Id;
        private readonly int step;
        private volatile bool running = true;
        private long sum = 0;
        private long count = 0;
        public readonly Thread Thread;

        public WorkerThread(int id, int step)
        {
            Id = id;
            this.step = step;
            Thread = new Thread(Run);
            Thread.Name = $"Worker--{id}";
        }

        private void Run()
        {
            long value = 0;
            while (running)
            {
                sum += value;
                value += step;
                count++;
            }
            Console.WriteLine($"Thread {Id} finished: sum = {sum}, elements count = {count}");
        }

        public void Start() => Thread.Start();

        public void Stop()
        {
            running = false;
            Thread.Join();
        }
    }

    public static void Main(string[] args)
    {
        var workers = new WorkerThread[NUM_THREADS];

        for (int i = 0; i < NUM_THREADS; i++)
        {
            workers[i] = new WorkerThread(i + 1, STEP_BASE * (i + 1));
            workers[i].Start();
            Console.WriteLine($"Thread {i + 1} launched (step = {STEP_BASE * (i + 1)})");
        }

        Thread controller = new Thread(() =>
        {
            for (int i = 0; i < NUM_THREADS; i++)
            {
                Thread.Sleep(TIME_MS);
                workers[i].Stop();
            }
            Console.WriteLine("All threads are finished");
        });
        controller.Name = "Controller";
        controller.Start();

        controller.Join();
    }
}