package projects.t1.nodes.nodeImplementations;

import java.awt.Color;
import java.awt.Graphics;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

import projects.t1.LogFile;
import projects.t1.nodes.messages.AYC_answer;
import projects.t1.nodes.messages.AYCoord;
import projects.t1.nodes.messages.AYThere;
import projects.t1.nodes.messages.AYThere_answer;
import projects.t1.nodes.messages.Accept;
import projects.t1.nodes.messages.Accept_answer;
import projects.t1.nodes.messages.Invitation;
import sinalgo.configuration.WrongConfigurationException;
import sinalgo.gui.transformation.PositionTransformation;
import sinalgo.nodes.Node;
import sinalgo.nodes.messages.Inbox;
import sinalgo.nodes.messages.Message;
import sinalgo.runtime.Global;
import sinalgo.tools.Tools;

public class LeaderNode extends Node {

	// conjunto dos membros do proprio grupo
	public ArrayList<Node> upSet;

	public static LogFile fileLog;
	// conjunto dos membros da uniao dos grupos
	public ArrayList<Node> up;
	// identificao do grupo (par [CoordID,count])
	public Node coordenatorGroup;
	public Node oldCoordenatorGroup;
	public int coordenatorCount = 0;
	// conjunto de outros coordenadores descobertos
	public ArrayList<Node> others;
	// Momento da ultima mensagem
	private double timeAYCoord;
	private double timerToMerge = 0;
	private double timeStartAYThere = 0;
	// Tipo do estado: 0 - 'Normal' | 1 - 'Election' | 2 'Reorganizing'
	private int state = 0;
	// contadores de mensagens
	private int waitingAnswerAYCoord = 0;
	// AnswerInvitation
	private double timeOutAnswerInvitation = 0;
	private int waitingAnswerInvitation = 0;
	private double timeMerge = 0;

	// coin
	private Random randomGenerator;
	private int coinChancePositive = 70;
	private boolean log_on = true;
	private double timeOut = 50;
	private double waitConst = 50;

	@Override
	public void handleMessages(Inbox inbox) {
		while (inbox.hasNext()) {
			Message message = inbox.next();
			if (message instanceof AYCoord) {
				this.answerAYCoord((AYCoord) message);
			}
			if (message instanceof AYC_answer) {
				this.processAYC_answer((AYC_answer) message);
			}
			if (message instanceof Invitation) {
				this.answerInvitation((Invitation) message);
			}
			if (message instanceof Accept) {
				this.answerAccept((Accept) message);
			}
			if (message instanceof Accept_answer) {
				this.processAccept_answer((Accept_answer) message);
			}
			if (message instanceof AYThere) {
				this.answerAYThere((AYThere) message);
			}
			if (message instanceof AYThere_answer) {
				this.processAYThere_answer((AYThere_answer) message);
			}
		}
	}

	@Override
	public void preStep() {
		this.checkMembers();
		this.checkCoord();
		this.checkTimerToMerge();
		this.timeOutAYCoord();
		this.timeOutAYThere();
		this.timeOutMerge();
		this.timeOutAnswerInvitation();
	}

	@Override
	public void init() {
		this.up = new ArrayList<Node>();
		this.upSet = new ArrayList<Node>();
		this.others = new ArrayList<Node>();
		this.coordenatorGroup = this;
		this.setColor(Color.RED);
		this.randomGenerator = new Random();
		if (fileLog == null) {
			fileLog = new LogFile(this.getNameFile());
		}
	}

	@Override
	public void draw(Graphics g, PositionTransformation pt, boolean highlight) {
		this.drawAsDisk(g, pt, highlight, this.drawingSizeInPixels);
		this.drawNodeAsDiskWithText(g, pt, highlight, String.valueOf(this.ID), 20, Color.BLACK);
		if (this.IamCoordenator()) {
			this.setColor(Color.blue);
		} else {
			this.setColor(Color.RED);
		}
	}

	@Override
	public void neighborhoodChange() {
	}

	@Override
	public void postStep() {
		if (this.IamCoordenator()) {
			// if (Global.currentTime % 10 == 0) {
			if (this.up.size() == (Tools.getNodeList().size() - 1)) {
				fileLog.addStep(Global.currentTime, ((int) (Math.round(Global.currentTime))) + ";" + 1);
			} else {
				fileLog.addStep(Global.currentTime, ((int) (Math.round(Global.currentTime))) + ";" + 0);
			}
			// }
		}
	}

