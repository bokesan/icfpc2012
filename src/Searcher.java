
public class Searcher {

	private MineMap map;
	private int points = 0;
	private int maxPoints = 0;
	private int lambdas = 0;
	private String steps = "";
	private String bestSteps = "";
	private int maxSteps;
	private int[][] visited;
	private int depth = 0;

	Searcher(MineMap map) {
		maxSteps = map.n * map.m;
		this.map = map;
		visited = new int[map.n][map.m];
		for (int i = 0; i < map.n; i++) {
			for (int j = 0; j < map.m; j++) {
				visited[i][j] = 0;
			}
		}
		visited[map.getPosN()][map.getPosM()] = 1;
	}

	Searcher(MineMap map, int points, int lambdas, String steps, int[][] visited, int depth) {
		maxSteps = map.n * map.m;
		this.map = new MineMap(map.toString(), map.n, map.m, map.getWaterLevel(), map.floodSpeed, map.waterProof,map.getMoveCount(), map.getWaterCount());
		this.points = points;
		this.lambdas = lambdas;
		this.steps = steps;
		this.visited = new int[map.n][map.m];
		this.depth = depth;
		for (int i = 0; i < map.n; i++) {
			for (int j = 0; j < map.m; j++) {
				this.visited[i][j] = visited[i][j];
			}
		}
	}
	
	public static boolean shutDown = false;

