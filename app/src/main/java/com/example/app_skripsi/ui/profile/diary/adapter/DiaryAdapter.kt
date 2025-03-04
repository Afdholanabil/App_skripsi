package com.example.app_skripsi.ui.profile.diary.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.app_skripsi.data.model.DiaryModel
import com.example.app_skripsi.databinding.ItemBukuHarianBinding
import com.example.app_skripsi.utils.EmotionUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DiaryAdapter(private var diaryList: List<DiaryModel>) :
    RecyclerView.Adapter<DiaryAdapter.DiaryViewHolder>() {

    class DiaryViewHolder(val binding: ItemBukuHarianBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiaryViewHolder {
        val binding = ItemBukuHarianBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DiaryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DiaryViewHolder, position: Int) {
        val diary = diaryList[position]

        // Ambil data dari `isiDiary`
        val title = diary.isiDiary["judul"] as? String ?: "Tanpa Judul"
        val content = diary.isiDiary["isi"] as? String ?: "Tidak ada isi"
        val activity = diary.isiDiary["kegiatan"] as? String ?: "Tidak diketahui"
        val emotion = diary.isiDiary["emosi"] as? String ?: "Normal"

        // Format timestamp ke tanggal yang mudah dibaca
        val formattedDate = formatTimestamp(diary.tanggal.seconds)

        // Set data ke tampilan
        holder.binding.tvDate.text = formattedDate
        holder.binding.tvTitle.text = title
        holder.binding.tvContent.text = content
        holder.binding.tvActivityType.text = activity
        holder.binding.ivEmotion.setImageResource(EmotionUtils.getEmotionIcon(emotion))
    }

    override fun getItemCount(): Int = diaryList.size

    /** 🔹 Fungsi untuk memperbarui data dengan `DiffUtil` */
    fun updateData(newDiaryList: List<DiaryModel>) {
        val diffCallback = DiaryDiffCallback(diaryList, newDiaryList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        diaryList = newDiaryList
        diffResult.dispatchUpdatesTo(this) // 🔥 Perbarui UI hanya untuk item yang berubah
    }

    /** 🔹 Fungsi untuk mengubah Timestamp ke format tanggal */
    private fun formatTimestamp(timestampSeconds: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        val date = Date(timestampSeconds * 1000) // Ubah detik ke milidetik
        return sdf.format(date)
    }
}