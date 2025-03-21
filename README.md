# HotDogManager

## Overview

HotDogManager is a Java simulation of the producer-consumer problem. The project models "making machines" that produce hotdogs and "packing machines" that process (pack) them. Hotdogs are added to a shared bounded buffer and then consumed by the packing machines. The program logs the operations to a file (`log.txt`) and provides a summary of production and packing counts at the end.

## Features

- **Multi-threaded Simulation:**  
  Multiple producer threads (making machines) generate hotdogs, while multiple consumer threads (packing machines) process them.

- **Bounded Buffer:**  
  A thread-safe buffer is used to manage hotdogs between production and consumption, employing wait/notify mechanisms for proper synchronization.

- **Logging:**  
  A log file is generated to record every hotdog added and taken, as well as a final summary of operations performed by each machine.

- **Completion Handling:**  
  A shared completion flag signals consumers when production has finished, preventing consumer threads from waiting indefinitely.

## How It Works

1. **Input Parameters:**  
   The program expects four command-line arguments:
   - `num_hotdogs`: Total number of hotdogs to produce.
   - `num_slots`: Capacity of the shared buffer.
   - `num_making_machines`: Number of making machines (producer threads).
   - `num_packing_machines`: Number of packing machines (consumer threads).

2. **Production:**  
   - Each making machine creates hotdogs with a unique ID.
   - Work is simulated using a busy-wait loop.
   - Hotdogs are added to the buffer, and each operation is logged.

3. **Consumption:**  
   - Packing machines remove hotdogs from the buffer.
   - They simulate packing work before logging the operation.
   - The consumer threads check for a completion flag (`productionComplete`) to exit their loops gracefully when production is finished.

4. **Logging:**  
   - The log file (`log.txt`) records:
     - Initial simulation parameters.
     - Every production (`"mX puts Y"`) and consumption (`"pX gets Y from mX"`) event.
     - A summary of the number of hotdogs produced by each making machine and packed by each packing machine.

## Project Structure

- **HotDogManager.java:**  
  The main class that initializes the simulation, creates the producer and consumer threads, and writes the log and summary.

- **MakingMachine:**  
  A producer class responsible for making hotdogs. Each instance produces hotdogs until a predefined number is reached.

- **PackingMachine:**  
  A consumer class that packs hotdogs retrieved from the buffer.

- **Buffer:**  
  A thread-safe, bounded buffer that holds hotdogs. It uses synchronized methods along with wait/notify for coordination between producers and consumers. It also includes a `productionComplete` flag to signal termination.

- **Hotdog:**  
  A simple model class representing a hotdog with an ID and the ID of the machine that produced it.

- **WorkUnit & Worker:**  
  Utility components that simulate work (time delays) for production, sending, packing, and taking operations.

## How to Compile and Run

1. **Compilation:**

   Open your terminal, navigate to the project directory, and compile the code:
   ```bash
   javac HotDogManager.java
   ```

2. **Execution:**

   Run the program by providing the required arguments. For example:
   ```bash
   java HotDogManager 100 4 3 3
   ```
   This command runs the simulation with:
   - 100 hotdogs to produce.
   - A buffer with a capacity of 4.
   - 3 making machines (producers).
   - 3 packing machines (consumers).

## Concurrency and Synchronization

- **Buffer Synchronization:**  
  The buffer’s `put()` and `get()` methods are synchronized to ensure that only one thread modifies the buffer at a time. Producers wait if the buffer is full, while consumers wait if it is empty.

- **Completion Flag:**  
  A `productionComplete` flag in the `Buffer` class is set after all producer threads finish. This flag is used by consumers to exit their loop when no more hotdogs are expected.

- **Thread Safety:**  
  Shared counters, machine IDs, and log file writing are protected using synchronization blocks and mutexes (locks).

## Known Issues and Future Improvements

- **Busy-Wait Loops:**  
  The current work simulation uses busy-wait loops. For a more efficient simulation, consider using `Thread.sleep()`.

- **Termination Robustness:**  
  Although the production complete flag works in this example, further testing and enhancements might be required for more complex scenarios. Extra hotdogs might be placed in the buffer after completing production.

- **Improved Concurrency Utilities:**  
  Consider replacing the custom buffer with Java’s built-in concurrency utilities (e.g., `BlockingQueue`) for a more robust and maintainable solution.

## License

This project is provided as-is for educational purposes.

---
