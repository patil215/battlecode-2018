
// import the API.
// See xxx for the javadocs.
import java.util.*;
import java.awt.Point;

import bc.*;

public class Player {

	public static GameController gc;
	static ArrayList<Unit> blueprints;
	static int workerCount;
	static Team enemy;
	static Navigation workerNav;
	static Navigation armyNav;
	static HashMap<Integer, RobotMemory> robotMemory;

	private static List<Point> getEnemyUnits(VecUnit initUnits) {
		List<Point> targets = new ArrayList<>();
		for(int i = 0; i < initUnits.size(); i++) {
			Unit unit = initUnits.get(i);
			if(unit.team() == enemy) {
				MapLocation unitLoc = unit.location().mapLocation();
				targets.add(new Point(unitLoc.getX(), unitLoc.getY()));
			}
		}
		return targets;
	}

	private static List<Point> getInitialKarb(PlanetMap map) {
		Planet planet = map.getPlanet();
		long maxX = map.getWidth();
		long maxY = map.getHeight();
		List<Point> targets = new ArrayList<>();
		for(int x = 0; x < maxX; x++) {
			for(int y = 0; y < maxY; y++) {
				MapLocation loc = new MapLocation(planet, x, y);
				if(map.initialKarboniteAt(loc) > 0) {
					targets.add(new Point(x, y)); 
				}
			}
		}
		return targets;
	}

	public static void main(String[] args) {
		// Connect to the manager, starting the game
		gc = new GameController();

		robotMemory = new HashMap<Integer, RobotMemory>();

		if (gc.team() == Team.Red) {
			enemy = Team.Blue;
		} else {
			enemy = Team.Red;
		}

		PlanetMap map = gc.startingMap(gc.planet());
		armyNav = new Navigation(map, getEnemyUnits(map.getInitial_units()));
		workerNav = new Navigation(map, getInitialKarb(map));
		
		InitialTurns();

		while (true) {
			VecUnit units = gc.myUnits();
			for (int index = 0; index < units.size(); index++) {
				if (!robotMemory.containsKey(units.get(index).id())) {
					robotMemory.put(units.get(index).id(), new RobotMemory());
				}
			}

			organizeUnits(units);

			MoveUnits(units);
		}
	}

	private static void MoveUnits(VecUnit units) {
		for (int index = 0; index < units.size(); index++) {
			Unit unit = units.get(index);
			switch (unit.unitType()) {
			case Worker:
				WorkerController.moveWorker(unit);
				break;
			case Factory:
				FactoryController.moveFactory(unit);
				break;
			case Ranger:
				RangerController.moveRanger(unit);
				break;
			default:
				break;
			}
		}
		// Submit the actions we've done, and wait for our next turn.
		gc.nextTurn();
	}

	private static void InitialTurns() {
		gc.nextTurn();

		VecUnit startingWorkers = gc.units();
		for (int index = 0; index < startingWorkers.size(); index++) {
			Unit worker = startingWorkers.get(index);
			if (Utils.tryAndBuild(worker.id(), UnitType.Factory)) {
				break;
			}
		}

		gc.nextTurn();

		for (int index = 0; index < startingWorkers.size(); index++) {
			Unit worker = startingWorkers.get(index);
			Utils.tryAndReplicate(worker.id());
		}

		gc.nextTurn();
	}

	private static void organizeUnits(VecUnit units) {
		workerCount = 0;
		ArrayList<Unit> blueprints = new ArrayList<Unit>();
		for (int index = 0; index < units.size(); index++) {
			Unit unit = units.get(index);
			if (unit.unitType() == UnitType.Worker) {
				workerCount++;
			}
			if ((unit.unitType() == UnitType.Factory || unit.unitType() == UnitType.Rocket)
					&& unit.structureIsBuilt() == 0) {
				blueprints.add(unit);
			}
		}
		Player.blueprints = blueprints;
	}
}
