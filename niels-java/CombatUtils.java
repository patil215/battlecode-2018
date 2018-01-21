import bc.Unit;
import bc.UnitType;

import java.util.HashMap;

public class CombatUtils {
	/**
	Used to keep track of enemy health decreasing after being marked.
	This is necessary because if other units on our team "intend" to shoot at a particular unit on the enemy team,
	before the turn has ended, that means the effective health of that unit on the enemy team is lower.
	Accordingly, these changes should be cached and taken into account.
	 */
	private static HashMap<Unit, Long> markedEnemyHealth = new HashMap<>();

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

	public static long targetScoreRanger(Unit attacker, Unit target) {
		if (target == null || !Player.gc.canAttack(attacker.id(), target.id())) {
			return Long.MAX_VALUE;
		}

		long targetHealth = getTargetHealth(target);
		if (targetHealth == 0) {
			return Long.MAX_VALUE; // Make sure we don't "overkill" units
		}

		if (target.unitType() == UnitType.Worker) {
			return targetHealth * 3;
		} else if (target.unitType() == UnitType.Healer) {
			return targetHealth / 2;
		} else if (target.unitType() == UnitType.Factory) {
			return targetHealth * 2;
		} else if (target.unitType() == UnitType.Rocket) {
			return targetHealth * 3;
		}

		return targetHealth;
	}

	public static void attack(Unit attacker, Unit target) {
		Player.gc.attack(attacker.id(), target.id());

		// Update health in cache
		if (markedEnemyHealth.containsKey(target)) {
			markedEnemyHealth.put(target, markedEnemyHealth.get(target) - attacker.damage());
		} else {
			markedEnemyHealth.put(target, target.health() - attacker.damage());
		}
	}
}