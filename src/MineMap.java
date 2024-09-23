import java.io.*;


public class MineMap {

        /** Breite (Anzahl Spalten). */
        public final int n;
        /** Hoehe (Anzahl Zeilen). */
        public final int m;
        private byte[] map;
        public final int lambdasTotal;
        public int lambdasCollected = 0;
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
        private boolean completed = false;
        public int liftN;
        public int liftM;
        private String[] trampolines = new String[9];
        private String trampolineTargets;
        private String trampolineSources;
        private int[] trampTargetCoordsN = new int[9];
        private int[] trampTargetCoordsM = new int[9];
        private int[] trampSourceCoordsN = new int[9];
        private int[] trampSourceCoordsM = new int[9];
        private final int beardMax;
        private int razors;
        private int beardCount;

        /** Karte von stdin lesen. */
        MineMap() {
                int flooding = 0;
                int water = 0;
                int proof = 10;
                int beard = 25;
                int startrazors = 0;
                int countN = 0;
                int countM = 0;
                String mapString = "";
                try {
                        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
                        String str = "";
                        //map
                        boolean bottom = false;
                        int trampcount = 0;
                        while (in.ready()) {
                                str = in.readLine();
                                if (str.equals("")) bottom = true;
                                if (!bottom) {
                                        countM++;
                                        mapString += str + "\n";
                                        countN = Math.max(countN, str.length());
                                }
                                if (str.contains("Water ")) water = Integer.parseInt(str.substring(6).trim());
                                if (str.contains("Flooding ")) flooding = Integer.parseInt(str.substring(9).trim());
                                if (str.contains("Waterproof ")) proof = Integer.parseInt(str.substring(11).trim());
                                if (str.contains("Trampoline ")) {
                                        trampolines[trampcount] = str.substring(11, 12) + str.substring(21, 22);
                                        trampcount++;
                                }
                                if (str.contains("Growth ")) beard = Integer.parseInt(str.substring(7).trim());
                                if (str.contains("Razors ")) startrazors = Integer.parseInt(str.substring(7).trim());
                        }

                } catch (IOException e) {}
                this.startWater = water;
                this.waterLevel = water;
                this.waterProof = proof;
                this.floodSpeed = flooding;
                this.razors = startrazors;
                this.beardMax = beard;
                this.beardCount = beard -1;
                n = countN;
                m = countM;
                lambdasTotal = fillMap(mapString);
        }

        /** Kopier-Konstruktor. */
        MineMap(MineMap map) {
                this.startWater = map.startWater;
                this.waterLevel = map.startWater;
                this.waterProof = map.waterProof;
                this.floodSpeed = map.floodSpeed;
                this.moveCount = map.moveCount;
                this.underWaterCount = map.underWaterCount;
                this.trampolines = map.trampolines;
                this.n = map.n;
                this.m = map.m;
                this.lambdasTotal = map.lambdasTotal;
                this.lambdasCollected = map.lambdasCollected;
                this.beardMax = map.beardMax;
                this.beardCount = map.beardCount;
                this.razors = map.razors;
                this.trampolineSources = map.trampolineSources;
                this.trampolineTargets = map.trampolineTargets;
                this.trampSourceCoordsM = map.trampSourceCoordsM;
                this.trampSourceCoordsN = map.trampSourceCoordsN;
                this.trampTargetCoordsM = map.trampTargetCoordsM;
                this.trampTargetCoordsN = map.trampTargetCoordsN;
                this.map = new byte[n*m];
                System.arraycopy(map.map, 0, this.map, 0, m*n);
                this.positionM = map.positionM;
                this.positionN = map.positionN;
                this.liftM = map.liftM;
                this.liftN = map.liftN;
                this.finished = map.finished;
                this.completed = map.completed;
                this.crashed = map.crashed;
        }


        //mehrere Schritte
        public void move(String directions) {
            int n = directions.length();
            for (int i = 0; i < n; i++) {
                move(directions.charAt(i));
            }
        }

