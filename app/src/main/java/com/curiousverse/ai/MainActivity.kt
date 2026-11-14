package com.curiousverse.ai

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private val Ink = Color(0xFF090D1B)
private val Panel = Color(0xFF141B2E)
private val Violet = Color(0xFF8B5CF6)
private val Cyan = Color(0xFF22D3EE)
private val Muted = Color(0xFF9AA7C2)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CuriousVerseApp() }
    }
}

@Composable
fun CuriousVerseApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("curiousverse", Context.MODE_PRIVATE) }
    var apiUrl by remember { mutableStateOf(prefs.getString("api_url", "") ?: "") }
    var selectedTab by remember { mutableIntStateOf(0) }
    var topic by remember { mutableStateOf("") }
    var script by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Your next big idea starts here.") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Violet,
            secondary = Cyan,
            background = Ink,
            surface = Panel,
            onBackground = Color.White,
            onSurface = Color.White
        )
    ) {
        Scaffold(
            containerColor = Ink,
            bottomBar = {
                NavigationBar(containerColor = Color(0xFF101629), contentColor = Color.White) {
                    NavigationBarItem(
                        selected = selectedTab == 0, onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Home, null) }, label = { Text("Studio") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1, onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.VideoLibrary, null) }, label = { Text("Projects") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2, onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.Movie, null) }, label = { Text("Tools") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3, onClick = { selectedTab = 3 },
                        icon = { Icon(Icons.Default.Settings, null) }, label = { Text("Settings") }
                    )
                }
            }
        ) { padding ->
            when (selectedTab) {
                0 -> StudioScreen(
                    modifier = Modifier.padding(padding),
                    topic = topic, onTopicChange = { topic = it },
                    script = script, status = status, busy = busy,
                    onGenerate = {
                        if (topic.trim().length < 3) {
                            status = "Enter a topic with at least 3 characters."
                        } else if (apiUrl.isBlank()) {
                            status = "Add your backend URL in Settings first."
                            selectedTab = 3
                        } else {
                            scope.launch {
                                busy = true
                                script = ""
                                status = "Connecting to CuriousVerse AI…"
                                try {
                                    val result = withContext(Dispatchers.IO) { requestScript(apiUrl, topic.trim()) }
                                    script = result
                                    status = "Your script is ready. Review it before production."
                                } catch (e: Exception) {
                                    status = e.message ?: "Something went wrong. Check your server and try again."
                                } finally {
                                    busy = false
                                }
                            }
                        }
                    }
                )
                1 -> PlaceholderScreen(Modifier.padding(padding), "Your projects", "Your generated scripts and video projects will appear here.")
                2 -> ToolsScreen(Modifier.padding(padding))
                else -> SettingsScreen(
                    Modifier.padding(padding), apiUrl,
                    onSave = {
                        apiUrl = it.trim().trimEnd('/')
                        prefs.edit().putString("api_url", apiUrl).apply()
                    }
                )
            }
        }
    }
}

