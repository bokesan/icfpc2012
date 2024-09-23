import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class Searcher {

        private MineMap map;
        private int points = 0;
        private int maxPoints = 0;
        private int lambdas = 0;
        private String steps = "";
        private String bestSteps = "";
        private int maxSteps;
        private int[] visited;
        private int depth = 0;

        public static volatile boolean shutDown = false;
        public static boolean skipPathFinder = false;

        Searcher(MineMap map) {
                maxSteps = map.n * map.m;
                this.map = map;
                visited = new int[map.n * map.m];
                setVisited(map.getPosN(), map.getPosM(), 1);
        }

        Searcher(MineMap map, int points, int lambdas, String steps, int[] visited, int depth) {
                maxSteps = map.n * map.m;


                this.map = new MineMap(map);
                this.points = points;
                this.lambdas = lambdas;
                this.steps = steps;
                this.visited = new int[map.n * map.m];
                System.arraycopy(visited, 0, this.visited, 0, map.n * map.m);
                this.depth = depth;
        }

        private int getVisited(int col, int row) {
            return visited[col + map.n * row];
        }

        private void setVisited(int col, int row, int value) {
            visited[col + map.n * row] = value;
        }

        private void bumpVisited(int col, int row) {
            visited[col + map.n * row]++;
        }

        public Result search() {

                depth = maxSteps;

                //SIGINT registrieren
                Thread hook = new Thread() {
                    @Override
                    public void run()
                    {
                            Searcher.shutDown = true;
                            try {
                                    Thread.sleep(4000);
                            } catch (InterruptedException e) {}
                    }
                };
                Runtime.getRuntime().addShutdownHook(hook);


                Result best;

                MineMap savemap = new MineMap(map);
                Result resPath = searchOn();

                Searcher.skipPathFinder = true;
                Result resNoPath = searchOn();




                if (resNoPath.points > resPath.points) {
                    best = resNoPath;
                } else {
                    best = resPath;
                }


                while (true) {
                    Searcher.skipPathFinder = false;
                    map = new MineMap(savemap);
                    map.move(best.steps);
                    steps = best.steps;
                    points = best.points;
                    Result newTryPath = searchOn();
                    Searcher.skipPathFinder = true;
                    map = new MineMap(savemap);
                    map.move(best.steps);
                    steps = best.steps;
                    points = best.points;
                    Result newTryNoPath = searchOn();
                    Result newTry;
                    if (newTryPath.points > newTryNoPath.points) newTry = newTryPath; else newTry = newTryNoPath;
                    if (newTry.points > best.points) {
                        best = newTry;
                    } else {
                        break;
                    }
                }


                if (best.steps.length() == 0 || (!best.steps.substring(best.steps.length()-1).equals("A") && !best.completed)) {
                        best.steps += "A";
                        best.points += 25*best.lambdas;
                }
                if (!Searcher.shutDown)
                    Runtime.getRuntime().removeShutdownHook(hook);
                return best;
        }

        private Result searchOn() {
                if (Searcher.shutDown) return new Result(bestSteps, maxPoints, lambdas); //Zeit alle
                if (map.completed()) {
                        Result res = new Result(bestSteps, maxPoints, lambdas); //TODO: ordentlich aufs Ende pruefen
                        res.completed = true;
                        return res;
                }

                if (steps.length() == maxSteps-1 || steps.length() >= depth) { //maximale Schritte erreicht
                        return new Result(bestSteps, maxPoints, lambdas);
                }
                String dirs = getPossibleDirections();

                if (dirs.isEmpty()) { //keine moeglichen Wege mehr
                        return new Result(bestSteps, maxPoints, lambdas);
                }

                dirs = getBestDirections(dirs);
                Result best = new Result(bestSteps, maxPoints, lambdas);

                if (dirs.length() > 1 && dirs.startsWith("!")) {
                    for (char c : dirs.substring(1).toCharArray()) {
                        if (getPossibleDirections().indexOf(c) != -1) step(c); else break;
                    }
                    Result res = searchOn();
                    if (res.points > best.points) best = res;
                } else {
                    for (char c : dirs.toCharArray()) {
                        Searcher s = new Searcher(map, points, lambdas, steps, visited, depth);
                        s.step(c);
                        Result res = s.searchOn();
                        if (res.points > best.points) best = res;
                    }
                }

                return best;
        }

        public void step(char dir) {
                points--;
                if (map.lambdasLeft() == 0) {
                        setVisited(map.getPosN(), map.getPosM(), -5);
                } else {
                        bumpVisited(map.getPosN(), map.getPosM());
                }
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

                if (map.completed()) points += map.lambdasTotal * 50;

                if (points > maxPoints) {
                        maxPoints = points;
                        bestSteps = steps;
                }


        }

        private String getPossibleDirections() {
            StringBuilder m = new StringBuilder(4);
                if (canMoveLeft()) m.append('L');
                if (canMoveUp()) m.append('U');
                if (canMoveRight()) m.append('R');
                if (canMoveDown()) m.append('D');
                return m.toString();
        }

        private String getBestDirections(String dirs) {




                if (map.getWaterCount() >= map.waterProof-1) {//fast ertrunken
                        if (map.getWaterLevel() == map.getPosM() && (map.getMoveCount() % map.floodSpeed < map.floodSpeed -1)) { //koennen noch hoch fliehen
                                if (dirs.contains("U")) return "U"; else return "";
                        } else return ""; //muss abbrechen statt ertrinken
                }


                if (map.getRazors() > 0) {
                    if (map.getContent(map.getPosN()+1, map.getPosM()) == 'W' || map.getContent(map.getPosN(), map.getPosM()+1) == 'W' ||
                            map.getContent(map.getPosN()+1, map.getPosM()+1) == 'W' || map.getContent(map.getPosN()-1, map.getPosM()) == 'W' ||
                            map.getContent(map.getPosN(), map.getPosM()-1) == 'W' || map.getContent(map.getPosN()-1, map.getPosM()-1) == 'W' ||
                            map.getContent(map.getPosN()-1, map.getPosM()+1) == 'W' || map.getContent(map.getPosN()+1, map.getPosM()-1) == 'W') return "S";
                }

                dirs = dontVibrate(dirs);
                if (dirs.isEmpty()) return dirs;

                CannedPlanMatcher canner = new CannedPlanMatcher(map);
                //canner.setVerbose(true);
                String directions = canner.match();
                if (directions != null) {
                    return "!" + directions;
                }

                if (map.lambdasLeft() == 0) return toTheLift(dirs);

                directions = tryLambdas();
                if (!directions.isEmpty() && getPossibleDirections().indexOf(directions.charAt(0)) >= 0) return directions;

                directions = getLambdasNext(dirs);
                if (!directions.isEmpty()) return directions;
                directions = getLambdasNear(dirs);
                if (!directions.isEmpty()) return directions;
                directions = getLambdasFar(dirs);
                if (!directions.isEmpty()) return directions;

                String dir = "";
                int low = -1;
                int len = dirs.length();
                for (int k = 0; k < len; k++) {
                    char c = dirs.charAt(k);
                        switch (c) {
                        case 'L':
                                if (getVisited(map.getPosN()-1, map.getPosM()) < low || low == -1) {low = getVisited(map.getPosN()-1, map.getPosM()); dir = "L";}
                                break;
                        case 'R':
                                if (getVisited(map.getPosN()+1, map.getPosM()) < low || low == -1) {low = getVisited(map.getPosN()+1, map.getPosM()); dir = "R";}
                                break;
                        case 'U':
                                if (getVisited(map.getPosN(), map.getPosM()+1) < low || low == -1) {low = getVisited(map.getPosN(), map.getPosM()+1); dir = "U";}
                                break;
                        case 'D':
                                if (getVisited(map.getPosN(), map.getPosM()-1) < low || low == -1) {low = getVisited(map.getPosN(), map.getPosM()-1); dir = "D";}
                                break;
                        }
                }
                return dir;
        }

        private String tryLambdas() {
                if (Searcher.skipPathFinder) return "";
                final Loc robot = Loc.of(map.getPosN(), map.getPosM());

                // Liste der Lambdas
                List<Loc> lams = new ArrayList<Loc>();
                for (int c = 0; c < map.n; c++) {
                        for (int r = 0; r < map.m; r++) {
                                if (map.getContent(c, r) == '\\') {
                                        lams.add(new Loc(c,r));
                                }
                        }
                }

                Collections.sort(lams, new Comparator<Loc>() {
                    @Override
                    public int compare(Loc a, Loc b) {
                        int ma = robot.minStepsTo(a);
                        int mb = robot.minStepsTo(b);
                        return ma - mb;
                    }
                });

                Pathfinder pf = new Pathfinder(map);
                int minPathLen = Integer.MAX_VALUE;
                List<Loc> minPath = null;
                int k = 0;
                while (k < lams.size() && lams.get(k).minStepsTo(robot) < minPathLen) {
                    List<Loc> path = pf.computePath(lams.get(k));
                    int cost;
                    if (path != null && (cost = (int) path.get(path.size() - 2).sG) < minPathLen) {
                        minPathLen = cost;
                        minPath = path;
                    }
                    k++;
                }
                if (minPath != null) {
                    Loc next = minPath.get(1);
                    return directionTo(robot, next);
                }
                return "";
        }

        private String toTheLift(String dirs) {

                Pathfinder pf = new Pathfinder(map);


                Loc robot = new Loc(map.getPosN(), map.getPosM());
                Loc lift = new Loc(map.liftN, map.liftM);
                List<Loc> path = pf.computePath(lift);
                if (path != null && path.size() >= 2) {
                        Loc next = path.get(1);
                        return directionTo(robot, next);
                }

                String LR; String UD; int lrvalue; int udvalue;
                if (map.getPosN() > map.liftN) LR = "L"; else if (map.getPosN() == map.liftN) LR = "LR"; else LR = "R";
                if (map.getPosM() > map.liftM) UD = "U"; else if (map.getPosM() == map.liftM) UD = "UD"; else UD = "D";
                lrvalue = Math.abs(map.getPosN() - map.liftN);
                udvalue = Math.abs(map.getPosM() - map.liftM);
                LR = checkDirs(LR, dirs);
                UD = checkDirs(UD, dirs);


                if (lrvalue > udvalue && !LR.equals("")) return LR;
                else if (udvalue > lrvalue && !UD.equals("")) return UD;
                else if (udvalue == lrvalue && !(LR.equals("") && UD.equals(""))) return LR+UD;
                else return dirs;
        }

        private String directionTo(Loc robot, Loc next) {
                char obj = map.getContent(next);
            if (obj >= '1' && obj <= '9') {
                // trampolined
                if (next.equals(map.getTrampolineDestination(robot.up()))) return "U";
                if (next.equals(map.getTrampolineDestination(robot.down()))) return "D";
                if (next.equals(map.getTrampolineDestination(robot.left()))) return "L";
                if (next.equals(map.getTrampolineDestination(robot.right()))) return "R";
                throw new AssertionError("implementation problem: trampoline into space");
            }
                if (next.x > robot.x) return "R";
                if (next.x < robot.x) return "L";
                if (next.y > robot.y) return "U";
                return "D";
        }

        private String checkDirs(String dirs1, String dirs2) {
                String res = "";
                for (char c : dirs1.toCharArray()) {
                        if (dirs2.indexOf(c) != -1) res += c;
                }
                return res;
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
                        //pruefen, ob es sich um eine Falle handelt
                        if (map.getContent(map.getPosN(), map.getPosM()+2) == '*' && (map.getContent(map.getPosN()+1, map.getPosM()+1) == '#' || map.getContent(map.getPosN()+1, map.getPosM()+1) == '*') && (map.getContent(map.getPosN()-1, map.getPosM()+1) == '#' || map.getContent(map.getPosN()-1, map.getPosM()+1) == '*')) lNext = poss;
                }
                return lNext;
        }

        private String getLambdasNear(String poss) {
                String lNext = "";
                for (char c : poss.toCharArray()) {
                        switch (c) {
                        case 'U':
                                if (map.getPosM()+2 > map.m-1) break;
                                if ((map.getContent(map.getPosN(), map.getPosM()+2) == '\\' || map.getContent(map.getPosN()+1, map.getPosM()+1) == '\\' || map.getContent(map.getPosN()-1, map.getPosM()+1) == '\\')) lNext += c;
                                break;
                        case 'D':
                                if (map.getPosM()-2 < 1) break;
                                if ((map.getContent(map.getPosN(), map.getPosM()-2) == '\\' || map.getContent(map.getPosN()-1, map.getPosM()-1) == '\\' || map.getContent(map.getPosN()+1, map.getPosM()-1) == '\\')) lNext += c;
                                break;
                        case 'L':
                                if (map.getPosN()-2 < 1) break;
                                if ((map.getContent(map.getPosN()-2, map.getPosM()) == '\\' || map.getContent(map.getPosN()-1, map.getPosM()-1) == '\\' || map.getContent(map.getPosN()-1, map.getPosM()+1) == '\\'))lNext += c;
                                break;
                        case 'R':
                                if (map.getPosN()+2 > map.n-1) break;
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
                                if (map.getPosM()+3 > map.m-1) break;
                                if ((map.getContent(map.getPosN(), map.getPosM()+2) != '#' && map.getContent(map.getPosN(), map.getPosM()+2) != '*' && map.getContent(map.getPosN(), map.getPosM()+2) != 'L') && (map.getContent(map.getPosN(), map.getPosM()+3) == '\\' || map.getContent(map.getPosN()+1, map.getPosM()+2) == '\\' || map.getContent(map.getPosN()-1, map.getPosM()+2) == '\\')) lNext += c;
                                break;
                        case 'D':
                                if (map.getPosM()-3 < 1) break;
                                if ((map.getContent(map.getPosN(), map.getPosM()-2) != '#' && map.getContent(map.getPosN(), map.getPosM()-2) != '*' && map.getContent(map.getPosN(), map.getPosM()-2) != 'L') && (map.getContent(map.getPosN(), map.getPosM()-3) == '\\' || map.getContent(map.getPosN()-1, map.getPosM()-2) == '\\' || map.getContent(map.getPosN()+1, map.getPosM()-2) == '\\')) lNext += c;
                                break;
                        case 'L':
                                if (map.getPosN()-3 < 1) break;
                                if ((map.getContent(map.getPosN()-2, map.getPosM()) != '#' && map.getContent(map.getPosN()-2, map.getPosM()) != '*' && map.getContent(map.getPosN()-2, map.getPosM()) != 'L') && (map.getContent(map.getPosN()-3, map.getPosM()) == '\\' || map.getContent(map.getPosN()-2, map.getPosM()-1) == '\\' || map.getContent(map.getPosN()-2, map.getPosM()+1) == '\\')) lNext += c;
                                break;
                        case 'R':
                                if (map.getPosN()+3 > map.n-1) break;
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
                if (map.getContent(map.getPosN()-1, map.getPosM()) == 'W') return false;
                if (map.getContent(map.getPosN()-1, map.getPosM()) == 'L') return false;
                if (map.getContent(map.getPosN()-1, map.getPosM()) == '*' && map.getContent(map.getPosN()-2, map.getPosM()) != ' ') return false;
                if (map.getContent(map.getPosN()-1, map.getPosM()) == '@' && map.getContent(map.getPosN()-2, map.getPosM()) != ' ') return false;
                if (getVisited(map.getPosN()-1, map.getPosM()) == -5) return false;
                if (map.getTrampolineTargets().indexOf(map.getContent(map.getPosN()-1, map.getPosM())) != -1) return false;
                return true;
        }

        private boolean canMoveRight() {
                if (map.getContent(map.getPosN()+1, map.getPosM()) == '#') return false;
                if (map.getContent(map.getPosN()+1, map.getPosM()) == 'W') return false;
                if (map.getContent(map.getPosN()+1, map.getPosM()) == 'L') return false;
                if (map.getContent(map.getPosN()+1, map.getPosM()) == '*' && map.getContent(map.getPosN()+2, map.getPosM()) != ' ') return false;
                if (map.getContent(map.getPosN()+1, map.getPosM()) == '@' && map.getContent(map.getPosN()+2, map.getPosM()) != ' ') return false;
                if (getVisited(map.getPosN()+1, map.getPosM()) == -5) return false;
                if (map.getTrampolineTargets().indexOf(map.getContent(map.getPosN()+1, map.getPosM())) != -1) return false;
                return true;
        }

        private boolean canMoveUp() {
                if (map.getContent(map.getPosN(), map.getPosM()+1) == '@') return false;
                if (map.getContent(map.getPosN(), map.getPosM()+1) == '*') return false;
                if (map.getContent(map.getPosN(), map.getPosM()+1) == '#') return false;
                if (map.getContent(map.getPosN(), map.getPosM()+1) == 'W') return false;
                if (map.getContent(map.getPosN(), map.getPosM()+1) == 'L') return false;
                //fallender Stein ueber dir
                if (map.getPosM() + 3 < map.m && map.getContent(map.getPosN(), map.getPosM()+3) == '@' && map.getContent(map.getPosN(), map.getPosM()+2) == ' ' && map.getContent(map.getPosN(), map.getPosM()+1) == ' ') return false;
                if (map.getPosM() + 3 < map.m && map.getContent(map.getPosN(), map.getPosM()+3) == '*' && map.getContent(map.getPosN(), map.getPosM()+2) == ' ' && map.getContent(map.getPosN(), map.getPosM()+1) == ' ') return false;

                if (getVisited(map.getPosN(), map.getPosM()+1) == -5) return false;
                if (map.getTrampolineTargets().indexOf(map.getContent(map.getPosN(), map.getPosM()+1)) != -1) return false;
                return true;
        }

        private boolean canMoveDown() {
                if (map.getContent(map.getPosN(), map.getPosM()-1) == '*') return false;
                if (map.getContent(map.getPosN(), map.getPosM()-1) == '@') return false;
                if (map.getContent(map.getPosN(), map.getPosM()-1) == '#') return false;
                if (map.getContent(map.getPosN(), map.getPosM()-1) == 'W') return false;
                if (map.getContent(map.getPosN(), map.getPosM()-1) == 'L') return false;


                //oben ein Stein

                if (map.getContent(map.getPosN(), map.getPosM()+1) == '*') return false;
                if (map.getContent(map.getPosN(), map.getPosM()+1) == '@') return false;
                //oben rechts ein Stein
                if (map.getContent(map.getPosN(), map.getPosM()+1) == ' ' && map.getContent(map.getPosN()+1, map.getPosM()+1) == '*' && (map.getContent(map.getPosN()+1, map.getPosM()) == '*' || map.getContent(map.getPosN()+1, map.getPosM()) == '@') && (map.getContent(map.getPosN()+2, map.getPosM()) != ' ' || map.getContent(map.getPosN()+2, map.getPosM()+1) != ' ')) return false;
                if (map.getContent(map.getPosN(), map.getPosM()+1) == ' ' && map.getContent(map.getPosN()+1, map.getPosM()+1) == '@' && (map.getContent(map.getPosN()+1, map.getPosM()) == '*' || map.getContent(map.getPosN()+1, map.getPosM()) == '@') && (map.getContent(map.getPosN()+2, map.getPosM()) != ' ' || map.getContent(map.getPosN()+2, map.getPosM()+1) != ' ')) return false;
                //oben links ein Stein
                if (map.getContent(map.getPosN(), map.getPosM()+1) == ' ' && map.getContent(map.getPosN()-1, map.getPosM()+1) == '*' && (map.getContent(map.getPosN()-1, map.getPosM()) == '*' || map.getContent(map.getPosN()-1, map.getPosM()) == '\\' || map.getContent(map.getPosN()-1, map.getPosM()) == '@')) return false;
                if (map.getContent(map.getPosN(), map.getPosM()+1) == ' ' && map.getContent(map.getPosN()-1, map.getPosM()+1) == '@' && (map.getContent(map.getPosN()-1, map.getPosM()) == '*' || map.getContent(map.getPosN()-1, map.getPosM()) == '\\' || map.getContent(map.getPosN()-1, map.getPosM()) == '@')) return false;


                if (getVisited(map.getPosN(), map.getPosM()-1) == -5) return false;
                if (map.getTrampolineTargets().indexOf(map.getContent(map.getPosN(), map.getPosM()-1)) != -1) return false;
                return true;
        }

        public String visitedToString() {
                String result = "";
                for (int m = map.m - 1; m >= 0; m--) {
                        for (int n = 0; n < map.n; n++) {
                                result += "|" + getVisited(n, m);
                        }
                        result += '\n';
                }
                return result;
        }
}
