package com.fitcoachai.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitcoachai.app.data.model.ExerciseResult
import com.fitcoachai.app.data.model.RawExercise
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject
import android.util.Log

sealed class ExerciseState {
    object Loading : ExerciseState()
    data class Success(val exercises: List<ExerciseResult>) : ExerciseState()
    data class Error(val message: String) : ExerciseState()
}

class ExerciseViewModel : ViewModel() {

    private val _exerciseState = MutableStateFlow<ExerciseState>(ExerciseState.Loading)
    val exerciseState: StateFlow<ExerciseState> = _exerciseState

    private val _muscleExercises = MutableStateFlow<List<ExerciseResult>>(emptyList())
    val muscleExercises: StateFlow<List<ExerciseResult>> = _muscleExercises

    private val _burnExercises = MutableStateFlow<List<ExerciseResult>>(emptyList())
    val burnExercises: StateFlow<List<ExerciseResult>> = _burnExercises

    private val _powerExercises = MutableStateFlow<List<ExerciseResult>>(emptyList())
    val powerExercises: StateFlow<List<ExerciseResult>> = _powerExercises

    private val _flowExercises = MutableStateFlow<List<ExerciseResult>>(emptyList())
    val flowExercises: StateFlow<List<ExerciseResult>> = _flowExercises

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private var allExercises: List<ExerciseResult> = emptyList()
    
    private val source1Url = "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/dist/exercises.json"
    private val source2Url = "https://wger.de/api/v2/exercise/?language=2&limit=200"

    init {
        loadMatrixArsenal()
    }

    private fun loadMatrixArsenal() {
        viewModelScope.launch {
            _exerciseState.value = ExerciseState.Loading
            try {
                val githubResults = mutableListOf<ExerciseResult>()
                
                // --- SOURCE 1: GITHUB DB (Visuals & Flow) ---
                val rawJson1 = withContext(Dispatchers.IO) { URL(source1Url).readText() }
                val rawList1: List<RawExercise> = Gson().fromJson(rawJson1, object : TypeToken<List<RawExercise>>() {}.type)
                githubResults.addAll(rawList1.map { raw ->
                    ExerciseResult(
                        id = raw.id.hashCode(),
                        uuid = raw.id,
                        name = raw.name,
                        category = raw.category,
                        muscle = raw.primaryMuscles.firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "Total Body",
                        imageUrl = if (raw.images.isNotEmpty()) "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/exercises/${raw.images.first()}" else null,
                        description = raw.instructions.joinToString(" "),
                        instructions = raw.instructions,
                        level = raw.level,
                        equipment = raw.equipment ?: "Poids du corps",
                        force = raw.force,
                        secondaryMuscles = raw.secondaryMuscles,
                        safetyTips = when {
                            raw.level == "expert" -> "Mouvement technique. Gardez une forme parfaite."
                            raw.category == "powerlifting" -> "Contrôlez la descente et évitez les à-coups."
                            else -> "Gardez le dos droit et contrôlez le mouvement."
                        }
                    )
                })

                // --- SOURCE 2: WGER API (Technical Context) ---
                val wgerResults = mutableListOf<ExerciseResult>()
                try {
                    val rawJson2 = withContext(Dispatchers.IO) { URL(source2Url).readText() }
                    val wgerObj = JSONObject(rawJson2)
                    val wgerArray = wgerObj.getJSONArray("results")
                    for (i in 0 until wgerArray.length()) {
                        val item = wgerArray.getJSONObject(i)
                        val name = item.getString("name")
                        if (name.length > 3) {
                            wgerResults.add(ExerciseResult(
                                id = item.getInt("id") + 100000,
                                name = name,
                                category = "Technique",
                                description = cleanHtml(item.getString("description")),
                                level = "Expert",
                                equipment = "Variable",
                                muscle = "Complexe"
                            ))
                        }
                    }
                } catch (e: Exception) { Log.e("ARSENAL", "Wger Link unstable") }

                // 🧬 NEURAL MERGE: Prioritize GitHub visuals but include Wger's technical diversity
                allExercises = (githubResults + wgerResults).distinctBy { it.name.lowercase() }.shuffled()
                
                _exerciseState.value = ExerciseState.Success(allExercises)
                populateGoalStreams()
            } catch (e: Exception) {
                _exerciseState.value = ExerciseState.Error("ARSENAL_SYNC_FAILED")
            }
        }
    }

    private fun cleanHtml(html: String): String {
        return html.replace(Regex("<[^>]*>"), "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .trim()
    }

    private fun populateGoalStreams() {
        _muscleExercises.value = allExercises.filter { ex ->
            val cat = ex.category.lowercase()
            val equ = ex.equipment?.lowercase() ?: ""
            !cat.contains("cardio") && (equ.contains("dumbbell") || equ.contains("barbell") || equ.contains("machine") || equ.contains("bench"))
        }.take(15)

        _burnExercises.value = allExercises.filter { ex ->
            val cat = ex.category.lowercase()
            val mus = ex.muscle.lowercase()
            cat.contains("cardio") || mus.contains("legs") || mus.contains("back") || cat.contains("full")
        }.take(15)

        _powerExercises.value = allExercises.filter { ex ->
            ex.level == "expert" || ex.level == "intermediate" || ex.force == "pull" || ex.force == "push"
        }.take(15)

        _flowExercises.value = allExercises.filter { ex ->
            val cat = ex.category.lowercase()
            cat.contains("stretching") || cat.contains("yoga") || ex.equipment?.lowercase()?.contains("body only") == true || ex.equipment?.lowercase()?.contains("poids du corps") == true
        }.take(15)
    }

    fun searchExercises(query: String) {
        _searchQuery.value = query
        val list = if (query.isEmpty()) allExercises else allExercises.filter { 
            it.name.contains(query, true) || it.category.contains(query, true) || it.muscle.contains(query, true) 
        }
        _exerciseState.value = ExerciseState.Success(list)
    }

    fun loadExercisesByObjectif(objectif: String) {
        val obj = objectif.lowercase()
        val filtered = allExercises.filter { ex ->
            val cat = ex.category.lowercase()
            val mus = ex.muscle.lowercase()
            when {
                obj.contains("poids") || obj.contains("perdre") -> cat.contains("cardio") || mus.contains("legs")
                obj.contains("muscle") || obj.contains("masse") -> !cat.contains("cardio") && !cat.contains("stretch")
                else -> true
            }
        }
        _exerciseState.value = ExerciseState.Success(filtered.take(40))
    }
}
