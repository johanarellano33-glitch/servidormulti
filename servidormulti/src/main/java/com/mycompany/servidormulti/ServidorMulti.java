package com.mycompany.servidormulti;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServidorMulti {
    
    private static final int PUERTO = 8080;
    private static final int MAX_HILOS = 100;
    
    static ConcurrentHashMap<String, UnCliente> clientes = new ConcurrentHashMap<>();
    static ConcurrentHashMap<String, gato> juegosActivos = new ConcurrentHashMap<>();
    private static ExecutorService poolHilos;
    
    public static boolean verificarCredenciales(String nombre, String password) {
        return DatabaseManager.verificarCredenciales(nombre, password);
    }
    
    public static boolean registrarUsuario(String nombre, String password) {
        return DatabaseManager.registrarUsuario(nombre, password);
    }
    
    public static String generarClaveJuego(String jugador1, String jugador2) {
        return jugador1.compareTo(jugador2) < 0 ? 
               jugador1 + ":" + jugador2 : 
               jugador2 + ":" + jugador1;
    }
    
    private static void inicializarServidor() {
        DatabaseManager.inicializar();
        poolHilos = Executors.newFixedThreadPool(MAX_HILOS);
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            cerrarServidor();
        }));
    }
    
    private static void cerrarServidor() {
        System.out.println("Cerrando servidor...");
        poolHilos.shutdown();
        DatabaseManager.cerrarConexion();
        System.out.println("Servidor cerrado correctamente.");
    }
    
    public static void main(String[] args) {
        inicializarServidor();
        
        int contadorClientes = 0;
        
        try (ServerSocket servidorSocket = new ServerSocket(PUERTO)) {
            System.out.println("Servidor iniciado en el puerto " + PUERTO);
            
            while (true) {
                Socket socket = servidorSocket.accept();
                String idCliente = String.valueOf(contadorClientes);
                
                UnCliente cliente = new UnCliente(socket, idCliente);
                clientes.put(idCliente, cliente);
                
                poolHilos.execute(cliente);
                
                System.out.println("Cliente #" + contadorClientes + " conectado (ID: " + idCliente + ")");
                contadorClientes++;
            }
        } catch (IOException e) {
            System.err.println("Error en ServidorMulti: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cerrarServidor();
        }
    }
}