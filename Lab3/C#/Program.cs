using System;
using System.Collections.Generic;
using System.Threading;

namespace ProducerConsumer
{
    class Program
    {
        // --- Config ---
        private const int StorageSize   = 3;
        private const int TotalItems    = 12;
        private const int ProducerCount = 3;
        private const int ConsumerCount = 2;

        // --- Semaphores (matching reference naming) ---
        private Semaphore Access;  // mutual exclusion (binary)
        private Semaphore Full;    // free slots  (starts at StorageSize)
        private Semaphore Empty;   // filled slots (starts at 0)

        // --- Shared storage ---
        private readonly List<string> storage = new List<string>();

        // --- Shared item counter ---
        private int nextItem = 0;
        private readonly object counterLock = new object();

        static void Main(string[] args)
        {
            new Program().Starter(StorageSize, TotalItems);
            Console.ReadKey();
        }

        private void Starter(int storageSize, int totalItems)
        {
            Access = new Semaphore(1, 1);
            Full   = new Semaphore(storageSize, storageSize);
            Empty  = new Semaphore(0, storageSize);

            // Distribute items among producers and consumers
            int pBase = totalItems / ProducerCount;
            int pRem  = totalItems % ProducerCount;
            int cBase = totalItems / ConsumerCount;
            int cRem  = totalItems % ConsumerCount;

            var threads = new Thread[ProducerCount + ConsumerCount];

            for (int i = 0; i < ProducerCount; i++)
            {
                int share = pBase + (i == ProducerCount - 1 ? pRem : 0);
                int id    = i + 1;
                threads[i] = new Thread(Producer);
                threads[i].Name = $"Producer {id}";
                threads[i].Start(share);
            }

            for (int i = 0; i < ConsumerCount; i++)
            {
                int share = cBase + (i == ConsumerCount - 1 ? cRem : 0);
                int id    = i + 1;
                threads[ProducerCount + i] = new Thread(Consumer);
                threads[ProducerCount + i].Name = $"Consumer {id}";
                threads[ProducerCount + i].Start(share);
            }

            foreach (var t in threads)
                t.Join();

            Console.WriteLine("\nAll done.");
        }

        private void Producer(object itemCount)
        {
            int count = (int)itemCount;
            string name = Thread.CurrentThread.Name;

            for (int i = 0; i < count; i++)
            {
                int item;
                lock (counterLock) { item = ++nextItem; }

                Full.WaitOne();    // wait for a free slot
                Access.WaitOne();  // enter critical section

                storage.Add("item " + item);
                Console.WriteLine($"  {name} added item {item,3}  | storage: [{string.Join(", ", storage)}]");

                Access.Release();  // leave critical section
                Empty.Release();   // signal: one more item ready
            }
        }

        private void Consumer(object itemCount)
        {
            int count = (int)itemCount;
            string name = Thread.CurrentThread.Name;

            for (int i = 0; i < count; i++)
            {
                Empty.WaitOne();   // wait for an available item
                Access.WaitOne();  // enter critical section

                string item = storage[0];
                storage.RemoveAt(0);
                Console.WriteLine($"Consumer {name} took  {item,8}  | storage: [{string.Join(", ", storage)}]");

                Access.Release();  // leave critical section
                Full.Release();    // signal: one more slot free
            }
        }
    }
}