package cu.wilb3r.offlinechat

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import cu.wilb3r.offlinechat.Constants.REQUEST_PERMISSION_CODE
import java.lang.reflect.Method
import kotlin.properties.Delegates

class P2PManagerActivity : AppCompatActivity(), WifiP2pManager.ConnectionInfoListener {
    private val TAG = this::class.java.simpleName
    private lateinit var channel: WifiP2pManager.Channel
    private lateinit var manager: WifiP2pManager
    private val intentFilter = IntentFilter()
    private var isWifiP2pEnabled by Delegates.notNull<Boolean>()
    private val peers = mutableListOf<WifiP2pDevice>()
    private lateinit var user: String

    private val peerListListener = WifiP2pManager.PeerListListener { peerList ->
        val refreshedPeers = peerList.deviceList
        if (refreshedPeers != peers) {
            peers.clear()
            peers.addAll(refreshedPeers)
            Log.i("refreshedPeers: ", refreshedPeers.toString())

            // If an AdapterView is backed by this data, notify it
            // of the change. For instance, if you have a ListView of
            // available peers, trigger an update.
            //(listAdapter as WiFiPeerListAdapter).notifyDataSetChanged()

            // Perform any other updates needed based on the new list of
            // peers connected to the Wi-Fi P2P network.
        }

        if (peers.isEmpty()) {
            Log.d(TAG, "No devices found")
            return@PeerListListener
        }
    }

    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                    // Determine if Wifi P2P mode is enabled or not, alert
                    // the Activity.
                    val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                    isWifiP2pEnabled = state == WifiP2pManager.WIFI_P2P_STATE_ENABLED
                    Log.d(TAG, "isWifiP2pEnabled: $state")
                }
                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    // The peer list has changed! We should probably do something about
                    // that.
                    if (ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        ActivityCompat.requestPermissions(
                            this@P2PManagerActivity,
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                            REQUEST_PERMISSION_CODE
                        )
                        return
                    }
                    manager.requestPeers(channel, peerListListener)
                    Log.d(TAG, "P2P peers changed")
                }
                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    Log.d(TAG, "connection changed")
                    manager.requestConnectionInfo(
                        channel,
                        this@P2PManagerActivity
                    )
                }
                WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
//                (supportFragmentManager.findFragmentById(R.id.frag_list) as DeviceListFragment)
//                    .apply {
//                        updateThisDevice(
//                            intent.getParcelableExtra(
//                                WifiP2pManager.EXTRA_WIFI_P2P_DEVICE)!! as WifiP2pDevice
//                        )
//                    }
//                }
                }
            }

        }
    }

    private val groupInfoListener: WifiP2pManager.GroupInfoListener = WifiP2pManager.GroupInfoListener {
        manager.removeGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                println("removeGroup Success")
            }

            override fun onFailure(p0: Int) {
                println("removeGroup Failure")
            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_p2_p_manager)
        intent.getStringExtra("user")?.let {
            user = it
        }
        addIntentActions()
        manager = (applicationContext.getSystemService(WIFI_P2P_SERVICE) as WifiP2pManager).apply {
            channel = initialize(this@P2PManagerActivity, mainLooper, null)
            if (ActivityCompat.checkSelfPermission(
                    this@P2PManagerActivity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            requestGroupInfo(channel, groupInfoListener)
            renameDevice(this, channel)
            //Start discovering peers
            discoverPeers(channel, object : WifiP2pManager.ActionListener {

                override fun onSuccess() {
                    // Code for when the discovery initiation is successful goes here.
                    // No services have actually been discovered yet, so this method
                    // can often be left blank. Code for peer discovery goes in the
                    // onReceive method, detailed below.
                }

                override fun onFailure(reasonCode: Int) {
                    // Code for when the discovery initiation fails goes here.
                    // Alert the user that something went wrong.
                }
            })
        }
    }

    override fun onConnectionInfoAvailable(p0: WifiP2pInfo?) {
        if (p0 != null) {
            Log.d("ConnectionInfoAvailable", "${p0.groupFormed}")
        }
    }

    private fun addIntentActions() {
        // Indicates a change in the Wi-Fi P2P status.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)

        // Indicates a change in the list of available peers.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)

        // Indicates the state of Wi-Fi P2P connectivity has changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)

        // Indicates this device's details have changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(broadcastReceiver, intentFilter)

    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(broadcastReceiver)
    }

    private fun renameDevice(wifiP2pManager: WifiP2pManager, channel: WifiP2pManager.Channel) {
        try {
            val m: Method = wifiP2pManager.javaClass.getMethod(
                "setDeviceName",
                WifiP2pManager.Channel::class.java,
                String::class.java,
                WifiP2pManager.ActionListener::class.java
            )
            m.invoke(wifiP2pManager, channel, user, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    Log.d(TAG, "Name change successful.")
                }

                override fun onFailure(reason: Int) {
                    Log.d(TAG, "name change failed: $reason")
                }
            })
        } catch (e: Exception) {
            println(e.message)
            Log.d(TAG, "No such method")
        }
    }
}