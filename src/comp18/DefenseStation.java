package comp18;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class DefenseStation {

    BufferedReader in;
    PrintWriter out;
    ArrayList <ProcessState> states;
    int i = 0;
    JFrame frame = new JFrame("DefenseStation");
    JTextField textField = new JTextField(40);
    JTextArea messageArea = new JTextArea(8, 40);
    String my_cod;

    public DefenseStation() {
    	states = new ArrayList<ProcessState>();
        // Layout GUI
        textField.setEditable(false);
        messageArea.setEditable(false);
        frame.getContentPane().add(textField, "North");
        frame.getContentPane().add(new JScrollPane(messageArea), "Center");
        frame.pack();
        my_cod = getName();
        // Add Listeners
        textField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	String resp = my_cod+":"+String.format("%03d", i)+":DEC:"+textField.getText();
                out.println(resp);
                textField.setText("");
                messageArea.append("=>"+resp + "\n");
                states.add(new ProcessState(i,System.currentTimeMillis()));
            	i++;
            }
        });
    }

    /**
     * Prompt for and return the address of the server.
     */
    private String getServerAddress() {
        return JOptionPane.showInputDialog(
            frame,
            "Endereço da Centro de Controle (IP):",
            "DroneDefense",
            JOptionPane.QUESTION_MESSAGE);
    }

    /**
     * Prompt for and return the desired screen name.
     */
    private String getName() {
        return JOptionPane.showInputDialog(
            frame,
            "Selecione um Id de 2 char para esta estação remota:",
            "Remote DefenseSystem",
            JOptionPane.PLAIN_MESSAGE);
    }
    private String getInfo(int process){
        return JOptionPane.showInputDialog(
                frame,
                "Qual o código XPDR da aeronave #"+process+" ?",
                "Remote DefenseSystem",
                JOptionPane.PLAIN_MESSAGE);
    }
    private String getResult(int process,String action){
        return JOptionPane.showInputDialog(
                frame,
                "Ação a ser executada para o plote #"+process+":\n"+action+"\nQual o Resultado?",
                "Remote DefenseSystem",
                JOptionPane.PLAIN_MESSAGE);
    }
    /**
     * Connects to the server then enters the processing loop.
     */
    private void run() throws IOException {
    	
        // Make connection and initialize streams
        String serverAddress = "127.0.0.1";//getServerAddress();
        Socket socket = new Socket(serverAddress, 9001);
        in = new BufferedReader(new InputStreamReader(
            socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
        // Process all messages from server, according to the protocol.
        while (true) {
            textField.setEditable(true);
            String line = in.readLine();
            messageArea.append("<="+line + "\n");
            String dest = line.substring(0,2);
            int proc_num = Integer.parseInt(line.substring(3,6));
            String stage = line.substring(7,10);
            if (proc_num<states.size()){
            	ProcessState s = states.get(proc_num);
            	if(s.timedOut()){
                	String error = "Process#"+proc_num+" timed out.\n";
                	messageArea.append(error);
                	out.print(error);
            	} else {
                    if (stage.equals("INF")) {
                    	if(s.isAtStage(0)){
                    		out.println(my_cod+":"+String.format("%03d", i)+":SIN:"+ getInfo(proc_num));
                    		s.goAhead();
                    	} else {
                        	String error = "Message out of sequence.\n";
                        	messageArea.append(error);
                        	out.print(error);
                    	}
                    }
                    if (stage.equals("RES")) {
                    	if(s.isAtStage(1)){
                    		out.println(my_cod+":"+String.format("%03d", i)+":RES:"+ getResult(proc_num,line.substring(11,14)));
                    		s.goAhead();
                    	} else {
                        	String error = "Message out of sequence.\n";
                        	messageArea.append(error);
                        	out.print(error);
                    	}
                    }   
            	}
            } else {
            	String error = "There is no process#"+proc_num+"\n";
            	messageArea.append(error);
            	out.print(error);
            }
        }
    }
    /**
     * Runs the client as an application with a closeable frame.
     */
    public static void main(String[] args) throws Exception {
    	//System.out.println(System.currentTimeMillis());
        DefenseStation client = new DefenseStation();
        client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        client.frame.setVisible(true);
        client.run();
    }
}