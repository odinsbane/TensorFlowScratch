package org.orangepalantir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * This will be a bootstrap class for using re-inforcement learning
 * Created on 10/05/18.
 */
public class TTTNetworkLearning {
    public Network brain;
    public TTTNetworkLearning(Network b){
        brain = b;
    }

    /**
     * This will create training data of legal moves.
     */
    public void trainForLegalMoves(){
        List<double[]> states = new ArrayList<>();
        List<double[]> moves = new ArrayList<>();

        double[] state = new double[9];

        Queue<double[]> unexplored = new LinkedList<>();
        unexplored.add(state);

        while(unexplored.size()>0){

            double[] s = unexplored.poll();
            if(finished(s)){
                continue;
            }
            if (contains(states, s)) {
                continue;
            }
            states.add(s);
            double[] m = getPossible(s);
            moves.add(m);
            unexplored.addAll(getPossibleStates(s, m));
        }
        List<List<double[]>> trainingData = new ArrayList<>(moves.size());
        for(int i = 0; i<moves.size(); i++){
            trainingData.add(Arrays.asList(states.get(i), moves.get(i)));
        }

        double e = evaluate(trainingData);
        double c = evaluateLegalMoves(trainingData);
        System.out.println("possible: " + trainingData.size());
        System.out.println("before error of: " + e + ", with: " + c + " correct");

        brain.stochasticGradientDescent(trainingData, 100, 100, 0.51, null);

        e = evaluate(trainingData);
        c = evaluateLegalMoves(trainingData);

        System.out.println("after error of: " + e + ", with: " + c + " correct");

    }

    /**
     *
     * @param data
     * @return
     */
    double evaluate(List<List<double[]>> data){
        double sum = 0;
        for(List<double[]> pair: data){

            double[] state = pair.get(0);
            double[] move = pair.get(1);
            double[] est = brain.feedForward(state);
            for(int i = 0; i<move.length; i++){
                double v = move[i] - est[i];
                sum += v*v;
            }

        }
        return sum;
    }

    double evaluateLegalMoves(List<List<double[]>> data){
        double sum = 0;
        for(List<double[]> pair: data){

            double[] state = pair.get(0);
            double[] move = pair.get(1);
            double[] est = brain.feedForward(state);
            int d = brain.maxIndex(est);
            if(move[d]!=0){
                sum++;
            }

        }
        return sum;
    }

    static public boolean contains(List<double[]> states, double[] s){
        for(double[] state: states){
            if (Arrays.equals(state, s)) return true;
        }
        return false;
    }

    public boolean finished(double[] state){
        double sum = 0;
        for(double d: state){
            sum += d*d;
        }
        if(sum == 9) return true;

        //across top
        sum = 0;
        for(int i = 0; i<3; i++){
            sum += state[i];
        }
        if(sum*sum == 9) return true;

        //across middle
        sum = 0;
        for(int i = 0; i<3; i++){
            sum += state[i+3];
        }
        if(sum*sum == 9) return true;

        //across bottom
        sum = 0;
        for(int i = 0; i<3; i++){
            sum += state[i+6];
        }
        if(sum*sum == 9) return true;

        //verticle left
        sum = 0;
        for(int i = 0; i<3; i++){
            sum += state[i*3];
        }
        if(sum*sum == 9) return true;

        //verticle middle
        sum = 0;
        for(int i = 0; i<3; i++){
            sum += state[i*3 + 1];
        }
        if(sum*sum == 9) return true;

        //verticle right
        sum = 0;
        for(int i = 0; i<3; i++){
            sum += state[i*3 + 2];
        }
        if(sum*sum == 9) return true;

        sum = 0;
        for(int i = 0; i<3; i++){
            sum += state[i + 3*i];
        }
        if(sum*sum == 9) return true;

        sum = 0;
        for(int i = 0; i<3; i++){
            sum += state[2 - i + 3*i];
        }
        if(sum*sum == 9) return true;


        return false;

    }

    /**
     * Determines the next move and calls getPossible(state, move).
     * @param state
     * @return
     */
    double[] getPossible(double[] state){
        double s = 0;
        for(double d: state){
            s+=d;
        }
        int move = s==0?1:-1;
        return getPossible(state, move);
    }
    /**
     * Gets a double of the possible spots that move can play.
     *
     * @param move -1 is o and 1 is x.
     * @return 18 element array, the first 9 elements are x moves and the last nine elements are o moves.
     */
    double[] getPossible(double[] state, int move){
        double[] moves = new double[18];
        int offset = move==1?0:9;

        for(int i = 0; i<state.length; i++){
            if(state[i]==0){
                moves[i + offset] = 1;
            }
        }
        return moves;
    }


    public double[] predict(double[] state){
        return brain.feedForward(state);
    }
    /**
     *
     * @param state
     * @param moves 18 element array with yes or no values.
     * @return
     */
    List<double[]> getPossibleStates(double[] state, double[] moves){
        List<double[]> vals = new ArrayList<>();
        for(int i = 0; i<9; i++){

            if(moves[i]!=0){
                double[] newstate = Arrays.copyOf(state, 9);
                if(newstate[i]!=0){
                    System.out.println("broken");
                }
                newstate[i] = 1;
                vals.add(newstate);
            }
        }

        for(int i = 0; i<9; i++){
            if(moves[i + 9]!=0){
                double[] newstate = Arrays.copyOf(state, 9);
                if(newstate[i]!=0){
                    System.out.println("broken");
                }
                newstate[i] = -1;
                vals.add(newstate);
            }
        }

        return vals;
    }


    public void saveNetwork(Path bp) throws IOException {
        brain.save(bp);
    }

    public static void main(String[] args) throws IOException {
        String brainFile="ttt-nn.dat";

        Path bp = Paths.get(brainFile);
        Network b;
        if(Files.exists(bp)){
            b = Network.load(bp);
        } else{
            b = new Network(new int[]{9, 27, 27, 18});
        }
        TTTNetworkLearning student = new TTTNetworkLearning(b);

        student.trainForLegalMoves();

        student.saveNetwork(bp);

    }
}
