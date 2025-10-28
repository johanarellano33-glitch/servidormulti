package com.mycompany.servidormulti;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Random;

public class UnCliente implements Runnable {
    
    final DataOutputStream salida;
    final DataInputStream entrada;
    
    private String idCliente;
    private int mensajesEnviados = 0;
    private boolean autenticado = false;
    
    // Almacena las invitaciones pendientes: clave = nombre del invitador
    private String invitacionPendiente = null;
    
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
        menu.append("  JUGAR nombre      - Invita a un usuario a jugar al gato\n");
        menu.append("  ACEPTAR           - Acepta una invitación de juego\n");
        menu.append("  RECHAZAR          - Rechaza una invitación de juego\n");
        menu.append("  MOVER fila col    - Realiza un movimiento (ej: MOVER 0 1)\n");
        menu.append("  RENDIRSE          - Te rindes en el juego actual\n");
        menu.append("  JUEGOS            - Muestra tus juegos activos\n");
        menu.append("  RANKING           - Muestra el ranking general de jugadores\n");
        menu.append("  VS nombre1 nombre2 - Compara estadísticas entre 2 jugadores\n");
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

                // --- 1. Comando HELP ---
                if (comando.equals("HELP")) {
                    if (!autenticado) {
                        salida.writeUTF("Debes iniciar sesión para ver los comandos. Usa: LOGIN nombre password");
                        continue;
                    }
                    enviarMenuAyuda();
                    continue;
                }

                // --- 2. Comando USERS (listar todos los usuarios registrados) ---
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

                // --- 3. Comando ONLINE (listar usuarios conectados) ---
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

                // --- 4. Manejo de comandos REGISTER / LOGIN ---
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
                
                // --- 5. Comandos BLOCK / UNBLOCK / BLOCKLIST ---
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
                
                // --- 6. Comando JUGAR (invitar a jugar al gato) ---
                if (comando.equals("JUGAR")) {
                    if (!autenticado) {
                        salida.writeUTF("Error: Debes estar autenticado para jugar.");
                        continue;
                    }
                    
                    if (partesComando.length != 2) {
                        salida.writeUTF("Error de sintaxis. Usa: JUGAR nombre_usuario");
                        continue;
                    }
                    
                    String oponente = partesComando[1];
                    
                    // Verificar que el oponente no sea el mismo usuario
                    if (oponente.equals(idCliente)) {
                        salida.writeUTF("Error: No puedes jugar contigo mismo.");
                        continue;
                    }
                    
                    // Verificar que el oponente esté conectado
                    UnCliente clienteOponente = ServidorMulti.clientes.get(oponente);
                    if (clienteOponente == null) {
                        salida.writeUTF("Error: El usuario '" + oponente + "' no está conectado.\nUsa ONLINE para ver usuarios conectados.");
                        continue;
                    }
                    
                    // Verificar que no haya bloqueos
                    if (DatabaseManager.estaBloqueado(oponente, idCliente)) {
                        salida.writeUTF("Error: No puedes jugar con '" + oponente + "' porque te ha bloqueado.");
                        continue;
                    }
                    
                    if (DatabaseManager.estaBloqueado(idCliente, oponente)) {
                        salida.writeUTF("Error: No puedes jugar con '" + oponente + "' porque lo has bloqueado.");
                        continue;
                    }
                    
                    // Verificar que no exista ya un juego entre estos dos jugadores
                    String claveJuego = ServidorMulti.generarClaveJuego(idCliente, oponente);
                    if (ServidorMulti.juegosActivos.containsKey(claveJuego)) {
                        salida.writeUTF("Error: Ya tienes un juego activo con '" + oponente + "'.");
                        continue;
                    }
                    
                    // Enviar invitación al oponente
                    clienteOponente.invitacionPendiente = idCliente;
                    clienteOponente.salida.writeUTF("\n*** " + idCliente + " te ha invitado a jugar al GATO ***\n" +
                                                     "Escribe ACEPTAR para jugar o RECHAZAR para declinar.");
                    salida.writeUTF("Invitación enviada a '" + oponente + "'. Esperando respuesta...");
                    continue;
                }
                
