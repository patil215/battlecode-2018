import bc.*;

import static bc.UnitType.Worker;

public class WorkerController {

	public enum Mode {
		BUILD_FACTORIES, BUILD_ROCKETS, IDLE
	}
	public static int MAX_NUMBER_WORKERS;

	static void moveWorker(Unit unit) {
		if (!unit.location().isInGarrison()) {

			// 0. Try to harvest
			Utils.tryAndHarvest(unit);

			// 1. Try to replicate
			if (CensusCounts.getUnitCount(Worker) < MAX_NUMBER_WORKERS) {
				Utils.tryAndReplicate(unit);
			}

			// 2. Try to finish building a building
			boolean buildingBlueprint = BuildUtils.tryToBuildBlueprints(unit);
			if (buildingBlueprint) {
				return; // Nothing else to do
			}

			// 3. Try to flee from enemies
			Unit nearbyEnemy = Utils.getMostDangerousNearbyEnemy(unit);
			if (nearbyEnemy != null) {
				Utils.fleeFrom(unit, nearbyEnemy);
			}

			// 4. Try to build a building or rocket (if Karbonite exists)
			switch (Utils.getMemory(unit).workerMode) {
				case BUILD_FACTORIES: {
					// Try to build factory
					if (Player.gc.karbonite() >= Constants.FACTORY_COST
							&& ((!Player.hasMadeBluePrintThisTurn && Player.blueprints.size() == 0)
							|| Player.greedyEconMode)) {
						Player.hasMadeBluePrintThisTurn = Utils.tryAndBuild(unit, UnitType.Factory);
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
			boolean builderMoveResult = Utils.tryToMoveAccordingToDijkstraMap(unit, Player.builderNav);

			// 6. Try to move using Karbonite map
			boolean harvesterMoveResult = false;
			if (!builderMoveResult) {
				System.out.println("harvestermove");
				harvesterMoveResult = Utils.tryToMoveAccordingToDijkstraMap(unit, Player.workerNav);
			}

			// 7. Try to move randomly
			boolean randomMoveResult = false;
			if (!harvesterMoveResult && !builderMoveResult) {
				System.out.println("randommmove");
				randomMoveResult = Utils.moveRandom(unit);
			}
			System.out.println(randomMoveResult);

			// 8. Harvest any Karbonite in adjacent squares
			Utils.tryAndHarvest(unit);
		}

	}

	private static void movePossiblyUsingBuilderMap(Unit unit) {
		Direction direction = Player.builderNav.getNextDirection(unit);
		if (direction != null) {
			if (unit.movementHeat() < Constants.MAX_MOVEMENT_HEAT) {
				Player.gc.moveRobot(unit.id(), direction);
			}
		}
	}

	private static void movePossiblyUsingKarboniteMap(Unit unit) {
		Direction direction = Player.workerNav.getNextDirection(unit);
		if (direction != null) {
			if (unit.movementHeat() < Constants.MAX_MOVEMENT_HEAT) {
				Player.gc.moveRobot(unit.id(), direction);
			}
		}
	}
}
