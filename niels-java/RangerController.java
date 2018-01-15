import bc.Direction;
import bc.Location;
import bc.MapLocation;
import bc.Planet;
import bc.Unit;
import bc.UnitType;
import bc.VecUnit;

public class RangerController {

	private static long targetScore(Unit unit, Unit target) {
		if (target == null || !Player.gc.canAttack(unit.id(), target.id())) {
			return Long.MAX_VALUE;
		}
		if (target.unitType() == UnitType.Worker) {
			return target.health() * 3;
		} else if (target.unitType() == UnitType.Healer) {
			return target.health() / 2;
		} else if (target.unitType() == UnitType.Factory) {
			return target.health() * 2;
		} else if (target.unitType() == UnitType.Rocket) {
			return Long.MAX_VALUE;
		}
		return target.health();
	}

	public static void moveRanger(Unit unit) {

		if (!unit.location().isInGarrison()) {
			VecUnit foes = Player.gc.senseNearbyUnitsByTeam(unit.location().mapLocation(), unit.visionRange(),
					Player.enemy);
			if (foes.size() > 0) {
				combatMicro(unit, foes);
			} else {
				moveRecon(unit);
			}
		}
	}

	private static void moveRecon(Unit unit) {
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

		if (Player.robotMemory.get(unit.id()).pathToTarget != null
				&& Player.robotMemory.get(unit.id()).pathToTarget.size() == 0) {
			Player.robotMemory.get(unit.id()).reachedDest = true;
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

	public static void combatMicro(Unit unit, VecUnit foes) {
		Unit target = null;
		Unit threat = null;
		for (int index = 0; index < foes.size(); index++) {
			Unit foe = foes.get(index);
			MapLocation unitLocation = unit.location().mapLocation();
			if ((unitLocation.distanceSquaredTo(foe.location().mapLocation()) < unit.attackRange()
					&& (foe.unitType() == UnitType.Mage || foe.unitType() == UnitType.Knight
							|| foe.unitType() == UnitType.Ranger))
					&& (threat == null || unitLocation.distanceSquaredTo(foe.location().mapLocation()) < unitLocation
							.distanceSquaredTo(threat.location().mapLocation()))) {
				threat = foe;
			}
			if (targetScore(unit, foe) < targetScore(unit, target)) {
				target = foe;
			}
		}
		if (targetScore(unit, target) < Long.MAX_VALUE && target != null && Player.gc.canAttack(unit.id(), target.id()) && unit.attackHeat() < 10) {
			Player.gc.attack(unit.id(), target.id());
			System.out.println("attack");
		}
		if (threat != null) {
			Direction toMove = Utils.fleeFrom(unit, threat);
			if (toMove != null && unit.movementHeat() < 10 && Player.gc.canMove(unit.id(), toMove)) {
				Player.gc.moveRobot(unit.id(), toMove);
			}
		} else {
			Utils.moveRandom(unit);
		}
	}
}
