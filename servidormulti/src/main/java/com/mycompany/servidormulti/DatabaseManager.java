package com.mycompany.servidormulti;

import java.sql.*;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:chat.db";
    private static Connection conn = null;

   
    public static void inicializar() {
        try {
            conn = DriverManager.getConnection(DB_URL);
            conn.setAutoCommit(true); 
            crearTablas();
            System.out.println("Base de datos inicializada correctamente.");
        } catch (SQLException e) {
            System.err.println("Error al inicializar la base de datos: " + e.getMessage());
        }
    }

  
    private static void crearTablas() throws SQLException {
    String sqlUsuarios = "CREATE TABLE IF NOT EXISTS usuarios ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "nombre TEXT UNIQUE NOT NULL, "
            + "password TEXT NOT NULL"
            + ");";

    String sqlBloqueos = "CREATE TABLE IF NOT EXISTS bloqueos ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "usuario_bloqueador TEXT NOT NULL, "
            + "usuario_bloqueado TEXT NOT NULL, "
            + "FOREIGN KEY (usuario_bloqueador) REFERENCES usuarios(nombre), "
            + "FOREIGN KEY (usuario_bloqueado) REFERENCES usuarios(nombre), "
            + "UNIQUE(usuario_bloqueador, usuario_bloqueado)"
            + ");";

    String sqlEstadisticas = "CREATE TABLE IF NOT EXISTS estadisticas ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "jugador TEXT NOT NULL, "
            + "victorias INTEGER DEFAULT 0, "
            + "empates INTEGER DEFAULT 0, "
            + "derrotas INTEGER DEFAULT 0, "
            + "puntos INTEGER DEFAULT 0, "
            + "FOREIGN KEY (jugador) REFERENCES usuarios(nombre), "
            + "UNIQUE(jugador)"
            + ");";

    String sqlGrupos = "CREATE TABLE IF NOT EXISTS grupos ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "nombre TEXT UNIQUE NOT NULL, "
            + "creador TEXT NOT NULL, "
            + "fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
            + "FOREIGN KEY (creador) REFERENCES usuarios(nombre)"
            + ");";

    String sqlMiembrosGrupo = "CREATE TABLE IF NOT EXISTS miembros_grupo ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "grupo_nombre TEXT NOT NULL, "
            + "usuario TEXT NOT NULL, "
            + "fecha_union TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
            + "FOREIGN KEY (grupo_nombre) REFERENCES grupos(nombre), "
            + "FOREIGN KEY (usuario) REFERENCES usuarios(nombre), "
            + "UNIQUE(grupo_nombre, usuario)"
            + ");";

    String sqlMensajesGrupo = "CREATE TABLE IF NOT EXISTS mensajes_grupo ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "grupo_nombre TEXT NOT NULL, "
            + "remitente TEXT NOT NULL, "
            + "mensaje TEXT NOT NULL, "
            + "fecha_envio TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
            + "FOREIGN KEY (grupo_nombre) REFERENCES grupos(nombre), "
            + "FOREIGN KEY (remitente) REFERENCES usuarios(nombre)"
            + ");";

    String sqlMensajesLeidos = "CREATE TABLE IF NOT EXISTS mensajes_leidos ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "usuario TEXT NOT NULL, "
            + "mensaje_id INTEGER NOT NULL, "
            + "FOREIGN KEY (usuario) REFERENCES usuarios(nombre), "
            + "FOREIGN KEY (mensaje_id) REFERENCES mensajes_grupo(id), "
            + "UNIQUE(usuario, mensaje_id)"
            + ");";

    try (Statement stmt = conn.createStatement()) {
        stmt.execute(sqlUsuarios);
        stmt.execute(sqlBloqueos);
        stmt.execute(sqlEstadisticas);
        stmt.execute(sqlGrupos);
        stmt.execute(sqlMiembrosGrupo);
        stmt.execute(sqlMensajesGrupo);
        stmt.execute(sqlMensajesLeidos);
        
        
        crearGrupoTodos();
    }
}


