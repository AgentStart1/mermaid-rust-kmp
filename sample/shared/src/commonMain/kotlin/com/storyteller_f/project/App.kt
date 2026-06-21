package com.storyteller_f.project

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

expect fun renderMermaidDiagram(input: String): String

@Composable
fun App() {
    MaterialTheme {
        var mermaidInput by remember {
            mutableStateOf(
                """flowchart LR
    A[Start] --> B{Decision}
    B -->|Yes| C[OK]
    B -->|No| D[Cancel]"""
            )
        }
        var svgOutput by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()

        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .safeContentPadding()
                .fillMaxSize()
                .padding(16.dp),
        ) {
            Text("Mermaid Diagram Renderer", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = mermaidInput,
                onValueChange = { mermaidInput = it },
                modifier = Modifier.fillMaxWidth().height(160.dp),
                label = { Text("Mermaid syntax") },
            )
            Spacer(Modifier.height(8.dp))

            Row {
                Button(onClick = {
                    val input = mermaidInput
                    scope.launch {
                        try {
                            val result = withContext(Dispatchers.Default) {
                                renderMermaidDiagram(input)
                            }
                            svgOutput = result
                            errorMessage = null
                        } catch (e: Exception) {
                            errorMessage = e.message ?: "Unknown error"
                            svgOutput = ""
                        }
                    }
                }) {
                    Text("Render")
                }
                PRESETS.forEach { (label, code) ->
                    Button(
                        onClick = {
                            mermaidInput = code
                            scope.launch {
                                try {
                                    val result = withContext(Dispatchers.Default) {
                                        renderMermaidDiagram(code)
                                    }
                                    svgOutput = result
                                    errorMessage = null
                                } catch (e: Exception) {
                                    errorMessage = e.message ?: "Unknown error"
                                    svgOutput = ""
                                }
                            }
                        },
                        modifier = Modifier.padding(start = 8.dp),
                    ) {
                        Text(label)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            if (errorMessage != null) {
                Text(
                    text = "Error: $errorMessage",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            if (svgOutput.isNotEmpty()) {
                Text("SVG output:", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = svgOutput,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(8.dp)
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState()),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

private val PRESETS = listOf(
    "Sequence" to """sequenceDiagram
    Alice->>Bob: Hello Bob
    Bob-->>Alice: Hi Alice""",
    "Class" to """classDiagram
    Animal <|-- Duck
    Animal : +int age
    Duck : +swim()""",
    "Pie" to """pie title Pets
    "Dogs" : 386
    "Cats" : 85
    "Rats" : 15""",
)