	@Override
	public void checkRequirements() throws WrongConfigurationException {
	}

	private boolean IamCoordenator() {
		if (this.coordenatorGroup == null) {
			this.coordenatorGroup = this;
			return true;
		}
		return this.ID == this.coordenatorGroup.ID;
	}

	/*-------------------------------------------------------------------------------------------------*/

	private void checkTimerToMerge() {
		if ((this.state == 0) && (this.timerToMerge == Global.currentTime)) {
			this.merge();
		}
	}

	private void merge() {
		log("Start merge " + this.ID);
		if ((this.IamCoordenator()) && (this.state == 0)) {
			this.state = 1;
			this.coordenatorCount++;
			this.timerToMerge = 0;
			this.upSet = this.up;
			this.up = new ArrayList<Node>();
			this.waitingAnswerInvitation = 0;
			Invitation message = new Invitation(this, this.coordenatorCount);
			for (Node no : this.others) {
				this.waitingAnswerInvitation++;
				log("Invitation from from " + no.ID);
				this.send(message, no);
			}
			for (Node no : this.upSet) {
				this.waitingAnswerInvitation++;
				log("Invitation from from " + no.ID);
				this.send(message, no);
			}
			this.timeOutAnswerInvitation = Global.currentTime;
			this.timeMerge = Global.currentTime;
		}
	}

	/*-------------------------------------------------------------------------------------------------*/

	private void checkMembers() {
		if ((Global.currentTime % this.waitConst == 0) && this.flipTheCoin()) {
			if ((this.IamCoordenator()) && (this.waitingAnswerAYCoord == 0) && (this.state == 0)) {
				this.others = new ArrayList<Node>();
				AYCoord ayCoord = new AYCoord(this);
				this.broadcast(ayCoord);
				this.waitingAnswerAYCoord = this.outgoingConnections.size();
				this.timeAYCoord = Global.currentTime;
			}
		}
	}

	private void timeOutAYCoord() {
		double temp = Global.currentTime - this.timeAYCoord;
		if ((this.state == 0) && (this.waitingAnswerAYCoord > 0) && (temp > this.timeOut)) {
			this.waitingAnswerAYCoord = 0;
			this.timeAYCoord = 0;
			log("time out AYCoord " + this.ID);
		}
	}

	private void timeOutAnswerInvitation() {
		double temp = Global.currentTime - this.timeOutAnswerInvitation;
		if ((this.waitingAnswerInvitation > 0) && (temp > this.timeOut)) {
			log("Time out timeOutAnswerInvitation by id " + this.ID + " SIZE " + this.waitingAnswerInvitation);
			this.state = 0;
			this.waitingAnswerInvitation = 0;
			this.timeOutAnswerInvitation = 0;
			log("Time out timeOutAnswerInvitation " + this.ID);
		}
	}

	private void answerAYCoord(AYCoord aycoord) {
		AYC_answer ayc_answer = new AYC_answer(this, this.coordenatorGroup);
		this.send(ayc_answer, aycoord.sender);
	}

	private void processAYC_answer(AYC_answer message) {
		if (message.coord.ID != this.ID) {
			// existe algu�m que tem outro coordenador
			log("AYC_answer by " + message.node.ID + " say coord is " + message.coord);
			this.others.add(message.node);
		}
		this.waitingAnswerAYCoord--;
		// se todo mundo respondeu e existe + 1 coordenador
		if ((this.timerToMerge < Global.currentTime) && (this.waitingAnswerAYCoord == 0) && (this.others.size() > 0)) {
			this.timerToMerge = Global.currentTime + (Tools.getNodeList().size() * 10 + (10 - this.ID * 10));
			log("SetUP timerToMerge " + this.ID + " STATUS " + this.state + " timerToMerge " + this.timerToMerge);
		}
	}

	/*-------------------------------------------------------------------------------------------------*/

	private void answerInvitation(Invitation message) {
		if (this.state == 0) {
			this.oldCoordenatorGroup = this.coordenatorGroup;
			this.upSet = this.up;
			this.state = 1;
			this.coordenatorGroup = message.coordenator;
			this.coordenatorCount = message.coordenatorCount;
			if (this.oldCoordenatorGroup == this) {
				for (Node no : this.upSet) {
					this.send(message, no);
				}
			}
			Accept accept = new Accept(this, this.coordenatorCount);
			this.send(accept, message.coordenator);
			this.timeMerge = Global.currentTime;
		}
	}

