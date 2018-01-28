import bc.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static bc.UnitType.Worker;

public class Utils {
	/**
	 * Includes diagonals (1 vertical, 1 horiz = 1^2 + 1^2 = 2, this is inclusive)
	 * Will not include any other squares.
	 */
	private static final long DISTANCE_SQUARED_FOR_ONLY_SURROUNDINGS = 2L;

	static int countSurrounding(MapLocation loc, Function<MapLocation, Boolean> f) {
		int count = 0;
		for (Direction dir : Direction.values()) {
			if (dir == Direction.Center) {
				continue;
			}
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

	static int countNearbyUnderConstruction(MapLocation loc) {
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

	private static boolean locInList(ArrayList<MapLocation> bestFactoryLocations, MapLocation location) {
		for (MapLocation loc : bestFactoryLocations) {
			if (loc.getX() == location.getX() && loc.getY() == location.getY()) {
				return true;
			}
		}
		return false;
	}

	public static boolean tryAndBuild(Unit worker, UnitType type) {
		ArrayList<MapLocation> bestFactoryLocations = BuildUtils.getBestFactoryLocations();
		for (Direction dir : Direction.values()) {
			MapLocation proposedLocation = worker.location().mapLocation().add(dir);
			if (locInList(bestFactoryLocations, proposedLocation)
					&& Player.gc.canBlueprint(worker.id(), type, dir)) {
				addBlueprint(worker, type, dir);
				return true;
			}
		}
		return false;
	}

	private static void addBlueprint(Unit worker, UnitType type, Direction dir) {
		MapLocation workerLoc = worker.location().mapLocation();
		MapLocation blueprintLoc = workerLoc.add(dir);

		// Assign number of knights if first factory
		if (Constants.BEGINNING_KNIGHTS == -1) { 
			int distance;
			if (Player.seenEnemies) {
				distance = Player.armyNav.getDijkstraMapValue(blueprintLoc);
			} else {
				Navigation enemyNav = new Navigation(Player.map, Player.getInitialEnemyUnitLocations()); 
				distance = enemyNav.getDijkstraMapValue(blueprintLoc);
			}
			// Black magic 
			Constants.BEGINNING_KNIGHTS = Math.max(0, 5 - (distance / 3));
		}

		Player.gc.blueprint(worker.id(), type, dir);
		Player.builderNav.addTarget(workerLoc.add(dir));
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
		int workerId = worker.id();
		MapLocation workerLoc = worker.location().mapLocation();

		int bestCount = -1;
		Direction bestDir = null;
		// Pick direction maximizing number of blueprints around it
		for (Direction dir : Direction.values()) {
			if (dir == Direction.Center) {
				continue;
			}
			if (Player.gc.canReplicate(workerId, dir)) {
				MapLocation newLoc = workerLoc.add(dir);
				int numFactories = countNearbyUnderConstruction(newLoc);
				if (numFactories > bestCount) {
					bestCount = numFactories;
					bestDir = dir;
				}
			}
		}

		if (bestDir == null) {
			return false;
		}

		Direction karbDir = Player.workerNav.getNextDirection(worker);
		if (bestCount == 0 && karbDir != null) {
			Player.gc.replicate(worker.id(), karbDir);
		} else { 
			Player.gc.replicate(worker.id(), bestDir);
		}

		CensusCounts.incrementUnitCount(Worker);
		return true;
	}

	public static boolean tryAndHarvest(Unit worker) {
		int workerId = worker.id();
		MapLocation workerLoc = worker.location().mapLocation();

		long maxKarb = 0;
		Direction harvestDir = null;
		MapLocation harvestLocation = null;

		for (Direction direction : Direction.values()) {
			MapLocation newLoc = workerLoc.add(direction);
			if (Player.map.onMap(newLoc)) {
				long karb = Player.gc.karboniteAt(newLoc);
				if (karb > maxKarb && Player.gc.canHarvest(workerId, direction)) {
					maxKarb = karb;
					harvestDir = direction;
					harvestLocation = newLoc;
				} else if (karb == 0) {
					Player.workerNav.removeTarget(newLoc);
				}
			}
		}

		if (harvestDir == null) {
			return false;
		}

		// Remove from target map if we make empty
		if (maxKarb - worker.workerHarvestAmount() <= 0) {
			Player.workerNav.removeTarget(harvestLocation);
		}

		Player.gc.harvest(workerId, harvestDir);
		return true;
	}

	public static boolean tryAndGetIntoRocket(Unit unit) {
		for (Unit potentialRocket : Player.friendlyUnits) {
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
		for (Unit potentialFactory : Player.friendlyUnits) {
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

	public static boolean tryToMoveAccordingToDijkstraMap(Unit unit, Navigation map, boolean getIntoFactories) {
		if (unit.movementHeat() >= 10) {
			return false;
		}

		Direction toMove = map.getNextDirection(unit);
		if (toMove != null) {
			Player.gc.moveRobot(unit.id(), toMove);
			return true;
		} else if (getIntoFactories) {
			return Utils.tryAndGetIntoFactory(unit);
		}

		return false;
	}

	public static boolean allUnitsStuck() {
		for (Unit unit : Player.friendlyUnits) {
			if (unit.unitType() != UnitType.Factory && unit.unitType() != UnitType.Rocket && unit.movementHeat() != 0) {
				return false;
			}
		}
		return true;
	}

	public static boolean moveRandom(Unit unit) {
		if (unit.movementHeat() >= Constants.MAX_MOVEMENT_HEAT) {
			return false;
		}

		ArrayList<Direction> nextMoves = new ArrayList<Direction>();
		for (Direction direction : Direction.values()) {
			if (Player.gc.canMove(unit.id(), direction)) {
				nextMoves.add(direction);
			}
		}

		if (nextMoves.size() > 0) {
			Player.gc.moveRobot(unit.id(), nextMoves.get((int) (Math.random() * nextMoves.size())));
			return true;
		}

		return false;
	}

	/**
	 * Returns null if can't move in any direction.
	 */
	public static Direction fleeFrom(Unit self, Unit foe) {
		Direction away = bc
				.bcDirectionOpposite(self.location().mapLocation().directionTo(foe.location().mapLocation()));
		if (Player.gc.canMove(self.id(), away)) {
			return away;
		} else {
			Direction left = bc.bcDirectionRotateLeft(away);
			Direction right = bc.bcDirectionRotateRight(away);
			for (int count = 0; count < 4; count++) {
				if (Player.gc.canMove(self.id(), left)) {
					return left;
				}
				if (Player.gc.canMove(self.id(), right)) {
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
					|| (foe.unitType() == UnitType.Factory
					&& CombatUtils.getTargetHealth(foe) == Constants.MAX_FACTORY_HEALTH)) {
				long newThreatDistance = unitLocation.distanceSquaredTo(foe.location().mapLocation());
				if (newThreatDistance < bestThreatDistance && newThreatDistance < unit.attackRange()) {
					threat = foe;
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
		if (Player.gc.round() >= Constants.ROCKET_CAPACITY_UPGRADE_ROUND) {
			return Constants.UPGRADED_ROCKET_CAPACITY;
		} else {
			return Constants.DEFAULT_ROCKET_CAPACITY;
		}
	}

	public static long getDistanceToClosestEnemySpawn(MapLocation mapLocation) {
		long minDist = Long.MAX_VALUE;
		for(Unit spawnUnit : Player.initialUnits) {
			if(spawnUnit.team() == Player.friendlyTeam) {
				continue;
			}
			minDist = Math.min(minDist, mapLocation.distanceSquaredTo(spawnUnit.location().mapLocation()));
		}
		return minDist;
	}
}
