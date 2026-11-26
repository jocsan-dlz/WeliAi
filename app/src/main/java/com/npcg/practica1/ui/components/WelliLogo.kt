package com.npcg.practica1.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.npcg.practica1.R

@Composable
fun WelliLogo(
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    showText: Boolean = true
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_welli_logo),
            contentDescription = "Logotipo Oficial WelliAI",
            modifier = Modifier.size(size)
        )

        if (showText) {
            Spacer(modifier = Modifier.width(14.dp))
            Row {
                Text(
                    text = "Welli",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "AI",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}
