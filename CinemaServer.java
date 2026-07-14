/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ticketbookingrmi;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class CinemaServer {

    public static void main(String[] args) throws Exception {

        // ── SSL Configuration ───────────────────────────────────────
        System.setProperty("javax.net.ssl.keyStore",           "src/ssl/serverKeystore.jks");
        System.setProperty("javax.net.ssl.keyStorePassword",   "cinema123");
        System.setProperty("javax.net.ssl.trustStore",         "src/ssl/truststore.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "cinema123");

        // ── Change these 3 values per run ──────────────────────────
        String myId    = "Luxor";         // "Cairo"  | "Alex"  | "Luxor"
        int    myPort  = 2002;            //  2000    |  2001   |  2002
        String mySeat  = "B7";
        String myMovie = "Interstellar";
        // ───────────────────────────────────────────────────────────

        int totalServers = 3;

        String[][] allServers = {
            {"Cairo", "2000"},
            {"Alex",  "2001"},
            {"Luxor", "2002"}
        };

        // ── Start this server ───────────────────────────────────────
        BookingServiceImpl myService = new BookingServiceImpl(myId, totalServers);

        javax.rmi.ssl.SslRMIClientSocketFactory csf = new javax.rmi.ssl.SslRMIClientSocketFactory();
        javax.rmi.ssl.SslRMIServerSocketFactory ssf = new javax.rmi.ssl.SslRMIServerSocketFactory();

        Registry myRegistry = LocateRegistry.createRegistry(myPort, csf, ssf);
        myRegistry.rebind("CinemaBooking", myService);

        System.out.println("[" + myId + "] SSL-secured cinema server started on port " + myPort);
        System.out.println("[" + myId + "] TLS mutual authentication active");

        // ── Wait for all 3 servers to start ────────────────────────
        System.out.println("[" + myId + "] Waiting 15 seconds for all servers to start...");
        Thread.sleep(10000);

        // ── Connect to peers (with retry) ───────────────────────────
        for (String[] server : allServers) {
            if (!server[0].equals(myId)) {
                BookingService stub = null;
                int attempts = 0;
                while (stub == null && attempts < 10) {
                    try {
                        Registry reg = LocateRegistry.getRegistry("localhost",
                                Integer.parseInt(server[1]), csf);
                        stub = (BookingService) reg.lookup("CinemaBooking");
                        myService.addPeer(server[0], stub);
                        System.out.println("[" + myId + "] Securely connected to " + server[0]);
                    } catch (Exception e) {
                        attempts++;
                        System.out.println("[" + myId + "] Waiting for " + server[0]
                                + "... attempt " + attempts + "/10");
                        Thread.sleep(2000);
                    }
                }
            }
        }

        // ── Wait 3 more seconds for everyone to finish connecting ───
        System.out.println("[" + myId + "] All peers connected, waiting 3 more seconds...");
        Thread.sleep(3000);

        // ── Mark myself as ready ────────────────────────────────────
        myService.setReady();
        System.out.println("[" + myId + "] Marked as READY");

        // ── Wait until ALL peers are ready ──────────────────────────
        System.out.println("[" + myId + "] Waiting for all peers to be ready...");
        for (String[] server : allServers) {
            if (!server[0].equals(myId)) {
                boolean peerReady = false;
                while (!peerReady) {
                    try {
                        peerReady = myService.getPeer(server[0]).isReady();
                        if (!peerReady) {
                            System.out.println("[" + myId + "] " + server[0] + " not ready yet...");
                            Thread.sleep(1000);
                        }
                    } catch (Exception e) {
                        Thread.sleep(1000);
                    }
                }
                System.out.println("[" + myId + "] " + server[0] + " is READY");
            }
        }

    // ── All ready — start booking ───────────────────────────────
          System.out.println("[" + myId + "] All peers ready! Starting booking...");

          if (myId.equals("Luxor")) {
              System.out.println("[Luxor] Standing by — will reply to any requests");
              Thread.sleep(60000);
          } else {
              // Wait until the same exact millisecond so both fire together
              long startTime = System.currentTimeMillis();
              // Round up to next 5-second mark so all servers hit it together
              long fireAt = (startTime / 5000 + 1) * 5000;
              System.out.println("[" + myId + "] Firing at T=" + fireAt
                      + " (in " + (fireAt - startTime) + "ms)");
              Thread.sleep(fireAt - startTime);
              System.out.println("[" + myId + "] FIRE!");
              myService.requestSeat(myMovie, mySeat);
          }
    }
}