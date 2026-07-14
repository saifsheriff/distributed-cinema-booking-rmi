package ticketbookingrmi;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class BookingServiceImpl extends UnicastRemoteObject implements BookingService {

    public static final String RELEASED = "RELEASED";
    public static final String WANTED   = "WANTED";
    public static final String HELD     = "HELD";

    private String state = RELEASED;
    private int lamportClock = 0;
    private int myRequestTimestamp = 0;
    private String serverId;
    private int totalServers;
    private boolean ready = false;

    private int replyCount = 0;
    private Queue<String> deferredQueue = new LinkedList<>();
    private Map<String, BookingService> peers = new HashMap<>();

    // ── Constructor ────────────────────────────────────────────────
    public BookingServiceImpl(String serverId, int totalServers) throws RemoteException {
        super(0,
            new javax.rmi.ssl.SslRMIClientSocketFactory(),
            new javax.rmi.ssl.SslRMIServerSocketFactory()
        );
        this.serverId = serverId;
        this.totalServers = totalServers;
    }

    // ── Peer management ───────────────────────────────────────────
    public void addPeer(String id, BookingService peer) {
        peers.put(id, peer);
    }

    public BookingService getPeer(String id) {
        return peers.get(id);
    }

    // ── Ready flag ────────────────────────────────────────────────
    public void setReady() {
        this.ready = true;
    }

    @Override
    public boolean isReady() throws RemoteException {
        return ready;
    }

// ── Request to book a cinema seat ─────────────────────────────
    public void requestSeat(String movieTitle, String seatNumber) {

        // Set state and timestamp — synchronized
        synchronized (this) {
            state = WANTED;
            lamportClock++;
            myRequestTimestamp = lamportClock;
            replyCount = 0;
        }

        System.out.println("[" + serverId + "] REQUESTING seat " + seatNumber
                + " for \"" + movieTitle + "\" | Timestamp: " + myRequestTimestamp);

        // Send requests OUTSIDE synchronized block so we can receive replies
        for (Map.Entry<String, BookingService> entry : peers.entrySet()) {
            try {
                entry.getValue().receiveRequest(myRequestTimestamp, serverId);
            } catch (RemoteException e) {
                System.err.println("[" + serverId + "] ERROR sending request to " + entry.getKey());
            }
        }

        // Wait for replies — synchronized
        synchronized (this) {
            while (replyCount < totalServers - 1) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            // Enter Critical Section
            state = HELD;
        }

        System.out.println("[" + serverId + "] ENTERED CS — booking seat " + seatNumber);
        bookSeat(movieTitle, seatNumber);
        releaseSeat();
    }

    // ── Simulate writing to cinema database ───────────────────────
    private void bookSeat(String movieTitle, String seatNumber) {
        System.out.println("[" + serverId + "] Writing to DB: seat " + seatNumber
                + " for \"" + movieTitle + "\"...");
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("[" + serverId + "] Seat " + seatNumber
                + " CONFIRMED for \"" + movieTitle + "\"!");
    }

    // ── Release CS and flush deferred queue ───────────────────────
    private synchronized void releaseSeat() {
        state = RELEASED;
        System.out.println("[" + serverId + "] RELEASED CS");

        for (String deferredId : deferredQueue) {
            try {
                BookingService peer = peers.get(deferredId);
                if (peer == null) continue;
                peer.receiveReply(serverId);
                System.out.println("[" + serverId + "] Sent deferred REPLY to " + deferredId);
            } catch (RemoteException e) {
                System.err.println("[" + serverId + "] ERROR sending deferred reply to " + deferredId);
            }
        }
        deferredQueue.clear();
    }

    // ── Called remotely: another server requests CS ───────────────
    @Override
    public synchronized void receiveRequest(int timestamp, String fromId) throws RemoteException {
        lamportClock = Math.max(lamportClock, timestamp) + 1;

        System.out.println("[" + serverId + "] Received REQUEST from " + fromId
                + " | Their T=" + timestamp
                + " | My T=" + myRequestTimestamp
                + " | My state=" + state);

        boolean defer = false;

        if (state.equals(HELD)) {
            // I am in CS — they must wait
            defer = true;
            System.out.println("[" + serverId + "] I am in CS → DEFER " + fromId);

        } else if (state.equals(WANTED)) {
            if (myRequestTimestamp < timestamp) {
                // My request is older → I have priority
                defer = true;
                System.out.println("[" + serverId + "] My T=" + myRequestTimestamp
                        + " < Their T=" + timestamp + " → I have priority → DEFER " + fromId);
            } else if (myRequestTimestamp == timestamp) {
                // Tie-break: lower process ID wins (Alex < Cairo, so Alex wins)
                defer = serverId.compareTo(fromId) > 0;
                System.out.println("[" + serverId + "] TIE! " + serverId
                        + ".compareTo(" + fromId + ")=" + serverId.compareTo(fromId)
                        + " → defer=" + defer);
            } else {
                // Their timestamp is lower → they have priority → reply immediately
                System.out.println("[" + serverId + "] Their T=" + timestamp
                        + " < My T=" + myRequestTimestamp + " → They have priority → REPLY to " + fromId);
            }

        } else {
            // RELEASED → always reply immediately
            System.out.println("[" + serverId + "] I am RELEASED → REPLY immediately to " + fromId);
        }

        if (defer) {
            deferredQueue.add(fromId);
            System.out.println("[" + serverId + "] Queued deferred reply to " + fromId);
        } else {
            BookingService peer = peers.get(fromId);
            if (peer != null) {
                peer.receiveReply(serverId);
                System.out.println("[" + serverId + "] Sent immediate REPLY to " + fromId);
            } else {
                // Peer not in map yet — defer instead of dropping
                deferredQueue.add(fromId);
                System.out.println("[" + serverId + "] Peer " + fromId + " not found — deferring reply");
            }
        }
    }

    // ── Called remotely: reply to our request ────────────────────
    @Override
    public synchronized void receiveReply(String fromId) throws RemoteException {
        replyCount++;
        System.out.println("[" + serverId + "] Received REPLY from " + fromId
                + " | Total: " + replyCount + "/" + (totalServers - 1));
        notifyAll();
    }
}