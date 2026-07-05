package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.model.EnvVar

@Composable
fun EnvVarEditor(
    envVars: List<EnvVar>,
    onAdd: () -> Unit,
    onUpdate: (Int, String, String) -> Unit,
    onRemove: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        envVars.forEachIndexed { index, envVar ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = envVar.key,
                    onValueChange = { onUpdate(index, it, envVar.value) },
                    placeholder = { Text("KEY", style = MaterialTheme.typography.bodySmall) },
                    modifier = Modifier.weight(0.4f),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = envVar.value,
                    onValueChange = { onUpdate(index, envVar.key, it) },
                    placeholder = { Text("VALUE", style = MaterialTheme.typography.bodySmall) },
                    modifier = Modifier.weight(0.5f),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = { onRemove(index) }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remove",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        TextButton(
            onClick = onAdd,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Add Variable")
        }
    }
}
