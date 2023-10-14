package com.dongtran.wirelesschargeoptimize

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dongtran.wirelesschargeoptimize.ui.theme.WirelessChargeOptimizeTheme

class MainActivity : ComponentActivity() {
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                val temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
                val chargingStatus = when (intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
                    BatteryManager.BATTERY_STATUS_CHARGING -> "Đang sạc"
                    BatteryManager.BATTERY_STATUS_DISCHARGING -> "Đang sử dụng"
                    else -> "Không sạc"
                }
                val chargingSource = when (intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)) {
                    BatteryManager.BATTERY_PLUGGED_AC -> "Sạc từ dây"
                    BatteryManager.BATTERY_PLUGGED_USB -> "Sạc từ cổng USB"
                    BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Sạc không dây"
                    else -> "Không xác định nguồn"
                }
                val batteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
                val batteryVoltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
                val batteryCycleCount = intent.getIntExtra("android.os.extra.BATTERY_CYCLE_COUNT", 0)
                val chargingCurrent = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, 0)

                batteryTemperature = temperature
                batteryStatus = chargingStatus
                chargingSourceText = chargingSource
                batteryLevelText = "$batteryLevel%"
                batteryVoltageText = "$batteryVoltage mV"
            }
        }
    }

    // Sử dụng MutableState để lưu trạng thái nhiệt độ pin và các thông tin khác
    private var batteryTemperature by mutableStateOf(0)
    private var batteryStatus by mutableStateOf("Không xác định")
    private var chargingSourceText by mutableStateOf("Không xác định nguồn")
    private var batteryLevelText by mutableStateOf("0%")
    private var batteryVoltageText by mutableStateOf("0 mV")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WirelessChargeOptimizeTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ChargingOptimizeScreen(
                        batteryTemperature,
                        batteryStatus,
                        chargingSourceText,
                        batteryLevelText,
                        batteryVoltageText,
                    )
                }
            }
        }

        // Khởi động dịch vụ ở đây
        val serviceIntent = Intent(this, ForegroundService::class.java)
        startService(serviceIntent)

        // Đăng ký BroadcastReceiver để lắng nghe thông báo nhiệt độ pin và các thông tin khác
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(batteryReceiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Hủy đăng ký BroadcastReceiver khi hoạt động bị hủy
        unregisterReceiver(batteryReceiver)
    }
}

@Composable
fun ChargingOptimizeScreen(
    temperature: Int,
    chargingStatus: String,
    chargingSource: String,
    batteryLevel: String,
    batteryVoltage: String,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Wireless Charging Optimize",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Battery Temperature: $temperature °C",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        Text(
            text = "Battery Status: $chargingStatus",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        Text(
            text = "Charging Source: $chargingSource",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        Text(
            text = "Battery Level: $batteryLevel",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        Text(
            text = "Battery Voltage: $batteryVoltage",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ChargingOptimizeScreenPreview() {
    WirelessChargeOptimizeTheme {
        ChargingOptimizeScreen(
            temperature = 25,
            chargingStatus = "Đang sạc",
            chargingSource = "Sạc từ dây",
            batteryLevel = "80%",
            batteryVoltage = "4000 mV",
        )
    }
}
