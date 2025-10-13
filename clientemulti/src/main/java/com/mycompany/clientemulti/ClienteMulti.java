package com.mycompany.clientemulti;
import java.io.IOException;
import java.net.Socket;

public class ClienteMulti {
    public static void main(String[] args) {
        Socket s = null;
        Thread hiloParaRecibir = null; // Necesario para gestionar el hilo receptor
        
        try {
            s = new Socket("localhost", 8080);
            
            // Creamos los hilos
            Thread hiloParaMandar = new Thread(new ParaMandar(s), "sender");
            hiloParaRecibir = new Thread(new ParaRecibir(s), "receiver"); 

            hiloParaMandar.start();
            hiloParaRecibir.start();
            
            hiloParaMandar.join(); // Espera a que el hilo de envío termine (ej: por 'salir')
            
            // Lógica para cerrar la conexión y terminar el hilo de recepción
            if (s != null && !s.isClosed()) {
                s.close(); 
            }
            
        } catch (Exception e) {
            // No imprimir el mensaje si la conexión se cierra limpiamente
            if (!"Connection reset".equalsIgnoreCase(e.getMessage()) && !"Socket closed".equalsIgnoreCase(e.getMessage())) {
                System.out.println("Error en ClienteMulti: " + e.getMessage());
            }
        } finally {
            // Fallback para asegurar el cierre del socket
            if (s != null && !s.isClosed()) {
                try { s.close(); } catch (IOException ignore) {}
            }
        }
    }
}

