package com.example.weather_app

import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

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

        val fileContent = readCitiesDataFromInternalStorage()
        val rows = getRowsFromFileContent(fileContent)
        val citiesDataRefreshTime = rows.map {
            row ->
            val formalizedRow = row.substring(0, row.length - 1)
            getCityNameAndLastUpdateDateFromRow(formalizedRow)
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
                    removeCityFromInternalStorage(singleCityData[0])
                    val zonedDateTime = BasicDataFragment.getTimeForPlace(weatherData)
                    val currentUTCTime = ZonedDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)
                    saveCityDataToInternalStorage(weatherData, String.format("%02d:%02d:%02d %02d.%02d.%d", zonedDateTime?.hour, zonedDateTime?.minute, zonedDateTime?.second, zonedDateTime?.dayOfMonth, zonedDateTime?.monthValue, zonedDateTime?.year),
                        String.format("%02d:%02d:%02d %02d.%02d.%d", currentUTCTime?.hour, currentUTCTime?.minute, currentUTCTime?.second, currentUTCTime?.dayOfMonth, currentUTCTime?.monthValue, currentUTCTime?.year))
                }
            }
        }


    }

    private fun getCityNameAndLastUpdateDateFromRow(formalizedRow: String) =
        listOf(
            formalizedRow.substring(0, formalizedRow.indexOf('|')),
            formalizedRow.substring(formalizedRow.lastIndexOf('|') + 1)
        )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(R.layout.fragment_cities, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val fileContent = readCitiesDataFromInternalStorage()
        if (fileContent != ""){
            var splittedCities = getCitiesNamesFromFileContent(fileContent)
            splittedCities = splittedCities.filter { it.isNotEmpty() }
            splittedCities.forEach {cityName ->
                createFavouriteCityBtn(cityName, view)
            }
        }

        requireView().findViewById<ImageButton>(R.id.addCityBtn).setOnClickListener {
            var newCity = requireView().findViewById<EditText>(R.id.cityNameText).text.toString()
            newCity = newCity.trim()
            val cities = readCitiesDataFromInternalStorage()
            if (!cities.contains("$newCity|")){
                val weatherData = MainActivity.getLocationDataByCityName(newCity, requireContext())
                if (weatherData != null){
                    val zonedDateTime = BasicDataFragment.getTimeForPlace(weatherData)
                    val currentUTCTime = ZonedDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)

                    saveCityDataToInternalStorage(weatherData, String.format("%02d:%02d:%02d %02d.%02d.%d", zonedDateTime?.hour, zonedDateTime?.minute, zonedDateTime?.second, zonedDateTime?.dayOfMonth, zonedDateTime?.monthValue, zonedDateTime?.year),
                        String.format("%02d:%02d:%02d %02d.%02d.%d", currentUTCTime?.hour, currentUTCTime?.minute, currentUTCTime?.second, currentUTCTime?.dayOfMonth, currentUTCTime?.monthValue, currentUTCTime?.year))
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

    private fun getCitiesNamesFromFileContent(fileContent: String): List<String> {
        var splittedCities = getRowsFromFileContent(fileContent)
        splittedCities = splittedCities.map { row ->
            row.substring(0, row.indexOf('|'))
        }
        return splittedCities
    }

    private fun getRowsFromFileContent(fileContent: String): List<String> {
        var splittedCities = fileContent.split("\n")
        splittedCities = splittedCities.dropLast(1)
        return splittedCities
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

            val fileContent = readCitiesDataFromInternalStorage()
            val rows = getRowsFromFileContent(fileContent)
            var lastUpdateTimeDifference = 0L

            if (BasicDataFragment.timeCounterSchedulerActive){
                BasicDataFragment.timeCounterSchedulerActive = false
                BasicDataFragment.timeCounterScheduler.shutdown()
            }

            for (row in rows){
                val formalizedRow = row.substring(0, row.length - 1)
                val cityData = getCityNameAndLastUpdateDateFromRow(formalizedRow)
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
                    val weatherData = MainActivity.setLocationDataByCityName(cityName, requireContext(), adapter)
                    if (weatherData != null){
                        removeCityFromInternalStorage(cityName)
                        val zonedDateTime = BasicDataFragment.getTimeForPlace(weatherData)
                        val currentUTCTime = ZonedDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)
                        saveCityDataToInternalStorage(weatherData, String.format("%02d:%02d:%02d %02d.%02d.%d", zonedDateTime?.hour, zonedDateTime?.minute, zonedDateTime?.second, zonedDateTime?.dayOfMonth, zonedDateTime?.monthValue, zonedDateTime?.year),
                            String.format("%02d:%02d:%02d %02d.%02d.%d", currentUTCTime?.hour, currentUTCTime?.minute, currentUTCTime?.second, currentUTCTime?.dayOfMonth, currentUTCTime?.monthValue, currentUTCTime?.year))
                    }


                } else {
                    setCityDataFromFileLines(rows, cityName, adapter, true)

                }

            } else {
                setCityDataFromFileLines(rows, cityName, adapter, false)
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
            removeCityFromInternalStorage(city)
            view.findViewById<LinearLayout>(R.id.favouriteCitiesLabel).removeView(cityLabel)
        }

        cityLabel.addView(cityBtn)
        cityLabel.addView(deleteCityBtn)
        view.findViewById<LinearLayout>(R.id.favouriteCitiesLabel).addView(cityLabel)
    }

    private fun setCityDataFromFileLines(
        rows: List<String>,
        cityName: String,
        adapter: MainActivity.ViewPagerAdapter,
        timerEnable: Boolean
    ) {
        for (row in rows) {
            val formalizedRow = row.substring(0, row.length - 1)
            val cityDataList = formalizedRow.split('|')
            if (cityDataList[0] == cityName) {
                val weatherData = WeatherData(
                    cityDataList[0],
                    cityDataList[1].toDouble(),
                    cityDataList[2].toDouble(),
                    cityDataList[3].toDouble(),
                    cityDataList[6].toInt(),
                    cityDataList[5].toDouble(),
                    cityDataList[7],
                    cityDataList[8]
                )
                (adapter.getFragmentAtPosition(0) as BasicDataFragment).setWeatherData(
                    weatherData,
                    timerEnable
                )
                break
            }
        }
    }

    private fun removeCityFromInternalStorage(city: String) {
        var fileContent = readCitiesDataFromInternalStorage()
        val row = getCityRowFromFileContent(fileContent, city)
        fileContent = fileContent.replace(row, "")
        val internalStorage = "weather_data.txt"
        val fileOutputStream: FileOutputStream =
            requireActivity().openFileOutput(internalStorage, AppCompatActivity.MODE_PRIVATE)
        fileOutputStream.bufferedWriter().use { it.write(fileContent) }
        fileOutputStream.close()
    }

    private fun getCityRowFromFileContent(fileContent: String, city: String): String {
        var row = fileContent.substring(fileContent.indexOf("$city|"))
        row = row.substring(0, row.indexOf("\n") + 1)
        return row
    }


    private fun readCitiesDataFromInternalStorage(): String {
        val internalStorage = "weather_data.txt"
        var fileContent = ""
        try {

            val inputStream: FileInputStream = requireActivity().openFileInput(internalStorage)
            fileContent = inputStream.bufferedReader().use { it.readText() }
            inputStream.close()
        } catch (_: FileNotFoundException) {}

        return fileContent
    }

    private fun saveCityDataToInternalStorage(
        weatherData: WeatherData,
        formattedTime: String,
        formattedGettingDataTime: String
    ) {
        val internalStorage = "weather_data.txt"
        val outputStream: FileOutputStream = requireActivity().openFileOutput(internalStorage,
            AppCompatActivity.MODE_APPEND
        )
        outputStream.bufferedWriter().use { it.write("${weatherData.name}|${weatherData.coord.lat}|${weatherData.coord.lon}|${weatherData.main.temp}|$formattedTime|${weatherData.main.pressure}|${weatherData.timezone}|${weatherData.weather[0].main}|${weatherData.weather[0].icon}|$formattedGettingDataTime|\n") }

        outputStream.close()
    }

}

