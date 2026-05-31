package com.example.game

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import java.util.Locale
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.min
import kotlin.math.PI
import kotlin.random.Random

// Particles for satisfying hit effects
data class Spark(
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    val size: Float,
    var alpha: Float,
    var age: Float,
    val maxAge: Float
)

@Composable
fun GameApp(viewModel: GameViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val showNamePrompt by viewModel.showNamePrompt.collectAsState()

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. Draw an overlay grid on the background for arcade texture
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val strokeWidth = 1.dp.toPx()
                        val gridSpacing = 40.dp.toPx()
                        
                        // Vertical grid lines
                        var x = 0f
                        while (x < size.width) {
                            drawLine(
                                color = ElectricPurple.copy(alpha = 0.04f),
                                start = Offset(x, 0f),
                                end = Offset(x, size.height),
                                strokeWidth = strokeWidth
                            )
                            x += gridSpacing
                        }
                        
                        // Horizontal grid lines
                        var y = 0f
                        while (y < size.height) {
                            drawLine(
                                color = ElectricPurple.copy(alpha = 0.04f),
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = strokeWidth
                            )
                            y += gridSpacing
                        }
                    }
            )

            // 2. Cinematic Center Glow Area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val maxGlowRadius = Math.min(size.width, size.height) * 0.7f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    ElectricBlue.copy(alpha = 0.06f),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = maxGlowRadius
                            ),
                            center = center,
                            radius = maxGlowRadius
                        )
                    }
            )

            // Screen Dispatcher
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    Screen.Home -> HomeScreen(viewModel)
                    Screen.Game -> GameScreen(viewModel)
                    Screen.Result -> ResultScreen(viewModel)
                    Screen.Leaderboard -> LeaderboardScreen(viewModel)
                }
            }

            // 3. Vignette Overlay: absolute inset-0 pointer-events-none shadow-[inset_0_0_120px_rgba(0,0,0,0.8)]
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val maxRadius = Math.max(size.width, size.height) * 0.75f
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.15f),
                                    Color.Black.copy(alpha = 0.65f),
                                    Color.Black.copy(alpha = 0.92f)
                                ),
                                center = center,
                                radius = maxRadius
                            )
                        )
                    }
            )

            // High Score Initials Dialog
            if (showNamePrompt) {
                NamePromptDialog(
                    onDismiss = { /* Forced completion, must submit */ },
                    onSubmit = { initials ->
                        viewModel.submitLeaderboardScore(initials)
                    }
                )
            }
        }
    }
}

