import bc.*;

import java.util.*;
import java.util.function.*;

public class Utils {
	static int countSurrounding(MapLocation loc, 
			Function<MapLocation, Boolean> f) {
		int count = 0;
		for (Direction dir : Direction.values()) {
			if(dir == Direction.Center) continue;
			MapLocation newLoc = loc.add(dir);
			if(Player.map.onMap(newLoc) && f.apply(newLoc)) { 
				count++; 
			}
		}
		return count;
	}

	static int countNearbyWorkers(MapLocation loc) {
		VecUnit workers = 
			Player.gc.senseNearbyUnitsByType(loc, 1L, UnitType.Worker);
		int numOurTeam = 0;
		for(int i = 0; i < workers.size(); i++) {
			if(workers.get(i).team() == Player.friendlyTeam) {
				numOurTeam++;
			}
		}
		return numOurTeam;
	}

	static int countNearbyConstruction(MapLocation loc) {
		VecUnit units = 
			Player.gc.senseNearbyUnitsByType(loc, 1L, UnitType.Factory);
		int numOurTeam = 0;
		for(int i = 0; i < units.size(); i++) {
			Unit factory = units.get(i);
			if(factory.team() == Player.friendlyTeam
					&& factory.structureIsBuilt() == 0) {
				numOurTeam++;
			}
		}
		return numOurTeam;
	}

	public static int countEmptySquaresSurrounding(MapLocation cur) {
		return countSurrounding(cur, loc -> 
				Player.map.onMap(loc)
				&& Player.map.isPassableTerrainAt(loc) == 1 
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
			if(Player.gc.canBlueprint(workerId, type, direction)) { 
				int numSurroundingWorkers = countNearbyWorkers(newLoc);
				if(numSurroundingWorkers > bestCount) {
					bestNextPositions.clear();
					bestNextPositions.add(newLoc);
					bestCount = numSurroundingWorkers;
				} else if(numSurroundingWorkers == bestCount) {
					bestNextPositions.add(newLoc);
				}
			}
		}
		int bestEmptySquareCount = -1;
		MapLocation bestLoc = null;
		// of those, pick the one that has the most free spaces around it
		for (MapLocation possibleBest : bestNextPositions) {
			int emptySquares = countEmptySquaresSurrounding(possibleBest); 
			if(emptySquares > bestEmptySquareCount) {
				bestLoc = possibleBest;
				bestEmptySquareCount = emptySquares;
			}
		}
		if(bestLoc == null) {
			return false;
		} else {
			Direction dir = loc.directionTo(bestLoc);
			Player.gc.blueprint(workerId, type, dir);
			return true;
		}
	}

	public static boolean tryAndUnload(Unit unit) {
		if (unit.structureGarrison().size() <= 0) {
			return false;
		}

		for (Direction direction : Direction.values()) {
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
			if (dir == Direction.Center) continue;
			if(Player.gc.canReplicate(workerId, dir)) { 
				MapLocation newLoc = loc.add(dir);
				int numFactories = countNearbyConstruction(newLoc); 
				if(numFactories > bestCount) {
					bestCount = numFactories;
					bestDir = dir;
				}
			}
		}
		if(bestDir == null) {
			return false;
		} else {
			Player.gc.replicate(worker.id(), bestDir);
			return true;
		}
	}

	public static boolean tryAndHarvest(Unit worker) {
		int workerId = worker.id();
		if (worker.abilityHeat() < Constants.MAX_ABILITY_HEAT) {
			for (Direction direction : Direction.values()) {
				if (Player.gc.canHarvest(workerId, direction)) {
					Player.gc.harvest(workerId, direction);
					return true;
				}
			}
		}
		return false;
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

	public static boolean moveAccordingToDjikstraMap(Unit unit, Navigation map) {
		if (unit.movementHeat() < Constants.MAX_MOVEMENT_HEAT) {
			Direction toMove = map.getNextDirection(unit);
			if (toMove != null) {
				Player.gc.moveRobot(unit.id(), toMove);
				return true;
			}
		}
		return false;
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
	 * Returns the nearest enemyTeam unit that can deal damage and is within the passed in unit's line of sight.
	 */
	public static Unit getMostDangerousNearbyEnemy(Unit unit) {
		VecUnit nearbyEnemies =
				Player.gc.senseNearbyUnitsByTeam(unit.location().mapLocation(), unit.visionRange(), Player.enemyTeam);

		Unit threat = null;
		long bestThreatDistance = Long.MAX_VALUE;
		for (int i = 0; i < nearbyEnemies.size(); i++) {
			Unit foe = nearbyEnemies.get(i);
			MapLocation unitLocation = unit.location().mapLocation();

			if (foe.unitType() == UnitType.Mage || foe.unitType() == UnitType.Knight || foe.unitType() == UnitType.Ranger) {
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
		if (Player.gc.canHeal(self.id(), target.id())) {
			Player.gc.heal(self.id(), target.id());
			return true;
		}
		return false;
	}
}
