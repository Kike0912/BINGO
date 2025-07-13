package com.example.bingogame

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bingogame.ui.theme.BingoGameTheme
import java.util.*
import kotlin.random.Random

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private lateinit var tts: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        enableEdgeToEdge()
        setContent {
            BingoGameTheme {
                BingoApp(tts)
            }
        }
    }

    override fun onDestroy() {
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun BingoApp(tts: TextToSpeech) {
    var matrixSizeText by remember { mutableStateOf("5") }
    var matrixSize by remember { mutableStateOf(5) }
    var uid by remember { mutableStateOf("") }
    var showGame by remember { mutableStateOf(false) }
    var bingoNumbers by remember { mutableStateOf(generateBingoMatrix(matrixSize)) }
    val markedNumbers = remember { mutableStateListOf<Int>() }
    val context = LocalContext.current

    if (!showGame) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🎯 Juego de Bingo", fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = matrixSizeText,
                onValueChange = {
                    matrixSizeText = it
                    matrixSize = it.toIntOrNull()?.coerceIn(3, 10) ?: 5
                },
                label = { Text("Tamaño de la matriz (3 - 10)") }
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(onClick = {
                uid = "UID-${Random.nextInt(100, 999)}-${Random.nextInt(100, 999)}"
                bingoNumbers = generateBingoMatrix(matrixSize)
                markedNumbers.clear()
                showGame = true
            }) {
                Text("Generar Bingo")
            }
        }
    } else {
        val isBingoAchieved = isBingo(bingoNumbers, matrixSize, markedNumbers.toSet())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .background(if (isBingoAchieved) Color(0xFFB9FBC0) else Color.White),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(visible = true, enter = fadeIn() + slideInVertically()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("UID del Jugador: $uid", fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("¡Toca los números para marcarlos!", fontSize = 18.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            for (row in bingoNumbers.chunked(matrixSize)) {
                Row(horizontalArrangement = Arrangement.Center) {
                    row.forEach { cell ->
                        val isMarked = markedNumbers.contains(cell) || cell == 0
                        val animatedColor by animateColorAsState(
                            targetValue = if (isMarked) Color.Green else Color(0xFF7B4D9F),
                            label = "colorAnim"
                        )

                        Box(
                            modifier = Modifier
                                .padding(6.dp)
                                .size(50.dp)
                                .background(animatedColor, CircleShape)
                                .border(3.dp, Color.Red, CircleShape)
                                .clickable {
                                    if (cell != 0 && !markedNumbers.contains(cell)) {
                                        markedNumbers.add(cell)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (cell == 0) "★" else "$cell",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(onClick = {
                if (isBingoAchieved) {
                    Toast.makeText(context, "¡BINGO!", Toast.LENGTH_LONG).show()
                    tts.speak("Bingo!", TextToSpeech.QUEUE_FLUSH, null, "bingo")
                } else {
                    Toast.makeText(context, "Aún no hay Bingo", Toast.LENGTH_SHORT).show()
                }
            }) {
                Text("Validar Bingo")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(onClick = {
                bingoNumbers = generateBingoMatrix(matrixSize)
                markedNumbers.clear()
            }) {
                Text("🎲 Regenerar Carta")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(onClick = {
                showGame = false
            }) {
                Text("↩️ Volver al inicio")
            }

            Spacer(modifier = Modifier.height(10.dp))

            AnimatedVisibility(visible = isBingoAchieved, enter = fadeIn() + scaleIn()) {
                Text(
                    "🎉 ¡BINGO!",
                    fontSize = 24.sp,
                    color = Color(0xFF2E7D32),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

fun generateBingoMatrix(size: Int): List<Int> {
    val numbers = (1..100).shuffled().take(size * size).toMutableList()
    if (size % 2 != 0) {
        numbers[(size * size) / 2] = 0 // Centro libre
    }
    return numbers
}

fun isBingo(numbers: List<Int>, size: Int, marked: Set<Int>): Boolean {
    val grid = numbers.chunked(size)

    fun isMarked(value: Int): Boolean {
        return value == 0 || marked.contains(value)
    }

    // Filas
    for (row in grid) if (row.all(::isMarked)) return true

    // Columnas
    for (i in 0 until size) if ((0 until size).all { j -> isMarked(grid[j][i]) }) return true

    // Diagonales
    if ((0 until size).all { i -> isMarked(grid[i][i]) }) return true
    if ((0 until size).all { i -> isMarked(grid[i][size - i - 1]) }) return true

    // Cuadrados de al menos 2x2
    for (i in 0 until size - 1) {
        for (j in 0 until size - 1) {
            // Verifica si las 4 esquinas del cuadrado 2x2 están marcadas
            val topLeft = grid[i][j]
            val topRight = grid[i][j + 1]
            val bottomLeft = grid[i + 1][j]
            val bottomRight = grid[i + 1][j + 1]

            if (isMarked(topLeft) && isMarked(topRight) &&
                isMarked(bottomLeft) && isMarked(bottomRight)
            ) {
                return true
            }
        }
    }

    return false
}
