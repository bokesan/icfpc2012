
public class Result {

	public String steps;
	public int points;
	public int lambdas;
	public boolean completed = false;
	
	Result(String steps, int points, int lambdas) {
		this.steps = steps;
		this.points = points;
		this.lambdas = lambdas;
	}
}
