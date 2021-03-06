package competitor;

import snowbound.api.*;
import snowbound.api.util.*;
import static snowbound.api.util.Utility.*;

import java.util.*;

@Agent(name = "A Smiling AI")
public class CompetitorAI extends AI {

	private boolean start = true;

	Position spawnSpawn;
	Position target;

	Unit layers;
	Turn turn;

	Base[] covered = new Base[2];
	Base goalBase;
	List<Position> path;

	public CompetitorAI() {
		System.err.println("New game");
		System.out.println("New game");


	}

	public Action action(Turn turn) {
		Unit actor = turn.actor();
		this.turn = turn;


		if(start){
			start = false;

			//Pick spawnSpawn, highest concentration of base
			double concentration = 0;
			spawnSpawn = null;
			double con = 0;
			for(Position spawn : actor.team().spawns()){
				for(Base base : turn.allBases()){

					con += 1.0/(new Pathfinding()).getPath(turn, spawn, base.position()).size();
				}
				if(con > concentration){
					concentration = con;
					spawnSpawn = spawn;
				}
				con = 0;
			}
			//Spawn near spawnSpawn
			System.out.println(bestBase(turn,spawnSpawn));
			layers = actor;
			//First unit will alwsy be layers
			//Layers leads charges upon bases
			//Woo
		}


		//Spawn logic
		if(actor.position() == null){
			//Reset goals upon death
			//They weren't very good goals anyway

			boolean layersDied = true;
			for (Unit u: turn.myUnits()) {
				if (u.perk() == Perk.CLEATS && u.position() != null) {
					layersDied = false;
				}
			}

			if (layersDied || turn.allTeams().size() == 2) {
				System.out.println("SPAWNING: Coat");
				Positionable pos = bestLayerSpawn(turn);
				System.out.println(pos);
				layers = actor;
				return new SpawnAction(pos, Perk.CLEATS);
			} else {
				System.out.println("SPAWNING: Walrus");
				return new SpawnAction(bestThugSpawn(turn), Perk.BUCKET);
			}
		}

		//		if (turn.hasBaseAt(turn.actor().position()) && turn.myBases().contains(turn.baseAt(turn.actor().position()))) {
		//			return new CaptureAction();
		//		}


		switch (actor.perk()) {
		case CLEATS:
			layers = actor;
			goalBase = bestBase(turn,actor.position());
			// Find and capture bases

			// Check if layers' on a base
			if (turn.hasBaseAt(layers.position())) {
				// Check if we own it
				if (!turn.myBases().contains(turn.baseAt(turn.actor().position()))) {
					// We don't own this base

					covered[0] = covered[1];
					covered[1] = null;
					return new CaptureAction();
				}
			}

			path = (new Pathfinding()).getPath(turn, actor.position(), goalBase.position());
			if(path.size() < actor.statistic(Stat.MOVE)){
				target = path.get(path.size()-1);
			}else{
				target = path.get(actor.statistic(Stat.MOVE) - 1);
			}
			covered[0] = covered[1];
			covered[1] = goalBase;
			return new MoveAction(target);
		default:
			// Get snowballs, then guard
			// Technically bucket

			if (turn.hasBaseAt(actor.position())) {
				if (!turn.baseAt(actor.position()).isOwnedBy(turn.myTeam())) {
					return new CaptureAction();
				}
			}

			// Protect Layers
			if (numEnemiesAtBase(goalBase) > numFriendsAtBase(goalBase)) {


				path = (new Pathfinding()).getPath(turn, actor.position(), goalBase.position());
				if(path.size() < 3){
					target = path.get(path.size()-1);
				}else{
					target = path.get(2);
				}

				System.out.println("BUCKET: helping capture " + target);
				return new MoveAction(target);
			}

			if (actor.snowballs() > 0) {
				Unit enemy = null;
				for (Unit u : turn.allUnits()) {
					if (!u.team().equals(turn.myTeam())) {
						if (u.position() == null) continue;
						if (u.position().distance(actor.position()) < 4) {
							// TODO Enemy close to layers, but far from us
							if (enemy == null) {
								enemy = u;
							} else {
								if (enemy.position() == null || layers.position() == null) continue;
								if (u.position().distance(layers.position()) < enemy.position().distance(layers.position())) {
									enemy = u;
								}
							}
						}
					}
				}
				if (enemy != null) {
					return new ThrowAction(enemy);
				}
			}

			if (actor.snowballs() < 3) {
				System.out.println("BUCKET: No snow");
				if (turn.tileAt(actor.position()).snow() > 0) {
					System.out.println("BUCKET: Gathering snowballs");
					return new GatherAction();
				}
				// Be sad
				Position next = actor.position();
				for (Position p1 : turn.tileAt(actor.position()).neighbors()) {
					for (Position p2 : turn.tileAt(p1).neighbors()) {
						for (Position p3 : turn.tileAt(p2).neighbors()) {
							if (turn.tileAt(p3).snow() > 0) {
								if(layers.position() == null){
									System.out.println("Layers be null");
									continue;
								}
								if(next == null){
									System.out.println("Next be null");
									continue;
								}
								if (p3.distance(layers.position()) < next.distance(layers.position())) {
									next = p3;
								}
							}
						}
					}
				}
				path = (new Pathfinding()).getPath(turn, actor.position(), next);
				if(path.size() < 3){
					target = path.get(path.size()-1);
				}else{
					target = path.get(2);
				}
				System.out.println(target);
				return new MoveAction(target);
			}
			if (layers.position() != null) {
				if (actor.position().distance(layers.position()) > 3) {
					System.out.println("BUCKET: Chasing longcoat");

					path = (new Pathfinding()).getPath(turn, actor.position(), goalBase.position());
					if(path.size() < 3){
						target = path.get(path.size()-1);
					}else{
						target = path.get(2);
					}
					return new MoveAction(target);
				}
			}
			break;
		}



		return null;
	}

