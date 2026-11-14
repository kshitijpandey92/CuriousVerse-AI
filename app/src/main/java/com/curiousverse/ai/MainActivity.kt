package com.curiousverse.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { App() }
    }
}

@Composable
fun App() {
    var topic by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("Ready to create!") }
    MaterialTheme(colorScheme = darkColorScheme()) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text("CuriousVerse AI", style = MaterialTheme.typography.headlineLarge)
            Text("Create amazing YouTube videos with AI")
            OutlinedTextField(
                value = topic,
                onValueChange = { topic = it },
                label = { Text("Enter your video topic") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    message = if (topic.isBlank()) "Enter a topic first"
                    else "Your topic is ready: $topic"
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Generate Video Script") }
            Text(message)
            Text("AI scripts • Voiceovers • Video rendering • Thumbnails • YouTube")
        }
    }
}