        //ein Schritt, inklusive Map Update
        public void move(char direction) {
                if (direction == 'A') {
                        finished = true;
                        return;
                }
                moveCount++;
                switch (direction) {
                case 'U':
                        if (!canMoveUp()) {reOrderMap(); return;}
                        set(positionN, positionM, ' ');
                        positionM++;
                        break;
                case 'D':
                        if (!canMoveDown()) {reOrderMap(); return;}
                        set(positionN, positionM, ' ');
                        positionM--;
                        break;
                case 'L':
                        if (!canMoveLeft()) {reOrderMap(); return;}
                        set(positionN, positionM, ' ');
                        positionN--;
                        break;
                case 'R':
                        if (!canMoveRight()) {reOrderMap(); return;}
                        set(positionN, positionM, ' ');
                        positionN++;
                        break;
                case 'W':
                    reOrderMap();
                    return;
                case 'S':
                    shave();
                    return;
                }

                char obj = get(positionN, positionM);
                switch (obj) {
                case '*':
                    if (direction == 'L') {
                        if (get(positionN-1, positionM) == ' ') {
                            set(positionN-1, positionM, '*');
                        } else {
                            set(positionN+1, positionM, 'R');
                            reOrderMap();
                            return;
                        }
                    }
                    else if (direction == 'R') {
                        if (get(positionN+1, positionM) == ' ') {
                            set(positionN+1, positionM, '*');
                        } else {
                            set(positionN-1, positionM, 'R');
                            reOrderMap();
                            return;
                        }
                    }
                    break;
                case '@':
                    if (direction == 'L') {
                        if (get(positionN-1, positionM) == ' ') {
                            set(positionN-1, positionM, '@');
                        } else {
                            set(positionN+1, positionM, 'R');
                            reOrderMap();
                            return;
                        }
                    }
                    else if (direction == 'R') {
                        if (get(positionN+1, positionM) == ' ') {
                            set(positionN+1, positionM, '@');
                        } else {
                            set(positionN-1, positionM, 'R');
                            reOrderMap();
                            return;
                        }
                    }
                    break;
                case '\\':
                    lambdasCollected++;
                    break;
                case 'O':
                    finished = true;
                    completed = true;
                    break;
                case '!':
                    razors++;
                    break;
                }

                int index = trampolineSources.indexOf(obj);
                if (index >= 0) {
                        positionN = trampTargetCoordsN[index];
                        positionM = trampTargetCoordsM[index];
                        for (String tramp : trampolines) {
                                if (!(tramp == null) && tramp.charAt(1) == get(positionN,positionM))
                                    set(trampSourceCoordsN[trampolineSources.indexOf(tramp.charAt(0))], trampSourceCoordsM[trampolineSources.indexOf(tramp.charAt(0))], ' ');
                        }
                }

                set(positionN, positionM, 'R');

                reOrderMap();
        }

        private void shave() {
            if (razors > 0) {
                razors--;
                for (int ox = -1; ox <= 1; ox++) {
                    for (int oy = -1; oy <= 1; oy++) {
                        if (get(positionN+ox, positionM+oy) == 'W')
                            set(positionN+ox, positionM+oy, ' ');
                    }
                }
            }
        }

        public Loc getTrampolineDestination(char t) {
            int index = trampolineSources.indexOf(t);
            int positionN = trampTargetCoordsN[index];
            int positionM = trampTargetCoordsM[index];
            return Loc.of(positionN, positionM);
        }

        public Loc getTrampolineDestination(Loc p) {
            char obj = getContent(p);
            if (obj >= 'A' && obj <= 'I')
                return getTrampolineDestination(obj);
            return null;
        }

        private void reOrderMap() {
                byte[] newmap = new byte[map.length];
                System.arraycopy(map, 0, newmap, 0, map.length);
                for (int m = 0; m < this.m; m++) {
                        for (int n = 0; n < this.n; n++) {
                            switch (get(n,m)) {
                            case '*':
                                rockFall(newmap, m, n);
                                break;
                            case '@':
                                horockFall(newmap, m, n);
                                break;
                            case 'L':
                                //Lift geht auf
                                if (isLiftOpen()) set(newmap, n, m, 'O');
                                break;
                            case 'W':
                                growBeard(newmap, m, n);
                                break;
                            }
                        }
                }

                if (beardCount == 0) beardCount = beardMax; else beardCount--;
                if (positionM < waterLevel) underWaterCount++; else underWaterCount = 0;
                if (underWaterCount > waterProof) crashed = true;
                if (floodSpeed > 0 && moveCount % floodSpeed == 0) waterLevel++;

                map = newmap;
        }

