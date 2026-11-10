# QueueDesk — IT Helpdesk Ticketing & Triage System

An internal IT ticketing, triage, analytics, and persistence platform built in Java.

## Features
- **Core Domain Model**: Polymorphic `Ticket` hierarchy supporting `BugTicket`, `FeatureRequestTicket`, and `AccessRequestTicket`.
- **Capability Modeling**: `Escalatable` interface with conditional escalation badges and pattern matching.
- **Triage Queues**: Dual `PriorityQueue` implementations with natural ordering (Priority descending) and custom multi-level comparators.
- **Repository Pattern**: Generic `Repository<T>` with safe `Optional` lookups (`findByIdSafe`), unchecked exception handling (`TicketNotFoundException`), and read-only views.
- **Stream Analytics**: Declarative Stream API pipelines for priority distribution, assignee grouping, average effort, map-reduce total effort, and tag flattening.
- **Dual Persistence & Migration**: Legacy pipe-delimited parsing (`tickets.txt`) with automated migration to polymorphic Jackson JSON (`tickets.json`).
- **Reporting**: End-of-day summary report generator (`daily-report.txt`).

## Quick Start

### Run with script:
```bash
./run.sh
```

### Run with Java CLI:
```bash
java -cp "lib/*:." Main.java
```

### Compile & Run from binaries:
```bash
javac -cp "lib/*:." -d out *.java
java -cp "lib/*:out" Main
```
