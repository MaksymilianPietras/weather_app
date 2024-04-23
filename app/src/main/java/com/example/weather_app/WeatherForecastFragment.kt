package com.example.weather_app

import android.content.res.Resources
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
import android.widget.ScrollView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.marginTop
import com.squareup.picasso.Picasso
import kotlinx.coroutines.runBlocking
import kotlin.math.min


class WeatherForecastFragment : Fragment() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (arguments != null){
            val weatherForecast: WeatherForecast? = arguments?.getParcelable("WeatherForecast") as WeatherForecast?
            val apiUri = arguments?.getString("forecastUri")
            if (apiUri != null && weatherForecast != null) {
                setForecastInfo(weatherForecast, view, apiUri)
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

        fun setForecastInfo(weatherForecast: WeatherForecast, view: View, weatherForecastApiUri: String){
            val mainContainer = view.findViewById<LinearLayout>(R.id.mainContainer)
            weatherForecast.list.forEach { element ->
                val forecastDataBlock = LinearLayout(view.context)
                forecastDataBlock.orientation = LinearLayout.VERTICAL

                val layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )

                setForecastDataBlockParams(view, layoutParams, forecastDataBlock)

                val tempRangeSubBlock = createTempRangeSubBlock(view, element, weatherForecastApiUri)
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
            val temp = TextView(view.context)
            temp.text = element.main.temp.toString()
            weatherMainData.addView(temp)
            temp.textSize = view.resources.getDimension(R.dimen.forecast_header_info_text_size)

            val weather = TextView(view.context)
            weather.text = element.weather[0].main
            weather.textSize = view.resources.getDimension(R.dimen.forecast_header_info_text_size)
            weatherMainData.addView(weather)
            return weatherMainData
        }

        private fun createTempRangeSubBlock(
            view: View,
            forecastItem: WeatherForecast.ForecastItem,
            weatherForecastApiUri: String
        ): LinearLayout {
            val tempRangeSubBlock = LinearLayout(view.context)
            tempRangeSubBlock.orientation = LinearLayout.HORIZONTAL
            tempRangeSubBlock.gravity = Gravity.CENTER

            val minTemp = TextView(view.context)
            minTemp.text = forecastItem.main.temp_min.toString()
            minTemp.textSize = view.resources.getDimension(R.dimen.forecast_default_info_text_size)

            tempRangeSubBlock.addView(minTemp)

            runBlocking {
                val imageView = ImageView(view.context)
                Picasso.get()
                    .load(weatherForecastApiUri)
                    .into(imageView)
            }

            val maxTemp = TextView(view.context)
            maxTemp.text = forecastItem.main.temp_max.toString()
            maxTemp.textSize = view.resources.getDimension(R.dimen.forecast_default_info_text_size)
            tempRangeSubBlock.addView(maxTemp)
            return tempRangeSubBlock
        }
    }
}