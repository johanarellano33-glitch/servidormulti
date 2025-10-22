
package com.mycompany.servidormulti;

public class gato {
    private char[][] tablero;
    private String jugador1;
    private String jugador2;
    private String turnoActual;
    private char simboloJugador1;
    private char simboloJugador2; 
    private boolean juegoTerminado;
    private String ganador;
    
   
   public gato(String jugador1, String jugador2, boolean jugador1Empieza) {
        this.tablero = new char[3][3];
        this.jugador1 = jugador1;
        this.jugador2 = jugador2;
        this.juegoTerminado = false;
        this.ganador = null;
        
      
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                tablero[i][j] = '-';
            }
        }
        
       
        if (jugador1Empieza) {
            this.simboloJugador1 = 'X';
            this.simboloJugador2 = 'O';
            this.turnoActual = jugador1;
        } else {
            this.simboloJugador1 = 'O';
            this.simboloJugador2 = 'X';
            this.turnoActual = jugador2;
        }
    }
   public String getJugador1() {
        return jugador1;
    }
    
    public String getJugador2() {
        return jugador2;
    }
    
    public String getTurnoActual() {
        return turnoActual;
    }
    
    public boolean isJuegoTerminado() {
        return juegoTerminado;
    }
    
    public String getGanador() {
        return ganador;
    }
    
    public char getSimbolo(String jugador) {
        if (jugador.equals(jugador1)) {
            return simboloJugador1;
        } else {
            return simboloJugador2;
        }
    }
  
    public boolean realizarMovimiento(String jugador, int fila, int columna) {
        if (!jugador.equals(turnoActual)) {
            return false;
        }
        
     
        if (fila < 0 || fila > 2 || columna < 0 || columna > 2) {
            return false;
        }
     
        if (tablero[fila][columna] != '-') {
            return false;
        }
        
       
        char simbolo = jugador.equals(jugador1) ? simboloJugador1 : simboloJugador2;
        tablero[fila][columna] = simbolo;
        
      
        verificarEstadoJuego();
        
        
        if (!juegoTerminado) {
            turnoActual = turnoActual.equals(jugador1) ? jugador2 : jugador1;
        }
        
        return true;
   
    }
    private void verificarEstadoJuego() {
        // Verificar filas
        for (int i = 0; i < 3; i++) {
            if (tablero[i][0] != '-' && 
                tablero[i][0] == tablero[i][1] && 
                tablero[i][1] == tablero[i][2]) {
                juegoTerminado = true;
                ganador = tablero[i][0] == simboloJugador1 ? jugador1 : jugador2;
                return;
            }
        }
        for (int j = 0; j < 3; j++) {
            if (tablero[0][j] != '-' && 
                tablero[0][j] == tablero[1][j] && 
                tablero[1][j] == tablero[2][j]) {
                juegoTerminado = true;
                ganador = tablero[0][j] == simboloJugador1 ? jugador1 : jugador2;
                return;
            }
        }
        if (tablero[0][0] != '-' && 
            tablero[0][0] == tablero[1][1] && 
            tablero[1][1] == tablero[2][2]) {
            juegoTerminado = true;
            ganador = tablero[0][0] == simboloJugador1 ? jugador1 : jugador2;
            return;
        }
    
    