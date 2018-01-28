import bc.*;

import static bc.UnitType.Worker;

public class WorkerController {

	public enum Mode {
		BUILD_FACTORIES, BUILD_ROCKETS
	}

	public static int MAX_NUMBER_WORKERS;

	static void moveWorker(Unit unit) {
		if (!unit.location().isInGarrison()) {

			// 1. Try to replicate
			if (CensusCounts.getUnitCount(Worker) < MAX_NUMBER_WORKERS) {
				Utils.tryAndReplicate(unit);
			}

			// 2. Try to finish building a building
			boolean buildingBlueprint = BuildUtils.tryToBuildBlueprints(unit);

			// 3. Try to flee from enemies
			Unit nearbyEnemy = Utils.getMostDangerousNearbyEnemy(unit);
			if (nearbyEnemy != null) {
				Utils.fleeFrom(unit, nearbyEnemy);
			}

			// 4. Try to build a building or rocket (if Karbonite is enough)
			switch (Utils.getMemory(unit).workerMode) {
				case BUILD_FACTORIES: {
					// Try to build factory
					if (Player.gc.karbonite() >= Constants.FACTORY_COST) {
						Utils.tryAndBuild(unit, UnitType.Factory);
					}
					break;
				}

				case BUILD_ROCKETS: {
					// Try to build rocket
					if (Player.gc.karbonite() >= Constants.ROCKET_COST) {
						Utils.tryAndBuild(unit, UnitType.Rocket);
					}
					break;
				}
			}

			// 5. Try to move using builder map
			boolean builderMoveResult = Utils.tryToMoveAccordingToDijkstraMap(unit, Player.builderNav, false);

			// 6. Try to move using Karbonite map
			boolean shouldHarvest = Player.robotMemory.get(unit.id()).searchForKarbonite;
			boolean harvesterMoveResult = false;
			if (!builderMoveResult && shouldHarvest) {
				harvesterMoveResult = Utils.tryToMoveAccordingToDijkstraMap(unit, Player.workerNav, false);
			}

			// 7. Try to move towards built factories
			boolean factoryMoveResult = false;
			int dMapValue = Player.completedFactoryNav.getDijkstraMapValue(unit.location().mapLocation());
			if (!harvesterMoveResult 
					&& !builderMoveResult 
					&& dMapValue >= Constants.SAFE_FACTORY_DISTANCE) {
				factoryMoveResult = Utils.tryToMoveAccordingToDijkstraMap(unit, Player.completedFactoryNav, false);
			}

			// 8. Try to move randomly
			boolean randomMoveResult = false;
			if(!factoryMoveResult && !harvesterMoveResult && !builderMoveResult) {
				randomMoveResult = Utils.moveRandom(unit);
			}

			// 9. Harvest any Karbonite in adjacent squares
			Utils.tryAndHarvest(unit);
		}

	}
}