@Composable
private fun StudioScreen(
    modifier: Modifier,
    topic: String,
    onTopicChange: (String) -> Unit,
    script: String,
    status: String,
    busy: Boolean,
    onGenerate: () -> Unit
) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(46.dp).background(Brush.linearGradient(listOf(Violet, Cyan)), RoundedCornerShape(15.dp)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.AutoAwesome, null, tint = Color.White) }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("CURIOUSVERSE", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text("AI CREATOR STUDIO", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.weight(1f))
            Surface(color = Color(0xFF183B3B), shape = RoundedCornerShape(50)) {
                Text("BETA", color = Cyan, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        Box(
            Modifier.fillMaxWidth().background(
                Brush.linearGradient(listOf(Color(0xFF35246C), Color(0xFF152C4A), Color(0xFF122238))),
                RoundedCornerShape(26.dp)
            ).padding(22.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("CREATE WITHOUT LIMITS", color = Color(0xFFC4B5FD), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp)
                Text("Turn curiosity into\\ncontent that clicks.", color = Color.White, fontSize = 29.sp, lineHeight = 34.sp, fontWeight = FontWeight.ExtraBold)
                Text("Your AI-powered YouTube production workspace.", color = Color(0xFFD5DDF0), fontSize = 14.sp)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard("01", "IDEA", Modifier.weight(1f))
            MetricCard("02", "SCRIPT", Modifier.weight(1f))
            MetricCard("03", "VIDEO", Modifier.weight(1f))
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Start a new creation", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("What should your next video explore?", color = Muted, fontSize = 13.sp)
            OutlinedTextField(
                value = topic, onValueChange = onTopicChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. Secrets hidden beneath the ocean…", color = Muted) },
                minLines = 3, shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Violet, unfocusedBorderColor = Color(0xFF34415C),
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                    cursorColor = Cyan
                )
            )
            Button(
                onClick = onGenerate, enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Violet)
            ) {
                if (busy) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                else Icon(Icons.Default.AutoAwesome, null)
                Spacer(Modifier.width(9.dp))
                Text(if (busy) "CREATING YOUR SCRIPT…" else "GENERATE WITH GEMINI", fontWeight = FontWeight.Bold)
            }
            Text(status, color = if (status.contains("ready", true)) Cyan else Muted, fontSize = 12.sp)
        }

        if (script.isNotBlank()) {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("YOUR GENERATED SCRIPT", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                    Text(script, color = Color(0xFFE8ECF7), fontSize = 14.sp, lineHeight = 22.sp)
                }
            }
        }
        Text("Built for creators • Powered by Gemini", color = Color(0xFF65718B), fontSize = 11.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
private fun MetricCard(number: String, label: String, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text(number, color = Cyan, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
            Text(label, color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
    }
}

@Composable
private fun PlaceholderScreen(modifier: Modifier, title: String, description: String) {
    Column(modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.VideoLibrary, null, tint = Violet, modifier = Modifier.size(52.dp))
        Spacer(Modifier.height(16.dp))
        Text(title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(description, color = Muted, fontSize = 14.sp)
        Spacer(Modifier.height(12.dp))
        Text("PROJECT LIBRARY • COMING NEXT", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ToolsScreen(modifier: Modifier) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Creator tools", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        Text("Your production pipeline, in one workspace.", color = Muted)
        listOf(
            "AI script writer" to "Available now • Gemini",
            "Voiceover studio" to "Planned • narration and voice selection",
            "Video renderer" to "Planned • scenes, captions and music",
            "Thumbnail designer" to "Planned • branded cover art",
            "YouTube publishing" to "Planned • secure Google authorization"
        ).forEach { (name, detail) ->
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(17.dp)) {
                Column(Modifier.fillMaxWidth().padding(17.dp)) {
                    Text(name, color = Color.White, fontWeight = FontWeight.Bold)
                    Text(detail, color = Muted, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(modifier: Modifier, apiUrl: String, onSave: (String) -> Unit) {
    var draft by remember(apiUrl) { mutableStateOf(apiUrl) }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Settings", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
        Text("Connect your private CuriousVerse backend.", color = Muted)
        Text("BACKEND URL", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
        OutlinedTextField(
            value = draft, onValueChange = { draft = it },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            placeholder = { Text("https://your-backend.example.com") },
            shape = RoundedCornerShape(15.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Violet, unfocusedBorderColor = Color(0xFF34415C),
                focusedTextColor = Color.White, unfocusedTextColor = Color.White
            )
        )
        Button(onClick = { onSave(draft) }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(15.dp)) {
            Text("SAVE CONNECTION")
        }
        Text("Keep your Gemini API key on the backend only. Never paste it into this app or share it in chat.", color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
    }
}

private fun requestScript(baseUrl: String, topic: String): String {
    val endpoint = baseUrl.trim().trimEnd('/') + "/script"
    val connection = (URL(endpoint).openConnection() as HttpURLConnection)
    try {
        connection.requestMethod = "POST"
        connection.connectTimeout = 15000
        connection.readTimeout = 120000
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Accept", "application/json")
        val body = JSONObject().put("topic", topic).toString()
        connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        val json = runCatching { JSONObject(text) }.getOrNull()
        if (code !in 200..299) {
            val detail = json?.optString("detail").orEmpty()
            throw IllegalStateException(if (detail.isNotBlank()) detail else "Server returned HTTP $code.")
        }
        return json?.optString("script")?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("The server returned an empty script.")
    } finally {
        connection.disconnect()
    }
}
