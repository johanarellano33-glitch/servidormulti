package com.mycompany.servidormulti;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Map;
import java.util.Set;

public class UnCliente implements Runnable {
    
    final DataOutputStream salida;
    final DataInputStream entrada;
    
    private String idCliente;
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

                // --- 1. Manejo de comandos REGISTER / LOGIN ---
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
                            ServidorMulti.clientes.remove(idCliente);
                            idCliente = nombre;
                            ServidorMulti.clientes.put(idCliente, this);
                            
                            salida.writeUTF("¡Inicio de sesión exitoso! Eres: " + idCliente + ". Tienes mensajes ilimitados.");
                            System.out.println("Cliente se autenticó como " + nombre);
                        } else {
                            salida.writeUTF("Error de inicio de sesión. Credenciales incorrectas.");
                        }
                    }
                    continue;
                }
                
                // --- 2. Comandos BLOCK / UNBLOCK / BLOCKLIST ---
                if (comando.equals("BLOCK")) {
                    if (!autenticado) {
                        salida.writeUTF("Error: Debes estar autenticado para bloquear usuarios.");
                        continue;
                    }
                    
                    if (partesComando.length != 2) {
                        salida.writeUTF("Error de sintaxis. Usa: BLOCK nombre_usuario");
                        continue;
                    }
                    
                    String usuarioABloquear = partesComando[1];
                    
                    // Verificar que el usuario a bloquear exista
                    if (!DatabaseManager.usuarioExiste(usuarioABloquear)) {
                        salida.writeUTF("Error: El usuario '" + usuarioABloquear + "' no existe.");
                        continue;
                    }
                    
                    // Verificar que no se bloquee a sí mismo
                    if (usuarioABloquear.equals(idCliente)) {
                        salida.writeUTF("Error: No puedes bloquearte a ti mismo.");
                        continue;
                    }
                    
                    // Intentar bloquear
                    if (DatabaseManager.bloquearUsuario(idCliente, usuarioABloquear)) {
                        salida.writeUTF("Usuario '" + usuarioABloquear + "' bloqueado correctamente.");
                    } else {
                        salida.writeUTF("Error: El usuario '" + usuarioABloquear + "' ya está bloqueado.");
                    }
                    continue;
                }
                
                if (comando.equals("UNBLOCK")) {
                    if (!autenticado) {
                        salida.writeUTF("Error: Debes estar autenticado para desbloquear usuarios.");
                        continue;
                    }
                    
                    if (partesComando.length != 2) {
                        salida.writeUTF("Error de sintaxis. Usa: UNBLOCK nombre_usuario");
                        continue;
                    }
                    
                    String usuarioADesbloquear = partesComando[1];
                    
                    // Intentar desbloquear
                    if (DatabaseManager.desbloquearUsuario(idCliente, usuarioADesbloquear)) {
                        salida.writeUTF("Usuario '" + usuarioADesbloquear + "' desbloqueado correctamente.");
                    } else {
                        salida.writeUTF("Error: El usuario '" + usuarioADesbloquear + "' no está bloqueado.");
                    }
                    continue;
                }
                
                if (comando.equals("BLOCKLIST")) {
                    if (!autenticado) {
                        salida.writeUTF("Error: Debes estar autenticado para ver tu lista de bloqueados.");
                        continue;
                    }
                    
                    Set<String> bloqueados = DatabaseManager.obtenerBloqueados(idCliente);
                    
                    if (bloqueados.isEmpty()) {
                        salida.writeUTF("No tienes usuarios bloqueados.");
                    } else {
                        StringBuilder sb = new StringBuilder("Usuarios bloqueados: ");
                        for (String bloqueado : bloqueados) {
                            sb.append(bloqueado).append(", ");
                        }
                        // Eliminar la última coma y espacio
                        String lista = sb.substring(0, sb.length() - 2);
                        salida.writeUTF(lista);
                    }
                    continue;
                }
                
                // --- 3. Verificación de permisos para enviar ---
                if (!autenticado && mensajesEnviados >= 3) {
                    salida.writeUTF("Límite de 3 mensajes alcanzado. Debes autenticarte (ej: LOGIN nombre password) para enviar más.");
                    continue;
                }
                
                if (!autenticado) {
                    mensajesEnviados++;
                }

                // --- 4. Manejo de mensajes privados (@) ---
                if (mensaje.startsWith("@")) {
                    String[] partes = mensaje.split(" ", 2);
                    if (partes.length < 2) {
                         salida.writeUTF("Formato privado incorrecto. Usa: @ID_o_NOMBRE mensaje");
                         if (!autenticado) mensajesEnviados--;
                         continue;
                    }
                    
                    String aQuien = partes[0].substring(1);
                    UnCliente clienteDestino = ServidorMulti.clientes.get(aQuien);
                    
                    if (clienteDestino == null) {
                        salida.writeUTF("Error: Cliente con ID/Nombre '" + aQuien + "' no encontrado o desconectado.");
                        if (!autenticado) mensajesEnviados--;
                        continue;
                    }
                    
                    // Verificar si el remitente está bloqueado por el destinatario
                    if (autenticado && DatabaseManager.estaBloqueado(aQuien, idCliente)) {
                        salida.writeUTF("No puedes enviar mensajes a '" + aQuien + "' porque te ha bloqueado.");
                        if (!autenticado) mensajesEnviados--;
                        continue;
                    }
                    
                    // Verificar si el destinatario está bloqueado por el remitente
                    if (autenticado && DatabaseManager.estaBloqueado(idCliente, aQuien)) {
                        salida.writeUTF("No puedes enviar mensajes a '" + aQuien + "' porque lo has bloqueado.");
                        if (!autenticado) mensajesEnviados--;
                        continue;
                    }
                    
                    String remitente = idCliente;
                    String mensajeConRemitente = "(PRIVADO de " + remitente + "): " + partes[1];
                    clienteDestino.salida.writeUTF(mensajeConRemitente);
                    this.salida.writeUTF("(Mensaje enviado a " + aQuien + ")");
                    
                } else {
                    // --- 5. Manejo de mensajes públicos (Broadcast) ---
                    String remitente = idCliente;
                    String mensajeBroadcast = "[" + remitente + "]: " + mensaje;
                    
                    for (Map.Entry<String, UnCliente> entry : ServidorMulti.clientes.entrySet()) {
                        UnCliente cliente = entry.getValue();
                        
                        // No enviar a sí mismo
                        if (cliente == this) {
                            continue;
                        }
                        
                        // Si el remitente está autenticado, verificar bloqueos
                        if (autenticado) {
                            String nombreDestinatario = entry.getKey();
                            
                            // No enviar si el destinatario bloqueó al remitente
                            if (DatabaseManager.estaBloqueado(nombreDestinatario, idCliente)) {
                                continue;
                            }
                            
                            // No enviar si el remitente bloqueó al destinatario
                            if (DatabaseManager.estaBloqueado(idCliente, nombreDestinatario)) {
                                continue;
                            }
                        }
                        
                        cliente.salida.writeUTF(mensajeBroadcast);
                    }
                    
                    if (!autenticado) {
                        salida.writeUTF("Mensajes restantes: " + (3 - mensajesEnviados));
                    }
                }
            }
        } catch (IOException ex) {
            System.out.println("Cliente " + idCliente + " desconectado.");
            ServidorMulti.clientes.remove(idCliente);
        }
    }
}