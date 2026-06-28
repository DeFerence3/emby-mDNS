package com.deference.mdns

import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat.getSystemService

class MdnsBroadcaster(context: Context) {

    val nsdManager = (context.getSystemService(Context.NSD_SERVICE) as NsdManager)
    private var registrationListener: NsdManager.RegistrationListener? = null

    fun registerNginxService() {
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = "home-emby"
            serviceType = "_emby._tcp"
            port = 8096
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onRegistrationFailed(arg0: NsdServiceInfo, arg1: Int) {
                Log.i("mDNS", "mDNS onRegistrationFailed---> ${arg0.serviceName}")
            }
            override fun onServiceRegistered(p0: NsdServiceInfo?) {
                Log.i("mDNS", "mDNS Service successfully registered---> ${p0?.serviceName}")
            }
            override fun onServiceUnregistered(arg0: NsdServiceInfo) {
                Log.i("mDNS", "mDNS onServiceUnregistered--> ${arg0.serviceName}")
            }
            override fun onUnregistrationFailed(arg0: NsdServiceInfo, arg1: Int) {
                Log.i("mDNS", "mDNS onUnregistrationFailed---> ${arg0.serviceName}")
            }
        }

        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
    }

    fun stopBroadcasting() {
        registrationListener?.let {
            nsdManager.unregisterService(it)
        }
    }
}