                // --- 7. Comando ACEPTAR (aceptar invitación de juego) ---
                if (comando.equals("ACEPTAR")) {
                    if (!autenticado) {
                        salida.writeUTF("Error: Debes estar autenticado.");
                        continue;
                    }
                    
                    if (invitacionPendiente == null) {
                        salida.writeUTF("Error: No tienes invitaciones pendientes.");
                        continue;
                    }
                    
                    String invitador = invitacionPendiente;
                    UnCliente clienteInvitador = ServidorMulti.clientes.get(invitador);
                    
                    if (clienteInvitador == null) {
                        salida.writeUTF("Error: El usuario '" + invitador + "' ya no está conectado.");
                        invitacionPendiente = null;
                        continue;
                    }
                    
                    // Crear el juego
                    Random random = new Random();
                    boolean invitadorEmpieza = random.nextBoolean();
                    
                    String claveJuego = ServidorMulti.generarClaveJuego(invitador, idCliente);
                    gato juego = new gato(invitador, idCliente, invitadorEmpieza);
                    ServidorMulti.juegosActivos.put(claveJuego, juego);
                    
                    invitacionPendiente = null;
                    
                    // Notificar a ambos jugadores
                    String tablero = juego.obtenerTableroTexto();
                    String infoJuego = "\n*** JUEGO INICIADO ***\n" +
                                       invitador + " (" + juego.getSimbolo(invitador) + ") vs " + 
                                       idCliente + " (" + juego.getSimbolo(idCliente) + ")\n" +
                                       "Empieza: " + juego.getTurnoActual() + "\n" +
                                       tablero +
                                       "Usa: MOVER fila columna (ejemplo: MOVER 0 1)";
                    
                    clienteInvitador.salida.writeUTF(infoJuego);
                    this.salida.writeUTF(infoJuego);
                    continue;
                }
                
                // --- 8. Comando RECHAZAR (rechazar invitación) ---
                if (comando.equals("RECHAZAR")) {
                    if (!autenticado) {
                        salida.writeUTF("Error: Debes estar autenticado.");
                        continue;
                    }
                    
                    if (invitacionPendiente == null) {
                        salida.writeUTF("Error: No tienes invitaciones pendientes.");
                        continue;
                    }
                    
                    String invitador = invitacionPendiente;
                    UnCliente clienteInvitador = ServidorMulti.clientes.get(invitador);
                    
                    if (clienteInvitador != null) {
                        clienteInvitador.salida.writeUTF(idCliente + " ha rechazado tu invitación de juego.");
                    }
                    
                    salida.writeUTF("Has rechazado la invitación de " + invitador);
                    invitacionPendiente = null;
                    continue;
                }
                
