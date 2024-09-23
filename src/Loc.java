

/** Map location. */
public class Loc {

    public final int x;
    public final int y;

    /**
     * G score for A* search.
     */
    public double sG = -1;

    public Loc(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public boolean isOrigin() {
        return x == 0 && y == 0;
    }

    public static Loc of(int x, int y) {
        return new Loc(x, y);
    }

    public double getFValue(Loc target) {
        assert sG >= 0;
        return sG + distanceTo(target);
    }

    public Loc down() { return of(x,y-1); }
    public Loc up() { return of(x,y+1); }
    public Loc left() { return of(x-1,y); }
    public Loc right() { return of(x+1,y); }

    public Loc move(char dir) {
        switch (dir) {
        case 'L': return left();
        case 'R': return right();
        case 'U': return up();
        case 'D': return down();
        default:
            throw new AssertionError("unexpected direction: " + dir);
        }
    }

    /** Distance between two points. */
    public double distanceTo(Loc p) {
        int dx = x - p.x;
        int dy = y - p.y;
        return Math.sqrt(dx*dx + dy*dy);
    }

    /** Minimal number of steps from here to there. */
    public int minStepsTo(Loc p) {
        return Math.abs(x - p.x) + Math.abs(y - p.y);
    }

    @Override
    public int hashCode() {
        return 31 * x + y;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof Loc))
            return false;
        Loc other = (Loc) obj;
        return x == other.x && y == other.y;
    }

    @Override
    public String toString() {
        if (sG == -1)
            return "(" + x + "," + y + ")";
        return "(" + x + "," + y + "[" + sG + "])";
    }

    public boolean isNeighborOf(Loc e) {
        return (x == e.x && (y == e.y-1 || y == e.y+1)) ||
               (y == e.y && (x == e.x-1 || x == e.x+1));
    }


}
