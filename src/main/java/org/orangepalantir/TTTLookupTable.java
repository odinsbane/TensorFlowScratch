package org.orangepalantir;

import oracle.jrockit.jfr.JFR;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.EventQueue;
import java.awt.GridLayout;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.SynchronousQueue;

/**
 * Created by smithm3 on 01/05/18.
 */
public class TTTLookupTable {
    final int states = 19683;
    double[] table;
    final static int[] threes = {
            1,   3,    9,
            27,  81,   243,
            729, 2187, 6561
    };
    double alpha = 0.5;
    Random ng = new Random();

    static int pow(int base, int exp){
        int result = 1;
        for(int i = 0; i<exp; i++){
            result = base*result;
        }
        return result;
    }



    public TTTLookupTable(){
        table = new double[states];
        for(int i = 0; i<states; i++){
            table[i] = 0.5;
        }
    }

    public void load(Path in) throws IOException {
        try(
                DataInputStream dis = new DataInputStream(
                        Files.newInputStream(in)
                )){
            for(int i = 0; i<states; i++){
                table[i] = dis.readDouble();
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new IOException(e);
        }
    }

    public void save(Path out) throws IOException {
        try(
                DataOutputStream dos = new DataOutputStream(
                    Files.newOutputStream(
                        out,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING
                    )
            )){
            for(double d: table){
                dos.writeDouble(d);
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new IOException(e);
        }
    }

    interface Player{
        int getMove(int state);
    }

    class HumanPlayer implements Player{
        final int symbol;
        SynchronousQueue<Integer> queue = new SynchronousQueue<>(true);
        List<JButton> buttons = new ArrayList<>();
        HumanPlayer(int symbol){
            this.symbol=symbol;
            try {
                EventQueue.invokeAndWait(this::buildGui);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        public int getMove(int state){
            EventQueue.invokeLater(()->updateState(state));
            int move = 0;
            try {
                move = queue.take();
                int new_state = state + move;


                EventQueue.invokeLater(()->updateState(state));

                return new_state;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return state;
        }

        void buildGui(){
            JFrame frame = new JFrame("Human");
            JPanel content = new JPanel();
            content.setLayout(new GridLayout(3,3));
            for(int i = 0; i<9; i++){
                JButton button = new JButton("  ");
                int factor = threes[i];
                button.addActionListener(evt->{
                    if(button.getText().equals("O") || button.getText().equals("X")){
                        return;
                    }

                    queue.offer(factor*symbol);
                });
                content.add(button);
                buttons.add(button);
            }
            frame.setContentPane(content);
            frame.pack();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        }

        public void updateState(int state){
            for(int i = 0; i<9; i++){
                int piece = (state/threes[i])%3;
                String s = "  ";
                switch(piece){
                    case 1:
                        s = "X";
                        break;
                    case 2:
                        s = "O";
                        break;
                    default:
                        s = " ";
                }
                buttons.get(i).setText(s);
            }
        }
    }

    class RandomPlayer implements Player{
        final int symbol;

        public RandomPlayer(int symbol){
            this.symbol = symbol;
        }

        @Override
        public int getMove(int state) {
            int[] moves = getMoves(state, symbol);
            int possible = 0;
            for(int i = 0; i<9; i++){
                if(moves[i]>0){
                    possible += 1;
                }
            }
            int c = ng.nextInt(possible);
            int t = 0;
            for(int i = 0; i<9; i++){
                if(moves[i]>0){
                    if(t==c){
                        return moves[i];
                    } else{
                        t++;
                    }
                }
            }
            throw new RuntimeException("Error in Random move generation!");
        }
    }

    class FuzzyPlayer implements Player{
        final int symbol;

        public FuzzyPlayer(int symbol){
            this.symbol = symbol;
        }

        @Override
        public int getMove(int state) {
            int[] moves = getMoves(state, symbol);
            double possible = 0;
            int any = 0;
            for(int i = 0; i<9; i++){
                if(moves[i]>0){
                    possible += table[moves[i]];
                    any++;
                }
            }

            if(any==1){
                for(int i = 0; i<9; i++){
                    if(moves[i]>0){
                        return moves[i];
                    }
                }
            } else{
                double f = possible*ng.nextDouble();
                possible=0;
                for(int i = 0; i<9; i++){
                    if(moves[i]>0){
                        possible += table[moves[i]];
                        if(possible>=f){
                            return moves[i];
                        }
                    }
                }
            }

            throw new RuntimeException("Error in Fuzzy Move Generation.");

        }
    }

    class PlaysBest implements Player{
        final int symbol;
        public PlaysBest(int s){
            symbol = s;
        }
        @Override
        public int getMove(int state) {
            int[] moves = getMoves(state, symbol);
            double best = -1;
            int dex = -1;

            for(int i = 0; i<9; i++){
                if(moves[i]>0){
                    double p = table[moves[i]];
                    if(p>best){
                        best = p;
                        dex = i;
                    } else if(p==best){
                        if(ng.nextDouble()>0.5){
                            best = p;
                            dex = i;
                        }
                    }
                }
            }
            if(dex<0){
                throw new RuntimeException("Couldnt Find best move");
            }
            return state + symbol*threes[dex];
        }
    }

    int[] score;
    public void runTable(Path out){
        if(Files.exists(out)){
            try {
                load(out);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Player a = new HumanPlayer(1);
        Player b = new RandomPlayer(2);
        score = new int[]{0, 0};
        for(int i = 0; i<100000; i++){
            playgame(a,b);
        }

        try {
            save(out);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(Arrays.toString(score));
    }

    public void playgame(Player a, Player b){

        int[] moves =new int[9];
        int state = 0;
        Player p;
        int symbol;
        int winner = -1;
        int i;
        for(i = 0; i<9; i++){
            if(i%2==1){
                p=b;
                symbol = 2;
            } else{
                p=a;
                symbol = 1;
            }
            state = p.getMove(state);
            moves[i] = state;
            if (i>3 && won(state, symbol)) {
                winner = symbol;
                break;
            }
        }
        if(winner>0){
            score[winner -1] += 1;
        }
        updateTable(moves, i-1, winner);
    }

    public void updateTable(int[] moves, int n, int symbol){

        if(symbol<0){
            table[moves[n]] = 1;
            table[moves[n-1]] = 1;
        } else{
            table[moves[n]] = 1;
            table[moves[n-1]] = 0;
        }

        for(int i = n - 2; i>=0; i--){
            table[moves[i]] = table[moves[i]] + alpha*(table[moves[i+2]] - table[moves[i]]);
        }

    }



    public boolean won(int state, int symbol){

        if(state%3==symbol){
            //top left corner.
            if((state/threes[1])%3==symbol && state/threes[2]%3==symbol){
                //left to right.
                return true;
            }
            if((state/threes[3])%3==symbol && state/threes[6]%3==symbol){
                //top to bottom
                return true;
            }

            if((state/threes[4])%3==symbol && state/threes[8]%3==symbol){
                //diagonal left top to bottom right
                return true;
            }


        }

        if((state/threes[4])%3==symbol){
            //top left corner.
            if((state/threes[1])%3==symbol && state/threes[7]%3==symbol){
                //top to bottom.
                return true;
            }
            if((state/threes[3])%3==symbol && state/threes[5]%3==symbol){
                //left to right
                return true;
            }

            if((state/threes[2])%3==symbol && state/threes[6]%3==symbol){
                //diagonal right top to bottom left
                return true;
            }

        }
        if((state/threes[8])%3==symbol){
            //bottom right
            if((state/threes[2])%3==symbol && state/threes[5]%3==symbol){
                //top to bottom.
                return true;
            }
            if((state/threes[6])%3==symbol && state/threes[7]%3==symbol){
                //left to right
                return true;
            }

        }

        return false;

    }


    public static void main(String[] args){
        TTTLookupTable lu = new TTTLookupTable();
        lu.runTable(Paths.get("null.dat"));
    }

    int[] getMoves(int played, int symbol){
        int[] moves = new int[9];
        int c = played;
        for(int i = 0; i<9; i++){
            int o = c%3;
            if(o>0){
                moves[i] = -1;
            } else{
                moves[i] =played + symbol * threes[i];
            }
            c /= 3;
        }
        return moves;
    }


}

