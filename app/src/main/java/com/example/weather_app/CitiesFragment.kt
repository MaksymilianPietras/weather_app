package com.example.weather_app

import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit


class CitiesFragment : Fragment() {

    private var locationManager = LocationManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!MainActivity.isNetworkAvailable(requireContext())) {
            Toast.makeText(
                requireContext(),
                "Brak połączenia z internetem, nie można zaktualizować danych",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val fileContentJsonList = FileManager.readCitiesDataFromInternalStorage(requireActivity())
        val citiesDataRefreshTime = fileContentJsonList.map { city ->
            getCityNameAndLastUpdateDateFromRow(city)
        }
        val currentTime = ZonedDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)
        citiesDataRefreshTime.forEach { singleCityData ->
            val formatter = DateTimeFormatter.ofPattern("HH:mm:ss dd.MM.yyyy")
            val localDateTime = LocalDateTime.parse(singleCityData[1], formatter)
            val cityRefreshTime = ZonedDateTime.of(localDateTime, ZoneOffset.UTC)

            val difference = ChronoUnit.SECONDS.between(currentTime, cityRefreshTime)

            if (difference > SECONDS_TO_REFRESH_CITY_DATA) {
                Toast.makeText(
                    requireContext(), "Zaktualizowano dane o ${singleCityData[0]}",
                    Toast.LENGTH_SHORT
                ).show()

                val weatherData = locationManager.getLocationDataByCityName(singleCityData[0], requireContext())
                if (weatherData != null) {
                    FileManager.removeCityFromInternalStorage(singleCityData[0], requireActivity())
                    val zonedDateTime = BasicDataFragment.getTimeForPlace(weatherData)
                    val currentUTCTime = ZonedDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)
                    weatherData.formattedTime = formatDate(zonedDateTime)
                    weatherData.formattedGettingDataTime = formatDate(currentUTCTime)
                    FileManager.saveCityDataToInternalStorage(weatherData, requireActivity())
                }
            }
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_cities, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val fileContent = FileManager.readCitiesDataFromInternalStorage(requireActivity())
        if (fileContent.isNotEmpty()) {
            val splittedCities = FileManager.getCitiesNamesFromFileContent(fileContent)

            splittedCities.forEach { cityName ->
                createFavouriteCityBtn(cityName, view)
            }
        }

        requireView().findViewById<ImageButton>(R.id.addCityBtn).setOnClickListener {
            var newCity = requireView().findViewById<EditText>(R.id.cityNameText).text.toString()
            newCity = newCity.trim()
            val cities = FileManager.readCitiesDataFromInternalStorage(requireActivity())

            if (!fileContainsCity(newCity, cities)) {
                val weatherData = locationManager.getLocationDataByCityName(newCity, requireContext())
                val weatherForecast = locationManager.getLocationForecastByCityName(newCity, requireContext())
                if (weatherData != null && weatherForecast != null) {
                    updateAndSaveData(weatherData, requireActivity(), weatherForecast)
                    createFavouriteCityBtn(weatherData.name, view)
                    Toast.makeText(context, "Pomyślnie dodano ${weatherData.name} do ulubionych", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    private fun fileContainsCity(cityName: String, cities: List<WeatherData>): Boolean {
        for (city in cities) {
            if (city.name == cityName) {
                return true
            }
        }
        return false
    }

    private fun createFavouriteCityBtn(cityName: String, view: View) {
        val cityLabel = LinearLayout(requireContext())
        cityLabel.orientation = LinearLayout.HORIZONTAL
        val screenWidth = view.resources.displayMetrics.widthPixels
        cityLabel.layoutParams = LinearLayout.LayoutParams(
            (screenWidth * 0.8).toInt(),
            resources.getDimensionPixelSize(R.dimen.city_label_height)
        )

        cityLabel.gravity = Gravity.CENTER
        val cityBtn = Button(requireContext())
        cityBtn.text = cityName
        cityBtn.setBackgroundResource(R.drawable.additional_info_background)
        cityBtn.gravity = Gravity.CENTER
        cityBtn.setTextColor(ContextCompat.getColor(requireContext(), R.color.textColor))
        cityBtn.setAutoSizeTextTypeUniformWithConfiguration(
            resources.getDimension(R.dimen.city_label_text_min_size).toInt(),
            resources.getDimension(R.dimen.city_label_text_max_size).toInt(),
            1,
            TypedValue.COMPLEX_UNIT_SP
        )
        cityBtn.layoutParams = LinearLayout.LayoutParams(
            0,
            (resources.getDimensionPixelSize(R.dimen.city_label_height) * 0.8).toInt(),
            0.85F
        )


        cityBtn.setOnClickListener {
            Configuration.setTemperatureUnit(TemperatureUnit.K)

            val fileContent = FileManager.readCitiesDataFromInternalStorage(requireActivity())
            var lastUpdateTimeDifference = 0L

            for (city in fileContent) {
                val cityData = getCityNameAndLastUpdateDateFromRow(city)
                if (cityData[0].uppercase() == cityBtn.text.toString().uppercase()) {
                    val currentTime = ZonedDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)
                    val formatter = DateTimeFormatter.ofPattern("HH:mm:ss dd.MM.yyyy")
                    val localDateTime = LocalDateTime.parse(cityData[1], formatter)
                    val cityRefreshTime = ZonedDateTime.of(localDateTime, ZoneOffset.UTC)

                    lastUpdateTimeDifference =
                        ChronoUnit.SECONDS.between(cityRefreshTime, currentTime)
                    break
                }

            }
            val adapter =
                requireActivity().findViewById<ViewPager2>(R.id.viewPager).adapter as MainActivity.ViewPagerAdapter

            adapter.setCurrentItem(0)

            if (MainActivity.isNetworkAvailable(requireContext())) {
                if (lastUpdateTimeDifference > SECONDS_TO_REFRESH_CITY_DATA) {
                    updateCityDataForCityBtn(cityName, adapter, requireContext(), requireActivity())

                } else {
                    FileManager.setCityDataFromFileLines(
                        fileContent,
                        cityName,
                        adapter,
                        requireActivity()
                    )
                }
            } else {
                FileManager.setCityDataFromFileLines(
                    fileContent,
                    cityName,
                    adapter,
                    requireActivity()
                )
                adapter.getFragmentAtPosition(0).requireView()
                    .findViewById<TextView>(R.id.city).text =
                    "$cityName (Przestarzałe dane)"
            }
        }

        val deleteCityBtn = ImageButton(requireContext())
        deleteCityBtn.setBackgroundResource(R.drawable.delete_city_background)
        deleteCityBtn.setImageResource(R.drawable.trash)
        deleteCityBtn.scaleType = ImageView.ScaleType.FIT_XY
        val deleteButtonWidthPercent: Float = setWidthByOrientation()
        deleteCityBtn.layoutParams = LinearLayout.LayoutParams(
            0,
            resources.getDimensionPixelSize(R.dimen.city_label_height) / 2,
            deleteButtonWidthPercent
        )

        deleteCityBtn.setPadding(
            resources.getDimensionPixelSize(R.dimen.trash_image_padding_left_right),
            resources.getDimensionPixelSize(R.dimen.trash_image_padding_top_bottom),
            resources.getDimensionPixelSize(R.dimen.trash_image_padding_left_right),
            resources.getDimensionPixelSize(R.dimen.trash_image_padding_top_bottom)
        )

        deleteCityBtn.setOnClickListener {
            val city = cityBtn.text.toString()
            FileManager.removeCityFromInternalStorage(city, requireActivity())
            view.findViewById<LinearLayout>(R.id.favouriteCitiesLabel).removeView(cityLabel)
        }

        cityLabel.addView(cityBtn)
        cityLabel.addView(deleteCityBtn)
        view.findViewById<LinearLayout>(R.id.favouriteCitiesLabel).addView(cityLabel)
    }

    private fun setWidthByOrientation(): Float {
        val deleteButtonWidthPercent: Float =
            if (resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
                0.1F
            } else {
                0.2F
            }
        return deleteButtonWidthPercent
    }

    fun updateCityData(
        cityName: String,
        adapter: MainActivity.ViewPagerAdapter,
        context: Context,
        activity: FragmentActivity,
    ) {
        val weatherData = locationManager.getLocationDataByCityName(cityName, context)
        val weatherForecast = locationManager.getLocationForecastByCityName(cityName, context)
        if (weatherData != null && weatherForecast != null) {
            val basicDataFragment = (adapter.getFragmentAtPosition(0) as BasicDataFragment)
            val currentPickedCity = basicDataFragment.requireView().findViewById<TextView>(R.id.city).text
            if (currentPickedCity == cityName) {
                activity.runOnUiThread {
                    basicDataFragment.setWeatherData(weatherData)
                    MainActivity.setAdditionalInfoFragment(adapter, weatherData)
                    Toast.makeText(context, "Zaktualizowano dane o miastach", Toast.LENGTH_SHORT).show()
                }

                val apiManager = ApiManager()
                apiManager.setForecastUri(cityName)
            }

            updateAndSaveData(weatherData, activity, weatherForecast)
        }
    }

    fun updateCityDataForCityBtn(
        cityName: String,
        adapter: MainActivity.ViewPagerAdapter,
        context: Context,
        activity: FragmentActivity
    ) {
        val weatherData = locationManager.getLocationDataByCityName(cityName, context)
        val weatherForecast = locationManager.getLocationForecastByCityName(cityName, context)
        if (weatherData != null && weatherForecast != null) {
            val basicDataFragment = (adapter.getFragmentAtPosition(0) as BasicDataFragment)
            val forecastFragment = adapter.getFragmentAtPosition(FORECAST_FRAGMENT_INDEX)
            basicDataFragment.setWeatherData(weatherData)
            WeatherForecastFragment.setForecastInfo(weatherForecast, forecastFragment.requireView(), forecastFragment.requireActivity())
            MainActivity.setAdditionalInfoFragment(adapter, weatherData)
            ApiManager().setForecastUri(cityName)
            updateAndSaveData(weatherData, activity, weatherForecast)
        }
    }


    companion object {
        const val SECONDS_TO_REFRESH_CITY_DATA = 10

        fun updateCityDataForCityBtnFromFile(
            cityName: String,
            adapter: MainActivity.ViewPagerAdapter,
            activity: FragmentActivity
        ) {
            val weatherData =
                FileManager.readCitiesDataFromInternalStorageByCityName(activity, cityName)
            val weatherForecast =
                FileManager.readCitiesForecastFromInternalStorageByCityName(activity, cityName)
            val basicDataFragment = (adapter.getFragmentAtPosition(0) as BasicDataFragment)
            val forecastFragment = adapter.getFragmentAtPosition(FORECAST_FRAGMENT_INDEX)
            forecastFragment.lifecycleScope.launch{
                forecastFragment.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED){
                    WeatherForecastFragment.setForecastInfo(weatherForecast, forecastFragment.requireView(), forecastFragment.requireActivity())
                }
            }

            basicDataFragment.setWeatherData(weatherData)
            MainActivity.setAdditionalInfoFragment(adapter, weatherData)
            ApiManager().setForecastUri(cityName)
            updateAndSaveData(weatherData, activity, weatherForecast)
        }

        private fun updateAndSaveData(
            weatherData: WeatherData,
            activity: FragmentActivity,
            weatherForecast: WeatherForecast
        ) {
            val zonedDateTime = BasicDataFragment.getTimeForPlace(weatherData)
            val currentUTCTime = ZonedDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)
            weatherData.formattedTime = formatDate(zonedDateTime)
            weatherData.formattedGettingDataTime = formatDate(currentUTCTime)
            FileManager.saveCityDataToInternalStorage(weatherData, activity)
            FileManager.saveCityForecastToInternalStorage(weatherForecast, activity)
        }

        private fun formatDate(currentUTCTime: ZonedDateTime?) = String.format(
            "%02d:%02d:%02d %02d.%02d.%d",
            currentUTCTime?.hour, currentUTCTime?.minute, currentUTCTime?.second,
            currentUTCTime?.dayOfMonth, currentUTCTime?.monthValue, currentUTCTime?.year
        )

        fun getCityNameAndLastUpdateDateFromRow(weatherData: WeatherData) =
            listOf(
                weatherData.name,
                weatherData.formattedGettingDataTime
            )
    }
}

