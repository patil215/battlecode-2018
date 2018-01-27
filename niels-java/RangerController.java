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
		boolean result = Utils.tryToMoveAccordingToDijkstraMap(unit, Player.armyNav, true);
		if (!result) {
			Utils.moveRandom(unit);
		}
	}

	private static void combatMicro(Unit unit, VecUnit foes) {
		Unit threat = Utils.getMostDangerousNearbyEnemy(unit);

		Unit target = null;
		long bestTargetScore = Long.MAX_VALUE;
		for (int index = 0; index < foes.size(); index++) {
			Unit foe = foes.get(index);
			long newScore = CombatUtils.targetScore(unit, foe);
			if (newScore < bestTargetScore) {
				target = foe;
				bestTargetScore = newScore;
			}
		}
		if (target != null && unit.attackHeat() < Constants.MAX_ATTACK_HEAT) {
			CombatUtils.attack(unit, target);
		}
		if (threat != null) {
			Direction toMove = Utils.fleeFrom(unit, threat);
			if (toMove != null && unit.movementHeat() < Constants.MAX_MOVEMENT_HEAT) {
				Player.gc.moveRobot(unit.id(), toMove);
			}
		} else {
			if (target == null) {
				moveRecon(unit);
			}
		}
	}
}
