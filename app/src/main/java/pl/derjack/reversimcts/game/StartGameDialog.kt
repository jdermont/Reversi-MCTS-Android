package pl.derjack.reversimcts.game

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.dialog_new_game.view.*
import pl.derjack.reversimcts.R

class StartGameDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_new_game, null, false)
        val threadList = (1..Runtime.getRuntime().availableProcessors()).map { it.toString() }
        val spinnerArrayAdapter = ArrayAdapter<String>(context, android.R.layout.simple_spinner_item,
                threadList)
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        view.threadSpinner.adapter = spinnerArrayAdapter

        val timeList = listOf(0.1f,0.5f,1.0f,1.5f,2.0f,2.5f,3.0f).map { "$it s" }
        val timeSpinnerArrayAdapter = ArrayAdapter<String>(context, android.R.layout.simple_spinner_item,
                timeList)
        timeSpinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        view.timeSpinner.adapter = timeSpinnerArrayAdapter

        val alertDialogBuilder = AlertDialog.Builder(activity)
        alertDialogBuilder.setView(view)
        alertDialogBuilder.setTitle("New Game")
        alertDialogBuilder.setPositiveButton("OK", { dialog, which ->
            val threads =  view.threadSpinner.selectedItem.toString().toInt()
            val time = (view.timeSpinner.selectedItem.toString().split(" ")[0].toFloat() * 1000L).toLong()

            val fragment = fragmentManager?.findFragmentByTag(GameFragment.TAG) as GameFragment
            fragment.startNewGame(threads,time)
            dialog?.dismiss()
        })
        alertDialogBuilder.setNegativeButton("Cancel", { dialog, which ->
            dialog?.dismiss()
        })

        return alertDialogBuilder.create()
    }

    companion object {
        const val TAG = "StartGameDialog"
    }
}
