import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class HotDogManager {

    // input variables
    private static int num_hotdogs;
    private static int num_slots;
    private static int num_making_machines;
    private static int num_packing_machines;

    static private BufferedWriter writer;

    public static void main(String[] args) {
        num_hotdogs = Integer.parseInt(args[0]);
        num_slots = Integer.parseInt(args[1]);
        num_making_machines = Integer.parseInt(args[2]);
        num_packing_machines = Integer.parseInt(args[3]);

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
        } catch( IOException e){
            e.printStackTrace();
            return;
        }
        
        final Buffer buffer = new Buffer(num_slots);

        
		final Thread producer = new Thread(() -> {
			for (int i = 0; i < num_making_machines; i++) {
                MakingMachine m = new MakingMachine();
                while(MakingMachine.hotdog_id < num_hotdogs){
                    final Hotdog hotdog = m.make_hot_dog();
                    buffer.put(hotdog, writer);
                }
				
			}
		});
		
		final Thread consumer = new Thread(() -> {
			for (int i = 0; i < num_packing_machines; i++) {
                PackingMachine p = new PackingMachine();
                while(PackingMachine.latest_hotdog < num_hotdogs){
                    final Hotdog hotdog = buffer.get();
                    p.pack(hotdog, writer);
                }
				
			}
		});
		
        
        final Thread[] threads = {
			producer,
			consumer	
		};
		
		for (Thread thread : threads) {
			thread.start();
		}

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        try {
            if (writer != null) {
                writer.close(); // Close the writer in finally block
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class MakingMachine {
  
    //id generation performed using a lock to prevent multiple machines with same id
    private static volatile int id_gen = 1;
    private static final Object lock = new Object();

    //hot dog id generation
    public static volatile int hotdog_id = 0;
    private static final Object hotdog_lock = new Object();

    private String making_machine_id;
    private int made_count = 0;

    public MakingMachine(){
        synchronized(lock){
            making_machine_id = "m" + id_gen++;
        }
    }

    public int get_made_count(){
        return made_count;
    }

    public String get_making_machine_id(){
        return making_machine_id;
    }

    public Hotdog make_hot_dog(){
        // 1. check if number of hot_dogs below max slots -> might be unnecessary
        Integer hd_id;
        synchronized(hotdog_lock){
            hd_id = hotdog_id++;
        }

        // 2. make hot dog -> 4 units of time
        Worker.doWork(WorkUnit.TIME_TO_MAKE);
        
        return new Hotdog(hd_id, making_machine_id);
    }

    

}


class PackingMachine {

    private String packing_machine_id;

    //id generation performed using a lock to prevent multiple machines with same id
    private static volatile int id_gen = 1;
    private static final Object lock = new Object();

    //track the latest hotdog finished packing
    public static volatile int latest_hotdog = 0;
    private static final Object hotdog_lock = new Object();

    private int packed_count = 0;

    public PackingMachine(){
        synchronized(lock){
            packing_machine_id = "p" + id_gen++;
        }
    }
    

    public int get_packed_count(){
        return packed_count;
    }

    public String get_packing_machine_id(){
        return packing_machine_id;
    }

    public void pack(Hotdog hotdog, BufferedWriter writer){
        // 1. Check if any hotdogs in the buffer
        // 2. take 1 hotdog from the buffer -> 1 unit of time
        // 3. perform pack -> 2 unit of time
        // 4. add to log
        Worker.doWork(WorkUnit.TIME_TO_PACK);
        synchronized(hotdog_lock){
            latest_hotdog++;
        }
        write_record(hotdog, writer);
    }

    private void write_record(Hotdog hotdog, BufferedWriter writer){
        try {
            writer.write(packing_machine_id + " gets " + hotdog.get_hotdog_id() + " from " + hotdog.get_maker_id() );
            writer.newLine();
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

    Buffer(int num_slots) {
        buffer = new Hotdog[num_slots];
    }

    private void write_record(Hotdog hotdog, BufferedWriter writer){
        try {
            writer.write(hotdog.get_maker_id() + " puts " + hotdog.get_hotdog_id());
            writer.newLine();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    synchronized void put(Hotdog hotdog, BufferedWriter writer) {
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
        write_record(hotdog, writer);
        this.notifyAll();
    }

    synchronized Hotdog get() {
        while (itemCount == 0) {
            try {
                this.wait();
            } catch (InterruptedException e) {
            }
        }
        final Hotdog hotdog = buffer[front];
        front = (front + 1) % buffer.length;
        itemCount--;
        this.notifyAll();
        return hotdog;
    }
}

class Hotdog {
    
    private int id;
    private String making_machine_id;

    public Hotdog(int id, String making_machine_id) {
        this.id = id;
        this.making_machine_id = making_machine_id;
    }

    public String get_maker_id(){
        return making_machine_id;
    }

    public int get_hotdog_id(){
        return id;
    }

    @Override
    public String toString() {
        return "Hotdog [id=" + id + "]";
    }
}

enum WorkUnit{
    TIME_TO_TAKE(1),
    TIME_TO_SEND(1),
    TIME_TO_PACK(2),
    TIME_TO_MAKE(4);

    public final int timeTaken;

    WorkUnit(int timeTaken){
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
