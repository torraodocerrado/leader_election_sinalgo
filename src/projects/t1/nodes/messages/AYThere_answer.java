package projects.t1.nodes.messages;

import sinalgo.nodes.Node;
import sinalgo.nodes.messages.Message;

public class AYThere_answer extends Message {

	public Node sender;
	public boolean answer;
	public int coordenatorCount;

	@Override
	public Message clone() {
		return this;
	}

	public AYThere_answer(Node sender, boolean answer, int coordenatorCount) {
		this.sender = sender;
		this.answer = answer;
		this.coordenatorCount = coordenatorCount;
	}

}
