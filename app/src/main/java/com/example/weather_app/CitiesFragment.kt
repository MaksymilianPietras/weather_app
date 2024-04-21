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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.reflect.typeOf


private const val SECONDS_TO_REFRESH_CITY_DATA = 15

class CitiesFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!MainActivity.isNetworkAvailable(requireContext())){
            Toast.makeText(
                requireContext(),
                "Brak połączenia z internetem, nie można zaktualizować danych",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val fileContentJsonList = readCitiesDataFromInternalStorage(requireActivity())
        val citiesDataRefreshTime = fileContentJsonList.map {
            city ->
            getCityNameAndLastUpdateDateFromRow(city)
        }
        val currentTime = ZonedDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)
        citiesDataRefreshTime.forEach {
            singleCityData ->
            val formatter = DateTimeFormatter.ofPattern("HH:mm:ss dd.MM.yyyy")
            val localDateTime = LocalDateTime.parse(singleCityData[1], formatter)
            val cityRefreshTime = ZonedDateTime.of(localDateTime, ZoneOffset.UTC)

            val difference = ChronoUnit.SECONDS.between(currentTime, cityRefreshTime)

            if (difference > SECONDS_TO_REFRESH_CITY_DATA) {
                Toast.makeText(
                    requireContext(),
                    "Zaktualizowano dane o ${singleCityData[0]}",
                    Toast.LENGTH_SHORT
                ).show()

                val weatherData = MainActivity.getLocationDataByCityName(singleCityData[0], requireContext())
                if (weatherData != null) {
                    removeCityFromInternalStorage(singleCityData[0], requireActivity())
                    val zonedDateTime = BasicDataFragment.getTimeForPlace(weatherData)
                    val currentUTCTime = ZonedDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)
                    weatherData.formattedTime = String.format("%02d:%02d:%02d %02d.%02d.%d", zonedDateTime?.hour, zonedDateTime?.minute, zonedDateTime?.second, zonedDateTime?.dayOfMonth, zonedDateTime?.monthValue, zonedDateTime?.year)
                    weatherData.formattedGettingDataTime =  String.format("%02d:%02d:%02d %02d.%02d.%d", currentUTCTime?.hour, currentUTCTime?.minute, currentUTCTime?.second, currentUTCTime?.dayOfMonth, currentUTCTime?.monthValue, currentUTCTime?.year)
                    saveCityDataToInternalStorage(weatherData, requireActivity())
                }
            }
        }


    }

    private fun getCityNameAndLastUpdateDateFromRow(weatherData: WeatherData) =
        listOf(
            weatherData.name,
            weatherData.formattedGettingDataTime
        )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(R.layout.fragment_cities, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val fileContent = readCitiesDataFromInternalStorage(requireActivity())
        if (fileContent.isNotEmpty()){
            val splittedCities = getCitiesNamesFromFileContent(fileContent)

            splittedCities.forEach {cityName ->
                createFavouriteCityBtn(cityName, view)
            }
        }

        requireView().findViewById<ImageButton>(R.id.addCityBtn).setOnClickListener {
            var newCity = requireView().findViewById<EditText>(R.id.cityNameText).text.toString()
            newCity = newCity.trim()
            val cities = readCitiesDataFromInternalStorage(requireActivity())

            if (!fileContainsCity(newCity, cities)){
                val weatherData = MainActivity.getLocationDataByCityName(newCity, requireContext())
                if (weatherData != null){
                    val zonedDateTime = BasicDataFragment.getTimeForPlace(weatherData)
                    val currentUTCTime = ZonedDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)
                    weatherData.formattedTime = String.format("%02d:%02d:%02d %02d.%02d.%d", zonedDateTime?.hour, zonedDateTime?.minute, zonedDateTime?.second, zonedDateTime?.dayOfMonth, zonedDateTime?.monthValue, zonedDateTime?.year)
                    weatherData.formattedGettingDataTime = String.format("%02d:%02d:%02d %02d.%02d.%d", currentUTCTime?.hour, currentUTCTime?.minute, currentUTCTime?.second, currentUTCTime?.dayOfMonth, currentUTCTime?.monthValue, currentUTCTime?.year)
                    saveCityDataToInternalStorage(weatherData, requireActivity())
                    createFavouriteCityBtn(newCity, view)
                    Toast.makeText(
                        context,
                        "Pomyślnie dodano ${weatherData.name} do ulubionych",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            }
        }
    }



    private fun fileContainsCity(cityName: String, cities: List<WeatherData>): Boolean{
        for (city in cities){
            if (city.name == cityName){
                return true
            }
        }
        return false
    }



    private fun createFavouriteCityBtn(cityName: String, view: View) {
        val cityLabel = LinearLayout(requireContext())
        cityLabel.orientation = LinearLayout.HORIZONTAL
        cityLabel.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                resources.getDimensionPixelSize(R.dimen.city_label_height)

            )

        cityLabel.gravity = Gravity.CENTER
        val cityBtn = Button(requireContext())
        cityBtn.text = cityName
        cityBtn.setBackgroundResource(R.drawable.city_btn_background)
        cityBtn.gravity = Gravity.CENTER
        cityBtn.setTextColor(ContextCompat.getColor(requireContext(), R.color.textColor))
        cityBtn.setAutoSizeTextTypeUniformWithConfiguration(resources.getDimension(R.dimen.city_label_text_min_size).toInt(),
            resources.getDimension(R.dimen.city_label_text_max_size).toInt(), 1, TypedValue.COMPLEX_UNIT_SP)
        cityBtn.layoutParams = LinearLayout.LayoutParams(
            0,
            (resources.getDimensionPixelSize(R.dimen.city_label_height) * 0.8).toInt(),
            0.85F
        )


        cityBtn.setOnClickListener {
            Configuration.setTemperatureUnit(TemperatureUnit.K)

            val fileContent = readCitiesDataFromInternalStorage(requireActivity())
            var lastUpdateTimeDifference = 0L

            if (BasicDataFragment.timeCounterSchedulerActive){
                BasicDataFragment.timeCounterSchedulerActive = false
                BasicDataFragment.timeCounterScheduler.shutdown()
            }

            for (city in fileContent){
                val cityData = getCityNameAndLastUpdateDateFromRow(city)
                if (cityData[0] == cityBtn.text){
                    val currentTime = ZonedDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)
                    val formatter = DateTimeFormatter.ofPattern("HH:mm:ss dd.MM.yyyy")
                    val localDateTime = LocalDateTime.parse(cityData[1], formatter)
                    val cityRefreshTime = ZonedDateTime.of(localDateTime, ZoneOffset.UTC)

                    lastUpdateTimeDifference = ChronoUnit.SECONDS.between(cityRefreshTime, currentTime)
                    break
                }

            }
            val adapter = requireActivity().findViewById<ViewPager2>(R.id.viewPager).adapter as MainActivity.ViewPagerAdapter
            adapter.getFragmentAtPosition(0).requireView().findViewById<LinearLayout>(R.id.weatherMainData).removeAllViews()

            if (MainActivity.isNetworkAvailable(requireContext())){

                if (lastUpdateTimeDifference > SECONDS_TO_REFRESH_CITY_DATA){
                    updateCityData(cityName, adapter, true, requireContext(), requireActivity())


                } else {
                    setCityDataFromFileLines(fileContent, cityName, adapter, true)

                }

            } else {
                setCityDataFromFileLines(fileContent, cityName, adapter, false)
                adapter.getFragmentAtPosition(0).requireView().findViewById<TextView>(R.id.city).text =
                    "$cityName (Przestarzałe dane)"
            }
            adapter.setCurrentItem(0)

        }

        val deleteCityBtn = ImageButton(requireContext())
        deleteCityBtn.setBackgroundResource(R.drawable.delete_city_background)
        deleteCityBtn.setImageResource(R.drawable.trash)
        deleteCityBtn.scaleType = ImageView.ScaleType.FIT_XY
        deleteCityBtn.layoutParams = LinearLayout.LayoutParams(
            0,
            resources.getDimensionPixelSize(R.dimen.city_label_height) / 2,
            0.15F
        )

        deleteCityBtn.setPadding(
            resources.getDimensionPixelSize(R.dimen.trash_image_padding_left_right),
            resources.getDimensionPixelSize(R.dimen.trash_image_padding_top_bottom),
            resources.getDimensionPixelSize(R.dimen.trash_image_padding_left_right),
            resources.getDimensionPixelSize(R.dimen.trash_image_padding_top_bottom)
        )

        deleteCityBtn.setOnClickListener {
            val city = cityBtn.text.toString()
            removeCityFromInternalStorage(city, requireActivity())
            view.findViewById<LinearLayout>(R.id.favouriteCitiesLabel).removeView(cityLabel)
        }

        cityLabel.addView(cityBtn)
        cityLabel.addView(deleteCityBtn)
        view.findViewById<LinearLayout>(R.id.favouriteCitiesLabel).addView(cityLabel)
    }



    companion object {
        fun updateCityData(
            cityName: String,
            adapter: MainActivity.ViewPagerAdapter,
            startTimerCounter: Boolean,
            context: Context,
            activity: FragmentActivity
        ) {
            val weatherData =
                MainActivity.setLocationDataByCityName(cityName, context, adapter, startTimerCounter)
            if (weatherData != null) {
                removeCityFromInternalStorage(cityName, activity)
                val zonedDateTime = BasicDataFragment.getTimeForPlace(weatherData)
                val currentUTCTime = ZonedDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)
                weatherData.formattedTime = String.format("%02d:%02d:%02d %02d.%02d.%d", zonedDateTime?.hour, zonedDateTime?.minute, zonedDateTime?.second, zonedDateTime?.dayOfMonth, zonedDateTime?.monthValue, zonedDateTime?.year)
                weatherData.formattedGettingDataTime = String.format("%02d:%02d:%02d %02d.%02d.%d", currentUTCTime?.hour, currentUTCTime?.minute, currentUTCTime?.second, currentUTCTime?.dayOfMonth, currentUTCTime?.monthValue, currentUTCTime?.year)
                saveCityDataToInternalStorage(weatherData, activity)
            }
        }

        fun setCityDataFromFileLines(
            citiesData: List<WeatherData>,
            cityName: String,
            adapter: MainActivity.ViewPagerAdapter,
            timerEnable: Boolean
        ) {
            for (cityData in citiesData) {
                if (cityData.name == cityName) {
                    (adapter.getFragmentAtPosition(0) as BasicDataFragment).setWeatherData(
                        cityData,
                        timerEnable
                    )
                    MainActivity.setAdditionalInfoFragment(adapter, cityData, )
                    break
                }
            }
        }

        fun removeCityFromInternalStorage(city: String, activity: FragmentActivity) {
            var fileContent = readCitiesDataFromInternalStorage(activity)
            fileContent = fileContent.filter { it.name != city }
            val parsedFileContent = Gson().toJson(fileContent)
            val internalStorage = "weather_data.txt"
            val fileOutputStream: FileOutputStream =
                activity.openFileOutput(internalStorage, AppCompatActivity.MODE_PRIVATE)
            fileOutputStream.bufferedWriter().use { it.write(parsedFileContent) }
            fileOutputStream.close()
        }

        fun getCitiesNamesFromFileContent(weathersData: List<WeatherData>): List<String> {
            val splittedCities = ArrayList<String>()
            for (weatherData in weathersData){
                splittedCities.add(weatherData.name)
            }
            return splittedCities
        }


        fun readCitiesDataFromInternalStorage(activity: FragmentActivity): List<WeatherData> {
            val internalStorage = "weather_data.txt"
            val fileContent: String
            var citiesData: List<WeatherData> = ArrayList()
            try {

                val inputStream: FileInputStream = activity.openFileInput(internalStorage)
                fileContent = inputStream.bufferedReader().use { it.readText() }
                val typeToken = object : TypeToken<List<WeatherData>>() {}.type
                if (fileContent != ""){
                    citiesData = Gson().fromJson(fileContent, typeToken)

                }

                inputStream.close()
            } catch (_: FileNotFoundException) {}

            return citiesData
        }

        fun saveCityDataToInternalStorage(
            weatherData: WeatherData,
            activity: FragmentActivity
        ) {

            val citiesData = readCitiesDataFromInternalStorage(activity)
            val internalStorage = "weather_data.txt"
            val outputStream: FileOutputStream = activity.openFileOutput(internalStorage,
                AppCompatActivity.MODE_PRIVATE
            )

            val newFileContent = citiesData.toMutableList()
            newFileContent.add(weatherData)
            val contentToSave = Gson().toJson(newFileContent)

            outputStream.bufferedWriter().use { it.write(contentToSave) }

            outputStream.close()
        }
    }

}

