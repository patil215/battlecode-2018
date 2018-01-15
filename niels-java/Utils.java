import java.util.ArrayList;

import bc.*;

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

	public static boolean tryAndUnload(int structId) {
		for (Direction direction : Direction.values()) {
			if (Player.gc.canUnload(structId, direction)) {
				Player.gc.unload(structId, direction);
				return true;
			}
		}
		return false;
	}

	public static boolean tryAndReplicate(int workerId) {
		for (Direction direction : Direction.values()) {
			if (Player.gc.canReplicate(workerId, direction)) {
				Player.gc.replicate(workerId, direction);
				return true;
			}
		}
		return false;
	}

	public static boolean tryAndHarvest(int workerId) {
		for (Direction direction : Direction.values()) {
			if (Player.gc.canHarvest(workerId, direction)) {
				Player.gc.harvest(workerId, direction);
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
}
