package com.dongtran.wirelesschargeoptimize

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.app.AlarmManager
import android.app.PendingIntent
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.dongtran.wirelesschargeoptimize.ui.theme.WirelessChargeOptimizeTheme


class MainActivity : ComponentActivity() {
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                val temperatureInt = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
                val temperature = String.format("%.1f", temperatureInt / 10.0)
                val chargingStatus = when (intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
                    BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
                    BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
                    else -> "Not charging"
                }
                val chargingSource = when (intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)) {
                    BatteryManager.BATTERY_PLUGGED_AC -> "AC"
                    BatteryManager.BATTERY_PLUGGED_USB -> "USB"
                    BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
                    else -> "Unknown"
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
    private var batteryTemperature by mutableStateOf("0")
    private var batteryStatus by mutableStateOf("waiting")
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
    temperature: String,
    chargingStatus: String,
    chargingSource: String,
    batteryLevel: String,
    batteryVoltage: String,
) {
    val colorScheme = MaterialTheme.colorScheme

    val lottieView = LottieAnimationView(LocalContext.current).apply {
        setAnimation(R.raw.charging_animation)
        repeatCount = LottieDrawable.INFINITE
        playAnimation()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(0.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colorScheme.primary)
                .padding(12.dp)
        ) {
            Text("Wireless Charging Smart")
        }
        AndroidView(
            factory = { lottieView },
            modifier = Modifier
                .height(400.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 0.dp)
        ) {
            // Thẻ đầu tiên
            Card(
                modifier = Modifier
                    .weight(1f)
                    .padding(4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Status",
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Status: $chargingStatus",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .padding(4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Charging Source",
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "$chargingSource",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 0.dp)
        ) {
            // Thẻ đầu tiên
            Card(
                modifier = Modifier
                    .weight(1f)
                    .padding(4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Temperature",
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "$temperature °C",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .padding(4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Voltage",
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "$batteryVoltage",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally

        ){
            Text(
                text = "App is running. You can close the app.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ChargingOptimizeScreenPreview() {
    WirelessChargeOptimizeTheme {
        ChargingOptimizeScreen(
            temperature = "32",
            chargingStatus = "Đang sạc",
            chargingSource = "Sạc từ dây",
            batteryLevel = "80%",
            batteryVoltage = "4000 mV",
        )
    }
}
