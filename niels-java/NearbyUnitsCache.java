import bc.Unit;
import bc.VecUnit;

import java.util.HashMap;

public class NearbyUnitsCache {
	private static HashMap<Unit, VecUnit> nearbyEnemiesInSightRadius = new HashMap<>();

	public static void initializeAtStartOfTurn() {
		nearbyEnemiesInSightRadius.clear();
	}

	public static VecUnit getEnemiesInVisionRange(Unit self) {
		if (nearbyEnemiesInSightRadius.containsKey(self)) {
			return nearbyEnemiesInSightRadius.get(self);
		}
		VecUnit nearbyEnemies = Player.gc.senseNearbyUnitsByTeam(self.location().mapLocation(), self.visionRange(),
				Player.enemyTeam);
		nearbyEnemiesInSightRadius.put(self, nearbyEnemies);
		return nearbyEnemies;
	}
}
