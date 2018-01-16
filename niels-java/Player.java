
// import the API.
// See xxx for the javadocs.

import bc.*;
import java.util.*;
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;

import static bc.UnitType.Worker;

public class Player {

	public static GameController gc;

	static ArrayList<Unit> blueprints;
	static Team enemy;
	static Navigation workerNav;
	static Navigation armyNav;
	static HashMap<Integer, RobotMemory> robotMemory;
	static PlanetMap map;

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

	private static List<Point> getInitialKarb() {
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

	private static List<MapLocation> getUnseenLocs() {
		Planet planet = map.getPlanet();
		long maxX = map.getWidth();
		long maxY = map.getHeight();
		List<MapLocation> targets = new ArrayList<>();
		for (int x = 0; x < maxX; x++) {
			for (int y = 0; y < maxY; y++) {
				MapLocation loc = new MapLocation(planet, x, y);
				if (!Player.gc.canSenseLocation(loc)) {
					targets.add(loc);
				}
			}
		}
		return targets;
	}

	public static void main(String[] args) {
		// Connect to the manager, starting the game
		gc = new GameController();

		CensusCounts.resetCounts();
		robotMemory = new HashMap<>();
		enemy = Utils.getEnemyTeam();
		map = gc.startingMap(gc.planet());
		armyNav = new Navigation(map, getEnemyUnits(map.getInitial_units()));
		workerNav = new Navigation(map, getInitialKarb());

		setupResearchQueue();
		initialTurns();

		while (true) {
			VecUnit units = gc.myUnits();

			updateEnemyTargets();
			updateUnitStates(units);
			computeCensus(units);
			moveUnits(units);

			// Workers will update empty karbonite positions in workerController
			workerNav.recalcDistanceMap();

			//armyNav.printDistances();
		}
	}

	private static void updateEnemyTargets() {
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
		
		if(foes.size() == 0) {
			for(MapLocation loc : getUnseenLocs()) {
				armyNav.addTarget(loc);
			}
		}

		armyNav.recalcDistanceMap();
	}

	private static void setupResearchQueue() {
		gc.queueResearch(UnitType.Ranger); // Level 1 Ranger (ends at turn 25)
		gc.queueResearch(Worker); // Level 1 Worker (ends at turn 50)
		gc.queueResearch(Worker); // Level 2 Worker (ends at turn 125)
		gc.queueResearch(Worker); // Level 3 Worker (ends at turn 200)
		gc.queueResearch(Worker); // Level 4 Worker (ends at turn 275)
		gc.queueResearch(UnitType.Rocket); // Level 1 Rocket (ends at turn 375)
		gc.queueResearch(UnitType.Rocket); // Level 2 Rocket (ends at turn 475)
		gc.queueResearch(UnitType.Rocket); // Level 3 Rocket (ends at turn 575)
		gc.queueResearch(UnitType.Ranger); // Level 2 Ranger (ends at turn 675) TODO: adjust ranger to still attack even if it sees boi
		gc.queueResearch(UnitType.Ranger); // Level 3 Ranger (ends at turn 775) TODO: sniping code
		// TODO we have more space for research but we don't have any other units...
	}

	private static void updateUnitStates(VecUnit units) {
		for (int index = 0; index < units.size(); index++) {
			Unit unit = units.get(index);
			if (!robotMemory.containsKey(units.get(index).id())) {
				createNewUnitState(unit);
			} else {
				updateExistingUnitState(unit);
			}
		}
	}

	private static void createNewUnitState(Unit unit) {
		RobotMemory memory = new RobotMemory();
		switch (unit.unitType()) {
			case Worker: {
				if (gc.round() <= Constants.START_BUILDING_ROCKETS_ROUND
						&& CensusCounts.getWorkerModeCount(RobotMemory.WorkerMode.BUILD_FACTORIES) >= 2) {
						memory.workerMode = RobotMemory.WorkerMode.HARVESTER;
				} else if (gc.round()> Constants.START_BUILDING_ROCKETS_ROUND) {
					// TODO modify to make sure we have at least 2-3 factories
					memory.workerMode = RobotMemory.WorkerMode.BUILD_ROCKETS;
				}
				break;
			}
			default:
				break;
		}
		robotMemory.put(unit.id(), memory);
	}

	private static void updateExistingUnitState(Unit unit) {
		switch (unit.unitType()) {
			case Worker: {
				if (gc.round() >= Constants.START_BUILDING_ROCKETS_ROUND
						&& Utils.getMemory(unit).workerMode == RobotMemory.WorkerMode.BUILD_FACTORIES) {
					Utils.getMemory(unit).workerMode = RobotMemory.WorkerMode.BUILD_ROCKETS;
				}
			}
			default:
				break;
		}
	}

	private static void moveUnits(VecUnit units) {
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

	private static void initialTurns() {
		gc.nextTurn();

		VecUnit startingWorkers = gc.units();
		
		Player.updateUnitStates(startingWorkers);
		
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
		CensusCounts.resetCounts();

		ArrayList<Unit> blueprints = new ArrayList<Unit>();

		for (int index = 0; index < units.size(); index++) {
			Unit unit = units.get(index);
			if (unit.unitType() == Worker) {
				CensusCounts.workerCount++;
				CensusCounts.incrementWorkerModeCount(Utils.getMemory(unit).workerMode);
			}
			if ((unit.unitType() == UnitType.Factory || unit.unitType() == UnitType.Rocket)
					&& unit.structureIsBuilt() == 0) {
				blueprints.add(unit);
			}
		}
		Player.blueprints = blueprints;
	}
}
