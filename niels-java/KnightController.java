import bc.Direction;
import bc.Planet;
import bc.Unit;
import bc.VecUnit;

public class KnightController {

	public static void moveKnight(Unit unit) {
		if (unit.location().isInGarrison()) {
			runInGarrison(unit);
		} else {
			run(unit);
		}
	}

	private static void runInGarrison(Unit unit) {
		// Pass
	}

	private static void run(Unit unit) {
		if (Player.planet == Planet.Earth) {
			Utils.tryAndGetIntoRocket(unit);
		}

		VecUnit foes =
				Player.gc.senseNearbyUnitsByTeam(unit.location().mapLocation(), unit.visionRange(), Player.enemyTeam);
		if (foes.size() > 0) {
			combatMicro(unit);
		} else {
			moveRecon(unit);
		}
	}

	private static void moveRecon(Unit unit) {
		boolean result = Utils.tryToMoveAccordingToDijkstraMap(unit, Player.armyNav, false);
		if (!result) {
			Utils.moveRandom(unit);
		}
	}

	private static void combatMicro(Unit unit) {
		// Move
		if(!Utils.tryToMoveAccordingToDijkstraMap(unit, CombatUtils.microNav, false)){
			moveRecon(unit);
		}

		if (unit.attackHeat() >= Constants.MAX_ATTACK_HEAT) {
			return;
		}
		// Attack
		VecUnit targets =
				Player.gc.senseNearbyUnitsByTeam(unit.location().mapLocation(), unit.attackRange(), Player.enemyTeam);
		Unit target = null;
		long bestTargetScore = Long.MAX_VALUE;
		for (int index = 0; index < targets.size(); index++) {
			Unit foe = targets.get(index);
			long newScore = CombatUtils.targetScore(unit, foe);
			if (newScore < bestTargetScore) {
				target = foe;
				bestTargetScore = newScore;
			}
		}
		if (target != null) {
			CombatUtils.attack(unit, target);
		}

		// Javelin code
		/*targets = Player.gc.senseNearbyUnitsByTeam(unit.location().mapLocation(), unit.abilityRange(), Player.enemyTeam);
		target = null;
		bestTargetScore = Long.MAX_VALUE;
		for (int index = 0; index < targets.size(); index++) {
			Unit foe = targets.get(index);
			long newScore = CombatUtils.targetScore(unit, foe);
			if (newScore < bestTargetScore) {
				target = foe;
				bestTargetScore = newScore;
			}
		}
		if (target != null && unit.abilityHeat() < 10 && Player.gc.canJavelin(unit.id(), target.id())) {
			CombatUtils.attackJavelin(unit, target);
		}*/
	}
}