	public Result search() {

		depth = maxSteps;
		
		//SIGINT registrieren
		Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
            	Searcher.shutDown = true;
            	try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {}
            }
        });
	
		Result res = searchOn();
		
		if (!res.steps.substring(res.steps.length()-1).equals("A")) {
			res.steps += "A";
			res.points += 25*res.lambdas;
		}
		return res;

	}

	private Result searchOn() {
		if (Searcher.shutDown) return new Result(bestSteps, maxPoints, lambdas); //Zeit alle
		
		if (steps.length() == maxSteps-1 || steps.length() >= depth) { //maximale Schritte erreicht
			return new Result(bestSteps, maxPoints, lambdas);
		}
		String dirs = getPossibleDirections();
		
		if (dirs.length() == 0) { //keine möglichen Wege mehr
			return new Result(bestSteps, maxPoints, lambdas);
		}

		dirs = getBestDirections(dirs);
		Result best = new Result(bestSteps, maxPoints, lambdas);

		for (char c : dirs.toCharArray()) {
			Searcher s = new Searcher(map, points, lambdas, steps, visited, depth);
			s.step(c);
			Result res = s.searchOn();
			if (res.points > best.points) best = res;
		}
		return best;
	}

	public void step(char dir) {
		points--;
		visited[map.getPosN()][map.getPosM()]++;
		steps += dir;
		int lambdas = map.lambdasLeft();
		map.move(dir);
		switch(dir) {
		case 'W':
			break;
		case 'L':
		case 'R':
		case 'U':
		case 'D':
			points += (lambdas - map.lambdasLeft()) * 25;
			this.lambdas += lambdas - map.lambdasLeft();
			break;			
		}

		if (points > maxPoints) {
			maxPoints = points;
			bestSteps = steps;
		}

	}

	private String getPossibleDirections() {
		String m = "";
		if (canMoveLeft()) m += 'L';
		if (canMoveUp()) m += 'U';
		if (canMoveRight()) m += 'R';
		if (canMoveDown()) m += 'D';
		return m;
	}

	private String getBestDirections(String dirs) {
		//TODO: komische Funktionen überarbeiten
		//TODO: Wasser ernsthaft berücksichtigen
		//TODO: Venrünftige Pfade finden
		
		if (map.getWaterCount() >= map.waterProof) {//fast ertrunken
			if (map.getWaterLevel() == map.getPosM() && (map.getMoveCount() % map.floodSpeed < map.floodSpeed -1)) { //können noch hoch fliehen
				if (dirs.contains("U")) return "U"; else return "";
			} else return ""; //muss abbrechen statt ertrinken
		}
		
		String directions = getLambdasNext(dirs);
		if (directions.length() > 0) return directions;
		directions = getLambdasNear(dirs);
		if (directions.length() > 0) return directions;
		directions = getLambdasFar(dirs);
		if (directions.length() > 0) return directions;

		dirs = dontVibrate(dirs);

		String dir = "";
		int low = -1;
		for (char c : dirs.toCharArray()) {
			switch (c) {
			case 'L':
				if (visited[map.getPosN()-1][map.getPosM()] < low || low == -1) {low = visited[map.getPosN()-1][map.getPosM()]; dir = "L";}
				break;
			case 'R':
				if (visited[map.getPosN()+1][map.getPosM()] < low || low == -1) {low = visited[map.getPosN()+1][map.getPosM()]; dir = "R";}
				break;
			case 'U':
				if (visited[map.getPosN()][map.getPosM()+1] < low || low == -1) {low = visited[map.getPosN()][map.getPosM()+1]; dir = "U";}
				break;
			case 'D':
				if (visited[map.getPosN()][map.getPosM()-1] < low || low == -1) {low = visited[map.getPosN()][map.getPosM()-1]; dir = "D";}
				break;			
			}
		}
		return dir;
	}

	private String getLambdasNext(String poss) {
		String lNext = "";
		for (char c : poss.toCharArray()) {
			switch (c) {
			case 'U':
				if (map.getContent(map.getPosN(), map.getPosM()+1) == '\\') lNext += c;
				break;
			case 'D':
				if (map.getContent(map.getPosN(), map.getPosM()-1) == '\\') lNext += c;
				break;
			case 'L':
				if (map.getContent(map.getPosN()-1, map.getPosM()) == '\\') lNext += c;
				break;
			case 'R':
				if (map.getContent(map.getPosN()+1, map.getPosM()) == '\\') lNext += c;
				break;
			}
		}
		if (lNext == "U") {
			//prüfen, ob es sich um eine Falle handelt
			if (map.getContent(map.getPosN(), map.getPosM()+2) == '*' && (map.getContent(map.getPosN()+1, map.getPosM()+1) == '#' || map.getContent(map.getPosN()+1, map.getPosM()+1) == '*') && (map.getContent(map.getPosN()-1, map.getPosM()+1) == '#' || map.getContent(map.getPosN()-1, map.getPosM()+1) == '*')) lNext = poss;
		}
		return lNext;
	}

	private String getLambdasNear(String poss) {
		String lNext = "";
		for (char c : poss.toCharArray()) {
			switch (c) {
			case 'U':
				if ((map.getContent(map.getPosN(), map.getPosM()+2) == '\\' || map.getContent(map.getPosN()+1, map.getPosM()+1) == '\\' || map.getContent(map.getPosN()-1, map.getPosM()+1) == '\\')) lNext += c;
				break;
			case 'D':
				if ((map.getContent(map.getPosN(), map.getPosM()-2) == '\\' || map.getContent(map.getPosN()-1, map.getPosM()-1) == '\\' || map.getContent(map.getPosN()+1, map.getPosM()-1) == '\\')) lNext += c;
				break;
			case 'L':
				if ((map.getContent(map.getPosN()-2, map.getPosM()) == '\\' || map.getContent(map.getPosN()-1, map.getPosM()-1) == '\\' || map.getContent(map.getPosN()-1, map.getPosM()+1) == '\\'))lNext += c;
				break;
			case 'R':
				if ((map.getContent(map.getPosN()+2, map.getPosM()) == '\\' || map.getContent(map.getPosN()+1, map.getPosM()+1) == '\\' || map.getContent(map.getPosN()+1, map.getPosM()-1) == '\\'))lNext += c;
				break;
			}
		}
		return lNext;
	}

	private String getLambdasFar(String poss) {
		String lNext = "";
		for (char c : poss.toCharArray()) {
			switch (c) {
			case 'U':
				if ((map.getContent(map.getPosN(), map.getPosM()+2) != '#' && map.getContent(map.getPosN(), map.getPosM()+2) != '*' && map.getContent(map.getPosN(), map.getPosM()+2) != 'L') && (map.getContent(map.getPosN(), map.getPosM()+3) == '\\' || map.getContent(map.getPosN()+1, map.getPosM()+2) == '\\' || map.getContent(map.getPosN()-1, map.getPosM()+2) == '\\')) lNext += c;
				break;
			case 'D':
				if ((map.getContent(map.getPosN(), map.getPosM()-2) != '#' && map.getContent(map.getPosN(), map.getPosM()-2) != '*' && map.getContent(map.getPosN(), map.getPosM()-2) != 'L') && (map.getContent(map.getPosN(), map.getPosM()-3) == '\\' || map.getContent(map.getPosN()-1, map.getPosM()-2) == '\\' || map.getContent(map.getPosN()+1, map.getPosM()-2) == '\\')) lNext += c;
				break;
			case 'L':
				if ((map.getContent(map.getPosN()-2, map.getPosM()) != '#' && map.getContent(map.getPosN()-2, map.getPosM()) != '*' && map.getContent(map.getPosN()-2, map.getPosM()) != 'L') && (map.getContent(map.getPosN()-3, map.getPosM()) == '\\' || map.getContent(map.getPosN()-2, map.getPosM()-1) == '\\' || map.getContent(map.getPosN()-2, map.getPosM()+1) == '\\')) lNext += c;
				break;
			case 'R':
				if ((map.getContent(map.getPosN()+2, map.getPosM()) != '#' && map.getContent(map.getPosN()+2, map.getPosM()) != '*' && map.getContent(map.getPosN()+2, map.getPosM()) != 'L') && (map.getContent(map.getPosN()+3, map.getPosM()) == '\\' || map.getContent(map.getPosN()+2, map.getPosM()+1) == '\\' || map.getContent(map.getPosN()+2, map.getPosM()-1) == '\\') ) lNext += c;
				break;
			}
		}
		return lNext;
	}

	private String dontVibrate(String poss) {
		if (steps.length() < 2) return poss;
		String old = steps.substring(steps.length()-2);
		String newPoss = "";
		for (char c : poss.toCharArray()) {
			switch (c) {
			case 'U':
				if (!old.equals("UD")) newPoss += 'U';
				break;
			case 'D':
				if (!old.equals("DU")) newPoss += 'D';
				break;
			case 'L':
				if (!old.equals("LR")) newPoss += 'L';
				break;
			case 'R':
				if (!old.equals("RL")) newPoss += 'R';
				break;
			}
		}
		if (steps.length() < 3) return newPoss;
		old = steps.substring(steps.length()-3);
		String newNewPoss = "";
		for (char c : newPoss.toCharArray()) {
			switch (c) {
			case 'U':
				if (!old.equals("LDR") && !old.equals("RDL") && !old.equals("UDD")) newNewPoss += 'U';
				break;
			case 'D':
				if (!old.equals("LUR") && !old.equals("RUL") && !old.equals("DUU")) newNewPoss += 'D';
				break;
			case 'L':
				if (!old.equals("DRU") && !old.equals("URD") && !old.equals("LRR")) newNewPoss += 'L';
				break;
			case 'R':
				if (!old.equals("ULD") && !old.equals("DLU") && !old.equals("RLL")) newNewPoss += 'R';
				break;
			}
		}
		return newNewPoss;
	}

	private boolean canMoveLeft() {
		if (map.getContent(map.getPosN()-1, map.getPosM()) == '#') return false;
		if (map.getContent(map.getPosN()-1, map.getPosM()) == 'L') return false;
		if (map.getContent(map.getPosN()-1, map.getPosM()) == '*' && map.getContent(map.getPosN()-2, map.getPosM()) != ' ') return false;
		return true;
	}

	private boolean canMoveRight() {
		if (map.getContent(map.getPosN()+1, map.getPosM()) == '#') return false;
		if (map.getContent(map.getPosN()+1, map.getPosM()) == 'L') return false;
		if (map.getContent(map.getPosN()+1, map.getPosM()) == '*' && map.getContent(map.getPosN()+2, map.getPosM()) != ' ') return false;
		return true;
	}

	private boolean canMoveUp() {
		if (map.getContent(map.getPosN(), map.getPosM()+1) == '*') return false;
		if (map.getContent(map.getPosN(), map.getPosM()+1) == '#') return false;                    
		if (map.getContent(map.getPosN(), map.getPosM()+1) == 'L') return false;
		//fallender Stein über dir
		if (map.getPosM() + 3 < map.m && map.getContent(map.getPosN(), map.getPosM()+3) == '*' && map.getContent(map.getPosN(), map.getPosM()+2) == ' ' && map.getContent(map.getPosN(), map.getPosM()+1) == ' ') return false;
		return true;
	}

	private boolean canMoveDown() {
		if (map.getContent(map.getPosN(), map.getPosM()-1) == '*') return false;
		if (map.getContent(map.getPosN(), map.getPosM()-1) == '#') return false;
		if (map.getContent(map.getPosN(), map.getPosM()-1) == 'L') return false;
		if (map.getContent(map.getPosN(), map.getPosM()+1) == '*') return false;
		//oben ein Stein
		if (map.getContent(map.getPosN(), map.getPosM()+1) == ' ' && map.getContent(map.getPosN()+1, map.getPosM()+1) == '*' && map.getContent(map.getPosN()+1, map.getPosM()) == '*') return false;
		//oben rechts ein Stein
		if (map.getContent(map.getPosN(), map.getPosM()+1) == ' ' && map.getContent(map.getPosN()-1, map.getPosM()+1) == '*' && (map.getContent(map.getPosN()-1, map.getPosM()) == '*' || map.getContent(map.getPosN()-1, map.getPosM()) == '\\') && map.getContent(map.getPosN()-2, map.getPosM()) != ' ') return false;
		//oben links ein Stein
		return true;
	}

	public String visitedToString() {
		String result = "";
		for (int m = map.m - 1; m >= 0; m--) {
			for (int n = 0; n < map.n; n++) {
				result += "|"+visited[n][m];
			}
			result += '\n';
		}
		return result;
	}
}
