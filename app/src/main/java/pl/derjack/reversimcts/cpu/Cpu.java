package pl.derjack.reversimcts.cpu;

import android.graphics.Point;

import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import pl.derjack.reversimcts.objects.Game;

public class Cpu {
    private final static double INFINITY = 1000000.0;

    private int player;
    private Game game;

    private List<CpuWorker> cpuWorkers;
    private long timeInMillis;

    public Cpu(int threads, long timeInMillis) {
        player = Game.NONE;
        cpuWorkers = new ArrayList<>(threads);
        for (int i=0; i < threads; i++) {
            cpuWorkers.add(new CpuWorker());
        }
        this.timeInMillis = timeInMillis;
    }

    public int getPlayer() {
        return player;
    }

    public void setPlayer(int player) {
        this.player = player;
        for (CpuWorker cpuWorker : cpuWorkers) {
            cpuWorker.setPlayer(player);
        }
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public int games;

    public Move getBestMoveParallel() {
        final List<Move> moves = generateMoves(null);
        if (moves.size() == 1) {
            return moves.get(0);
        }

        ExecutorService executor = Executors.newFixedThreadPool(cpuWorkers.size());

        // Thread.interrupted() not working?
        final AtomicBoolean interrupted = new AtomicBoolean();

        for (final CpuWorker cpuWorker : cpuWorkers) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    cpuWorker.setGame(game);
                    cpuWorker.setProvenEnd(false);
                    while (!interrupted.get() && !cpuWorker.isProvenEnd()) {
                        cpuWorker.selectAndExpand(moves, games+1,0);
                        synchronized (Cpu.this) {
                            games++;
                        }
                    }
                }
            });
        }

        // reconsider this thing
        try {
            Thread.sleep(timeInMillis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        executor.shutdownNow();
        interrupted.set(true);

        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Move move = Collections.max(moves);
        return move;
    }

    private List<Move> generateMoves(Move parent) {
        List<Move> moves = getAvailableMoves();

        for (Move move : moves) {
            move.parent = parent;
            game.makeMove(move.p.x,move.p.y);
            move.nextPlayer = game.currentPlayer == move.player;
            if (game.isFinished()) {
                move.terminal = true;
                if (game.getWinner() == move.player) {
                    move.score = INFINITY;
                    move.games = 1;
                } else if (game.getWinner() != Game.NONE) {
                    move.score = -INFINITY;
                    move.games = 1;
                } else {
                    move.score = 0.5;
                    move.games = 1;
                }
            }
            game.undo();
        }

        return moves;
    }

    private List<Move> getAvailableMoves() {
        List<Point> movePts = game.getAvailableMoves(game.currentPlayer);
        List<Move> moves = new ArrayList<>();
        for (Point pt : movePts) {
            Move move = new Move(game.currentPlayer,pt);
            moves.add(move);
        }

        return moves;
    }

}
