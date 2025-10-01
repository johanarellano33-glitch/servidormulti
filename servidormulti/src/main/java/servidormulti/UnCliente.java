
package servidormulti;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class UnCliente implements Runnable {
    final DataOutputStream salida;
    
    final BufferedReader teclado = new BufferedReader (new InputStreamReader(System.in));
    final DataInputStream  entrada;
    
    UnCliente (Socket s) throws I0Exception {
        salida = new DataOutputStream(s.getOutputStream());
        entrada = new DataInputStream(s.getInputStream());
        
    }
    
    @Override
    public void run(){
        String mensaje;
        while ( true ){
            try {
                mensaje = entrada.readUTF();
                for (UnCliente cliente : ServidorMulti.clientes.values()){
                    cliente.salida.writeUTF(mensaje);
                }
            } catch (I0Exception ex){
            }
        }
    }
}
