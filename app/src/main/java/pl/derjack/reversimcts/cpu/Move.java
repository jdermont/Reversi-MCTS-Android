package pl.derjack.reversimcts.cpu;

import android.graphics.Point;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Move implements Comparable<Move> {
    public final int player;
    public final Point p;
    public boolean nextPlayer;
    public boolean terminal;

    public Move parent;
    public List<Move> children;
    public Lock lock;

    public double score;
    public int games;
    public int virtualLoss;

    public Move(int player, Point p) {
        this.player = player;
        this.p = p;
        this.children = new ArrayList<>();
        this.lock = new ReentrantLock();
    }

    @Override
    public int compareTo(Move o) {
        if (score == o.score) {
            return Integer.compare(games,o.games);
        }
        return Double.compare(score,o.score);
    }
}
