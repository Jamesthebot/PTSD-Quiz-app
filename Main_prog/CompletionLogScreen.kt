// CompletionLogScreen.kt
package com.example.eeeeeee // Your package

import UserProgressRepository // Your import
import QuizCompletionEntry // Your import
import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ViewModel for CompletionLogScreen
class CompletionLogViewModel(private val userProgressRepository: UserProgressRepository) : ViewModel() {
    val completionHistory: StateFlow<List<QuizCompletionEntry>> =
        userProgressRepository.quizCompletionHistoryFlow
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val totalCompletions: StateFlow<Int> = completionHistory.map { it.size }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

//    fun clearHistory() {
//        viewModelScope.launch {
//            userProgressRepository.clearQuizCompletionHistory()
//        }
//    }
}

class CompletionLogViewModelFactory(private val repository: UserProgressRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CompletionLogViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CompletionLogViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompletionLogScreen(
    navController: NavController,
    logViewModel: CompletionLogViewModel = viewModel(
        factory = CompletionLogViewModelFactory(
            UserProgressRepository(LocalContext.current.applicationContext as Application)
        )
    )
) {
    val completionHistory by logViewModel.completionHistory.collectAsState()
    val totalCompletions by logViewModel.totalCompletions.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quiz Completion Log") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Total Completions: $totalCompletions",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (completionHistory.isEmpty()) {
                Text("No completions logged yet.")
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(completionHistory.sortedByDescending { it.timestamp }) { entry -> // Show newest first
                        CompletionLogItem(entry)
                        Divider()
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

//            Button(
//                onClick = {
//                    // Add a confirmation dialog before clearing
//                    coroutineScope.launch {
//                        logViewModel.clearHistory()
//                    }
//                },
//                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
//            ) {
//                Text("Clear History")
//            }
        }
    }
}

@Composable
fun CompletionLogItem(entry: QuizCompletionEntry) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy - HH:mm:ss", Locale.getDefault()) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "User: ${entry.userName}",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                "Completed: ${dateFormat.format(Date(entry.timestamp))}",
                fontSize = 14.sp,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "Score: ${entry.score}",
                fontSize = 14.sp
            )
        }
    }
}
