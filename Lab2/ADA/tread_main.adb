with Ada.Text_IO;           use Ada.Text_IO;
with Ada.Numerics.Discrete_Random;
with Ada.Real_Time;         use Ada.Real_Time;

procedure Thread_Min is

   -- ── Constants ──────────────────────────────────────────────────────────
   Dim          : constant := 10_000_000;
   Thread_Count : constant := 5;

   -- ── Array ──────────────────────────────────────────────────────────────
   --  Ada arrays are declared with a subtype as the index.
   --  This means the index is guaranteed to always be in range — no out-of-bounds.
   subtype Index_T is Integer range 0 .. Dim - 1;
   type    Arr_T   is array (Index_T) of Integer;
   Arr : Arr_T;

   -- ── Random number generator ────────────────────────────────────────────
   --  Ada's random packages are generic — you must instantiate one
   --  for the specific type you want random values of.
   package Rand is new Ada.Numerics.Discrete_Random (Index_T);
   Gen : Rand.Generator;

   -- ── Protected object: shared minimum ──────────────────────────────────
   --  A protected object is Ada's monitor/mutex.
   --  - procedures are exclusive (only one caller at a time, like lock{})
   --  - functions are read-only and can run concurrently with each other
   protected Shared_Min is
      procedure Update (Value : Integer; Idx : Index_T);
      function  Get_Value return Integer;
      function  Get_Index return Integer;
   private
      Min_Val : Integer := Integer'Last;  -- Integer'Last = int.MaxValue
      Min_Idx : Integer := -1;
   end Shared_Min;

   protected body Shared_Min is
      procedure Update (Value : Integer; Idx : Index_T) is
      begin
         if Value < Min_Val then
            Min_Val := Value;
            Min_Idx := Integer (Idx);
         end if;
      end Update;

      -- Ada 2012 expression function shorthand (like => in C#)
      function Get_Value return Integer is (Min_Val);
      function Get_Index return Integer is (Min_Idx);
   end Shared_Min;

   -- ── Protected object: completion counter ───────────────────────────────
   --  An "entry" is like a procedure, but with a BARRIER — a condition that
   --  must be true before the caller is allowed in. If false, the caller
   --  blocks automatically (like Monitor.Wait) and is re-checked whenever
   --  any protected procedure runs and might have changed the condition.
   protected Counter is
      procedure Increment;
      entry     Wait_For_All;           -- blocks until Count = Thread_Count
   private
      Count : Integer := 0;
   end Counter;

   protected body Counter is
      procedure Increment is
      begin
         Count := Count + 1;
      end Increment;

      entry Wait_For_All when Count >= Thread_Count is
      begin
         null;                          -- nothing to do, barrier does the work
      end Wait_For_All;
   end Counter;

   -- ── Worker task type ───────────────────────────────────────────────────
   --  "task type" is like a Thread class — you can create multiple instances.
   --
   --  "entry Set_Bounds" is a RENDEZVOUS point:
   --    - the task body blocks at "accept Set_Bounds" waiting for a caller
   --    - the main task calls Workers(I).Set_Bounds(S, E) to hand off data
   --    - both sides meet, data is transferred, then both continue
   --  This replaces the constructor / Thread.Start(param) pattern.
   task type Worker is
      entry Set_Bounds (S : Integer; E : Integer);
   end Worker;

   task body Worker is
      Start_Idx : Integer;
      End_Idx   : Integer;
      Local_Min : Integer := Integer'Last;
      Local_Idx : Integer;
   begin
      -- Block here and wait for the main task to call Set_Bounds.
      -- The "do...end" block runs while both sides are synchronized.
      accept Set_Bounds (S : Integer; E : Integer) do
         Start_Idx := S;
         End_Idx   := E;
      end Set_Bounds;

      -- After rendezvous, run independently — no sharing here
      Local_Idx := Start_Idx;
      for I in Start_Idx .. End_Idx - 1 loop
         if Arr (I) < Local_Min then
            Local_Min := Arr (I);
            Local_Idx := I;
         end if;
      end loop;

      -- Write to shared state through the protected object (like lock{})
      Shared_Min.Update (Local_Min, Local_Idx);

      -- Signal completion (like Monitor.Pulse)
      Counter.Increment;
   end Worker;

   -- Declaring the array HERE causes all tasks to start immediately.
   -- They each run to "accept Set_Bounds" and wait there.
   Workers : array (0 .. Thread_Count - 1) of Worker;

   -- ── Other locals ───────────────────────────────────────────────────────
   Chunk_Size  : constant Integer := Dim / Thread_Count;
   Start_Time  : Time;
   Elapsed     : Time_Span;
   Seq_Min     : Integer := Integer'Last;
   Seq_Min_Idx : Integer := 0;

begin

   -- ── Initialize array ───────────────────────────────────────────────────
   Rand.Reset (Gen);
   for I in Index_T loop
      Arr (I) := Rand.Random (Gen);     -- random value in 0 .. Dim-1
   end loop;
   Arr (Rand.Random (Gen)) := -20;      -- plant negative at a random position

   -- ── Sequential ─────────────────────────────────────────────────────────
   Start_Time := Clock;
   for I in Index_T loop
      if Arr (I) < Seq_Min then
         Seq_Min     := Arr (I);
         Seq_Min_Idx := Integer (I);
      end if;
   end loop;
   Elapsed := Clock - Start_Time;

   Put_Line ("Sequential min: " & Integer'Image (Seq_Min) &
             "  (index:" & Integer'Image (Seq_Min_Idx) & ")");
   Put_Line ("Result:" &
             Integer'Image (Integer (To_Duration (Elapsed) * 1000.0)) &
             " milliseconds");
   New_Line;

   -- ── Parallel ───────────────────────────────────────────────────────────
   Start_Time := Clock;

   -- Hand bounds to each worker via rendezvous.
   -- Each call unblocks one waiting task and gives it its slice.
   for I in 0 .. Thread_Count - 1 loop
      declare
         S : constant Integer := I * Chunk_Size;
         E : constant Integer :=
               (if I = Thread_Count - 1 then Dim else S + Chunk_Size);
      begin
         Workers (I).Set_Bounds (S, E);
      end;
   end loop;

   -- Block here until all workers have called Counter.Increment.
   -- The barrier "when Count >= Thread_Count" handles the waking automatically.
   Counter.Wait_For_All;
   Elapsed := Clock - Start_Time;

   Put_Line ("Parallel min:   " & Integer'Image (Shared_Min.Get_Value) &
             "  (index:" & Integer'Image (Shared_Min.Get_Index) & ")");
   Put_Line ("Result:" &
             Integer'Image (Integer (To_Duration (Elapsed) * 1000.0)) &
             " milliseconds");

   -- Ada automatically waits for all tasks to fully terminate
   -- before allowing Thread_Min to exit. No explicit join needed.

end Thread_Min;