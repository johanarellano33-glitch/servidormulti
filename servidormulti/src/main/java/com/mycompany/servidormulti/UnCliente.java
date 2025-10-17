package com.mycompany.servidormulti;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.ArrayList; 

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

    /**
     * Envía el menú de comandos disponibles
     */
    private void enviarMenuAyuda() throws IOException {
        StringBuilder menu = new StringBuilder();
        menu.append("\nCOMANDOS DISPONIBLES:\n");
        menu.append("  HELP              - Muestra este menú de ayuda\n");
        menu.append("  USERS             - Lista todos los usuarios registrados\n");
        menu.append("  ONLINE            - Lista usuarios conectados ahora\n");
        menu.append("  BLOCK nombre      - Bloquea a un usuario\n");
        menu.append("  UNBLOCK nombre    - Desbloquea a un usuario\n");
        menu.append("  BLOCKLIST         - Muestra tus usuarios bloqueados\n");
        menu.append("  @nombre mensaje   - Envía mensaje privado\n");
        menu.append("  mensaje           - Envía mensaje público (broadcast)\n");
        menu.append("  salir             - Cierra la conexión\n");
        salida.writeUTF(menu.toString());
    }

    /**
     * Envía mensaje de bienvenida después del login
     */
    private void enviarMensajeBienvenida() throws IOException {
        StringBuilder bienvenida = new StringBuilder();
        bienvenida.append("\nInicio de sesión exitoso. Bienvenido ").append(idCliente).append("!\n");
        bienvenida.append("Tienes mensajes ilimitados.\n");
        bienvenida.append("Escribe HELP para ver todos los comandos.\n");
        salida.writeUTF(bienvenida.toString());
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

           
                if (comando.equals("HELP")) {
                    if (!autenticado) {
                        salida.writeUTF("Debes iniciar sesión para ver los comandos. Usa: LOGIN nombre password");
                        continue;
                    }
                    enviarMenuAyuda();
                    continue;
                }

               
                if (comando.equals("USERS")) {
                    if (!autenticado) {
                        salida.writeUTF("Error: Debes estar autenticado para ver la lista de usuarios.");
                        continue;
                    }
                    
                    List<String> usuarios = DatabaseManager.obtenerTodosLosUsuarios();
                    
                    if (usuarios.isEmpty()) {
                        salida.writeUTF("No hay usuarios registrados.");
                    } else {
                        StringBuilder sb = new StringBuilder("\nUSUARIOS REGISTRADOS:\n");
                        for (String usuario : usuarios) {
                            if (usuario.equals(idCliente)) {
                                sb.append("  - ").append(usuario).append(" (tu)\n");
                            } else {
                                sb.append("  - ").append(usuario).append("\n");
                            }
                        }
                        sb.append("Total: ").append(usuarios.size()).append(" usuarios");
                        salida.writeUTF(sb.toString());
                    }
                    continue;
                }

               
                if (comando.equals("ONLINE")) {
                    if (!autenticado) {
                        salida.writeUTF("Error: Debes estar autenticado para ver usuarios conectados.");
                        continue;
                    }
                    
                    List<String> conectados = DatabaseManager.obtenerUsuariosConectados(idCliente);
                    
                    if (conectados.isEmpty()) {
                        salida.writeUTF("No hay otros usuarios conectados en este momento.");
                    } else {
                        StringBuilder sb = new StringBuilder("\nUSUARIOS CONECTADOS:\n");
                        for (String usuario : conectados) {
                            sb.append("  - ").append(usuario).append("\n");
                        }
                        sb.append("Total: ").append(conectados.size()).append(" conectados");
                        salida.writeUTF(sb.toString());
                    }
                    continue;
                }

               
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
                            salida.writeUTF("Registro exitoso! Ahora usa LOGIN " + nombre + " [tu_password]");
                        } else {
                            salida.writeUTF("Error: El nombre de usuario '" + nombre + "' ya existe.");
                        }
                    } else if (comando.equals("LOGIN")) {
                        if (ServidorMulti.verificarCredenciales(nombre, password)) {
                            autenticado = true;
                            ServidorMulti.clientes.remove(idCliente);
                            idCliente = nombre;
                            ServidorMulti.clientes.put(idCliente, this);
                            
                            enviarMensajeBienvenida();
                            System.out.println("Cliente se autenticó como " + nombre);
                        } else {
                            salida.writeUTF("Error de inicio de sesión. Credenciales incorrectas.");
                        }
                    }
                    continue;
                }
                
               
                if (comando.equals("BLOCK")) {
                    if (!autenticado) {
                        salida.writeUTF("Error: Debes estar autenticado para bloquear usuarios.");
                        continue;
                    }
                    
                    if (partesComando.length != 2) {
                        salida.writeUTF("Error de sintaxis. Usa: BLOCK nombre_usuario\nPara ver usuarios disponibles usa: USERS");
                        continue;
                    }
                    
                    String usuarioABloquear = partesComando[1];
                    
                    if (!DatabaseManager.usuarioExiste(usuarioABloquear)) {
                        salida.writeUTF("Error: El usuario '" + usuarioABloquear + "' no existe.\nUsa USERS para ver usuarios disponibles.");
                        continue;
                    }
                    
                    if (usuarioABloquear.equals(idCliente)) {
                        salida.writeUTF("Error: No puedes bloquearte a ti mismo.");
                        continue;
                    }
                    
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
                        salida.writeUTF("Error de sintaxis. Usa: UNBLOCK nombre_usuario\nPara ver bloqueados usa: BLOCKLIST");
                        continue;
                    }
                    
                    String usuarioADesbloquear = partesComando[1];
                    
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
                        StringBuilder sb = new StringBuilder("\nUSUARIOS BLOQUEADOS:\n");
                        for (String bloqueado : bloqueados) {
                            sb.append("  - ").append(bloqueado).append("\n");
                        }
                        sb.append("Total: ").append(bloqueados.size()).append(" bloqueados");
                        salida.writeUTF(sb.toString());
                    }
                    continue;
                }
                
                // --- 6. Verificación de permisos para enviar ---
                if (!autenticado && mensajesEnviados >= 3) {
                    salida.writeUTF("Límite de 3 mensajes alcanzado. Debes autenticarte (ej: LOGIN nombre password) para enviar más.");
                    continue;
                }
                
                if (!autenticado) {
                    mensajesEnviados++;
                }

                // --- 7. Manejo de mensajes privados (@) ---
                if (mensaje.startsWith("@")) {
                    String[] partes = mensaje.split(" ", 2);
                    if (partes.length < 2) {
                         salida.writeUTF("Formato privado incorrecto. Usa: @nombre mensaje");
                         if (!autenticado) mensajesEnviados--;
                         continue;
                    }
                    
                    String aQuien = partes[0].substring(1);
                    UnCliente clienteDestino = ServidorMulti.clientes.get(aQuien);
                    
                    if (clienteDestino == null) {
                        salida.writeUTF("Error: Cliente con nombre '" + aQuien + "' no encontrado o desconectado.\nUsa ONLINE para ver usuarios conectados.");
                        if (!autenticado) mensajesEnviados--;
                        continue;
                    }
                    
                    if (autenticado && DatabaseManager.estaBloqueado(aQuien, idCliente)) {
                        salida.writeUTF("No puedes enviar mensajes a '" + aQuien + "' porque te ha bloqueado.");
                        if (!autenticado) mensajesEnviados--;
                        continue;
                    }
                    
                    if (autenticado && DatabaseManager.estaBloqueado(idCliente, aQuien)) {
                        salida.writeUTF("No puedes enviar mensajes a '" + aQuien + "' porque lo has bloqueado.");
                        if (!autenticado) mensajesEnviados--;
                        continue;
                    }
                    
                    String remitente = idCliente;
                    String mensajeConRemitente = "(PRIVADO de " + remitente + "): " + partes[1];
                    clienteDestino.salida.writeUTF(mensajeConRemitente);
                    this.salida.writeUTF("Mensaje enviado a " + aQuien);
                    
                } else {
                    // --- 8. Manejo de mensajes públicos (Broadcast) ---
                    String remitente = idCliente;
                    String mensajeBroadcast = "[" + remitente + "]: " + mensaje;
                    
                    for (Map.Entry<String, UnCliente> entry : ServidorMulti.clientes.entrySet()) {
                        UnCliente cliente = entry.getValue();
                        
                        if (cliente == this) {
                            continue;
                        }
                        
                        if (autenticado) {
                            String nombreDestinatario = entry.getKey();
                            
                            if (DatabaseManager.estaBloqueado(nombreDestinatario, idCliente)) {
                                continue;
                            }
                            
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