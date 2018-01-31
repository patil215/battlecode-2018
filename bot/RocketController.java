import bc.*;
import java.awt.Point;
import java.util.*;

/**
 * General strategy:
 *
 * - Past round 400, start building rockets
 * - Rule: only build rockets if we have X number of factories being maintained, otherwise build factories
 * - Once (round# < 500 && #rockets > 0): - Knowing rocket locations + capacity, pick X
 * closest units (Maybe a little bigger than X in case some die) and tell them to run to rocket
 * - All rockets take off at turn 749 lmao
 * - Pick an axis, dump some rockets on one side, some on the other side
 */
public class RocketController {

	public static void moveRocket(Unit unit) {
		if (Player.planet == Planet.Earth) {
			runEarthLogic(unit);
		} else if (Player.planet == Planet.Mars) {
			runMarsLogic(unit);
		}
	}

	private static void runEarthLogic(Unit unit) {
		if (unit.structureIsBuilt() == 0) {
			return;
		}

		// Launch if about to flood
		if (Player.gc.round() >= 749) {
			tryToLaunch(unit);
			return;
		}

		// Otherwise only launch if we have at least 1 unit
		if(unit.structureGarrison().size() < 1) {
			return;
		}

		// Launch if less than full health or if there is a nearby foe
		if (unit.health() < (Constants.MAX_ROCKET_HEALTH * (3.0 / 5.0))) {
			tryToLaunch(unit);
			return;
		}

		// Launch if full
		if (unit.structureGarrison().size() == unit.structureMaxCapacity() &&
				(Player.gc.round() % Constants.ROCKET_LAUNCH_INTERVAL == 0)) {
			tryToLaunch(unit);
			return;
				}
	}

	private static void runMarsLogic(Unit unit) {
		while (Utils.tryAndUnload(unit));
	}

	private static void tryToLaunch(Unit unit) {
		MapLocation location = findValidLocation(unit);
		if (location != null) {
			landingLocations.add(new Point(location.getX(), location.getY()));
			Player.gc.launchRocket(unit.id(), location);
		}
	}

	public static List<Point> validLocations = new ArrayList<>();
	static Set<Point> validLocationsSet = new HashSet<>();
	static Set<Point> landingLocations = new HashSet<>();

	static boolean setupCalled = false;
	public static void setup() {
		if(setupCalled) {
			return;
		}
		setupCalled = true;
		PlanetMap map = Player.gc.startingMap(Planet.Mars);
		for(int x = 0; x < map.getWidth(); x++) {
			for(int y = 0; y < map.getHeight(); y++) {
				MapLocation loc = new MapLocation(Planet.Mars, x, y);
				if(map.onMap(loc) && map.isPassableTerrainAt(loc) == 1) {
					validLocations.add(new Point(x, y));
				}
			}
		}
		validLocationsSet.addAll(validLocations);
	}

	private static Random rand = new Random();

	private static boolean allSurroundingFree(MapLocation location) {
		for (Direction dir : Direction.values()) {
			MapLocation surroundingLocation = location.add(dir);
			if (!validLocationsSet.contains(new Point(surroundingLocation.getX(), surroundingLocation.getY()))) {
				return false;
			}
		}
		return true;
	}

	private static MapLocation findValidLocation(Unit unit) {
		// Try ones with all squares free that we haven't landed on already
		for (int i = 0; i < 10; i++) {
			Point loc = validLocations.get(rand.nextInt(validLocations.size()));
			MapLocation proposedLocation = new MapLocation(Planet.Mars, loc.x, loc.y);
			Point proposedPoint = new Point(proposedLocation.getX(), proposedLocation.getY());
			if (!landingLocations.contains(proposedPoint)
					&& allSurroundingFree(proposedLocation)
					&& Player.gc.canLaunchRocket(unit.id(), proposedLocation)) {
				return proposedLocation;
			}
		}

		// Didn't work, try any now
		for (int i = 0; i < 10; i++) {
			Point loc = validLocations.get(rand.nextInt(validLocations.size()));
			MapLocation proposedLocation = new MapLocation(Planet.Mars, loc.x, loc.y);
			if (Player.gc.canLaunchRocket(unit.id(), proposedLocation)) {
				return proposedLocation;
			}
		}
		return null;

	}
}
