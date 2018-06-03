package pl.derjack.reversimcts.game;

import java.lang.ref.WeakReference;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import pl.derjack.reversimcts.cpu.Cpu;
import pl.derjack.reversimcts.cpu.Move;
import pl.derjack.reversimcts.objects.Game;

public class GameState {
    public enum State { NONE, CALCULATING }

    private ScheduledExecutorService executor;
    private State state;
    private WeakReference<Listener> listener;

    private Game game;
    private int humanPlayer;
    private Cpu cpu;
    private Move lastCalculatedMove;

    public GameState() {
        executor = Executors.newSingleThreadScheduledExecutor();
        state = State.NONE;
        humanPlayer = Game.NONE;
    }

    public void startGame(int threads, long timeInMillis) {
        lastCalculatedMove = null;
        humanPlayer = (humanPlayer + 1) & 1;
        game = new Game();
        cpu = new Cpu(threads,timeInMillis);
        cpu.setGame(game);
        cpu.setPlayer((humanPlayer + 1) & 1);
    }

    public Game getGame() {
        return game;
    }

    public Cpu getCpu() {
        return cpu;
    }

    public State getState() {
        return state;
    }

    public int getHumanPlayer() {
        return humanPlayer;
    }

    public void setListener(Listener listener) {
        this.listener = new WeakReference<>(listener);
    }

    public Move getLastCalculatedMove() {
        return lastCalculatedMove;
    }

    public boolean canMove() {
        return game != null && game.currentPlayer == humanPlayer;
    }

    public void calculateMove() {
        state = State.CALCULATING;
        executor.execute(new Runnable() {
            @Override
            public void run() {
                lastCalculatedMove = cpu.getBestMoveParallel();
                state = State.NONE;
                Listener listener = GameState.this.listener.get();
                if (listener != null) {
                    listener.onMoveCalculated(lastCalculatedMove);
                }
            }
        });
    }

    public interface Listener {
        void onMoveCalculated(Move move);
    }
}
