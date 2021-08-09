package com.tsourcecode.wiki.app

import android.app.AlertDialog
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.tsourcecode.wiki.lib.domain.backend.BackendController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditStateController(
        private val context: AppCompatActivity,
        private val backendController: BackendController,
) {
    private val btnCommit = context.findViewById<View>(R.id.btn_commit).apply {
        isEnabled = false
        setOnClickListener {
            it.isEnabled = false
            showCommitDialog()
        }
    }

    private fun showCommitDialog() {
        val input = EditText(context)
        AlertDialog.Builder(context)
                .setTitle("Enter commit message:")
                .setView(input)
                .setPositiveButton("OK") { dialog, which ->
                    val commitMessage = input.text.toString()

                    if (commitMessage.isEmpty()) {
                        return@setPositiveButton
                    }
                    GlobalScope.launch {
                        commit(commitMessage)
                        withContext(Dispatchers.Main) {
                            btnCommit.isEnabled = true
                        }
                    }
                }
                .setNegativeButton("Cancel") { dialog, which ->
                    dialog.cancel()
                    btnCommit.isEnabled = true
                }
                .setOnDismissListener {
                    btnCommit.isEnabled = true
                }
                .show()
    }
    private fun commit(message: String) {
        backendController.commit(message)
    }

    fun enableCommit() {
        btnCommit.isEnabled = true
    }
}