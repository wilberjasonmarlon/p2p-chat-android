package cu.wilb3r.offlinechat

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import cu.wilb3r.offlinechat.databinding.ActivityMainBinding
import kotlin.properties.Delegates

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val REQUEST_PERMISSION_CODE = 200

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.okbutton.setOnClickListener {
            Intent(this, P2PManagerActivity::class.java).also {
                it.putExtra("user", binding.txtName.text.toString())
                startActivity(it)
                finish()
            }
        }
        val wm = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        wm.setWifiEnabled(true)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_PERMISSION_CODE)
            return
        }
    }
}