import java.io.*;


public class MineMap {


	public final int n;
	public final int m;
	private char[][] map;
	public final int lambdasTotal;
	private int lambdasCollected = 0;
	private int positionN;
	private int positionM;
	public final int startWater;
	public final int floodSpeed;
	private int waterLevel;
	private int underWaterCount = 0;
	private int moveCount;
	public final int waterProof;
	private boolean finished = false;
	private boolean crashed = false;


	MineMap() { //Karte von stdin einlesen
		int flooding = 0;
		int water = 0;
		int proof = 10;
		int countN = 0;
		int countM = 0;
		String mapString = "";
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
			String str = "";
			//map
			while (in.ready()) {
				str = in.readLine();
				mapString += str + "\n";
				countN = Math.max(countN, str.length());
				countM++;
				if (str.contains("Water ")) water = Integer.parseInt(str.substring(6));
				if (str.contains("Flooding ")) water = Integer.parseInt(str.substring(9));
				if (str.contains("Waterproof ")) water = Integer.parseInt(str.substring(11));
			}
			
		} catch (IOException e) {}
		this.startWater = water;
		this.waterLevel = water;
		this.waterProof = proof;
		this.floodSpeed = flooding;
		n = countN;
		m = countM;
		lambdasTotal = fillMap(mapString);
	}

	//fertige Karte programmatisch reintun
	MineMap(String mapString,int countN, int countM, int startWater, int flooding, int waterproof, int moveCount, int underWaterCount) {
		this.startWater = startWater;
		this.waterLevel = startWater;
		this.waterProof = waterproof;
		this.floodSpeed = flooding;
		this.moveCount = moveCount;
		this.underWaterCount = underWaterCount;
		n = countN;
		m = countM;
		lambdasTotal = fillMap(mapString);
	}
	
	//mehrere Schritte
	public void move(String directions) {
		for (char c : directions.toCharArray()) {
			move(c);
		}
	}

	//ein Schritt, inklusive Map Update
	public void move(char direction) {
		if (direction == 'A') {
			finished = true;
			return;
		}
		moveCount++;
		if (direction == 'W') {
			reOrderMap();
			return;
		}
		map[positionN][positionM] = ' ';
		switch (direction) {
		case 'U':
			positionM++;
			break;
		case 'D':
			positionM--;
			break;
		case 'L':
			positionN--;
			break;
		case 'R':
			positionN++;
			break;
		}
		if (map[positionN][positionM] == '*') {
			if (direction == 'L') {
				if (map[positionN-1][positionM] == ' ') {
					map[positionN-1][positionM] = '*';
				} else {
					map[positionN+1][positionM] = 'R';
					reOrderMap();
					return;
				}
			}
			if (direction == 'R') {
				if (map[positionN+1][positionM] == ' ') {
					map[positionN+1][positionM] = '*';
				} else {
					map[positionN-1][positionM] = 'R';
					reOrderMap();
					return;
				}
			}
		}
		if (map[positionN][positionM] == '\\') lambdasCollected++;
		if (map[positionN][positionM] == 'O') finished = true;
		map[positionN][positionM] = 'R';
		reOrderMap();
	}

	private void reOrderMap() {
		char[][] newmap = new char[n][m];
		for (int m = this.m - 1; m >= 0; m--) {
			for (int n = 0; n < this.n; n++) {
				newmap[n][m] = map[n][m];
			}
		}
		for (int m = 1; m < this.m-1; m++) {
			for (int n = 1; n < this.n-1; n++) {
				//Stein fällt
				if (map[n][m] == '*' && map[n][m-1] == ' ') {
					newmap[n][m] = ' ';
					newmap[n][m-1] = '*';
					if (map[n][m-2] == 'R') crashed = true;
				}
				//Stein fällt nach links
				if (map[n][m] == '*' &&map[n][m-1] == '*' && map[n-1][m] == ' ' && map[n-1][m-1] == ' ') {
					newmap[n][m] = ' ';
					newmap[n-1][m-1] = '*';
					if (map[n-1][m-2] == 'R') crashed = true;
				}
				//Stein fällt nach recht
				if (map[n][m] == '*' && map[n][m-1] == '*' && (map[n-1][m] != ' ' || map[n-1][m-1] != ' ') && map[n+1][m] == ' ' && map[n+1][m-1] == ' ') {
					newmap[n][m] = ' ';
					newmap[n+1][m-1] = '*';
					if (map[n+1][m-2] == 'R') crashed = true;
				}
				//Stein fällt über Lambda nach rechts
				if (map[n][m] == '*' && map[n][m-1] == '\\' && map[n+1][m] == ' ' && map[n+1][m-1] == ' ') {
					newmap[n][m] = ' ';
					newmap[n+1][m-1] = '*';
					if (map[n+1][m-2] == 'R') crashed = true;
				}
				//Lift geht auf
				if (map[n][m] == 'L' && isLiftOpen()) {
					newmap[n][m] = 'O';
				}
			}
		}
		if (floodSpeed > 0 && moveCount % floodSpeed == 0) waterLevel++;
		if (positionM <= waterLevel) underWaterCount++; else underWaterCount = 0;
		if (underWaterCount > waterProof) crashed = true;

		map = newmap;
	}

	private int fillMap(String mapString) {
		int lambdas = 0;
		map = new char[n][m];
		int countn = 0;
		int countm = m - 1;
		for (char c : mapString.toCharArray()) {
			if (c != '\n') {
				map[countn][countm] = c;
				if (c == 'R') {positionN = countn; positionM = countm;}
				countn ++;
				if (c == '\\') lambdas++;
			} else {
				countm--;
				countn = 0;
			}
		}
		return lambdas;
	}


	public char getContent(int n, int m) {
		return map[n][m];
	}

	public int lambdasLeft() {
		return lambdasTotal - lambdasCollected;
	}

	public boolean isLiftOpen() {
		return lambdasCollected == lambdasTotal;
	}

	public int getPosN() {
		return positionN;
	}

	public int getPosM() {
		return positionM;
	}

	public int getWaterLevel() {
		return waterLevel;
	}

	public int getWaterCount() {
		return underWaterCount;
	}

	public boolean finished() {
		return finished;
	}

	public boolean crashed() {
		return crashed;
	}
	
	public int getMoveCount() {
		return moveCount;
	}

	@Override
	public String toString() {
		String result = "";
		for (int m = this.m - 1; m >= 0; m--) {
			for (int n = 0; n < this.n; n++) {
				result += map[n][m];
			}
			result += '\n';
		}
		return result;
	}
}
