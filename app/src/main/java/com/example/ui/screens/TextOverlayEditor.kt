package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.TextOverlay
import com.example.ui.theme.BorderDark
import com.example.ui.theme.OmkarCutRed
import com.example.ui.theme.OmkarCyan
import com.example.ui.theme.OmkarGold
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceVariantDark
import java.util.UUID

@Composable
fun TextOverlayDialog(
    initialOverlay: TextOverlay?,
    defaultStartMs: Long,
    defaultEndMs: Long,
    onDismiss: () -> Unit,
    onSave: (TextOverlay) -> Unit,
    onDelete: ((String) -> Unit)? = null
) {
    var text by remember { mutableStateOf(initialOverlay?.text ?: "") }
    var posY by remember { mutableFloatStateOf(initialOverlay?.positionY ?: 85f) }
    var posX by remember { mutableFloatStateOf(initialOverlay?.positionX ?: 50f) }
    var fontSize by remember { mutableFloatStateOf(initialOverlay?.fontSizeSp ?: 22f) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, BorderDark, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.TextFields,
                            contentDescription = null,
                            tint = OmkarGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (initialOverlay == null) "Add Text Overlay" else "Edit Text Overlay",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Text content") },
                    placeholder = { Text("e.g. Subscribe! / Key Highlight") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("text_overlay_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OmkarGold,
                        unfocusedBorderColor = BorderDark,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Vertical Position Slider
                Text(
                    text = "Vertical Position: ${posY.toInt()}%",
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = posY,
                    onValueChange = { posY = it },
                    valueRange = 10f..95f,
                    colors = SliderDefaults.colors(
                        thumbColor = OmkarGold,
                        activeTrackColor = OmkarGold,
                        inactiveTrackColor = SurfaceVariantDark
                    )
                )

                // Font Size Slider
                Text(
                    text = "Font Size: ${fontSize.toInt()}sp",
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = fontSize,
                    onValueChange = { fontSize = it },
                    valueRange = 14f..42f,
                    colors = SliderDefaults.colors(
                        thumbColor = OmkarCyan,
                        activeTrackColor = OmkarCyan,
                        inactiveTrackColor = SurfaceVariantDark
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (initialOverlay != null && onDelete != null) {
                        OutlinedButton(
                            onClick = {
                                onDelete(initialOverlay.id)
                                onDismiss()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = OmkarCutRed),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete")
                        }
                    }

                    Button(
                        onClick = {
                            if (text.isNotBlank()) {
                                val overlay = (initialOverlay ?: TextOverlay(
                                    id = UUID.randomUUID().toString(),
                                    text = text,
                                    startMs = defaultStartMs,
                                    endMs = defaultEndMs
                                )).copy(
                                    text = text,
                                    positionY = posY,
                                    positionX = posX,
                                    fontSizeSp = fontSize
                                )
                                onSave(overlay)
                                onDismiss()
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("save_text_overlay_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OmkarGold,
                            contentColor = Color.Black
                        )
                    ) {
                        Text("Save Text", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