private static void crearGrupoTodos() {
    String sql = "INSERT OR IGNORE INTO grupos (nombre, creador) VALUES ('Todos', 'SISTEMA')";
    try (Statement stmt = conn.createStatement()) {
        stmt.execute(sql);
    } catch (SQLException e) {
        System.err.println("Error al crear grupo Todos: " + e.getMessage());
    }
}
    
    public static boolean registrarUsuario(String nombre, String password) {
        String sql = "INSERT INTO usuarios (nombre, password) VALUES (?, ?)";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nombre);
            pstmt.setString(2, password);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            if (e.getErrorCode() == 19) {
                return false;
            }
            System.err.println("Error al registrar usuario: " + e.getMessage());
            return false;
        }
    }

    
    public static boolean verificarCredenciales(String nombre, String password) {
        String sql = "SELECT password FROM usuarios WHERE nombre = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nombre);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                String passwordGuardado = rs.getString("password");
                return passwordGuardado.equals(password);
            }
            return false;
        } catch (SQLException e) {
            System.err.println("Error al verificar credenciales: " + e.getMessage());
            return false;
        }
    }

    
    public static boolean usuarioExiste(String nombre) {
        String sql = "SELECT 1 FROM usuarios WHERE nombre = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nombre);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.err.println("Error al verificar existencia de usuario: " + e.getMessage());
            return false;
        }
    }

    
    public static List<String> obtenerTodosLosUsuarios() {
        List<String> usuarios = new ArrayList<>();
        String sql = "SELECT nombre FROM usuarios ORDER BY nombre";
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                usuarios.add(rs.getString("nombre"));
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener lista de usuarios: " + e.getMessage());
        }
        
        return usuarios;
    }

   
    public static List<String> obtenerUsuariosConectados(String usuarioActual) {
        List<String> conectados = new ArrayList<>();
        
        for (String nombre : ServidorMulti.clientes.keySet()) {
            if (!nombre.matches("\\d+") && !nombre.equals(usuarioActual)) {
                conectados.add(nombre);
            }
        }
        
        return conectados;
    }

    
    public static boolean bloquearUsuario(String usuarioBloqueador, String usuarioBloqueado) {
        if (usuarioBloqueador.equals(usuarioBloqueado)) {
            return false;
        }

        String sql = "INSERT INTO bloqueos (usuario_bloqueador, usuario_bloqueado) VALUES (?, ?)";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, usuarioBloqueador);
            pstmt.setString(2, usuarioBloqueado);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            if (e.getErrorCode() == 19) {
                return false;
            }
            System.err.println("Error al bloquear usuario: " + e.getMessage());
            return false;
        }
    }

   
    public static boolean desbloquearUsuario(String usuarioBloqueador, String usuarioBloqueado) {
        String sql = "DELETE FROM bloqueos WHERE usuario_bloqueador = ? AND usuario_bloqueado = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, usuarioBloqueador);
            pstmt.setString(2, usuarioBloqueado);
            int filasAfectadas = pstmt.executeUpdate();
            return filasAfectadas > 0;
        } catch (SQLException e) {
            System.err.println("Error al desbloquear usuario: " + e.getMessage());
            return false;
        }
    }

   
    public static boolean estaBloqueado(String usuarioBloqueador, String usuarioBloqueado) {
        String sql = "SELECT 1 FROM bloqueos WHERE usuario_bloqueador = ? AND usuario_bloqueado = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, usuarioBloqueador);
            pstmt.setString(2, usuarioBloqueado);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.err.println("Error al verificar bloqueo: " + e.getMessage());
            return false;
        }
    }

   
    public static Set<String> obtenerBloqueados(String usuarioBloqueador) {
        Set<String> bloqueados = new HashSet<>();
        String sql = "SELECT usuario_bloqueado FROM bloqueos WHERE usuario_bloqueador = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, usuarioBloqueador);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                bloqueados.add(rs.getString("usuario_bloqueado"));
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener lista de bloqueados: " + e.getMessage());
        }
        
        return bloqueados;
    }

    
    public static void inicializarEstadisticas(String jugador) {
        String sql = "INSERT OR IGNORE INTO estadisticas (jugador, victorias, empates, derrotas, puntos) VALUES (?, 0, 0, 0, 0)";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, jugador);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error al inicializar estadísticas: " + e.getMessage());
        }
    }

   
    public static void registrarVictoria(String jugador) {
        inicializarEstadisticas(jugador);
        String sql = "UPDATE estadisticas SET victorias = victorias + 1, puntos = puntos + 2 WHERE jugador = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, jugador);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error al registrar victoria: " + e.getMessage());
        }
    }

   
    public static void registrarEmpate(String jugador) {
        inicializarEstadisticas(jugador);
        String sql = "UPDATE estadisticas SET empates = empates + 1, puntos = puntos + 1 WHERE jugador = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, jugador);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error al registrar empate: " + e.getMessage());
        }
    }

   
    public static void registrarDerrota(String jugador) {
        inicializarEstadisticas(jugador);
        String sql = "UPDATE estadisticas SET derrotas = derrotas + 1 WHERE jugador = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, jugador);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error al registrar derrota: " + e.getMessage());
        }
    }

   
    public static List<String> obtenerRankingGeneral() {
        List<String> ranking = new ArrayList<>();
        String sql = "SELECT jugador, victorias, empates, derrotas, puntos " +
                     "FROM estadisticas " +
                     "WHERE (victorias + empates + derrotas) > 0 " +
                     "ORDER BY puntos DESC, victorias DESC";
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            int posicion = 1;
            while (rs.next()) {
                String jugador = rs.getString("jugador");
                int victorias = rs.getInt("victorias");
                int empates = rs.getInt("empates");
                int derrotas = rs.getInt("derrotas");
                int puntos = rs.getInt("puntos");
                
                String linea = String.format("%d. %-15s | V:%d E:%d D:%d | Puntos: %d",
                        posicion, jugador, victorias, empates, derrotas, puntos);
                ranking.add(linea);
                posicion++;
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener ranking: " + e.getMessage());
        }
        
        return ranking;
    }

    
    public static String obtenerEstadisticasVS(String jugador1, String jugador2) {
       
        if (!usuarioExiste(jugador1)) {
            return "Error: El usuario '" + jugador1 + "' no existe.";
        }
        if (!usuarioExiste(jugador2)) {
            return "Error: El usuario '" + jugador2 + "' no existe.";
        }

        inicializarEstadisticas(jugador1);
        inicializarEstadisticas(jugador2);

        String sql = "SELECT jugador, victorias, empates, derrotas, puntos FROM estadisticas WHERE jugador IN (?, ?)";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, jugador1);
            pstmt.setString(2, jugador2);
            ResultSet rs = pstmt.executeQuery();
            
            int v1 = 0, e1 = 0, d1 = 0, p1 = 0;
            int v2 = 0, e2 = 0, d2 = 0, p2 = 0;
            
            while (rs.next()) {
                String jugador = rs.getString("jugador");
                if (jugador.equals(jugador1)) {
                    v1 = rs.getInt("victorias");
                    e1 = rs.getInt("empates");
                    d1 = rs.getInt("derrotas");
                    p1 = rs.getInt("puntos");
                } else {
                    v2 = rs.getInt("victorias");
                    e2 = rs.getInt("empates");
                    d2 = rs.getInt("derrotas");
                    p2 = rs.getInt("puntos");
                }
            }
            
            int totalJuegos1 = v1 + e1 + d1;
            int totalJuegos2 = v2 + e2 + d2;
            
            double porcentajeV1 = totalJuegos1 > 0 ? (v1 * 100.0 / totalJuegos1) : 0;
            double porcentajeV2 = totalJuegos2 > 0 ? (v2 * 100.0 / totalJuegos2) : 0;
            
            StringBuilder sb = new StringBuilder();
            sb.append("\n========== ESTADISTICAS VS ==========\n");
            sb.append(String.format("%-15s | V:%d E:%d D:%d | Puntos: %d | Victorias: %.1f%%\n",
                    jugador1, v1, e1, d1, p1, porcentajeV1));
            sb.append(String.format("%-15s | V:%d E:%d D:%d | Puntos: %d | Victorias: %.1f%%\n",
                    jugador2, v2, e2, d2, p2, porcentajeV2));
            sb.append("=====================================");
            
            return sb.toString();
            
        } catch (SQLException e) {
            System.err.println("Error al obtener estadísticas VS: " + e.getMessage());
            return "Error al obtener estadísticas.";
        }
    }

