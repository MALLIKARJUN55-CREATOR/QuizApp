package com.example.quizapp

import androidx.core.text.HtmlCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.URL

data class Question(val text: String, val correct: String, val options: List<String>)

object Api {
    private fun decode(s: String) =
        HtmlCompat.fromHtml(s, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()

    private suspend fun get(url: String): JSONObject = withContext(Dispatchers.IO) {
        JSONObject(URL(url).readText())
    }

    suspend fun categories(): List<Pair<Int, String>> {
        val arr = get("https://opentdb.com/api_category.php").getJSONArray("trivia_categories")
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it); o.getInt("id") to decode(o.getString("name"))
        }
    }

    suspend fun questions(category: Int, difficulty: String): List<Question> {
        val json = get("https://opentdb.com/api.php?amount=10&category=$category&difficulty=$difficulty&type=multiple")
        if (json.getInt("response_code") != 0) throw IOException("No questions available (code ${json.getInt("response_code")})")
        val arr = json.getJSONArray("results")
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            val correct = decode(o.getString("correct_answer"))
            val wrong = o.getJSONArray("incorrect_answers")
            val options = (0 until wrong.length()).map { i -> decode(wrong.getString(i)) } + correct
            Question(decode(o.getString("question")), correct, options.shuffled())
        }
    }
}
