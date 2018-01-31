import bc.MapLocation;
import bc.Unit;
import bc.UnitType;
import bc.VecUnit;

import java.util.*;
import java.util.stream.Collectors;

public class CombatUtils {
	/**
	Used to keep track of enemyTeam health decreasing after being marked.
	This is necessary because if other units on our team "intend" to shoot at a particular unit on the enemyTeam team,
	before the turn has ended, that means the effective health of that unit on the enemyTeam team is lower.
	Accordingly, these changes should be cached and taken into account.
	 */
	private static HashMap<Unit, Long> markedEnemyHealth = new HashMap<>();

	private static HashMap<Unit, Unit> preferredRangerTargets = new HashMap<>();

	static Navigation microNav = new Navigation(Player.map, new HashSet<>(), Constants.KNIGHT_MICRO_NAV_MAXDIST);

	public static void initAtStartOfTurn() {
		if (CensusCounts.getUnitCount(UnitType.Knight) > 0) {
			microNav.clearTargets();
			for (Unit foe : Player.enemyUnits) {
				if (foe.location().isOnPlanet(Player.planet) && !foe.location().isInGarrison()) {
					MapLocation location = foe.location().mapLocation();
					microNav.addTarget(location);
				}
			}
			microNav.recalculateDistanceMap();
		}		
	}

	public static void cleanupAtEndOfTurn() {
		markedEnemyHealth = new HashMap<>();
	}

	public static long getTargetHealth(Unit target) {
		// Try getting health from cache
		if (markedEnemyHealth.containsKey(target)) {
			return markedEnemyHealth.get(target);
		}
		return target.health();
	}

	private static long rangerSubscore(Unit target) {
		long targetHealth = getTargetHealth(target);
		if (targetHealth == 0) {
			return Long.MAX_VALUE; // Make sure we don't "overkill" units
		}
		switch (target.unitType()) {
			case Mage:
				return targetHealth / 2;
			case Knight:
				return targetHealth * 2;
			case Ranger:
				return targetHealth * 2;
			case Healer:
				return targetHealth * 3;
			case Factory:
				return targetHealth * 4;
			case Worker:
				return targetHealth * 5;
			case Rocket:
				return targetHealth * 6;
			default:
				return targetHealth;
		}
	}

	private static long knightSubscore(Unit target) {
		return rangerSubscore(target);
	}

	/**
	 * Returns a score for a target. If the target is not attackable, returns Long.MAX_VALUE.
	 */
	public static long targetScore(Unit attacker, Unit target) {
		if (target == null || !Player.gc.canAttack(attacker.id(), target.id())) {
			return Long.MAX_VALUE;
		}

		switch (attacker.unitType()) {
			case Ranger:
				return rangerSubscore(target);
			case Knight:
				return knightSubscore(target);
			default:
				return knightSubscore(target);
		}
	}

	public static void attack(Unit attacker, Unit target) {
		Player.gc.attack(attacker.id(), target.id());

		// Update health in cache
		if (markedEnemyHealth.containsKey(target)) {
			long defense = 0;
			if (target.unitType() == UnitType.Knight) {
				defense = target.knightDefense();
			}

			markedEnemyHealth.put(target, markedEnemyHealth.get(target) - attacker.damage() + defense);
		} else {
			markedEnemyHealth.put(target, target.health() - attacker.damage());
		}
	}

	public static void attackJavelin(Unit attacker, Unit target) {
		Player.gc.javelin(attacker.id(), target.id());

		// Update health in cache
		if (markedEnemyHealth.containsKey(target)) {
			long defense = 0;
			if (target.unitType() == UnitType.Knight) {
				defense = target.knightDefense();
			}

			markedEnemyHealth.put(target, markedEnemyHealth.get(target) - attacker.damage() + defense);
		} else {
			markedEnemyHealth.put(target, target.health() - attacker.damage());
		}
	}


	public static void tryToOneShotWithRangers() {
		long start = System.currentTimeMillis();

		preferredRangerTargets.clear();

		List<Unit> friendlyRangers = Player.friendlyUnits.stream()
				.filter(unit -> (unit.unitType() == UnitType.Ranger && unit.location().isOnPlanet(Player.planet)
						&& !unit.location().isInGarrison())).collect(Collectors.toList());

		HashMap<Unit, ArrayList<Unit>> enemyToFriendlyRangers = new HashMap<>();

		// Use cache, but reverse it so that enemies are used
		for (Unit friendly : friendlyRangers) {
			VecUnit enemies = NearbyUnitsCache.getEnemiesInVisionRange(friendly);
			for (int i = 0; i < enemies.size(); i++) {
				Unit enemy = enemies.get(i);
				if (enemyToFriendlyRangers.containsKey(enemy)) {
					enemyToFriendlyRangers.get(enemy).add(friendly);
				} else {
					ArrayList<Unit> list = new ArrayList<>();
					list.add(friendly);
					enemyToFriendlyRangers.put(enemy, list);

				}
			}
		}

		ArrayList<Unit> enemyUnits = new ArrayList<>(enemyToFriendlyRangers.keySet());

		//Collections.shuffle(enemyUnits); // We go in random order to try to maximize optimality
		Collections.sort(enemyUnits, (o1, o2) ->
				Long.compare(CombatUtils.getTargetHealth(o1), CombatUtils.getTargetHealth(o2)));

		for (Unit enemy : enemyUnits) {
			List<Unit> friendliesInRange = enemyToFriendlyRangers.get(enemy);
			friendliesInRange = friendliesInRange.stream().filter(friendly ->
					(!preferredRangerTargets.containsKey(friendly)
							&& friendly.attackHeat() < Constants.MAX_ATTACK_HEAT
							&& Player.gc.canAttack(friendly.id(), enemy.id()))).collect(Collectors.toList());

			// If we can't one shot this unit, skip it, and let it be assigned normally.
			// Also skip if we're not really getting the benefit of collaborative one-shotting (i.e. the list of
			// friendlies is only 1).
			System.out.println(enemyToFriendlyRangers.size() + " enemies being considered");
			System.out.println(friendliesInRange.size() + " friendlies in range");
			if (friendliesInRange.size() < 2 ||
					CombatUtils.getTargetHealth(enemy) > friendliesInRange.size() * friendliesInRange.get(0).damage()) {
				continue;
			}

			// Sort by distance and assign in order of distance
			Collections.sort(friendliesInRange, (a, b) ->
					Long.compare(enemy.location().mapLocation().distanceSquaredTo(a.location().mapLocation()),
							enemy.location().mapLocation().distanceSquaredTo(b.location().mapLocation())));

			int index = 0;
			while (CombatUtils.getTargetHealth(enemy) > 0 && index < friendliesInRange.size()) {
				Unit friendly = friendliesInRange.get(index);
				preferredRangerTargets.put(friendliesInRange.get(index), enemy);
				System.out.println("Assigning " + friendly.id() + " to one-shot " + enemy.id());
				// TODO it might not be safe to just call attack here...
				CombatUtils.attack(friendly, enemy);
				index++;
			}
		}
		long end = System.currentTimeMillis();
		System.out.println("Took " + (end - start) + " milliseconds.");
	}

	public static boolean hasTriedToOneShot(Unit unit) {
		return preferredRangerTargets.containsKey(unit);
	}
}
