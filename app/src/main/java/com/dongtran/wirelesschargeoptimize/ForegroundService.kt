package com.dongtran.wirelesschargeoptimize

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import java.util.*

class ForegroundService : Service() {
    private val TAG = "ForegroundService"
    private val handler = Handler(Looper.getMainLooper())
    private var isCharging = false
    private var isChargedFull = false
    private val BATTERY_LEVEL_NEED_CHARGE = 25
    private val BATTERY_LEVEL_FULL = 85
    private lateinit var mqttClient: MqttClient
    private val mqttTopic = "wirelesscharge/data"
    private var isServiceRunning = false
    private var cnt = 0

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "ForegroundServiceChannel"
        private const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")

        if (!isServiceRunning) {
            val notification = createNotification()

            // Check battery
            val batteryPercentageInit = getBatteryPercentage(applicationContext)
            if (batteryPercentageInit < (BATTERY_LEVEL_FULL - 10)) {
                isCharging = true
            } else {
                isChargedFull = true
            }

            // Start the Foreground service with a notification
            startForeground(NOTIFICATION_ID, notification)
            isServiceRunning = true

            handler.postDelayed(object : Runnable {
                override fun run() {
                    // Send POST request here
                    Log.d(TAG, "Sending POST request...")
                    val batteryPercentageNow = getBatteryPercentage(applicationContext)
                    print("batteryPercentage: ")
                    println(batteryPercentageNow)

                    if (isCharging) {
                        if (batteryPercentageNow >= BATTERY_LEVEL_FULL) {
                            isCharging = false
                            isChargedFull = true
                        }
                    } else if (isChargedFull) {
                        if (batteryPercentageNow <= BATTERY_LEVEL_NEED_CHARGE) {
                            isCharging = true
                            isChargedFull = false
                        }
                    }

                    GlobalScope.launch(Dispatchers.IO) {
                        sendTelegramMessage(cnt, isCharging, batteryPercentageNow)
                        publishMqttMessage(mqttTopic, cnt, isCharging, batteryPercentageNow)
                    }

                    cnt++
                    // Schedule the next run after 10 seconds
                    handler.postDelayed(this, 60000)
                }
            }, 1)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service stopped")
        mqttClient.disconnect()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotification(): Notification {
        val notificationText = "Your Foreground Service is running."

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Foreground Service")
            .setContentText(notificationText)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun publishMqttMessage(topic: String, cnt: Int, charging: Boolean, batteryPercentageNow: Int) {
        var isCharging = '0'
        if(charging) isCharging = '1';
        val message = "$isCharging"
        try {
            // Customize the following values based on your settings
            val clientId = "phone_device"
            val brokerUri = "ssl://o126710c.ala.us-east-1.emqxsl.com:8883"

            mqttClient = MqttClient(brokerUri, clientId, MemoryPersistence())

            val connectOptions = MqttConnectOptions()
            connectOptions.userName = "dongtran"
            connectOptions.password = "dongtran".toCharArray()

            mqttClient.connect(connectOptions)

            // Publish data
            val topic = "wirelesscharge/data" // Set the topic you want to publish to

            val message = MqttMessage(message.toByteArray())
            mqttClient.publish(topic, message)
            println("Message published to MQTT topic: $topic")
            mqttClient.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
            println("Failed to publish MQTT message: ${e.message}")
        }
    }

    private fun sendTelegramMessage(cnt: Int, charging: Boolean, batteryPercentageNow: Int) {
        val token = "5868771943:AAFy3Yzhq5sW8BpsF9WxuGPMg-hFEvQkOA8" // Replace YOUR_BOT_TOKEN with your bot's token
        val chatId = "-4051901987" // Replace YOUR_CHAT_ID with the recipient's chat ID
        val message = "Charge optimize - $cnt - $charging - $batteryPercentageNow" // Your message content

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.telegram.org/bot$token/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val telegramAPI = retrofit.create(TelegramAPI::class.java)

        val call = telegramAPI.sendMessage(chatId, message)

        try {
            val response = call.execute()
            if (response.isSuccessful) {
                println("Message sent successfully!")
            } else {
                val errorBody = response.errorBody()?.string()
                println("Failed to send message: $errorBody")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            println("Failed to send message: ${e.message}")
        }
    }

    private fun getBatteryPercentage(context: Context): Int {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            context.registerReceiver(null, ifilter)
        }
        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1

        return (level.toFloat() / scale.toFloat() * 100).toInt()
    }
}

interface TelegramAPI {
    @FormUrlEncoded
    @POST("sendMessage")
    fun sendMessage(
        @Field("chat_id") chatId: String,
        @Field("text") text: String
    ): Call<Unit>
}
