package redstonelamp.event.player;

import redstonelamp.Player;
import redstonelamp.event.Event;
import redstonelamp.event.Listener;

public class PlayerQuitEvent extends Event {
	private Player player;
	private String message;
	private Event e = this;
	
	public PlayerQuitEvent(Player player, String message) {
		this.player = player;
		this.message = message;
	}
	
	public void execute(Listener listener) {
		listener.onEvent(e);
	}
	
	public Player getPlayer() {
		return this.player;
	}
	
	public String getQuitMessage(){
		return this.message;
	}
	
	public void setQuitMessage(String message){
		this.message = message;
	}
}
