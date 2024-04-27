package com.example.weather_app


import android.graphics.drawable.ColorDrawable
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit


class BasicDataFragment : Fragment() {
    companion object{

        fun getTimeForPlace(weatherData: WeatherData): ZonedDateTime? {
            val date = Instant.now()
            var zonedDateTime = ZonedDateTime.ofInstant(date, ZoneOffset.UTC)
            zonedDateTime = zonedDateTime.plusSeconds(weatherData.timezone.toLong())
            return zonedDateTime
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<Button>(R.id.temperature).setOnClickListener {
            Configuration.setTemperatureUnit(Configuration.getTemperatureUnit().next())
            val currentTemp = view.findViewById<Button>(R.id.temperature).text.toString()

            requireView().findViewById<TextView>(R.id.temperature).text = Configuration.getTemperature(currentTemp.substring(0, currentTemp.indexOf("°")).toDouble(),
                Configuration.getTemperatureUnit().prev())
            val adapter = requireActivity().findViewById<ViewPager2>(R.id.viewPager).adapter as MainActivity.ViewPagerAdapter

            adapter.getFragmentAtPosition(FORECAST_FRAGMENT_INDEX).view?.let { it1 ->
                WeatherForecastFragment.switchTemperatureUnit(
                    it1
                )
            }

        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.fragment_basic_data, container, false)
    }

    fun setWeatherData(weatherData: WeatherData){
        requireView().findViewById<TextView>(R.id.city).text = weatherData.name
        requireView().findViewById<TextView>(R.id.geoCords).text =
            "${weatherData?.coord?.lat}° lat. ${weatherData?.coord?.lon}° lon."
        val temperature = requireView().findViewById<TextView>(R.id.temperature)
        temperature.text = "${Configuration.convertTemperatureByLetter(weatherData.main.temp, "K", Configuration.getTemperatureUnit().name)}°${Configuration.getTemperatureUnit().name}"
        val zonedDateTime = getTimeForPlace(weatherData)
        requireView().findViewById<TextView>(R.id.time).text = "${zonedDateTime?.hour}:${zonedDateTime?.minute}"


        requireView().findViewById<TextView>(R.id.time).text = String.format("%02d:%02d:%02d %02d.%02d.%d", zonedDateTime?.hour, zonedDateTime?.minute, zonedDateTime?.second, zonedDateTime?.dayOfMonth, zonedDateTime?.monthValue, zonedDateTime?.year)
        requireView().findViewById<TextView>(R.id.pressure).text = "${weatherData?.main?.pressure} hPa"
        val apiManager = ApiManager()
        apiManager.setWeatherUriByCityName(weatherData.weather[0].icon)
        lifecycleScope.launch(Dispatchers.Main) {
            val imageView = ImageView(requireContext())
            Picasso.get()
                .load(apiManager.getWeatherUri())
                .into(imageView)



            val weatherType = TextView(requireContext())
            weatherType.text = weatherData.weather[0].main
            weatherType.gravity = Gravity.CENTER

            weatherType.textSize = resources.getDimension(R.dimen.weather_type_text_size)
            weatherType.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))

            val weatherDataLayout = view?.findViewById<LinearLayout>(R.id.weatherMainData)

            val widthInPixels = resources.getDimensionPixelSize(R.dimen.weather_img_width)
            val heightInPixels = resources.getDimensionPixelSize(R.dimen.weather_img_height)

            val layoutParams = LayoutParams(
                widthInPixels,
                heightInPixels
            )

            imageView.layoutParams = layoutParams
            weatherDataLayout?.removeAllViews()
            weatherDataLayout?.addView(weatherType)
            weatherDataLayout?.addView(imageView)

        }
    }



}