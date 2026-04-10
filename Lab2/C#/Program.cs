using System;
using System.Diagnostics;
using System.Threading;

namespace ThreadMinSharp
{
    class Program
    {
        private const int Dim = 10_000_000;
        private const int ThreadCount = 3;

        private readonly int[] _arr = new int[Dim];
        private readonly Thread[] _threads = new Thread[ThreadCount];

        private long _globalMin = long.MaxValue;
        private int _finishedThreads = 0;

        private readonly object _minLock = new();
        private readonly object _countLock = new();

        static void Main(string[] args)
        {
            var program = new Program();
            program.InitArr();

            MeasureAndPrint("Sequential min", () => program.PartMin(0, Dim));
            MeasureAndPrint("Parallel min  ", () => program.ParallelMin());

            Console.ReadKey();
        }

        private static void MeasureAndPrint(string label, Func<long> operation)
        {
            var sw = Stopwatch.StartNew();
            long result = operation();
            sw.Stop();
            Console.WriteLine($"{label}: {result}");
            Console.WriteLine($"Result: {sw.ElapsedMilliseconds} milliseconds\n");
        }

        private void InitArr()
        {
            var rnd = new Random();
            for (int i = 0; i < Dim; i++)
                _arr[i] = rnd.Next(0, Dim);

            _arr[0] = -1;
            _arr[0] = -20;
        }

        public long PartMin(int startIndex, int endIndex)
        {
            long localMin = long.MaxValue;
            for (int i = startIndex; i < endIndex; i++)
            {
                if (_arr[i] < localMin)
                    localMin = _arr[i];
            }
            return localMin;
        }

        private long ParallelMin()
        {
            int chunkSize = Dim / ThreadCount;

            for (int i = 0; i < ThreadCount; i++)
            {
                int start = i * chunkSize;
                int end = (i == ThreadCount - 1) ? Dim : start + chunkSize;

                _threads[i] = new Thread(ThreadWorker);
                _threads[i].Start(new Bounds(start, end));
            }

            lock (_countLock)
            {
                while (_finishedThreads < ThreadCount)
                    Monitor.Wait(_countLock);
            }

            return _globalMin;
        }

        private void ThreadWorker(object param)
        {
            if (param is not Bounds bounds) return;

            long localMin = PartMin(bounds.Start, bounds.End);

            lock (_minLock)
            {
                if (localMin < _globalMin)
                    _globalMin = localMin;
            }

            lock (_countLock)
            {
                _finishedThreads++;
                Monitor.Pulse(_countLock);
            }
        }

        private record Bounds(int Start, int End);
    }
}