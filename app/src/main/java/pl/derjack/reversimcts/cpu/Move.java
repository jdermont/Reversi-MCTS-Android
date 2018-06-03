package pl.derjack.reversimcts.cpu;

import android.graphics.Point;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import pl.derjack.reversimcts.objects.Game;

public class Move implements Comparable<Move> {
    public Point point;
    public int score;
    public int games;
    public boolean terminate;

    public Move parent;
    public List<Move> ruchy = new ArrayList<>();
    public int player = Game.NONE;

    public Move(Point point) {
        this.point = point;
    }

    public Move copy() {
        Move ruch = new Move(point);
        ruch.player = player;
        ruch.score = score;
        ruch.games = games;
        ruch.terminate = terminate;
        for (Move r : ruchy) {
            Move copy = r.copy();
            copy.parent = ruch;
            ruch.ruchy.add(copy);
        }

        return ruch;
    }

    public static List<Move> copyList(List<Move> ruchy) {
        List<Move> output = new ArrayList<>(ruchy.size());
        for (Move ruch : ruchy) {
            output.add(ruch.copy());
        }
        return output;
    }

    @Override
    public int compareTo(Move other) {
        if (games == other.games) {
            return Integer.compare(score,other.score);
        }
        return Integer.compare(games,other.games);
    }

    @Override
    public String toString() {
        return String.format(Locale.ENGLISH, "%d/%d %s",score,games,ruchy.isEmpty()?"":ruchy.toString());
    }

}