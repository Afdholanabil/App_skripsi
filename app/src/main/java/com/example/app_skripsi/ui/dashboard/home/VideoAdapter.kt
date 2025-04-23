package com.example.app_skripsi.ui.dashboard.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.app_skripsi.R
import com.example.app_skripsi.data.model.VideoModel
import com.example.app_skripsi.databinding.ItemVideoBinding

class VideoAdapter(
    private val videoList: List<VideoModel>,
    private val onItemClick: (VideoModel) -> Unit
) : RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {

    inner class VideoViewHolder(val binding: ItemVideoBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val binding = ItemVideoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VideoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = videoList[position]
        holder.binding.tvTitle.text = video.title

        // Set thumbnail
        if (video.thumbnailUrl != 0) {
            holder.binding.imgThumbnail.setImageResource(video.thumbnailUrl)
        } else {
            // Default thumbnail
            holder.binding.imgThumbnail.setImageResource(R.drawable.video_thumbnail)
        }

        // Add category badge if needed
//        holder.binding.tvCategory.text = video.category

        holder.binding.root.setOnClickListener { onItemClick(video) }
    }

    override fun getItemCount(): Int {
        return videoList.size
    }
}