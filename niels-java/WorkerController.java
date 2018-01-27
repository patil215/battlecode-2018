import bc.*;

import static bc.UnitType.Worker;

public class WorkerController {

	public enum Mode {
		HARVESTER, BUILD_FACTORIES, BUILD_ROCKETS, IDLE
	}
	public static int MAX_NUMBER_WORKERS;

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
					if (toMove == null) { 
						// nothing to harvest, change to builder
						RobotMemory workerMemory = Player.robotMemory.get(unit.id());
						workerMemory.workerMode = WorkerController.Mode.BUILD_FACTORIES;
						CensusCounts.decrementWorkerModeCount(WorkerController.Mode.HARVESTER);
						CensusCounts.incrementWorkerModeCount(WorkerController.Mode.BUILD_FACTORIES);
					} else if (unit.movementHeat() < Constants.MAX_MOVEMENT_HEAT) {
						Player.gc.moveRobot(unit.id(), toMove);
					}
				}
				// Move according to Dijkstra map
				break;
			}

			case BUILD_FACTORIES: {
				movePossiblyUsingBuilderMap(unit);
				// Try to build factory
				if (Player.gc.karbonite() >= Constants.FACTORY_COST
						&& ((!Player.hasMadeBluePrintThisTurn && Player.blueprints.size() == 0)
								|| Player.greedyEconMode)) {
					Player.hasMadeBluePrintThisTurn = Utils.tryAndBuild(unit, UnitType.Factory);
				}
				break;
			}

			case BUILD_ROCKETS: {
				movePossiblyUsingBuilderMap(unit);
				// Try to build rocket
				if (Player.gc.karbonite() >= Constants.ROCKET_COST) {
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

			// Harvest any Karbonite in adjacent squares
			// TODO: maybe we want to harvest first
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
