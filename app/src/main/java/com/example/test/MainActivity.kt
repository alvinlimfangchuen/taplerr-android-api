package com.example.test

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.test.ui.theme.TestTheme
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

data class ApiResponse(
    val status: String = "",
    val total_users: Int = 0
)

interface ApiService {
    @GET("totalUser")
    suspend fun getTotalUsers(): ApiResponse
}

class MainViewModel : ViewModel() {
    private val api = Retrofit.Builder()
        .baseUrl("https://staging.taplerr.com/api/")  // Fixed: Correct base URL
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiService::class.java)

    private val _totalUsers = mutableStateOf<Int?>(null)
    val totalUsers: State<Int?> = _totalUsers

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    init {
        fetchTotalUsers() // Added: Fetch data when ViewModel is created
    }

    fun fetchTotalUsers() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                Log.d("API", "Making API call...")
                val response = api.getTotalUsers()
                Log.d("API", "Response received: ${response.total_users}")
                _totalUsers.value = response.total_users
            } catch (e: Exception) {
                Log.e("API", "Error: ${e.message}", e)
                _error.value = e.message ?: "Unknown error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TestTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Scaffold { innerPadding ->
                        MainScreen(modifier = Modifier.padding(innerPadding))
                    }
                }
            }
        }
    }
}

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    val viewModel: MainViewModel = viewModel()
    val totalUsers by viewModel.totalUsers
    val isLoading by viewModel.isLoading
    val error by viewModel.error

    LaunchedEffect(Unit) {
        viewModel.fetchTotalUsers()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator()
                Text(
                    text = "Loading...",
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            error != null -> {
                Text(
                    text = "Error: $error",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
                Button(
                    onClick = { viewModel.fetchTotalUsers() },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Retry")
                }
            }
            totalUsers != null -> {
                Text(
                    text = "Total Users: $totalUsers",
                    style = MaterialTheme.typography.headlineMedium
                )
                Button(
                    onClick = { viewModel.fetchTotalUsers() },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text("Refresh")
                }
            }
            else -> {
                CircularProgressIndicator()
            }
        }
    }
}