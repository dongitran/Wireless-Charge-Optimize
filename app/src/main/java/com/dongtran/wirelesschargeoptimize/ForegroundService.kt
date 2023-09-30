package com.dongtran.wirelesschargeoptimize

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
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
class ForegroundService : Service() {
    private val TAG = "ForegroundService"
    private val timer = Timer()
    private val handler = Handler(Looper.getMainLooper())


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

            // Bắt đầu dịch vụ Foreground với thông báo
            startForeground(NOTIFICATION_ID, notification)
            isServiceRunning = true

            handler.postDelayed(object : Runnable {
                override fun run() {
                    // Gửi API POST ở đây
                    Log.d(TAG, "AAA POST request...")
                    GlobalScope.launch(Dispatchers.IO) {
                        sendTelegramMessage(cnt)
                    }
                    cnt++
                    // Lên lịch tiếp theo sau 10 giây
                    handler.postDelayed(this, 10000)
                }
            }, 10000)
        }

        // Lập lịch gửi API POST mỗi 2 phút (120000 milliseconds)
        //timer.scheduleAtFixedRate(object : TimerTask() {
        //    override fun run() {
        //        // Gửi API POST ở đây
        //        Log.d(TAG, "Sending POST request...")
        //        GlobalScope.launch(Dispatchers.IO) {
        //            sendTelegramMessage(cnt)
        //        }
        //        cnt = cnt + 1
        //    }
        //}, 0, 10000) // 120000 milliseconds = 2 phút




        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service stopped")
        timer.cancel()
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

    private fun sendTelegramMessage(@Field("cnt") cnt: Number,) {
        val token = "" // Thay YOUR_BOT_TOKEN bằng token của bot của bạn
        val chatId = "" // Thay YOUR_CHAT_ID bằng chat ID của người nhận
        val message = "Hello from Kotlin! " + cnt.toString() // Nội dung tin nhắn của bạn

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

interface TelegramAPI {
    @FormUrlEncoded
    @POST("sendMessage")
    fun sendMessage(
        @Field("chat_id") chatId: String,
        @Field("text") text: String
    ): Call<Unit>
}
