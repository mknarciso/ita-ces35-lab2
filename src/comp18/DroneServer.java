package comp18;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

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
    private static HashSet<PrintWriter> writers = new HashSet<PrintWriter>();

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
        private String name;
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private HashMap<String, Processo> processos;

        /**
         * Constructs a handler thread, squirreling away the socket.
         * All the interesting work is done in the run method.
         */
        public Handler(Socket socket) {
            this.socket = socket;
        }
        
        public boolean timeout(Processo processo){
        	//DEFINIR TAMANHO DO TIMEOUT
        	long timeout = 1000;
        	if(System.currentTimeMillis() - processo._ultimaInteracao > timeout)
        		return true;
        	return false;
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

                // Request a name from this client.  Keep requesting until
                // a name is submitted that is not already used.  Note that
                // checking for the existence of a name and adding the name
                // must be done while locking the set of names.
                while (true) {
                    String input = in.readLine();
                    //MENSAGEM DO TIPO -> AB:001:DEC:123:124:1001
                    System.out.println(input);
                    String estacao = input.substring(0,2);
                    String plot = input.substring(3,6);
                    String codigoMensagem = input.substring(7, 10);
                    String dados = input.substring(11);
                    int posicao_X, posicao_Y, id;
                    switch(codigoMensagem){
                    case "DEC":
                    	//Procura se o drone j� est� no banco de dados comparando
                    	//Seu posicionamento
                    	boolean droneNoBD = false;
                    	posicao_X = Integer.parseInt(dados.substring(0,3));
                    	posicao_Y = Integer.parseInt(dados.substring(4,7));
                    	synchronized(bancoDeDados){
                        	for(int i = 0; i < bancoDeDados.size(); i++){
                        		if(bancoDeDados.get(i)._pos_x == posicao_X && bancoDeDados.get(i)._pos_y == posicao_Y)
                        			droneNoBD = true;
                        	}
                    	}
  
                    	if(droneNoBD){
                    		out.println(estacao + ":" + plot + ":" + "IGN");
                            writers.add(out);
                    	}
                    	else{
                    		processos.put(plot, new Processo("INF", System.currentTimeMillis()));
                    		out.println(estacao + ":" + plot + ":" + "INF");
                    		writers.add(out);
                    	}
                    	break;
                    
                    case "SIN":
                    	posicao_X = Integer.parseInt(dados.substring(0,3));
                    	posicao_Y = Integer.parseInt(dados.substring(4,7));
                    	id = Integer.parseInt(dados.substring(8,12));
                    	if(!timeout(processos.get(plot))){
                    		processos.get(plot)._status = "SIN";
                        	processos.get(plot)._ultimaInteracao = System.currentTimeMillis();
                        	synchronized(bancoDeDados){
                        		bancoDeDados.add(new Drone(id, posicao_X, posicao_Y));
                        	}
                        	if(id % 2 == 0){ //Se c�digo for par, ent�o est� autorizado
                        		out.println(estacao + ":" + plot + ":RES:WELCOME");
                        		writers.add(out);
                        		processos.get(plot)._status = "OIE";
                        	}
                        	else{
                        		out.println(estacao + ":" + plot + ":RES:DESTROY");
                        		writers.add(out);
                        		processos.get(plot)._status = "DES";
                        	}
                    		processos.get(plot)._ultimaInteracao = System.currentTimeMillis();
                    	}
                    	else{
                    		processos.remove(plot);
                    	}
                    	
                    	break;
                    	
                    case "ACK":
                    	//Considerando que s� quem manda ACK � o DESTROY, retirar o drone do BD
                    	//Nos dados, s� vem o id do drone
                    	id = Integer.parseInt(dados);
                    	if(!timeout(processos.get(plot))){
                    		boolean achouDrone = false;
                        	int indice = -1;
                        	synchronized(bancoDeDados){
                        		for(int i = 0; i < bancoDeDados.size() && !achouDrone; i++){
                            		if(bancoDeDados.get(i)._id == id){
                            			achouDrone = true;
                            			indice = i;
                            		}
                            	}
                        		if(achouDrone)
                        			bancoDeDados.remove(indice);	
                        	}
                    	}
                    	processos.remove(plot); 	
                    	break;
                    }                          
                }
  

                // Accept messages from this client and broadcast them.
                // Ignore other clients that cannot be broadcasted to.
                /*while (true) {
                    String input = in.readLine();
                    if (input == null) {
                        return;
                    }
                    for (PrintWriter writer : writers) {
                        writer.println("MESSAGE " + name + ": " + input);
                    }
                }*/
            } catch (IOException e) {
                System.out.println(e);
            } finally {
                // This client is going down!  Remove its name and its print
                // writer from the sets, and close its socket.
                /*if (name != null) {
                    names.remove(name);
                }*/
                if (out != null) {
                    writers.remove(out);
                }
                try {
                    socket.close();
                } catch (IOException e) {
                }
            }
        }
    }
}