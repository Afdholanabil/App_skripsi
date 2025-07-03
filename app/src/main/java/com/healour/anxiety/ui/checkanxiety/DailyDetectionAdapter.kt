package com.healour.anxiety.ui.checkanxiety.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.healour.anxiety.R
import com.healour.anxiety.data.model.DailyDetectionData
import java.text.SimpleDateFormat
import java.util.Locale

class DailyDetectionAdapter(
    private val dailyDetections: List<Pair<String, DailyDetectionData>>
) : RecyclerView.Adapter<DailyDetectionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTanggal: TextView = view.findViewById(R.id.tv_tanggal)
        val ivSeeDetail: ImageView = view.findViewById(R.id.iv_see_detail)
        val llHarian: LinearLayout = view.findViewById(R.id.llHarian)
        val tvEmotion: TextView = view.findViewById(R.id.tvEmotion)
        val tvActivity: TextView = view.findViewById(R.id.tvActivity)
        val layoutGADScores: LinearLayout = view.findViewById(R.id.layoutGADScores)
        val tvTotalSkor: TextView = view.findViewById(R.id.tvTotalSkor) // Tambahkan referensi ke TextView total skor
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_deteksi_harian, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (dayNumber, dailyData) = dailyDetections[position]

        // Format tanggal
        val dateFormat = SimpleDateFormat("EEEE, dd-MM-yyyy", Locale("id", "ID"))
        val formattedDate = dateFormat.format(dailyData.tanggal.toDate())

        // Set tanggal
        holder.tvTanggal.text = "Hari -${dayNumber} | $formattedDate"

        // Setup toggle untuk melihat detail
        holder.ivSeeDetail.setOnClickListener {
            if (holder.llHarian.visibility == View.VISIBLE) {
                holder.llHarian.visibility = View.GONE
                holder.ivSeeDetail.setImageResource(R.drawable.add) // Ganti dengan icon plus
            } else {
                holder.llHarian.visibility = View.VISIBLE
                holder.ivSeeDetail.setImageResource(R.drawable.add_2) // Ganti dengan icon minus
            }
        }

        // Set emosi dan aktivitas
        holder.tvEmotion.text = "Emosi: ${dailyData.emosi}"
        holder.tvActivity.text = "Kegiatan: ${dailyData.kegiatan}"

        // Set total skor
        holder.tvTotalSkor.text = "Total Skor GAD 7: ${dailyData.totalSkor}"

        // Hapus semua view GAD lama
        holder.layoutGADScores.removeAllViews()

        // Tambahkan GAD scores
        addGADScoreTextView(holder, "GAD-1: ${getGADScoreText(dailyData.gad1)}", dailyData.gad1)
        addGADScoreTextView(holder, "GAD-2: ${getGADScoreText(dailyData.gad2)}", dailyData.gad2)
        addGADScoreTextView(holder, "GAD-3: ${getGADScoreText(dailyData.gad3)}", dailyData.gad3)
        addGADScoreTextView(holder, "GAD-4: ${getGADScoreText(dailyData.gad4)}", dailyData.gad4)
        addGADScoreTextView(holder, "GAD-5: ${getGADScoreText(dailyData.gad5)}", dailyData.gad5)
        addGADScoreTextView(holder, "GAD-6: ${getGADScoreText(dailyData.gad6)}", dailyData.gad6)
        addGADScoreTextView(holder, "GAD-7: ${getGADScoreText(dailyData.gad7)}", dailyData.gad7)
    }

    private fun addGADScoreTextView(holder: ViewHolder, scoreText: String, scoreValue: Int) {
        val textView = TextView(holder.itemView.context).apply {
            text = scoreText
            setPadding(0, 4, 0, 0)
            textSize = 14f // Sesuaikan dengan ukuran yang diinginkan
            setTypeface(resources.getFont(R.font.inter_light)) // Sesuaikan dengan font yang ada
        }
        holder.layoutGADScores.addView(textView)
    }

    private fun getGADScoreText(score: Int): String {
        return when (score) {
            0 -> "Tidak sama sekali"
            1 -> "Beberapa hari"
            2 -> "Lebih dari setengah hari"
            3 -> "Hampir setiap hari"
            else -> "Tidak diketahui"
        }
    }

    override fun getItemCount() = dailyDetections.size
}