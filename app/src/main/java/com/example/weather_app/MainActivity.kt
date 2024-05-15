package com.example.weather_app

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import kotlinx.coroutines.launch
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
const val DP_FOR_TABLET = 600

class MainActivity : AppCompatActivity() {
    private var viewPagerAdapter: ViewPagerAdapter? = null
    private var configuration: Configuration = Configuration
    private var locationManager = LocationManager()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        configuration.setTemperatureUnit(TemperatureUnit.K)


        val fileData = FileManager.readCitiesDataFromInternalStorage(this)
        val weatherForecastData = FileManager.readCitiesForecastFromInternalStorage(this)

        var weatherData: WeatherData? = null
        var weatherForecast: WeatherForecast? = null
        val apiManager = ApiManager()


        if (fileData.isNotEmpty() && weatherForecastData.isNotEmpty()) {
            val cityData = CitiesFragment.getCityNameAndLastUpdateDateFromRow(fileData[0])

            val currentTime = ZonedDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)
            val formatter = DateTimeFormatter.ofPattern("HH:mm:ss dd.MM.yyyy")

            val localDateTime = LocalDateTime.parse(cityData[1], formatter)
            val cityRefreshTime = ZonedDateTime.of(localDateTime, ZoneOffset.UTC)

            val lastUpdateTimeDifference = ChronoUnit.SECONDS.between(cityRefreshTime, currentTime)

