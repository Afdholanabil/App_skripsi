package com.example.app_skripsi.ui.dashboard.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.app_skripsi.data.model.VideoModel
import com.example.app_skripsi.databinding.ItemVideoBinding

class VideoAdapter(private val videoList: List<VideoModel>) : RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {

    inner class VideoViewHolder(val binding: ItemVideoBinding) : RecyclerView.ViewHolder(binding.root)
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): VideoAdapter.VideoViewHolder {
        val binding = ItemVideoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VideoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VideoAdapter.VideoViewHolder, position: Int) {
        val video = videoList[position]
        holder.binding.tvTitle.text = video.title
        holder.binding.tvDescription.text = video.description
        holder.binding.imgThumbnail.setImageResource(video.imageRes)
    }

    override fun getItemCount(): Int {
       return videoList.size
    }
}