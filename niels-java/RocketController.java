import bc.*;

/**
 * General strategy:
 *
 - Past round 400, start building rockets
 - Rule: only build rockets if we have X number of factories being maintained, otherwise build factories
 - Once (round# < 500 && #rockets > 0):
 - Knowing rocket locations + capacity, pick X closest units (Maybe a little bigger than X in case some die) and tell them to run to rocket
 - All rockets take off at turn 749 lmao
 - Pick an axis, dump some rockets on one side, some on the other side
 */
public class RocketController {
	public static void moveRocket(Unit unit) {

		if (unit.structureIsBuilt() == 0) {
			return;
		}

		if (unit.structureGarrison().size() < 2) {
			return;
		}

		// Launch if about to flood
		if (Player.gc.round() >= 749) {
			tryToLaunch(unit);
			return;
		}

		// Launch if less than half
		if (unit.health() < (Constants.ROCKET_HEALTH / 2)) {
			tryToLaunch(unit);
			return;
		}

		// Launch if full
		if (unit.structureGarrison().size() == Constants.MAX_ROCKET_CAPACITY) {
			tryToLaunch(unit);
			return;
		}
	}

	private static void tryToLaunch(Unit unit) {
		MapLocation location = findValidLocation(unit);
		if (location != null) {
			Player.gc.launchRocket(unit.id(), location);
		}
	}

	private static MapLocation findValidLocation(Unit unit) {
		PlanetMap map = Player.gc.startingMap(Planet.Mars);
		for (int i = 0; i < 50; i++) {
			MapLocation proposedLocation = new MapLocation(Planet.Mars, (int) (Math.random() * map.getWidth()),
					(int) (Math.random() * map.getHeight()));
			if (Player.gc.canLaunchRocket(unit.id(), proposedLocation)) {
				return proposedLocation;
			}
		}
		return null;

	}
}
