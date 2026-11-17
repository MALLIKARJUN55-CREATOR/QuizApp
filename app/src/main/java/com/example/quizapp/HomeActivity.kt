package com.example.quizapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.quizapp.databinding.ActivityHomeBinding
import com.example.quizapp.databinding.ItemScoreBinding
import com.google.android.gms.code_scanner.GmsBarcodeScanning
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class HomeActivity : AppCompatActivity() {
    private lateinit var b: ActivityHomeBinding
    private var cats = listOf<Pair<Int, String>>()
    private val levels = listOf("easy", "medium", "hard")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val user = try { FirebaseAuth.getInstance().currentUser } catch (e: Exception) { null }
        if (user == null) { startActivity(Intent(this, LoginActivity::class.java)); finish(); return }
        b = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.tvUser.text = "Hi, ${user.displayName ?: user.email}"
        b.spDifficulty.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, levels)

        lifecycleScope.launch {
            try {
                cats = Api.categories()
                b.spCategory.adapter = ArrayAdapter(
                    this@HomeActivity, android.R.layout.simple_spinner_dropdown_item, cats.map { it.second })
            } catch (e: Exception) { toast("Couldn't load categories: ${e.message}") }
        }

        b.btnStart.setOnClickListener {
            val c = cats.getOrNull(b.spCategory.selectedItemPosition) ?: return@setOnClickListener toast("Categories not loaded yet")
            startQuiz(c.first, c.second, levels[b.spDifficulty.selectedItemPosition])
        }

        // Google Code Scanner – QR text: "category=9;difficulty=easy"
        b.btnScan.setOnClickListener {
            GmsBarcodeScanning.getClient(this).startScan()
                .addOnSuccessListener { handleQr(it.rawValue) }
                .addOnFailureListener { toast("Scan failed: ${it.message}") }
        }

        val adapter = ScoreAdapter()
        b.rvHistory.layoutManager = LinearLayoutManager(this)
        b.rvHistory.adapter = adapter
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                AppDb.get(this@HomeActivity).dao().history(user.uid).collect { adapter.submitList(it) }
            }
        }

        b.btnSignOut.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            LoginActivity.client(this).signOut()
            startActivity(Intent(this, LoginActivity::class.java)); finish()
        }
    }

    private fun handleQr(text: String?) {
        val map = text.orEmpty().split(";").mapNotNull {
            val p = it.split("=", limit = 2)
            if (p.size == 2) p[0].trim().lowercase() to p[1].trim() else null
        }.toMap()
        val id = map["category"]?.toIntOrNull()
        val diff = map["difficulty"]?.lowercase()
        if (id == null || diff !in levels) return toast("Invalid QR. Expected category=9;difficulty=easy")
        startQuiz(id, cats.firstOrNull { it.first == id }?.second ?: "Category $id", diff!!)
    }

    private fun startQuiz(id: Int, name: String, diff: String) = startActivity(
        Intent(this, QuizActivity::class.java)
            .putExtra(QuizActivity.CAT_ID, id).putExtra(QuizActivity.CAT_NAME, name).putExtra(QuizActivity.DIFF, diff))

    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_LONG).show()
}

class ScoreAdapter : ListAdapter<ScoreEntry, ScoreAdapter.VH>(object : DiffUtil.ItemCallback<ScoreEntry>() {
    override fun areItemsTheSame(a: ScoreEntry, b: ScoreEntry) = a.id == b.id
    override fun areContentsTheSame(a: ScoreEntry, b: ScoreEntry) = a == b
}) {
    class VH(val b: ItemScoreBinding) : RecyclerView.ViewHolder(b.root)
    private val fmt = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(ItemScoreBinding.inflate(LayoutInflater.from(p.context), p, false))
    override fun onBindViewHolder(h: VH, i: Int) {
        val e = getItem(i)
        h.b.tvCategory.text = "${e.category} (${e.difficulty})"
        h.b.tvDate.text = fmt.format(Date(e.date))
        h.b.tvScore.text = "${e.score}/10"
    }
}
