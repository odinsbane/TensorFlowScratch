package org.orangepalantir.tttplayers;

import org.orangepalantir.TTTLookupTable;

/**
 * Created by smithm3 on 02/05/18.
 */
public class RandomPlayer implements Player {
    private TTTLookupTable tttLookupTable;
    final int symbol;

    public RandomPlayer(TTTLookupTable tttLookupTable, int symbol) {
        this.tttLookupTable = tttLookupTable;
        this.symbol = symbol;
    }

    @Override
    public int getMove(int state) {
        int[] moves = tttLookupTable.getMoves(state, symbol);
        int possible = 0;
        for (int i = 0; i < 9; i++) {
            if (moves[i] > 0) {
                possible += 1;
            }
        }
        int c = tttLookupTable.ng.nextInt(possible);
        int t = 0;
        for (int i = 0; i < 9; i++) {
            if (moves[i] > 0) {
                if (t == c) {
                    return moves[i];
                } else {
                    t++;
                }
            }
        }
        throw new RuntimeException("Error in Random move generation!");
    }
}
