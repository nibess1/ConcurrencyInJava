import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class HotDogManager {

    // input variables
    private static int num_hotdogs;
    private static int num_slots;
    private static int num_making_machines;
    private static int num_packing_machines;

    // writing to log and an object to serve as a mutex to ensure proper writing
    private static BufferedWriter writer;
    private static Object writer_lock = new Object();

    public static void main(String[] args) {
        num_hotdogs = Integer.parseInt(args[0]);
        num_slots = Integer.parseInt(args[1]);
        num_making_machines = Integer.parseInt(args[2]);
        num_packing_machines = Integer.parseInt(args[3]);

        // initial logs
        try {
            writer = new BufferedWriter(new FileWriter("log.txt"));
            writer.write("order:" + num_hotdogs);
            writer.newLine();
            writer.write("capacity:" + num_slots);
            writer.newLine();
            writer.write("making machines:" + num_making_machines);
            writer.newLine();
            writer.write("packing machines:" + num_packing_machines);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        final Buffer buffer = new Buffer(num_slots);

        // Making machine array to get summary
        final MakingMachine[] makers = new MakingMachine[num_making_machines];

        // Producer threading
        final Thread[] producer = new Thread[num_making_machines];
        for (int i = 0; i < num_making_machines; i++) {
            MakingMachine m = new MakingMachine();
            makers[m.get_machine_idx()] = m;
            Thread t = new Thread(() -> {
                // keep making new hot dogs until the cap is reached
                while (MakingMachine.hotdog_id < num_hotdogs) {
                    final Hotdog hotdog = m.make_hot_dog();
                    buffer.put(hotdog, writer, writer_lock);
                }
            });
            producer[i] = t;
        }

        // Packing machine array to track summary
        final PackingMachine[] packers = new PackingMachine[num_packing_machines];

        // consumer threading and logic
        final Thread[] consumer = new Thread[num_packing_machines];
        for (int i = 0; i < num_packing_machines; i++) {
            PackingMachine p = new PackingMachine();
            packers[p.get_machine_idx()] = p;
            final Thread t = new Thread(() -> {
                while (PackingMachine.latest_hotdog < num_hotdogs) {
                    final Hotdog hotdog = buffer.get();
                    // returns null if entered while loop after all the hotdogs have already been
                    // cleared
                    if (hotdog == null) {
                        break;
                    }
                    p.pack(hotdog, writer, writer_lock);
                    // System.out.println("packed " + hotdog+ " by " + p.get_packing_machine_id());
                }
            });
            consumer[i] = t;
        }

        ArrayList<Thread> threads = new ArrayList<>(num_making_machines + num_packing_machines);
        for (Thread t : producer) {
            threads.add(t);
        }
        for (Thread t : consumer) {
            threads.add(t);
        }
        // start all the threads together, might not be necessary
        for (Thread thread : threads) {
            thread.start();
        }

        // join the producer threads first to signify completion of production
        for (Thread thread : producer) {
            try {
                thread.join();
                buffer.productionComplete = true;
                synchronized (buffer) {
                    buffer.notifyAll(); // wake up any waiting consumers
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // join the consumer threads before printing out summary
        for (Thread thread : consumer) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // print out summary and close the buffered writer
        try {

            writer.write("summary:");
            writer.newLine();
            for (MakingMachine m : makers) {
                writer.write(m.get_making_machine_id() + " made " + m.get_made_count());
                writer.newLine();
            }

            for (PackingMachine p : packers) {
                writer.write(p.get_packing_machine_id() + " packed " + p.get_packed_count());
                writer.newLine();
            }

            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class MakingMachine {

    // id generation performed using a lock to prevent multiple machines with same
    // id
    public static volatile int id_gen = 1;
    private static final Object lock = new Object();

    // hot dog id generation
    public static volatile int hotdog_id = 0;
    private static final Object hotdog_lock = new Object();

    // local variables
    private String making_machine_id;
    //machine_idx used so that summary array is consistent
    private int machine_idx;
    private int made_count = 0;

    public MakingMachine() {
        synchronized (lock) {
            machine_idx = id_gen - 1;
            making_machine_id = "m" + id_gen++;
            // System.out.println(making_machine_id + "machine started");

        }
    }

    public int get_machine_idx() {
        return this.machine_idx;
    }

    public int get_made_count() {
        return made_count;
    }

    public String get_making_machine_id() {
        return making_machine_id;
    }

    public Hotdog make_hot_dog() {
        // 1. Ensure no 2 hotdogs can have the same id
        Integer hd_id;
        synchronized (hotdog_lock) {
            hd_id = hotdog_id++;
        }
        made_count++;
        // 2. make hot dog -> 4 units of time
        Worker.doWork(WorkUnit.TIME_TO_MAKE);

        return new Hotdog(hd_id, making_machine_id);
    }

}

class PackingMachine {

    private String packing_machine_id;
    private int machine_idx;

    // id generation performed using a lock to prevent multiple machines with same
    // id

    private static volatile int id_gen = 1;
    private static final Object lock = new Object();

    // track the latest hotdog finished packing
    public static volatile int latest_hotdog = 0;
    private static final Object hotdog_lock = new Object();

    private int packed_count = 0;

    public PackingMachine() {
        synchronized (lock) {
            machine_idx = id_gen - 1;
            packing_machine_id = "p" + id_gen++;
            // System.out.println(packing_machine_id + "machine started");
        }
    }

    public int get_machine_idx() {
        return this.machine_idx;
    }

    public int get_packed_count() {
        return packed_count;
    }

    public String get_packing_machine_id() {
        return packing_machine_id;
    }

    public void pack(Hotdog hotdog, BufferedWriter writer, Object writer_lock) {
        // 1. Check if any hotdogs in the buffer: performed in buffer.get()
        // 2. take 1 hotdog from the buffer -> 1 unit of time: performed in buffer.get()
        // 3. perform pack -> 2 unit of time
        // 4. add to log
        Worker.doWork(WorkUnit.TIME_TO_PACK);
        synchronized (hotdog_lock) {
            latest_hotdog++;
        }
        packed_count++;
        write_record(hotdog, writer, writer_lock);
    }

    private void write_record(Hotdog hotdog, BufferedWriter writer, Object lock) {
        try {
            synchronized (lock) {
                writer.write(packing_machine_id + " gets " + hotdog.get_hotdog_id() + " from " + hotdog.get_maker_id());
                writer.newLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

class Buffer {

    private static volatile Hotdog[] buffer;

    private static volatile int front = 0;

    private static volatile int back = 0;

    private static volatile int itemCount = 0;

    public volatile boolean productionComplete = false;

    // private static int put_count = 0;
    // private static int get_count = 0;

    Buffer(int num_slots) {
        buffer = new Hotdog[num_slots];
    }

    private void write_record(Hotdog hotdog, BufferedWriter writer, Object lock) {
        try {
            synchronized (lock) {
                writer.write(hotdog.get_maker_id() + " puts " + hotdog.get_hotdog_id());
                writer.newLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    synchronized void put(Hotdog hotdog, BufferedWriter writer, Object writerLock) {
        while (itemCount == buffer.length) {
            try {
                this.wait();
            } catch (InterruptedException e) {
            }
        }

        Worker.doWork(WorkUnit.TIME_TO_SEND);
        buffer[back] = hotdog;
        back = (back + 1) % buffer.length;
        itemCount++;
        write_record(hotdog, writer, writerLock);
        // System.out.println("total added " + put_count++);
        this.notifyAll();
    }

    synchronized Hotdog get() {
        while (itemCount == 0) {
            if (productionComplete) {
                return null;
            }
            try {
                this.wait();
            } catch (InterruptedException e) {
            }
        }
        Worker.doWork(WorkUnit.TIME_TO_TAKE);
        final Hotdog hotdog = buffer[front];
        front = (front + 1) % buffer.length;
        itemCount--;
        // System.out.println("total taken " + get_count++);

        this.notifyAll();
        return hotdog;
    }
}

// Hotdog class contains maker id for easier logging
class Hotdog {

    private int id;
    private String making_machine_id;

    public Hotdog(int id, String making_machine_id) {
        this.id = id;
        this.making_machine_id = making_machine_id;
    }

    public String get_maker_id() {
        return making_machine_id;
    }

    public int get_hotdog_id() {
        return id;
    }

    @Override
    public String toString() {
        return "Hotdog [id=" + id + "]";
    }
}

// A more reasonable way to manager work and time units compared to having
// random constants thrown around
enum WorkUnit {
    TIME_TO_TAKE(1),
    TIME_TO_SEND(1),
    TIME_TO_PACK(2),
    TIME_TO_MAKE(4);

    public final int timeTaken;

    WorkUnit(int timeTaken) {
        this.timeTaken = timeTaken;
    }

}

class Worker {
    private static final long limit = 300_000_000;

    public static void doWork(WorkUnit u) {
        for (int i = 0; i < u.timeTaken; i++) {
            long m = limit;
            while (m > 0) {
                m--;
            }
        }
    }

}
