package pl.derjack.reversimcts.game

import android.graphics.Point
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v4.app.LoaderManager
import android.support.v4.content.Loader
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import kotlinx.android.synthetic.main.fragment_game.*
import kotlinx.android.synthetic.main.fragment_game.view.*
import pl.derjack.reversimcts.R
import pl.derjack.reversimcts.cpu.Move
import pl.derjack.reversimcts.gfx.BoardView
import pl.derjack.reversimcts.objects.Game

class GameFragment : Fragment(), BoardView.BoardListener, GameState.Listener, LoaderManager.LoaderCallbacks<GameState> {
    var gameState: GameState? = null
    lateinit var handler: Handler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loaderManager.initLoader(LOADER_ID, null, this)

        gameState = GameState()
        gameState?.setListener(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        handler = Handler()
        val view = inflater.inflate(R.layout.fragment_game, container, false)
        view.newGameBtn.setOnClickListener { showNewGameDialog() }
        view.undoBtn.setOnClickListener { undoMove() }
        view.boardView.setListener(this)
        return view
    }

    override fun onResume() {
        super.onResume()

        updateViews()
    }

    override fun onTappedCell(coords: Point) {
        if (gameState?.canMove() == false) {
            return
        }

        val availableMoves = gameState?.game?.availableMoves
        for (move in availableMoves!!) {
            if (move == coords) {
                doMove(coords)
                break
            }
        }
    }

    private fun showNewGameDialog() {
        val dialog = StartGameDialog()
        dialog.show(fragmentManager, StartGameDialog.TAG)
    }

    fun startNewGame(threads: Int, time: Long) {
        gameState?.apply {
            startGame(threads, time)
            boardView.setGame(game)
            if (game.currentPlayer == cpu?.player) {
                calculateMove()
            }
        }
        updateViews()
    }

    private fun doMove(coords: Point) {
        gameState?.game?.apply {
            do_move(coords.x,coords.y)
            if (!isEnd && currentPlayer == gameState?.cpu?.player) {
                gameState?.calculateMove()
            } else if (isEnd) {
                val humanScore = getScore(gameState?.humanPlayer!!)
                val cpuScore = getScore(gameState?.cpu?.player!!)
                when {
                    humanScore > cpuScore -> Toast.makeText(context,"You win!",Toast.LENGTH_LONG).show()
                    humanScore < cpuScore -> Toast.makeText(context,"Cpu wins!",Toast.LENGTH_LONG).show()
                    else -> Toast.makeText(context,"A draw!",Toast.LENGTH_LONG).show()
                }
            }
        }
        updateViews()
    }

    private fun undoMove() {
        gameState?.game?.undo()
        while (gameState?.game?.currentPlayer != gameState?.humanPlayer && gameState?.game?.canUndo() == true) {
            gameState?.game?.undo()
        }
        if (gameState?.game?.currentPlayer == gameState?.cpu?.player) {
            gameState?.calculateMove()
        }
        updateViews()
    }

    private fun initializeViews() {
        boardView.setGame(gameState?.game)
    }

    private fun updateViews() {
        progressBar.visibility = if (gameState?.state == GameState.State.CALCULATING) View.VISIBLE else View.GONE
        undoBtn.isEnabled = canUndo()
        scoreTxt.text = gameState?.game?.run { "You: ${getScore(Game.FIRST)}, Cpu: ${getScore(Game.SECOND)}" } ?: "You: -, Cpu: -"
        gamesTxt.text = gameState?.run { "Simulated games: ${lastCalculatedMove?.games ?: '-'}" } ?: "Simulated games: -"

        boardView.invalidate()
    }

    private fun canUndo(): Boolean {
        return gameState?.canMove() == true && gameState?.game?.canUndo() == true
    }

    override fun onMoveCalculated(move: Move) {
        if (move.games == 0) {
            handler.postDelayed({ doMove(move.point) }, 250L)
        } else {
            handler.post({ doMove(move.point) })
        }
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<GameState> {
        Log.d(TAG,"onCreateLoader")
        return GameStateLoader(context!!)
    }

    override fun onLoadFinished(loader: Loader<GameState>, data: GameState?) {
        Log.d(TAG,"onLoadFinished")
        gameState = data
        gameState?.setListener(this)
        initializeViews()
        updateViews()
    }

    override fun onLoaderReset(loader: Loader<GameState>) {
        Log.d(TAG,"onLoaderReset")
    }

    companion object {
        const val TAG = "GameFragment"
        const val LOADER_ID = 6969
    }
}