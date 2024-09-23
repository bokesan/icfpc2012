
public class ALiftOfLambdas {


	public static void main(String[] args) {

		MineMap map = new MineMap();
		Searcher search = new Searcher(map);
		Result res = search.search();
		System.out.println(res.steps);
		
	}

}
