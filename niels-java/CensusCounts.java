import java.util.HashMap;

public class CensusCounts {
	static int workerCount;
	private static HashMap<RobotMemory.WorkerMode, Integer> workerModeCounts = new HashMap<>();

	static void resetCounts() {
		workerCount = 0;

		for (RobotMemory.WorkerMode mode : RobotMemory.WorkerMode.values()) {
			workerModeCounts.put(mode, 0);
		}
	}

	static void incrementWorkerModeCount(RobotMemory.WorkerMode mode) {
		workerModeCounts.put(mode, workerModeCounts.get(mode) + 1);
	}

	static int getWorkerModeCount(RobotMemory.WorkerMode mode) {
		if (workerModeCounts.containsKey(mode)) {
			return workerModeCounts.get(mode);
		}
		return 0;
	}
}
