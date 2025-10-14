package com.mycompany.servidormulti;

import java.io.IOException;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class ServidorMulti {
    
    // Almacena a los clientes conectados (key: ID temporal o Nombre de usuario)
    static ConcurrentHashMap<String, UnCliente> clientes = new ConcurrentHashMap<>();
    private static final String ARCHIVO_USUARIOS = "usuarios.txt";
    
    /**
     * Verifica si las credenciales coinciden con algún registro en el archivo.
     */
    public static boolean verificarCredenciales(String nombre, String password) {
        try (BufferedReader br = new BufferedReader(new FileReader(ARCHIVO_USUARIOS))) {
            String linea;
            String credencial = nombre + ":" + password; 
            while ((linea = br.readLine()) != null) {
                if (linea.equals(credencial)) {
                    return true;
                }
            }
        } catch (IOException e) {
            // Se ignora si el archivo no existe o hay error de lectura
        }
        return false;
    }
    
    /**
     * Intenta registrar un nuevo usuario si no existe.
     */
    public static boolean registrarUsuario(String nombre, String password) {
        // 1. Verificar si el nombre ya existe
        try (BufferedReader br = new BufferedReader(new FileReader(ARCHIVO_USUARIOS))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                if (linea.startsWith(nombre + ":")) {
                    return false; // El usuario ya existe
                }
            }
        } catch (IOException e) {
            // Ignorar
        }
        
        // 2. Si no existe, agregarlo al archivo
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(ARCHIVO_USUARIOS, true))) {
            String nuevaCredencial = nombre + ":" + password;
            bw.write(nuevaCredencial);
            bw.newLine();
            return true;
        } catch (IOException e) {
            System.err.println("Error al escribir en el archivo de usuarios: " + e.getMessage());
            return false;
        }
    }
    
    public static void main(String[] args) {
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
                System.out.println("Se conectó el chango #" + contador + " (ID: " + idCliente + ")");
                contador++;
            }
        } catch (IOException e) {
            System.err.println("Error en ServidorMulti: " + e.getMessage());
        }
    }
}