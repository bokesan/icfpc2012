import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;


public class Pathfinder {

    private static final int ROCK_COST = 100000;
    private static final int TRAMPOLINE_COST = 10000;


    private final MineMap map;

    private boolean verbose = false;

    public boolean isVerbose() {
        return verbose;
    }


    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }


    public Pathfinder(MineMap map) {
        this.map = map;
    }


    public List<Loc> computePath(Loc target) {
        Loc robot = new Loc(map.getPosN(), map.getPosM());
        return aStar(robot, target);
    }


    /**
     * A* algorithm straight from wikipedia.
     */
    private List<Loc> aStar(Loc start, Loc target) {
        int wildCapacityGuess = 3 * (map.n + map.m);
        Set<Loc> closedList = new HashSet<Loc>(wildCapacityGuess);
        Map<Loc, Loc> came_from = new HashMap<Loc, Loc>(wildCapacityGuess);
        start.sG = 0;
        ArrayOpenList openList = new ArrayOpenList(start, map.m * map.n);
        Loc[] neighbors = new Loc[5]; // use instead of list to avoid allocations
        for (;;) {
            Loc x = openList.extractMin(target);
            if (x == null) {
                if (verbose) {
                    System.err.println("* Path from " + start + " to "
                       + target + "(" + map.getContent(target.x, target.y) + "): no path found.");
                }
                return null; // failure
            }
            if (x.equals(target))
                break;
            closedList.add(x);
            getPossibleNeighbors(neighbors, x);
            for (int ni = 0; ; ni++) {
                Loc y = neighbors[ni];
                if (y == null)
                    break;
                if (closedList.contains(y))
                    continue;
                int cost;
                char c = map.getContent(y);
                if (isRock(c)) {
                    cost = ROCK_COST;
                } else if (isTrampolineTarget(c)) {
                    cost = TRAMPOLINE_COST;
                } else {
                    cost = 1;
                }
                double tentative_g_score = x.sG + cost;
                if (!openList.contains(y)) {
                    y.sG = tentative_g_score;
                    openList.add(y);
                    came_from.put(y, x);
                } else if (tentative_g_score < y.sG) {
                    openList.adjustGScore(y, tentative_g_score);
                    came_from.put(y, x);
                }
            }
        }
        // return path
        LinkedList<Loc> path = new LinkedList<Loc>();
        Loc n = new Loc(target.x, target.y);
        while (!n.equals(start)) {
            path.addFirst(n);
            n = came_from.get(n);
        }
        path.addFirst(n);
        try {
            assertPath(path);
        } catch (AssertionError e) {
            throw e;
        }

        if (verbose) {
            showPath(start, target, path);
        }
        return path;
    }


    private void showPath(Loc start, Loc target, LinkedList<Loc> path) {
        System.err.print("* Path from " + start + " to " + target + ":");
        for (Loc p : path)
            System.err.print(" " + p);
        System.err.println();
    }


    /**
     * Where can the bot move from x?
     */
    private void getPossibleNeighbors(Loc[] result, Loc x) {
        Loc t;
        int ri = 0;
        t = tryMove(x, x.down());  if (t != null) result[ri++] = t;
        t = tryMove(x, x.up());    if (t != null) result[ri++] = t;
        t = tryMove(x, x.left());  if (t != null) result[ri++] = t;
        t = tryMove(x, x.right()); if (t != null) result[ri++] = t;
        result[ri] = null;
    }

    private Loc tryMove(Loc x, Loc t) {
        char o = map.getContent(t);
        if (o == '#' || o == 'L' || isTrampolineTarget(o)) return null;

        if (isTrampoline(o)) {
            Loc dest = map.getTrampolineDestination(o);
            if (dest.isOrigin())
                throw new AssertionError("WTF!");
            return dest;
        }
        if (o == 'W')
            return null; // TODO

        if (isRock(o)) {
            if (x.y == t.y) {
                // can we move it?
                int delta = t.x - x.x;
                if (map.getContent(t.x + delta, t.y) != ' ')
                    return null; // nope
            } else {
                return null;
            }
        }
        return t;
    }


    private static boolean isTrampoline(char o) {
        return o >= 'A' && o <= 'I';
    }

    public static boolean isTrampolineTarget(char o) {
        return o >= '1' && o <= '9';
    }

    private static boolean isRock(char o) {
        return o == '*' || o == '@';
    }


    private void assertPath(List<Loc> path) {
        int n = path.size();
        assert n > 0;
        if (n > 2) {
            for (int i = 0; i < n-2; i++) {
                Loc n1 = path.get(i);
                Loc n2 = path.get(i+1);
                assert n1.sG < n2.sG;
            }
        }

    }

}
