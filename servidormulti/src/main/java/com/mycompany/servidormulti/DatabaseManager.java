package com.mycompany.servidormulti;

import java.sql.*;
import java.util.HashSet;
import java.util.Set;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:chat.db";
    private static Connection conn = null;

    /**
     * Inicializa la conexión y crea las tablas necesarias
     */
    public static void inicializar() {
        try {
            conn = DriverManager.getConnection(DB_URL);
            crearTablas();
            System.out.println("Base de datos inicializada correctamente.");
        } catch (SQLException e) {
            System.err.println("Error al inicializar la base de datos: " + e.getMessage());
        }
    }

    /**
     * Crea las tablas usuarios y bloqueos si no existen
     */
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

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sqlUsuarios);
            stmt.execute(sqlBloqueos);
        }
    }

    /**
     * Registra un nuevo usuario en la base de datos
     */
    public static boolean registrarUsuario(String nombre, String password) {
        String sql = "INSERT INTO usuarios (nombre, password) VALUES (?, ?)";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nombre);
            pstmt.setString(2, password);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            // Si el error es por duplicado (código 19 en SQLite)
            if (e.getErrorCode() == 19) {
                return false; // Usuario ya existe
            }
            System.err.println("Error al registrar usuario: " + e.getMessage());
            return false;
        }
    }

    /**
     * Verifica las credenciales de un usuario
     */
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

    /**
     * Verifica si un usuario existe en la base de datos
     */
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

    /**
     * Bloquea un usuario (usuarioBloqueador bloquea a usuarioBloqueado)
     */
    public static boolean bloquearUsuario(String usuarioBloqueador, String usuarioBloqueado) {
        // No permitir que un usuario se bloquee a sí mismo
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
            // Si el error es por duplicado (ya estaba bloqueado)
            if (e.getErrorCode() == 19) {
                return false;
            }
            System.err.println("Error al bloquear usuario: " + e.getMessage());
            return false;
        }
    }

    /**
     * Desbloquea un usuario
     */
    public static boolean desbloquearUsuario(String usuarioBloqueador, String usuarioBloqueado) {
        String sql = "DELETE FROM bloqueos WHERE usuario_bloqueador = ? AND usuario_bloqueado = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, usuarioBloqueador);
            pstmt.setString(2, usuarioBloqueado);
            int filasAfectadas = pstmt.executeUpdate();
            return filasAfectadas > 0; // Retorna true si se eliminó alguna fila
        } catch (SQLException e) {
            System.err.println("Error al desbloquear usuario: " + e.getMessage());
            return false;
        }
    }

    /**
     * Verifica si un usuario tiene bloqueado a otro
     */
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

    /**
     * Obtiene la lista de usuarios bloqueados por un usuario
     */
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

    /**
     * Cierra la conexión a la base de datos
     */
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