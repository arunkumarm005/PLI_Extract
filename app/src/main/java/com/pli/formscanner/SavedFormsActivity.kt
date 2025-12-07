package com.pli.formscanner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.gson.Gson
import com.pli.formscanner.database.FormEntity
import com.pli.formscanner.database.FormRepository
import com.pli.formscanner.databinding.ActivitySavedFormsBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SavedFormsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySavedFormsBinding
    private lateinit var repository: FormRepository
    private lateinit var adapter: SavedFormsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySavedFormsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = FormRepository(this)

        setupToolbar()
        setupRecyclerView()
        loadForms()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = "Saved Forms"
            setDisplayHomeAsUpEnabled(true)
        }
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = SavedFormsAdapter(
            onItemClick = { form -> viewFormDetails(form) },
            onDeleteClick = { form -> confirmDelete(form) }
        )
        
        binding.rvSavedForms.apply {
            layoutManager = LinearLayoutManager(this@SavedFormsActivity)
            this.adapter = this@SavedFormsActivity.adapter
        }
    }

    private fun loadForms() {
        binding.progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            repository.getAllForms().collect { forms ->
                binding.progressBar.visibility = View.GONE
                
                if (forms.isEmpty()) {
                    binding.tvEmpty.visibility = View.VISIBLE
                    binding.rvSavedForms.visibility = View.GONE
                } else {
                    binding.tvEmpty.visibility = View.GONE
                    binding.rvSavedForms.visibility = View.VISIBLE
                    adapter.submitList(forms)
                }
            }
        }
    }

    private fun viewFormDetails(form: FormEntity) {
        val gson = Gson()
        val fieldsMap = gson.fromJson(form.fieldsJson, Map::class.java)
        
        val details = buildString {
            append("Form ID: ${form.id}\n\n")
            append("Type: ${form.formType}\n")
            append("Status: ${if (form.isCompleted) "Completed" else "Draft"}\n")
            append("Created: ${formatDate(form.timestamp)}\n")
            append("Modified: ${formatDate(form.lastModified)}\n\n")
            append("Fields:\n")
            fieldsMap.forEach { (key, value) ->
                append("• $key: $value\n")
            }
        }
        
        AlertDialog.Builder(this)
            .setTitle("Form Details")
            .setMessage(details)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun confirmDelete(form: FormEntity) {
        AlertDialog.Builder(this)
            .setTitle("Delete Form")
            .setMessage("Are you sure you want to delete this form?")
            .setPositiveButton("Delete") { _, _ ->
                deleteForm(form)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteForm(form: FormEntity) {
        lifecycleScope.launch {
            try {
                repository.deleteForm(form)
                Toast.makeText(this@SavedFormsActivity, "Form deleted", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@SavedFormsActivity, "Delete failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    inner class SavedFormsAdapter(
        private val onItemClick: (FormEntity) -> Unit,
        private val onDeleteClick: (FormEntity) -> Unit
    ) : RecyclerView.Adapter<SavedFormsAdapter.ViewHolder>() {

        private val forms = mutableListOf<FormEntity>()

        fun submitList(newForms: List<FormEntity>) {
            forms.clear()
            forms.addAll(newForms)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(
                R.layout.item_saved_form,
                parent,
                false
            )
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(forms[position])
        }

        override fun getItemCount() = forms.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val cardView: MaterialCardView = view.findViewById(R.id.cardView)
            private val tvId: TextView = view.findViewById(R.id.tvId)
            private val tvType: TextView = view.findViewById(R.id.tvType)
            private val tvStatus: TextView = view.findViewById(R.id.tvStatus)
            private val tvDate: TextView = view.findViewById(R.id.tvDate)
            private val btnDelete: View = view.findViewById(R.id.btnDelete)

            fun bind(form: FormEntity) {
                tvId.text = "Form #${form.id}"
                tvType.text = form.formType
                tvStatus.text = if (form.isCompleted) "Completed" else "Draft"
                tvDate.text = formatDate(form.timestamp)

                cardView.setOnClickListener { onItemClick(form) }
                btnDelete.setOnClickListener { onDeleteClick(form) }
            }
        }
    }
}
