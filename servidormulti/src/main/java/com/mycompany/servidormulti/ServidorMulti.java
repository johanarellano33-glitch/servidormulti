package com.mycompany.servidormulti;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap; // Mejor práctica para hilos

public class ServidorMulti {
    // CAMBIO: Usamos ConcurrentHashMap para que sea seguro con múltiples hilos.
    static ConcurrentHashMap<String, UnCliente> clientes = new ConcurrentHashMap<>();
    
    public static void main(String[] args) {
        int puerto = 8080;
        int contador = 0;
        try (ServerSocket servidorSocket = new ServerSocket(puerto)) {
            System.out.println("Servidor iniciado en el puerto " + puerto);
            while (true) {
                Socket socket = servidorSocket.accept();
                
                String idCliente = Integer.toString(contador);
                
                // CAMBIO: Se pasa el idCliente al constructor de UnCliente
                UnCliente uncliente = new UnCliente(socket, idCliente); 
                Thread hilo = new Thread(uncliente);
                
                clientes.put(idCliente, uncliente);
                hilo.start();
                System.out.println("Se conectó el chango #" + contador + " (ID: " + idCliente + ")");
                contador++;
            }
        } catch (IOException e) {
            System.err.println("Error en ServidorMulti: " + e.getMessage());
        }
    }
}