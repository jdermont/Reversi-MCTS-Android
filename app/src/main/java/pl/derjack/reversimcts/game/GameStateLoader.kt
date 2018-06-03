package pl.derjack.reversimcts.game

import android.content.Context
import android.support.v4.content.Loader

class GameStateLoader(context: Context) : Loader<GameState>(context) {
    private var gameState: GameState? = null

    override fun onStartLoading() {
        if (gameState != null) {
            deliverResult(gameState)
            return
        }

        forceLoad()
    }

    override fun onForceLoad() {
        gameState = GameState()
        deliverResult(gameState)
    }
}
