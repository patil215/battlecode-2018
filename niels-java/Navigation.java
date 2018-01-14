import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Queue;
import java.util.Stack;

import bc.*;

/*
 * TODO: Fix / make work
 */

public class Navigation {
	private static MapLocation[][] prevNodes;
	private static int[][] distances;
	static PlanetMap map;

	public Direction directionTowards(MapLocation start, MapLocation end) {
		if (start.getX() < end.getX() && start.getY() < end.getY()) {
			return Direction.Northeast;
		} else if (start.getX() < end.getX() && start.getY() == end.getY()) {
			return Direction.East;
		} else if (start.getX() < end.getX() && start.getY() > end.getY()) {
			return Direction.Southeast;
		} else if (start.getX() == end.getX() && start.getY() < end.getY()) {
			return Direction.North;
		} else if (start.getX() == end.getX() && start.getY() == end.getY()) {
			return Direction.Center;
		} else if (start.getX() == end.getX() && start.getY() > end.getY()) {
			return Direction.South;
		} else if (start.getX() > end.getX() && start.getY() < end.getY()) {
			return Direction.Northwest;
		} else if (start.getX() > end.getX() && start.getY() == end.getY()) {
			return Direction.West;
		} else if (start.getX() > end.getX() && start.getY() > end.getY()) {
			return Direction.Southwest;
		}
		return null;
	}

	private Direction getDirection(int deltaX, int deltaY) {
		if (deltaX == 1 && deltaY == 1) {
			return Direction.Northeast;
		} else if (deltaX == 1 && deltaY == 0) {
			return Direction.East;
		} else if (deltaX == 1 && deltaY == -1) {
			return Direction.Southeast;
		} else if (deltaX == 0 && deltaY == 1) {
			return Direction.North;
		} else if (deltaX == 0 && deltaY == -1) {
			return Direction.South;
		} else if (deltaX == -1 && deltaY == 1) {
			return Direction.Northwest;
		} else if (deltaX == -1 && deltaY == -0) {
			return Direction.West;
		} else if (deltaX == -1 && deltaY == -1) {
			return Direction.Southwest;
		} else {
			return Direction.Center;
		}
	}

	private static void bfsFromPoint(MapLocation start) {
		Queue<Tuple<MapLocation, Integer>> points = new ArrayDeque<Tuple<MapLocation, Integer>>();
		points.add(new Tuple<MapLocation, Integer>(start, 0));
		prevNodes = new MapLocation[(int) map.getWidth()][(int) map.getHeight()];
		distances = new int[(int) map.getWidth()][(int) map.getHeight()];
		while (points.size() > 0) {
			Tuple<MapLocation, Integer> current = points.remove();
			for (int deltaX = -1; deltaX <= 1; deltaX++) {
				for (int deltaY = -1; deltaY <= 1; deltaY++) {
					MapLocation location = new MapLocation(current.x.getPlanet(), current.x.getX() + deltaX,
							current.x.getY() + deltaY);
					if (deltaX == 0 && deltaY == 0) {
						continue;
					}
					if (map.onMap(location) && map.isPassableTerrainAt(location) != 0 && location.getX() > 0
							&& location.getY() > 0) {
						if (((distances[location.getX() - 1][location.getY() - 1] == 0)
								|| distances[location.getX() - 1][location.getY() - 1] > current.y + 1)) {

							points.add(new Tuple<MapLocation, Integer>(location, current.y + 1));
							distances[location.getX() - 1][location.getY() - 1] = current.y + 1;
							prevNodes[location.getX() - 1][location.getY() - 1] = current.x;
						}
					}
				}
			}
		}
	}

	public Navigation(PlanetMap map) {
		prevNodes = null;
		distances = null;
		this.map = map;
	}

	public static Deque<MapLocation> getPathToDest(MapLocation start, MapLocation end) {
		bfsFromPoint(start);
		Deque<MapLocation> path = new ArrayDeque<MapLocation>();
		MapLocation current = end;
		while (current.getX() != start.getX() || current.getY() != start.getY()) {
			if (prevNodes[current.getX() - 1][current.getY() - 1] == null) {
				System.out.println("No path found");
				return null;
			} else {
				path.push(current);
				current = prevNodes[current.getX() - 1][current.getY() - 1];
			}
		}
		System.out.println("Path found");
		return path;
	}
}
