package com.mycompany.servidormulti;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Map;

public class UnCliente implements Runnable {
    
    final DataOutputStream salida;
    final DataInputStream entrada;
    
    private String idCliente; // ID temporal (número) o Nombre de usuario
    private int mensajesEnviados = 0;
    private boolean autenticado = false;
    
    UnCliente(Socket s, String id) throws IOException {
        this.idCliente = id;
        salida = new DataOutputStream(s.getOutputStream());
        entrada = new DataInputStream(s.getInputStream());
    }

    @Override
    public void run() {
        try {
            salida.writeUTF("Bienvenido. Tu ID es: " + idCliente + 
                             ". Tienes 3 mensajes gratis. Usa 'REGISTER nombre password' o 'LOGIN nombre password'.");
            
            while (true) {
                String mensaje = entrada.readUTF();
                
                String[] partesComando = mensaje.split(" ", 3);
                String comando = partesComando.length > 0 ? partesComando[0].toUpperCase() : "";

                // --- 1. Manejo de comandos (REGISTER / LOGIN) ---
                if (comando.equals("REGISTER") || comando.equals("LOGIN")) {
                    if (autenticado) {
                        salida.writeUTF("Ya estás autenticado como: " + idCliente);
                        continue;
                    }

                    if (partesComando.length != 3) {
                        salida.writeUTF("Error de sintaxis. Usa: " + comando + " nombre password");
                        continue;
                    }
                    
                    String nombre = partesComando[1];
                    String password = partesComando[2];
                    
                    if (comando.equals("REGISTER")) {
                        if (ServidorMulti.registrarUsuario(nombre, password)) {
                            salida.writeUTF("¡Registro exitoso! Ahora usa LOGIN.");
                        } else {
                            salida.writeUTF("Error: El nombre de usuario '" + nombre + "' ya existe.");
                        }
                    } else if (comando.equals("LOGIN")) {
                        if (ServidorMulti.verificarCredenciales(nombre, password)) {
                            
                            autenticado = true;
                            // 1. Quitar el ID temporal del mapa
                            ServidorMulti.clientes.remove(idCliente); 
                            // 2. Asignar el nombre de usuario como ID
                            idCliente = nombre; 
                            // 3. Volver a meter el objeto en el mapa con el nuevo ID (nombre)
                            ServidorMulti.clientes.put(idCliente, this); 
                            
                            salida.writeUTF("¡Inicio de sesión exitoso! Eres: " + idCliente + ". Tienes mensajes ilimitados.");
                            System.out.println("Cliente ID antiguo se autenticó como " + nombre);
                            
                        } else {
                            salida.writeUTF("Error de inicio de sesión. Credenciales incorrectas.");
                        }
                    }
                    continue; 
                }
                
                // --- 2. Verificación de permisos para enviar ---
                if (!autenticado && mensajesEnviados >= 3) {
                    salida.writeUTF("Límite de 3 mensajes alcanzado. Debes autenticarte (ej: LOGIN nombre password) para enviar más.");
                    continue;
                }
                
                // El mensaje se cuenta SÓLO si es un mensaje real (no un comando fallido o bloqueado)
                if (!autenticado) {
                    mensajesEnviados++;
                }

                // --- 3. Manejo de mensajes privados (@) ---
                if (mensaje.startsWith("@")) {
                    String[] partes = mensaje.split(" ", 2);
                    if (partes.length < 2) {
                         salida.writeUTF("Formato privado incorrecto. Usa: @ID_o_NOMBRE mensaje");
                         if (!autenticado) mensajesEnviados--;
                         continue;
                    }
                    
                    String aQuien = partes[0].substring(1);
                    UnCliente clienteDestino = ServidorMulti.clientes.get(aQuien);
                    
                    if (clienteDestino != null) {
                        String remitente = idCliente; 
                        String mensajeConRemitente = "(PRIVADO de " + remitente + "): " + partes[1];
                        clienteDestino.salida.writeUTF(mensajeConRemitente);
                        this.salida.writeUTF("(Mensaje enviado a " + aQuien + ")");
                    } else {
                        salida.writeUTF("Error: Cliente con ID/Nombre " + aQuien + " no encontrado.");
                        if (!autenticado) mensajesEnviados--;
                    }
                    
                } else {
                    // --- 4. Manejo de mensajes públicos (Broadcast) ---
                    String remitente = idCliente;
                    String mensajeBroadcast = "[" + remitente + "]: " + mensaje;
                    
                    for (Map.Entry<String, UnCliente> entry : ServidorMulti.clientes.entrySet()) {
                        UnCliente cliente = entry.getValue();
                        if (cliente != this) {
                            cliente.salida.writeUTF(mensajeBroadcast);
                        }
                    }
                    
                    if (!autenticado) {
                        salida.writeUTF("Mensajes restantes: " + (3 - mensajesEnviados));
                    }
                }
            }
        } catch (IOException ex) {
            // Manejo de desconexión: Limpiamos la lista de clientes.
            System.out.println("Cliente " + idCliente + " desconectado.");
            ServidorMulti.clientes.remove(idCliente);
        }
    }
}