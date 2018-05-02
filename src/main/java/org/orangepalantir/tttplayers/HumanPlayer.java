package org.orangepalantir.tttplayers;

import org.orangepalantir.TTTLookupTable;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.EventQueue;
import java.awt.GridLayout;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.SynchronousQueue;

/**
 * Created by smithm3 on 02/05/18.
 */
public class HumanPlayer implements Player {
    final int symbol;
    SynchronousQueue<Integer> queue = new SynchronousQueue<>(true);
    List<JButton> buttons = new ArrayList<>();

    public HumanPlayer(int symbol) {
        this.symbol = symbol;
        try {
            EventQueue.invokeAndWait(this::buildGui);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public int getMove(int state) {
        EventQueue.invokeLater(() -> updateState(state));
        try {
            int move = queue.take();
            int new_state = state + move;


            EventQueue.invokeLater(() -> updateState(new_state));

            return new_state;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return state;
    }

    void buildGui() {
        JFrame frame = new JFrame("Human");
        JPanel content = new JPanel();
        content.setLayout(new GridLayout(3, 3));
        for (int i = 0; i < 9; i++) {
            JButton button = new JButton("  ");
            int factor = TTTLookupTable.threes[i];
            button.addActionListener(evt -> {
                if (button.getText().equals("O") || button.getText().equals("X")) {
                    return;
                }

                queue.offer(factor * symbol);
            });
            content.add(button);
            buttons.add(button);
        }
        frame.setContentPane(content);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    public void updateState(int state) {
        for (int i = 0; i < 9; i++) {
            int piece = (state / TTTLookupTable.threes[i]) % 3;
            String s = "  ";
            switch (piece) {
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
