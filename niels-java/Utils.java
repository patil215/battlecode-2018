import bc.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static bc.UnitType.Worker;

public class Utils {
	// includes diagonals (1 vertical, 1 horiz = 1^2 + 1^2 = 2, this is inclusive)
	// will not include any other squares.
	private static final long DISTANCE_SQUARED_FOR_ONLY_SURROUNDINGS = 2L;

	static int countSurrounding(MapLocation loc, Function<MapLocation, Boolean> f) {
		int count = 0;
		for (Direction dir : Direction.values()) {
			if (dir == Direction.Center)
				continue;
			MapLocation newLoc = loc.add(dir);
			if (Player.map.onMap(newLoc) && f.apply(newLoc)) {
				count++;
			}
		}
		return count;
	}

	static int countNearbyWorkers(MapLocation loc) {
		VecUnit workers = Player.gc.senseNearbyUnitsByType(loc, DISTANCE_SQUARED_FOR_ONLY_SURROUNDINGS,
				Worker);
		int numOurTeam = 0;
		for (int i = 0; i < workers.size(); i++) {
			if (workers.get(i).team() == Player.friendlyTeam) {
				numOurTeam++;
			}
		}
		return numOurTeam;
	}

	static int countNearbyConstruction(MapLocation loc) {
		VecUnit units = Player.gc.senseNearbyUnitsByType(loc, DISTANCE_SQUARED_FOR_ONLY_SURROUNDINGS, UnitType.Factory);
		int numOurTeam = 0;
		for (int i = 0; i < units.size(); i++) {
			Unit factory = units.get(i);
			if (factory.team() == Player.friendlyTeam && !BuildUtils.isBuilt(factory)) {
				numOurTeam++;
			}
		}
		return numOurTeam;
	}

	public static int countEmptySquaresSurrounding(MapLocation cur) {
		return countSurrounding(cur, loc -> Player.map.onMap(loc) && Player.map.isPassableTerrainAt(loc) == 1
				&& !Player.gc.hasUnitAtLocation(loc));
	}

	public static boolean tryAndBuild(Unit worker, UnitType type) {
		int workerId = worker.id();
		MapLocation loc = worker.location().mapLocation();
		int bestCount = -1;
		List<MapLocation> bestNextPositions = new ArrayList<>();
		// find the positions around that the worker that maximize the number
		// of workers surrounding it
		for (Direction direction : Direction.values()) {
			MapLocation newLoc = loc.add(direction);
			if (Player.gc.canBlueprint(workerId, type, direction)) {
				int numSurroundingWorkers = countNearbyWorkers(newLoc);
				if (numSurroundingWorkers > bestCount) {
					bestNextPositions.clear();
					bestNextPositions.add(newLoc);
					bestCount = numSurroundingWorkers;
				} else if (numSurroundingWorkers == bestCount) {
					bestNextPositions.add(newLoc);
				}
			}
		}
		int bestEmptySquareCount = -1;
		MapLocation bestLoc = null;
		// of those, pick the one that has the most free spaces around it
		for (MapLocation possibleBest : bestNextPositions) {
			int emptySquares = countEmptySquaresSurrounding(possibleBest);
			if (emptySquares > bestEmptySquareCount) {
				bestLoc = possibleBest;
				bestEmptySquareCount = emptySquares;
			}
		}
		if (bestLoc == null) {
			return false;
		} else {
			Direction dir = loc.directionTo(bestLoc);
			addBlueprint(worker, type, dir);
			return true;
		}
	}

	private static void addBlueprint(Unit worker, UnitType type, Direction dir) {
		Player.gc.blueprint(worker.id(), type, dir);
		Player.builderNav.addTarget(worker.location().mapLocation().add(dir));
		Player.builderNav.recalculateDistanceMap();
	}

	public static boolean tryAndUnload(Unit unit) {
		if (unit.structureGarrison().size() <= 0) {
			return false;
		}

		List<Direction> dirs = Arrays.asList(Direction.values());
		Collections.shuffle(dirs);

		for (Direction direction : dirs) {
			if (Player.gc.canUnload(unit.id(), direction)) {
				Player.gc.unload(unit.id(), direction);
				return true;
			}
		}
		return false;
	}

	public static boolean tryAndReplicate(Unit worker) {
		int bestCount = -1;
		Direction bestDir = null;
		MapLocation loc = worker.location().mapLocation();
		int workerId = worker.id();
		// pick the direction that maximizes the number of blueprints around it
		for (Direction dir : Direction.values()) {
			if (dir == Direction.Center)
				continue;
			if (Player.gc.canReplicate(workerId, dir)) {
				MapLocation newLoc = loc.add(dir);
				int numFactories = countNearbyConstruction(newLoc);
				if (numFactories > bestCount) {
					bestCount = numFactories;
					bestDir = dir;
				}
			}
		}
		if (bestDir == null) {
			return false;
		} else {
			Player.gc.replicate(worker.id(), bestDir);
			CensusCounts.incrementUnitCount(Worker);
			return true;
		}
	}

	public static boolean tryAndHarvest(Unit worker) {
		int workerId = worker.id();
		MapLocation workerLoc = worker.location().mapLocation();
		long maxKarb = 0;
		Direction harvestDir = null;
		MapLocation harvestLocation = null;

		for (Direction direction : Direction.values()) {
			MapLocation newLoc = workerLoc.add(direction);
			if (Player.map.onMap(newLoc) 
					&& Player.gc.canHarvest(workerId, direction)) {
				long karb = Player.gc.karboniteAt(newLoc);
				if (karb == 0) {
					Player.workerNav.removeTarget(newLoc);
				} else if(karb > maxKarb) {
					maxKarb = karb;
					harvestDir = direction;
					harvestLocation = newLoc;
				}
			}
		}
		if (harvestDir == null) return false;
		// Remove from target map if we make empty
		if (maxKarb - worker.workerHarvestAmount() <= 0) {
			Player.workerNav.removeTarget(harvestLocation);
		}
		Player.gc.harvest(workerId, harvestDir);
		return true;
	}

