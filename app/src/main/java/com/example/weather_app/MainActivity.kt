package com.example.weather_app

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.gms.location.LocationServices
import android.Manifest
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import fuel.Fuel
import fuel.get
import kotlinx.coroutines.runBlocking

class MainActivity : AppCompatActivity() {
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        if (checkLocationPermission()) {
            getYourLocationData()
        } else {
            requestLocationPermission()
        }

        val fragmentList = listOf(BasicDataFragment(), CitiesFragment())
        val viewPagerAdapter = ViewPagerAdapter(fragmentList, supportFragmentManager, lifecycle)
        findViewById<ViewPager2>(R.id.viewPager).adapter = viewPagerAdapter
    }

    private fun checkLocationPermission(): Boolean {
        return (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED)
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            1
        )
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getYourLocationData()
            } else {
                Toast.makeText(
                    this,
                    "Brak zgody na lokalizację!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun getYourLocationData() {
        if (checkLocationPermission()) {
            fusedLocationProviderClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        var apiManager = ApiManager(location.latitude, location.longitude)
                        runBlocking {
                            val weather = Fuel.get(apiManager.getApiUri())
                                .body.toString()
                            println(weather)
                        }

                    } else {
                        Toast.makeText(
                            this,
                            "Nie można uzyskać aktualnej lokalizacji",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        } else {
            requestLocationPermission()
        }
    }


    private inner class ViewPagerAdapter(
        private val fragmentList: List<Fragment>,
        fragmentManager: FragmentManager,
        lifecycle: Lifecycle
    ) : FragmentStateAdapter(fragmentManager, lifecycle) {

        override fun getItemCount(): Int {
            return fragmentList.size
        }

        override fun createFragment(position: Int): Fragment {
            return fragmentList[position]
        }
    }
}