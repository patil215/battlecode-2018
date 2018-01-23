import static bc.UnitType.*;

import java.awt.*;
import java.util.*;
import java.util.List;

import bc.*;

public class Player {

	// Initialized once per game
	static GameController gc;
	static Team enemyTeam;
	static Team friendlyTeam;
	static Planet planet;
	static PlanetMap map;
	static boolean hasMadeBluePrintThisTurn;

	// Initialized/updated once per turn
	/**
	 * These variables are used to minimize calls to gc.units() and gc.myUnits(). Note that this can mean they are
	 * slightly out of date if we spawn a unit on a particular turn (since gc.units() would return that new unit
	 * immediately. However, this shouldn't make much of a difference because units have an initial cooldown, so they
	 * wouldn't be doing anything anyways.
	 */
	static ArrayList<Unit> friendlyUnits;
	static ArrayList<Unit> enemyUnits;
	static ArrayList<Unit> allUnits;

	static ArrayList<Unit> blueprints;

	// Dijkstra maps
	static Navigation workerNav;
	static Navigation armyNav;
	static Navigation builderNav;

	static HashMap<Integer, RobotMemory> robotMemory;
	static boolean seenEnemies = true;

	private static Set<Point> getInitialEnemyUnitLocations() {
		VecUnit initialUnits = map.getInitial_units();
		Set<Point> targets = new HashSet<>();
		for (int i = 0; i < initialUnits.size(); i++) {
			Unit unit = initialUnits.get(i);
			if (unit.team() == enemyTeam) {
				MapLocation unitLoc = unit.location().mapLocation();
				targets.add(new Point(unitLoc.getX(), unitLoc.getY()));
			}
		}
		return targets;
	}

