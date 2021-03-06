import bc.Unit;
import bc.UnitType;
import bc.bc;

public class FactoryController {
	private static int knightsProduced = 0;

	public enum Mode {
		PRODUCE, IDLE
	}

	public static void moveFactory(Unit unit) {
		if (unit.structureIsBuilt() == 0) {
			// Remove us from the builder map if we have at least 4 workers around us
			if (Utils.countNearbyFriendlyWorkers(unit.location().mapLocation()) >= 4) {
				Player.builderNav.removeTarget(unit.location().mapLocation());
				Player.builderNav.recalculateDistanceMap();
			}
			return;
		}

		switch (Utils.getMemory(unit).factoryMode) {
		case IDLE: {
			Utils.tryAndUnload(unit);
			break;
		}
		case PRODUCE: {
			moveProduce(unit);
			break;
		}
		}
	}

	private static void moveProduce(Unit unit) {
		boolean isProducing = unit.isFactoryProducing() == 1;

		if (CensusCounts.workersOnEarth <= 1 && !isProducing
				&& Player.gc.karbonite() >= bc.bcUnitTypeFactoryCost(UnitType.Worker)) {
			Player.gc.produceRobot(unit.id(), UnitType.Worker);
			CensusCounts.incrementUnitCount(UnitType.Worker);
			CensusCounts.workersOnEarth++;
		} else if (!isProducing && Player.gc.karbonite() >= bc.bcUnitTypeFactoryCost(UnitType.Ranger)) {
			// && CensusCounts.getUnitCount(UnitType.Ranger) <= 10 && Player.gc.round() >
			// 375) { // USED FOR LIMITING, REMEMBER TO TURN OFF
			if (knightsProduced < Constants.BEGINNING_KNIGHTS) {
				Player.gc.produceRobot(unit.id(), UnitType.Knight);
				CensusCounts.incrementUnitCount(UnitType.Knight);
				knightsProduced++;
			} else if (CensusCounts.getUnitCount(UnitType.Ranger) < Constants.MAX_RANGERS) {
				if (CensusCounts
						.getUnitCount(UnitType.Ranger) < (((CensusCounts.getUnitCount(UnitType.Healer) + 1) *3))) {
					Player.gc.produceRobot(unit.id(), UnitType.Ranger);
					CensusCounts.incrementUnitCount(UnitType.Ranger);
				} else {
					Player.gc.produceRobot(unit.id(), UnitType.Healer);
					CensusCounts.incrementUnitCount(UnitType.Healer);
				}
			}
		}
		Utils.tryAndUnload(unit);
	}

}
