package comp18;

public class ProcessState {
	long last_interaction;
	int step;
	int cod;
	int label;
	String pos;
	public ProcessState(int cod,long start_time, String posicao){
		this.cod = cod;
		last_interaction = start_time;
		step=0;
		pos = posicao;
	}
	
	public boolean timedOut(){
		if ((System.currentTimeMillis()-last_interaction)>15000){
			return true;
		}
		return false;
	}
	public boolean isAtStage(int i){
		return i==step;
	}
	public void goAhead(){
		last_interaction=System.currentTimeMillis();
		step++;
	}

	public String getPos() {
		// TODO Auto-generated method stub
		return pos;
	}
}
