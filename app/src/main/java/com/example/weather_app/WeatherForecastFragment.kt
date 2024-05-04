package com.example.weather_app

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.view.marginTop
import com.squareup.picasso.Picasso
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime


private const val DP_FOR_TABLET = 600

class WeatherForecastFragment : Fragment() {

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        //todo z jakiejs przyczyny arguments null
        super.onViewCreated(view, savedInstanceState)
        if (arguments != null) {
            val weatherForecast: WeatherForecast? =
                arguments?.getParcelable("WeatherForecast") as WeatherForecast?
            val apiUri = arguments?.getString("forecastUri")
            if (apiUri != null && weatherForecast != null) {
                setForecastInfo(weatherForecast, view)
            }
        }

        if (!isTablet(view.context)) {
            view.findViewById<ImageView>(R.id.closeForecastExtraInfoBtn).setOnClickListener {
                view.findViewById<ConstraintLayout>(R.id.forecastAdditionalInfo).visibility =
                    View.INVISIBLE
            }
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_weather_forecast, container, false)
    }

    companion object {
        fun newInstance(
            weatherForecast: WeatherForecast?,
            forecastApiUri: String?
        ): WeatherForecastFragment {
            val fragment = WeatherForecastFragment()
            if (weatherForecast == null || forecastApiUri == null) {
                return fragment
            }
            val args = Bundle().apply {
                putParcelable("WeatherForecast", weatherForecast)
                putString("forecastUri", forecastApiUri)
            }
            fragment.arguments = args
            return fragment
        }

        fun setForecastInfo(weatherForecast: WeatherForecast, view: View) {
            val mainContainer = view.findViewById<LinearLayout>(R.id.mainContainer)
            mainContainer.removeAllViews()
            weatherForecast.list.forEach { element ->
                val forecastDataBlock = LinearLayout(view.context)
                forecastDataBlock.orientation = LinearLayout.HORIZONTAL

                val layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )

                setForecastDataBlockParams(view, layoutParams, forecastDataBlock)

                val tempRangeSubBlock =
                    createDataSubBlock(view, element, weatherForecast.city.timezone)

                val weatherMainData = weatherForecastMainData(view, element)

                forecastDataBlock.addView(tempRangeSubBlock)
                forecastDataBlock.addView(weatherMainData)

                if (isTablet(view.context)) {
                    createTabletForecastAdditionalInfoLayout(view, element, forecastDataBlock)
                } else {
                    forecastDataBlock.setOnClickListener {
                        val forecastAdditionalInfoLayout =
                            view.findViewById<ConstraintLayout>(R.id.forecastAdditionalInfo)
                        forecastAdditionalInfoLayout.visibility = View.VISIBLE
                        val dataLayout =
                            forecastAdditionalInfoLayout.findViewById<LinearLayout>(R.id.additionalDataPane)

                        setForecastAdditionalInfo(
                            element,
                            dataLayout.findViewById(R.id.windPower),
                            dataLayout.findViewById(R.id.windDir),
                            dataLayout.findViewById(R.id.humidity),
                            dataLayout.findViewById(R.id.visibility),
                            dataLayout.findViewById(R.id.pressure)
                        )
                    }
                }

                mainContainer.addView(forecastDataBlock)
            }
        }

        private fun createTabletForecastAdditionalInfoLayout(
            view: View,
            element: WeatherForecast.ForecastItem,
            forecastDataBlock: LinearLayout
        ) {
            val forecastAdditionalInfo = LinearLayout(view.context)
            forecastAdditionalInfo.orientation = LinearLayout.VERTICAL
            forecastAdditionalInfo.gravity = Gravity.CENTER

            val layoutParams = LinearLayout.LayoutParams(
                400,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            layoutParams.setMargins(
                0,
                0,
                view.resources.getDimension(R.dimen.forecast_additional_data_right_margin).toInt(),
                0
            )
            forecastAdditionalInfo.layoutParams = layoutParams

            val windPower = createTextView(view)
            val windDir = createTextView(view)
            val humidity = createTextView(view)
            val visibility = createTextView(view)
            val pressure = createTextView(view)

            setForecastAdditionalInfo(element, windPower, windDir, humidity, visibility, pressure)
            forecastAdditionalInfo.addView(windPower)
            forecastAdditionalInfo.addView(windDir)
            forecastAdditionalInfo.addView(humidity)
            forecastAdditionalInfo.addView(visibility)
            forecastAdditionalInfo.addView(pressure)

            forecastDataBlock.addView(forecastAdditionalInfo)
        }

        private fun setForecastAdditionalInfo(
            element: WeatherForecast.ForecastItem,
            windPower: TextView,
            windDir: TextView,
            humidity: TextView,
            visibility: TextView,
            pressure: TextView
        ) {
            windPower.text = "Wind speed: ${element.wind.speed} m/sec"
            windDir.text = "Wind deg: ${element.wind.deg}°"
            humidity.text = "Humidity: ${element.main.humidity}%"
            visibility.text = "Visibility: ${element.visibility} m"
            pressure.text = "Pressure: ${element.main.pressure} hPa"
        }

        private fun createTextView(view: View): TextView {
            val textView = TextView(view.context)
            textView.setTextColor(ContextCompat.getColor(view.context, R.color.white))
            textView.gravity = Gravity.CENTER
            textView.textSize = view.resources.getDimension(R.dimen.forecast_default_info_text_size)
            return textView
        }

        private fun isTablet(context: Context): Boolean {
            val displayMetrics = context.resources.displayMetrics
            val smallestWidthDp = displayMetrics.widthPixels / displayMetrics.density
            return smallestWidthDp >= DP_FOR_TABLET
        }

        fun switchTemperatureUnit(view: View) {
            view.findViewById<LinearLayout>(R.id.mainContainer).children.forEach { child ->
                if (child is LinearLayout) {
                    val avgTemp = child.findViewById<TextView>(R.id.avgTemp)
                    val minTemp = child.findViewById<TextView>(R.id.minTemp)
                    val maxTemp = child.findViewById<TextView>(R.id.maxTemp)

                    avgTemp.text = Configuration.convertTemperatureByLetter(
                        avgTemp.text.substring(0, avgTemp.text.indexOf("°")).toDouble(),
                        avgTemp.text.substring(avgTemp.text.indexOf("°") + 1),
                        Configuration.getTemperatureUnit().name
                    ).toString() + "°${Configuration.getTemperatureUnit().name}"

                    minTemp.text = convertTemperatureFromRange(minTemp, "MIN: ")
                    maxTemp.text = convertTemperatureFromRange(maxTemp, "MAX: ")
                }
            }
        }

        private fun convertTemperatureFromRange(temp: TextView, label: String) =
            label + Configuration.convertTemperatureByLetter(
                temp.text.substring(temp.text.indexOf(" "), temp.text.indexOf("°"))
                    .toDouble(),
                temp.text.substring(temp.text.indexOf("°") + 1),
                Configuration.getTemperatureUnit().name
            ).toString() + "°${Configuration.getTemperatureUnit().name}"

        private fun setForecastDataBlockParams(
            view: View,
            layoutParams: LinearLayout.LayoutParams,
            forecastDataBlock: LinearLayout
        ) {
            val screenWidth = view.resources.displayMetrics.widthPixels
            val screenHeight = view.resources.displayMetrics.heightPixels

            layoutParams.width = (screenWidth * 0.8).toInt()
            if (isTablet(view.context)){
                layoutParams.height = (screenHeight * 0.3).toInt()
            } else {
                layoutParams.height = (screenHeight * 0.2).toInt()
            }


            val marginTopPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                view.resources.getDimensionPixelSize(R.dimen.forecast_block_margin).toFloat(),
                view.resources.displayMetrics
            ).toInt()

            layoutParams.topMargin = marginTopPx
            forecastDataBlock.layoutParams = layoutParams

            forecastDataBlock.setBackgroundResource(R.drawable.additional_info_background)
            forecastDataBlock.gravity = Gravity.CENTER
            forecastDataBlock.marginTop
        }

        private fun weatherForecastMainData(
            view: View,
            element: WeatherForecast.ForecastItem
        ): LinearLayout {
            val weatherMainData = LinearLayout(view.context)
            weatherMainData.orientation = LinearLayout.HORIZONTAL
            weatherMainData.gravity = Gravity.CENTER
            val apiManager = ApiManager()
            apiManager.setWeatherUriByWeatherCode(element.weather[0].icon)

            runBlocking {
                val imageView = ImageView(view.context)
                Picasso.get()
                    .load(apiManager.getWeatherUri())
                    .into(imageView)

                val widthInPixels = view.resources.getDimensionPixelSize(R.dimen.weather_img_width)
                val heightInPixels =
                    view.resources.getDimensionPixelSize(R.dimen.weather_img_height)

                val layoutParams = LinearLayout.LayoutParams(
                    widthInPixels,
                    heightInPixels
                )

                imageView.layoutParams = layoutParams

                imageView.layoutParams = layoutParams
                weatherMainData.addView(imageView)

            }
            return weatherMainData
        }

        private fun createDataSubBlock(
            view: View,
            forecastItem: WeatherForecast.ForecastItem,
            timezone: Long = 0
        ): LinearLayout {
            val dataSubBlock = LinearLayout(view.context)
            dataSubBlock.orientation = LinearLayout.VERTICAL
            dataSubBlock.gravity = Gravity.CENTER
            dataSubBlock.layoutParams
            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            layoutParams.setMargins(
                view.resources.getDimension(R.dimen.forecast_main_data_left_margin).toInt(), 0, 0, 0
            )
            dataSubBlock.layoutParams = layoutParams


            var zonedDateTime = convertUnixTimestampToUtc(forecastItem.dt)
            zonedDateTime = zonedDateTime.plusSeconds(timezone)
            val date = TextView(view.context)
            date.text = String.format(
                "Date: %02d.%02d.%d",
                zonedDateTime.dayOfMonth,
                zonedDateTime.monthValue,
                zonedDateTime.year
            )
            date.textSize = view.resources.getDimension(R.dimen.forecast_default_info_text_size)
            date.setTextColor(ContextCompat.getColor(view.context, R.color.white))

            dataSubBlock.addView(date)

            val time = TextView(view.context)
            time.text = String.format("Time: %02d:%02d", zonedDateTime.hour, zonedDateTime.minute)
            time.textSize = view.resources.getDimension(R.dimen.forecast_default_info_text_size)
            time.setTextColor(ContextCompat.getColor(view.context, R.color.white))
            dataSubBlock.addView(time)

            val avgTemp = TextView(view.context)
            avgTemp.text = Configuration.convertTemperatureByLetter(
                forecastItem.main.temp,
                "K",
                Configuration.getTemperatureUnit().name
            ).toString() + "°${Configuration.getTemperatureUnit().name}"
            avgTemp.textSize = view.resources.getDimension(R.dimen.forecast_header_info_text_size)
            avgTemp.setTextColor(ContextCompat.getColor(view.context, R.color.white))
            avgTemp.id = R.id.avgTemp
            dataSubBlock.addView(avgTemp)

            val minTemp = TextView(view.context)
            minTemp.text = "MIN: ${
                Configuration.convertTemperatureByLetter(
                    forecastItem.main.temp_min,
                    "K",
                    Configuration.getTemperatureUnit().name
                )
            }°${Configuration.getTemperatureUnit().name}"
            minTemp.textSize = view.resources.getDimension(R.dimen.forecast_default_info_text_size)
            minTemp.setTextColor(ContextCompat.getColor(view.context, R.color.white))
            minTemp.id = R.id.minTemp
            dataSubBlock.addView(minTemp)

            val maxTemp = TextView(view.context)
            maxTemp.text = "MAX: ${
                Configuration.convertTemperatureByLetter(
                    forecastItem.main.temp_max,
                    "K",
                    Configuration.getTemperatureUnit().name
                )
            }°${Configuration.getTemperatureUnit().name}"
            maxTemp.textSize = view.resources.getDimension(R.dimen.forecast_default_info_text_size)
            maxTemp.setTextColor(ContextCompat.getColor(view.context, R.color.white))
            maxTemp.id = R.id.maxTemp
            dataSubBlock.tag = "forecastData"
            dataSubBlock.addView(maxTemp)

            return dataSubBlock
        }

        private fun convertUnixTimestampToUtc(unixTimestamp: Long): ZonedDateTime {
            val instant = Instant.ofEpochSecond(unixTimestamp)
            return ZonedDateTime.ofInstant(instant, ZoneId.of("UTC"))
        }
    }

}