public class HotDogManager {

    private static final long limit = 300_000_000;

    private static int num_hotdogs;
    private static int num_slots;
    private static int num_making_machines;
    private static int num_packing_machines;



    private static void doWork(int n) {
        for (int i = 0; i < n; i++) {
            long m = limit;
            while (m > 0) {
                m--;
            }
        }
    }

    public static void main(String[] args) {
        num_hotdogs = Integer.parseInt(args[0]);
        num_slots = Integer.parseInt(args[1]);
        num_making_machines = Integer.parseInt(args[2]);
        num_packing_machines = Integer.parseInt(args[3]);
        
        
        final Buffer buffer = new Buffer(num_slots);

        
		final Thread producer = new Thread(() -> {
			for (int i = 0; i < n; i++) {
				doWork(2);
				final Hotdog hotdog = new Hotdog(i);
				buffer.put(hotdog);
			}
		});
		
		final Thread consumer = new Thread(() -> {
			for (int i = 0; i < n; i++) {
				@SuppressWarnings("unused")
				final Hotdog hotdog = buffer.get();
				doWork(5);
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
    }
}

class MakingMachine {

    private static final int time_to_make = 4;
    private static int id_gen = 1;
    private String making_machine_id;
    private int made_count = 0;

    public MakingMachine(){
        making_machine_id = "m" + id_gen++;
    }

    public int get_made_count(){
        return made_count;
    }

    private void make_hot_dog(){
        // 1. check if number of hot_dogs below max slots -> might be unnecessary
        // 2. make hot dog -> 2 units of time
        // 3. send to buffer -> 1 unit of time
        // 4. add to log
    }

    private void write_record(int hot_dog_id){

    }

}


class PackingMachine {

    private String packing_machine_id;
    private static int id_gen = 1;
    private int packed_count = 0;

    public PackingMachine(){
        packing_machine_id = "p" + id_gen++;
    }
    

    public int get_packed_count(){
        return packed_count;
    }

    public void pack(){
        // 1. Check if any hotdogs in the buffer
        // 2. take 1 hotdog from the buffer -> 1 unit of time
        // 3. perform pack -> 2 unit of time
        // 4. add to log
    }

    private void write_record(){

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
    
    synchronized void put(Hotdog hotdog) {
        while (itemCount == buffer.length) {
            try {
                this.wait();
            } catch (InterruptedException e) {
            }
        }
        buffer[back] = hotdog;
        back = (back + 1) % buffer.length;
        System.out.println(
            "Item count: " + itemCount + ", " +
            "Producing " + hotdog
        );
        itemCount++;
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
        System.out.println(
            "Item count: " + itemCount + ", " +
            "Consuming " + hotdog
        );
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
