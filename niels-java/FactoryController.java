import bc.Unit;
import bc.UnitType;
import bc.bc;

public class FactoryController {
	public static void moveFactory(Unit unit) {
		if (unit.structureIsBuilt() == 0) {
			return;
		}
		if (unit.isFactoryProducing() == 0 && Player.gc.karbonite() >= bc.bcUnitTypeFactoryCost(UnitType.Ranger)) {
			Player.gc.produceRobot(unit.id(), UnitType.Ranger);
		}
		if (unit.structureGarrison().size() > 0) {
			Utils.tryAndUnload(unit.id());
		}
	}

}
