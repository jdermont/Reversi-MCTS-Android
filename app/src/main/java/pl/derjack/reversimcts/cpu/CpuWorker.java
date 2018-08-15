package pl.derjack.reversimcts.cpu;

import android.graphics.Point;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import pl.derjack.reversimcts.objects.Game;

public class CpuWorker {
    private final static double INFINITY = 1000000.0;

    private int player;
    private Random random;
    private Game game;

    private boolean provenEnd;

    public CpuWorker() {
        player = Game.NONE;
        random = new RandomXS128();
        game = new Game();
    }

    public void setPlayer(int player) {
        this.player = player;
    }

    public void setGame(Game game) {
        this.game.setGame(game);
    }

    public boolean isProvenEnd() {
        return provenEnd;
    }

    public void setProvenEnd(boolean provenEnd) {
        this.provenEnd = provenEnd;
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

    public void selectAndExpand(List<Move> moves, int games, int level) {
        int visits = moves.get(0).parent == null ? games : moves.get(0).parent.games;
        List<Integer> indexes = new ArrayList<>();
        double max = -2*INFINITY;
        for (int i=0; i < moves.size(); i++) {
            Move move = moves.get(i);
            double a,b,c;
            if (move.games == 0) {
                a = 0.5 - 0.25 * move.virtualLoss;
                b = 0.5;
                c = 0.0;
                int x = move.p.x;
                int y = move.p.y;
                if ((x == 0 || x == Game.SIZE-1) && (y == 0 || y == Game.SIZE-1)) {
                    c += 0.25;
                }
                if (move.nextPlayer) {
                    c += 0.5;
                }
            } else {
                a = (move.score - 3.0 * move.virtualLoss) / move.games;
                b = 0.5 * Math.sqrt( Math.log(visits) / move.games );
                c = 0.0;
                int x = move.p.x;
                int y = move.p.y;
                if ((x == 0 || x == Game.SIZE-1) && (y == 0 || y == Game.SIZE-1)) {
                    c += 10.0;
                }
                if (!move.terminal) {
                    if (move.nextPlayer) {
                        c += 30.0;
                    }
                }
                c /= move.games;
            }

            if (a + b + c > max) {
                max = a + b + c;
                indexes.clear();
                indexes.add(i);
            } else if (a + b + c == max) {
                indexes.add(i);
            }
        }

        Move move = moves.get(indexes.get(random.nextInt(indexes.size())));
        move.lock.lock();
        move.virtualLoss++;
        if (move.score == INFINITY) {
            move.games++;
            if (level == 0) {
                provenEnd = true;
                move.virtualLoss--;
                move.lock.unlock();
                return;
            }
            Move parent = move.parent;
            while (parent != null && parent.player == move.player) {
                parent.lock.lock();
                parent.score = INFINITY;
                parent.games++;
                parent.virtualLoss--;
                parent.lock.unlock();
                parent = parent.parent;
            }
            if (parent != null && parent.player != move.player) {
                parent.lock.lock();
                parent.score = -INFINITY;
                parent.games++;
                parent.virtualLoss--;
                parent.lock.unlock();
                parent = parent.parent;
            }
            while (parent != null) {
                parent.lock.lock();
                parent.score += parent.player == move.player ? 1 : 0;
                parent.games++;
                parent.virtualLoss--;
                parent.lock.unlock();
                parent = parent.parent;
            }
            move.virtualLoss--;
            move.lock.unlock();
            return;
        } else if (move.score == -INFINITY) {
            boolean allChildrenAreBad = true;
            for (Move m : moves) {
                if (m.score > -INFINITY) {
                    allChildrenAreBad = false;
                    break;
                }
            }
            move.games++;
            Move parent = move.parent;
            if (allChildrenAreBad) {
                if (level == 0) {
                    provenEnd = true;
                    move.virtualLoss--;
                    move.lock.unlock();
                    return;
                }
                while (parent != null && parent.player != move.player) {
                    parent.lock.lock();
                    parent.score = INFINITY;
                    parent.games++;
                    parent.virtualLoss--;
                    parent.lock.unlock();
                    parent = parent.parent;
                }
                if (parent != null && parent.player == move.player) {
                    parent.lock.lock();
                    parent.score = -INFINITY;
                    parent.games++;
                    parent.virtualLoss--;
                    parent.lock.unlock();
                    parent = parent.parent;
                }
            }
            while (parent != null) {
                parent.lock.lock();
                parent.score += parent.player == move.player ? 0 : 1;
                parent.games++;
                parent.virtualLoss--;
                parent.lock.unlock();
                parent = parent.parent;
            }
            move.virtualLoss--;
            move.lock.unlock();
            return;
        } else if (move.terminal) {
            move.games++;
            Move parent = move.parent;
            while (parent != null) {
                parent.lock.lock();
                parent.score += 0.5;
                parent.games++;
                parent.virtualLoss--;
                parent.lock.unlock();
                parent = parent.parent;
            }
            move.virtualLoss--;
            move.lock.unlock();
            if (level == 0) {
                boolean allTerminal = true;
                for (Move m : moves) {
                    if (!m.terminal) {
                        allTerminal = false;
                        break;
                    }
                }
                if (allTerminal) {
                    provenEnd = true;
                }
            }
            return;
        }
        game.makeMove(move.p.x,move.p.y);
        if (move.terminal) {
            System.out.println("error?");
        } else if (move.games == 0) {
            double score = simulateOne(move.player);
            move.score += score;
            move.games++;
            move.virtualLoss--;
            move.lock.unlock();
            Move parent = move.parent;
            while (parent != null) {
                parent.lock.lock();
                parent.score += parent.player == move.player ? score : 1-score;
                parent.games++;
                parent.virtualLoss--;
                parent.lock.unlock();
                parent = parent.parent;
            }
        } else {
            if (move.children.isEmpty()) {
                move.children = generateMoves(move);
            }
            move.lock.unlock();
            selectAndExpand(move.children,games, level+1);
        }
        game.undo();
    }

    private Game newGame = new Game();
    private int[] pts = new int[1024];

    private double simulateOne(int forPlayer) {
        newGame.setGame(game);
        while (!newGame.isFinished()) {
            long moves = newGame.moves[newGame.currentPlayer];
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
                    }
                }
                x++;
                if ((x&7) == 0) {
                    x = 0;
                    y++;
                }
            }
            int k = random.nextInt(j/2);
            newGame.makeMoveNoHistory(pts[2*k], pts[2*k+1]);
        }
        int winner = newGame.getWinner();
        if (winner == forPlayer) return 1;
        else if (winner != Game.NONE) return 0;
        return 0.5;
    }
}
