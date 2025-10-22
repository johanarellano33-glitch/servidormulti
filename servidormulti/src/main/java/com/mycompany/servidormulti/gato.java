
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
}