@Composable
fun HomeScreen(viewModel: GameViewModel) {
    val bestLevel by viewModel.bestLevel.collectAsState()
    val bestAccuracy by viewModel.bestAccuracy.collectAsState()
    
    // Animation for pulsing glow circle of the logo
    val infiniteTransition = rememberInfiniteTransition(label = "LogoPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ScalePulse"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // App Header: Sophisticated Accent Label
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(ElectricBlue, RoundedCornerShape(3.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "ULTRA PRECISION",
                color = ElectricBlue.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(ElectricBlue, RoundedCornerShape(3.dp))
            )
        }

        // Central Logo & Pulsing Ring
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(240.dp)
            ) {
                // Pulse background circle outline representing custom visual track
                Canvas(modifier = Modifier.size(210.dp * pulseScale)) {
                    drawCircle(
                        color = ElectricBlue.copy(alpha = 0.12f),
                        style = Stroke(width = 4.dp.toPx())
                    )
                    drawCircle(
                        color = ElectricPurple.copy(alpha = 0.05f),
                        style = Stroke(width = 16.dp.toPx())
                    )
                }

                // Core Name branding
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "ONE",
                        fontSize = 54.sp,
                        fontWeight = FontWeight.ExtraLight,
                        color = GhostWhite,
                        letterSpacing = 8.sp,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = "TAP",
                        fontSize = 62.sp,
                        fontWeight = FontWeight.Light,
                        color = ElectricBlue,
                        letterSpacing = 10.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "MATCH MICROSECONDS",
                        fontSize = 10.sp,
                        color = MutedSlate,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Quick Stats Card with Sophisticated Glass Look
            Card(
                colors = CardDefaults.cardColors(containerColor = SpaceCard.copy(alpha = 0.75f)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .width(290.dp)
                    .padding(vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("BEST LEVEL", fontSize = 9.sp, color = MutedSlate, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "LVL $bestLevel",
                            color = NeonYellow,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Light,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Box(
                        modifier = Modifier
                            .height(30.dp)
                            .width(1.dp)
                            .background(Color.White.copy(alpha = 0.08f))
                    )
                    Column(horizontalAlignment = Alignment.End) {
                        Text("PRECISION", fontSize = 9.sp, color = MutedSlate, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            String.format(Locale.getDefault(), "%.1f%%", bestAccuracy),
                            color = NeonGreen,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Light,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Action Buttons Row with Console Navigation Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // PLAY BUTTON (Large Electric Pulsing Button with sleek touch shadow)
            Button(
                onClick = { viewModel.navigateTo(Screen.Game) },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .testTag("play_button")
                    .border(1.dp, GhostWhite.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = DeepNavy,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "START CAMPAIGN",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepNavy,
                        letterSpacing = 2.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // The capsule bottom nav bar from HTML
            ConsoleNavigationBar(
                currentScreen = Screen.Home,
                onTabSelected = { viewModel.navigateTo(it) }
            )
        }
    }
}

@Composable
fun GameScreen(viewModel: GameViewModel) {
    val playState by viewModel.playState.collectAsState()
    val level by viewModel.currentLevel.collectAsState()
    val targetWidth by viewModel.targetWidth.collectAsState()
    val targetCenterAngle by viewModel.targetCenterAngle.collectAsState()
    val lastAccuracy by viewModel.lastAccuracy.collectAsState()

    var ballAngle by remember { mutableStateOf(0f) }
    val sparks = remember { mutableStateListOf<Spark>() }
    
    // Core timing-loop for fluid continuous rotation
    LaunchedEffect(playState) {
        if (playState == PlayState.Running) {
            val speed = viewModel.getCurrentSpeed() // Deg per sec
            var lastTime = System.nanoTime()
            while (playState == PlayState.Running) {
                withFrameMillis { time ->
                    val now = System.nanoTime()
                    val deltaSeconds = (now - lastTime) / 1_000_000_000f
                    lastTime = now
                    val prevAngle = ballAngle
                    val nextAngle = (ballAngle + speed * deltaSeconds) % 360f
                    ballAngle = nextAngle
                    
                    // Play a subtle mechanical clock/radar tick sound upon wrapping complete 360 loop rotation
                    if (nextAngle < prevAngle) {
                        GameEffectsManager.playTick()
                    }
                }
            }
        }
    }

    // Dynamic Physics Particle Loop
    LaunchedEffect(playState) {
        if (playState == PlayState.Hit) {
            // Particle update is active
        }
        while (true) {
            withFrameMillis {
                val iterator = sparks.iterator()
                while (iterator.hasNext()) {
                    val s = iterator.next()
                    s.x += s.vx
                    s.y += s.vy
                    s.alpha = (1f - s.age / s.maxAge).coerceAtLeast(0f)
                    s.age += 16f // 16ms approx
                    if (s.age >= s.maxAge) {
                        iterator.remove()
                    }
                }
            }
        }
    }

    // Spring Screen Shaker offset on MISS
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(playState) {
        if (playState == PlayState.Miss) {
            repeat(4) {
                shakeOffset.animateTo(24f, spring(dampingRatio = 0.2f, stiffness = 1200f))
                shakeOffset.animateTo(-24f, spring(dampingRatio = 0.2f, stiffness = 1200f))
            }
            shakeOffset.animateTo(0f, spring(stiffness = Spring.StiffnessLow))
        }
    }

    // Tap processor covering the entire viewport surface
    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset(x = shakeOffset.value.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (playState == PlayState.Ready) {
                    viewModel.startSpinning()
                } else if (playState == PlayState.Running) {
                    viewModel.processTap(ballAngle)
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Status Header Info row (Sophisticated Dark layout)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, start = 8.dp, end = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.returnToHome() },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close/Exit to main menu",
                        tint = GhostWhite.copy(alpha = 0.6f)
                    )
                }

                Column {
                    Text(
                        text = "CURRENT LEVEL",
                        color = ElectricBlue.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = String.format(Locale.getDefault(), "%02d", level),
                        color = GhostWhite,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = (-1).sp
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                val bestLevel by viewModel.bestLevel.collectAsState()
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "PERSONAL BEST",
                        color = MutedSlate,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = String.format(Locale.getDefault(), "%02d", bestLevel),
                        color = GhostWhite.copy(alpha = 0.7f),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = (-1).sp
                    )
                }
            }

            // Orbital Game Area Canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val density = LocalDensity.current
                var canvasWidth = 300f
                var canvasHeight = 300f

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .aspectRatio(1f)
                        .padding(16.dp)
                        .testTag("game_canvas")
                ) {
                    canvasWidth = size.width
                    canvasHeight = size.height
                    
                    val centerPoint = Offset(size.width / 2f, size.height / 2f)
                    val radius = min(size.width, size.height) * 0.36f

                    // 1. Draw outer orbit track (slate-900 border)
                    drawCircle(
                        color = Color(0xFF0F172A),
                        center = centerPoint,
                        radius = radius,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // 2. Draw Target Zone segment
                    val targetStartAngle = (targetCenterAngle - targetWidth / 2f)
                    
                    // Outer neon green glow beneath the target zone
                    drawArc(
                        color = NeonGreen.copy(alpha = 0.25f),
                        startAngle = targetStartAngle,
                        sweepAngle = targetWidth,
                        useCenter = false,
                        topLeft = Offset(centerPoint.x - radius, centerPoint.y - radius),
                        size = Size(radius * 2f, radius * 2f),
                        style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Sharp vibrant emerald target segment
                    drawArc(
                        color = NeonGreen,
                        startAngle = targetStartAngle,
                        sweepAngle = targetWidth,
                        useCenter = false,
                        topLeft = Offset(centerPoint.x - radius, centerPoint.y - radius),
                        size = Size(radius * 2f, radius * 2f),
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw inner target guidelines
                    drawCircle(
                        color = ElectricBlue.copy(alpha = 0.05f),
                        center = centerPoint,
                        radius = radius - 16.dp.toPx(),
                        style = Stroke(width = 1.dp.toPx())
                    )

                    // 3. Draw Rotating Ball dot indicator
                    val angleRad = Math.toRadians(ballAngle.toDouble())
                    val ballX = centerPoint.x + radius * cos(angleRad).toFloat()
                    val ballY = centerPoint.y + radius * sin(angleRad).toFloat()

                    // Trail effect
                    for (i in 1..4) {
                        val trailAngle = (ballAngle - i * 4f)
                        val trailRad = Math.toRadians(trailAngle.toDouble())
                        val tx = centerPoint.x + radius * cos(trailRad).toFloat()
                        val ty = centerPoint.y + radius * sin(trailRad).toFloat()
                        drawCircle(
                            color = ElectricBlue.copy(alpha = 0.2f / i),
                            radius = (10.dp.toPx() * (1f - i * 0.15f)),
                            center = Offset(tx, ty)
                        )
                    }

                    val ballColor = when (playState) {
                        PlayState.Hit -> NeonGreen
                        PlayState.Miss -> NeonRed
                        else -> ElectricBlue
                    }

                    // Shadow/Glow around the active precision ball
                    drawCircle(
                        color = ballColor.copy(alpha = 0.45f),
                        radius = 18.dp.toPx(),
                        center = Offset(ballX, ballY)
                    )

                    drawCircle(
                        color = ballColor,
                        radius = 11.dp.toPx(),
                        center = Offset(ballX, ballY)
                    )

                    // Spawning sparks burst on HIT triggers
                    if (playState == PlayState.Hit && sparks.isEmpty()) {
                        // Spawn 32 particles in random direction
                        for (j in 0 until 35) {
                             val angle = Random.nextDouble(0.0, 2 * PI)
                             val speed = Random.nextFloat() * 12f + 4f
                             val sizeV = Random.nextFloat() * 6.dp.toPx() + 3.dp.toPx()
                             sparks.add(
                                 Spark(
                                     x = ballX,
                                     y = ballY,
                                     vx = cos(angle).toFloat() * speed,
                                     vy = sin(angle).toFloat() * speed,
                                     color = if (Random.nextBoolean()) NeonGreen else ElectricBlue,
                                     size = sizeV,
                                     alpha = 1f,
                                     age = 0f,
                                     maxAge = 400f + Random.nextInt(200)
                                 )
                             )
                        }
                    }

                    // Render current spark visual array list
                    sparks.forEach { spark ->
                        drawCircle(
                            color = spark.color.copy(alpha = spark.alpha),
                            radius = spark.size,
                            center = Offset(spark.x, spark.y)
                        )
                    }
                }

                // Help instructions overlay in center of orbital ring
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(16.dp)
                ) {
                    AnimatedContent(
                        targetState = playState,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "InstructionText"
                    ) { state ->
                        when (state) {
                            PlayState.Ready -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "READY",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraLight,
                                        color = MutedSlate,
                                        letterSpacing = 2.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "TAP TO SPIN",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ElectricBlue,
                                        letterSpacing = 1.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                            PlayState.Running -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "LOCK IN",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MutedSlate,
                                        letterSpacing = 2.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "TAP NOW",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = NeonGreen,
                                        letterSpacing = 1.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                            PlayState.Hit -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            text = String.format(Locale.getDefault(), "%.0f", lastAccuracy),
                                            fontSize = 54.sp,
                                            fontWeight = FontWeight.ExtraLight,
                                            color = GhostWhite,
                                            letterSpacing = (-1).sp
                                        )
                                        Text(
                                            text = "%",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Light,
                                            color = GhostWhite.copy(alpha = 0.7f),
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (lastAccuracy >= 95) "PERFECT!" else if (lastAccuracy >= 85) "EXCELLENT!" else "HIT!",
                                        fontSize = 10.sp,
                                        color = NeonGreen,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 2.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                            PlayState.Miss -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "0",
                                        fontSize = 54.sp,
                                        fontWeight = FontWeight.ExtraLight,
                                        color = NeonRed,
                                        letterSpacing = (-1).sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "TOO OFF",
                                        fontSize = 10.sp,
                                        color = MutedSlate,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 2.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom responsive prompt button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AnimatedVisibility(
                    visible = playState == PlayState.Hit || playState == PlayState.Miss,
                    enter = slideInVertically(initialOffsetY = { 40 }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { 40 }) + fadeOut()
                ) {
                    if (playState == PlayState.Hit) {
                        Button(
                            onClick = { viewModel.proceedToNextLevel() },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("next_stage_button")
                        ) {
                            Text(
                                "PROCEED TO LEVEL ${level + 1}",
                                color = DeepNavy,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                        }
                    } else {
                        Button(
                            onClick = { viewModel.navigateTo(Screen.Result) },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonRed),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("view_results_button")
                        ) {
                            Text(
                                "VIEW RESULTS",
                                color = GhostWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                        }
                    }
                }

                // Simple Elegant Pulse Dot footer message
                if (playState == PlayState.Ready || playState == PlayState.Running) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 12.dp)
                    ) {
                        val pulseAlphaTransition = rememberInfiniteTransition(label = "PulseAlpha")
                        val pulseAlpha by pulseAlphaTransition.animateFloat(
                            initialValue = 0.2f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(800, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "PulseAlphaValue"
                        )
                        
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(ElectricBlue.copy(alpha = pulseAlpha), RoundedCornerShape(3.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "TAP ANYWHERE TO ACTIVATE HIT",
                            color = GhostWhite.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ResultScreen(viewModel: GameViewModel) {
    val level by viewModel.currentLevel.collectAsState()
    val playState by viewModel.playState.collectAsState()
    val lastAccuracy by viewModel.lastAccuracy.collectAsState()
    val bestLevel by viewModel.bestLevel.collectAsState()
    val bestAccuracy by viewModel.bestAccuracy.collectAsState()

    val wasNewRecord = (level >= bestLevel)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Upper status text Header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(
                text = "SEQUENCE OVER",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MutedSlate,
                letterSpacing = 3.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "LEVEL BREAKDOWN",
                fontSize = 28.sp,
                fontWeight = FontWeight.Light,
                color = ElectricBlue,
                letterSpacing = 1.sp
            )
        }

        // Center card stats view
        Card(
            colors = CardDefaults.cardColors(containerColor = SpaceCard.copy(alpha = 0.75f)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Game Over status badge
                Box(
                    modifier = Modifier
                        .background(NeonRed.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                        .border(1.dp, NeonRed.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "FAILED AT STAGE $level",
                        color = NeonRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Score metrics layout
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("FINAL SCORE", fontSize = 9.sp, color = MutedSlate, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "LVL $level",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Light,
                            color = GhostWhite,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(35.dp)
                            .width(1.dp)
                            .background(Color.White.copy(alpha = 0.08f))
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("LAST ACCURACY", fontSize = 9.sp, color = MutedSlate, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "0.0%",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Light,
                            color = NeonRed,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Divider line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.05f))
                )

                // Highscore achievements showcase details
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Stats Information Info Icon",
                            tint = NeonYellow,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "PERSONAL BEST LEVEL",
                            fontSize = 12.sp,
                            color = GhostWhite.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Normal
                        )
                    }
                    Text(
                        "LVL $bestLevel",
                        fontSize = 15.sp,
                        color = NeonYellow,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Success tick icon info",
                            tint = NeonGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "PERSONAL BEST ACCURACY",
                            fontSize = 12.sp,
                            color = GhostWhite.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Normal
                        )
                    }
                    Text(
                        String.format(Locale.getDefault(), "%.1f%%", bestAccuracy),
                        fontSize = 15.sp,
                        color = NeonGreen,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                if (wasNewRecord) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .background(ElectricBlue.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                            .border(1.dp, ElectricBlue.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Star record achievement badge icon",
                                tint = ElectricBlue,
                                modifier = Modifier
                                    .size(20.dp)
                                    .padding(end = 4.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "NEW LEADERBOARD MILESTONE!",
                                fontSize = 10.sp,
                                color = ElectricBlue,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // Navigation Action Buttons setup
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // RETRY BUTTON (Wide primary)
            Button(
                onClick = { viewModel.restartGame(); viewModel.navigateTo(Screen.Game) },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .testTag("retry_button")
                    .border(1.dp, GhostWhite.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Retry level",
                        tint = DeepNavy,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "RETRY STAGE",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepNavy,
                        letterSpacing = 1.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Beautiful Bottom Navigation Console Pill
            ConsoleNavigationBar(
                currentScreen = Screen.Result,
                onTabSelected = { viewModel.navigateTo(it) }
            )
        }
    }
}

@Composable
fun LeaderboardScreen(viewModel: GameViewModel) {
    val scores by viewModel.leaderboard.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top status labels & nav header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.returnToHome() },
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back back navigation arrow details to return",
                    tint = GhostWhite.copy(alpha = 0.6f)
                )
            }
            
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "TOP PLAYERS DATA",
                    fontSize = 11.sp,
                    color = MutedSlate,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    "GLOBAL LEADERBOARD",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Light,
                    color = ElectricBlue,
                    letterSpacing = 1.sp
                )
            }

            // Dummy box to offset the back button
            Box(modifier = Modifier.size(44.dp))
        }

        // Scoring list representation containing current scores in table
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Table Header descriptor
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("RANK/PLAYER", fontSize = 10.sp, color = MutedSlate, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Row(horizontalArrangement = Arrangement.spacedBy(30.dp)) {
                    Text("STAGE", fontSize = 10.sp, color = MutedSlate, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Text("PRECISION", fontSize = 10.sp, color = MutedSlate, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }

            // Score loop list rows
            scores.forEach { score ->
                val rowBorderColor = if (score.isLocalPlayer) ElectricBlue else Color.White.copy(alpha = 0.05f)
                val rowBackground = if (score.isLocalPlayer) ElectricBlue.copy(alpha = 0.08f) else SpaceCard.copy(alpha = 0.4f)
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(rowBackground, RoundedCornerShape(16.dp))
                        .border(1.dp, rowBorderColor, RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Badge numbering accent colors
                        val rankColor = when (score.rank) {
                            1 -> NeonYellow
                            2 -> ElectricBlue
                            3 -> ElectricPurple
                            else -> MutedSlate
                        }

                        Text(
                            "#${score.rank}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = rankColor,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.width(36.dp)
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    score.name,
                                    fontSize = 15.sp,
                                    fontWeight = if (score.isLocalPlayer) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (score.isLocalPlayer) ElectricBlue else GhostWhite,
                                )
                                if (score.isLocalPlayer) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(ElectricBlue.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            "YOU",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ElectricBlue,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                            Text(
                                score.date,
                                fontSize = 9.sp,
                                color = MutedSlate,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(36.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "LVL ${score.levelReached}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonYellow,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            String.format(Locale.getDefault(), "%.1f%%", score.accuracy),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonGreen,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.width(55.dp),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Bottom responsive action bottom capsule navigation bar
        ConsoleNavigationBar(
            currentScreen = Screen.Leaderboard,
            onTabSelected = { viewModel.navigateTo(it) }
        )
    }
}

// Dialog Prompt popup prompting players initials
@Composable
fun NamePromptDialog(
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var rawText by remember { mutableStateOf("") }
    val maxInitialsLength = 10

    Dialog(
        onDismissRequest = { /* forced submit modal screen */ },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SpaceCard),
            border = BorderStroke(2.dp, ElectricBlue),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "New score recording indicator trophies gold",
                    tint = NeonYellow,
                    modifier = Modifier.size(48.dp)
                )

                Text(
                    "STAGE RECORD DETECTED!",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonYellow,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )

                Text(
                    "You've broken into the Matrix top ranks. Key in your digital initials to claim your position:",
                    fontSize = 12.sp,
                    color = GhostWhite,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                OutlinedTextField(
                    value = rawText,
                    onValueChange = { input ->
                        if (input.length <= maxInitialsLength) {
                            rawText = input.uppercase().replace(Regex("[^A-Z0-9 ]"), "")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("name_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricBlue,
                        unfocusedBorderColor = ElectricPurple.copy(alpha = 0.5f),
                        focusedTextColor = GhostWhite,
                        unfocusedTextColor = GhostWhite,
                        cursorColor = ElectricBlue
                    ),
                    placeholder = {
                        Text(
                            "PILOT INITIALS",
                            fontSize = 12.sp,
                            color = MutedSlate,
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (rawText.trim().isNotEmpty()) {
                                onSubmit(rawText)
                            }
                        }
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            if (rawText.trim().isNotEmpty()) {
                                onSubmit(rawText)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        enabled = rawText.trim().isNotEmpty()
                    ) {
                        Text(
                            "SUBMIT SCORE",
                            fontWeight = FontWeight.Bold,
                            color = DeepNavy,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConsoleNavigationBar(
    currentScreen: Screen,
    onTabSelected: (Screen) -> Unit
) {
    Row(
        modifier = Modifier
            .width(280.dp)
            .background(Color(0xFF0F172A).copy(alpha = 0.6f), RoundedCornerShape(32.dp))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(32.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Tab 1: Home/Play
        IconButton(
            onClick = { onTabSelected(Screen.Home) },
            modifier = Modifier
                .size(48.dp)
                .background(
                    if (currentScreen == Screen.Home) ElectricBlue.copy(alpha = 0.15f) else Color.Transparent,
                    RoundedCornerShape(24.dp)
                )
        ) {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = "Home",
                tint = if (currentScreen == Screen.Home) ElectricBlue else MutedSlate
            )
        }

        // Tab 2: Game
        IconButton(
            onClick = { onTabSelected(Screen.Game) },
            modifier = Modifier
                .size(48.dp)
                .background(
                    if (currentScreen == Screen.Game) ElectricBlue.copy(alpha = 0.15f) else Color.Transparent,
                    RoundedCornerShape(24.dp)
                )
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Game",
                tint = if (currentScreen == Screen.Game) ElectricBlue else MutedSlate
            )
        }

        // Tab 3: Leaderboard
        IconButton(
            onClick = { onTabSelected(Screen.Leaderboard) },
            modifier = Modifier
                .size(48.dp)
                .background(
                    if (currentScreen == Screen.Leaderboard) ElectricBlue.copy(alpha = 0.15f) else Color.Transparent,
                    RoundedCornerShape(24.dp)
                )
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Leaderboard",
                tint = if (currentScreen == Screen.Leaderboard) ElectricBlue else MutedSlate
            )
        }
    }
}
