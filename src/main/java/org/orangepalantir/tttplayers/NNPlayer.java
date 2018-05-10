package org.orangepalantir.tttplayers;

import org.orangepalantir.Network;
import org.orangepalantir.TTTLookupTable;
import org.orangepalantir.TTTNetworkLearning;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Created by smithm3 on 10/05/18.
 */
public class NNPlayer implements Player{
    TTTNetworkLearning ai;
    public NNPlayer(Path brain){
        try {
            Network n = Network.load(brain);
            ai = new TTTNetworkLearning(n);
        } catch (IOException e) {
            e.printStackTrace();
            ai = new TTTNetworkLearning(new Network(new int[]{9, 27, 18}));
        }
    }
    @Override
    public int getMove(int state) {
        double[] ds = getMoveSpace(state);
        double[] moves = ai.predict(ds);
        return getIntSpace(moves) + state;
    }

    /**
     *
     * @param state
     * @return
     */
    double[] getMoveSpace(int state){
        double[] ds= new double[9];
        for(int i = 0; i<9; i++){
            int pos = (state/ TTTLookupTable.threes[i])%3;
            if(pos!=0){
                ds[i] = pos==1?1:-1;
            }
        }
        return ds;
    }

    int getIntSpace(double[] move){
        double max = 0;
        int dex = -1;
        for(int i = 0; i<move.length; i++){
            if(move[i]>max){
                max = move[i];
                dex = i;
            }
        }



        if(dex<9){
            //add an x
            return TTTLookupTable.threes[dex];
        } else{
            return TTTLookupTable.threes[dex-9]*2;
        }
    }
}
