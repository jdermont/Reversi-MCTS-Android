package pl.derjack.reversimcts.cpu;

import android.graphics.Point;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;

import pl.derjack.reversimcts.objects.Game;

public class CpuThread {
    private Random random;
    private Game game;

    private Game tempGame = new Game();
    private int[] pts = new int[144];

    public CpuThread() {
        this.game = new Game();
        random = new RandomXS128();
    }

    public void setGame(Game game) {
        this.game.setGame(game);
    }

    public void startJob(final Game game, final List<Move> ruchy, final Object lock, ExecutorService executorService) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                setGame(game);
                final List<Move> ruchyCopy = Move.copyList(ruchy);
                fillRuchy(ruchyCopy);
                synchronized (lock) {
                    for (int i=0;i<ruchy.size();i++) {
                        ruchy.get(i).score += ruchyCopy.get(i).score;
                        ruchy.get(i).games += ruchyCopy.get(i).games;
                    }
                }
            }
        });

    }

    public void fillRuchy(List<Move> ruchy) {
        Deque<Move> stack = new ArrayDeque<>();
        int i=0;
        while (!Thread.interrupted()) {
            Move ruch = selectRec(ruchy,i+1, 1);
            stack.clear();

            stack.push(ruch);
            Move parent = ruch.parent;
            while (parent != null) {
                stack.push(parent);
                parent = parent.parent;
            }

            int count = stack.size();
            while (!stack.isEmpty()) {
                Move r = stack.pop();
                game.do_move(r.point.x,r.point.y);
            }

            if (game.isEnd()) {
                int score = game.getWinner()==ruch.player ? 1 : 0;
                ruch.score += score;
                ruch.games++;
                Move r = ruch;
                while (r.parent != null) {
                    if (ruch.player == r.parent.player) {
                        r.parent.score += score;
                    } else {
                        r.parent.score += 1-score;
                    }
                    r.parent.games++;
                    r = r.parent;
                }
            } else {
                int score = simulateForRuch(ruch);
                Move r = ruch;
                while (r.parent != null) {
                    if (ruch.player == r.parent.player) {
                        r.parent.score += score;
                    } else {
                        r.parent.score += 1-score;
                    }
                    r.parent.games++;
                    r = r.parent;
                }
            }

            for (int j=0;j<count;j++) {
                game.undo();
            }
            i++;
        }
    }

    private int simulateForRuch(Move ruch) {
        int score = simulateOne(ruch.player);
        ruch.score += score;
        ruch.games += 1;
        return score;
    }

    private Move selectRec(List<Move> ruchy, int games, int level) {
        List<Integer> indexes = new ArrayList<>();
        double max = -1000000;
        for (int i=0;i<ruchy.size();i++) {
            Move ruch = ruchy.get(i);
            double a = 1000000.0,b = 1000000.0;
            if (ruch.games > 0) {
                a = (double)ruch.score / ruch.games;
                b = 0.5 * Math.sqrt( Math.log(games) / ruch.games);
            }

            if (a + b > max) {
                max = a + b;
                indexes.clear();
                indexes.add(i);
            } else if (a + b == max) {
                indexes.add(i);
            }
        }

        Move ruch = ruchy.get(indexes.get(random.nextInt(indexes.size())));
        game.do_move(ruch.point.x,ruch.point.y);
        if (!ruch.ruchy.isEmpty()) {
            Move o = selectRec(ruch.ruchy, games, level+1);
            game.undo();
            return o;
        } else if (ruch.games >= 1 && !ruch.terminate) {
            ruch.ruchy = generateMoves(ruch, 1);
            Move o = selectRec(ruch.ruchy, games, level+1);
            game.undo();
            return o;
        }
        game.undo();
        return ruch;
    }

    private List<Move> generateMoves(Move parent, int level) {
        List<Move> ruchy = getAvailableMoves();
        for (Move ruch : ruchy) {
            ruch.parent = parent;
            game.do_move(ruch.point.x,ruch.point.y);
            if (!game.isEnd() && level-1 > 0) {
                ruch.ruchy = generateMoves(ruch, level-1);
            } else if (game.isEnd()) {
                ruch.terminate = true;
                if (game.getWinner() == ruch.player) {
                    ruch.score = 1;
                    ruch.games = 1;
                    Move r = ruch;
                    while (r.parent != null) {
                        if (ruch.player == r.parent.player) {
                            r.parent.score += 1;
                        } else {
                            r.parent.score += 0;
                        }
                        r.parent.games += 1;
                        r = r.parent;
                    }

                } else {
                    ruch.score = 0;
                    ruch.games = 1;
                    Move r = ruch;
                    while (r.parent != null) {
                        if (ruch.player == r.parent.player) {
                            r.parent.score += 0;
                        } else {
                            r.parent.score += 1;
                        }
                        r.parent.games += 1;
                        r = r.parent;
                    }
                }
            }
            game.undo();
        }

        return ruchy;
    }

    private List<Move> getAvailableMoves() {
        List<Point> moves = game.getAvailableMoves();
        List<Move> ruchy = new ArrayList<>();
        for (Point m : moves) {
            Move ruch = new Move(m);
            ruch.player = game.currentPlayer;
            ruchy.add(ruch);
        }

        return ruchy;
    }

    private int simulateOne(int forPlayer) {
        tempGame.setGame(game);
        while (!tempGame.isEnd()) {
            long moves = tempGame.moves[tempGame.currentPlayer];
            int j = 0;
            for (int x=0,y=0;moves!=0;moves<<=1) {
                if ((moves&Long.MIN_VALUE) != 0) {
                    pts[j] = x;
                    pts[j+1] = y;
                    j += 2;
                    if ((x == 0 || x == Game.SIZE-1) && (y == 0 || y == Game.SIZE-1)) {
                        pts[j] = x;
                        pts[j+1] = y;
                        j += 2;

                        pts[j] = x;
                        pts[j+1] = y;
                        j += 2;
                    }
                }
                x++;
                if ((x&7) == 0) {
                    x = 0;
                    y++;
                }
            }
            int k = random.nextInt(j/2);
            tempGame.do_move_no_history(pts[2*k], pts[2*k+1]);
        }
        int winner = tempGame.getWinner();
        if (winner == forPlayer) return 1;
        return 0;
    }
}
