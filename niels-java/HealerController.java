import bc.*;

public class HealerController {

	public static void moveHealer(Unit self) {
		if (!self.location().isInGarrison()) {
			if (Player.planet == Planet.Earth) {
				Utils.tryAndGetIntoRocket(self);
			}

			tryToHealAndOvercharge(self);

			VecUnit foes = Player.gc.senseNearbyUnitsByTeam(self.location().mapLocation(), Constants.FLEE_RADIUS,
					Player.enemyTeam);
			if (foes.size() > 0) {
				fleeFromEnemy(self);
			} else {
				moveRecon(self);
			}
		}
	}

	private static void tryToHealAndOvercharge(Unit self) {
		VecUnit nearbyFriendlies = Player.gc.senseNearbyUnitsByTeam(self.location().mapLocation(), self.attackRange(),
				Player.friendlyTeam);

		long bestScore = 0;
		Unit target = null;
		for (int i = 0; i < nearbyFriendlies.size(); i++) {
			Unit unit = nearbyFriendlies.get(i);
			long newScore = healTargetScore(unit);
			if (newScore > bestScore) {
				target = unit;
				bestScore = newScore;
			}
		}

		if (target != null) {
			Utils.tryAndHeal(self, target);
		}

		// Pick an overcharge target
		if (self.abilityHeat() < Constants.MAX_ABILITY_HEAT) {
			Unit overchargeTarget = pickBestOverchargeTarget(self, nearbyFriendlies);
			if (overchargeTarget != null) {
				Player.gc.overcharge(self.id(), overchargeTarget.id());
				Player.moveUnit(Player.gc.unit(overchargeTarget.id()));
			}
		}
	}

	/**
	 * Picks best target based off of health.
	 */
	private static Unit pickBestOverchargeTarget(Unit self, VecUnit nearbyFriendlies) {
		long lowestHealth = Long.MAX_VALUE;
		long largestHeat = 0;
		Unit overchargeTarget = null;
		for (int i = 0; i < nearbyFriendlies.size(); i++) {
			Unit unit = nearbyFriendlies.get(i);
			long newHeat = unit.unitType() == UnitType.Knight ? unit.abilityHeat() : 0;
			long newHealth = unit.health();
			if (newHeat > largestHeat || (newHeat == largestHeat && newHealth < lowestHealth)) {
				if (unit.unitType() == UnitType.Ranger && Player.gc.canOvercharge(self.id(), unit.id())) {
					overchargeTarget = unit;
					lowestHealth = newHealth;
					largestHeat = newHeat;
				}
			}
		}
		return overchargeTarget;
	}

	private static long healTargetScore(Unit unit) {
		if (unit == null || unit.unitType() == UnitType.Factory || unit.unitType() == UnitType.Rocket) {
			return 0;
		}

		return unit.maxHealth() - unit.health();
	}

	private static void moveRecon(Unit unit) {
		boolean result = Utils.tryToMoveAccordingToDijkstraMap(unit, Player.armyNav, true);
		if (!result) {
			Utils.moveRandom(unit);
		}
	}

	private static void fleeFromEnemy(Unit unit) {
		Unit threat = Utils.getMostDangerousNearbyEnemy(unit);
		if (threat != null) {
			Direction toMove = Utils.fleeFrom(unit, threat);
			if (toMove != null && unit.movementHeat() < Constants.MAX_MOVEMENT_HEAT) {
				Player.gc.moveRobot(unit.id(), toMove);
			}
		}
	}
}
