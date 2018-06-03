package pl.derjack.reversimcts

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import pl.derjack.reversimcts.game.GameFragment


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .add(R.id.container, GameFragment(),GameFragment.TAG)
                    .commit()
        }

    }

}
