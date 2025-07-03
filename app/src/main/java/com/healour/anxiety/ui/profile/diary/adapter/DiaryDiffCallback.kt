package com.healour.anxiety.ui.profile.diary.adapter

import androidx.recyclerview.widget.DiffUtil
import com.healour.anxiety.data.model.DiaryModel

class DiaryDiffCallback(
    private val oldList: List<DiaryModel>,
    private val newList: List<DiaryModel>
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldList.size
    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].tanggal == newList[newItemPosition].tanggal
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition] // Data class sudah memiliki equals()
    }
}
