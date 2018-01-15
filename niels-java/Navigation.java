import bc.Direction;
import bc.MapLocation;
import bc.Planet;
import bc.PlanetMap;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.Queue;

public class Navigation {
	private int[][] distances;
	private final PlanetMap map;
	private final Planet planet;
	private Set<Point> targets;

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
		Queue<MapLocation> queue = new ArrayDeque<>();
		for (Point target : targets) {
			distances[target.x][target.y] = 0;
			queue.add(new MapLocation(planet, target.x, target.y));
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
				if (map.onMap(newLocation) && distances[newX][newY] > curDistance + 1) {
					distances[newX][newY] = curDistance + 1;
					queue.add(newLocation);
				}
			}
		}
	}

	private void initializeDistances() {
		for (int i = 0; i < distances.length; i++) {
			for (int j = 0; j < distances[0].length; j++) {
				distances[i][j] = Integer.MAX_VALUE;
			}
		}
		recalcDistanceMap();
	}

	public Navigation(PlanetMap map, List<Point> targets) {
		this.map = map;
		this.planet = map.getPlanet();
		this.distances = new int[(int) map.getWidth()][(int) map.getHeight()];
		this.targets = new HashSet<>(targets);
		initializeDistances();
	}

	public Direction getNextDirection(MapLocation start) {
		int minDist = Integer.MAX_VALUE;
		Direction next = Direction.Center;
		for (Direction dir : Direction.values()) {
			Point delta = dirToDisp.get(dir);
			int newX = start.getX() + delta.x;
			int newY = start.getY() + delta.y;
			MapLocation newLoc = new MapLocation(planet, newX, newY);
			if (map.onMap(newLoc) && distances[newX][newY] < minDist
					&& Player.gc.canMove(Player.gc.senseUnitAtLocation(start).id(), dir)) {
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
