import android.content.Context

import android.util.Log
import android.util.Log.e
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer

@Serializable
data class QuizCompletionEntry(
    val timestamp : Long,
    val score: Int,
    val userName: String
)

val Context.userProgressDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_progress")

object UserProgressKeys {
    val QUIZ_ANSWERS_MAP = stringPreferencesKey("quiz_answers_map")
    val CURRENT_QUESTION_INDEX = stringPreferencesKey("current_question_index")
    val QUIZ_COMPLETION_COUNT = intPreferencesKey("quiz_completion_count")
    val QUIZ_COMPLETION_HISTORY = stringPreferencesKey("quiz_completion_history")
}

class UserProgressRepository(private val context: Context) {
    private val mapSerializer = MapSerializer(Int.serializer(), Int.serializer())
    private val completionEntryListSerializer = ListSerializer(QuizCompletionEntry.serializer())
    private val dataStore = context.userProgressDataStore

    val savedAnswersFlow: Flow<Map<Int, Int>?> = context.userProgressDataStore.data
        .map { preferences ->
            preferences[UserProgressKeys.QUIZ_ANSWERS_MAP]?.let { jsonString ->
                try {
                    Json.decodeFromString(mapSerializer, jsonString)
                } catch (e: Exception) {
                    Log.e("userProgressRepo", "Error deserializing answers: ${e.message}")
                    null
                }
            }
        }

    suspend fun saveAnswers(answers: Map<Int, Int>) {
        val jsonString = Json.encodeToString(mapSerializer, answers)
        context.userProgressDataStore.edit { settings ->
            settings[UserProgressKeys.QUIZ_ANSWERS_MAP] = jsonString
        }
    }

    val savedQuestionIndexFlow: Flow<Int?> = context.userProgressDataStore.data
        .map { preferences ->
            preferences[UserProgressKeys.CURRENT_QUESTION_INDEX]?.toIntOrNull()
        }

    suspend fun saveCurrentQuestionIndex(index: Int) {
        context.userProgressDataStore.edit { settings ->
            settings[UserProgressKeys.CURRENT_QUESTION_INDEX] = index.toString()
        }
    }

    val QuizCompletionCountFlow: Flow<Int> = dataStore.data
        .map { preferences ->
            preferences[UserProgressKeys.QUIZ_COMPLETION_COUNT] ?: 0
        }

    suspend fun incrementQuizCompletionCount() {
        dataStore.edit { settings ->
            val currentCount = settings[UserProgressKeys.QUIZ_COMPLETION_COUNT] ?: 0
            settings[UserProgressKeys.QUIZ_COMPLETION_COUNT] = currentCount + 1
            Log.d("UserProgressRepo", "Quiz completion count incremented to: ${currentCount + 1}")
        }
    }

    val quizCompletionHistoryFlow: Flow<List<QuizCompletionEntry>> = dataStore.data
        .map { preferences ->
            preferences[UserProgressKeys.QUIZ_COMPLETION_HISTORY]?.let { jsonString ->
                try {
                    Json.decodeFromString(completionEntryListSerializer, jsonString)
                } catch (e: Exception) {
                    Log.e("UserProgressRepo", "Error deserializing quiz completion history: ${e.message}")
                    emptyList()
                }
            } ?:emptyList()
        }

    suspend fun addQuizCompletionEntry(userName: String, score: Int) {
        val newEntry = QuizCompletionEntry(
            timestamp = System.currentTimeMillis(),
            score = score,
            userName = userName
        )
        dataStore.edit { settings ->
            val currentHistoryJson = settings[UserProgressKeys.QUIZ_COMPLETION_HISTORY]
            val currentHistory = if (currentHistoryJson != null) {
                try {
                    Json.decodeFromString(completionEntryListSerializer, currentHistoryJson)
                } catch (e: Exception) {
                    Log.e("UserProgressRepo", "Error deserializing history before adding new entry: ${e.message}", e)
                    emptyList()
                }
            } else {
                emptyList()
            }

            val updatedHistory = currentHistory + newEntry
            settings[UserProgressKeys.QUIZ_COMPLETION_HISTORY] = Json.encodeToString(completionEntryListSerializer, updatedHistory)
            Log.d("UserProgressRepo", "Added new completion entry. History size: ${updatedHistory.size}")

            val currentCount = settings[UserProgressKeys.QUIZ_COMPLETION_COUNT] ?: 0
            settings[UserProgressKeys.QUIZ_COMPLETION_COUNT] = currentCount + 1
        }
    }

    suspend fun clearUserProgress() {
        context.userProgressDataStore.edit {
            it.remove(UserProgressKeys.QUIZ_ANSWERS_MAP)
            it.remove(UserProgressKeys.CURRENT_QUESTION_INDEX)
        }
    }
}