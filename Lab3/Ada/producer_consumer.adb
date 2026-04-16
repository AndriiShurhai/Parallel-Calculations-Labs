with Ada.Text_IO;       use Ada.Text_IO;
with GNAT.Semaphores;   use GNAT.Semaphores;
with Ada.Containers.Indefinite_Doubly_Linked_Lists;
use Ada.Containers;

procedure Producer_Consumer is

   -- ── Config ─────────────────────────────────────────────────────────────
   Storage_Size   : constant := 3;
   Total_Items    : constant := 12;
   Producer_Count : constant := 3;
   Consumer_Count : constant := 2;

   -- ── Semaphores (matching reference naming) ─────────────────────────────
   Access_Storage : Counting_Semaphore (1, Default_Ceiling);
   Full_Storage   : Counting_Semaphore (Storage_Size, Default_Ceiling);
   Empty_Storage  : Counting_Semaphore (0, Default_Ceiling);

   -- ── Shared storage ─────────────────────────────────────────────────────
   package String_Lists is new Indefinite_Doubly_Linked_Lists (String);
   use String_Lists;
   Storage : List;

   -- ── Shared item counter ────────────────────────────────────────────────
   --  Protected object ensures only one task increments at a time.
   protected Counter is
      procedure Next (Item : out Integer);
   private
      Value : Integer := 0;
   end Counter;

   protected body Counter is
      procedure Next (Item : out Integer) is
      begin
         Value := Value + 1;
         Item  := Value;
      end Next;
   end Counter;

   -- ── Helper: compute per-task share ────────────────────────────────────
   function Share (Total : Integer; Parts : Integer; Index : Integer)
      return Integer is
   begin
      if Index = Parts then
         return Total / Parts + Total mod Parts;  -- last one gets remainder
      else
         return Total / Parts;
      end if;
   end Share;

   -- ── Producer task type ─────────────────────────────────────────────────
   task type Producer is
      entry Set_Params (Id : Integer; Count : Integer);
   end Producer;

   task body Producer is
      My_Id    : Integer;
      My_Count : Integer;
      Item     : Integer;
   begin
      accept Set_Params (Id : Integer; Count : Integer) do
         My_Id    := Id;
         My_Count := Count;
      end Set_Params;

      for I in 1 .. My_Count loop
         Counter.Next (Item);

         Full_Storage.Seize;        --  wait for a free slot
         Access_Storage.Seize;      --  enter critical section

         Storage.Append ("item " & Item'Img);
         Put_Line ("  Producer" & My_Id'Img &
                   " added item" & Item'Img &
                   "  | storage size:" & Integer'Image (Integer (Storage.Length)));

         Access_Storage.Release;    --  leave critical section
         Empty_Storage.Release;     --  signal: one more item ready
      end loop;
   end Producer;

   -- ── Consumer task type ─────────────────────────────────────────────────
   task type Consumer is
      entry Set_Params (Id : Integer; Count : Integer);
   end Consumer;

   task body Consumer is
      My_Id    : Integer;
      My_Count : Integer;
   begin
      accept Set_Params (Id : Integer; Count : Integer) do
         My_Id    := Id;
         My_Count := Count;
      end Set_Params;

      for I in 1 .. My_Count loop
         Empty_Storage.Seize;       --  wait for an available item
         Access_Storage.Seize;      --  enter critical section

         declare
            Item : constant String := First_Element (Storage);
         begin
            Storage.Delete_First;
            Put_Line ("Consumer" & My_Id'Img &
                      " took  " & Item &
                      "  | storage size:" & Integer'Image (Integer (Storage.Length)));
         end;

         Access_Storage.Release;    --  leave critical section
         Full_Storage.Release;      --  signal: one more slot free
      end loop;
   end Consumer;

   -- ── Task arrays ────────────────────────────────────────────────────────
   --  Tasks start running immediately when declared.
   --  They each block at "accept Set_Params" waiting for the main task.
   Producers : array (1 .. Producer_Count) of Producer;
   Consumers : array (1 .. Consumer_Count) of Consumer;

begin
   -- Hand each task its quota via rendezvous
   for I in 1 .. Producer_Count loop
      Producers (I).Set_Params (I, Share (Total_Items, Producer_Count, I));
   end loop;

   for I in 1 .. Consumer_Count loop
      Consumers (I).Set_Params (I, Share (Total_Items, Consumer_Count, I));
   end loop;

   -- Ada implicitly joins all tasks before this procedure exits.
   Put_Line ("Main: waiting for all tasks to finish...");

end Producer_Consumer;