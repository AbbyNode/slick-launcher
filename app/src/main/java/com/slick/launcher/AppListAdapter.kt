package com.slick.launcher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppListAdapter(
    private val items: List<AppListItem>,
    private val onAppClicked: (AppInfo) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_APP = 1
    }

    inner class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val letterText: TextView = view.findViewById(R.id.text_letter)
    }

    inner class AppViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val appIcon: ImageView = view.findViewById(R.id.app_icon)
        val appName: TextView = view.findViewById(R.id.app_name)
    }

    override fun getItemViewType(position: Int) = when (items[position]) {
        is AppListItem.Header -> TYPE_HEADER
        is AppListItem.App    -> TYPE_APP
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(
                inflater.inflate(R.layout.item_section_header, parent, false)
            )
            else -> AppViewHolder(
                inflater.inflate(R.layout.item_app, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is AppListItem.Header -> (holder as HeaderViewHolder).letterText.text =
                item.letter.toString()

            is AppListItem.App -> (holder as AppViewHolder).apply {
                appIcon.setImageDrawable(item.appInfo.icon)
                appName.text = item.appInfo.label
                itemView.setOnClickListener { v ->
                    // Subtle press-and-release scale animation before launching
                    v.animate()
                        .scaleX(0.94f).scaleY(0.94f)
                        .setDuration(70)
                        .setInterpolator(DecelerateInterpolator())
                        .withEndAction {
                            v.animate()
                                .scaleX(1f).scaleY(1f)
                                .setDuration(130)
                                .setInterpolator(DecelerateInterpolator())
                                .withEndAction { onAppClicked(item.appInfo) }
                                .start()
                        }
                        .start()
                }
            }
        }
    }

    override fun getItemCount(): Int = items.size
}
