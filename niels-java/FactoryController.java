import bc.Unit;
import bc.UnitType;
import bc.bc;

public class FactoryController {

	public enum Mode {
		PRODUCE,
		IDLE
	}

	public static void moveFactory(Unit unit) {
		// Don't do anything if we're not built yet
		if (unit.structureIsBuilt() == 0) {
			return;
		}

		switch (Utils.getMemory(unit).factoryMode) {
			case IDLE: {
				// Do nothing
				break;
			}
			case PRODUCE: {
				moveProduce(unit);
				break;
			}
		}
	}

	private static void moveProduce(Unit unit) {
		if (CensusCounts.getUnitCount(UnitType.Worker) <= 1 && unit.isFactoryProducing() == 0 && Player.gc.karbonite() >= bc.bcUnitTypeFactoryCost(UnitType.Worker)) {
			Player.gc.produceRobot(unit.id(), UnitType.Worker);
		} else if (unit.isFactoryProducing() == 0 && Player.gc.karbonite() >= bc.bcUnitTypeFactoryCost(UnitType.Ranger)) {
		//&& CensusCounts.getUnitCount(UnitType.Ranger) <= 10 && Player.gc.round() > 375) { // USED FOR LIMITING, REMEMBER TO TURN OFF
			if (Math.random() < 0.2 && CensusCounts.getUnitCount(UnitType.Ranger) >= 4) {
				Player.gc.produceRobot(unit.id(), UnitType.Healer);
			} else {
				Player.gc.produceRobot(unit.id(), UnitType.Ranger);
			}
		}
		Utils.tryAndUnload(unit);
	}

}
