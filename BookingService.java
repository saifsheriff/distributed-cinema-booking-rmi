/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ticketbookingrmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface BookingService extends Remote {
    // Send a seat booking REQUEST <timestamp, serverId>
    void receiveRequest(int timestamp, String serverId) throws RemoteException;

    // Send a REPLY (seat booking approved)
    void receiveReply(String fromServerId) throws RemoteException;
    
    boolean isReady() throws RemoteException; 
}