	private Positionable bestThugSpawn(Turn turn) {
		Position spawn = null;
		if (layers.position() != null) {
			for(Position temp : turn.myTeam().spawns()){
				if (spawn == null && temp != layers.position() && !turn.hasUnitAt(temp)) {
					spawn = temp;
				} else {
					if (temp.distance(layers.position()) < spawn.distance(layers.position()) && !turn.hasUnitAt(temp)) {
						spawn = temp;
					}
				}
			}
			return spawn;
		} else {
			// Layers kicked the bucket
			return bestLayerSpawn(turn);
		}

	}

	private Positionable bestLayerSpawn(Turn turn) {
		double concentration = 0;
		Position spawn = null;
		double con = 0;
		for(Position temp : turn.actor().team().spawns()){
			for(Base base : turn.allBases()){
				if (base.isOwnedBy(turn.myTeam()) || turn.hasUnitAt(temp)) continue;
				con += 1.0/temp.distance(base.position());
			}
			if(con > concentration){
				concentration = con;
				spawn = temp;
			}
			con = 0;
		}
		while (spawn == null) {
			Position temp = any(turn.myTeam().spawns());
			if(!turn.hasUnitAt(temp)){
				return temp;
			}
		}

		return spawn;
	}

	private Base bestBase(Turn turn, Position pos) {
		double minCost = Double.MAX_VALUE;
		double cost = 0;
		int conWeight = 2;
		Base goBase = null;
		for(Base base : turn.allBases()){
			if((cost = (new Pathfinding()).getPath(turn, base.position(), pos).size() * conWeight * concentration(turn.enemyUnits(),base.position())) < minCost && !turn.myBases().contains(base)){
				if (!base.equals(covered[0]) && !base.equals(covered[1])) {
					minCost = cost;
					goBase = base;
				}
			}
		}
		if(goBase==null){
			for(Base base : turn.allBases()){
				if((cost = (new Pathfinding()).getPath(turn, base.position(), pos).size() * conWeight * concentration(turn.enemyUnits(),base.position())) < minCost && !turn.myBases().contains(base)){
					minCost = cost;
					goBase = base;
				}
			}
		}
		return goBase;
	}

	private double concentration(Set<? extends Positionable> things, Position pos) {
		double con = 0;
		for(Positionable spot : things){
			if (spot == null || spot.position() == null) { continue; }
			con += 1.0/spot.position().distance(pos);  

		}
		return con;
	}

	private int numEnemiesAtBase(Base b) {
		int n = 0;
		if (b == null) { return 0; }
		for (Position p: b.coverage()) {
			if (turn.hasUnitAt(p)) {
				if (!turn.unitAt(p).team().equals(turn.myTeam())) {
					n++;
				}
			}
		}
		return n;
	}

	private int numFriendsAtBase(Base b) {
		int n = 0;
		for (Position p: b.coverage()) {
			if (turn.hasUnitAt(p)) {
				if (turn.unitAt(p).team().equals(turn.myTeam())) {
					n++;
				}
			}
		}
		return n;
	}
}