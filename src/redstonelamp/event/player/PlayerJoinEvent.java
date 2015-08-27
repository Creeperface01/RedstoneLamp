package redstonelamp.event.player;

import redstonelamp.Player;
import redstonelamp.event.Event;
import redstonelamp.event.Listener;

public class PlayerJoinEvent extends Event {
	private Player player;
	private String message;
	private Event e = this;
	
	public PlayerJoinEvent(Player player. String message) {
		this.player = player;
	}
	
	public void execute(Listener listener) {
		listener.onEvent(e);
	}
	
	public Player getPlayer() {
		return this.player;
	}
	
	public String getJoinMessage(){
		return this.message;
	}
	
	public void setJoinMessage(String message){
		this.message = message;
	}
}
