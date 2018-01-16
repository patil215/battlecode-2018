import bc.Unit;
import bc.UnitType;
import bc.VecUnit;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;

import static bc.UnitType.Rocket;

public class CensusCounts {

	static Map<UnitType, Integer> unitCounts = new EnumMap<>(UnitType.class);

	private static Map<WorkerController.Mode, Integer> workerModeCounts = new EnumMap<>(WorkerController.Mode.class);
	private static Map<FactoryController.Mode, Integer> factoryModeCounts = new EnumMap<>(FactoryController.Mode.class);
	private static Map<RangerController.Mode, Integer> rangerModeCounts  = new EnumMap<>(RangerController.Mode.class);

	public static void computeCensus(VecUnit units) {
		resetCounts();

		ArrayList<Unit> blueprints = new ArrayList<Unit>();

		for (int index = 0; index < units.size(); index++) {
			Unit unit = units.get(index);

			// If it's in the process of building, just add the blueprint
			if ((unit.unitType() == UnitType.Factory || unit.unitType() == Rocket) && unit.structureIsBuilt() == 0) {
				blueprints.add(unit);
				continue;
			}

			CensusCounts.incrementUnitCount(unit.unitType());
			switch (unit.unitType()) {
				case Worker: {
					CensusCounts.incrementWorkerModeCount(Utils.getMemory(unit).workerMode);
					break;
				}

				case Factory: {
					CensusCounts.incrementFactoryModeCount(Utils.getMemory(unit).factoryMode);
					break;
				}

				case Ranger: {
					CensusCounts.incrementRangerModeCount(Utils.getMemory(unit).rangerMode);
				}
			}
		}
		Player.blueprints = blueprints;
	}


	public static void resetCounts() {
		for (UnitType type : UnitType.values()) {
			unitCounts.put(type, 0);
		}

		for (WorkerController.Mode mode : WorkerController.Mode.values()) {
			workerModeCounts.put(mode, 0);
		}

		for (FactoryController.Mode mode : FactoryController.Mode.values()) {
			factoryModeCounts.put(mode, 0);
		}

		for (RangerController.Mode mode : RangerController.Mode.values()) {
			rangerModeCounts.put(mode, 0);
		}
	}

	private static void incrementUnitCount(UnitType type) {
		if (!unitCounts.containsKey(type)) {
			unitCounts.put(type, 1);
		}
		unitCounts.put(type, unitCounts.get(type) + 1);
	}

	public static int getUnitCount(UnitType type) {
		return unitCounts.get(type);
	}

	public static void incrementWorkerModeCount(WorkerController.Mode mode) {
		workerModeCounts.put(mode, workerModeCounts.get(mode) + 1);
	}

	public static void decrementWorkerModeCount(WorkerController.Mode mode) {
		workerModeCounts.put(mode, workerModeCounts.get(mode) - 1);
	}

	public static int getWorkerModeCount(WorkerController.Mode mode) {
		if (workerModeCounts.containsKey(mode)) {
			return workerModeCounts.get(mode);
		}
		return 0;
	}

	public static void incrementFactoryModeCount(FactoryController.Mode mode) {
		factoryModeCounts.put(mode, factoryModeCounts.get(mode) + 1);
	}

	public static void decrementFactoryModeCount(FactoryController.Mode mode) {
		factoryModeCounts.put(mode, factoryModeCounts.get(mode) - 1);
	}

	public static int getFactoryModeCount(FactoryController.Mode mode) {
		if (factoryModeCounts.containsKey(mode)) {
			return factoryModeCounts.get(mode);
		}
		return 0;
	}

	public static void incrementRangerModeCount(RangerController.Mode mode) {
		rangerModeCounts.put(mode, rangerModeCounts.get(mode) + 1);
	}

	public static void decrementRangerModeCount(RangerController.Mode mode) {
		rangerModeCounts.put(mode, rangerModeCounts.get(mode) - 1);
	}

	public static int getRangerModeCount(RangerController.Mode mode) {
		if (rangerModeCounts.containsKey(mode)) {
			return rangerModeCounts.get(mode);
		}
		return 0;
	}
}
