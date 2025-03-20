package com.example.view360.communication

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import java.net.InetAddress

class Connection(private val context: Context) {
    private val wifiManager: WifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    fun isWifiEnabled(): Boolean {
        return wifiManager.isWifiEnabled
    }

    fun getGatewayIp(): String? {
        return try {
            val dhcpInfo = wifiManager.dhcpInfo
            val serverIp = dhcpInfo.gateway
            InetAddress.getByAddress(
                byteArrayOf(
                    (serverIp and 0xFF).toByte(),
                    (serverIp shr 8 and 0xFF).toByte(),
                    (serverIp shr 16 and 0xFF).toByte(),
                    (serverIp shr 24 and 0xFF).toByte()
                )
            ).hostAddress
        } catch (e: Exception) {
            Log.e("WifiHelper", "Error getting hotspot server IP", e)
            e.toString()
        }
    }

    fun isHotspotEnabled(): Boolean {
        return try {
            val method = wifiManager.javaClass.getDeclaredMethod("isWifiApEnabled")
            method.isAccessible = true
            method.invoke(wifiManager) as Boolean
        } catch (e: Exception) {
            Log.e("HotspotHelper", "Error checking hotspot status", e)
            false
        }
    }
}