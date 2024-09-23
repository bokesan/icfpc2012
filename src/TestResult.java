
public class TestResult {

	
	public static void main(String[] args) {

		MineMap map = new MineMap();
		String steps = args[0];
		map.move(steps);
		int lambdascore = (map.lambdasTotal - map.lambdasLeft()) * 25;
		int points = lambdascore  - map.getMoveCount();
		if (!map.crashed() && map.finished()) points += lambdascore;
		if (!map.crashed() && map.completed()) points += lambdascore;
		System.out.println(points);
	}
	
}
