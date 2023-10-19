package com.dongtran.wirelesschargeoptimize

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Handler
import android.os.IBinder
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

class MyForegroundService : Service() {

    private var isServiceRunning = false
    private val handler = Handler()
    private var cnt = 0

    private var isCharging = false
    private var isChargedFull = false
    private val BATTERY_LEVEL_NEED_CHARGE = 25
    private val BATTERY_LEVEL_FULL = 85

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isServiceRunning) {
            val batteryPercentageInit = getBatteryPercentage(applicationContext)
            if (batteryPercentageInit < (BATTERY_LEVEL_FULL - 1)) {
                isCharging = true
            } else {
                isChargedFull = true
            }

            isServiceRunning = true
            startWork()
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun startWork() {
        handler.postDelayed({
            // Thực hiện công việc của dịch vụ ở đây, ví dụ:
            println("MyForegroundService is running")

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


            Thread {
                sendTelegramMessage(cnt, isCharging, batteryPercentageNow)
                publishMqttMessage("wirelesscharge/datax", cnt, isCharging, batteryPercentageNow)
            }.start()
            cnt++

            startWork()
        }, 10000)
    }

    override fun onDestroy() {
        isServiceRunning = false
        super.onDestroy()
    }

    private fun publishMqttMessage(topic: String, cnt: Int, charging: Boolean, batteryPercentageNow: Int) {
        var isCharging = '0'
        if(charging) isCharging = '1';
        val message = "$isCharging"
        try {
            // Customize the following values based on your settings
            val clientId = "phone_device"
            val brokerUri = "ssl://o126710c.ala.us-east-1.emqxsl.com:8883"

            var mqttClient = MqttClient(brokerUri, clientId, MemoryPersistence())

            val connectOptions = MqttConnectOptions()
            connectOptions.userName = "dongtran"
            connectOptions.password = "dongtran".toCharArray()

            mqttClient.connect(connectOptions)

            // Publish data
            val message = MqttMessage(message.toByteArray())
            mqttClient.publish(topic, message)

            val messageDebug = "Charge optimize - $cnt - $charging - $batteryPercentageNow"
            val messageDebugObj = MqttMessage(messageDebug.toByteArray())
            val topicDebug = "wirelesscharge/data_debug"
            mqttClient.publish(topicDebug, messageDebugObj)

            println("Message published to MQTT topic: $topic")
            mqttClient.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
            println("Failed to publish MQTT message: ${e.message}")
        }
    }

    private fun sendTelegramMessage(cnt: Int, charging: Boolean, batteryPercentageNow: Int) {
        try {
            val token = "5868771943:AAFy3Yzhq5sW8BpsF9WxuGPMg-hFEvQkOA8" // Replace YOUR_BOT_TOKEN with your bot's token
            val chatId = "-4051901987" // Replace YOUR_CHAT_ID with the recipient's chat ID
            val message = "AAAA - $cnt - $charging - $batteryPercentageNow" // Your message content

            val retrofit = Retrofit.Builder()
                .baseUrl("https://api.telegram.org/bot$token/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val telegramAPI = retrofit.create(TelegramAPI::class.java)

            val call = telegramAPI.sendMessage(chatId, message)

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
        try{
            val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
                context.registerReceiver(null, ifilter)
            }
            val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1

            return (level.toFloat() / scale.toFloat() * 100).toInt()
        }
        catch(e: Exception){
            e.printStackTrace()
            println("Failed to get battery percentage: ${e.message}")
            return 0;
        }
    }
}
