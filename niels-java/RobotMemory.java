/**
 * Contains all state per each individual robot.
 */
public class RobotMemory {
	public WorkerController.Mode workerMode = WorkerController.Mode.BUILD_FACTORIES;
	public FactoryController.Mode factoryMode = FactoryController.Mode.PRODUCE;
	public RangerController.Mode rangerMode = RangerController.Mode.FIGHT_ENEMIES;
}
