package com.example.app_skripsi.ui.checkanxiety.shortdetection

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.app_skripsi.R
import com.example.app_skripsi.data.model.ShortDetectionModel
import com.example.app_skripsi.databinding.ItemRiwayatShortDetectionBinding

class ShortDetectionAdapter(private val shortDetectionList: List<ShortDetectionModel>) :
    RecyclerView.Adapter<ShortDetectionAdapter.ShortDetectionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShortDetectionViewHolder {
        val binding = ItemRiwayatShortDetectionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ShortDetectionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ShortDetectionViewHolder, position: Int) {
        val item = shortDetectionList[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int {
        return shortDetectionList.size
    }

    class ShortDetectionViewHolder(private val binding: ItemRiwayatShortDetectionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ShortDetectionModel) {
            // Format tanggal dengan SimpleDateFormat jika diperlukan
            val formattedDate = item.tanggal.toDate().toString()

            // Bind data ke item layout
            binding.tvDate.text = "Hari: ${item.hari}, ${formattedDate}"
            binding.tvEmotion.text = "Emosi: ${item.emosi}"
            binding.tvActivity.text = "Kegiatan: ${item.kegiatan}"
            binding.tvskorGAD.text = "Total Skor GAD 7: ${item.total_skor}"

            // GAD Scores
            binding.layoutGADScores.removeAllViews()
            val gadScores = listOf(
                "GAD-1: ${item.gad1}",
                "GAD-2: ${item.gad2}",
                "GAD-3: ${item.gad3}",
                "GAD-4: ${item.gad4}",
                "GAD-5: ${item.gad5}",
                "GAD-6: ${item.gad6}",
                "GAD-7: ${item.gad7}"
            )

            gadScores.forEach { score ->
                val textView = TextView(binding.root.context).apply {
                    text = score
                    textSize = 14f
                    setTypeface(resources.getFont(R.font.inter_regular))
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                }
                binding.layoutGADScores.addView(textView)
            }
        }
    }
}
