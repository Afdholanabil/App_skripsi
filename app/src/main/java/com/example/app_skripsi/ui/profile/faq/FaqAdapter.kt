package com.example.app_skripsi.ui.profile.faq

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.app_skripsi.R
import com.example.app_skripsi.data.model.FaqModel
import com.example.app_skripsi.databinding.ItemFaqBinding

class FaqAdapter(private val faqList: List<FaqModel>) : RecyclerView.Adapter<FaqAdapter.FaqViewHolder>() {

    class FaqViewHolder(val binding: ItemFaqBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FaqViewHolder {
        val binding = ItemFaqBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FaqViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FaqViewHolder, position: Int) {
        val faq = faqList[position]

        holder.binding.tvTitleFaq.text = faq.question
        holder.binding.tvDescFaq.text = faq.answer

        // Set visibility berdasarkan status isExpanded
        holder.binding.tvDescFaq.visibility = if (faq.isExpanded) View.VISIBLE else View.GONE
        holder.binding.ivSeeDetail.setImageResource(if (faq.isExpanded) R.drawable.add_2 else R.drawable.add)

        // Handle klik untuk expand/hide
        holder.binding.root.setOnClickListener {
            faq.isExpanded = !faq.isExpanded
            notifyItemChanged(position)
        }

        holder.binding.ivSeeDetail.setOnClickListener {
            faq.isExpanded = !faq.isExpanded
            notifyItemChanged(position)
        }
    }

    override fun getItemCount(): Int = faqList.size
}
