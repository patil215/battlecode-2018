public class RobotMemory {

	public enum WorkerMode {
		HARVESTER,
		BUILD_FACTORIES,
		BUILD_ROCKETS
	}

	public WorkerMode workerMode = WorkerMode.BUILD_FACTORIES;
}
