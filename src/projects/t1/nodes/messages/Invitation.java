package projects.t1.nodes.messages;

import sinalgo.nodes.Node;
import sinalgo.nodes.messages.Message;

public class Invitation extends Message {
	public Node coord;
	public int coordenatorCount;

	@Override
	public Message clone() {
		return this;
	}

	public Invitation(Node coord, int coordenatorCount) {
		this.coord = coord;
		this.coordenatorCount = coordenatorCount;
	}

}