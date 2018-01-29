import bc.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BuildUtils {
	/**
	Used to keep track of structure health increasing after being marked.
	*/
	private static HashMap<Unit, Long> markedStructureHealth = new HashMap<>();
	private static ArrayList<MapLocation> bestFactoryLocations = new ArrayList<>();

	public static void cleanupAtEndOfTurn() {
		markedStructureHealth = new HashMap<>();
	}

	public static boolean isBuilt(Unit target) {
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

	private static void clearDestroyedFactories() {
		// avoid ConcurrentModificationException
		List<Point> oldTargets = new ArrayList<>(Player.completedFactoryNav.getTargets());
		for(Point target : oldTargets) {
			MapLocation targetLoc = new MapLocation(Player.planet, target.x, target.y);
			if(Player.gc.hasUnitAtLocation(targetLoc)) {
				Unit building = Player.gc.senseUnitAtLocation(targetLoc);
				if (building.team() != Player.friendlyTeam
						|| (building.unitType() != UnitType.Factory 
						&& building.unitType() != UnitType.Rocket)) {
					Player.completedFactoryNav.removeTarget(targetLoc);
				}
			} else { 
				Player.completedFactoryNav.removeTarget(targetLoc);
			}
		}
	}

	/**
	 * Tries to build a blueprint. Returns -1 if no buildable blueprint was found, 0 if we successfully contributed to
	 * building a blueprint, and 1 if we successfully finished building a blueprint.
	 */
	public static int tryToBuildBlueprints(Unit unit) {
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
					Player.robotMemory.get(unit.id()).searchForKarbonite = false;
					MapLocation blueprintLoc = blueprint.location().mapLocation();

					clearDestroyedFactories();
					Player.completedFactoryNav.addTarget(blueprintLoc);
					Player.completedFactoryNav.recalculateDistanceMap();

					Player.builderNav.removeTarget(blueprintLoc);
					Player.builderNav.recalculateDistanceMap();
					return 1;
				} else {
					return 0;
				}
			}
		}
		return -1;
	}

	public static void findBestFactoryBuildLocations() {
		bestFactoryLocations = new ArrayList<>();

		// First consider only workers that have no enemies in their sight range
		ArrayList<Unit> workersWithNoEnemies = new ArrayList<>();
		ArrayList<Unit> allWorkers = new ArrayList<>();
		for (Unit unit : Player.friendlyUnits) {
			if (unit.unitType() != UnitType.Worker) {
				continue;
			}

			if (unit.location().isInGarrison()) {
				continue;
			}

			VecUnit nearbyEnemies = NearbyUnitsCache.getEnemiesInVisionRange(unit);
			int numberMilitaryUnits = 0;
			for (int i = 0; i < nearbyEnemies.size(); i++) {
				Unit foe = nearbyEnemies.get(i);
				if (foe.unitType() == UnitType.Mage || foe.unitType() == UnitType.Knight
						|| foe.unitType() == UnitType.Ranger || foe.unitType() == UnitType.Factory) {
					numberMilitaryUnits++;
				}
			}
			if (numberMilitaryUnits == 0) {
				workersWithNoEnemies.add(unit);
			}
			allWorkers.add(unit);
		}

		boolean wasAbleToFindValidWorker = addToBestFactoryLocations(workersWithNoEnemies);
		if (!wasAbleToFindValidWorker) {
			addToBestFactoryLocations(allWorkers);
		}

	}

	private static boolean addToBestFactoryLocations(ArrayList<Unit> workersToConsider) {
		for (Unit unit : workersToConsider) {
			MapLocation unitLocation = unit.location().mapLocation();
			for (Direction dir : Direction.values()) {
				MapLocation proposedLoc = unitLocation.add(dir);

				if (!Player.map.onMap(proposedLoc) || Player.map.isPassableTerrainAt(proposedLoc) == 0) {
					continue;
				}

				if (bestFactoryLocations.isEmpty()) {
					bestFactoryLocations.add(proposedLoc);
				} else {
					int numSurroundingWorkersProposed = Utils.countNearbyWorkers(proposedLoc);
					long distToEnemySpawnProposed = Utils.getDistanceToClosestEnemySpawn(proposedLoc);
					int numSurroundingWorkersBest = Utils.countNearbyWorkers(bestFactoryLocations.get(0));
					long distToEnemySpawnBest = Utils.getDistanceToClosestEnemySpawn(bestFactoryLocations.get(0));

					if (numSurroundingWorkersProposed == numSurroundingWorkersBest
							&& distToEnemySpawnProposed == distToEnemySpawnBest) {
						bestFactoryLocations.add(proposedLoc);
					} else if (numSurroundingWorkersProposed > numSurroundingWorkersBest ||
							(numSurroundingWorkersProposed == numSurroundingWorkersBest && distToEnemySpawnProposed < distToEnemySpawnBest)) {
						bestFactoryLocations.clear();
						bestFactoryLocations.add(proposedLoc);
					}
				}
			}
		}
		return bestFactoryLocations.size() > 0;
	}

	public static ArrayList<MapLocation> getBestFactoryLocations() {
		return bestFactoryLocations;
	}
}
