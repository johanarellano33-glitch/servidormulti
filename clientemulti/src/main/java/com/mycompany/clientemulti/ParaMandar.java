package com.mycompany.clientemulti;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class ParaMandar implements Runnable {
    private final BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in));
    private final DataOutputStream salida;
    private final Socket socket;
    
    public ParaMandar(Socket s) throws IOException {
        this.socket = s;
        this.salida = new DataOutputStream(s.getOutputStream());
    }
    
    @Override
    public void run() {
        try {
            while (true) {
                String mensaje = teclado.readLine();
                if (mensaje == null) break;
                
                salida.writeUTF(mensaje);
                salida.flush();
                
                if ("salir".equalsIgnoreCase(mensaje)) {
                    System.out.println("Cerrando conexión...");
                    // Al cerrar el socket aquí, se fuerza la desconexión
                    // lo que terminará el hilo ParaRecibir y ClienteMulti continuará.
                    socket.close(); 
                    break;
                }
            }
        } catch (IOException ex) {
            // Silenciar errores comunes de cierre de socket (p. ej., "Broken pipe")
            if (!socket.isClosed()) {
                System.out.println("Error en ParaMandar: " + ex.getMessage());
            }
        }
    }
}