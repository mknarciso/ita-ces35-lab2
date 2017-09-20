package comp18;

import java.util.Date;

public class Processo {
	String _status;
	long _ultimaInteracao; 
	
	public Processo (String status, long criacao){
		_status = status;
		_ultimaInteracao = criacao;
	}
	
}
