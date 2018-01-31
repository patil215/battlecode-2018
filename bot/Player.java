import static bc.UnitType.*;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import bc.*;

public class Player {

	// Initialized once per game
	static GameController gc;
	static Team friendlyTeam;
	static Team enemyTeam;
	static Planet planet;
	static PlanetMap map;
	static long reachableKarbonite;
	static ArrayList<Unit> initialUnits;
	static boolean sentWorkerToMars = false;

	// Initialized/updated once per turn
	/**
	 * These variables are used to minimize calls to gc.units() and gc.myUnits().
	 * Note that this can mean they are slightly out of date if we spawn a unit on a
	 * particular turn (since gc.units() would return that new unit immediately.
	 * However, this shouldn't make much of a difference because units have an
	 * initial cooldown, so they wouldn't be doing anything anyways. In cases it
	 * matters, we end up updating them through calls to getUnits();
	 */
	static ArrayList<Unit> friendlyUnits;
	static ArrayList<Unit> enemyUnits;
	static ArrayList<Unit> allUnits;
	static ArrayList<Unit> blueprints;

	// Dijkstra maps
	static Navigation workerNav;
	static Navigation armyNav;
	static Navigation builderNav;
	static Navigation completedFactoryNav;

	static HashMap<Integer, RobotMemory> robotMemory;

	static boolean seenEnemies = true;
	private static int stuckCounter;

	public static Set<Point> getInitialEnemyUnitLocations() {
		Set<Point> targets = new HashSet<>();
		for (Unit unit : initialUnits) {
			if (unit.team() == enemyTeam) {
				MapLocation unitLoc = unit.location().mapLocation();
				targets.add(new Point(unitLoc.getX(), unitLoc.getY()));
			}
		}
		return targets;
	}

	private static Set<Point> getInitialAllyUnitLocations() {
		Set<Point> targets = new HashSet<>();
		for (Unit unit : initialUnits) {
			if (unit.team() == friendlyTeam) {
				MapLocation unitLoc = unit.location().mapLocation();
				targets.add(new Point(unitLoc.getX(), unitLoc.getY()));
			}
		}
		return targets;
	}

