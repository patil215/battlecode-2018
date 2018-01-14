import bc.Unit;
import bc.UnitType;

public class WorkerController {
	static void moveWorker(Unit unit) {
		int id = unit.id();
		boolean built = false;
		for (Unit print : Player.blueprints) {
			if (Player.gc.canBuild(id, print.id())) {
				Player.gc.build(id, print.id());
				built = true;
			}
		}
		if (Player.workerCount < 5 && !built) {
			Utils.tryAndReplicate(id);
		} else if (!built && !Utils.tryAndHarvest(id)) {
			Utils.moveRandom(unit);
			if (Player.gc.karbonite() > 50) {
				Utils.tryAndBuild(unit.id(), UnitType.Factory);
			}
		}

	}

}
