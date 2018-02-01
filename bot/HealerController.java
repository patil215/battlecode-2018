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
	 * Picks best target based off of potential.
	 */
	
	private static Unit pickBestOverchargeTarget(Unit self, VecUnit nearbyFriendlies) {
		Unit lowestPotential = null;
		for(int i = 0; i < nearbyFriendlies.size(); i++) {
			Unit target = nearbyFriendlies.get(i);
			if (target.unitType()!= UnitType.Ranger && target.unitType() != UnitType.Knight) {
				continue;
			}
			if (!Player.gc.canOvercharge(self.id(), target.id())) {
				continue;
			}
			if (lowestPotential == null) {
				lowestPotential = target;
				continue;
			}
			if (Player.armyNav.getDijkstraMapValue(target.location().mapLocation()) < Player.armyNav.getDijkstraMapValue(lowestPotential.location().mapLocation())) {
				lowestPotential = target;
			}
			if (Player.armyNav.getDijkstraMapValue(target.location().mapLocation())
					== Player.armyNav.getDijkstraMapValue(lowestPotential.location().mapLocation())
					&& target.health() < lowestPotential.health()) {
				lowestPotential = target;
			}
		}
		return lowestPotential;
	}

	private static long healTargetScore(Unit unit) {
		if (unit == null || unit.unitType() == UnitType.Factory || unit.unitType() == UnitType.Rocket) {
			return 0;
		}

		return unit.maxHealth() - unit.health();
	}

	private static void moveRecon(Unit unit) {
		boolean result = Utils.tryToMoveAccordingToDijkstraMap(unit, Player.armyNav, false);
		if (!result) {
			Utils.moveRandom(unit);
		}
	}

	private static void fleeFromEnemy(Unit unit) {
		Unit threat = Utils.getMostDangerousNearbyEnemy(unit);
		if (threat != null) {
			Utils.tryAndFleeFrom(unit, threat);
		}
	}
}
