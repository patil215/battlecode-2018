import bc.*;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.Queue;

public class Navigation {
	private int[][] distances;
	private final PlanetMap map;
	private final Planet planet;
	private Set<Point> targets;
	private int maxDistance;

	public void printDistances() {
		System.out.println(Player.gc.round());
		for(int i = 0; i < distances.length; i++) {
			for(int j = 0; j < distances[0].length; j++) {
				System.out.print(distances[i][j] + " ");
			}
			System.out.println();
		}
	}

	private static final Map<Direction, Point> createDirToDispMap() {
		EnumMap<Direction, Point> map = new EnumMap<Direction, Point>(Direction.class);
		map.put(Direction.Northeast, new Point(1, 1));
		map.put(Direction.East, new Point(1, 0));
		map.put(Direction.Southeast, new Point(1, -1));
		map.put(Direction.North, new Point(0, 1));
		map.put(Direction.Center, new Point(0, 0));
		map.put(Direction.South, new Point(0, -1));
		map.put(Direction.Northwest, new Point(-1, 1));
		map.put(Direction.West, new Point(-1, 0));
		map.put(Direction.Southwest, new Point(-1, -1));
		return map;
	}

	private static final Map<Direction, Point> dirToDisp = createDirToDispMap();

	private static Map<Point, Direction> createDispToDirMap() {
		HashMap<Point, Direction> map = new HashMap<>();
		for (Direction dir : Direction.values()) {
			map.put(dirToDisp.get(dir), dir);
		}
		return map;
	}

	public static final Map<Point, Direction> dispToDir = createDispToDirMap();

	public void recalcDistanceMap() {
		long start = System.currentTimeMillis();
		for (int i = 0; i < distances.length; i++) {
			for (int j = 0; j < distances[0].length; j++) {
				distances[i][j] = Integer.MAX_VALUE;
			}
		}
		Queue<MapLocation> queue = new ArrayDeque<>();
		for (Point target : targets) {
			distances[target.x][target.y] = 0;
			queue.add(new MapLocation(planet, target.x, target.y));
		}
		VecUnit units = Player.gc.myUnits();
		Set<Point> buildings = new HashSet<>();
		for(int i = 0; i < units.size(); i++) {
			Unit unit = units.get(i);
			UnitType type = unit.unitType();
			// TODO: go through factories
			if(type == UnitType.Rocket || type == UnitType.Factory) {
				MapLocation loc = unit.location().mapLocation();
				buildings.add(new Point(loc.getX(), loc.getY()));
			}
		}
		while (!queue.isEmpty()) {
			MapLocation loc = queue.poll();
			int curDistance = distances[loc.getX()][loc.getY()];
			for (Direction dir : Direction.values()) {
				if (dir == Direction.Center) {
					continue;
				}
				Point disp = dirToDisp.get(dir);

				int newX = loc.getX() + disp.x;
				int newY = loc.getY() + disp.y;
				MapLocation newLocation = new MapLocation(planet, newX, newY);
				if (map.onMap(newLocation) 
						&& map.isPassableTerrainAt(newLocation) == 1 
						&& !buildings.contains(new Point(newX, newY))
						&& distances[newX][newY] > curDistance + 1
						&& curDistance + 1 < maxDistance) {
					distances[newX][newY] = curDistance + 1;
					queue.add(newLocation);
				}
			}
		}
		long end = System.currentTimeMillis();
	}


	public Navigation(PlanetMap map, List<Point> targets, int maxDistance) {
		this.map = map;
		this.planet = map.getPlanet();
		this.maxDistance = maxDistance;
		this.distances = new int[(int) map.getWidth()][(int) map.getHeight()];
		this.targets = new HashSet<>(targets);
		recalcDistanceMap();
	}

	public Navigation(PlanetMap map, List<Point> targets) {
		this(map, targets, Integer.MAX_VALUE);
	}

	/**
	 * Returns a direction to move according to a start location.
	 *
	 * Tries all possible directions, returning the best one we can move towards.
	 *
	 * Null is returned if all adjacent squares are 'too far' (over threshold)
	 * or impossible to reach
	 */
	public Direction getNextDirection(Unit unit) {
		int minDist = Integer.MAX_VALUE;
		Direction next = null;
		MapLocation start = unit.location().mapLocation();
		List<Direction> dirs = Arrays.asList(Direction.values());
		Collections.shuffle(dirs);
		for (Direction dir : dirs) {
			Point delta = dirToDisp.get(dir);
			int newX = start.getX() + delta.x;
			int newY = start.getY() + delta.y;
			MapLocation newLoc = new MapLocation(planet, newX, newY);
			if (map.onMap(newLoc) && distances[newX][newY] < minDist
					&& Player.gc.canMove(unit.id(), dir)) {
				next = dir;
				minDist = distances[newX][newY];
			}
		}
		return next;
	}

	public void addTarget(MapLocation pos) {
		targets.add(new Point(pos.getX(), pos.getY()));
	}

	public void removeTarget(MapLocation pos) {
		targets.remove(new Point(pos.getX(), pos.getY()));
	}
	
	public Set<Point> getTargets() {
		return targets;
	}
}
