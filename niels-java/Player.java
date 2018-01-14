
// import the API.
// See xxx for the javadocs.
import java.util.ArrayList;
import java.util.HashMap;

import bc.*;

public class Player {

	public static GameController gc;
	static ArrayList<Unit> blueprints;
	static int workerCount;
	static Team enemy;
	static Navigation nav;
	static HashMap<Integer, RobotMemory> robotMemory;

	public static void main(String[] args) {
		// Connect to the manager, starting the game
		gc = new GameController();

		robotMemory = new HashMap<Integer, RobotMemory>();

		nav = new Navigation(gc.startingMap(gc.planet()));

		if (gc.team() == Team.Red) {
			enemy = Team.Blue;
		} else {
			enemy = Team.Red;
		}

		InitialTurns();

		while (true) {
			VecUnit units = gc.myUnits();
			for (int index = 0; index < units.size(); index++) {
				if (!robotMemory.containsKey(units.get(index).id())) {
					robotMemory.put(units.get(index).id(), new RobotMemory());
				}
			}

			organizeUnits(units);

			MoveUnits(units);
		}
	}

	private static void MoveUnits(VecUnit units) {
		for (int index = 0; index < units.size(); index++) {
			Unit unit = units.get(index);
			switch (unit.unitType()) {
			case Worker:
				WorkerController.moveWorker(unit);
				break;
			case Factory:
				FactoryController.moveFactory(unit);
				break;
			case Ranger:
				RangerController.moveRanger(unit);
				break;
			default:
				break;
			}
		}
		// Submit the actions we've done, and wait for our next turn.
		gc.nextTurn();
	}

	private static void InitialTurns() {
		gc.nextTurn();

		VecUnit startingWorkers = gc.units();
		for (int index = 0; index < startingWorkers.size(); index++) {
			Unit worker = startingWorkers.get(index);
			if (Utils.tryAndBuild(worker.id(), UnitType.Factory)) {
				break;
			}
		}

		gc.nextTurn();

		for (int index = 0; index < startingWorkers.size(); index++) {
			Unit worker = startingWorkers.get(index);
			Utils.tryAndReplicate(worker.id());
		}

		gc.nextTurn();
	}

	private static void organizeUnits(VecUnit units) {
		workerCount = 0;
		ArrayList<Unit> blueprints = new ArrayList<Unit>();
		for (int index = 0; index < units.size(); index++) {
			Unit unit = units.get(index);
			if (unit.unitType() == UnitType.Worker) {
				workerCount++;
			}
			if ((unit.unitType() == UnitType.Factory || unit.unitType() == UnitType.Rocket)
					&& unit.structureIsBuilt() == 0) {
				blueprints.add(unit);
			}
		}
		Player.blueprints = blueprints;
	}
}