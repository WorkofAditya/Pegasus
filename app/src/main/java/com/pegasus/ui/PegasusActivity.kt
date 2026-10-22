package com.pegasus.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pegasus.PegasusApp
import com.pegasus.utils.DeviceUtils
import com.pegasus.utils.NetworkUtils
import org.json.JSONObject

class PegasusActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PegasusDashboard()
                }
            }
        }
    }
}

@Composable
fun PegasusDashboard() {
    val context = LocalContext.current as com.pegasus.PegasusApp
    var lastCommand by remember { mutableStateOf<String>("No command executed") }
    var isExecuting by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "PEGASUS",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Device: ${DeviceUtils.getDeviceModel()}\nAndroid: ${DeviceUtils.getAndroidVersion()}\nNetwork: ${NetworkUtils.getNetworkType(context)}",
            fontSize = 12.sp
        )

        HorizontalDivider()

        // Command Buttons
        Text("Execute Command", fontWeight = FontWeight.SemiBold)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CommandButton("System Info") {
                executeCommand(it, "system", JSONObject().put("action", "info"))
            }
            CommandButton("Battery") {
                executeCommand(it, "system", JSONObject().put("action", "battery"))
            }
            CommandButton("Location") {
                executeCommand(it, "location", JSONObject().put("action", "once"))
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CommandButton("List Files") {
                executeCommand(it, "file", JSONObject().put("action", "list").put("path", "/sdcard"))
            }
            CommandButton("Packages") {
                executeCommand(it, "package", JSONObject().put("action", "list"))
            }
            CommandButton("Processes") {
                executeCommand(it, "process", JSONObject().put("action", "list"))
            }
        }

        HorizontalDivider()

        // Output
        Text("Last Output:", fontWeight = FontWeight.SemiBold)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = lastCommand,
                    fontSize = 10.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun CommandButton(label: String, onClick: (String) -> Unit) {
    Button(
        onClick = { onClick(label) },
        modifier = Modifier.weight(1f)
    ) {
        Text(text = label, fontSize = 12.sp)
    }
}

fun executeCommand(context: Context, type: String, payload: JSONObject) {
    // This is a synchronous call for demo purposes
    // In production, this would be async
    val dispatcher = PegasusApp.instance.getDispatcher()
    val result = dispatcher.execute(type, payload)
    // Update UI with result
}