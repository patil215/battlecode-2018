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
					&& Player.gc.round() > Constants.RANGER_SNIPE_COMPLETE_ROUND
					&& Player.gc.isBeginSnipeReady(unit.id())) {
				if(!snipeInvisibleArea(unit)) {
					moveRecon(unit);
				}
			} else {
				moveRecon(unit);
			}
		}
	}

	private static Random rand = new Random();
	// ONLY USABLE ON MARS
	private static boolean snipeInvisibleArea(Unit unit) {
		int id = unit.id();
		if (Player.enemyUnits.size() > 0) {
			MapLocation loc = Player.enemyUnits.get(0).location().mapLocation();
			if(Player.gc.canBeginSnipe(id, loc)) {
				Player.gc.beginSnipe(id, loc);
				return true;
			}
		}
		for(int i = 0; i < 20; i++) { 
			Point randPoint = RocketController.validLocations.get(rand
					.nextInt(RocketController.validLocations.size()));
			MapLocation loc = new MapLocation(Planet.Mars, randPoint.x, randPoint.y); 
			if(!Player.gc.canSenseLocation(loc) 
					&& Player.gc.canBeginSnipe(id, loc)) {
				System.out.println("Sniping at " + randPoint.x + " " + randPoint.y);
				Player.gc.beginSnipe(id, loc);
				return true;
			}
		}
		return false;
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

		if (target != null && unit.attackHeat() < Constants.MAX_ATTACK_HEAT && !CombatUtils.hasTriedToOneShot(unit)) {
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
			long newScore = CombatUtils.targetScore(unit, foe);
			if (newScore < bestTargetScore) {
				target = foe;
				bestTargetScore = newScore;
			}
		}
		
		if (target != null && unit.attackHeat() < Constants.MAX_ATTACK_HEAT && !CombatUtils.hasTriedToOneShot(unit)) {
			CombatUtils.attack(unit, target);
		}
	}
}
