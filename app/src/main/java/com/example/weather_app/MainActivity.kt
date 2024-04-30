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
import android.widget.TextView
import androidx.core.app.ActivityCompat

import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.location.FusedLocationProviderClient
import fuel.Fuel
import fuel.get
import kotlinx.coroutines.runBlocking
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.io.FileOutputStream
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

const val ADDITIONAL_INFO_FRAGMENT_INDEX = 2
const val FORECAST_FRAGMENT_INDEX = 3

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

        val fileData = FileManager.readCitiesDataFromInternalStorage(this)
        val weatherForecastData = FileManager.readCitiesForecastFromInternalStorage(this)

        var weatherData: WeatherData? = null
        var weatherForecast: WeatherForecast? = null
        var apiManager: ApiManager? = null



        if (fileData.isNotEmpty() && weatherForecastData.isNotEmpty()){
            val cityData = CitiesFragment.getCityNameAndLastUpdateDateFromRow(fileData[0])

            val currentTime = ZonedDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)
            val formatter = DateTimeFormatter.ofPattern("HH:mm:ss dd.MM.yyyy")

            val localDateTime = LocalDateTime.parse(cityData[1], formatter)
            val cityRefreshTime = ZonedDateTime.of(localDateTime, ZoneOffset.UTC)

            val lastUpdateTimeDifference = ChronoUnit.SECONDS.between(cityRefreshTime, currentTime)

            if (isNetworkAvailable(this) && lastUpdateTimeDifference > CitiesFragment.SECONDS_TO_REFRESH_CITY_DATA){
                weatherData = getLocationDataByCityName(fileData[0].name, this)
                weatherForecast = getLocationForecastByCityName(fileData[0].name, this)
                apiManager = ApiManager()
                apiManager.setForecastUri(fileData[0].name)
                if (weatherData != null && weatherForecast != null){
                    val currentUTCTime = ZonedDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)
                    weatherData.formattedGettingDataTime = String.format("%02d:%02d:%02d %02d.%02d.%d", currentUTCTime?.hour, currentUTCTime?.minute, currentUTCTime?.second, currentUTCTime?.dayOfMonth, currentUTCTime?.monthValue, currentUTCTime?.year)
                    FileManager.saveCityDataToInternalStorage(weatherData, this)
                    weatherForecast.formattedGettingDataTime = String.format("%02d:%02d:%02d %02d.%02d.%d", currentUTCTime?.hour, currentUTCTime?.minute, currentUTCTime?.second, currentUTCTime?.dayOfMonth, currentUTCTime?.monthValue, currentUTCTime?.year)
                    FileManager.saveCityForecastToInternalStorage(weatherForecast, this)
                }

            } else {
                weatherData = FileManager.readCitiesDataFromInternalStorage(this)[0]
                weatherForecast = FileManager.readCitiesForecastFromInternalStorage(this)[0]
                apiManager = ApiManager()
                apiManager.setForecastUri(weatherData.name)
            }

        }


        val fragmentList = mutableListOf(BasicDataFragment(), CitiesFragment(), WindFragment.newInstance(weatherData), WeatherForecastFragment.newInstance(weatherForecast, apiManager?.getForecastUri()))
        viewPagerAdapter = ViewPagerAdapter(fragmentList, supportFragmentManager, lifecycle)
        findViewById<ViewPager2>(R.id.viewPager).adapter = viewPagerAdapter


        val basicDataFragment = (viewPagerAdapter.getFragmentAtPosition(0) as BasicDataFragment)
        if (weatherData != null && weatherForecast != null && apiManager != null) {
            lifecycleScope.launch {
                basicDataFragment.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    basicDataFragment.setWeatherData(weatherData)
                    setAdditionalInfoFragment(viewPagerAdapter, weatherData)
                    setForecastFragment(
                        viewPagerAdapter,
                        weatherForecast,
                        apiManager.getForecastUri()
                    )
                }
            }

        }

        val citiesNames = FileManager.getCitiesNamesFromFileContent(fileData)
        createGettingFavouriteCityDataRoutine(citiesNames, viewPagerAdapter, this, this)

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
            if (location == null){
                return
            }
            val weatherData = getLocationDataByCityCords(location, context)
            val weatherForecast = getLocationForecastByCityCords(location, context)
            if (weatherData != null && weatherForecast != null){

                (viewPagerAdapter.getFragmentAtPosition(0) as BasicDataFragment).setWeatherData(weatherData)
                setAdditionalInfoFragment(viewPagerAdapter, weatherData)
                val apiManager = ApiManager()
                apiManager.setForecastUriByCords(location.latitude, location.longitude)

                setForecastFragment(viewPagerAdapter, weatherForecast, apiManager.getForecastUri())
            }

        }

        fun createGettingFavouriteCityDataRoutine(
            citiesNames: List<String>,
            adapter: MainActivity.ViewPagerAdapter,
            context: Context,
            activity: AppCompatActivity
        ) {
            val scheduler = Executors.newScheduledThreadPool(1)

            val refreshCityDataTask = Runnable {
                if (isNetworkAvailable(context)){
                    for (city in citiesNames){
                        CitiesFragment.updateCityData(city, adapter, context, activity)
                    }
                    activity.runOnUiThread {
                        Toast.makeText(
                            context,
                            "Zaktualizowano dane o miastach",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            scheduler.scheduleAtFixedRate(
                refreshCityDataTask, 0,
                10, TimeUnit.SECONDS
            )
        }


        fun setAdditionalInfoFragment(
            viewPagerAdapter: ViewPagerAdapter,
            weatherData: WeatherData,
        ) {
            if (viewPagerAdapter.itemCount > ADDITIONAL_INFO_FRAGMENT_INDEX) {
                val windFragment = viewPagerAdapter.getFragmentAtPosition(ADDITIONAL_INFO_FRAGMENT_INDEX)

                windFragment.lifecycleScope.launch {
                    windFragment.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                        WindFragment.setLocationAdditionalInfo(weatherData, windFragment.requireView())
                    }
                }
            } else {
                viewPagerAdapter.addFragmentToViewPager(WindFragment.newInstance(weatherData))
            }
        }

        fun setForecastFragment(
            viewPagerAdapter: ViewPagerAdapter,
            weatherForecast: WeatherForecast,
            weatherApiUri: String
        ) {
            if (viewPagerAdapter.itemCount > FORECAST_FRAGMENT_INDEX) {
                val forecastFragment = viewPagerAdapter.getFragmentAtPosition(FORECAST_FRAGMENT_INDEX)
                forecastFragment.lifecycleScope.launch {
                    forecastFragment.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                        WeatherForecastFragment.setForecastInfo(weatherForecast, forecastFragment.requireView())
                    }
                }
            } else {
                viewPagerAdapter.addFragmentToViewPager(WeatherForecastFragment.newInstance(weatherForecast, weatherApiUri))
            }
        }


        private fun getLocationDataByCityCords(location: android.location.Location?, context: Context): WeatherData?{
            if (location != null) {
                var weatherData: WeatherData
                val apiManager = ApiManager(location.latitude, location.longitude)
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


        fun getLocationForecastByCityCords(location: android.location.Location?, context: Context): WeatherForecast?{
            if (location != null) {
                var weatherForecast: WeatherForecast
                val apiManager = ApiManager()
                apiManager.setForecastUriByCords(location.latitude, location.longitude)
                runBlocking {
                    val body = Fuel.get(apiManager.getForecastUri()).body
                    weatherForecast = Gson().fromJson(body, WeatherForecast::class.java)
                }
                if (weatherForecast.city.name == ""){
                    Toast.makeText(
                        context,
                        "Brak danych pogodowych dla podanej lokalizacji!",
                        Toast.LENGTH_SHORT
                    ).show()
                    return null
                }
                return weatherForecast
            } else {
                Toast.makeText(
                    context,
                    "Nie można uzyskać aktualnej lokalizacji",
                    Toast.LENGTH_SHORT
                ).show()
            }
            return null
        }

        fun getLocationForecastByCityName(cityName: String, context: Context): WeatherForecast?{
            if (cityName != "") {
                var weatherForecast: WeatherForecast
                val apiManager = ApiManager()
                apiManager.setForecastUri(cityName)

                runBlocking {
                    val body = Fuel.get(apiManager.getForecastUri()).body
                    weatherForecast = Gson().fromJson(body, WeatherForecast::class.java)
                }
                if (weatherForecast.city.name == ""){
                    Toast.makeText(
                        context,
                        "Brak danych pogodowych dla podanej lokalizacji!",
                        Toast.LENGTH_SHORT
                    ).show()
                    return null
                }
                return weatherForecast

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
        private val fragmentList: MutableList<Fragment>,
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

        fun addFragmentToViewPager(fragment: Fragment){
            fragmentList.add(fragment);
        }




    }
}