public static boolean crearGrupo(String nombreGrupo, String creador) {
    if (nombreGrupo.equalsIgnoreCase("Todos")) {
        return false; 
    }
    
    String sql = "INSERT INTO grupos (nombre, creador) VALUES (?, ?)";
    
    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, nombreGrupo);
        pstmt.setString(2, creador);
        pstmt.executeUpdate();
        
        
        unirseAGrupo(nombreGrupo, creador);
        return true;
    } catch (SQLException e) {
        if (e.getErrorCode() == 19) {
            return false; 
        }
        System.err.println("Error al crear grupo: " + e.getMessage());
        return false;
    }
}


public static boolean eliminarGrupo(String nombreGrupo, String usuario) {
    if (nombreGrupo.equalsIgnoreCase("Todos")) {
        return false; 
    }
    
   
    String sqlVerificar = "SELECT creador FROM grupos WHERE nombre = ?";
    try (PreparedStatement pstmt = conn.prepareStatement(sqlVerificar)) {
        pstmt.setString(1, nombreGrupo);
        ResultSet rs = pstmt.executeQuery();
        
        if (!rs.next()) {
            return false; 
        }
        
        String creador = rs.getString("creador");
        if (!creador.equals(usuario)) {
            return false; 
        }
    } catch (SQLException e) {
        System.err.println("Error al verificar creador: " + e.getMessage());
        return false;
    }
    
   
    try (Statement stmt = conn.createStatement()) {
        stmt.execute("DELETE FROM mensajes_leidos WHERE mensaje_id IN (SELECT id FROM mensajes_grupo WHERE grupo_nombre = '" + nombreGrupo + "')");
        stmt.execute("DELETE FROM mensajes_grupo WHERE grupo_nombre = '" + nombreGrupo + "'");
        stmt.execute("DELETE FROM miembros_grupo WHERE grupo_nombre = '" + nombreGrupo + "'");
        stmt.execute("DELETE FROM grupos WHERE nombre = '" + nombreGrupo + "'");
        return true;
    } catch (SQLException e) {
        System.err.println("Error al eliminar grupo: " + e.getMessage());
        return false;
    }
}


