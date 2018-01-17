import bc.*;

public class RangerController {

	public enum Mode {
		FIGHT_ENEMIES
	}

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
			return target.health() * 3;
		}
		return target.health();
	}

	public static void moveRanger(Unit unit) {
		if (!unit.location().isInGarrison()) {

			if (Player.planet == Planet.Earth) {
				tryToGetIntoRocket(unit);
			}

			VecUnit foes = Player.gc.senseNearbyUnitsByTeam(unit.location().mapLocation(), unit.visionRange(),
					Player.enemy);
			if (foes.size() > 0) {
				combatMicro(unit, foes);
			} else {
				moveRecon(unit);
			}
		}
	}

	private static void tryToGetIntoRocket(Unit unit) {
		VecUnit units = Player.gc.myUnits();
		for (int i = 0; i < units.size(); i++) {
			Unit potentialRocket = units.get(i);
			if (potentialRocket.unitType() == UnitType.Rocket && potentialRocket.structureIsBuilt() == 1) {
				if (Player.gc.canLoad(potentialRocket.id(), unit.id())) {
					Player.gc.load(potentialRocket.id(), unit.id());
					return; // We done after loading
				}
			}
		}
	}

	private static void moveRecon(Unit unit) {
		if (unit.movementHeat() < 10) {
			Direction toMove = Player.armyNav.getNextDirection(unit);

			if (toMove == null) {
				return;
			}
			
			if (Player.gc.canMove(unit.id(), toMove)) {
				Player.gc.moveRobot(unit.id(), toMove);
			}
		}
	}

	public static void combatMicro(Unit unit, VecUnit foes) {
		Unit target = null;
		Unit threat = Utils.getMostDangerousNearbyEnemy(unit);
		for (int index = 0; index < foes.size(); index++) {
			Unit foe = foes.get(index);
			if (targetScore(unit, foe) < targetScore(unit, target)) {
				target = foe;
			}
		}
		if (target != null && Player.gc.canAttack(unit.id(), target.id()) && unit.attackHeat() < 10) {
			Player.gc.attack(unit.id(), target.id());
		}
		if (threat != null) {
			Direction toMove = Utils.fleeFrom(unit, threat);
			if (toMove != null && unit.movementHeat() < 10 && Player.gc.canMove(unit.id(), toMove)) {
				Player.gc.moveRobot(unit.id(), toMove);
			}
		} else {
			if(target == null) {
				moveRecon(unit);
			}
		}
	}
}
