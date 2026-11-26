package com.npcg.practica1.ui.screens.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.npcg.practica1.domain.model.ChatRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelliChatScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WelliChatViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Conversar con WelliAI") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::startNewConversation, enabled = !state.sending) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Iniciar nueva conversación")
                    }
                }
            )
        },
        bottomBar = {
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.draft,
                        onValueChange = viewModel::onDraftChange,
                        modifier = Modifier.weight(1f),
                        label = { Text("Escribe tu pregunta") },
                        maxLines = 4,
                        enabled = !state.sending,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { viewModel.send() })
                    )
                    IconButton(onClick = viewModel::send, enabled = !state.sending && state.draft.isNotBlank()) {
                        if (state.sending) CircularProgressIndicator()
                        else Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar mensaje", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    ) { padding ->
        if (state.messages.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(28.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("¡Hola! Soy WelliAI.", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                Text("Pregúntame sobre recetas, sustituciones, técnicas o conservación de alimentos.", modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp),
                reverseLayout = true,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (state.sending) {
                    item(key = "typing") {
                        Text("WelliAI está pensando…", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(8.dp))
                    }
                }
                items(state.messages.asReversed(), key = { it.id }) { message ->
                    val user = message.role == ChatRole.USER
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (user) Arrangement.End else Arrangement.Start
                    ) {
                        Text(
                            text = message.text,
                            modifier = Modifier
                                .fillMaxWidth(0.86f)
                                .background(
                                    if (user) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(18.dp)
                                )
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            color = if (user) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
