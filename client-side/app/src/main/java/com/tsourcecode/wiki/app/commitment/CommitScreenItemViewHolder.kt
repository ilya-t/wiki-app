package com.tsourcecode.wiki.app.commitment

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.tsourcecode.wiki.app.R
import com.tsourcecode.wiki.lib.domain.commitment.StatusModel
import com.tsourcecode.wiki.lib.domain.commitment.StatusViewItem

class CommitScreenItemViewHolder(
        private val itemView: View,
        private val binder: (StatusViewItem) -> Unit,

) : RecyclerView.ViewHolder(itemView) {
    fun bind(item: StatusViewItem) {
        binder.invoke(item)
    }

    companion object {
        fun createMessageHolder(c: Context, statusModel: StatusModel): CommitScreenItemViewHolder {
            val root = LayoutInflater.from(c).inflate(R.layout.commit_item_message, null)
            val input = root.findViewById<AppCompatEditText>(R.id.commit_message_input)
            input.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

                override fun afterTextChanged(s: Editable?) {
                    statusModel.updateCommitText(s.toString())
                }

            })
            val confirm = root.findViewById<AppCompatButton>(R.id.commit_message_confirm_button)
            confirm.setOnClickListener {
                statusModel.commit(input.text.toString())
            }
            val binder = { item: StatusViewItem ->
                val commitItem = item as StatusViewItem.CommitViewItem
                input.setText(commitItem.commitMessage)
            }
            return CommitScreenItemViewHolder(root, binder)
        }

        fun createDiffHolder(c: Context): CommitScreenItemViewHolder {
            val root = LayoutInflater.from(c).inflate(R.layout.commit_item_diff, null)
            val fileName = root.findViewById<AppCompatTextView>(R.id.commit_diff_filename)
            val binder = { item: StatusViewItem ->
                val diffItem = item as StatusViewItem.FileViewItem
                fileName.text = diffItem.fileStatus.status.toString() + " : " +  diffItem.fileStatus.path
            }
            return CommitScreenItemViewHolder(root, binder)
        }
    }
}