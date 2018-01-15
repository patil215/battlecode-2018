import bc.Direction;
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
		} 
		if (!built && !Utils.tryAndHarvest(unit)) {
			if (Utils.getMemory(unit).isHarvester) {
				Direction toMove = Player.workerNav.getNextDirection(unit.location().mapLocation());
				if (toMove != null && Player.gc.canMove(unit.id(), toMove) && unit.movementHeat() < 10) {
					Player.gc.moveRobot(unit.id(), toMove);
				} else {
					Utils.moveRandom(unit);
				}
			} else {
				Utils.moveRandom(unit);
			}
			if (Player.gc.karbonite() > 50 && !Utils.getMemory(unit).isHarvester) {
				Utils.tryAndBuild(unit.id(), UnitType.Factory);
			}
		}

	}

}
