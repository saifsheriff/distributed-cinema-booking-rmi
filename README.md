# Distributed Cinema Booking System (Java RMI + Ricart-Agrawala)

A fault-tolerant, distributed cinema ticket booking system built across three simulated regional servers, implementing mutual exclusion and secure communication from first principles rather than relying on a database lock.

## Overview

Built for a Distributed Systems Security course (CCY3302), this project simulates three independent cinema booking servers — Cairo, Alexandria, and Luxor — that must coordinate ticket reservations over the network without a central authority. The core challenge: prevent two servers from double-booking the same seat when requests arrive concurrently, while keeping all communication authenticated and encrypted between peers.

Rather than using an external coordination service, the system implements the **Ricart-Agrawala distributed mutual exclusion algorithm** from scratch, backed by **Lamport logical clocks** for consistent event ordering across nodes with no shared clock.

## Architecture & Tech Stack

- **Language:** Java (RMI — Remote Method Invocation)
- **Concurrency control:** Ricart-Agrawala mutual exclusion algorithm
- **Event ordering:** Lamport logical clocks
- **Security:** Mutual TLS (mTLS) over SSL sockets — both client and server authenticate each other via certificates, not just server-side TLS
- **Topology:** 3 peer servers (Cairo, Alex, Luxor), each capable of both requesting and granting access to the critical section (seat booking)

```
   [Cairo Server] <----mTLS----> [Alex Server]
         \                          /
          \--------mTLS------------/
                   |
             [Luxor Server]

Each node: Ricart-Agrawala request/reply queue + Lamport clock
```

*(Diagram placeholder — swap in the actual architecture SVG/image here)*

## What I Did

- Implemented the Ricart-Agrawala algorithm across three independent RMI servers, handling REQUEST, REPLY, and RELEASE message types for entry into the critical section (the seat-booking operation)
- Implemented Lamport logical clocks to maintain a consistent causal ordering of booking requests across nodes with no synchronized wall clock
- Configured mutual TLS over SSL sockets so every peer-to-peer connection required both sides to present a valid certificate — not just standard one-way server authentication
- Debugged and resolved deadlock conditions that emerged when multiple servers issued simultaneous requests, including race conditions in the request queue
- Built retry logic to handle peer synchronization failures and transient network issues between the three servers
- Produced a full technical report and system architecture diagram documenting the design decisions and failure modes encountered

## Key Findings / Results

- Successfully prevented double-booking under concurrent request scenarios across all three simulated servers
- Identified and resolved deadlock scenarios caused by [*fill in: e.g. simultaneous REQUEST broadcast timing / reply queue ordering — add the specific root cause once confirmed*]
- Verified correct Lamport clock ordering under out-of-order message delivery

*(Add a screenshot or terminal log here showing: 2+ servers running, a booking request being granted/queued, and the mTLS handshake succeeding — this is the single most convincing piece of evidence in the whole repo)*

## Challenges & What I'd Improve

- Deadlock resolution was the hardest part — initial implementation could stall when [*fill in the specific trigger, e.g. two nodes issued requests within the same clock tick*]. Resolved by [*fill in your fix — tie-breaking rule, message ordering fix, etc.*]
- Peer synchronization was brittle under simulated network delay; added retry loops to handle this, though a production version would benefit from a proper failure-detector or timeout/backoff strategy
- Given more time, I'd replace the current retry approach with exponential backoff and add persistent logging for post-hoc debugging of ordering issues

## Skills Demonstrated

`Java` `RMI` `Distributed Systems` `Mutual Exclusion Algorithms` `Ricart-Agrawala` `Lamport Clocks` `mTLS` `SSL/TLS` `Concurrency Debugging` `Network Security`

## Course Context

Developed for CCY3302 — Distributed Systems Security, Arab Academy for Science, Technology & Maritime Transport (AASTMT)
