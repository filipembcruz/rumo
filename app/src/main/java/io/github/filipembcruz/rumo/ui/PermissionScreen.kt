package io.github.filipembcruz.rumo.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PermissionScreen() {
    val context = LocalContext.current
    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Text("Bem-vindo ao Rumo", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "Para mostrar quanto tempo você passa em cada app, o Rumo precisa do " +
                "\"Acesso ao uso\" do Android.",
            style = MaterialTheme.typography.bodyLarge,
        )
        SectionCard(title = "O que isso permite") {
            Text(
                "• Ler o tempo de uso e as aberturas de cada app neste celular.\n" +
                    "• Contar quantas vezes você desbloqueou o aparelho.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        SectionCard(title = "Seus dados") {
            Text(
                "Tudo fica guardado só neste celular. O Rumo não tem servidor e não envia " +
                    "nada para a internet. Você pode revogar o acesso a qualquer momento nas " +
                    "configurações do Android.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Text(
            "Na próxima tela, toque em Rumo e ative \"Permitir acesso ao uso\". Depois volte para cá.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = {
                context.startActivity(
                    Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text("Abrir configurações") }
    }
}
