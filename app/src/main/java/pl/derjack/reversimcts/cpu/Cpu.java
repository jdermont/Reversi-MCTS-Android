package pl.derjack.reversimcts.cpu;

import android.graphics.Point;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import pl.derjack.reversimcts.objects.Game;

public class Cpu {
    public int player = Game.NONE;

    private int threads;
    private List<CpuThread> cpuThreads;
    private long timeInMillis;
    private Game game;
    private final Object lock = new Object();
    private Random random = new RandomXS128();

    public Cpu(int threads, long timeInMillis) {
        this.threads = threads;
        cpuThreads = new ArrayList<>(threads);
        for (int i=0;i<threads;i++) {
            cpuThreads.add(new CpuThread());
        }
        this.timeInMillis = timeInMillis;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public void setPlayer(int player) {
        this.player = player;
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

    public Move getBestMoveParallel() {
        final List<Move> ruchy = generateMoves(null,1);
        if (ruchy.size() == 1) return ruchy.get(0);
        ExecutorService executorService = Executors.newFixedThreadPool(threads);
        for (CpuThread cpuThread : cpuThreads) {
            cpuThread.startJob(game, ruchy, lock, executorService);
        }
        try {
            Thread.sleep(timeInMillis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        executorService.shutdownNow();
        try {
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        int games = 0;
        for (Move r : ruchy) {
            games += r.games;
        }
        Collections.shuffle(ruchy,random);
        Move max = Collections.max(ruchy);
        max.games = games;
        return max;
    }

    public List<Move> generateMoves(Move parent, int level) {
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

}
