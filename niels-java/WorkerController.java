import bc.*;

import static bc.UnitType.Worker;

public class WorkerController {

	public enum Mode {
		HARVESTER, BUILD_FACTORIES, BUILD_ROCKETS, IDLE
	}

	private static final int ROCKET_BUILD_KARB_THRESHOLD = 80;
	private static final int FACTORY_BUILD_KARB_THRESHOLD = 100;
	private static final int MAX_NUMBER_WORKERS = 6;

	static void moveWorker(Unit unit) {
		if (!unit.location().isInGarrison()) {
			// TODO maybe prioritize fleeing over building in certain cases

			// Try to replicate
			if (CensusCounts.getUnitCount(Worker) < MAX_NUMBER_WORKERS) {
				Utils.tryAndReplicate(unit);
			}

			boolean buildingBlueprint = BuildUtils.tryToBuildBlueprints(unit);
			if (buildingBlueprint) {
				return; // Nothing else to do
			}

			// Try to flee from enemies
			Unit nearbyEnemy = Utils.getMostDangerousNearbyEnemy(unit);
			if (nearbyEnemy != null) {
				Utils.fleeFrom(unit, nearbyEnemy);
			}

			switch (Utils.getMemory(unit).workerMode) {
			case HARVESTER: {
				MapLocation workerLoc = unit.location().mapLocation();
				long karbAtLoc = Player.gc.karboniteAt(workerLoc);
				if (karbAtLoc == 0) {
					Player.workerNav.removeTarget(workerLoc);
					Direction toMove = Player.workerNav.getNextDirection(unit);
					if (toMove != null && unit.movementHeat() < Constants.MAX_MOVEMENT_HEAT) {
						Player.gc.moveRobot(unit.id(), toMove);
					}
				}
				// Move according to Dijkstra map
				break;
			}

			case BUILD_FACTORIES: {
				movePossiblyUsingBuilderMap(unit);
				// Try to build factory
				if (Player.gc.karbonite() > FACTORY_BUILD_KARB_THRESHOLD && !Player.hasMadeBluePrintThisTurn
						&& Player.blueprints.size() == 0) {
					Player.hasMadeBluePrintThisTurn = Utils.tryAndBuild(unit, UnitType.Factory);
				}
				System.out.println(Player.gc.round() + ": Player has built blueprint this turn: " + Player.hasMadeBluePrintThisTurn
						+ " Blueprints: " + Player.blueprints);
				break;
			}

			case BUILD_ROCKETS: {
				movePossiblyUsingBuilderMap(unit);
				// Try to build rocket
				if (Player.gc.karbonite() > ROCKET_BUILD_KARB_THRESHOLD) {
					System.out.println("Building rocket");
					Utils.tryAndBuild(unit, UnitType.Rocket);
				}
				break;
			}

			case IDLE: {
				// Move randomly and do nothing else
				Utils.moveRandom(unit);
				break;
			}
			}

			// Harvest any Karbonite in a square we're on TODO: maybe we want to harvest
			// first
			Utils.tryAndHarvest(unit);
		}

	}

	private static void movePossiblyUsingBuilderMap(Unit unit) {
		Direction direction = Player.builderNav.getNextDirection(unit);
		if (direction != null) {
			if (unit.movementHeat() < Constants.MAX_MOVEMENT_HEAT) {
				Player.gc.moveRobot(unit.id(), direction);
			}
		} else {
			Utils.moveRandom(unit);
		}
	}
}
