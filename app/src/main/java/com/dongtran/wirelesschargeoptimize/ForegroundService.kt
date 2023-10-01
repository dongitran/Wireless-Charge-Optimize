package com.dongtran.wirelesschargeoptimize

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.annotation.RequiresApi
import java.util.*
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.PeriodicWorkRequest
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class ForegroundService : Service() {
    private val TAG = "ForegroundService"
    private val timer = Timer()
    private val handler = Handler(Looper.getMainLooper())
    private var isCharging = false
    private var isChargedFull = false
    private val BATTERY_LEVEL_NEED_CHARGE = 30
    private val BATTERY_LEVEL_FULL = 85
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "ForegroundServiceChannel"
        private const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private var isServiceRunning = false
    private var cnt = 0

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")

        if (!isServiceRunning) {
            val notification = createNotification()

            // Check battery
            val batteryPercentageInit = getBatteryPercentage(applicationContext)
            if(batteryPercentageInit < (BATTERY_LEVEL_FULL - 10)) {
                isCharging = true;
            }
            else{
                isChargedFull = true;
            }

            // Bắt đầu dịch vụ Foreground với thông báo
            startForeground(NOTIFICATION_ID, notification)
            isServiceRunning = true

            val periodicWorkRequest = PeriodicWorkRequest.Builder(MyWorker::class.java,
                15, TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
                "myWork",
                ExistingPeriodicWorkPolicy.REPLACE,
                periodicWorkRequest
            )

            /*
            handler.postDelayed(object : Runnable {
                override fun run() {
                    // Gửi API POST ở đây
                    Log.d(TAG, "Sending POST request...")
                    val batteryPercentageNow = getBatteryPercentage(applicationContext)
                    print("batteryPercentage: ")
                    println(batteryPercentageNow)

                    if(isCharging){
                        if(batteryPercentageNow >= BATTERY_LEVEL_FULL){
                            isCharging = false
                            isChargedFull = true
                        }
                    }
                    else if(isChargedFull){
                        if(batteryPercentageNow <= BATTERY_LEVEL_NEED_CHARGE) {
                            isCharging = true
                            isChargedFull = false
                        }
                    }

                    GlobalScope.launch(Dispatchers.IO) {
                        sendTelegramMessage(cnt, isCharging, batteryPercentageNow)
                    }
                    cnt++
                    // Lên lịch tiếp theo sau 10 giây
                    handler.postDelayed(this, 10000)
                }
            }, 10000)
             */
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service stopped")
        timer.cancel()
        wakeLock?.release()
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

        val notification = Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Foreground Service")
            .setContentText(notificationText)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .build()

        return notification
    }

    private fun sendTelegramMessage(@Field("cnt") cnt: Number,@Field("charging") charging: Boolean,@Field("batteryPercentageNow") batteryPercentageNow: Number) {
        val token = "5868771943:AAFy3Yzhq5sW8BpsF9WxuGPMg-hFEvQkOA8" // Thay YOUR_BOT_TOKEN bằng token của bot của bạn
        val chatId = "-4051901987" // Thay YOUR_CHAT_ID bằng chat ID của người nhận
        val message = "Charge optimize! " + cnt.toString() + " - " + charging.toString() + " - " + batteryPercentageNow.toString() // Nội dung tin nhắn của bạn

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.telegram.org/bot$token/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val telegramAPI = retrofit.create(TelegramAPI::class.java)

        val call = telegramAPI.sendMessage(chatId, message)

        try {
            val response = call.execute()
            if (response.isSuccessful) {
                println("Tin nhắn đã được gửi thành công!")
            } else {
                val errorBody = response.errorBody()?.string()
                println("Gửi tin nhắn thất bại: $errorBody")
                //updateNotification("Failed to send message: $errorBody")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            println("Gửi tin nhắn thất bại: ${e.message}")
            //updateNotification("Failed to send message: ${e.message}")
        }
    }
}

class MyWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val batteryPercentageNow = getBatteryPercentage(applicationContext)
        println("doWork")
        print("batteryPercentage: ")
        println(batteryPercentageNow)

        GlobalScope.launch(Dispatchers.IO) {
            sendTelegramMessage()
        }

        return Result.success()
    }

    private fun sendTelegramMessage() {
        val token = "" // Thay YOUR_BOT_TOKEN bằng token của bot của bạn
        val chatId = "" // Thay YOUR_CHAT_ID bằng chat ID của người nhận
        val message = "Charge optimize! " // Nội dung tin nhắn của bạn

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.telegram.org/bot$token/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val telegramAPI = retrofit.create(TelegramAPI::class.java)

        val call = telegramAPI.sendMessage(chatId, message)

        try {
            val response = call.execute()
            if (response.isSuccessful) {
                println("Tin nhắn đã được gửi thành công!")
            } else {
                val errorBody = response.errorBody()?.string()
                println("Gửi tin nhắn thất bại: $errorBody")
                //updateNotification("Failed to send message: $errorBody")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            println("Gửi tin nhắn thất bại: ${e.message}")
            //updateNotification("Failed to send message: ${e.message}")
        }
    }
}


private fun getBatteryPercentage(context: Context): Int {
    val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
        context.registerReceiver(null, ifilter)
    }
    val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1

    val batteryPct = (level.toFloat() / scale.toFloat() * 100).toInt()
    return batteryPct
}

interface TelegramAPI {
    @FormUrlEncoded
    @POST("sendMessage")
    fun sendMessage(
        @Field("chat_id") chatId: String,
        @Field("text") text: String
    ): Call<Unit>
}
