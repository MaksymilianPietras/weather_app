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
import android.content.Context
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import fuel.Fuel
import fuel.get
import kotlinx.coroutines.runBlocking
import com.google.gson.Gson

class MainActivity : AppCompatActivity() {
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var viewPagerAdapter: ViewPagerAdapter
    private var configuration: Configuration = Configuration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        configuration.setTemperatureUnit(TemperatureUnit.K)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        if (checkLocationPermission()) {
            setYourLocationData()

        } else {
            requestLocationPermission()
        }

        val fragmentList = listOf(BasicDataFragment(), CitiesFragment())
        viewPagerAdapter = ViewPagerAdapter(fragmentList, supportFragmentManager, lifecycle)
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
                setYourLocationData()
            } else {
                Toast.makeText(
                    this,
                    "Brak zgody na lokalizację!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun setYourLocationData(){
        if (checkLocationPermission()) {
            fusedLocationProviderClient.lastLocation
                .addOnSuccessListener { location ->
                    setLocationDataByCords(location, this, viewPagerAdapter)
                }
        } else {
            requestLocationPermission()
        }
    }

    companion object {
        fun setLocationDataByCords(location: android.location.Location?, context: Context, viewPagerAdapter: ViewPagerAdapter){
            if (location != null) {
                var weatherData: WeatherData
                var apiManager = ApiManager(location.latitude, location.longitude)
                runBlocking {
                    var body = Fuel.get(apiManager.getApiUri()).body
                    weatherData = Gson().fromJson(body, WeatherData::class.java)
                    (viewPagerAdapter.getFragmentAtPosition(0) as BasicDataFragment).setWeatherData(weatherData)
                }

            } else {
                Toast.makeText(
                    context,
                    "Nie można uzyskać aktualnej lokalizacji",
                    Toast.LENGTH_SHORT
                ).show()
            }

        }

        fun setLocationDataByCityName(cityName: String, context: Context, viewPagerAdapter: ViewPagerAdapter){
            if (cityName != "") {
                var weatherData: WeatherData
                val apiManager = ApiManager(cityName)
                runBlocking {
                    val body = Fuel.get(apiManager.getApiUri()).body
                    weatherData = Gson().fromJson(body, WeatherData::class.java)
                    (viewPagerAdapter.getFragmentAtPosition(0) as BasicDataFragment).setWeatherData(weatherData)
                }

            } else {
                Toast.makeText(
                    context,
                    "Nie można uzyskać aktualnej lokalizacji",
                    Toast.LENGTH_SHORT
                ).show()
            }

        }
    }


    inner class ViewPagerAdapter(
        private val fragmentList: List<Fragment>,
        fragmentManager: FragmentManager,
        lifecycle: Lifecycle,
    ) : FragmentStateAdapter(fragmentManager, lifecycle) {

        override fun getItemCount(): Int {
            return fragmentList.size
        }

        override fun createFragment(position: Int): Fragment {
            return fragmentList[position]
        }

        fun getFragmentAtPosition(position: Int): Fragment {
            return fragmentList[position]
        }

        fun setCurrentItem(position: Int){
            findViewById<ViewPager2>(R.id.viewPager).currentItem = position
        }

    }
}