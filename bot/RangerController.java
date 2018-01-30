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

			VecUnit foes = NearbyUnitsCache.getEnemiesInVisionRange(unit);
			if (foes.size() > 0) {
				combatMicro(unit, foes);
			} else {
				moveRecon(unit);
			}
		}
	}

	private static void moveRecon(Unit unit) {
		boolean result = Utils.tryToMoveAccordingToDijkstraMap(unit, Player.armyNav, false);
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
			Utils.tryAndFleeFrom(unit, threat);
		} else {
			if (target == null) {
				moveRecon(unit);
			}
		}
	}
}