            if (isNetworkAvailable(this) && lastUpdateTimeDifference > CitiesFragment.SECONDS_TO_REFRESH_CITY_DATA) {
                weatherData = locationManager.getLocationDataByCityName(fileData[0].name, this)
                weatherForecast = locationManager.getLocationForecastByCityName(fileData[0].name, this)
                apiManager.setForecastUri(fileData[0].name)
                if (weatherData != null && weatherForecast != null) {
                    val currentUTCTime = ZonedDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)
                    weatherData.formattedGettingDataTime = String.format(
                        "%02d:%02d:%02d %02d.%02d.%d",
                        currentUTCTime?.hour,
                        currentUTCTime?.minute,
                        currentUTCTime?.second,
                        currentUTCTime?.dayOfMonth,
                        currentUTCTime?.monthValue,
                        currentUTCTime?.year
                    )
                    FileManager.saveCityDataToInternalStorage(weatherData, this)
                    weatherForecast.formattedGettingDataTime = String.format(
                        "%02d:%02d:%02d %02d.%02d.%d",
                        currentUTCTime?.hour,
                        currentUTCTime?.minute,
                        currentUTCTime?.second,
                        currentUTCTime?.dayOfMonth,
                        currentUTCTime?.monthValue,
                        currentUTCTime?.year
                    )
                    FileManager.saveCityForecastToInternalStorage(weatherForecast, this)
                }
            } else {
                weatherData = FileManager.readCitiesDataFromInternalStorage(this)[0]
                weatherForecast = FileManager.readCitiesForecastFromInternalStorage(this)[0]
                apiManager.setForecastUri(weatherData.name)
            }
        }
        val initialized = Configuration.fragments != null

        initializeFragments(initialized, weatherData, weatherForecast, apiManager)
        if (!isTablet(this)){
            addFragmentsAdapterToPager()
        } else {
            val fragmentTransaction = supportFragmentManager.beginTransaction()
            for ((index, fragment) in Configuration.fragments?.withIndex()!!) {
                val fragmentContainerId = when (index) {
                    0 -> R.id.fragment1Container
                    1 -> R.id.fragment2Container
                    2 -> R.id.fragment3Container
                    3 -> R.id.fragment4Container
                    else -> -1
                }

                if (fragmentContainerId != -1) {
                    fragmentTransaction.replace(fragmentContainerId, fragment)
                }
            }
            fragmentTransaction.commit()
        }

        launchTasks(initialized, weatherData, weatherForecast, fileData)
    }

    private fun addFragmentsAdapterToPager() {
        viewPagerAdapter = ViewPagerAdapter(Configuration.fragments!!, supportFragmentManager, lifecycle)
        findViewById<ViewPager2>(R.id.viewPager).adapter = viewPagerAdapter
    }

    private fun launchTasks(
        initialized: Boolean,
        weatherData: WeatherData?,
        weatherForecast: WeatherForecast?,
        fileData: List<WeatherData>,
    ) {
        if (initialized) return
        val basicDataFragment = (Configuration.fragments?.get(0) as BasicDataFragment)
        val citiesNames = FileManager.getCitiesNamesFromFileContent(fileData)
        if (weatherData != null && weatherForecast != null) {
            basicDataFragment.setWeatherData(weatherData)
            setAdditionalInfoFragment(viewPagerAdapter, weatherData)
        }

        createGettingFavouriteCityDataRoutine(citiesNames, viewPagerAdapter, this, this)
    }

    private fun initializeFragments(
        initialized: Boolean,
        weatherData: WeatherData?,
        weatherForecast: WeatherForecast?,
        apiManager: ApiManager
    ) {
        if (initialized) return
        Configuration.fragments = mutableListOf(
            BasicDataFragment(),
            CitiesFragment(),
            WindFragment.newInstance(weatherData),
            WeatherForecastFragment.newInstance(weatherForecast, apiManager.getForecastUri())
        )
        for (fragment in Configuration.fragments!!) {
            fragment.retainInstance = true
        }
    }


    companion object {

        fun isTablet(context: Context): Boolean {
            val displayMetrics = context.resources.displayMetrics
            val smallestWidthDp = displayMetrics.widthPixels / displayMetrics.density
            return smallestWidthDp >= DP_FOR_TABLET
        }

        fun createGettingFavouriteCityDataRoutine(
            citiesNames: List<String>,
            adapter: MainActivity.ViewPagerAdapter?,
            context: Context,
            activity: FragmentActivity
        ) {
            if (Configuration.isRefreshDataRoutineRunning){
                Configuration.scheduledExecutorService.shutdown()
            }
            Configuration.isRefreshDataRoutineRunning = true
            Configuration.scheduledExecutorService = Executors.newScheduledThreadPool(1)

            val refreshCityDataTask = Runnable {
                if (isNetworkAvailable(context)) {
                    val citiesFragment =Configuration.fragments?.get(1) as CitiesFragment
                    val basicDataFragment = (Configuration.fragments?.get(0) as BasicDataFragment)
                    if (basicDataFragment.isAdded){
                        for (city in citiesNames) {
                            citiesFragment.updateCityData(city, adapter, context, activity)
                        }
                    }

                }
            }

            Configuration.scheduledExecutorService.scheduleAtFixedRate(refreshCityDataTask, 10, 10, TimeUnit.SECONDS)
        }


        fun setAdditionalInfoFragment(
            viewPagerAdapter: ViewPagerAdapter?,
            weatherData: WeatherData,
        ) {
            if (Configuration.fragments?.size!! > ADDITIONAL_INFO_FRAGMENT_INDEX) {
                val windFragment =
                    Configuration.fragments?.get(ADDITIONAL_INFO_FRAGMENT_INDEX)

                windFragment?.lifecycleScope?.launch {
                    windFragment.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                        WindFragment.setLocationAdditionalInfo(
                            weatherData, windFragment.requireView()
                        )
                    }
                }
            } else {
                viewPagerAdapter?.addFragmentToViewPager(WindFragment.newInstance(weatherData))
            }
        }


        fun isNetworkAvailable(context: Context): Boolean {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
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

        fun setCurrentItem(position: Int) {
            findViewById<ViewPager2>(R.id.viewPager).currentItem = position
        }

        fun addFragmentToViewPager(fragment: Fragment) {
            fragmentList.add(fragment);
        }
    }
}