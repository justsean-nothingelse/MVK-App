package com.seanpereyra.mvk.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.seanpereyra.mvk.data.Voucher
import com.seanpereyra.mvk.ui.theme.Mvk
import com.seanpereyra.mvk.ui.theme.MvkType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoucherFormSheet(
    title: String,
    initial: Voucher,
    confirmLabel: String = "Save to library",
    hint: String? = null,
    onSave: (Voucher) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = Mvk.colors
    var code by remember { mutableStateOf(initial.code) }
    var network by remember { mutableStateOf(initial.network) }
    var slot by remember { mutableStateOf(initial.slot) }
    var duration by remember { mutableStateOf(initial.duration) }
    val valid = code.trim().length >= 4

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.surface,
    ) {
        Column(
            Modifier
                .padding(horizontal = 22.dp)
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = 18.dp),
        ) {
            Text(title, style = MvkType.title.copy(fontSize = 30.sp), color = c.text)
            if (hint != null) {
                Spacer(Modifier.height(4.dp))
                Text(hint, style = MvkType.body.copy(fontSize = 13.sp), color = c.textDim)
            }
            Spacer(Modifier.height(18.dp))

            val fieldColors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = c.text, unfocusedBorderColor = c.line,
                focusedLabelColor = c.text, unfocusedLabelColor = c.textDim,
                focusedTextColor = c.text, unfocusedTextColor = c.text, cursorColor = c.text,
            )

            OutlinedTextField(
                value = code,
                onValueChange = { v -> code = v.filter { it.isLetterOrDigit() }.take(12) },
                label = { Text("Voucher code") },
                singleLine = true,
                textStyle = MvkType.code.copy(fontSize = 24.sp, letterSpacing = 0.12.em),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
                colors = fieldColors,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
                supportingText = { Text("Case-sensitive. Double-check 0/O and l/I.") },
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = network, onValueChange = { network = it.take(16) }, label = { Text("Network") },
                    singleLine = true, colors = fieldColors, shape = RoundedCornerShape(14.dp),
                    textStyle = TextStyle(fontSize = 15.sp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier.weight(1.4f),
                )
                OutlinedTextField(
                    value = slot, onValueChange = { v -> slot = v.filter(Char::isDigit).take(5) }, label = { Text("No.") },
                    singleLine = true, colors = fieldColors, shape = RoundedCornerShape(14.dp),
                    textStyle = TextStyle(fontSize = 15.sp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(0.8f),
                )
                OutlinedTextField(
                    value = duration, onValueChange = { duration = it.take(10) }, label = { Text("Time") },
                    singleLine = true, colors = fieldColors, shape = RoundedCornerShape(14.dp),
                    textStyle = TextStyle(fontSize = 15.sp), placeholder = { Text("6h") },
                    modifier = Modifier.weight(0.9f),
                )
            }
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp),
                ) { Text("Cancel", style = MvkType.bodyMedium, color = c.text) }
                Button(
                    onClick = {
                        onSave(
                            initial.copy(
                                code = code.trim(),
                                network = network.trim().uppercase(),
                                slot = slot.trim(),
                                duration = duration.trim(),
                            )
                        )
                    },
                    enabled = valid,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.onAccent),
                    modifier = Modifier
                        .weight(1.6f)
                        .height(54.dp),
                ) { Text(confirmLabel, style = MvkType.bodyMedium.copy(fontSize = 15.sp)) }
            }
        }
    }
}
