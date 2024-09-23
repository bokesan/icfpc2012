/**
 * Fertige Kartenstuecke und Loesungspfade.
 */
public class CannedPlanMatcher {

    private final MineMap map;
    private boolean verbose = false;

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public CannedPlanMatcher(MineMap map) {
        this.map = map;
    }



    /**
     * Gegen alle vordefinierten Karten matchen, und den Plan des ersten Treffers zurückgeben.
     * @return Befehlsfolge, oder {@code null} falls keine Karte gematched wurde.
     */
    public String match() {
        for (Plan p : PLANS) {
            String s = p.match(map);
            if (s != null) {
                if (verbose) {
                    System.err.println("Matched canned plan " + p.name);
                }
                return s;
            }
        }
        return null;
    }



    public static class Plan {

        public final int width;
        public final int height;
        public final int robotX;
        public final int robotY;
        public final String name;
        public final char[][] map;

        public final String solution;

        public Plan(String name, String[] map, String solution) {
            this.name = name;
            width = map[0].length();
            height = map.length;
            this.map = new char[width][height];
            int rx = 0;
            int ry = 0;
            for (int c = 0; c < width; c++) {
                for (int r = 0; r < height; r++) {
                    char obj = map[height - r - 1].charAt(c);
                    if (obj == '/')
                        obj = '\\';
                    this.map[c][r] = obj;
                    if (obj == 'R') {
                        rx = c;
                        ry = r;
                    }
                }
            }
            robotX = rx;
            robotY = ry;
            this.solution = solution;
        }

        public String match(MineMap m) {
            int xoff = m.getPosN() - robotX;
            int yoff = m.getPosM() - robotY;
            if (xoff < 0 || yoff < 0 || xoff + width > m.n || yoff + height > m.m)
                return null;
            for (int c = 0; c < width; c++) {
                for (int r = 0; r < height; r++) {
                    char expected = this.map[c][r];
                    char actual = m.getContent(c+xoff, r+yoff);
                    switch (expected) {
                    case '?':
                        if (!(actual == ' ' || actual == '.' || actual == '\\'))
                            return null;
                        break;
                    case '*':
                        if (!(actual == '*' || actual == '@'))
                            return null;
                        break;
                    case '_':
                        break;
                    case '=':
                        if (!Pathfinder.isTrampolineTarget(actual))
                            return null;
                        break;
                    default:
                        if (actual != expected)
                            return null;
                        break;
                    }
                }
            }
            return solution;
        }

    }


    private static final Plan[] PLANS = {
        new Plan("ROCK BARRIER ABOVE", new String[]{ "#*#", "R.?" }, "RRL"),
        new Plan("ROCK BARRIER ABOVE", new String[]{ "#*#", "?R?" }, "RL"),
        new Plan("ROCK BARRIER ABOVE", new String[]{ "#*#", "?.R" }, "LRL"),
        new Plan("ROCK BARRIER ABOVE2", new String[]{ "#*#", "?.?", "R??" }, "RURL"),
        new Plan("ROCK BARRIER ABOVE2", new String[]{ "#*#", "?.?", "?R?" }, "URL"),
        new Plan("ROCK BARRIER ABOVE2", new String[]{ "#*#", "?.?", "??R" }, "LURL"),

        new Plan("trampoline3 - We should not do this 1", new String[]{
              "?*?",
              "*=*",
              "...",
              "??R"
        }, "UL"),

        new Plan("PATH FIX 1", new String[]{
                "????*",
                "?**R*",
                "?...."
            }, "ULLLDDRRR"),

        new Plan("BEARD 1 FIX", new String[]{
                "_**?",
                "#?R?",
                "_?/?"
        }, "R")

    };

}
