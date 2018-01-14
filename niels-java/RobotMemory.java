import java.util.*;

import bc.*;
public class RobotMemory {
	public Deque<MapLocation> pathToTarget;
	public MapLocation currentTarget;
	public boolean reachedDest;
	
	public RobotMemory() {
		this.pathToTarget = new ArrayDeque<>();
		this.reachedDest = true;
		this.currentTarget = null;
	}
}
