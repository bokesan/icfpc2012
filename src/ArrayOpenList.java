


/**
 * Priority queue implemented as array.
 * <p>
 * Since we have a comparatively small number of nodes, performance should be ok.
 */
public class ArrayOpenList {

    private final Loc[] ns;
    
    private int size;
    
    public ArrayOpenList(Loc n, int maxCapacity) {
        ns = new Loc[maxCapacity];
        ns[0] = n;
        size = 1;
    }
    
    /**
     * @return Returns and removes the smallest element. Returns <code>null</code>
     * if the list is empty.
     */
    public Loc extractMin(Loc target) {
        int minIndex = -1;
        double minScore = Double.MAX_VALUE;
        for (int i = 0; i < size; i++) {
            Loc n = ns[i];
            if (n != null) {
                double score = n.getFValue(target);
                if (score < minScore) {
                    minIndex = i;
                    minScore = score;
                }
            }
        }
        if (minIndex < 0)
            return null;
        Loc n = ns[minIndex];
        ns[minIndex] = null;
        return n;
    }

    public void add(Loc n) {
        for (int i = 0; i < size; i++) {
            if (ns[i] == null) {
                ns[i] = n;
                return;
            }
        }
        ns[size++] = n;
    }
    
    public boolean contains(Loc n) {
        for (int i = 0; i < size; i++) {
            if (n.equals(ns[i]))
                return true;
        }
        return false;
    }
    
    public void adjustGScore(Loc n, double score) {
        n.sG = score;
    }

}
