import bc.*;

public class RangerController {

	public enum Mode {
		FIGHT_ENEMIES
	}

	public static void moveRanger(Unit unit) {
		if (!unit.location().isInGarrison()) {

			if (Player.planet == Planet.Earth) {
				Utils.tryAndGetIntoRocket(unit);
			}

			VecUnit foes = Player.gc.senseNearbyUnitsByTeam(unit.location().mapLocation(), unit.visionRange(),
					Player.enemyTeam);
			if (foes.size() > 0) {
				combatMicro(unit, foes);
			} else {
				moveRecon(unit);
			}
		}
	}

	private static void moveRecon(Unit unit) {
		Utils.moveAccordingToDjikstraMap(unit, Player.armyNav);
	}

	public static void combatMicro(Unit unit, VecUnit foes) {
		Unit target = null;
		Unit threat = Utils.getMostDangerousNearbyEnemy(unit);
		for (int index = 0; index < foes.size(); index++) {
			Unit foe = foes.get(index);
			if (CombatUtils.targetScore(unit, foe) < CombatUtils.targetScore(unit, target)) {
				target = foe;
			}
		}
		if (target != null && Player.gc.canAttack(unit.id(), target.id())
				&& unit.attackHeat() < Constants.MAX_ATTACK_HEAT) {
			CombatUtils.attack(unit, target);
		}
		if (threat != null) {
			Direction toMove = Utils.fleeFrom(unit, threat);
			if (toMove != null && unit.movementHeat() < Constants.MAX_MOVEMENT_HEAT
					&& Player.gc.canMove(unit.id(), toMove)) {
				Player.gc.moveRobot(unit.id(), toMove);
			}
		} else {
			if(target == null) {
				moveRecon(unit);
			}
		}
	}
}
