import bc.*;

import java.util.ArrayList;

public class Utils {

	public static boolean tryAndBuild(int workerId, UnitType type) {
		for (Direction direction : Direction.values()) {
			if (Player.gc.canBlueprint(workerId, type, direction)) {
				Player.gc.blueprint(workerId, type, direction);
				return true;
			}
		}
		return false;
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
		for (Direction direction : Direction.values()) {
			if (Player.gc.canReplicate(worker.id(), direction)) {
				Player.gc.replicate(worker.id(), direction);
				return true;
			}
		}
		return false;
	}

	public static boolean tryAndHarvest(Unit worker) {
		int workerId = worker.id();
		if (worker.abilityHeat() < Constants.ABILITY_HEAT) {
			for (Direction direction : Direction.values()) {
				if (Player.gc.canHarvest(workerId, direction)) {
					Player.gc.harvest(workerId, direction);
					return true;
				}
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
	 * Returns the nearest enemy unit that can deal damage and is within the passed in unit's line of sight.
	 */
	public static Unit getMostDangerousNearbyEnemy(Unit unit) {
		VecUnit nearbyEnemies =
				Player.gc.senseNearbyUnitsByTeam(unit.location().mapLocation(), unit.visionRange(), Player.enemy);

		Unit threat = null;
		for (int i = 0; i < nearbyEnemies.size(); i++) {
			Unit foe = nearbyEnemies.get(i);
			MapLocation unitLocation = unit.location().mapLocation();
			if ((unitLocation.distanceSquaredTo(foe.location().mapLocation()) < unit.attackRange()
					&& (foe.unitType() == UnitType.Mage || foe.unitType() == UnitType.Knight
					|| foe.unitType() == UnitType.Ranger))
					&& (threat == null || unitLocation.distanceSquaredTo(foe.location().mapLocation()) < unitLocation
					.distanceSquaredTo(threat.location().mapLocation()))) {
				threat = foe;
			}
		}
		return threat;
	}

	public static Planet getLocationPlanet(Location loc) {
		Planet planet = null;
		for (Planet p : Planet.values()) {
			if (loc.isOnPlanet(p)) {
				planet = p;
			}
		}
		return planet;
	}

	public static Team getEnemyTeam() {
		if (Player.gc.team() == Team.Red) {
			return Team.Blue;
		}
		return Team.Red;
	}

	public static RobotMemory getMemory(Unit unit) {
		return Player.robotMemory.get(unit.id());
	}
}
