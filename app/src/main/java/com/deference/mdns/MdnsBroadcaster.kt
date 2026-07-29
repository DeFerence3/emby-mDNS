package com.deference.mdns

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log

class MdnsBroadcaster(context: Context) {

    private val nsdManager: NsdManager = context.applicationContext.getSystemService(NsdManager::class.java)

    private var registrationListener: NsdManager.RegistrationListener? = null

    @Synchronized
    fun registerNginxService() {
        // Prevent registering the same listener more than once.
        if (registrationListener != null) {
            Log.d(TAG, "mDNS service is already registered or registering")
            return
        }

        val serviceInfo = NsdServiceInfo().apply {
            serviceName = SERVICE_NAME
            serviceType = SERVICE_TYPE
            port = SERVICE_PORT
        }

        val listener = object : NsdManager.RegistrationListener {

            override fun onRegistrationFailed(
                serviceInfo: NsdServiceInfo,
                errorCode: Int
            ) {
                Log.e(
                    TAG,
                    "Registration failed for ${serviceInfo.serviceName}: $errorCode"
                )

                synchronized(this@MdnsBroadcaster) {
                    registrationListener = null
                }
            }

            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                // Android may change the name if another device is using it.
                Log.i(
                    TAG,
                    "Service registered as ${serviceInfo.serviceName}"
                )
            }

            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                Log.i(
                    TAG,
                    "Service unregistered: ${serviceInfo.serviceName}"
                )

                synchronized(this@MdnsBroadcaster) {
                    registrationListener = null
                }
            }

            override fun onUnregistrationFailed(
                serviceInfo: NsdServiceInfo,
                errorCode: Int
            ) {
                Log.e(
                    TAG,
                    "Unregistration failed for ${serviceInfo.serviceName}: $errorCode"
                )
            }
        }

        registrationListener = listener

        try {
            nsdManager.registerService(
                serviceInfo,
                NsdManager.PROTOCOL_DNS_SD,
                listener
            )
        } catch (exception: RuntimeException) {
            registrationListener = null
            Log.e(TAG, "Unable to register mDNS service", exception)
        }
    }

    @Synchronized
    fun stopBroadcasting() {
        val listener = registrationListener ?: return

        try {
            nsdManager.unregisterService(listener)
        } catch (exception: IllegalArgumentException) {
            // The listener may already have been unregistered.
            Log.w(TAG, "mDNS listener was already unregistered", exception)
            registrationListener = null
        } catch (exception: RuntimeException) {
            Log.e(TAG, "Unable to unregister mDNS service", exception)
            registrationListener = null
        }
    }

    companion object {
        private const val TAG = "mDNS"

        private const val SERVICE_NAME = "home-emby"
        private const val SERVICE_TYPE = "_emby._tcp"
        private const val SERVICE_PORT = 8096
    }
}