package com.example.eeeeeee

import UserProgressKeys
import UserProgressRepository
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

import androidx.compose.foundation.lazy.*
import androidx.compose.material3.Card
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Button
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.graphics.Color
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.Flow

import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlin.text.toIntOrNull
import androidx.compose.runtime.collectAsState
import userProgressDataStore

import android.media.AudioAttributes
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.ui.layout.ContentScale
import androidx.media3.common.BuildConfig
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

       setContent {
           MyApp()
       }
    }
}

data class QuizQuestion (
    val id: Int,
    val questionText: String
)

val allQuizQuestions = listOf(
    QuizQuestion(id = 1, questionText = "Repeated, disturbing, and unwanted memories of the stressful experience?"),
    QuizQuestion(id = 2, questionText = "Repeated, disturbing dreams of the stressful experience?"),
    QuizQuestion(id = 3, questionText = "Suddenly feeling or acting as if the stressful experience were actually happening again (as if you were actually back there reliving it)?"),
    QuizQuestion(id = 4, questionText = "Feeling very upset when something reminded you of the stressful experience?"),
    QuizQuestion(id = 5, questionText = "Having strong physical reactions when something reminded you of the stressful experience (for example: heart pounding, trouble breathing, sweating)?"),
    QuizQuestion(id = 6, questionText = "Avoiding memories, thoughts, or feelings related to the stressful experience?"),
    QuizQuestion(id = 7, questionText = "Avoiding external reminders of the stressful experience\n" +
            "(for example: people, places, conversations, activities,\n" +
            "objects, or situations)?"),
    QuizQuestion(id = 8, questionText = "Trouble remembering important parts of the stressful experience?"),
    QuizQuestion(id = 9, questionText = "Having strong negative beliefs about yourself, other people, or the world (for example, having thoughts such as:\n" +
            "I am bad, there is something seriously wrong with me, no one can be trusted, the world is completely dangerous)?"),
    QuizQuestion(id = 10, questionText = "Blaming yourself or someone else for the stressful experience or what happened after it?"),
    QuizQuestion(id = 11, questionText = "Having strong negative feelings such as fear, horror, anger, guilt, or shame?"),
    QuizQuestion(id = 12, questionText = "Loss of interest in activities that you used to enjoy?"),
    QuizQuestion(id = 13, questionText = "Feeling distant or cut off from other people?"),
    QuizQuestion(id = 14, questionText = "Trouble experiencing positive feelings (for example: being unable to feel happiness or have loving feelings for people close to you)?"),
    QuizQuestion(id = 15, questionText = "Irritable behavior, angry outbursts, or acting aggressively?"),
    QuizQuestion(id = 16, questionText = "Taking too many risks or doing things that could cause you harm?"),
    QuizQuestion(id = 17, questionText = "Being “superalert” or watchful or on guard?"),
    QuizQuestion(id = 18, questionText = "Feeling jumpy or easily startled?"),
    QuizQuestion(id = 19, questionText = "Having difficulty concentrating?"),
    QuizQuestion(id = 20, questionText = "Trouble falling or staying asleep? "),
)

data class QuizUiState(
    val currentQuestionIndex: Int = 0,
    val currentQuestion: QuizQuestion? = null, 
    val answers: Map<Int, Int> = emptyMap(),
    val isQuizFinished: Boolean = false,
    val isLoading: Boolean = true
)

val Context.userProgressDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_progress")

object UserProgressKeys {
    val QUIZ_ANSWERS_MAP = stringPreferencesKey("quiz_answers_map")
    val CURRENT_QUESTION_INDEX =
        stringPreferencesKey("current_question_index") 
    val QUIZ_COMPLETION_COUNT = intPreferencesKey("quiz_completion_count")
}

class UserProgressRepository(private val context: Context) {
    private val mapSerializer = MapSerializer(Int.serializer(), Int.serializer())
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

    suspend fun clearUserProgress() {
        context.userProgressDataStore.edit {
            it.remove(UserProgressKeys.QUIZ_ANSWERS_MAP)
            it.remove(UserProgressKeys.CURRENT_QUESTION_INDEX)
        }
        Log.d("UserProgressRepo", "clearUserProgress: Cleared answers map and current question index.") 
    }
}


