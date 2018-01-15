
// import the API.
// See xxx for the javadocs.

import bc.*;
import java.util.*;
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;

public class Player {

	public static GameController gc;
	static ArrayList<Unit> blueprints;
	static Team enemy;
	static Navigation workerNav;
	static Navigation armyNav;
	static HashMap<Integer, RobotMemory> robotMemory;

	private static List<Point> getEnemyUnits(VecUnit initUnits) {
		List<Point> targets = new ArrayList<>();
		for (int i = 0; i < initUnits.size(); i++) {
			Unit unit = initUnits.get(i);
			if (unit.team() == enemy) {
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
		for (int x = 0; x < maxX; x++) {
			for (int y = 0; y < maxY; y++) {
				MapLocation loc = new MapLocation(planet, x, y);
				if (map.initialKarboniteAt(loc) > 0) {
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

		enemy = Utils.getEnemyTeam();

		PlanetMap map = gc.startingMap(gc.planet());
		armyNav = new Navigation(map, getEnemyUnits(map.getInitial_units()));
		workerNav = new Navigation(map, getInitialKarb(map));

		InitialTurns();

		while (true) {
			VecUnit units = gc.myUnits();
			VecUnit foes = gc.senseNearbyUnitsByTeam(new MapLocation(gc.planet(), 1, 1), 250, Utils.getEnemyTeam());

			for (Point loc : new HashSet<Point>(armyNav.getTargets())) {
				MapLocation target = new MapLocation(gc.planet(), loc.x, loc.y);
				if (gc.canSenseLocation(target)
						&& (!gc.hasUnitAtLocation(target) || gc.senseUnitAtLocation(target).team() == gc.team())) {
					armyNav.removeTarget(target);
				}
			}

			for (int index = 0; index < foes.size(); index++) {
				armyNav.addTarget(foes.get(index).location().mapLocation());
			}

			armyNav.recalcDistanceMap();

			initNewUnitMemories(units);

			computeCensus(units);

			MoveUnits(units);
		}
	}

	private static void initNewUnitMemories(VecUnit units) {
		for (int index = 0; index < units.size(); index++) {
			Unit unit = units.get(index);
			if (!robotMemory.containsKey(units.get(index).id())) {
				RobotMemory memory = new RobotMemory();
				if(CensusCounts.workerBuilderCount >= 2 && unit.unitType() == UnitType.Worker) {
					memory.workerRole = RobotMemory.WorkerRole.HARVESTER;
				}
				robotMemory.put(unit.id(), memory);
			}
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
		
		Player.initNewUnitMemories(startingWorkers);
		
		for (int index = 0; index < startingWorkers.size(); index++) {
			Unit worker = startingWorkers.get(index);
			if (Utils.tryAndBuild(worker.id(), UnitType.Factory)) {
				break;
			}
		}

		gc.nextTurn();

		for (int index = 0; index < startingWorkers.size(); index++) {
			Utils.tryAndReplicate(startingWorkers.get(index));
		}

		gc.nextTurn();
	}

	private static void computeCensus(VecUnit units) {
		CensusCounts.workerCount = 0;
		CensusCounts.workerHarvesterCount = 0;
		CensusCounts.workerBuilderCount = 0;

		ArrayList<Unit> blueprints = new ArrayList<Unit>();

		for (int index = 0; index < units.size(); index++) {
			Unit unit = units.get(index);
			if (unit.unitType() == UnitType.Worker) {
				CensusCounts.workerCount++;
				switch (Utils.getMemory(unit).workerRole) {
					case HARVESTER:
						CensusCounts.workerHarvesterCount++;
						break;
					case BUILDER:
						CensusCounts.workerBuilderCount++;
						break;
				}
			}
			if ((unit.unitType() == UnitType.Factory || unit.unitType() == UnitType.Rocket)
					&& unit.structureIsBuilt() == 0) {
				blueprints.add(unit);
			}
		}
		Player.blueprints = blueprints;
	}
}
