# Hospital Diagnostics Coordination System

This project implements and compares multiple concurrency control mechanisms in Java
for a hospital diagnostics coordination system under different workload patterns.

---

## Java Version
- **Java JDK 11 or higher**
- Developed and tested using **Java JDK 21**
- No external libraries required (uses only `java.util.concurrent`)

---

## Project Structure

Each synchronization strategy is implemented as a separate module:

- `Monitor-Based-Implementation`
- `BlockingQueue-Implementation`
- `ReentrantLock-Producer-Consumer-Implementation`
- `ReentrantReadWriteLock-Implementation`

Each directory contains a standalone implementation with the same entry point:
`HospitalDiagnosticsSystem.java`

---

## Architecture Overview

The system follows a **Producer–Consumer architecture**:

- **Producers** submit diagnostic orders
- **Consumers** process orders
- **Auditors** validate consistency and system state
- **Supervisor** manages lifecycle and shutdown

Concurrency control varies by implementation:
- **Monitor**: Java intrinsic locks (`synchronized`, `wait`, `notify`)
- **BlockingQueue**: Java concurrent queue abstraction
- **ReentrantLock (PC)**: Explicit locking with conditions
- **ReentrantReadWriteLock**: Separate read/write locking with fairness enabled

---

## Compilation Instructions

### Prerequisites
- Java JDK 11 or higher installed
- `JAVA_HOME` properly configured
- No external dependencies required

### Compile
Navigate to the required implementation directory:

```bash
cd <Implementation directory>
javac *.java
```
### Run
Select workload or run in balanced workload:
-`CALM`
-`SURGE`
-`WRITER_HEAVY`
-`READER_HEAVY`

```bash
java HospitalDiagnosticsSystem <Workload>
```