class QuizViewModel(private val userProgressRepository: UserProgressRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    init {
        Log.d("QuizViewModel", "ViewModel initialized. Calling loadInitialProgress.")
        loadInitialProgress()
    }

    private fun loadInitialProgress() {
        viewModelScope.launch {
            // No need to update isLoading to true here if QuizUiState's default is isLoading = true
            // _uiState.update { it.copy(isLoading = true) }

            val savedAnswers = userProgressRepository.savedAnswersFlow.firstOrNull() ?: emptyMap()
            val rawSavedIndex = userProgressRepository.savedQuestionIndexFlow.firstOrNull()
            val savedIndex = rawSavedIndex ?: 0

            Log.d("QuizViewModel", "loadInitialProgress: SavedIndex = $savedIndex, SavedAnswersSize = ${savedAnswers.size}, TotalQuestions = ${allQuizQuestions.size}")
            Log.d("QuizViewModel", "loadInitialProgress: SavedAnswersSize = ${savedAnswers.size}, TotalQuestions = ${allQuizQuestions.size}")

            if (allQuizQuestions.isEmpty()) {
                Log.d("QuizViewModel", "loadInitialProgress: No questions defined.")
                _uiState.update {
                    it.copy(
                        currentQuestion = null,
                        isQuizFinished = true, 
                        isLoading = false
                    )
                }
                return@launch
            }
            val wasPreviouslyCompleted = savedIndex == allQuizQuestions.size && allQuizQuestions.isNotEmpty()

            Log.d("QuizViewModel", "loadInitialProgress: wasQuizCompleted = $wasPreviouslyCompleted (based on savedIndex: $savedIndex >= allQuizQuestions.size: ${allQuizQuestions.size})")

            if (wasPreviouslyCompleted) {
                Log.d("QuizViewModel", "loadInitialProgress: Quiz was previously completed.")
                _uiState.update {
                    it.copy(
                        answers = savedAnswers, // Keep saved answers for results display
                        isQuizFinished = true,
                        isLoading = false,
                        currentQuestionIndex = savedIndex, // or allQuizQuestions.size
                        currentQuestion = null
                    )
                }
            } else {
                Log.d("QuizViewModel", "loadInitialProgress: Quiz is new, in progress, or being restarted. Current effective index: $savedIndex")
                _uiState.update {
                    it.copy(
                        currentQuestionIndex = savedIndex,
                        currentQuestion = allQuizQuestions.getOrNull(savedIndex),
                        answers = savedAnswers,
                        isQuizFinished = false,
                        isLoading = false
                    )
                }
            }
            Log.d("QuizViewModel", "loadInitialProgress: Final uiState.isQuizFinished = ${_uiState.value.isQuizFinished}, currentQuestionIndex = ${_uiState.value.currentQuestionIndex}")
        }
    }

    fun answerQuestion(questionId: Int, rating: Int) {
        // answrs map is updated
        val updatedAnswers = _uiState.value.answers.toMutableMap().apply { this[questionId] = rating }
        _uiState.update { currentState ->
            currentState.copy(answers = updatedAnswers)
        }
        viewModelScope.launch {
            userProgressRepository.saveAnswers(updatedAnswers)
            Log.d("QuizViewModel", "answerQuestion: Saved answer for QID $questionId. All answers: $updatedAnswers")
        }
    }

    fun nextQuestion(userName:String) {
        val currentIndex = _uiState.value.currentQuestionIndex
        // val currentQuestionId = _uiState.value.currentQuestion?.id

        if (currentIndex < allQuizQuestions.size - 1) {
            val nextIndex = currentIndex + 1
            Log.d("QuizViewModel", "nextQuestion: Moving to next question. Index: $nextIndex")
            _uiState.update { currentState ->
                currentState.copy(
                    currentQuestionIndex = nextIndex,
                    currentQuestion = allQuizQuestions.getOrNull(nextIndex)
                )
            }
            viewModelScope.launch {
                userProgressRepository.saveCurrentQuestionIndex(nextIndex)
                Log.d("QuizViewModel", "nextQuestion: Saved next question index: $nextIndex")
            }
        } else {
            Log.d("QuizViewModel", "nextQuestion: Finishing quiz. Current Index: $currentIndex")
            val finalAnswers = _uiState.value.answers
            val finalTotalScore = finalAnswers.values.sum()

            viewModelScope.launch {
                userProgressRepository.saveAnswers(finalAnswers)
                userProgressRepository.saveCurrentQuestionIndex(allQuizQuestions.size)
                userProgressRepository.addQuizCompletionEntry(userName, finalTotalScore)
                _uiState.update {
                    it.copy(
                        isQuizFinished = true,
                        currentQuestionIndex = allQuizQuestions.size, 
                        currentQuestion = null,
                        isLoading = false
                    )
                }
                Log.d("QuizViewModel", "nextQuestion: Quiz Finished! uiState.isQuizFinished = true. Answers: ${_uiState.value.answers}")
            }
        }
    }

    fun previousQuestion() {
        val currentIndex = _uiState.value.currentQuestionIndex
        if (currentIndex > 0) {
            val prevIndex = currentIndex - 1
            Log.d("QuizViewModel", "previousQuestion: Moving to previous question. Index: $prevIndex")

            _uiState.update {
                it.copy(
                    currentQuestionIndex = prevIndex,
                    currentQuestion = allQuizQuestions.getOrNull(prevIndex)
                )
            }
        }
        else {
            Log.d("QuizViewModel", "previousQuestion: Already at the first question.")
        }
    }

    fun restartQuizForCurrentUser() {
        Log.d("QuizViewModel", "restartQuizForCurrentUser: Restarting quiz.")
        viewModelScope.launch {
            userProgressRepository.clearUserProgress()
            _uiState.value = QuizUiState(
                currentQuestionIndex = 0,
                currentQuestion = allQuizQuestions.getOrNull(0),
                answers = emptyMap(),
                isQuizFinished = false,
                isLoading = false 
            )
            userProgressRepository.saveCurrentQuestionIndex(0)
            Log.d("QuizViewModel", "restartQuizForCurrentUser: Quiz progress cleared and state reset. New index 0 saved.")
        }
    }

    class QuizViewModelFactory(private val repository: UserProgressRepository,
        private val application: Application
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(QuizViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return QuizViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

object NavDestinations {
    const val HOME_ROUTE = "home"
    const val QUIZ_ROUTE_TEMPLATE = "quiz/{userName}"
    fun quizRoute(userName: String) = "quiz/$userName"
    const val QUIZ_RESULTS_ROUTE_TEMPLATE = "quiz_results/{userName}"
    fun quizResultsRoute(userName: String) = "quiz_results/$userName"
    const val COMPLETION_LOG_ROUTE = "completion_log" 
}

@Composable
fun MyApp() {
    val navController = rememberNavController()

    NavHost(navController, startDestination = "home") {
        composable("home") { HomeScreen(navController) }
        composable(
            route = "second/{userName}",
            arguments = listOf(navArgument("userName") { type = NavType.StringType })
        ) { backStackEntry ->
            val userNameArgument = backStackEntry.arguments?.getString("userName")
            SecondScreen(navController, userNameArgument ?: "Guest")
        }

        composable(
            route = "quiz/{userName}",
            arguments = listOf(navArgument("userName") { type = NavType.StringType })
        ) { backStackEntry ->
            val userNameArgument = backStackEntry.arguments?.getString("userName")
            QuestionScreen(
                navController = navController,
                userName = userNameArgument ?: "User"
            )
        }

        composable(
            route = "quiz_results/{userName}",
            arguments = listOf(navArgument("userName") { type = NavType.StringType })
        ) { backStackEntry ->
            val userNameArgument = backStackEntry.arguments?.getString("userName")
            QuizResultsScreen(
                navController = navController,
                userName = userNameArgument ?: "User"
            )
        }

        composable(NavDestinations.COMPLETION_LOG_ROUTE) {
            CompletionLogScreen(navController = navController)
        }
    }
}

@Composable
fun HomeScreen(navController: NavController) {
    var name by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Please enter your name: ")
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = name,
            onValueChange = {name = it},
            label = { Text("Name") },
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { navController.navigate("second/$name") },
            modifier = Modifier.offset(x = 0.dp, y = (320).dp)
        ) {
            Text(
                text = "continue",
                fontSize = 24.sp
            )
        }
    }
}

@Composable
fun SecondScreen(navController: NavController, name: String) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val userProgressRepository = remember { UserProgressRepository(application) }
    val coroutineScope = rememberCoroutineScope()

    BackHandler {
        navController.popBackStack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 32.dp),
        ) {
            Text(
                text = "Welcome, $name! :)",
                fontSize = 24.sp,
            )

            Text(
                text = "This is a quiz that would assist in the measurement of PTSD (Post Traumatic Stress Disorder) of yourself!",
                modifier = Modifier.padding(25.dp),
                fontSize = 24.sp,
            )

            Spacer(modifier = Modifier.height(20.dp))

            Image (
                painter = painterResource(id = R.drawable.thumbs_up),
                contentDescription = "Thumbs up",
                modifier = Modifier
                    .size(200.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
        }


        Text(
            text = "There will be 20 total questions, and your day-to day progress will be saved. \n0 = Strongly Disagree\n4 = Strongly Agree",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 200.dp), 
            fontSize = 24.sp,
        )

        Button(
            onClick = {
                Log.d("SecondScreen", "Start Quiz button clicked for $name. Clearing progress for new session.")
                coroutineScope.launch {
                    userProgressRepository.clearUserProgress()
                    userProgressRepository.saveCurrentQuestionIndex(0)
                    Log.d("SecondScreen", "Progress cleared. Navigating to quiz screen for $name.")
                    navController.navigate("quiz/$name")
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-100).dp)
        ) {
            Text("Start Quiz / Continue to Next Step")
        }

        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-40).dp)
        ) {
            Text("Back to Home")
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionScreen(navController: NavController, userName: String) {
    val application = LocalContext.current.applicationContext as Application
    val userProgressRepository = remember { UserProgressRepository(application) }
    val quizViewModel: QuizViewModel = viewModel(
        key = "quiz_vm_$userName", 
        factory = QuizViewModel.QuizViewModelFactory(userProgressRepository, application)
    )
    val uiState by quizViewModel.uiState.collectAsState()
    val currentQuestion = uiState.currentQuestion
    var selectedRating by remember(currentQuestion?.id, uiState.answers) {
        mutableStateOf(currentQuestion?.let { uiState.answers[it.id] })
    }
    val context = LocalContext.current

    Log.d("QuestionScreen", "Composing. isLoading: ${uiState.isLoading}, isQuizFinished: ${uiState.isQuizFinished}, CurrentQIndex: ${uiState.currentQuestionIndex}, CurrentQ: ${currentQuestion?.id}")


    LaunchedEffect(uiState.isQuizFinished) {
        if (uiState.isQuizFinished) {
            if (navController.currentDestination?.route?.startsWith("quiz/") == true) {
                Log.d("QuestionScreen", "LaunchedEffect: Quiz is finished, navigating to results for $userName.")
                Toast.makeText(context, "Quiz Finished! Navigating to results.", Toast.LENGTH_LONG).show()
                navController.navigate("quiz_results/$userName") {
                    popUpTo("quiz/$userName") { inclusive = true }
                    launchSingleTop = true 
                }
            }
        }
    }

    if (uiState.isLoading) {
        Log.d("QuestionScreen", "Displaying: Loading quiz progress...")
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
            Text("Loading quiz progress...")
        }
        return
    }

    if (uiState.isQuizFinished && !uiState.isLoading) {
        Log.d("QuestionScreen", "Displaying: Quiz Finished! Preparing results...")
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Quiz Finished! Preparing results...")
            CircularProgressIndicator()
        }
        return 
    }


    BackHandler {
        Log.d("QuestionScreen", "Back pressed. Current index: ${uiState.currentQuestionIndex}")
        if (uiState.currentQuestionIndex > 0) {
            quizViewModel.previousQuestion()
        } else {
            navController.popBackStack(route = "second/$userName", inclusive = false)
        }
    }

    if (currentQuestion == null && !uiState.isLoading && !uiState.isQuizFinished) {
        Log.d("QuestionScreen", "Displaying: CurrentQuestion is null, but not loading and not finished. This might be an issue.")
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Error: Could not load question.")
        }
        return
    }
    if (currentQuestion == null) {

        Log.d("QuestionScreen", "Displaying: currentQuestion is null unexpectedly. Loading fallback.")
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading question (fallback)...")
            CircularProgressIndicator()
        }
        return
    }


    Log.d("QuestionScreen", "Displaying: Question UI for QID ${currentQuestion.id}")
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Question ${uiState.currentQuestionIndex + 1} of ${allQuizQuestions.size}") }
            )
        }
    ) { paddingValues ->
        Column (
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text (
                text = "Survey for $userName",
                fontSize = 20.sp,
                style = MaterialTheme.typography.headlineSmall
            )

            Card (modifier = Modifier.fillMaxWidth()) {
                Column (modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = currentQuestion.questionText,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        (0..4).forEach { ratingValue ->
                            Button(
                                onClick = { selectedRating = ratingValue },
                                colors = ButtonDefaults.buttonColors (
                                    containerColor = if (selectedRating == ratingValue) Color(0xFFD00580) else Color(0xFFB0BEC5)
                                )
                            ) {
                                Text(text = "$ratingValue")
                            }
                        }
                    }
                    if (selectedRating != null) {
                        Text(
                            "Your rating: $selectedRating",
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (uiState.currentQuestionIndex > 0) Arrangement.SpaceBetween else Arrangement.End
            ) {
                if (uiState.currentQuestionIndex > 0) {
                    Button(onClick = {
                        Log.d("QuestionScreen", "Previous button clicked.")
                        quizViewModel.previousQuestion()
                    }) {
                        Text("Previous")
                    }
                }

                Button (
                    onClick = {
                        if(selectedRating != null) {
                            Log.d("QuestionScreen", "Next/Finish button clicked. QID: ${currentQuestion.id}, Rating: $selectedRating")
//                            val currentAnswers = quizViewModel.uiState.value.answers
//                            val scoreForThisCompletion = currentAnswers.values.sum()

                            quizViewModel.answerQuestion(currentQuestion.id, selectedRating!!)
                            quizViewModel.nextQuestion(userName)
                        }
                        else {
                            Toast.makeText(context, "Please select a rating", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = selectedRating != null
                ) {
                    Text(
                        if (uiState.currentQuestionIndex < allQuizQuestions.size - 1) {
                            "Next"
                        }
                        else {
                            "Finish"
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizResultsScreen(navController: NavController, userName: String) {
    val application = LocalContext.current.applicationContext as Application
    val userProgressRepository = remember { UserProgressRepository(application) }

    val coroutineScope = rememberCoroutineScope()
    val savedAnswerMap by userProgressRepository.savedAnswersFlow.collectAsState(initial = null)

    var totalScore by remember { mutableStateOf(0) }
    var isLoadingScore by remember { mutableStateOf(true) }

    LaunchedEffect(savedAnswerMap) {
        savedAnswerMap?.let { answers ->
            totalScore = answers.values.sum()
            isLoadingScore = false
        }
    }

    val completionCount by userProgressRepository.QuizCompletionCountFlow.collectAsState(initial = 0)
    val YoutubeVideoURL = listOf (
        "https://www.youtube.com/watch?v=dH128i8vFnk",
        "https://www.youtube.com/watch?v=I_fEBSqGmyA",
        "https://www.youtube.com/watch?v=jgMH89btVQA",
        "https://www.youtube.com/watch?v=TCybFc3sHlc",
        "https://www.youtube.com/watch?v=aQoxYF6C-eM",
        "https://www.youtube.com/watch?v=3AAH7vINhmQ",
        "https://youtu.be/Ljss_Ut5pxY?si=L3eXzEM-qni_9wKn",
        "https://youtu.be/q1YVvndNyqM?si=QNid2lBb6tyoWoyG",
        "https://www.youtube.com/watch?v=Dd1hnPMjNYk",
        "https://youtu.be/eiB-ibIwJN4?si=TNHhm0pHkOF4sKSk",
    )

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Quiz Results for $userName") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Congratulations, $userName!",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
                text = "You have completed the quiz!",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Score Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Your Current Score:", style = MaterialTheme.typography.titleMedium)
                    if (isLoadingScore) {
                        CircularProgressIndicator(modifier = Modifier.padding(top = 8.dp))
                    } else {
                        Text(
                            text = "$totalScore",
                            style = MaterialTheme.typography.displayMedium,
                            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                        )
                    }
                }
            }

            // Completions Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Total Quiz Completions:", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "$completionCount",
                        style = MaterialTheme.typography.displayMedium,
                        modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            val localContext = LocalContext.current
            var randomVideoId by remember { mutableStateOf<String?>(null) }

            // Effect to select a random video when the screen is first composed or relevant data is ready
            LaunchedEffect(Unit) { // Or key on isLoadingScore == false if you want to wait for score
                if (YoutubeVideoURL.isNotEmpty()) {
                    randomVideoId = YoutubeVideoURL.random()
                }
            }

            randomVideoId?.let { videoId ->
                Text(
                    text = "Here's a video that will be helpful for you:",
                    style = MaterialTheme.typography.titleMedium,
                )
                YouTubeVideoItem(videoId = videoId, context = localContext)
                Button(onClick = {
                    val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("$videoId"))
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("$videoId"))
                    try {
                        localContext.startActivity(appIntent)
                    } catch (ex: Exception) { // If YouTube app is not installed
                        localContext.startActivity(webIntent)
                    }
                }) {
                    Text("Watch Video")
                }

                Spacer(modifier = Modifier.height(30.dp))

                Text(
                    text = "Create some of your own beats!",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(18.dp))
                Button(onClick = {
                    val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://suno.com/create?wid=default"))
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://suno.com/create?wid=default"))
                    try {
                        localContext.startActivity(appIntent)
                    } catch (ex: Exception) { // If YouTube app is not installed
                        localContext.startActivity(webIntent)
                    }
                }) {
                    Text("Suno Music Creator")
                }
            }

            Spacer(modifier = Modifier.height(180.dp))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Button(onClick = { navController.navigate(NavDestinations.COMPLETION_LOG_ROUTE) },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(y = 20.dp)
        ) {
            Text("View Log")
        }

        Button(
            onClick = {
                navController.popBackStack("home", inclusive = false)
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-15).dp, x = -(110).dp)
        ) {
            Text("Back to Home")
        }

        Button(onClick = {
            coroutineScope.launch {
                userProgressRepository.clearUserProgress()
                userProgressRepository.saveCurrentQuestionIndex(0)
                navController.navigate("quiz/$userName") {
                    popUpTo("quiz_results/$userName") { inclusive = true }
                    launchSingleTop = true
                }
            }
        },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-15).dp, x = (120).dp)
        ) {
            Text("Retake Quiz")
        }

    }
}

@Composable
fun YouTubeVideoItem(videoId: String, context: Context) {
    val thumbnailUrl = "https://img.youtube.com/vi/$videoId/0.jpg" // Standard quality thumbnail

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                // Action to open video (can also be handled by a separate button)
                val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("$videoId"))
                val webIntent =
                    Intent(Intent.ACTION_VIEW, Uri.parse("$videoId"))
                try {
                    context.startActivity(appIntent)
                } catch (ex: Exception) { // If YouTube app is not installed
                    context.startActivity(webIntent)
                }
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
    }
}

fun getPromptFromScore(score: Int): String {
    return when (score) {
        in 0..20 -> "acoustic, calm, uplifting, nature-inspired"
        in 21..30 -> "soft piano, warm strings, hopeful"
        in 31..45 -> "gentle ambient, atmospheric pads, peaceful tones"
        in 46..60 -> "soothing instrumental, emotionally warm, deep reverb"
        in 61..80 -> "grounding ambient, soft rain, low drones, no rhythm"
        else -> "ambient calm music"
    }
}

//private var globalMediaPlayer: MediaPlayer? = null
//
//fun playAudioFromUrl(url: String, context: Context) {
//    globalMediaPlayer?.apply {
//        stop()
//        reset()
//        release()
//    }
//    globalMediaPlayer = MediaPlayer().apply {
//        setAudioAttributes(
//            AudioAttributes.Builder()
//                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
//                .setUsage(AudioAttributes.USAGE_MEDIA)
//                .build()
//        )
//        try {
//            setDataSource(url)
//            setOnPreparedListener { it.start() }
//            prepareAsync()
//        } catch (e: Exception) {
//            Log.e("MediaPlayer", "Error: ${e.message}")
//            release()
//            globalMediaPlayer = null
//        }
//    }
//}
