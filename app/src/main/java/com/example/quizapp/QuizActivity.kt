package com.example.quizapp

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.quizapp.databinding.ActivityQuizBinding
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class QuizActivity : AppCompatActivity() {
    companion object { const val CAT_ID = "cat_id"; const val CAT_NAME = "cat_name"; const val DIFF = "diff" }

    private lateinit var b: ActivityQuizBinding
    private var questions = listOf<Question>()
    private var index = 0
    private var score = 0
    private var answered = false
    private val green = ColorStateList.valueOf(Color.parseColor("#2E7D32"))
    private val red = ColorStateList.valueOf(Color.parseColor("#C62828"))
    private lateinit var defaultTint: ColorStateList
    private val buttons by lazy { listOf(b.b0, b.b1, b.b2, b.b3) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityQuizBinding.inflate(layoutInflater)
        setContentView(b.root)
        defaultTint = b.b0.backgroundTintList!!
        buttons.forEachIndexed { i, btn -> btn.setOnClickListener { answer(i) } }
        b.btnNext.setOnClickListener { index++; if (index < questions.size) showQuestion() else showResult() }
        b.btnPlayAgain.setOnClickListener { load() }
        b.btnHome.setOnClickListener { finish() }
        load()
    }

    private fun load() {
        b.loading.visibility = View.VISIBLE
        b.quizGroup.visibility = View.GONE
        b.resultGroup.visibility = View.GONE
        lifecycleScope.launch {
            try {
                questions = Api.questions(intent.getIntExtra(CAT_ID, 9), intent.getStringExtra(DIFF) ?: "easy")
                index = 0; score = 0
                showQuestion()
            } catch (e: Exception) {
                Toast.makeText(this@QuizActivity, "Couldn't load quiz: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun showQuestion() {
        val q = questions[index]
        answered = false
        b.loading.visibility = View.GONE
        b.resultGroup.visibility = View.GONE
        b.quizGroup.visibility = View.VISIBLE
        b.tvProgress.text = "Question ${index + 1} / ${questions.size}"
        b.progressBar.max = questions.size
        b.progressBar.setProgressCompat(index + 1, true)
        b.tvQuestion.text = q.text
        buttons.forEachIndexed { i, btn -> btn.text = q.options[i]; btn.backgroundTintList = defaultTint }
        b.btnNext.visibility = View.INVISIBLE
        b.btnNext.text = if (index == questions.lastIndex) "Finish" else "Next"
    }

    private fun answer(i: Int) {
        if (answered) return
        answered = true
        val q = questions[index]
        val correctIdx = q.options.indexOf(q.correct)
        if (i == correctIdx) score++ else buttons[i].backgroundTintList = red
        buttons[correctIdx].backgroundTintList = green
        b.btnNext.visibility = View.VISIBLE
    }

    private fun showResult() {
        b.quizGroup.visibility = View.GONE
        b.resultGroup.visibility = View.VISIBLE
        b.tvScore.text = "$score / ${questions.size}"
        val entry = ScoreEntry(
            uid = FirebaseAuth.getInstance().currentUser?.uid ?: "anon",
            date = System.currentTimeMillis(),
            category = intent.getStringExtra(CAT_NAME) ?: "",
            difficulty = intent.getStringExtra(DIFF) ?: "",
            score = score)
        lifecycleScope.launch { AppDb.get(this@QuizActivity).dao().insert(entry) }
    }
}
