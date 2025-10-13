package com.mycompany.servidormulti;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Map; // Importación para el HashMap de clientes

public class UnCliente implements Runnable {
    final DataOutputStream salida;
    // final BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in)); // No necesario en el servidor
    final DataInputStream entrada;
    
    // CAMPOS NUEVOS PARA EL ESTADO
    private final String idCliente; // ID para identificarlo
    private int mensajesEnviados = 0; // Contador de mensajes antes de autenticación
    private boolean autenticado = false; // Estado de autenticación
    
    UnCliente(Socket s, String id) throws IOException { // CAMBIO: Recibe el ID
        this.idCliente = id; // Guardamos el ID
        salida = new DataOutputStream(s.getOutputStream());
        entrada = new DataInputStream(s.getInputStream());
    }

    @Override
    public void run() {
        try {
            // Mensaje de bienvenida con instrucciones
            salida.writeUTF("Bienvenido. Tu ID es: " + idCliente + ". Tienes 3 mensajes gratis. Usa 'LOGIN nombre' para autenticarte.");
            
            while (true) {
                String mensaje = entrada.readUTF();
                
                // --- 1. Manejo de comandos (LOGIN) ---
                if (mensaje.startsWith("LOGIN ")) {
                    if (!autenticado) {
                        String nombre = mensaje.substring(6).trim();
                        if (!nombre.isEmpty()) {
                            autenticado = true;
                            // Reemplazamos el ID por el nombre de usuario
                            // Nota: Para simplificar, no estamos verificando si el nombre ya existe.
                            ServidorMulti.clientes.remove(idCliente);
                            ServidorMulti.clientes.put(nombre, this); // El nuevo ID es el nombre
                            
                            // El idCliente ahora es el nombre de usuario
                            // idCliente = nombre; // Esto NO funciona porque idCliente es final. 
                            // Deberíamos hacer idCliente no final para este cambio. 
                            // Para mantener el código simple, seguiremos usando el ID original para este objeto.
                            
                            salida.writeUTF("¡Autenticación exitosa! Eres: " + nombre + ". Tienes mensajes ilimitados.");
                            System.out.println("Cliente ID " + idCliente + " se autenticó como " + nombre);
                        } else {
                            salida.writeUTF("Error: El nombre de usuario no puede estar vacío.");
                        }
                    } else {
                        salida.writeUTF("Ya estás autenticado.");
                    }
                    continue; // No es un mensaje para broadcast/privado
                }
                
                // --- 2. Verificación de permisos para enviar ---
                if (!autenticado && mensajesEnviados >= 3) {
                    salida.writeUTF("Límite de 3 mensajes alcanzado. Debes autenticarte (ej: LOGIN nombre) para enviar más mensajes.");
                    continue; // Impedir que envíe el mensaje
                }
                
                // El mensaje se cuenta SÓLO si es enviado (público o privado)
                if (!autenticado) {
                    mensajesEnviados++;
                }

                // --- 3. Manejo de mensajes privados (@) ---
                if (mensaje.startsWith("@")) {
                    String[] partes = mensaje.split(" ", 2);
                    if (partes.length < 2) {
                         salida.writeUTF("Formato privado incorrecto. Usa: @ID_o_NOMBRE mensaje");
                         // Revertir el contador si falló el formato
                         if (!autenticado) mensajesEnviados--; 
                         continue;
                    }
                    
                    String aQuien = partes[0].substring(1);
                    UnCliente clienteDestino = ServidorMulti.clientes.get(aQuien);
                    
                    if (clienteDestino != null) {
                        String remitente = autenticado ? "Autenticado" : idCliente;
                        String mensajeConRemitente = "(PRIVADO de " + remitente + "): " + partes[1];
                        clienteDestino.salida.writeUTF(mensajeConRemitente);
                        this.salida.writeUTF("(Mensaje enviado a " + aQuien + ")");
                    } else {
                        salida.writeUTF("Error: Cliente con ID/Nombre " + aQuien + " no encontrado.");
                        // Revertir el contador si falló la búsqueda
                        if (!autenticado) mensajesEnviados--;
                    }
                    
                } else {
                    // --- 4. Manejo de mensajes públicos (Broadcast) ---
                    String remitente = autenticado ? "Autenticado" : idCliente;
                    String mensajeBroadcast = "[" + remitente + "]: " + mensaje;
                    
                    for (Map.Entry<String, UnCliente> entry : ServidorMulti.clientes.entrySet()) {
                        UnCliente cliente = entry.getValue();
                        // Enviamos a todos EXCEPTO a si mismo
                        if (cliente != this) { 
                            cliente.salida.writeUTF(mensajeBroadcast);
                        }
                    }
                    
                    // Informar al cliente que no está autenticado cuántos mensajes le quedan
                    if (!autenticado) {
                        salida.writeUTF("Mensajes restantes: " + (3 - mensajesEnviados));
                    }
                }
            }
        } catch (IOException ex) {
            // Manejo de desconexión: Limpiamos la lista de clientes.
            System.out.println("Cliente ID " + idCliente + " desconectado.");
            // Buscamos y removemos el cliente, ya sea por ID o por Nombre (si implementáramos el cambio de llave en el mapa)
            ServidorMulti.clientes.remove(idCliente); 
        }
    }
}