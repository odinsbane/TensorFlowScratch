package org.orangepalantir.tttplayers;

/**
 * Created by smithm3 on 02/05/18.
 */
public interface Player{
    int getMove(int state);
    default void finished(int result){

    }
}
