import bc.Direction;
import bc.MapLocation;
import bc.Unit;
import bc.UnitType;

import java.util.*;
import java.awt.Point;

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
		List<Point> oldTargets = new ArrayList<>(Player.factoryNav.getTargets());
		for(Point target : oldTargets) {
			MapLocation targetLoc = 
				new MapLocation(Player.planet, target.x, target.y);
			if(Player.gc.hasUnitAtLocation(targetLoc)) {
				Unit building = Player.gc.senseUnitAtLocation(targetLoc);
				if(building.team() != Player.friendlyTeam 
						&& building.unitType() != UnitType.Factory 
						&& building.unitType() != UnitType.Rocket) {
					Player.factoryNav.removeTarget(targetLoc);
				}
			} else { 
				Player.factoryNav.removeTarget(targetLoc);
			}
		}
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
					Player.robotMemory.get(unit.id()).searchForKarbonite = false;
					MapLocation blueprintLoc = blueprint.location().mapLocation();

					clearDestroyedFactories();
					Player.factoryNav.addTarget(blueprintLoc);
					Player.factoryNav.recalculateDistanceMap();

					Player.builderNav.removeTarget(blueprintLoc);
					Player.builderNav.recalculateDistanceMap();
				}
				return true;
			}
		}
		return false;
	}

	public static void findBestFactoryBuildLocations() {
		bestFactoryLocations = new ArrayList<>();

		for (Unit unit : Player.friendlyUnits) {
			if (unit.unitType() != UnitType.Worker) {
				continue;
			}

			if (unit.location().isInGarrison()) {
				continue;
			}

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
					long distToEnemySpawnProposed = Utils.distToClosestEnemySpawn(proposedLoc);
					int numSurroundingWorkersBest = Utils.countNearbyWorkers(bestFactoryLocations.get(0));
					long distToEnemySpawnBest = Utils.distToClosestEnemySpawn(bestFactoryLocations.get(0));

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
	}

	public static ArrayList<MapLocation> getBestFactoryLocations() {
		return bestFactoryLocations;
	}
}
