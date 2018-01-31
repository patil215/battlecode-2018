import bc.*;
import java.util.*;
import java.awt.Point;

public class RangerController {

	public enum Mode {
		FIGHT_ENEMIES
	}

	public static void moveRanger(Unit unit) {
		if (!unit.location().isInGarrison() && unit.rangerIsSniping() == 0) {

			if (Player.planet == Planet.Earth) {
				Utils.tryAndGetIntoRocket(unit);
			}

			VecUnit foes = NearbyUnitsCache.getEnemiesInVisionRange(unit);
			if (foes.size() > 0) {
				combatMicro(unit, foes);
			} else if (!Utils.targetsExist(Player.armyNav, unit.location().mapLocation()) 
					&& Player.gc.round() > Constants.RANGER_SNIPE_COMPLETE_ROUND) {
				if(!tryToSnipe(unit)) {
					moveRecon(unit);
				}
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
		double bestTargetScore = Long.MAX_VALUE;
		for (int index = 0; index < foes.size(); index++) {
			Unit foe = foes.get(index);
			double newScore = CombatUtils.targetScore(unit, foe);
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

		if(target != null) {
			return;
		}

		for (int index = 0; index < foes.size(); index++) {
			Unit foe = foes.get(index);
			double newScore = CombatUtils.targetScore(unit, foe);
			if (newScore < bestTargetScore) {
				target = foe;
				bestTargetScore = newScore;
			}
		}

		if (target != null && unit.attackHeat() < Constants.MAX_ATTACK_HEAT) {
			CombatUtils.attack(unit, target);
		}
	}

	private static Random rand = new Random();
	private static boolean tryToSnipe(Unit unit) {

		// ONLY USABLE ON MARS
		if (!Player.gc.isBeginSnipeReady(unit.id()) || !unit.location().isOnPlanet(Planet.Mars)) {
			return false;
		}

		int id = unit.id();

		// Try to shoot at enemies we have seen first
		for (Unit enemy : Player.enemyUnits) {
			if (CombatUtils.getTargetHealth(enemy) > CombatUtils.getNumberSnipersAimedAt(enemy) * unit.damage()) {
				MapLocation loc = enemy.location().mapLocation();
				if (Player.gc.canBeginSnipe(id, loc)) {
					Player.gc.beginSnipe(id, loc);
					CombatUtils.incrementNumberSnipersAimedAt(enemy);
					return true;
				}
			}
		}

		// Try random locations otherwise
		for(int i = 0; i < 20; i++) {
			Point randPoint = RocketController.validLocations.get(rand
					.nextInt(RocketController.validLocations.size()));
			MapLocation loc = new MapLocation(Planet.Mars, randPoint.x, randPoint.y);
			if(!Player.gc.canSenseLocation(loc)
					&& Player.gc.canBeginSnipe(id, loc)) {
				Player.gc.beginSnipe(id, loc);
				return true;
			}
		}
		return false;
	}

}
