package projects.t1.nodes.messages;

import sinalgo.nodes.Node;
import sinalgo.nodes.messages.Message;

public class Ready extends Message {
	public Node sender;
	public int coordenatorCount;

	@Override
	public Message clone() {
		return this;
	}

	public Ready(Node sender, int coordenatorCount) {
		this.sender = sender;
		this.coordenatorCount = coordenatorCount;
	}

}