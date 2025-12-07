package com.pli.formscanner

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pli.formscanner.databinding.ItemFieldBinding
import com.pli.formscanner.models.ExtractedField

class FieldAdapter(
    private val fields: List<ExtractedField>
) : RecyclerView.Adapter<FieldAdapter.FieldViewHolder>() {

    inner class FieldViewHolder(val binding: ItemFieldBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FieldViewHolder {
        val binding = ItemFieldBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return FieldViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FieldViewHolder, position: Int) {
        val field = fields[position]

        holder.binding.apply {
            tvFieldName.text = field.fieldLabel
            tvFieldValue.text = field.value
            tvConfidence.text = "${field.confidence}%"

            // Set confidence indicator color
            val color = when {
                field.confidence >= 90 -> Color.parseColor("#4CAF50") // Green
                field.confidence >= 75 -> Color.parseColor("#FF9800") // Orange
                else -> Color.parseColor("#F44336") // Red
            }
            confidenceIndicator.setBackgroundColor(color)
        }
    }

    override fun getItemCount() = fields.size
}