	/*-------------------------------------------------------------------------------------------------*/
	private void answerAccept(Accept message) {
		log("Accept from " + message.sender.ID);
		Accept_answer accept_answer;
		if ((this.state == 1) && (this.IamCoordenator()) && (message.coordenatorCount == this.coordenatorCount)) {
			this.up.add(message.sender);
			accept_answer = new Accept_answer(this, true);
		} else {
			accept_answer = new Accept_answer(this, false);
		}
		this.send(accept_answer, message.sender);
		this.waitingAnswerInvitation--;

		if (this.waitingAnswerInvitation == 0) {
			this.state = 0;
		}
	}

	private void processAccept_answer(Accept_answer message) {
		this.state = 0;
		this.others.clear();
		this.timerToMerge = 0;
		this.timeMerge = 0;
		log("Accept_answer from " + this.ID + " by " + message.sender.ID);
	}

	/*-------------------------------------------------------------------------------------------------*/

	private void checkCoord() {
		if ((Global.currentTime % this.waitConst == 0) && this.flipTheCoin()) {
			if ((this.timeStartAYThere == 0) && (this.state == 0) && (!this.IamCoordenator())) {
				AYThere aythere = new AYThere(this, this.coordenatorCount);
				this.send(aythere, this.coordenatorGroup);
				this.timeStartAYThere = Global.currentTime;
				log("AYThere from " + this.coordenatorGroup.ID);
			}
		}
	}

	private void timeOutAYThere() {
		if (this.timeStartAYThere > 0) {
			double temp = Global.currentTime - this.timeStartAYThere;
			if ((this.state == 0) && (temp > this.timeOut)) {
				this.timeStartAYThere = 0;
				log("Time out timeOutAYThere " + this.ID);
				this.recovery(" timeOutAYThere coord " + this.coordenatorGroup);
			}
		}

	}

	private void answerAYThere(AYThere message) {
		AYThere_answer ayt_answer;
		if (this.IamCoordenator()) {
			if ((message.coordenatorCount == this.coordenatorCount) && (this.up.contains(message.sender))) {
				ayt_answer = new AYThere_answer(this, true, Math.max(this.coordenatorCount, message.coordenatorCount));
			} else {
				ayt_answer = new AYThere_answer(this, false, Math.max(this.coordenatorCount, message.coordenatorCount));
			}
			this.send(ayt_answer, message.sender);
		}
	}

	private void recovery(String motive) {
		log("Recorevy " + this.ID + " MOTIVO " + motive);
		this.coordenatorCount++;
		this.oldCoordenatorGroup = this.coordenatorGroup;
		this.coordenatorGroup = this;
		this.state = 0;
		this.timeAYCoord = 0;
		this.timerToMerge = 0;
		this.timeStartAYThere = 0;
		this.waitingAnswerAYCoord = 0;
		this.timeOutAnswerInvitation = 0;
		this.waitingAnswerInvitation = 0;
		this.timeMerge = 0;

		this.up = new ArrayList<Node>();
		this.upSet = new ArrayList<Node>();
		this.others = new ArrayList<Node>();
	}

	private void processAYThere_answer(AYThere_answer message) {
		if (!IamCoordenator()) {
			if (message.answer) {
				this.timeStartAYThere = 0;
			}
		}
	}

	private void timeOutMerge() {
		if (this.state == 1) {
			double temp = Global.currentTime - this.timeMerge;
			if (temp > this.timeOut) {
				this.timeMerge = 0;
				log("timeOutAcceptInvitation " + this.ID + " time " + temp);
				this.recovery("timeOutMerge");
			}
		}
	}

	/*-------------------------------------------------------------------------------------------------*/

	private void log(String message) {
		if (this.log_on) {
			if (IamCoordenator())
				System.out.println(Global.currentTime + "-N-" + this.ID + ": " + message);
			else
				System.out.println(Global.currentTime + "-C-" + this.ID + ": " + message);
		}
	}

	private boolean flipTheCoin() {
		int num_randomico = randomGenerator.nextInt(100);
		if (coinChancePositive > num_randomico) {
			return true;
		} else {
			return false;
		}
	}

	private String getNameFile() {
		String prefixo = "0.025_";
		SimpleDateFormat ft = new SimpleDateFormat("dd-MM-yyyy-'at'-hh-mm-ss-SSS-a");
		Date today = new Date();
		return prefixo + ft.format(today) + "_report.csv";
	}
}
