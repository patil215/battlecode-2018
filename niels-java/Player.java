import bc.*;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static bc.UnitType.*;

public class Player {

	// Initialized once per game
	static GameController gc;
	static Team enemyTeam;
	static Team friendlyTeam;
	static Planet planet;
	static PlanetMap map;
	static boolean hasMadeBluePrintThisTurn;
	static long reachableKarbonite;
	static boolean greedyEconMode;

	// Initialized/updated once per turn
	/**
	 * These variables are used to minimize calls to gc.units() and gc.myUnits().
	 * Note that this can mean they are slightly out of date if we spawn a unit on a
	 * particular turn (since gc.units() would return that new unit immediately.
	 * However, this shouldn't make much of a difference because units have an
	 * initial cooldown, so they wouldn't be doing anything anyways.
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
	private static int stuckCounter;

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

	private static Set<Point> getInitialAllyUnitLocations() {
		VecUnit initialUnits = map.getInitial_units();
		Set<Point> targets = new HashSet<>();
		for (int i = 0; i < initialUnits.size(); i++) {
			Unit unit = initialUnits.get(i);
			if (unit.team() == friendlyTeam) {
				MapLocation unitLoc = unit.location().mapLocation();
				targets.add(new Point(unitLoc.getX(), unitLoc.getY()));
			}
		}
		return targets;
	}

	/*
	 * Note: call this to also compute the karbonite that is safe to grab
	 */
	private static Set<Point> getInitialKarboniteLocations() {
		long maxX = map.getWidth();
		long maxY = map.getHeight();
		Set<Point> targets = new HashSet<>();
		Set<Point> foeSpawns = getInitialEnemyUnitLocations();
		Set<Point> spawns = getInitialAllyUnitLocations();
		reachableKarbonite = 0;
		for (int x = 0; x < maxX; x++) {
			for (int y = 0; y < maxY; y++) {
				MapLocation loc = new MapLocation(planet, x, y);
				long karbonite = map.initialKarboniteAt(loc);
				if (karbonite > 0) {
					targets.add(new Point(x, y));
					long minFoeDistance = Long.MAX_VALUE;
					for (Point foeSpawn : foeSpawns) {
						MapLocation spawnLoc = new MapLocation(gc.planet(), foeSpawn.x, foeSpawn.y);
						minFoeDistance = Math.min(minFoeDistance, spawnLoc.distanceSquaredTo(loc));
					}
					long minAllyDistance = Long.MAX_VALUE;
					for (Point allySpawn : spawns) {
						MapLocation spawnLoc = new MapLocation(gc.planet(), allySpawn.x, allySpawn.y);
						minAllyDistance = Math.min(minAllyDistance, spawnLoc.distanceSquaredTo(loc));
					}
					if (minAllyDistance * 3 < minFoeDistance * 2) {
						reachableKarbonite += karbonite;
					}
				}
			}
		}
		System.out.println(reachableKarbonite);
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
		// Lower is firster. Basically determines the order (by type) that units should
		// execute
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

	public static void getUnits(boolean sort) {
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

		updateUnitStates(friendlyUnits);

		if (sort) {
			// Sort friendly units so that they're processed in a way that coordinates the
			// armies better
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
		if (gc.round() == Constants.LIMIT_WORKER_REP_ROUND) {
			WorkerController.MAX_NUMBER_WORKERS = 6;
		}
		Player.hasMadeBluePrintThisTurn = false;
		getUnits(true);
		CombatUtils.initAtStartOfTurn();
		BuildUtils.findBestFactoryBuildLocations();
	}

	private static void finishTurn() {
		moveNewlyCreatedUnits();

		// TODO: Find better criteria for this
		if (gc.planet() == Planet.Earth && Utils.stuck()) {
			stuckCounter++;
		} else {
			stuckCounter = 0;
		}
		if (stuckCounter > Constants.AMOUNT_STUCK_BEFORE_KILL && gc.round() > 100 && friendlyUnits.size()>1
				&& gc.planet() == Planet.Earth) {
			gc.disintegrateUnit(Player.friendlyUnits.get((int) (Math.random() * friendlyUnits.size())).id());
			stuckCounter = 0;
		}

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
		robotMemory = new HashMap<>();
		CensusCounts.resetCounts();
		getUnits(false);
		initArmyMap();
		workerNav = new Navigation(map, getInitialKarboniteLocations());
		builderNav = new Navigation(map, new HashSet<>(), Constants.BUILDER_NAV_SIZE);
		greedyEconMode = Player.reachableKarbonite >= Constants.REACHABLE_KARBONITE_THREASHOLD;
		if (greedyEconMode) {
			WorkerController.MAX_NUMBER_WORKERS = 12;
		} else {
			WorkerController.MAX_NUMBER_WORKERS = 6;
		}
	}

	public static void main(String[] args) {
		initializeVariables();
		setupResearchQueue();

		while (true) {
			try {
				if (gc.getTimeLeftMs() <= 100) {
					finishTurn();
					continue;
				}

				beginTurn();

				if (gc.round() % 3 == 0 && gc.round() > Constants.CLUMP_THRESHOLD) {
					updateRangerTargets();
				} else if (gc.round() == Constants.CLUMP_THRESHOLD) {
					armyNav = new Navigation(map, getInitialEnemyUnitLocations());
				}
				CensusCounts.computeCensus(friendlyUnits);
				moveUnits(friendlyUnits);

				// Workers will update empty karbonite positions in workerController
				if (gc.round() % 3 == 0) {
					workerNav.recalculateDistanceMap();
				}

				// Submit the actions we've done, and wait for our next turn.
				finishTurn();
			} catch (Exception e) {
				e.printStackTrace();
				gc.nextTurn();
			}
		}
	}

	private static void moveNewlyCreatedUnits() {
		// Backup the IDs of old units
		HashSet<Integer> oldIds =
				Player.friendlyUnits.stream().map(unit -> unit.id()).collect(Collectors.toCollection(HashSet::new));

		// Update units list, and run recently created units
		Player.getUnits(true);
		for (Unit unit : Player.friendlyUnits) {
			if (!oldIds.contains(unit.id())) {
				moveUnit(unit);
				break;
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
						&& unit.structureGarrison().size() < unit.structureMaxCapacity()) {
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
		/*gc.queueResearch(Ranger); // Ranger 1 complete round 25
		gc.queueResearch(Healer); // Healer 1 complete round 50
		gc.queueResearch(Healer); // Healer 2 complete round 150
		gc.queueResearch(Healer); // Healer 3 complete round 250
		gc.queueResearch(Rocket); // Rocket 1 complete round 300
		gc.queueResearch(Rocket); // Rocket 2 complete round 400
		gc.queueResearch(Rocket); // Rocket 3 complete round 500
		// Remember to update the Utils.getMaxRocketCapacity if Rocket III timing is
		// changed
		gc.queueResearch(Ranger); // Ranger 2 complete round 600
		gc.queueResearch(Worker); // Worker 1 complete round 625
		gc.queueResearch(Worker); // Worker 2 complete round 700
		gc.queueResearch(Ranger); // Ranger 3 complete round 900 (this is useless but might throw other team off)
		 */
		
		gc.queueResearch(Worker); // 25
		gc.queueResearch(Knight); // 50
		gc.queueResearch(Knight); // 125
		gc.queueResearch(Knight); // 225
		gc.queueResearch(Healer); // 250
		gc.queueResearch(Healer); // 350		
		gc.queueResearch(Healer); // 450
		gc.queueResearch(Rocket); // 500
		gc.queueResearch(Rocket); // 600
		gc.queueResearch(Rocket); // 700
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
				memory.workerMode = WorkerController.Mode.BUILD_FACTORIES;
				CensusCounts.incrementWorkerModeCount(WorkerController.Mode.BUILD_FACTORIES);
			} else if (gc.round() > Constants.START_BUILDING_ROCKETS_ROUND) {
				// TODO modify to make sure we have at least 2-3 factories
				memory.workerMode = WorkerController.Mode.BUILD_ROCKETS;
				CensusCounts.incrementWorkerModeCount(WorkerController.Mode.BUILD_ROCKETS);
			}
			break;
		}
		case Factory: {
			memory.factoryMode = FactoryController.Mode.PRODUCE;
			CensusCounts.incrementFactoryModeCount(FactoryController.Mode.PRODUCE);
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
				int numRangers = CensusCounts.getMilitaryCount();
				if (numRockets * Utils.getMaxRocketCapacity() >= numRangers) {
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
						+ Constants.ROCKET_COST) // If we have enough Karbonite compared to rockets
						|| CensusCounts.getMilitaryCount() // If we have way more rockets than Karbonite
						< (CensusCounts.getUnitCount(Rocket) * Utils.getMaxRocketCapacity())) {
					if (Utils.getMemory(unit).factoryMode == FactoryController.Mode.IDLE) {
						Utils.getMemory(unit).factoryMode = FactoryController.Mode.PRODUCE;
						CensusCounts.decrementFactoryModeCount(FactoryController.Mode.IDLE);
						CensusCounts.incrementFactoryModeCount(FactoryController.Mode.PRODUCE);
					}
				} else {
					if (Utils.getMemory(unit).factoryMode == FactoryController.Mode.PRODUCE) {
						Utils.getMemory(unit).factoryMode = FactoryController.Mode.IDLE;
						CensusCounts.decrementFactoryModeCount(FactoryController.Mode.PRODUCE);
						CensusCounts.incrementFactoryModeCount(FactoryController.Mode.IDLE);
					}
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
			moveUnit(unit);
		}
	}

	public static void moveUnit(Unit unit) {
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

	private static void initArmyMap() {
		if (Constants.CLUMP_THRESHOLD > 0) {
			ArrayList<Unit> units = Player.friendlyUnits;
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
