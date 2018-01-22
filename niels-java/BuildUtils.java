import bc.Unit;
import bc.UnitType;

import java.util.HashMap;

public class BuildUtils {
	/**
	 Used to keep track of structure health increasing after being marked.
	 */
	private static HashMap<Unit, Long> markedStructureHealth = new HashMap<>();

	public static void cleanupAtEndOfTurn() {
		markedStructureHealth = new HashMap<>();
	}

	private static boolean isBuilt(Unit target) {
		long health = getStructureHealth(target);
		return (target.unitType() == UnitType.Rocket && health >= Constants.MAX_ROCKET_HEALTH)
				|| (target.unitType() == UnitType.Factory && health >= Constants.MAX_FACTORY_HEALTH);
	}

	public static long getStructureHealth(Unit target) {
		// Try getting health from cache
		if (markedStructureHealth.containsKey(target)) {
			return markedStructureHealth.get(target);
		}
		return target.health();
	}

	/**
	 * Tries to build a blueprint. Returns true if it finds one and begins building.
	 */
	public static boolean tryToBuildBlueprints(Unit unit) {
		for (Unit blueprint : Player.blueprints) {
			if (Player.gc.canBuild(unit.id(), blueprint.id()) && !isBuilt(blueprint)) {
				Player.gc.build(unit.id(), blueprint.id());

				// Update health in cache
				if (markedStructureHealth.containsKey(blueprint)) {
					markedStructureHealth.put(blueprint, markedStructureHealth.get(blueprint) + unit.workerBuildHealth());
				} else {
					markedStructureHealth.put(blueprint, blueprint.health() + unit.workerBuildHealth());
				}

				if (isBuilt(blueprint)) {
					Player.builderNav.removeTarget(unit.location().mapLocation());
					Player.builderNav.recalculateDistanceMap();
				}
				return true;
			}
		}
		return false;
	}
}
