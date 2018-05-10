package org.orangepalantir;

import org.orangepalantir.tttplayers.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Random;

/**
 * Created by smithm3 on 01/05/18.
 */
public class TTTLookupTable {
    final int states = 19683;
    public double[] table;
    public final static int[] threes = {
            1,   3,    9,
            27,  81,   243,
            729, 2187, 6561
    };
    double alpha = 0.001;
    public Random ng = new Random();

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

    int[] score;

        public void runTable(Path out) {
            if (Files.exists(out)) {
                try {
                    load(out);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Player a = new HumanPlayer(1);
            Player b = new NNPlayer(Paths.get("ttt-nn.dat"));
            score = new int[]{0, 0};
            for (int i = 0; i < 100000; i++) {
                playgame(a, b);
            }

        try {
            save(out);
        } catch (IOException e) {
            e.printStackTrace();
        }
            System.out.println(Arrays.toString(score));
        }

        public void playgame(Player a, Player b) {

            int[] moves = new int[9];
            int state = 0;
            Player p;
            int symbol;
            int winner = -1;
            int i;
            for (i = 0; i < 9; i++) {
                if (i % 2 == 1) {
                    p = b;
                    symbol = 2;
                } else {
                    p = a;
                    symbol = 1;
                }
                state = p.getMove(state);
                moves[i] = state;
                if (i > 3 && won(state, symbol)) {
                    winner = symbol;
                    break;
                }
            }
            if (winner > 0) {
                score[winner - 1] += 1;
            }
            updateTable(moves, i);
            //printTableWinners();
        }

        public void printMoves(int[] moves, int winner){
            String[] ss = {"  ", "  ", "  ", "  ", "  ", "  ", "  ", "  ", "  "};
            int last = 0;
            int counter = 0;
            for (int m : moves) {
                int delta = m - last;
                for (int k = 0; k < 9; k++) {
                    int dex = (delta / threes[k]) % 3;
                    if (dex > 0) {
                        ss[k] = (dex == 1 ? "X" : "O") + counter;
                        break;
                    }
                }
                counter++;
                last = m;
            }
            for (int k = 0; k < 3; k++) {
                for (int j = 0; j < 3; j++) {
                    int dex = 3 * j + k;
                    System.out.print(ss[dex]);
                }
                System.out.println("");
            }
            System.out.println("......: " + winner);
        }

    public void printBoard(int board){
        String[] ss = {"_", "_", "_", "_", "_", "_", "_", "_", "_"};
        int last = 0;
        int counter = 0;
        for (int k = 0; k < 9; k++) {
            int dex = (board / threes[k]) % 3;
            if (dex > 0) {
                ss[k] = (dex == 1 ? "X" : "O");
            }
        }

        for (int k = 0; k < 3; k++) {
            for (int j = 0; j < 3; j++) {
                int dex = 3 * j + k;
                System.out.print(ss[dex]);
            }
            System.out.println("");
        }
        System.out.println("---");
    }



    public void updateTable(int[] moves, int n) {
            int winner;
            if(n==9){
                n = n-1;
                //the loop finished without a break.
                table[moves[n]] = 1;
                table[moves[n - 1]] = 1;
                winner = -1;
            }else{
                table[moves[n]] = 1;
                table[moves[n - 1]] = 0;
                winner = (n%2) + 1;
            }
            //printMoves(moves, winner);
            for (int i = n - 2; i >= 0; i--) {
                table[moves[i]] = table[moves[i]] + alpha * (table[moves[i + 2]] - table[moves[i]]);
            }

        }
    public void printTableWinners(){
        for(int i = 0; i<table.length; i++){
            if(table[i]==1.0){
                printBoard(i);
            }
        }
    }


        public boolean won(int state, int symbol) {

            if (state % 3 == symbol) {
                //top left corner.
                if ((state / threes[1]) % 3 == symbol && state / threes[2] % 3 == symbol) {
                    //left to right.
                    return true;
                }
                if ((state / threes[3]) % 3 == symbol && state / threes[6] % 3 == symbol) {
                    //top to bottom
                    return true;
                }

                if ((state / threes[4]) % 3 == symbol && state / threes[8] % 3 == symbol) {
                    //diagonal left top to bottom right
                    return true;
                }


            }

            if ((state / threes[4]) % 3 == symbol) {
                //top left corner.
                if ((state / threes[1]) % 3 == symbol && state / threes[7] % 3 == symbol) {
                    //top to bottom.
                    return true;
                }
                if ((state / threes[3]) % 3 == symbol && state / threes[5] % 3 == symbol) {
                    //left to right
                    return true;
                }

                if ((state / threes[2]) % 3 == symbol && state / threes[6] % 3 == symbol) {
                    //diagonal right top to bottom left
                    return true;
                }

            }
            if ((state / threes[8]) % 3 == symbol) {
                //bottom right
                if ((state / threes[2]) % 3 == symbol && state / threes[5] % 3 == symbol) {
                    //top to bottom.
                    return true;
                }
                if ((state / threes[6]) % 3 == symbol && state / threes[7] % 3 == symbol) {
                    //left to right
                    return true;
                }

            }

            return false;

        }


        public static void main(String[] args) {
            TTTLookupTable lu = new TTTLookupTable();
            lu.runTable(Paths.get("lookup.dat"));
        }

        public static int[] getMoves(int played, int symbol) {
            int[] moves = new int[9];
            int c = played;
            for (int i = 0; i < 9; i++) {
                int o = c % 3;
                if (o > 0) {
                    moves[i] = -1;
                } else {
                    moves[i] = played + symbol * threes[i];
                }
                c /= 3;
            }
            return moves;
        }
    }



