package com.example.weather_app


import android.os.Bundle
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime


class BasicDataFragment : Fragment() {
    private var cityName: String = ""
    private var geoCords: String = ""
    private var tem: String = ""
    private var time: String = ""
    private var pressure: String = ""
    private var weatherImgUrl: String = ""
    private var weatherKind: String = ""

    companion object {
        fun getTimeForPlace(weatherData: WeatherData): ZonedDateTime? {
            val date = Instant.now()
            var zonedDateTime = ZonedDateTime.ofInstant(date, ZoneOffset.UTC)
            zonedDateTime = zonedDateTime.plusSeconds(weatherData.timezone.toLong())
            return zonedDateTime
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateUI(view)
        bindTemperatureButton(view)
    }

    private fun bindTemperatureButton(view: View) {
        view.findViewById<Button>(R.id.temperature).setOnClickListener {
            Configuration.setTemperatureUnit(Configuration.getTemperatureUnit().next())
            val currentTemp = view.findViewById<Button>(R.id.temperature).text.toString()

            requireView().findViewById<TextView>(R.id.temperature).text =
                Configuration.getTemperature(
                    currentTemp.substring(0, currentTemp.indexOf("°")).toDouble(),
                    Configuration.getTemperatureUnit().prev()
                )
            val adapter =
                requireActivity().findViewById<ViewPager2>(R.id.viewPager).adapter as MainActivity.ViewPagerAdapter

            adapter.getFragmentAtPosition(FORECAST_FRAGMENT_INDEX).view?.let { it1 ->
                WeatherForecastFragment.switchTemperatureUnit(it1)
            }
        }
    }

    private fun updateUI(view: View) {
        view.findViewById<TextView>(R.id.city).text = cityName
        view.findViewById<TextView>(R.id.geoCords).text = geoCords
        view.findViewById<TextView>(R.id.temperature).text = tem
        view.findViewById<TextView>(R.id.time).text = time
        view.findViewById<TextView>(R.id.pressure).text = pressure
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_basic_data, container, false)
    }

    fun setWeatherData(weatherData: WeatherData) {
        cityName = weatherData.name
        tem = "${
            Configuration.convertTemperatureByLetter(
                weatherData.main.temp,
                "K",
                Configuration.getTemperatureUnit().name
            )
        }°${Configuration.getTemperatureUnit().name}"
        geoCords = "${weatherData.coord.lat}° lat. ${weatherData.coord.lon}° lon."
        pressure = "${weatherData.main.pressure} hPa"
        val zonedDateTime = getTimeForPlace(weatherData)
        time = String.format(
            "%02d:%02d:%02d %02d.%02d.%d",
            zonedDateTime?.hour,
            zonedDateTime?.minute,
            zonedDateTime?.second,
            zonedDateTime?.dayOfMonth,
            zonedDateTime?.monthValue,
            zonedDateTime?.year
        )

        if (view != null) updateUI(requireView())

        val apiManager = ApiManager()
        apiManager.setWeatherUriByCityName(weatherData.weather[0].icon)
        lifecycleScope.launch(Dispatchers.Main) {
            val weatherIcon = ImageView(requireContext())
            Picasso.get()
                .load(apiManager.getWeatherUri())
                .into(weatherIcon)

            val weatherType = TextView(requireContext())
            weatherType.text = weatherData.weather[0].main
            weatherType.gravity = Gravity.CENTER

            weatherType.textSize = resources.getDimension(R.dimen.weather_type_text_size)
            weatherType.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))


            val weatherDataLayout = view?.findViewById<LinearLayout>(R.id.weatherMainData)

            setIconLayout(weatherIcon)
            weatherDataLayout?.removeAllViews()
            weatherDataLayout?.addView(weatherType)
            weatherDataLayout?.addView(weatherIcon)

            setWeatherPaneParams(weatherType)

            weatherImgUrl = apiManager.getWeatherUri()
            weatherKind = weatherType.text.toString()
        }
    }

    private fun setWeatherPaneParams(weatherType: TextView) {
        weatherType.setShadowLayer(
            5.0F, 5.0F, 5.0F,
            ContextCompat.getColor(requireView().context, R.color.black)
        )
    }

    private fun setIconLayout(weatherIcon: ImageView) {
        val widthInPixels = resources.getDimensionPixelSize(R.dimen.weather_img_width)
        val heightInPixels = resources.getDimensionPixelSize(R.dimen.weather_img_height)

        val layoutParams = LayoutParams(widthInPixels, heightInPixels)
        weatherIcon.layoutParams = layoutParams
    }
}