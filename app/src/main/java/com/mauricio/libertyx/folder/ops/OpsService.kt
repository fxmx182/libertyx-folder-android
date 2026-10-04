package com.mauricio.libertyx.folder.ops

import android.app.Notification
import com.mauricio.libertyx.folder.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.mauricio.libertyx.folder.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** Mantém o processo vivo durante cópias longas e mostra o progresso na notificação. */
class OpsService : Service() {
    private val scope = CoroutineScope(Job() + Dispatchers.Main)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(NotificationChannel("ops", getString(R.string.notif_canal_ops), NotificationManager.IMPORTANCE_LOW))
        }
        val n = notificacao(Ops.state.value)
        if (Build.VERSION.SDK_INT >= 29) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        else startForeground(1, n)
        scope.launch {
            var ultimo = 0L
            Ops.state.collectLatest { s ->
                if (s == null || s.terminou) {
                    stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(); return@collectLatest
                }
                val agora = System.currentTimeMillis()
                if (agora - ultimo > 500) { ultimo = agora; nm.notify(1, notificacao(s)) }
            }
        }
        return START_NOT_STICKY
    }

    private fun notificacao(s: OpState?): Notification {
        val abrir = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val b = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, "ops") else @Suppress("DEPRECATION") Notification.Builder(this)
        return b.setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(s?.titulo ?: getString(R.string.app_name))
            .setContentText(s?.atual ?: "")
            .setProgress(1000, ((s?.fracao ?: 0f) * 1000).toInt(), (s?.total ?: 0L) == 0L)
            .setOngoing(true)
            .setContentIntent(abrir)
            .build()
    }

    override fun onDestroy() { scope.coroutineContext[Job]?.cancel(); super.onDestroy() }
}
