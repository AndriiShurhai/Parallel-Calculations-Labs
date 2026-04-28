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
        private long _globalMinIndex = -1;
        private int _finishedThreads = 0;

        private readonly object _minLock = new();
        private readonly object _countLock = new();

        static void Main(string[] args)
        {
            var program = new Program();
            program.InitArr();

            MeasureAndPrint("Sequential min", () => program.PartMin(0, Dim));
            
            program._globalMin = long.MaxValue; 
            MeasureAndPrint("Parallel min  ", () => program.ParallelMin());

            Console.WriteLine("Press any key to exit...");
            Console.ReadKey();
        }

        private static void MeasureAndPrint(string label, Func<(long Value, long Index)> operation)
        {
            var sw = Stopwatch.StartNew();
            var result = operation();
            sw.Stop();
            
            Console.WriteLine($"{label}: {result.Value} at index: {result.Index}");
            Console.WriteLine($"Elapsed: {sw.ElapsedMilliseconds} ms\n");
        }

        private void InitArr()
        {
            var rnd = new Random();
            for (int i = 0; i < Dim; i++)
                _arr[i] = rnd.Next(0, Dim);

            _arr[500] = -20; 
        }

        public (long Value, long Index) PartMin(int startIndex, int endIndex)
        {
            long localMin = long.MaxValue;
            long localMinIndex = -1;
            for (int i = startIndex; i < endIndex; i++)
            {
                if (_arr[i] < localMin)
                {
                    localMin = _arr[i];
                    localMinIndex = i;
                }
            }
            return (localMin, localMinIndex);
        }

        private (long Value, long Index) ParallelMin()
        {
            int chunkSize = Dim / ThreadCount;
            _finishedThreads = 0; // Reset counter

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

            return (_globalMin, _globalMinIndex);
        }

        private void ThreadWorker(object? param)
        {
            if (param is not Bounds bounds) return;

            var result = PartMin(bounds.Start, bounds.End);

            lock (_minLock)
            {
                if (result.Value < _globalMin)
                {
                    _globalMin = result.Value;
                    _globalMinIndex = result.Index;
                }
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