	/**
	 * Runs a weighted breadth-first search in order to find reachable karbonite.
	 */
	private static Set<Point> getInitialKarboniteLocations() {
		long karbonite = 0;
		Navigation karbNav = new Navigation(map, getInitialAllyUnitLocations());
		Set<Point> karbLocs = new HashSet<>();
		for (int i = 0; i < karbNav.distances.length; i++) {
			for (int d = 0; d < karbNav.distances[i].length; d++) {
				if (karbNav.distances[i][d] == Integer.MAX_VALUE) {
					continue;
				}
				MapLocation karbLocation = new MapLocation(planet, i, d);
				long karboniteAtLoc = map.initialKarboniteAt(karbLocation);
				karbonite += karboniteAtLoc;
				if(karboniteAtLoc > 0) {
					karbLocs.add(new Point(karbLocation.getX(), karbLocation.getY()));
				}
			}
		}
		reachableKarbonite = karbonite / 3; // "Closer to us than enemy"
		return karbLocs;
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

	private static int getOrderFromUnitType(UnitType type) {
		// Lower = firster. Determines order (by type) units should execute.
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

	public static void getUnits(boolean sortFriendlyUnits) {
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

		if (sortFriendlyUnits) {
			// Sort friendly units so that they're processed in a way that coordinates the
			// armies better
			try {
				Collections.sort(friendlyUnits, (a, b) -> {
					// Sorts in ascending order, so lower values should be better
					if (a.unitType() != b.unitType()) {
						int aTypeValue = getOrderFromUnitType(a.unitType());
						int bTypeValue = getOrderFromUnitType(b.unitType());
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
							case Factory: {
								// Sort factories by distance to enemy. This makes units default to spawning closer to enemy.
								int aDijValue = Player.armyNav.getBuildingDijkstraMapValue(a.location().mapLocation());
								int bDijValue = Player.armyNav.getBuildingDijkstraMapValue(b.location().mapLocation());
								return Integer.compare(aDijValue, bDijValue);
							}
						}
					}
					return 0;
				});
			} catch (Exception e) {

			}
		}
	}

	private static void beginTurn() {
		if (gc.round() == Constants.LIMIT_WORKER_REP_ROUND) {
			WorkerController.MAX_NUMBER_WORKERS = Math.min(4, WorkerController.MAX_NUMBER_WORKERS);
		}
		getUnits(true);
		CombatUtils.initAtStartOfTurn();
		NearbyUnitsCache.initializeAtStartOfTurn();
		BuildUtils.initAtStartOfTurn();
	}

	private static void countStuckWorkers() {
		for (Unit unit : friendlyUnits) {
			unit = Player.gc.unit(unit.id());

			if (unit.unitType() != Worker) {
				continue;
			}

			if (unit.movementHeat() < Constants.MAX_MOVEMENT_HEAT && unit.abilityHeat() < Constants.MAX_ABILITY_HEAT) {
				Utils.getMemory(unit).timeSinceLastMeaningfulAction++;
			} else {
				Utils.getMemory(unit).timeSinceLastMeaningfulAction = 0;
			}
		}

	}

	private static void countAndKillStuckUnits(int maxUnitsToKill) {
		int numKilled = 0;
		for (Unit unit : friendlyUnits) {
			UnitType type = unit.unitType();
			if (!(type == Ranger || type == Healer || type == Mage || type == Knight)) {
				continue;
			}
			if (unit.movementHeat() < Constants.MAX_MOVEMENT_HEAT && unit.attackHeat() < Constants.MAX_ATTACK_HEAT) {
				// TODO might not kill healers when needed
				Utils.getMemory(unit).timeSinceLastMeaningfulAction++;
			} else {
				Utils.getMemory(unit).timeSinceLastMeaningfulAction = 0;
			}

			if (Utils.getMemory(unit).timeSinceLastMeaningfulAction >= Constants.KILL_AFTER_USELESS_THRESHOLD) {
				try {
					// Player.gc.disintegrateUnit(unit.id());
					numKilled++;
					if (numKilled >= maxUnitsToKill) {
						break;
					}
				} catch (Exception e) {
					break;
				}
			}
		}
	}

	private static void finishTurn() {
		moveNewlyCreatedUnits();

		if (gc.getTimeLeftMs() > Constants.TIME_BUFFER_MS) {
			countStuckWorkers();
			//countAndKillStuckUnits(3);
		}

		CombatUtils.cleanupAtEndOfTurn();
		BuildUtils.cleanupAtEndOfTurn();

		// Fix their stupid memory leak error
		if (gc.round() > 0 && gc.round() % 10 == 0) {
			System.runFinalization();
			System.gc();
		}

		gc.nextTurn();
	}

	private static void determineMaxNumberOfWorkers() {
		// IF YOU CHANGE THE 10 HERE, YOU NEED TO CHANGE THE SOFT CAPPING. BE VERY CAREFUL.
		WorkerController.MAX_NUMBER_WORKERS = Math.min((int) (Player.reachableKarbonite / 50.0), 10);
	}

	private static void determineIfClumping() {
		/*
		VecUnit starting = gc.startingMap(Planet.Earth).getInitial_units();
		long minDist = Long.MAX_VALUE;
		for (int outer = 0; outer < starting.size(); outer++) {
			if (starting.get(outer).team() == Player.enemyTeam) {
				continue;
			}

			for (int inner = 0; inner < starting.size(); inner++) {
				if (starting.get(outer).team() == Player.friendlyTeam) {
					continue;
				}
				minDist = Math.min(minDist, starting.get(outer).location().mapLocation()
						.distanceSquaredTo(starting.get(inner).location().mapLocation()));
			}
		}
		if (minDist >= 800) {
			Constants.CLUMP_THRESHOLD = 125;
		} else {
			Constants.CLUMP_THRESHOLD = -1;
		}*/
		Constants.CLUMP_THRESHOLD = -1;
	}

	private static void initializeVariables() {
		gc = new GameController();
		planet = Player.gc.planet();
		map = gc.startingMap(planet);
		friendlyTeam = gc.team();
		enemyTeam = Utils.getEnemyTeam();
		robotMemory = new HashMap<>();
		initialUnits = new ArrayList<>();
		VecUnit initials = map.getInitial_units();
		for (int i = 0; i < initials.size(); i++) {
			initialUnits.add(initials.get(i));
		}

		CensusCounts.resetCounts();
		getUnits(false);
		determineIfClumping();

		workerNav = new Navigation(map, getInitialKarboniteLocations());
		builderNav = new Navigation(map, new HashSet<>(), Constants.BUILDER_NAV_SIZE);
		completedFactoryNav = new Navigation(map, new HashSet<>());

		determineMaxNumberOfWorkers();
		initArmyMap();
	}

	public static void main(String[] args) {
		initializeVariables();
		RocketController.setup();
		setupResearchQueue();

		while (true) {
			try {
				if (gc.getTimeLeftMs() <= Constants.TIME_BUFFER_MS) {
					finishTurn();
					continue;
				}

				beginTurn();

				if (gc.round() % Constants.ARMY_MAP_RECALCULATE_INTERVAL == 0
						&& gc.round() > Constants.CLUMP_THRESHOLD) {
					updateArmyTargets();
				} else if (gc.round() == Constants.CLUMP_THRESHOLD) {
					armyNav = new Navigation(map, getInitialEnemyUnitLocations());
				}

				if (gc.round() == Constants.STOP_HARVESTING_KARB_GLOBALLY_ROUND && planet == Planet.Earth) {
					workerNav.setThreshold(Constants.KARB_SMALL_HARVEST_DISTANCE);
					workerNav.recalculateDistanceMap();
				}

				CensusCounts.computeCensus(friendlyUnits);
				// CombatUtils.tryToOneShotWithRangers(); Doesn't work well
				moveUnits(friendlyUnits);

				// Periodically update karbonite locations
				if (gc.round() % Constants.KARBONITE_MAP_RECALCULATE_INTERVAL == 0 && planet == Planet.Earth) {
					workerNav.recalculateDistanceMap();
				}

				// Reupdate worker nav on Mars
				if (planet == Planet.Mars && gc.asteroidPattern().hasAsteroid(gc.round())) {
					workerNav.addTarget(gc.asteroidPattern().asteroid(gc.round()).getLocation());
					workerNav.recalculateDistanceMap();
				}

				finishTurn();

			} catch (Exception e) {
				e.printStackTrace();
				gc.nextTurn();
			}
		}
	}

	private static void moveNewlyCreatedUnits() {
		// Backup the IDs of old units
		HashSet<Integer> oldIds = Player.friendlyUnits.stream().map(unit -> unit.id())
				.collect(Collectors.toCollection(HashSet::new));

		// Update units list, and run recently created units
		Player.getUnits(true);
		for (Unit unit : Player.friendlyUnits) {
			if (!oldIds.contains(unit.id())) {
				moveUnit(unit);
				break;
			}
		}
	}

	private static void updateArmyTargets() {
		// Move towards rockets if we need to bail
		if (planet == Planet.Earth && gc.round() >= Constants.START_GETTING_INTO_ROCKETS_ROUND) {
			armyNav.clearTargets();

			// Move toward rockets
			for (Unit unit : friendlyUnits) {
				// Add all rockets that are ready to be loaded up
				if (unit.unitType() == Rocket && BuildUtils.isBuilt(unit)
						&& unit.structureGarrison().size() < unit.structureMaxCapacity()) {
					armyNav.addTarget(unit.location().mapLocation());
				}
			}

			armyNav.recalculateDistanceMap();
		} else {
			// Move toward enemies
			for (Point loc : new HashSet<>(armyNav.getTargets())) {
				MapLocation target = new MapLocation(planet, loc.x, loc.y);
				if (gc.canSenseLocation(target)
						&& (!gc.hasUnitAtLocation(target) || gc.senseUnitAtLocation(target).team() == friendlyTeam)) {
					armyNav.removeTarget(target);
				}
			}

			for (Unit unit : enemyUnits) {
				Location enemyLoc = unit.location();

				if(enemyLoc.isOnPlanet(planet)) {
					armyNav.addTarget(enemyLoc.mapLocation());
				}
			}

			if (Player.seenEnemies && enemyUnits.size() == 0) {
				Player.seenEnemies = false;
				for (MapLocation loc : getUnseenLocations()) {
					armyNav.addTarget(loc);
				}
			} else if (enemyUnits.size() > 0) {
				Player.seenEnemies = true;
			}

			armyNav.recalculateDistanceMap();
		}
	}

	private static void setupResearchQueue() {
		gc.queueResearch(Worker); // Worker 1 complete round 25
		gc.queueResearch(Ranger); // Ranger 1 complete round 50
		gc.queueResearch(Healer); // Healer 1 complete round 75
		gc.queueResearch(Healer); // Healer 2 complete round 175
		gc.queueResearch(Healer); // Healer 3 complete round 275
		gc.queueResearch(Ranger); // Ranger 2 complete round 375
		gc.queueResearch(Rocket); // Rocket 1 complete round 450
		gc.queueResearch(Rocket); // Rocket 2 complete round 550
		gc.queueResearch(Rocket); // Rocket 3 complete round 650// Remember to update the Utils.getMaxRocketCapacity if Rocket III timing is // changed
		gc.queueResearch(Ranger); // Ranger 3 complete round 850 (this is useless but might throw other team off)
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
				// Replenish factories
				int numFactories = CensusCounts.getUnitCount(Factory);
				if (numFactories == 0) { // Replenish a factory if we lose it
					if (Utils.getMemory(unit).workerMode == WorkerController.Mode.BUILD_ROCKETS) {
						CensusCounts.decrementWorkerModeCount(WorkerController.Mode.BUILD_ROCKETS);
						Utils.getMemory(unit).workerMode = WorkerController.Mode.BUILD_FACTORIES;
						CensusCounts.incrementWorkerModeCount(WorkerController.Mode.BUILD_FACTORIES);
					}
				} else {
					if (Utils.getMemory(unit).workerMode == WorkerController.Mode.BUILD_FACTORIES) {
						CensusCounts.decrementWorkerModeCount(WorkerController.Mode.BUILD_FACTORIES);
						Utils.getMemory(unit).workerMode = WorkerController.Mode.BUILD_ROCKETS;
						CensusCounts.incrementWorkerModeCount(WorkerController.Mode.BUILD_ROCKETS);
					}
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
						CensusCounts.decrementFactoryModeCount(FactoryController.Mode.IDLE);
						Utils.getMemory(unit).factoryMode = FactoryController.Mode.PRODUCE;
						CensusCounts.incrementFactoryModeCount(FactoryController.Mode.PRODUCE);
					}
				} else {
					if (Utils.getMemory(unit).factoryMode == FactoryController.Mode.PRODUCE) {
						CensusCounts.decrementFactoryModeCount(FactoryController.Mode.PRODUCE);
						Utils.getMemory(unit).factoryMode = FactoryController.Mode.IDLE;
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
		for (Unit unit : units) {
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
