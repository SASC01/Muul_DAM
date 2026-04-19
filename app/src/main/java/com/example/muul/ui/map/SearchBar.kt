package com.example.muul.ui.map

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MuulSearchBar(viewModel: MapViewModel) {
    val query by viewModel.searchQuery.collectAsState()

    OutlinedTextField(
        value = query,
        onValueChange = { viewModel.setSearchQuery(it) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        placeholder = { Text("Buscar lugares...") },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = "Buscar")
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Limpiar")
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(24.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        )
    )
}