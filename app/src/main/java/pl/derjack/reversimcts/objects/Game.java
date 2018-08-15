package pl.derjack.reversimcts.objects;

import android.graphics.Point;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class Game {
    public static final int FIRST = 0;
    public static final int SECOND = 1;
    public static final int NONE = -1;

    public static final int SIZE = 8;

    private static final long DOWN_MASK = 0x00FFFFFFFFFFFFFFL;
    private static final long UP_MASK = 0xFFFFFFFFFFFFFF00L;
    private static final long LEFT_MASK = 0xFEFEFEFEFEFEFEFEL;
    private static final long RIGHT_MASK = 0x7F7F7F7F7F7F7F7FL;
    private static final long UPLEFT_MASK = UP_MASK&LEFT_MASK;
    private static final long DOWNLEFT_MASK = DOWN_MASK&LEFT_MASK;
    private static final long UPRIGHT_MASK = UP_MASK&RIGHT_MASK;
    private static final long DOWNRIGHT_MASK = DOWN_MASK&RIGHT_MASK;

    public int currentPlayer;

    public long currentBoard;
    public long opponentBoard;
    public long emptyBoard;

    public long[] moves;
    public Stack<State> states;
    public Stack<Point> lastMoves;

    public Game() {
        currentPlayer = FIRST;

        currentBoard = 34628173824L;
        opponentBoard = 68853694464L;
        generateEmptyBoard();

        moves = new long[2];
        generateMoves(FIRST);

        states = new Stack<>();
        lastMoves = new Stack<>();
    }

    public void setGame(Game game) {
        this.currentPlayer = game.currentPlayer;
        this.currentBoard = game.currentBoard;
        this.opponentBoard = game.opponentBoard;
        this.emptyBoard = game.emptyBoard;
        this.moves[0] = game.moves[0];
        this.moves[1] = game.moves[1];
    }

    public void changePlayer() {
        currentPlayer = (currentPlayer+1)&1;
    }

    private void generateEmptyBoard() {
        emptyBoard = ~(currentBoard|opponentBoard);
    }

    public void undo() {
        if (!states.isEmpty()) {
            State state = states.pop();
            currentPlayer = state.player;
            currentBoard = state.own;
            opponentBoard = state.opponent;
            generateEmptyBoard();
            generateMoves(currentPlayer);
            lastMoves.pop();
        }
    }

    public void makeMove(int x, int y) {
        states.push(new State(currentBoard,opponentBoard,currentPlayer));
        addMoves(currentPlayer, x, y);
        changePlayer();
        generateMoves(currentPlayer);
        if (moves[currentPlayer] == 0) generateMoves((currentPlayer+1)&1);
        if (!isFinished() && moves[currentPlayer] == 0) changePlayer();
        lastMoves.add(new Point(x,y));
    }

    public void makeMoveNoHistory(int x, int y) {
        addMoves(currentPlayer, x, y);
        changePlayer();
        generateMoves(currentPlayer);
        if (moves[currentPlayer] == 0) generateMoves((currentPlayer+1)&1);
        if (!isFinished() && moves[currentPlayer] == 0) changePlayer();
    }

    public void addMoves(int player, int x, int y) {
        long t = Long.MIN_VALUE;
        t >>>= x+SIZE*y;
        if (player == FIRST) currentBoard |= t;
        else opponentBoard |= t;
        long r = 0L;
        r |= isDirectionOK(player,-1,LEFT_MASK,t);
        r |= isDirectionOK(player,1,RIGHT_MASK,t);
        r |= isDirectionOK(player,-SIZE,UP_MASK,t);
        r |= isDirectionOK(player,SIZE,DOWN_MASK,t);
        r |= isDirectionOK(player,-1-SIZE,UPLEFT_MASK,t);
        r |= isDirectionOK(player,1-SIZE,UPRIGHT_MASK,t);
        r |= isDirectionOK(player,-1+SIZE,DOWNLEFT_MASK,t);
        r |= isDirectionOK(player,1+SIZE,DOWNRIGHT_MASK,t);
        currentBoard ^= r;
        opponentBoard ^= r;
        generateEmptyBoard();
    }

    private long isDirectionOK(int player, int shift, long mask, long t) {
        long p = 0L;
        if (shift > 0) {
            t >>>= shift;
            t &= mask;
            while (t != 0 && (t&emptyBoard) == 0) {
                if ((t&(player==FIRST?currentBoard:opponentBoard)) == t) {
                    return p;
                }
                p |= t;
                t >>>= shift;
                t &= mask;
            }
        } else {
            shift *= -1;
            t <<= shift;
            t &= mask;
            while (t != 0 && (t&emptyBoard) == 0) {
                if ((t&(player==FIRST?currentBoard:opponentBoard)) == t) {
                    return p;
                }
                p |= t;
                t <<= shift;
                t &= mask;
            }
        }
        return 0L;
    }

    public boolean isFinished() {
        return moves[FIRST]==0 && moves[SECOND]==0;
    }

    private void generateMoves(int player) {
        moves[player] = getMoves(player);
    }

    private long getMoves(int player) {
        long legal = 0L;
        long potentialMoves;
        long currentBoard,opponentBoard;
        if (player == FIRST) {
            currentBoard = this.currentBoard;
            opponentBoard = this.opponentBoard;
        } else {
            currentBoard = this.opponentBoard;
            opponentBoard = this.currentBoard;
        }
        // UP
        potentialMoves = (currentBoard >>> SIZE) & DOWN_MASK & opponentBoard;
        while (potentialMoves != 0L) {
            long tmp = (potentialMoves >>> SIZE) & DOWN_MASK;
            legal |= tmp & emptyBoard;
            potentialMoves = tmp & opponentBoard;
        }
        // DOWN
        potentialMoves = (currentBoard << SIZE) & UP_MASK & opponentBoard;
        while (potentialMoves != 0L) {
            long tmp = (potentialMoves << SIZE) & UP_MASK;
            legal |= tmp & emptyBoard;
            potentialMoves = tmp & opponentBoard;
        }
        // LEFT
        potentialMoves = (currentBoard >>> 1L) & RIGHT_MASK & opponentBoard;
        while (potentialMoves != 0L) {
            long tmp = (potentialMoves >>> 1L) & RIGHT_MASK;
            legal |= tmp & emptyBoard;
            potentialMoves = tmp & opponentBoard;
        }
        // RIGHT
        potentialMoves = (currentBoard << 1L) & LEFT_MASK & opponentBoard;
        while (potentialMoves != 0L) {
            long tmp = (potentialMoves << 1L) & LEFT_MASK;
            legal |= tmp & emptyBoard;
            potentialMoves = tmp & opponentBoard;
        }
        // UP LEFT
        potentialMoves = (currentBoard >>> (SIZE + 1L)) & DOWNRIGHT_MASK & opponentBoard;
        while (potentialMoves != 0L) {
            long tmp = (potentialMoves >>> (SIZE + 1L)) & DOWNRIGHT_MASK;
            legal |= tmp & emptyBoard;
            potentialMoves = tmp & opponentBoard;
        }
        // UP RIGHT
        potentialMoves = (currentBoard >>> (SIZE - 1L)) & DOWNLEFT_MASK & opponentBoard;
        while (potentialMoves != 0L) {
            long tmp = (potentialMoves >>> (SIZE - 1L)) & DOWNLEFT_MASK;
            legal |= tmp & emptyBoard;
            potentialMoves = tmp & opponentBoard;
        }
        // DOWN LEFT
        potentialMoves = (currentBoard << (SIZE - 1L)) & UPRIGHT_MASK & opponentBoard;
        while (potentialMoves != 0L) {
            long tmp = (potentialMoves << (SIZE - 1L)) & UPRIGHT_MASK;
            legal |= tmp & emptyBoard;
            potentialMoves = tmp & opponentBoard;
        }
        // DOWN RIGHT
        potentialMoves = (currentBoard << (SIZE + 1L)) & UPLEFT_MASK & opponentBoard;
        while (potentialMoves != 0L) {
            long tmp = (potentialMoves << (SIZE + 1L)) & UPLEFT_MASK;
            legal |= tmp & emptyBoard;
            potentialMoves = tmp & opponentBoard;
        }

        return legal;
    }

    public long getHash(int player) {
        long hash = (69696969*currentBoard) ^ (137111*opponentBoard);
        hash ^= 9991234567L*(player+1);
        return hash;
    }

    public List<Point> getAvailableMoves(int player) {
        List<Point> availableMoves = new ArrayList<>();
        long moves = this.moves[player];
        for (int x=0,y=0;moves!=0;moves<<=1) {
            if ((moves&Long.MIN_VALUE) != 0) {
                availableMoves.add(new Point(x, y));
            }
            x++;
            if ((x&7) == 0) {
                x = 0;
                y++;
            }
        }
        return availableMoves;
    }

    public List<Point> getAvailableMoves() {
        List<Point> availableMoves = new ArrayList<>();
        long moves = this.moves[currentPlayer];
        for (int x=0,y=0;moves!=0;moves<<=1) {
            if ((moves&Long.MIN_VALUE) != 0) {
                availableMoves.add(new Point(x, y));
            }
            x++;
            if ((x&7) == 0) {
                x = 0;
                y++;
            }
        }
        return availableMoves;
    }

    public int getWinner() {
        int first = 0,second = 0;
        long cb = currentBoard;
        while (cb != 0) {
            cb &= cb-1;
            first++;
        }
        long op = opponentBoard;
        while (op != 0) {
            op &= op-1;
            second++;
        }
        if (first > second) return FIRST;
        else if (first < second) return SECOND;
        else return NONE;
    }

    public int getScore(int player) {
        int output = 0;
        long b = player == FIRST ? currentBoard : opponentBoard;
        while (b != 0) {
            b &= b-1;
            output++;
        }
        return output;
    }

    public boolean canUndo() {
        return !states.isEmpty();
    }

    public Point getLastMove() {
        if (!lastMoves.isEmpty()) return lastMoves.peek();
        return null;
    }

    public int getEmpty() {
        int empty = 0;
        long eb = emptyBoard;
        while (eb != 0) {
            eb &= eb-1;
            empty++;
        }
        return empty;
    }

    private static class State {
        long own;
        long opponent;
        int player;

        State(long own, long opponent, int player) {
            this.own = own;
            this.opponent = opponent;
            this.player = player;
        }

        @Override
        public String toString() {
            return own+" "+opponent+" "+player;
        }
    }
}
