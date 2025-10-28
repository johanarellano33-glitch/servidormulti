package com.mycompany.servidormulti;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class ServidorMulti {
    
    static ConcurrentHashMap<String, UnCliente> clientes = new ConcurrentHashMap<>();
    // Almacena los juegos activos: clave = "jugador1:jugador2" (ordenado alfabéticamente)
    static ConcurrentHashMap<String, gato> juegosActivos = new ConcurrentHashMap<>();
    
    /**
     * Verifica si las credenciales coinciden (ahora usando DatabaseManager)
     */
    public static boolean verificarCredenciales(String nombre, String password) {
        return DatabaseManager.verificarCredenciales(nombre, password);
    }
    
    /**
     * Registra un nuevo usuario (ahora usando DatabaseManager)
     */
    public static boolean registrarUsuario(String nombre, String password) {
        return DatabaseManager.registrarUsuario(nombre, password);
    }
    
    /**
     * Genera una clave única para identificar un juego entre dos jugadores
     */
    public static String generarClaveJuego(String jugador1, String jugador2) {
        // Ordenar alfabéticamente para evitar duplicados
        if (jugador1.compareTo(jugador2) < 0) {
            return jugador1 + ":" + jugador2;
        } else {
            return jugador2 + ":" + jugador1;
        }
    }
    
    public static void main(String[] args) {
        // Inicializar la base de datos
        DatabaseManager.inicializar();
        
        // Agregar shutdown hook para cerrar la conexión limpiamente
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            DatabaseManager.cerrarConexion();
        }));
        
        int puerto = 8080;
        int contador = 0;
        
        try (ServerSocket servidorSocket = new ServerSocket(puerto)) {
            System.out.println("Servidor iniciado en el puerto " + puerto);
            
            while (true) {
                Socket socket = servidorSocket.accept();
                String idCliente = Integer.toString(contador);
                UnCliente uncliente = new UnCliente(socket, idCliente);
                Thread hilo = new Thread(uncliente);
                
                clientes.put(idCliente, uncliente);
                hilo.start();
                System.out.println("Se conectó el cliente #" + contador + " (ID: " + idCliente + ")");
                contador++;
            }
        } catch (IOException e) {
            System.err.println("Error en ServidorMulti: " + e.getMessage());
        }
    }
}