public static boolean unirseAGrupo(String nombreGrupo, String usuario) {
    
    if (!grupoExiste(nombreGrupo)) {
        return false;
    }
    
    String sql = "INSERT INTO miembros_grupo (grupo_nombre, usuario) VALUES (?, ?)";
    
    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, nombreGrupo);
        pstmt.setString(2, usuario);
        pstmt.executeUpdate();
        return true;
    } catch (SQLException e) {
        if (e.getErrorCode() == 19) {
            return false; 
        }
        System.err.println("Error al unirse al grupo: " + e.getMessage());
        return false;
    }
}


public static boolean salirDeGrupo(String nombreGrupo, String usuario) {
    if (nombreGrupo.equalsIgnoreCase("Todos")) {
        return false; // No se puede salir del grupo "Todos"
    }
    
    String sql = "DELETE FROM miembros_grupo WHERE grupo_nombre = ? AND usuario = ?";
    
    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, nombreGrupo);
        pstmt.setString(2, usuario);
        int filasAfectadas = pstmt.executeUpdate();
        return filasAfectadas > 0;
    } catch (SQLException e) {
        System.err.println("Error al salir del grupo: " + e.getMessage());
        return false;
    }
}


public static boolean grupoExiste(String nombreGrupo) {
    String sql = "SELECT 1 FROM grupos WHERE nombre = ?";
    
    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, nombreGrupo);
        ResultSet rs = pstmt.executeQuery();
        return rs.next();
    } catch (SQLException e) {
        System.err.println("Error al verificar grupo: " + e.getMessage());
        return false;
    }
}


