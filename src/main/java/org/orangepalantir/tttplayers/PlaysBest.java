package org.orangepalantir.tttplayers;

import org.orangepalantir.TTTLookupTable;

/**
 * Created by smithm3 on 02/05/18.
 */
public class PlaysBest implements Player {
    private TTTLookupTable tttLookupTable;
    final int symbol;

    public PlaysBest(TTTLookupTable tttLookupTable, int s) {
        this.tttLookupTable = tttLookupTable;
        symbol = s;
    }

    @Override
    public int getMove(int state) {
        int[] moves = TTTLookupTable.getMoves(state, symbol);
        double best = -1;
        int dex = -1;
        int best_count = 0;
        for (int i = 0; i < 9; i++) {
            if (moves[i] > 0) {
                double p = tttLookupTable.table[moves[i]];
                if (p > best) {
                    best = p;
                    dex = i;
                } else if (p == best) {
                    best_count++;
                }
            }
        }
        if (best_count > 1) {
            int choice = tttLookupTable.ng.nextInt(best_count);
            int t = 0;
            for (int i = 0; i < 9; i++) {
                if (moves[i] > 0) {

                    double p = tttLookupTable.table[moves[i]];
                    if (p == best) {
                        if (choice == t) {
                            dex = i;
                            break;
                        } else {
                            t++;
                        }
                    }
                }
            }
        }
        if (dex < 0) {
            throw new RuntimeException("Couldnt Find best move");
        }
        return moves[dex];
    }
}