        private void horockFall(byte[] newmap, int m, int n) {
            //HO-Stein faellt
            if (get(n,m-1) == ' ') {
                    set(newmap, n, m, ' ');
                    set(newmap, n, m-1, (get(n,m-2) == ' ') ? '@' : '\\');
                    if (get(n,m-2) == 'R') crashed = true;
            }
            //HO-Stein faellt nach rechts
            if ((get(n,m-1) == '*' || get(n,m-1) == '@') && get(n+1,m) == ' ' && get(n+1,m-1) == ' ') {
                set(newmap, n, m, ' ');
                    set(newmap, n+1, m-1, (get(n+1,m-2) == ' ') ? '@' : '\\');
                    if (get(n+1,m-2) == 'R') crashed = true;
            }
            //HO-Stein faellt nach links
            if ((get(n,m-1) == '*' || get(n,m-1) == '@') && (get(n+1,m) != ' ' || get(n+1,m-1) != ' ') && get(n-1,m) == ' ' && get(n-1,m-1) == ' ') {
                set(newmap, n, m, ' ');
                    set(newmap, n-1, m-1, (get(n-1,m-2) == ' ') ? '@' : '\\');
                    if (get(n-1,m-2) == 'R') crashed = true;
            }
            //HO-Stein faellt ueber Lambda nach rechts
            if (get(n,m-1) == '\\' && get(n+1,m) == ' ' && get(n+1,m-1) == ' ') {
                set(newmap, n, m, ' ');
                set(newmap, n+1, m-1, (get(n+1,m-2) == ' ') ? '@' : '\\');
                if (get(n+1,m-2) == 'R') crashed = true;
            }
        }

        private void rockFall(byte[] newmap, int m, int n) {
            //Stein faellt
            if (get(n,m-1) == ' ') {
                    set(newmap, n, m, ' ');
                    set(newmap, n, m-1, '*');
                    if (get(n,m-2) == 'R') crashed = true;
            }
            //Stein faellt nach rechts
            if ((get(n,m-1) == '*' || get(n,m-1) == '@') && get(n+1,m) == ' ' && get(n+1,m-1) == ' ') {
                    set(newmap, n, m, ' ');
                    set(newmap, n+1, m-1, '*');
                    if (get(n+1,m-2) == 'R') crashed = true;
            }
            //Stein faellt nach links
            if ((get(n,m-1) == '*' || get(n,m-1) == '@') && (get(n+1,m) != ' ' || get(n+1,m-1) != ' ') && get(n-1,m) == ' ' && get(n-1,m-1) == ' ') {
                    set(newmap, n, m, ' ');
                    set(newmap, n-1, m-1, '*');
                    if (get(n-1,m-2) == 'R') crashed = true;
            }
            //Stein faellt ueber Lambda nach rechts
            if (get(n,m-1) == '\\' && get(n+1,m) == ' ' && get(n+1,m-1) == ' ') {
                    set(newmap, n, m, ' ');
                    set(newmap, n+1, m-1, '*');
                    if (get(n+1,m-2) == 'R') crashed = true;
            }
        }

        private void growBeard(byte[] newmap, int m, int n) {
            //Bartwachstum
            if (beardCount == 0) {
                for (int ox = -1; ox <= 1; ox++) {
                    for (int oy = -1; oy <= 1; oy++) {
                        if (get(n+ox,m+oy) == ' ')
                            set(newmap, n+ox, m+oy, 'W');
                    }
                }
            }
        }

