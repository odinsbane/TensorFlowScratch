package org.orangepalantir.tttplayers;

import org.orangepalantir.TTTLookupTable;

/**
 * Created by smithm3 on 02/05/18.
 */
public class FuzzyPlayer implements Player {
    private TTTLookupTable tttLookupTable;
    final int symbol;

    public FuzzyPlayer(TTTLookupTable tttLookupTable, int symbol) {
        this.tttLookupTable = tttLookupTable;
        this.symbol = symbol;
    }

    @Override
    public int getMove(int state) {
        int[] moves = tttLookupTable.getMoves(state, symbol);
        double possible = 0;
        int any = 0;
        for (int i = 0; i < 9; i++) {
            if (moves[i] > 0) {
                possible += tttLookupTable.table[moves[i]];
                any++;
            }
        }

        if (any == 1) {
            for (int i = 0; i < 9; i++) {
                if (moves[i] > 0) {
                    return moves[i];
                }
            }
        } else {
            double f = possible * tttLookupTable.ng.nextDouble();
            possible = 0;
            for (int i = 0; i < 9; i++) {
                if (moves[i] > 0) {
                    possible += tttLookupTable.table[moves[i]];
                    if (possible >= f) {
                        return moves[i];
                    }
                }
            }
        }

        throw new RuntimeException("Error in Fuzzy Move Generation.");

    }
}
