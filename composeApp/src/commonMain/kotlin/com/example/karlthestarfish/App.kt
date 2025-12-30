package com.example.karlthestarfish

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.karlthestarfish.karl.KarlTheBot
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch



data class Message(val text: String, val isFromUser: Boolean)

@Composable
@Preview
fun App() {
    val karl = remember { KarlTheBot() }
    var userInput by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(listOf<Message>()) }
    var isChatVisible by remember { mutableStateOf(false) }

    // remember a CoroutineScope for launching non-composable coroutines
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        // Extracted chat area (now receives weight modifier from Column)
        ChatArea(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            messages = messages,
            isChatVisible = isChatVisible,
            setIsChatVisible = { isChatVisible = it }
        )

        // Extracted input row
        InputRow(
            userInput = userInput,
            onUserInputChange = { userInput = it },
            onSend = { msg ->
                // Add user message
                messages = messages + Message(msg, true)
                // clear input
                userInput = ""
                // launch Karl's reply
                coroutineScope.launch {
                    delay(500)
                    val karlResponse = karl.reply(msg)
                    messages = messages + Message(karlResponse, false)
                }
            }
        )
    }
}

@Composable
fun ChatArea(
    modifier: Modifier = Modifier,
    messages: List<Message>,
    isChatVisible: Boolean,
    setIsChatVisible: (Boolean) -> Unit
) {
    Box(modifier = modifier) {
        // ChatHistory should float above the starfish when visible
        ChatHistory(
            messages = messages,
            isChatVisible = isChatVisible,
            setIsChatVisible = setIsChatVisible,
            bottomOffset = 88.dp // avoid being covered by the input row
        )

        // Drag handle on the far right to hint at swiping
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(36.dp)
                .align(Alignment.CenterEnd)
                .pointerInput(Unit) {
                    detectDragGestures { _, dragAmount ->
                        if (dragAmount.x < -20f) setIsChatVisible(true)
                        if (dragAmount.x > 20f) setIsChatVisible(false)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Open chat",
                tint = colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }

        // Starfish and speech bubble should go to back when chat is visible
        val starfishAlpha = if (isChatVisible) 0.35f else 1f
        Box(
            modifier = Modifier
                .zIndex(if (isChatVisible) 0f else 1f)
                .alpha(starfishAlpha)
        ) {
            StarfishArea(messages = messages)
        }
    }
}

@Composable
private fun BoxScope.ChatHistory(
    messages: List<Message>,
    isChatVisible: Boolean,
    setIsChatVisible: (Boolean) -> Unit,
    bottomOffset: Dp = 0.dp
) {
    if (isChatVisible) {
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .width(280.dp)
                .padding(8.dp)
                .padding(bottom = bottomOffset)
                .align(Alignment.TopEnd)
                .zIndex(2f)
                .pointerInput(Unit) {
                    detectDragGestures { _, dragAmount ->
                        if (dragAmount.x > 20f) {
                            setIsChatVisible(false)
                        }
                    }
                },
            reverseLayout = true
        ) {
            items(messages.reversed()) { message ->
                ChatBubble(
                    message = message.text,
                    isFromUser = message.isFromUser,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(36.dp)
                .align(Alignment.TopEnd)
                .pointerInput(Unit) {
                    detectDragGestures { _, dragAmount ->
                        if (dragAmount.x < -20f) {
                            setIsChatVisible(true)
                        }
                    }
                }
        )
    }
}

@Composable
private fun StarfishArea(messages: List<Message>) {
    Box(contentAlignment = Alignment.BottomStart) {
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier
                .padding(start = 12.dp)
        ) {
        if (messages.isNotEmpty() && !messages.last().isFromUser) {
            ComicSpeechBubble(
                message = messages.last().text,
                modifier = Modifier.padding(bottom = 8.dp, start = 8.dp, end = 200.dp)
            )
        }

        Image(
            // show the provided starfish image resource
            painter = painterResource("inspirationAndArt/ourCarl.png"),
            contentDescription = "Karl the Starfish",
            modifier = Modifier
                .size(200.dp)
                .padding(8.dp),
            contentScale = ContentScale.Fit
            )
        }
    }
}

private fun ColumnScope.painterResource(resource: String): Painter {
    return painterResource(resource)
}

// Update InputRow signature to accept focus callback
@Composable
fun InputRow(
    userInput: String,
    onUserInputChange: (String) -> Unit,
    onSend: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = userInput,
            onValueChange = onUserInputChange,
            label = { Text("Type a message...") },
            modifier = Modifier
                .weight(1f),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = colorScheme.surfaceVariant,
                unfocusedContainerColor = colorScheme.surfaceVariant,
                focusedLabelColor = colorScheme.onSurfaceVariant,
                unfocusedLabelColor = colorScheme.onSurfaceVariant,
                cursorColor = colorScheme.primary,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            shape = RoundedCornerShape(24.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(colorScheme.primary)
                .clickable {
                    if (userInput.isNotBlank()) {
                        onSend(userInput)
                    }
                }
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send",
                tint = colorScheme.onPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// Fancy comic speech bubble using a rounded rect + triangular tail drawn with Canvas
@Composable
fun ComicSpeechBubble(
    message: String,
    modifier: Modifier = Modifier
) {
    val tailColor = colorScheme.secondaryContainer
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(tailColor)
                .padding(12.dp)
        ) {
            Text(
                text = message,
                color = colorScheme.onSecondaryContainer
            )
        }
        // tail triangle
        Canvas(modifier = Modifier.size(24.dp, 12.dp)) {
            val path = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width / 2f, size.height)
                lineTo(size.width, 0f)
                close()
            }
            drawPath(path, color = tailColor)
        }
    }
}

@Composable
fun ChatBubble(
    message: String,
    isFromUser: Boolean,
    modifier: Modifier = Modifier
) {
    val bubbleColor = if (isFromUser) {
        Color(0xFFE3F2FD) // Light blue for user messages
    } else {
        Color.White // White for bot messages
    }

    val alignment = if (isFromUser) Alignment.CenterEnd else Alignment.CenterStart

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(bubbleColor)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = message,
                color = Color.Black,
                fontSize = 14.sp
            )
        }
    }
}
