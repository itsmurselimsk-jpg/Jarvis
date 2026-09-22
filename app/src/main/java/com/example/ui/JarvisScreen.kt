package com.example.ui

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.JarvisTab
import com.example.data.model.MessageSender
import com.example.ui.components.ArcReactorCore
import com.example.ui.components.AudioWaveformEqualizer
import com.example.ui.components.StarkProtocolCard
import com.example.ui.components.StatusBeacon
import com.example.ui.components.TelemetryGaugeCard
import com.example.ui.components.TerminalMessageBubble
import com.example.ui.theme.ArcBlue
import com.example.ui.theme.ArcCyan
import com.example.ui.theme.ArcCyanDark
import com.example.ui.theme.DeepSpace
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.StarkGold
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidBlack
import com.example.ui.theme.WarningRed

@Composable
fun JarvisScreen(
    viewModel: JarvisViewModel = viewModel()
) {
    val diagnostics by viewModel.diagnostics.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val isFlashlightOn by viewModel.isFlashlightOn.collectAsState()
    val voiceEnabled by viewModel.voiceEnabled.collectAsState()
    val isOverclocked by viewModel.isOverclocked.collectAsState()
    val protocols by viewModel.protocols.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()

    var inputPrompt by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll chat on new message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Speech Recognizer Launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.setListening(false)
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                viewModel.sendUserPrompt(spokenText)
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBlack),
        containerColor = VoidBlack,
        topBar = {
            JarvisTopAppBar(
                isOnline = diagnostics.isOnline,
                voiceEnabled = voiceEnabled,
                isFlashlightOn = isFlashlightOn,
                onToggleVoice = { viewModel.toggleVoice() },
                onToggleFlashlight = { viewModel.toggleFlashlight() },
                onRefreshDiagnostics = { viewModel.runFullDiagnosticScan() }
            )
        },
        bottomBar = {
            JarvisBottomDock(
                inputText = inputPrompt,
                onInputChanged = { inputPrompt = it },
                onSend = {
                    if (inputPrompt.isNotBlank()) {
                        viewModel.sendUserPrompt(inputPrompt)
                        inputPrompt = ""
                    }
                },
                onStartListening = {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speaking to J.A.R.V.I.S...")
                    }
                    try {
                        viewModel.setListening(true)
                        speechLauncher.launch(intent)
                    } catch (_: Exception) {
                        viewModel.setListening(false)
                    }
                },
                isListening = isListening,
                isProcessing = isProcessing
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Segmented Mode Navigation Bar
            JarvisTabSelector(
                selectedTab = selectedTab,
                onTabSelected = { viewModel.selectTab(it) }
            )

            // Screen Content based on Selected Tab
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (selectedTab) {
                    JarvisTab.CORE_HUD -> {
                        CoreHudView(
                            batteryPct = diagnostics.batteryPct,
                            isSpeaking = isSpeaking,
                            isListening = isListening,
                            isProcessing = isProcessing,
                            isOverclocked = isOverclocked,
                            diagnostics = diagnostics,
                            messages = messages,
                            onCoreClick = { viewModel.runFullDiagnosticScan() },
                            onChipClick = { viewModel.sendUserPrompt(it) },
                            onVocalize = { viewModel.vocalizeMessage(it) }
                        )
                    }
                    JarvisTab.PROTOCOLS -> {
                        ProtocolsView(
                            protocols = protocols,
                            onExecute = { viewModel.executeProtocol(it) }
                        )
                    }
                    JarvisTab.TELEMETRY -> {
                        TelemetryDashboardView(
                            diagnostics = diagnostics,
                            isFlashlightOn = isFlashlightOn,
                            onRefresh = { viewModel.runFullDiagnosticScan() }
                        )
                    }
                    JarvisTab.ARCHIVES -> {
                        ArchivesView(
                            messages = messages,
                            isSpeaking = isSpeaking,
                            onSpeakMessage = { viewModel.vocalizeMessage(it) },
                            onClearHistory = { viewModel.clearHistory() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun JarvisTopAppBar(
    isOnline: Boolean,
    voiceEnabled: Boolean,
    isFlashlightOn: Boolean,
    onToggleVoice: () -> Unit,
    onToggleFlashlight: () -> Unit,
    onRefreshDiagnostics: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = SurfaceDark.copy(alpha = 0.95f),
        tonalElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 0.5.dp,
                    brush = Brush.horizontalGradient(listOf(GlassBorder, ArcCyan.copy(alpha = 0.4f), GlassBorder)),
                    shape = RoundedCornerShape(0.dp)
                )
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = "STARK INDUSTRIES",
                    color = StarkGold,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "J.A.R.V.I.S.",
                        color = ArcCyan,
                        fontSize = 18.sp,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    StatusBeacon(isOnline = isOnline, text = "OS 9.4")
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Flashlight toggle
                IconButton(
                    onClick = onToggleFlashlight,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = if (isFlashlightOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                        contentDescription = "Toggle Repulsor Beam",
                        tint = if (isFlashlightOn) StarkGold else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Voice output toggle
                IconButton(
                    onClick = onToggleVoice,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = if (voiceEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                        contentDescription = "Toggle Voice Synthesis",
                        tint = if (voiceEnabled) ArcCyan else WarningRed,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Diagnostic refresh
                IconButton(
                    onClick = onRefreshDiagnostics,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Diagnostic Sweep",
                        tint = TextCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun JarvisTabSelector(
    selectedTab: JarvisTab,
    onTabSelected: (JarvisTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DeepSpace)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        JarvisTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab
            val tabTitle = when (tab) {
                JarvisTab.CORE_HUD -> "ARC CORE"
                JarvisTab.PROTOCOLS -> "PROTOCOLS"
                JarvisTab.TELEMETRY -> "TELEMETRY"
                JarvisTab.ARCHIVES -> "ARCHIVES"
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) ArcCyan.copy(alpha = 0.18f) else Color.Transparent)
                    .border(
                        1.dp,
                        if (isSelected) ArcCyan else GlassBorder.copy(alpha = 0.25f),
                        RoundedCornerShape(6.dp)
                    )
                    .clickable { onTabSelected(tab) }
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tabTitle,
                    color = if (isSelected) ArcCyan else TextMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    letterSpacing = 0.8.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CoreHudView(
    batteryPct: Int,
    isSpeaking: Boolean,
    isListening: Boolean,
    isProcessing: Boolean,
    isOverclocked: Boolean,
    diagnostics: com.example.data.model.SystemDiagnostics,
    messages: List<com.example.data.model.JarvisMessage>,
    onCoreClick: () -> Unit,
    onChipClick: (String) -> Unit,
    onVocalize: (com.example.data.model.JarvisMessage) -> Unit
) {
    val lastJarvisMessage = remember(messages) {
        messages.lastOrNull { it.sender == MessageSender.JARVIS }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 16.dp)
    ) {
        // Quick Telemetry Header Strip
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceDark.copy(alpha = 0.6f))
                    .border(1.dp, GlassBorder.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SYS: OPTIMAL",
                    color = SuccessGreen,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "PWR: $batteryPct%",
                    color = if (batteryPct > 20) ArcCyan else WarningRed,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "RAM: ${diagnostics.usedRamMb}MB",
                    color = TextCyan,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "${diagnostics.batteryTempC}°C",
                    color = StarkGold,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Central Arc Reactor HUD Core
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ArcReactorCore(
                    size = 210.dp,
                    powerPct = batteryPct,
                    isSpeaking = isSpeaking,
                    isListening = isListening,
                    isProcessing = isProcessing,
                    isOverclocked = isOverclocked,
                    onCoreClick = onCoreClick
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Equalizer Bar
                AudioWaveformEqualizer(
                    isActive = isSpeaking || isListening,
                    barCount = 20,
                    color = if (isOverclocked) WarningRed else ArcCyan
                )

                Text(
                    text = if (isSpeaking) "J.A.R.V.I.S. VOCALIZING..." else if (isListening) "LISTENING TO DIRECTIVE..." else if (isProcessing) "COMPUTING NEURAL RESPONSE..." else "ARC REACTOR CORE ACTIVE • TAP FOR DIAGNOSTIC",
                    color = if (isSpeaking || isListening) ArcCyan else TextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
        }

        // Latest Transmission / Dialogue Card
        item {
            if (lastJarvisMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceDark.copy(alpha = 0.85f))
                        .border(1.dp, ArcCyan.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(ArcCyan)
                                )
                                Text(
                                    text = "J.A.R.V.I.S. TRANSMISSION",
                                    color = ArcCyan,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                            IconButton(
                                onClick = { onVocalize(lastJarvisMessage) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSpeaking) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Speak",
                                    tint = TextCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Text(
                            text = lastJarvisMessage.text,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }
            }
        }

        // Quick Command Directive Chips
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "QUICK DIRECTIVES",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "Diagnostic sweep",
                        "Check power cells",
                        "House party protocol",
                        "Engage repulsor beam",
                        "Perimeter threat status",
                        "Stealth envelope"
                    ).forEach { chipText ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(DeepSpace)
                                .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                                .clickable { onChipClick(chipText) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = chipText.uppercase(),
                                color = TextCyan,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProtocolsView(
    protocols: List<com.example.data.model.StarkProtocol>,
    onExecute: (com.example.data.model.StarkProtocol) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp)
    ) {
        item {
            Text(
                text = "STARK PROTOCOL DIRECTIVES",
                color = StarkGold,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Text(
                text = "Authorized user: Tony Stark. Touch any protocol to toggle deployment parameters.",
                color = TextSecondary,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
            )
        }

        items(protocols) { protocol ->
            StarkProtocolCard(
                protocol = protocol,
                onClick = { onExecute(protocol) }
            )
        }
    }
}

@Composable
fun TelemetryDashboardView(
    diagnostics: com.example.data.model.SystemDiagnostics,
    isFlashlightOn: Boolean,
    onRefresh: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 20.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LIVE SENSOR TELEMETRY",
                    color = ArcCyan,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Telemetry",
                        tint = ArcCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Power Grid Section
        item {
            TelemetryGaugeCard(
                title = "Main Power Grid",
                valueText = "${diagnostics.batteryPct}%",
                subText = if (diagnostics.isCharging) "Charging via external arc grid • ${diagnostics.batteryVoltageV}V" else "Discharging • Nominal flight capacity",
                fraction = diagnostics.batteryPct / 100f,
                accentColor = if (diagnostics.batteryPct > 20) ArcCyan else WarningRed,
                icon = Icons.Default.Bolt
            )
        }

        // Thermals
        item {
            TelemetryGaugeCard(
                title = "Core Thermal Sensor",
                valueText = "${diagnostics.batteryTempC} °C",
                subText = "Operating within safe titanium alloy margins (Max 65°C)",
                fraction = (diagnostics.batteryTempC / 65f).coerceIn(0f, 1f),
                accentColor = if (diagnostics.batteryTempC < 45f) StarkGold else WarningRed,
                icon = Icons.Outlined.Thermostat
            )
        }

        // RAM Allocation
        item {
            TelemetryGaugeCard(
                title = "Processor Memory Matrix",
                valueText = "${diagnostics.usedRamMb} MB / ${diagnostics.totalRamMb} MB",
                subText = "${diagnostics.ramUsagePct.toInt()}% computational memory engaged",
                fraction = diagnostics.ramUsagePct / 100f,
                accentColor = ArcBlue,
                icon = Icons.Outlined.Memory
            )
        }

        // Storage Matrix
        item {
            TelemetryGaugeCard(
                title = "Storage Matrix",
                valueText = "${String.format("%.1f", diagnostics.usedStorageGb)} GB / ${String.format("%.1f", diagnostics.totalStorageGb)} GB",
                subText = "${diagnostics.storageUsagePct.toInt()}% memory banks filled",
                fraction = diagnostics.storageUsagePct / 100f,
                accentColor = SuccessGreen,
                icon = Icons.Default.Storage
            )
        }

        // Network Uplink
        item {
            TelemetryGaugeCard(
                title = "Quantum Uplink",
                valueText = diagnostics.networkStatus,
                subText = if (diagnostics.isOnline) "Encrypted satellite feed connected" else "Local autonomous processing only",
                fraction = if (diagnostics.isOnline) 1.0f else 0.0f,
                accentColor = if (diagnostics.isOnline) ArcCyan else WarningRed,
                icon = Icons.Default.Wifi
            )
        }
    }
}

@Composable
fun ArchivesView(
    messages: List<com.example.data.model.JarvisMessage>,
    isSpeaking: Boolean,
    onSpeakMessage: (com.example.data.model.JarvisMessage) -> Unit,
    onClearHistory: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "COMMUNICATIONS ARCHIVE (${messages.size})",
                color = ArcCyan,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            IconButton(
                onClick = onClearHistory,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = "Clear History",
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(messages, key = { it.id }) { message ->
                TerminalMessageBubble(
                    message = message,
                    isSpeakingThis = isSpeaking && message.sender == MessageSender.JARVIS,
                    onSpeakClick = { onSpeakMessage(message) }
                )
            }
        }
    }
}

@Composable
fun JarvisBottomDock(
    inputText: String,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit,
    onStartListening: () -> Unit,
    isListening: Boolean,
    isProcessing: Boolean
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding(),
        color = SurfaceDark.copy(alpha = 0.96f),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 0.5.dp,
                    brush = Brush.horizontalGradient(listOf(GlassBorder, ArcCyan.copy(alpha = 0.5f), GlassBorder)),
                    shape = RoundedCornerShape(0.dp)
                )
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Microphone Button
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(if (isListening) WarningRed else ArcCyan.copy(alpha = 0.15f))
                    .border(
                        1.5.dp,
                        if (isListening) WarningRed else ArcCyan,
                        CircleShape
                    )
                    .clickable(onClick = onStartListening)
                    .testTag("voice_input_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice Directive",
                    tint = if (isListening) VoidBlack else ArcCyan,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Text Input Field
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChanged,
                modifier = Modifier
                    .weight(1f)
                    .testTag("command_input_field"),
                placeholder = {
                    Text(
                        text = if (isProcessing) "J.A.R.V.I.S. is thinking..." else "Directive to J.A.R.V.I.S...",
                        color = TextMuted,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ArcCyan,
                    unfocusedBorderColor = GlassBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = DeepSpace,
                    unfocusedContainerColor = DeepSpace,
                    cursorColor = ArcCyan
                )
            )

            // Send Button
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(if (inputText.isNotBlank() && !isProcessing) ArcCyan else SurfaceElevated)
                    .clickable(enabled = inputText.isNotBlank() && !isProcessing, onClick = onSend)
                    .testTag("send_command_button"),
                contentAlignment = Alignment.Center
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = ArcCyan,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send Directive",
                        tint = if (inputText.isNotBlank()) VoidBlack else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
