
// import the API.
// See xxx for the javadocs.
import java.util.ArrayList;

import bc.*;

public class Player {

	static GameController gc;
	static ArrayList<Unit> blueprints;
	static int workerCount;
	static Team enemy;

	public static boolean tryAndBuild(int workerId, UnitType type) {
		for (Direction direction : Direction.values()) {
			if (gc.canBlueprint(workerId, type, direction)) {
				gc.blueprint(workerId, type, direction);
				return true;
			}
		}
		return false;
	}

	public static boolean tryAndUnload(int structId) {
		for (Direction direction : Direction.values()) {
			if (gc.canUnload(structId, direction)) {
				gc.unload(structId, direction);
				return true;
			}
		}
		return false;
	}

	public static boolean tryAndReplicate(int workerId) {
		for (Direction direction : Direction.values()) {
			if (gc.canReplicate(workerId, direction)) {
				gc.replicate(workerId, direction);
				return true;
			}
		}
		return false;
	}

	public static boolean tryAndHarvest(int workerId) {
		for (Direction direction : Direction.values()) {
			if (gc.canHarvest(workerId, direction)) {
				gc.harvest(workerId, direction);
				return true;
			}
		}
		return false;
	}

	public static void main(String[] args) {

		// Connect to the manager, starting the game
		gc = new GameController();

		if (gc.team() == Team.Red) {
			enemy = Team.Blue;
		} else {
			enemy = Team.Red;
		}

		VecUnit startingWorkers = gc.units();
		for (int index = 0; index < startingWorkers.size(); index++) {
			Unit worker = startingWorkers.get(index);
			if (tryAndBuild(worker.id(), UnitType.Factory)) {
				break;
			}
		}
		for (int index = 0; index < startingWorkers.size(); index++) {
			Unit worker = startingWorkers.get(index);
			tryAndReplicate(worker.id());
		}

		gc.nextTurn();

		while (true) {
			VecUnit units = gc.myUnits();
			organizeUnits(units);

			for (int index = 0; index < units.size(); index++) {
				Unit unit = units.get(index);
				switch (unit.unitType()) {
				case Worker:
					moveWorker(unit);
					break;
				case Factory:
					moveFactory(unit);
					break;
				case Ranger:
					moveRanger(unit);
					break;
				}
			}
			// Submit the actions we've done, and wait for our next turn.
			gc.nextTurn();
		}
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

	private static void moveRanger(Unit unit) {
		if (!unit.location().isInGarrison()) {
			VecUnit foes = gc.senseNearbyUnitsByTeam(unit.location().mapLocation(), unit.attackRange(), enemy);
			if (unit.attackHeat() < 10) {
				for (int index = 0; index < foes.size(); index++) {
					if(gc.canAttack(unit.id(), foes.get(index).id())) {
						gc.attack(unit.id(), foes.get(index).id());
						break;
					}
				}
			}
			moveRandom(unit);
		}
	}

	private static void moveFactory(Unit unit) {
		if (unit.structureIsBuilt() == 0) {
			return;
		}
		if (unit.isFactoryProducing() == 0 && gc.karbonite() >= bc.bcUnitTypeFactoryCost(UnitType.Ranger)) {
			gc.produceRobot(unit.id(), UnitType.Ranger);
		}
		if (unit.structureGarrison().size() > 0) {
			tryAndUnload(unit.id());
		}
	}

	private static void moveWorker(Unit unit) {
		int id = unit.id();
		boolean built = false;
		for (Unit print : blueprints) {
			if (gc.canBuild(id, print.id())) {
				gc.build(id, print.id());
				built = true;
			}
		}
		if (workerCount < 3 && !built) {
			Player.tryAndReplicate(id);
		} else if (!built && !tryAndHarvest(id)) {
			moveRandom(unit);
		}

	}

	private static void moveRandom(Unit unit) {
		if (unit.movementHeat() >= 10) {
			return;
		}
		ArrayList<Direction> nextMoves = new ArrayList<Direction>();
		for (Direction direction : Direction.values()) {
			if (gc.canMove(unit.id(), direction)) {
				nextMoves.add(direction);
			}
		}
		if (nextMoves.size() > 0) {
			gc.moveRobot(unit.id(), nextMoves.get((int) (Math.random() * nextMoves.size())));
		}
	}
}