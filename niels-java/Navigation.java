import java.util.*;
import java.awt.Point;

import bc.*;

/*
 * TODO: Fix / make work
 */

public class Navigation {
	private int[][] distances; 
	private final PlanetMap map;
  private final Planet planet;
	private Set<Point> targets;

	private class Tuple<A, B> {
		public A x;
		public B y;

		public Tuple(A x, B y) {
			this.x = x;
			this.y = y;
		}
	}

	private static final Map<Direction, Point> createDirToDispMap() {
		EnumMap<Direction, Point> map = 
      new EnumMap<Direction, Point>(Direction.class);
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
		for(Direction dir : Direction.values()) {
			map.put(dirToDisp.get(dir), dir);
		}
		return map;
	}

	public static final Map<Point, Direction> dispToDir = createDispToDirMap();

	private static int getDispTowards(int start, int end) {
		if(start < end) {
			return 1;
		} else if(start == end) {
			return 0;
		}
		return -1;
	}

	public Direction directionTowards(MapLocation start, MapLocation end) {
		int dispX = getDispTowards(start.getX(), end.getX());
		int dispY = getDispTowards(start.getY(), end.getY());
		return dispToDir.get(new Point(dispX, dispY));
	}

	private Direction getDirection(int deltaX, int deltaY) {
		return dispToDir.get(new Point(deltaX, deltaY));
	}
	
	public void recalcDistanceMap() {
		Queue<MapLocation> queue = new ArrayDeque<>();
		for(Point target : targets) {
			distances[target.x][target.y] = 0;
		}
		while(!queue.isEmpty()) {
			MapLocation loc = queue.poll();
			int curDistance = distances[loc.getX()][loc.getY()];
			for(Direction dir : Direction.values()) {
				if(dir == Direction.Center) {
					continue;
				}
				Point disp = dirToDisp.get(dir);

				int newX = loc.getX() + disp.x;
				int newY = loc.getY() + disp.y;
				MapLocation newLocation = new MapLocation(planet, newX, newY);
				if(map.onMap(newLocation) && distances[newX][newY] > curDistance + 1) {
					distances[newX][newY] = curDistance + 1;
					queue.add(newLocation);
				}
			}
		}
	}

	private void initializeDistances(VecUnit units) {
		for(int i = 0; i < distances.length; i++) {
			for(int j = 0; j < distances[0].length; j++) {
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
	}


	public Direction getNextDirection(MapLocation start) {
    int minDist = Integer.MAX_VALUE;
    Direction next = Direction.Center;
    for(Direction dir : Direction.values()) {
      Point delta = dirToDisp.get(dir);
      int newX = start.getX();
      int newY = start.getY();
      MapLocation newLoc = new MapLocation(planet, newX, newY);
      if(map.onMap(newLoc) && distances[newX][newY] < minDist) {
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
}