	private static Set<Point> getInitialKarboniteLocations() {
		long maxX = map.getWidth();
		long maxY = map.getHeight();
		Set<Point> targets = new HashSet<>();
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

	private static List<MapLocation> getUnseenLocations() {
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

	private static int getValueFromUnitType(UnitType type) {
		// Lower is firster. Basically determines the order (by type) that units should execute
		switch (type) {
			case Knight:
				return 0;
			case Ranger:
				return 1;
			case Worker:
				return 2;
			case Healer:
				return 3;
			case Factory:
				return 4;
			case Rocket:
				return 4;
			default:
				return Integer.MAX_VALUE;

		}
	}

	private static void getUnits(boolean sort) {
		allUnits = new ArrayList<>();
		friendlyUnits = new ArrayList<>();
		enemyUnits = new ArrayList<>();
		VecUnit units = gc.units();
		for (int i = 0; i < units.size(); i++) {
			Unit unit = units.get(i);
			allUnits.add(unit);
			if (unit.team() == enemyTeam) {
				enemyUnits.add(unit);
			} else {
				friendlyUnits.add(unit);
			}
		}

		if (sort) {
			// Sort friendly units so that they're processed in a way that coordinates the armies better
			Collections.sort(friendlyUnits, (a, b) -> {
				// Sorts in ascending order, so lower values should be better
				if (a.unitType() != b.unitType()) {
					int aTypeValue = getValueFromUnitType(a.unitType());
					int bTypeValue = getValueFromUnitType(b.unitType());
					return Integer.compare(aTypeValue, bTypeValue);
				} else {
					if (a.location().isInGarrison() && b.location().isInGarrison()) {
						return 0;
					} else if (a.location().isInGarrison()) {
						return -1;
					} else if (b.location().isInGarrison()) {
						return 1;
					}

					switch (a.unitType()) {
						case Ranger: // Fall through
						case Knight: // Fall through
						case Healer: {
							int aDijValue = Player.armyNav.getDijkstraMapValue(a.location().mapLocation());
							int bDijValue = Player.armyNav.getDijkstraMapValue(b.location().mapLocation());
							return Integer.compare(aDijValue, bDijValue); // Lower is better
						}
						case Worker: {
							int aDijValue = Player.workerNav.getDijkstraMapValue(a.location().mapLocation());
							int bDijValue = Player.workerNav.getDijkstraMapValue(b.location().mapLocation());
							return Integer.compare(aDijValue, bDijValue);
						}
					}
				}
				return 0;
			});
		}
	}

	private static void beginTurn() {
		Player.hasMadeBluePrintThisTurn = false;
		getUnits(true);
		CombatUtils.initAtStartOfTurn();
	}

	private static void finishTurn() {
		CombatUtils.cleanupAtEndOfTurn();
		BuildUtils.cleanupAtEndOfTurn();

		// Fix their stupid memory leak error
		if (gc.round() > 0 && gc.round() % 10 == 0) {
			long start = System.currentTimeMillis();
			System.runFinalization();
			System.gc();
			long end = System.currentTimeMillis();
		}

		gc.nextTurn();
	}

	private static void initializeVariables() {
		gc = new GameController();
		planet = Player.gc.planet();
		map = gc.startingMap(planet);
		friendlyTeam = gc.team();
		enemyTeam = Utils.getEnemyTeam();
		getUnits(false);
		initArmyMap();
		workerNav = new Navigation(map, getInitialKarboniteLocations());
		builderNav = new Navigation(map, new HashSet<>(), Constants.BUILDER_NAV_SIZE);
		robotMemory = new HashMap<>();
		CensusCounts.resetCounts();
	}

	public static void main(String[] args) {
		initializeVariables();
		setupResearchQueue();
		initialTurns();

		while (true) {
			try {
				if (gc.getTimeLeftMs() <= 100) {
					System.out.println("Out of time. Waiting for passive regen...");
					finishTurn();
					continue;
				}				
				
				beginTurn();

				if (gc.round() % 3 == 0 && gc.round() > Constants.CLUMP_THRESHOLD) {
					updateRangerTargets();
				} else if (gc.round() == Constants.CLUMP_THRESHOLD) {
					armyNav = new Navigation(map, getInitialEnemyUnitLocations());
				}
				
				updateUnitStates(friendlyUnits);
				CensusCounts.computeCensus(friendlyUnits);
				moveUnits(friendlyUnits);
				
				// Workers will update empty karbonite positions in workerController
				if (gc.round() % 3 == 0) {
					workerNav.recalculateDistanceMap();
				}

				// Submit the actions we've done, and wait for our next turn.
				finishTurn();
			} catch(Exception e) {
				e.printStackTrace();
				gc.nextTurn();
			}
		}
	}

	private static void updateRangerTargets() {
		// Move towards rockets if we need to bail
		if (planet == Planet.Earth && gc.round() >= 500) {
			for (Point loc : new HashSet<>(armyNav.getTargets())) {
				MapLocation target = new MapLocation(planet, loc.x, loc.y);
				armyNav.removeTarget(target);
			}

			// Move toward rockets
			for (Unit unit : friendlyUnits) {
				// Add all rockets that are ready to be loaded up
				// TODO don't use constant for rocket capacity
				if (unit.unitType() == Rocket && unit.structureIsBuilt() == 1
						&& unit.structureGarrison().size() < Constants.MAX_ROCKET_CAPACITY) {
					armyNav.addTarget(unit.location().mapLocation());
				}
			}
			armyNav.recalculateDistanceMap();
			// Move towards enemies
		} else {
			// Move toward enemies
			VecUnit foes = gc.senseNearbyUnitsByTeam(new MapLocation(planet, 1, 1), 250, Utils.getEnemyTeam());

			for (Point loc : new HashSet<>(armyNav.getTargets())) {
				MapLocation target = new MapLocation(planet, loc.x, loc.y);
				if (gc.canSenseLocation(target)
						&& (!gc.hasUnitAtLocation(target) || gc.senseUnitAtLocation(target).team() == friendlyTeam)) {
					armyNav.removeTarget(target);
				}
			}

			for (int index = 0; index < foes.size(); index++) {
				armyNav.addTarget(foes.get(index).location().mapLocation());
			}
			if (Player.seenEnemies && foes.size() == 0) {
				Player.seenEnemies = false;
				for (MapLocation loc : getUnseenLocations()) {
					armyNav.addTarget(loc);
				}
			} else if (foes.size() > 0) {
				Player.seenEnemies = true;
			}
			armyNav.recalculateDistanceMap();
		}
	}

	private static void setupResearchQueue() {
		gc.queueResearch(Ranger); // Ranger 1 complete round 50
		gc.queueResearch(Healer); // Healer 1 complete round 75
		gc.queueResearch(Healer); // Healer 2 complete round 175
		gc.queueResearch(Healer); // Healer 3 complete round 275
		gc.queueResearch(Rocket); // Rocket 1 complete round 325
		gc.queueResearch(Rocket); // Rocket 2 complete round 425
		gc.queueResearch(Rocket); // Rocket 3 complete round 525
		gc.queueResearch(Ranger); // Ranger 2 complete round 626
		gc.queueResearch(Worker);
	}

	private static void updateUnitStates(ArrayList<Unit> units) {
		for (Unit unit : units) {
			if (!robotMemory.containsKey(unit.id())) {
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
			if (gc.round() <= Constants.START_BUILDING_ROCKETS_ROUND) {
				int numFactoryBuilders = CensusCounts.getWorkerModeCount(WorkerController.Mode.BUILD_FACTORIES);
				int numHarvesters = CensusCounts.getWorkerModeCount(WorkerController.Mode.HARVESTER);
				if (numFactoryBuilders < numHarvesters) {
					memory.workerMode = WorkerController.Mode.BUILD_FACTORIES;
					CensusCounts.incrementWorkerModeCount(WorkerController.Mode.BUILD_FACTORIES);
				} else {
					memory.workerMode = WorkerController.Mode.HARVESTER;
					CensusCounts.incrementWorkerModeCount(WorkerController.Mode.HARVESTER);
				}
			} else if (gc.round() > Constants.START_BUILDING_ROCKETS_ROUND) {
				// TODO modify to make sure we have at least 2-3 factories
				memory.workerMode = WorkerController.Mode.BUILD_ROCKETS;
				CensusCounts.incrementWorkerModeCount(WorkerController.Mode.BUILD_ROCKETS);
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
			// Make workers start to build rockets after a certain round
			if (gc.round() >= Constants.START_BUILDING_ROCKETS_ROUND) {
				// Only try to make rockets if we have units that need them
				int numRockets = CensusCounts.getUnitCount(Rocket);
				int numRangers = CensusCounts.getUnitCount(Ranger);
				if (numRockets * Constants.MAX_ROCKET_CAPACITY >= numRangers) {
					// TODO all of these workers might switch back and forth at once - is this what
					// we want?
					Utils.getMemory(unit).workerMode = WorkerController.Mode.IDLE;
				} else {
					Utils.getMemory(unit).workerMode = WorkerController.Mode.BUILD_ROCKETS;
				}
			}
			break;
		}
		case Factory: {
			if (gc.round() >= Constants.START_BUILDING_ROCKETS_ROUND) {
				// Limit factory production of units such that we can still build a rocket
				int numberFactoriesProducingUnits = CensusCounts.getFactoryModeCount(FactoryController.Mode.PRODUCE);

				if (gc.karbonite() > (((numberFactoriesProducingUnits + 1) * Constants.RANGER_COST)
						+ Constants.ROCKET_COST)) {
					Utils.getMemory(unit).factoryMode = FactoryController.Mode.PRODUCE;
					CensusCounts.decrementFactoryModeCount(FactoryController.Mode.IDLE);
					CensusCounts.incrementFactoryModeCount(FactoryController.Mode.PRODUCE);
				} else {
					Utils.getMemory(unit).factoryMode = FactoryController.Mode.IDLE;
					CensusCounts.decrementFactoryModeCount(FactoryController.Mode.PRODUCE);
					CensusCounts.incrementFactoryModeCount(FactoryController.Mode.IDLE);
				}

			}
			break;
		}
		default:
			break;
		}
	}

	private static void moveUnits(ArrayList<Unit> units) {
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
				case Rocket:
					RocketController.moveRocket(unit);
					break;
				case Knight:
					KnightController.moveKnight(unit);
					break;
				case Healer:
					HealerController.moveHealer(unit);
					break;
			default:
				break;
			}
		}
	}

	private static void initialTurns() {
		
		finishTurn();

		// Turn
		beginTurn();
		for (int index = 0; index < friendlyUnits.size(); index++) {
			Utils.tryAndReplicate(friendlyUnits.get(index));
		}
		updateUnitStates(friendlyUnits);
		finishTurn();

		// Turn
		beginTurn();
		for (int index = 0; index < friendlyUnits.size(); index++) {
			Unit worker = friendlyUnits.get(index);
			if (Utils.tryAndBuild(worker, Factory)) {
				break;
			}
		}
		finishTurn();
	}

	private static void initArmyMap() {
		if (Constants.CLUMP_THRESHOLD > 0) {
			VecUnit units = Player.gc.myUnits();
			Set<Point> rallyPoints = new HashSet<>();
			for (int index = 0; index < units.size(); index++) {
				MapLocation current = units.get(index).location().mapLocation();
				Direction[] directions = Direction.values();
				for (int count = 0; count < 10; count++) {
					Direction toTry = directions[(int) (directions.length * Math.random())];
					MapLocation next = current.add(toTry);
					if (map.onMap(next) && map.isPassableTerrainAt(next) != 0) {
						current = next;
					}
				}
				rallyPoints.add(new Point(current.getX(), current.getY()));
			}
			armyNav = new Navigation(map, rallyPoints);
		} else {
			armyNav = new Navigation(map, getInitialEnemyUnitLocations());
		}
	}
}
