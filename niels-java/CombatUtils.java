import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import bc.Unit;
import bc.UnitType;

public class CombatUtils {
	/**
	Used to keep track of enemyTeam health decreasing after being marked.
	This is necessary because if other units on our team "intend" to shoot at a particular unit on the enemyTeam team,
	before the turn has ended, that means the effective health of that unit on the enemyTeam team is lower.
	Accordingly, these changes should be cached and taken into account.
	 */
	private static HashMap<Unit, Long> markedEnemyHealth = new HashMap<>();
	private static HashMap<Integer, Integer> rangerTargets = new HashMap<>();

	static Navigation microNav;

	public static void initAtStartOfTurn() {
		// Commented because we aren't using knights
		/*
		VecUnit foes = Player.gc.senseNearbyUnitsByTeam(new MapLocation(Player.gc.planet(), 1,1), Long.MAX_VALUE, Player.enemyTeam);
		Set<Point> useless = new HashSet<>();
		for (int i = 0; i < foes.size(); i++) {
			MapLocation location = foes.get(i).location().mapLocation();
			useless.add(new Point(location.getX(), location.getY()));
		}
		microNav = new Navigation(Player.map, useless, 10);
		*/
	}

	public static void cleanupAtEndOfTurn() {
		markedEnemyHealth = new HashMap<>();
	}

	public static long getTargetHealth(Unit target) {
		// Try getting health from cache
		if (markedEnemyHealth.containsKey(target)) {
			return markedEnemyHealth.get(target);
		}
		return target.health();
	}

	private static long subScore(Unit target) {
		long targetHealth = getTargetHealth(target);
		if (targetHealth == 0) {
			return Long.MAX_VALUE; // Make sure we don't "overkill" units
		}

		switch (target.unitType()) {
			case Knight:
				return targetHealth * 2;
			case Ranger:
				return targetHealth * 2;
			case Healer:
				return targetHealth * 3;
			case Factory:
				return targetHealth * 4;
			case Worker:
				return targetHealth * 5;
			case Rocket:
				return targetHealth * 6;
			default:
				return targetHealth;
		}
	}

	/**
	 * Returns a score for a target. If the target is not attackable, returns Long.MAX_VALUE.
	 */
	public static long targetScore(Unit attacker, Unit target) {
		if (target == null || !Player.gc.canAttack(attacker.id(), target.id())) {
			return Long.MAX_VALUE;
		}

		return subScore(target);
	}

	public static void attack(Unit attacker, Unit target) {
		Player.gc.attack(attacker.id(), target.id());

		// Update health in cache
		if (markedEnemyHealth.containsKey(target)) {
			long defense = 0;
			if (target.unitType() == UnitType.Knight) {
				defense = target.knightDefense();
			}

			markedEnemyHealth.put(target, markedEnemyHealth.get(target) - attacker.damage() + defense);
		} else {
			markedEnemyHealth.put(target, target.health() - attacker.damage());
		}
	}

	public static long assignRangerTargets() {
		Collections.sort(Player.enemyUnits, (a, b) -> Long.compare(subScore(a), subScore(b)));


		for (Unit foe : Player.enemyUnits) {

		}

		for (Unit friendly : Player.friendlyUnits) {
			if (friendly.unitType() != UnitType.Ranger) {
				continue;
			}


		}
		return 0;
	}
}
