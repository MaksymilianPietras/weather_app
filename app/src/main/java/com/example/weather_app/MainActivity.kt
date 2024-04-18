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
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import fuel.Fuel
import fuel.get
import kotlinx.coroutines.runBlocking
import com.google.gson.Gson
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

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

        val fragmentList = listOf(BasicDataFragment(), CitiesFragment(), WindFragment())
        viewPagerAdapter = ViewPagerAdapter(fragmentList, supportFragmentManager, lifecycle)
        findViewById<ViewPager2>(R.id.viewPager).adapter = viewPagerAdapter

        //TODO naprawić włączanie timera bo daje niepełne info i sie wiesza
        val fileData = CitiesFragment.readCitiesDataFromInternalStorage(this)
        val citiesNames = CitiesFragment.getCitiesNamesFromFileContent(fileData)
        createGettingFavouriteCityDataRoutine(citiesNames, viewPagerAdapter)

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

    private fun createGettingFavouriteCityDataRoutine(
        citiesNames: List<String>,
        adapter: MainActivity.ViewPagerAdapter,
    ) {
        val scheduler = Executors.newScheduledThreadPool(1)

        val refreshCityDataTask = Runnable {
            if (isNetworkAvailable(this)){
                for (city in citiesNames){
                    CitiesFragment.updateCityData(city, adapter, false, this, this)
                }
                Toast.makeText(
                    this,
                    "Zakutalizowano dane o miastach",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        scheduler.scheduleAtFixedRate(
            refreshCityDataTask, 0,
            1, TimeUnit.SECONDS
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
            val weatherData = getLocationDataByCityCords(location, context)
            if (weatherData != null){
                (viewPagerAdapter.getFragmentAtPosition(0) as BasicDataFragment).setWeatherData(weatherData, true)
                (viewPagerAdapter.getFragmentAtPosition(2) as WindFragment).setLocationAdditionalInfo(weatherData)
            }

        }

        fun setLocationDataByCityName(cityName: String, context: Context, viewPagerAdapter: ViewPagerAdapter, startTimerCounter: Boolean): WeatherData?{
            val weatherData = getLocationDataByCityName(cityName, context)
            if (weatherData != null){
                (viewPagerAdapter.getFragmentAtPosition(0) as BasicDataFragment).setWeatherData(weatherData, startTimerCounter)
                (viewPagerAdapter.getFragmentAtPosition(2) as WindFragment).setLocationAdditionalInfo(weatherData)
            }
            return weatherData
        }

        fun getLocationDataByCityCords(location: android.location.Location?, context: Context): WeatherData?{
            if (location != null) {
                var weatherData: WeatherData
                var apiManager = ApiManager(location.latitude, location.longitude)
                runBlocking {
                    var body = Fuel.get(apiManager.getApiUri()).body
                    weatherData = Gson().fromJson(body, WeatherData::class.java)
                }
                if (weatherData.name == ""){
                        Toast.makeText(
                            context,
                            "Brak danych pogodowych dla podanej lokalizacji!",
                            Toast.LENGTH_SHORT
                        ).show()
                    return null
                }
                return weatherData
            } else {
                Toast.makeText(
                    context,
                    "Nie można uzyskać aktualnej lokalizacji",
                    Toast.LENGTH_SHORT
                ).show()
            }
            return null
        }

        fun getLocationDataByCityName(cityName: String, context: Context): WeatherData?{
            if (cityName != "") {
                var weatherData: WeatherData
                val apiManager = ApiManager(cityName)
                runBlocking {
                    val body = Fuel.get(apiManager.getApiUri()).body
                    weatherData = Gson().fromJson(body, WeatherData::class.java)
                }
                if (weatherData.name == ""){
                    Toast.makeText(
                        context,
                        "Brak danych pogodowych dla podanej lokalizacji!",
                        Toast.LENGTH_SHORT
                    ).show()
                    return null
                }
                return weatherData

            } else {
                Toast.makeText(
                    context,
                    "Nie można uzyskać aktualnej lokalizacji",
                    Toast.LENGTH_SHORT
                ).show()
            }
            return null
        }

        fun isNetworkAvailable(context: Context): Boolean {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
            return networkCapabilities != null &&
                    (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
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