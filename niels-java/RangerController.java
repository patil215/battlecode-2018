import bc.Direction;
import bc.Location;
import bc.Planet;
import bc.Unit;
import bc.VecUnit;

public class RangerController {
	public static void moveRanger(Unit unit) {
		if (!unit.location().isInGarrison()) {
			VecUnit foes = Player.gc.senseNearbyUnitsByTeam(unit.location().mapLocation(), unit.attackRange(),
					Player.enemy);
			if (unit.attackHeat() < 10) {
				for (int index = 0; index < foes.size(); index++) {
					if (Player.gc.canAttack(unit.id(), foes.get(index).id())) {
						Player.gc.attack(unit.id(), foes.get(index).id());
						break;
					}
				}
			}
			VecUnit startingUnits = Player.gc.startingMap(Planet.Earth).getInitial_units();
			Location target = null;
			for (int index = 0; index < startingUnits.size(); index++) {
				if (startingUnits.get(index).team() == Player.enemy && (target == null || Math.random() > .5)) {
					target = startingUnits.get(index).location();
				}
			}

			if (Player.robotMemory.get(unit.id()).currentTarget == null) {
				Player.robotMemory.get(unit.id()).currentTarget = target.mapLocation();
				Player.robotMemory.get(unit.id()).pathToTarget = Player.nav.getPathToDest(unit.location().mapLocation(),
						target.mapLocation());
			}

			if (unit.movementHeat() < 10 && Player.robotMemory.get(unit.id()).pathToTarget != null
					&& Player.robotMemory.get(unit.id()).reachedDest == false) {
				Direction toMove = Player.nav.directionTowards(unit.location().mapLocation(),
						Player.robotMemory.get(unit.id()).pathToTarget.pop());

				System.out.println("Target is " + target);
				if (toMove == null) {
					System.out.println("Trying to move to null location.");
					return;
				}
				if (Player.gc.canMove(unit.id(), toMove)) {
					Player.gc.moveRobot(unit.id(), toMove);
				}
			} else if (Player.robotMemory.get(unit.id()).reachedDest) {
				Utils.moveRandom(unit);
			}
		}
	}
}