	public static boolean tryAndGetIntoRocket(Unit unit) {
		for (int i = 0; i < Player.friendlyUnits.size(); i++) {
			Unit potentialRocket = Player.friendlyUnits.get(i);
			if (potentialRocket.unitType() == UnitType.Rocket && potentialRocket.structureIsBuilt() == 1) {
				if (Player.gc.canLoad(potentialRocket.id(), unit.id())) {
					Player.gc.load(potentialRocket.id(), unit.id());
					return true;
				}
			}
		}
		return false;
	}

	public static boolean tryAndGetIntoFactory(Unit unit) {
		for (int i = 0; i < Player.friendlyUnits.size(); i++) {
			Unit potentialFactory = Player.friendlyUnits.get(i);
			if (potentialFactory.unitType() == UnitType.Factory && potentialFactory.structureIsBuilt() == 1
					// Make sure factory has one slot to still produce
					&& potentialFactory.structureGarrison().size() < potentialFactory.structureMaxCapacity() - 1) {
				if (Player.gc.canLoad(potentialFactory.id(), unit.id())) {
					Player.gc.load(potentialFactory.id(), unit.id());
					return true;
				}
			}
		}
		return false;
	}

	public static boolean moveAccordingToDijkstraMap(Unit unit, Navigation map) {
		if (unit.movementHeat() < Constants.MAX_MOVEMENT_HEAT) {
			Direction toMove = map.getNextDirection(unit);
			if (toMove != null) {
				Player.gc.moveRobot(unit.id(), toMove);
				return true;
			} else {
				Utils.tryAndGetIntoFactory(unit);
			}
		}
		return false;
	}

	public static boolean stuck() {
		ArrayList<Unit> units = Player.friendlyUnits;
		for (int index = 0; index < units.size(); index++) {
			if (units.get(index).unitType() != UnitType.Factory && units.get(index).unitType() != UnitType.Rocket
					&& units.get(index).movementHeat() != 0) {
				return false;
			}
		}
		return true;
	}

	public static void moveRandom(Unit unit) {
		if (unit.movementHeat() >= 10) {
			return;
		}
		ArrayList<Direction> nextMoves = new ArrayList<Direction>();
		for (Direction direction : Direction.values()) {
			if (Player.gc.canMove(unit.id(), direction)) {
				nextMoves.add(direction);
			}
		}
		if (nextMoves.size() > 0) {
			Player.gc.moveRobot(unit.id(), nextMoves.get((int) (Math.random() * nextMoves.size())));
		}
	}

	/**
	 * Returns null if can't move in any direction.
	 */
	public static Direction fleeFrom(Unit ours, Unit foe) {
		Direction away = bc
				.bcDirectionOpposite(ours.location().mapLocation().directionTo(foe.location().mapLocation()));
		if (Player.gc.canMove(ours.id(), away)) {
			return away;
		} else {
			Direction left = bc.bcDirectionRotateLeft(away);
			Direction right = bc.bcDirectionRotateRight(away);
			for (int count = 0; count < 4; count++) {
				if (Player.gc.canMove(ours.id(), left)) {
					return left;
				}
				if (Player.gc.canMove(ours.id(), right)) {
					return right;
				}
				left = bc.bcDirectionRotateLeft(left);
				right = bc.bcDirectionRotateRight(right);
			}
		}
		return null;

	}

	/**
	 * Returns the nearest enemyTeam unit that can deal damage and is within the
	 * passed in unit's line of sight.
	 */
	public static Unit getMostDangerousNearbyEnemy(Unit unit) {
		VecUnit nearbyEnemies = Player.gc.senseNearbyUnitsByTeam(unit.location().mapLocation(), unit.visionRange(),
				Player.enemyTeam);

		Unit threat = null;
		long bestThreatDistance = Long.MAX_VALUE;
		for (int i = 0; i < nearbyEnemies.size(); i++) {
			Unit foe = nearbyEnemies.get(i);
			MapLocation unitLocation = unit.location().mapLocation();

			if (foe.unitType() == UnitType.Mage || foe.unitType() == UnitType.Knight
					|| foe.unitType() == UnitType.Ranger
					|| (foe.unitType() == UnitType.Factory && foe.health() == Constants.MAX_FACTORY_HEALTH)) {
				long newThreatDistance = unitLocation.distanceSquaredTo(foe.location().mapLocation());
				if (threat == null || newThreatDistance < bestThreatDistance) {
					if (newThreatDistance < unit.attackRange()) {
						threat = foe;
					}
				}
			}
		}
		return threat;
	}

	public static Team getEnemyTeam() {
		if (Player.friendlyTeam == Team.Red) {
			return Team.Blue;
		}
		return Team.Red;
	}

	public static RobotMemory getMemory(Unit unit) {
		return Player.robotMemory.get(unit.id());
	}

	public static boolean tryAndHeal(Unit self, Unit target) {
		if (Player.gc.canHeal(self.id(), target.id()) && self.attackHeat() < Constants.MAX_ATTACK_HEAT) {
			Player.gc.heal(self.id(), target.id());
			return true;
		}
		return false;
	}

	public static int getMaxRocketCapacity() {
		if (Player.gc.round() >= 500) {
			return Constants.UPGRADED_ROCKET_CAPACITY;
		} else {
			return Constants.DEFAULT_ROCKET_CAPACITY;
		}
	}
}
