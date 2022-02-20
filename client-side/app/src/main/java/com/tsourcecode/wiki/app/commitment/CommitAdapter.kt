package com.tsourcecode.wiki.app.commitment

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tsourcecode.wiki.lib.domain.commitment.StatusModel
import com.tsourcecode.wiki.lib.domain.commitment.StatusViewItem
import com.tsourcecode.wiki.lib.domain.commitment.StatusViewModel

private const val TYPE_COMMIT_ITEM = 0
private const val TYPE_FILE_DIFF_ITEM = 1

class CommitAdapter(private val statusModel: StatusModel) : RecyclerView.Adapter<CommitScreenItemViewHolder>() {
    var viewModel: StatusViewModel = StatusViewModel()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): CommitScreenItemViewHolder {
        return when (type) {
            TYPE_COMMIT_ITEM -> CommitScreenItemViewHolder.createMessageHolder(parent.context, statusModel)
            TYPE_FILE_DIFF_ITEM -> CommitScreenItemViewHolder.createDiffHolder(parent.context)
            else -> throw IllegalStateException("Unsupported type $type")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (viewModel.items[position]) {
            is StatusViewItem.CommitViewItem -> TYPE_COMMIT_ITEM
            is StatusViewItem.FileViewItem -> TYPE_FILE_DIFF_ITEM
        }
    }

    override fun onBindViewHolder(vh: CommitScreenItemViewHolder, position: Int) {
        vh.bind(viewModel.items[position])
    }

    override fun getItemCount(): Int {
        return viewModel.items.size
    }
}
