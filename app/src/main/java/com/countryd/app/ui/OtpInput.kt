package com.countryd.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import kotlinx.coroutines.delay

@Composable
fun OtpInput(value: String, error: Boolean, onChange: (String) -> Unit) {
    val focus = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(250)
        focus.requestFocus()
    }
    BasicTextField(
        value,
        { onChange(it.filter(Char::isDigit).take(6)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier =
            Modifier.fillMaxWidth()
                .padding(top = 12.dp)
                .focusRequester(focus)
                .onFocusChanged { focused = it.isFocused }
                .semantics { contentDescription = "6-digit verification code" },
        decorationBox = { input ->
            Box {
                Box(Modifier.size(1.dp).alpha(0f)) { input() }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(6) { index ->
                        val digit = value.getOrNull(index)
                        val active = focused && index == value.length.coerceAtMost(5)
                        Box(
                            Modifier.weight(1f)
                                .aspectRatio(.86f)
                                .background(
                                    if (error) Color(0xFFFFF4F4)
                                    else if (digit != null) Color.White else Color(0xFFF3F8F4),
                                    RoundedCornerShape(14.dp),
                                )
                                .border(
                                    1.5.dp,
                                    if (error) Color(0xFFC93636)
                                    else if (active) Green
                                    else if (digit != null) Color(0xFFB9D9C4)
                                    else Color(0xFFDCE6DF),
                                    RoundedCornerShape(14.dp),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (digit != null)
                                Text(
                                    digit.toString(),
                                    fontFamily = Onest,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (error) Color(0xFFC93636) else Ink,
                                )
                            else if (active)
                                Box(Modifier.width(2.dp).height(22.dp).background(Green))
                            else
                                Box(
                                    Modifier.size(4.dp)
                                        .background(Color(0xFFC7D5CD), RoundedCornerShape(2.dp))
                                )
                        }
                    }
                }
            }
        },
    )
}