                // --- 9. Comando MOVER (realizar un movimiento en el gato) ---
                if (comando.equals("MOVER")) {
                    if (!autenticado) {
                        salida.writeUTF("Error: Debes estar autenticado.");
                        continue;
                    }
                    
                    if (partesComando.length != 3) {
                        salida.writeUTF("Error de sintaxis. Usa: MOVER fila columna (ejemplo: MOVER 0 1)");
                        continue;
                    }
                    
                    // Buscar si el usuario tiene algún juego activo
                    gato juegoActual = null;
                    String claveJuegoActual = null;
                    String oponente = null;
                    
                    for (Map.Entry<String, gato> entry : ServidorMulti.juegosActivos.entrySet()) {
                        gato juego = entry.getValue();
                        if (juego.getJugador1().equals(idCliente) || juego.getJugador2().equals(idCliente)) {
                            juegoActual = juego;
                            claveJuegoActual = entry.getKey();
                            oponente = juego.getJugador1().equals(idCliente) ? juego.getJugador2() : juego.getJugador1();
                            break;
                        }
                    }
                    
                    if (juegoActual == null) {
                        salida.writeUTF("Error: No tienes ningún juego activo.");
                        continue;
                    }
                    
                    // Parsear fila y columna
                    int fila, columna;
                    try {
                        fila = Integer.parseInt(partesComando[1]);
                        columna = Integer.parseInt(partesComando[2]);
                    } catch (NumberFormatException e) {
                        salida.writeUTF("Error: Fila y columna deben ser números entre 0 y 2.");
                        continue;
                    }
                    
                    // Realizar el movimiento
                    if (!juegoActual.realizarMovimiento(idCliente, fila, columna)) {
                        if (!juegoActual.getTurnoActual().equals(idCliente)) {
                            salida.writeUTF("Error: No es tu turno. Espera a que " + oponente + " haga su movimiento.");
                        } else if (fila < 0 || fila > 2 || columna < 0 || columna > 2) {
                            salida.writeUTF("Error: Posición inválida. Usa valores entre 0 y 2.");
                        } else {
                            salida.writeUTF("Error: Esa casilla ya está ocupada. Elige otra.");
                        }
                        continue;
                    }
                    
                    // Mostrar el tablero actualizado a ambos jugadores
                    String tablero = juegoActual.obtenerTableroTexto();
                    UnCliente clienteOponente = ServidorMulti.clientes.get(oponente);
                    
                    if (juegoActual.isJuegoTerminado()) {
                        String resultado;
                        if (juegoActual.getGanador().equals("EMPATE")) {
                            resultado = "\n*** JUEGO TERMINADO - EMPATE ***\n" + tablero;
                            // Registrar empate para ambos jugadores
                            DatabaseManager.registrarEmpate(idCliente);
                            DatabaseManager.registrarEmpate(oponente);
                        } else {
                            resultado = "\n*** JUEGO TERMINADO ***\n" + tablero + 
                                       "GANADOR: " + juegoActual.getGanador() + "!";
                            // Registrar victoria y derrota
                            DatabaseManager.registrarVictoria(juegoActual.getGanador());
                            String perdedor = juegoActual.getGanador().equals(idCliente) ? oponente : idCliente;
                            DatabaseManager.registrarDerrota(perdedor);
                        }
                        
                        this.salida.writeUTF(resultado);
                        if (clienteOponente != null) {
                            clienteOponente.salida.writeUTF(resultado);
                        }
                        
                        ServidorMulti.juegosActivos.remove(claveJuegoActual);
                    } else {
                        String estado = tablero + "Turno de: " + juegoActual.getTurnoActual();
                        this.salida.writeUTF(estado);
                        if (clienteOponente != null) {
                            clienteOponente.salida.writeUTF(estado);
                        }
                    }
                    continue;
                }
                
                // --- 10. Comando RENDIRSE ---
                if (comando.equals("RENDIRSE")) {
                    if (!autenticado) {
                        salida.writeUTF("Error: Debes estar autenticado.");
                        continue;
                    }
                    
                    // Buscar si el usuario tiene algún juego activo
                    gato juegoActual = null;
                    String claveJuegoActual = null;
                    String oponente = null;
                    
                    for (Map.Entry<String, gato> entry : ServidorMulti.juegosActivos.entrySet()) {
                        gato juego = entry.getValue();
                        if (juego.getJugador1().equals(idCliente) || juego.getJugador2().equals(idCliente)) {
                            juegoActual = juego;
                            claveJuegoActual = entry.getKey();
                            oponente = juego.getJugador1().equals(idCliente) ? juego.getJugador2() : juego.getJugador1();
                            break;
                        }
                    }
                    
                    if (juegoActual == null) {
                        salida.writeUTF("Error: No tienes ningún juego activo.");
                        continue;
                    }
                    
                    juegoActual.terminarJuego(idCliente);
                    
                    salida.writeUTF("Te has rendido. " + oponente + " gana el juego.");
                    UnCliente clienteOponente = ServidorMulti.clientes.get(oponente);
                    if (clienteOponente != null) {
                        clienteOponente.salida.writeUTF(idCliente + " se ha rendido. Has ganado el juego!");
                    }
                    
                    ServidorMulti.juegosActivos.remove(claveJuegoActual);
                    continue;
                }
                
