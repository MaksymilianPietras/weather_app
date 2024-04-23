package com.example.weather_app

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
import androidx.core.view.marginTop
import com.squareup.picasso.Picasso
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime


class WeatherForecastFragment : Fragment() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (arguments != null){
            val weatherForecast: WeatherForecast? = arguments?.getParcelable("WeatherForecast") as WeatherForecast?
            val apiUri = arguments?.getString("forecastUri")
            if (apiUri != null && weatherForecast != null) {
                setForecastInfo(weatherForecast, view)
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
        fun newInstance(weatherForecast: WeatherForecast, forecastApiUri: String): WeatherForecastFragment {
            val fragment = WeatherForecastFragment()
            val args = Bundle().apply {
                putParcelable("WeatherForecast", weatherForecast)
                putString("forecastUri", forecastApiUri)
            }
            fragment.arguments = args
            return fragment
        }

        fun setForecastInfo(weatherForecast: WeatherForecast, view: View){
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

                val tempRangeSubBlock = createDataSubBlock(view, element, weatherForecast.city.timezone)
                val weatherMainData = weatherForecastMainData(view, element)


                forecastDataBlock.addView(tempRangeSubBlock)
                forecastDataBlock.addView(weatherMainData)


                mainContainer.addView(forecastDataBlock)
            }

        }

        private fun setForecastDataBlockParams(
            view: View,
            layoutParams: LinearLayout.LayoutParams,
            forecastDataBlock: LinearLayout
        ) {
            val screenWidth = view.resources.displayMetrics.widthPixels
            val screenHeight = view.resources.displayMetrics.heightPixels

            layoutParams.width = (screenWidth * 0.8).toInt()
            layoutParams.height = (screenHeight * 0.2).toInt()

            val marginTopPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                view.resources.getDimensionPixelSize(R.dimen.forecast_block_margin).toFloat(),
                view.resources.displayMetrics
            ).toInt()

            layoutParams.topMargin = marginTopPx
            forecastDataBlock.layoutParams = layoutParams

            forecastDataBlock.setBackgroundResource(R.drawable.city_btn_background)
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
            apiManager.setWeatherUriByCityName(element.weather[0].icon)

            runBlocking {
                val imageView = ImageView(view.context)
                Picasso.get()
                    .load(apiManager.getWeatherUri())
                    .into(imageView)

                val layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                val screenWidth = view.resources.displayMetrics.widthPixels
                val screenHeight = view.resources.displayMetrics.heightPixels

                layoutParams.width = (screenWidth * 0.7).toInt()
                layoutParams.height = (screenHeight * 0.2).toInt()
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

            var zonedDateTime = convertUnixTimestampToUtc(forecastItem.dt)
            zonedDateTime = zonedDateTime.plusSeconds(timezone)
            val date = TextView(view.context)
            date.text = String.format("Date: %02d.%02d.%d", zonedDateTime.dayOfMonth, zonedDateTime.monthValue, zonedDateTime.year)
            date.textSize = view.resources.getDimension(R.dimen.forecast_default_info_text_size)

            dataSubBlock.addView(date)

            val time = TextView(view.context)
            time.text = String.format("Time: %02d:%02d", zonedDateTime.hour, zonedDateTime.minute)
            time.textSize = view.resources.getDimension(R.dimen.forecast_default_info_text_size)
            dataSubBlock.addView(time)

            val avgTemp = TextView(view.context)
            avgTemp.text = forecastItem.main.temp.toString()
            avgTemp.textSize = view.resources.getDimension(R.dimen.forecast_header_info_text_size)
            dataSubBlock.addView(avgTemp)

            val minTemp = TextView(view.context)
            minTemp.text = "MIN: ${forecastItem.main.temp_min}"
            minTemp.textSize = view.resources.getDimension(R.dimen.forecast_default_info_text_size)
            dataSubBlock.addView(minTemp)

            val maxTemp = TextView(view.context)
            maxTemp.text = "MAX: ${forecastItem.main.temp_max}"
            maxTemp.textSize = view.resources.getDimension(R.dimen.forecast_default_info_text_size)
            dataSubBlock.addView(maxTemp)
            return dataSubBlock
        }

        fun convertUnixTimestampToUtc(unixTimestamp: Long): ZonedDateTime {
            val instant = Instant.ofEpochSecond(unixTimestamp)
            return ZonedDateTime.ofInstant(instant, ZoneId.of("UTC"))
        }
    }
}