public static boolean esMiembroDeGrupo(String nombreGrupo, String usuario) {
    String sql = "SELECT 1 FROM miembros_grupo WHERE grupo_nombre = ? AND usuario = ?";
    
    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, nombreGrupo);
        pstmt.setString(2, usuario);
        ResultSet rs = pstmt.executeQuery();
        return rs.next();
    } catch (SQLException e) {
        System.err.println("Error al verificar membresía: " + e.getMessage());
        return false;
    }
}


public static List<String> obtenerTodosLosGrupos() {
    List<String> grupos = new ArrayList<>();
    String sql = "SELECT nombre, creador FROM grupos ORDER BY nombre";
    
    try (Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {
        
        while (rs.next()) {
            String nombre = rs.getString("nombre");
            String creador = rs.getString("creador");
            grupos.add(nombre + " (creado por: " + creador + ")");
        }
    } catch (SQLException e) {
        System.err.println("Error al obtener grupos: " + e.getMessage());
    }
    
    return grupos;
}


public static List<String> obtenerGruposDeUsuario(String usuario) {
    List<String> grupos = new ArrayList<>();
    String sql = "SELECT grupo_nombre FROM miembros_grupo WHERE usuario = ? ORDER BY grupo_nombre";
    
    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, usuario);
        ResultSet rs = pstmt.executeQuery();
        
        while (rs.next()) {
            grupos.add(rs.getString("grupo_nombre"));
        }
    } catch (SQLException e) {
        System.err.println("Error al obtener grupos del usuario: " + e.getMessage());
    }
    
    return grupos;
}


public static void guardarMensajeGrupo(String nombreGrupo, String remitente, String mensaje) {
    String sql = "INSERT INTO mensajes_grupo (grupo_nombre, remitente, mensaje) VALUES (?, ?, ?)";
    
    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, nombreGrupo);
        pstmt.setString(2, remitente);
        pstmt.setString(3, mensaje);
        pstmt.executeUpdate();
    } catch (SQLException e) {
        System.err.println("Error al guardar mensaje: " + e.getMessage());
    }
}


public static List<String> obtenerMensajesNoLeidos(String nombreGrupo, String usuario) {
    List<String> mensajes = new ArrayList<>();
    String sql = "SELECT m.id, m.remitente, m.mensaje, m.fecha_envio " +
                 "FROM mensajes_grupo m " +
                 "WHERE m.grupo_nombre = ? " +
                 "AND m.id NOT IN (SELECT mensaje_id FROM mensajes_leidos WHERE usuario = ?) " +
                 "ORDER BY m.fecha_envio ASC";
    
    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, nombreGrupo);
        pstmt.setString(2, usuario);
        ResultSet rs = pstmt.executeQuery();
        
        while (rs.next()) {
            int mensajeId = rs.getInt("id");
            String remitente = rs.getString("remitente");
            String mensaje = rs.getString("mensaje");
            String fecha = rs.getString("fecha_envio");
            
            mensajes.add("[" + fecha.substring(0, 16) + "] " + remitente + ": " + mensaje);
            
          
            marcarMensajeComoLeido(usuario, mensajeId);
        }
    } catch (SQLException e) {
        System.err.println("Error al obtener mensajes: " + e.getMessage());
    }
    
    return mensajes;
}


private static void marcarMensajeComoLeido(String usuario, int mensajeId) {
    String sql = "INSERT OR IGNORE INTO mensajes_leidos (usuario, mensaje_id) VALUES (?, ?)";
    
    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, usuario);
        pstmt.setInt(2, mensajeId);
        pstmt.executeUpdate();
    } catch (SQLException e) {
        System.err.println("Error al marcar mensaje como leído: " + e.getMessage());
    }
}
    public static void cerrarConexion() {
        if (conn != null) {
            try {
                conn.close();
                System.out.println("Conexión a la base de datos cerrada.");
            } catch (SQLException e) {
                System.err.println("Error al cerrar la conexión: " + e.getMessage());
            }
        }
    }
}