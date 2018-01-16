import bc.*;

public class WorkerController {

	private static final int ROCKET_BUILD_KARB_THRESHOLD = 100;
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
		if (CensusCounts.workerCount < MAX_NUMBER_WORKERS) {
			Utils.tryAndReplicate(unit);
		}

		switch (Utils.getMemory(unit).workerMode) {
			case HARVESTER: {
				MapLocation workerLoc = unit.location().mapLocation();
				long karbAtLoc = Player.gc.karboniteAt(workerLoc);
				if(karbAtLoc == 0) {
					Player.workerNav.removeTarget(workerLoc);
					Direction toMove = Player.workerNav.getNextDirection(workerLoc);
					if (toMove != null && unit.movementHeat() < Constants.MOVEMENT_HEAT) {
						Player.gc.moveRobot(unit.id(), toMove);
					}
				}
				// Move according to Dijkstra map
				break;
			}

			case BUILD_FACTORIES: {
				Utils.moveRandom(unit);
				// Try to build factory
				if (Player.gc.karbonite() > FACTORY_BUILD_KARB_THRESHOLD) {
					Utils.tryAndBuild(unit.id(), UnitType.Factory);
				}
				break;
			}

			case BUILD_ROCKETS: {
				// TODO build rockets near factories
				Utils.moveRandom(unit);
				// Try to build rocket
				if (Player.gc.karbonite() > ROCKET_BUILD_KARB_THRESHOLD) {
					System.out.println("Building rocket");
					Utils.tryAndBuild(unit.id(), UnitType.Rocket);
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
