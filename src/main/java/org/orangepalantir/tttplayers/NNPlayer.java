package org.orangepalantir.tttplayers;

import org.orangepalantir.Network;
import org.orangepalantir.TTTLookupTable;
import org.orangepalantir.TTTNetworkLearning;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by smithm3 on 10/05/18.
 */
public class NNPlayer implements Player{
    TTTNetworkLearning ai;
    Map<Integer, double[]> playedMoves = new TreeMap<>();
    Map<Integer, List<double[]>> valuedMoves = new HashMap<>();
    int count = 0;
    Path brain;
    Random ng = new Random();
    public NNPlayer(Path brain){
        this.brain = brain;

        try {
            Network n = Network.load(brain);
            ai = new TTTNetworkLearning(n);
        } catch (IOException e) {
            e.printStackTrace();
            ai = new TTTNetworkLearning(new Network(new int[]{9, 64, 64,  18}));
        }
    }
    @Override
    public int getMove(int state) {
        double[] ds = getMoveSpace(state);



        double[] moves = ai.predict(ds);
        double[] possible = ai.getPossible(ds);

        int dex = -1;
        double max = 0;
        int count = 0;
        for(int i = 0; i<moves.length; i++){
            moves[i] = moves[i]*possible[i];
            if(moves[i]>max){
                dex = i;
                max = moves[i];
                count = 1;
            } else if(moves[i]>0 && moves[i]==max){
                count ++;
            }
        }



        if(dex<0){
            System.out.println("no valid moves");
            for(int i = 0; i<possible.length; i++){
                if(possible[i]==1){
                    dex = i;
                    break;
                }
            }
        } else if (count>1){
            int step = ng.nextInt(count);
            int j = 0;
            for(int i = dex; i<moves.length; i++){
                if(moves[i]==max) j++;
                if(j==step){
                    dex = i;
                    break;
                }
            }

        }

        int goodMove;
        if(dex>8){
            goodMove = 2*TTTLookupTable.threes[dex - 9];
        } else{
            goodMove = TTTLookupTable.threes[dex];
        }
        for(int i = 0; i<moves.length; i++){
            if(i!=dex){
                moves[i] = 0;
            }else{
                moves[i] = 1;
            }
        }

        playedMoves.put(state, moves);
        return goodMove + state;
    }

    /**
     * Given the state of the board, this returns a 9 element double that contains a -1 for o a 1 for x and 0 for empty.
     *
     * @param state integer state that is broken up using power of 3 bins.
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

    /**
     * Grabs the best chosen move and refines the double[].
     * @param move
     * @return
     */
    int getIntSpace(double[] move){
        int dex = -1;
        double max = 0;
        for(int i = 0; i<move.length; i++){
                if(move[i]>max){
                    dex = i;
                    max = move[i];
                }
        }

        for(int i = 0; i<move.length; i++){
            move[i] = 0;
            if(i==dex) move[i] = 1;
        }

        if(dex<9){
            //add an x
            return TTTLookupTable.threes[dex];
        } else{
            return TTTLookupTable.threes[dex-9]*2;
        }
    }

    @Override
    public void finished(int result){
        double rate = 0.9;
        double min = 0.001;
        if(result == 1 || result==0){
            double value = 1;
            for(Integer state: playedMoves.keySet()){
                if(valuedMoves.containsKey(state)){
                    List<double[]> pair = valuedMoves.get(state);
                    double[] played = playedMoves.get(state);
                    double[] prev = pair.get(1);
                    for(int i = 0; i<18; i++){
                        if(played[i]!=0){
                            prev[i] = prev[i] + rate*(value - prev[i]);
                            value = prev[i];
                        }
                    }
                } else{
                    double[] dstate = new double[9];
                    for(int i = 0; i<9; i++){
                        int p = (state/TTTLookupTable.threes[i])%3;
                        if(p==1){
                            dstate[i] = 1;
                        } else if(p==2){
                            dstate[i] = -1;
                        }
                    }
                    double[] moves  = ai.getPossible(dstate);
                    double[] played = playedMoves.get(state);
                    for(int i = 0; i<18; i++){
                        if(played[i]==1){
                            moves[i] = moves[i] + rate*(value - moves[i]);
                            value = moves[i];
                        }
                    }
                    valuedMoves.put(state, Arrays.asList(dstate, moves));


                }
            }
        } else if(result==-1){
            count = 0;
            //lost
            for(Integer state: playedMoves.keySet()){
                double value = 0;
                if(valuedMoves.containsKey(state)){
                    List<double[]> pair = valuedMoves.get(state);
                    double[] played = playedMoves.get(state);
                    double[] prev = pair.get(1);
                    for(int i = 0; i<18; i++){
                        if(played[i]!=0){
                            // still legal, but not good.
                            prev[i] = prev[i] + rate*(value - prev[i]);
                            value = prev[i];
                        }
                    }
                } else{
                    double[] dstate = new double[9];
                    for(int i = 0; i<9; i++){
                        int p = (state/TTTLookupTable.threes[i])%3;
                        if(p==1){
                            dstate[i] = 1;
                        } else if(p==2){
                            dstate[i] = -1;
                        }
                    }
                    double[] moves  = ai.getPossible(dstate);
                    double[] played = playedMoves.get(state);
                    for(int i = 0; i<18; i++){
                        if(played[i]==1){
                            moves[i] = moves[i] + rate*(value - moves[i]);
                            value = moves[i];
                        }
                    }
                    valuedMoves.put(state, Arrays.asList(dstate, moves));


                }
            }
        }
        playedMoves.clear();
        if(valuedMoves.size()>0){
            count++;
            if(count<5) {
                System.out.println(result + ", " + count);
                ai.trainToWin(valuedMoves.values().stream().collect(Collectors.toList()));
                try {
                    ai.saveNetwork(brain);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }
}
