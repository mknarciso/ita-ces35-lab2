package comp18;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * A multithreaded chat room server.  When a client connects the
 * server requests a screen name by sending the client the
 * text "SUBMITNAME", and keeps requesting a name until
 * a unique one is received.  After a client submits a unique
 * name, the server acknowledges with "NAMEACCEPTED".  Then
 * all messages from that client will be broadcast to all other
 * clients that have submitted a unique screen name.  The
 * broadcast messages are prefixed with "MESSAGE ".
 *
 * Because this is just a teaching example to illustrate a simple
 * chat server, there are a few features that have been left out.
 * Two are very useful and belong in production code:
 *
 *     1. The protocol should be enhanced so that the client can
 *        send clean disconnect messages to the server.
 *
 *     2. The server should do some logging.
 */
public class DroneServer {

    /**
     * The port that the server listens on.
     */
    private static final int PORT = 9001;

    /**
     * The set of all names of clients in the chat room.  Maintained
     * so that we can check that new clients are not registering name
     * already in use.
     */    
    private static ArrayList<Drone> bancoDeDados = new ArrayList<Drone>();

    /**
     * The set of all the print writers for all the clients.  This
     * set is kept so we can easily broadcast messages.
     */
    
    /**
     * The appplication main method, which just listens on a port and
     * spawns handler threads.
     */
    public static void main(String[] args) throws Exception {
        System.out.println("O servidor esta rodando.");
        ServerSocket listener = new ServerSocket(PORT);
        try {
            while (true) {
                new Handler(listener.accept()).start();
            }
        } finally {
            listener.close();
        }
    }

    /**
     * A handler thread class.  Handlers are spawned from the listening
     * loop and are responsible for a dealing with a single client
     * and broadcasting its messages.
     */
    private static class Handler extends Thread {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private HashMap<String, ProcessState> processos;

        /**
         * Constructs a handler thread, squirreling away the socket.
         * All the interesting work is done in the run method.
         */
        public Handler(Socket socket) {
            this.socket = socket;
            processos = new HashMap<String, ProcessState>();
        }

        /**
         * Services this thread's client by repeatedly requesting a
         * screen name until a unique one has been submitted, then
         * acknowledges the name and registers the output stream for
         * the client in a global set, then repeatedly gets inputs and
         * broadcasts them.
         */
        public void run() {
            try {

                // Create character streams for the socket.
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                int k = 1;
                ProcessState state;
                
                // Request a name from this client.  Keep requesting until
                // a name is submitted that is not already used.  Note that
                // checking for the existence of a name and adding the name
                // must be done while locking the set of names.
                while (true) {
                    String input = in.readLine();
                    //MENSAGEM DO TIPO -> AB:001:DEC:123:124:1001
                    System.out.println(input);
                    String plot = input.substring(0,3);
                    String codigoMensagem = input.substring(4, 7);
                    String dados = input.substring(8);
                    int posicao_X, posicao_Y, id;
                	String[] posicoes;

                    switch(codigoMensagem){
                    case "DEC":
                    	//Procura se o drone já está no banco de dados comparando
                    	//Seu posicionamento
                    	boolean droneNoBD = false;
                    	posicoes = dados.split(":");
                    	posicao_X = Integer.parseInt(posicoes[0]);
                    	posicao_Y = Integer.parseInt(posicoes[1]);
                    	synchronized(bancoDeDados){
                        	for(int i = 0; i < bancoDeDados.size(); i++){
                        		if(bancoDeDados.get(i)._pos_x == posicao_X && bancoDeDados.get(i)._pos_y == posicao_Y)
                        			droneNoBD = true;
                        	}
                    	}
  
                    	if(droneNoBD){
                    		out.println(plot + ":" + "IGN");                    	}
                    	else{
                    		state = new ProcessState(k++, System.currentTimeMillis());
                    		processos.put(plot, state);
                    		out.println(plot + ":" + "INF");
                    		state.goAhead();
                    	}
                    	break;
                    
                    case "SIN":
                    	posicoes = dados.split(":");
                    	posicao_X = Integer.parseInt(posicoes[0]);
                    	posicao_Y = Integer.parseInt(posicoes[1]);
                    	id = Integer.parseInt(posicoes[2]);
                        state = processos.get(plot);
                    	if(!state.timedOut()){
                    		if(state.isAtStage(1)){
                            	synchronized(bancoDeDados){
                            		bancoDeDados.add(new Drone(id, posicao_X, posicao_Y));
                            		state.label = id;
                            	}
                            	if(id % 2 == 0){ //Se código for par, então está autorizado
                            		out.println(plot + ":ACT:WELCOME");
                            	}
                            	else{
                            		out.println(plot + ":ACT:DESTROY");
                            	}
                    			state.goAhead();
                        	}
                    		else{
                    			String error = "Message out of sequence";
                            	out.print(error);
                    		}
                    	}
                    	else{
                    		String error = "Process#"+state.cod+" timed out.\n";
                        	out.print(error);
                    	}
                    	
                    	break;
                    	
                    case "DONE":
                        System.out.println("Transação terminada");

                    	
                    case "RES":
                    	//Considerando que só quem manda ACK é o DESTROY, retirar o drone do BD
                    	//Nos dados, só vem o id do drone
                    	posicoes = dados.split(":");
                        state = processos.get(plot);
                        String acao = posicoes[0];
                        String resultado = posicoes[1];
                    	if(!state.timedOut()){
                    		if(state.isAtStage(2)){
                    			if(acao == "DESTROY" && resultado == "DONE"){
                    				boolean achouDrone = false;
                                	int indice = -1;
                                	synchronized(bancoDeDados){
                                		for(int i = 0; i < bancoDeDados.size() && !achouDrone; i++){
                                    		if(bancoDeDados.get(i)._id == state.label){
                                    			achouDrone = true;
                                    			indice = i;
                                    		}
                                    	}
                                		if(achouDrone)
                                			bancoDeDados.remove(indice);	
                                	}
                            	}       				
                            	out.println(plot + ":ACK");
                            	state.goAhead();
                    		}
                    		else{
                    			String error = "Message out of sequence";
                            	out.print(error);
                    		}
                    	}
                    	else{
                    		String error = "Process#"+state.cod+" timed out.\n";
                        	out.print(error);
                    	}
                    	break;
                    	
                    default:
                        System.out.println("Comando não identificado");
                    }                          
                }
            } catch (IOException e) {
                System.out.println(e);
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                }
            }
        }
    }
}