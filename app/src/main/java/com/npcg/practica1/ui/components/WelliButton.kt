package com.npcg.practica1.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun WelliButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isSecondary: Boolean = false
) {
    if (isSecondary) {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = enabled,
            shape = MaterialTheme.shapes.medium
        ) {
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    } else {
        Button(
            onClick = onClick,
            modifier = modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = enabled,
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    }
}
