package projects.t2.nodes.messages;

import sinalgo.nodes.Node;

public class Propose extends T2Message {
	public Node coord;

	public Propose(Node sender, Node coord) {
		super(sender);
		this.coord = coord;
	}
}