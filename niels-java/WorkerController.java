import bc.Direction;
import bc.Unit;
import bc.UnitType;

public class WorkerController {

	private static final int FACTORY_BUILD_KARB_THRESHOLD = 130;
	private static final int MAX_NUMBER_WORKERS = 5;

	static void moveWorker(Unit unit) {

		// TODO maybe prioritize fleeing over building in certain cases
		boolean buildingBlueprint = tryToBuildBlueprints(unit);
		if (buildingBlueprint) {
			return; // Nothing else to do
		}

		// Try to flee from enemies
		Unit nearbyEnemy = Utils.getMostDangerousNearbyEnemy(unit);
		if (nearbyEnemy != null) {
			Utils.fleeFrom(unit, nearbyEnemy);
		}

		// Try to replicate
		if (Player.workerCount < MAX_NUMBER_WORKERS) {
			Utils.tryAndReplicate(unit);
		}

		switch (Utils.getMemory(unit).workerRole) {
			case HARVESTER: {
				// Move according to Dijkstra map
				Direction toMove = Player.workerNav.getNextDirection((unit.location().mapLocation()));
				if (toMove != null && unit.movementHeat() < Constants.MOVEMENT_HEAT) {
					Player.gc.moveRobot(unit.id(), toMove);
				}
				break;
			}

			case BUILDER: {
				Utils.moveRandom(unit);
				// Try to build factory
				if (Player.gc.karbonite() > FACTORY_BUILD_KARB_THRESHOLD) {
					Utils.tryAndBuild(unit.id(), UnitType.Factory);
				}
				break;
			}
		}

		// Harvest any Karbonite in a square we're on TODO: maybe we want to harvest first
		Utils.tryAndHarvest(unit);

	}

	/**
	 * Tries to build a blueprint. Returns true if it finds one and begins building.
	 */
	private static boolean tryToBuildBlueprints(Unit unit) {
		for (Unit blueprint : Player.blueprints) {
			if (Player.gc.canBuild(unit.id(), blueprint.id())) {
				Player.gc.build(unit.id(), blueprint.id());
				return true;
			}
		}
		return false;
	}

}
