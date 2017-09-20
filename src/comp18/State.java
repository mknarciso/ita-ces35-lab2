package comp18;

public class State {
	long last_interaction;
	int step;
	int cod;
	public State(int cod,long start_time){
		this.cod = cod;
		last_interaction = start_time;
		step=0;
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
}