                // --- 11. Comando JUEGOS (ver juegos activos) ---
                if (comando.equals("JUEGOS")) {
                    if (!autenticado) {
                        salida.writeUTF("Error: Debes estar autenticado.");
                        continue;
                    }
                    
                    StringBuilder sb = new StringBuilder("\nTUS JUEGOS ACTIVOS:\n");
                    int contador = 0;
                    
                    for (Map.Entry<String, gato> entry : ServidorMulti.juegosActivos.entrySet()) {
                        gato juego = entry.getValue();
                        if (juego.getJugador1().equals(idCliente) || juego.getJugador2().equals(idCliente)) {
                            String oponente = juego.getJugador1().equals(idCliente) ? juego.getJugador2() : juego.getJugador1();
                            sb.append("  - Jugando contra: ").append(oponente);
                            sb.append(" | Turno de: ").append(juego.getTurnoActual()).append("\n");
                            contador++;
                        }
                    }
                    
                    if (contador == 0) {
                        salida.writeUTF("No tienes juegos activos.");
                    } else {
                        sb.append("Total: ").append(contador).append(" juegos");
                        salida.writeUTF(sb.toString());
                    }
                    continue;
                }
                
                // --- 12. Comando RANKING (ver ranking general) ---
                if (comando.equals("RANKING")) {
                    if (!autenticado) {
                        salida.writeUTF("Error: Debes estar autenticado para ver el ranking.");
                        continue;
                    }
                    
                    List<String> ranking = DatabaseManager.obtenerRankingGeneral();
                    
                    if (ranking.isEmpty()) {
                        salida.writeUTF("Aún no hay jugadores en el ranking. ¡Sé el primero en jugar!");
                    } else {
                        StringBuilder sb = new StringBuilder("\n========== RANKING GENERAL ==========\n");
                        for (String linea : ranking) {
                            sb.append(linea).append("\n");
                        }
                        sb.append("=====================================");
                        salida.writeUTF(sb.toString());
                    }
                    continue;
                }
                
                // --- 13. Comando VS (comparar estadísticas entre dos jugadores) ---
                if (comando.equals("VS")) {
                    if (!autenticado) {
                        salida.writeUTF("Error: Debes estar autenticado.");
                        continue;
                    }
                    
                    if (partesComando.length != 3) {
                        salida.writeUTF("Error de sintaxis. Usa: VS nombre1 nombre2");
                        continue;
                    }
                    
                    String jugador1 = partesComando[1];
                    String jugador2 = partesComando[2];
                    
                    String resultado = DatabaseManager.obtenerEstadisticasVS(jugador1, jugador2);
                    salida.writeUTF(resultado);
                    continue;
                }
                
                // --- 14. Verificación de permisos para enviar ---
                if (!autenticado && mensajesEnviados >= 3) {
                    salida.writeUTF("Límite de 3 mensajes alcanzado. Debes autenticarte (ej: LOGIN nombre password) para enviar más.");
                    continue;
                }
                
                if (!autenticado) {
                    mensajesEnviados++;
                }

                // --- 13. Manejo de mensajes privados (@) ---
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
                    // --- 14. Manejo de mensajes públicos (Broadcast) ---
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
            
            // Terminar todos los juegos donde participe este cliente
            if (autenticado) {
                for (Map.Entry<String, gato> entry : ServidorMulti.juegosActivos.entrySet()) {
                    gato juego = entry.getValue();
                    if (juego.getJugador1().equals(idCliente) || juego.getJugador2().equals(idCliente)) {
                        String oponente = juego.getJugador1().equals(idCliente) ? juego.getJugador2() : juego.getJugador1();
                        juego.terminarJuego(idCliente);
                        
                        UnCliente clienteOponente = ServidorMulti.clientes.get(oponente);
                        if (clienteOponente != null) {
                            try {
                                clienteOponente.salida.writeUTF(idCliente + " se ha desconectado. Has ganado el juego por abandono.");
                            } catch (IOException e) {
                                // Ignorar si el oponente también se desconectó
                            }
                        }
                        
                        ServidorMulti.juegosActivos.remove(entry.getKey());
                    }
                }
            }
            
            ServidorMulti.clientes.remove(idCliente);
        }
    }
}