        private int fillMap(String mapString) {
                String tt= "";
                String ts = "";

                for (String s : trampolines) {
                        if (!(s == null) && s.length() > 1) {
                                tt += s.substring(1);
                                ts += s.substring(0, 1);
                        }
                }
                trampolineTargets = tt;
                trampolineSources = ts;
                int lambdas = 0;
                map = new byte[n*m];
                int countn = 0;
                int countm = m - 1;
                for (char c : mapString.toCharArray()) {
                        if (c != '\n') {
                                set(countn, countm, c);
                                if (trampolineTargets.indexOf(c) != -1) {
                                    int index = 0;
                                    for (char t : trampolineTargets.toCharArray()) {
                                        if (t == c) {
                                            trampTargetCoordsN[index] = countn;
                                            trampTargetCoordsM[index] = countm;
                                        }
                                        index++;
                                    }
                                }
                                if (trampolineSources.indexOf(c) != -1) {
                                        int index = trampolineSources.indexOf(c);
                                        trampSourceCoordsN[index] = countn;
                                        trampSourceCoordsM[index] = countm;
                                }
                                if (c == 'R') {positionN = countn; positionM = countm;}
                                if (c == '\\') lambdas++;
                                if (c == '@') lambdas++;
                                if (c == 'L' || c == 'O') {
                                        liftN = countn;
                                        liftM = countm;
                                }
                                countn ++;
                        } else {
                                countm--;
                                countn = 0;
                        }
                }
                return lambdas;
        }


        private char get(int n, int m) {
                return (char) map[n + this.n * m];
        }

        public char getContent(int n, int m) {
                return (char) map[n + this.n * m];
        }

        private void set(int col, int row, char obj) {
            map[col + this.n * row] = (byte) obj;
        }

        private void set(byte[] map, int col, int row, char obj) {
            map[col + this.n * row] = (byte) obj;
        }

        public char getContent(Loc p) {
            return getContent(p.x, p.y);
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

        public boolean completed() {
                return completed;
        }

        public String[] getTrampolines() {
                return trampolines;
        }

        public String getTrampolineTargets() {
                return trampolineTargets;
        }

        public int getRazors() {
            return razors;
        }

        private boolean canMoveLeft() {
            char o = this.getContent(this.getPosN()-1, this.getPosM());
                if (o == '#') return false;
                if (o == 'W') return false;
                if (o == 'L') return false;
                if (o == '*' && this.getContent(this.getPosN()-2, this.getPosM()) != ' ') return false;
                if (o == '@' && this.getContent(this.getPosN()-2, this.getPosM()) != ' ') return false;
                if (trampolineTargets.indexOf(o) >= 0) return false;
                return true;
        }

        private boolean canMoveRight() {
            char o = this.getContent(this.getPosN()+1, this.getPosM());
                if (o == '#') return false;
                if (o == 'W') return false;
                if (o == 'L') return false;
                if (o == '*' && this.getContent(this.getPosN()+2, this.getPosM()) != ' ') return false;
                if (o == '@' && this.getContent(this.getPosN()+2, this.getPosM()) != ' ') return false;
                if (trampolineTargets.indexOf(o) >= 0) return false;
                return true;
        }

        private boolean canMoveUp() {
            char o = this.getContent(this.getPosN(), this.getPosM()+1);
                if (o == '*') return false;
                if (o == '@') return false;
                if (o == '#') return false;
                if (o == 'W') return false;
                if (o == 'L') return false;
                if (trampolineTargets.indexOf(o) >= 0) return false;
                return true;
        }

        private boolean canMoveDown() {
            char o = this.getContent(this.getPosN(), this.getPosM()-1);
                if (o == '*') return false;
                if (o == '@') return false;
                if (o == '#') return false;
                if (o == 'W') return false;
                if (o == 'L') return false;
                if (trampolineTargets.indexOf(o) >= 0) return false;
                return true;
        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder(n * m);

            for (int m = this.m - 1; m >= 0; m--) {
                for (int n = 0; n < this.n; n++) {
                    char c = getContent(n,m);
                    if (c >= ' ')
                        result.append(c);

                }
                result.append('\n');

            }
            return result.toString();
        }
}
