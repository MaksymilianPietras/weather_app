package com.example.weather_app


import android.graphics.Color
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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
    companion object{

        fun getTimeForPlace(weatherData: WeatherData): ZonedDateTime? {
            val date = Instant.now()
            var zonedDateTime = ZonedDateTime.ofInstant(date, ZoneOffset.UTC)
            zonedDateTime = zonedDateTime.plusSeconds(weatherData.timezone.toLong())
            return zonedDateTime
        }

        fun newInstance(weatherData: WeatherData?): BasicDataFragment {
            val fragment = BasicDataFragment()
            if (weatherData == null){
                return fragment
            }
            val args = Bundle().apply {
                putString("cityName", weatherData.name)
                putString("geoCords", "${weatherData.coord.lat}° lat. ${weatherData.coord.lon}° lon.")
                putString("tem", "${Configuration.convertTemperatureByLetter(weatherData.main.temp, "K", Configuration.getTemperatureUnit().name)}°${Configuration.getTemperatureUnit().name}")
                val zonedDateTime = getTimeForPlace(weatherData)
                putString("time", String.format("%02d:%02d:%02d %02d.%02d.%d", zonedDateTime?.hour, zonedDateTime?.minute, zonedDateTime?.second, zonedDateTime?.dayOfMonth, zonedDateTime?.monthValue, zonedDateTime?.year))
                putString("pressure", "${weatherData.main.pressure} hPa")
                val apiManager = ApiManager()
                apiManager.setWeatherUriByCityName(weatherData.weather[0].icon)
                putString("weatherImgUrl", apiManager.getWeatherUri())
                putString("weatherKind", weatherData.weather[0].main)
            }
            fragment.cityName = weatherData.name
            fragment.geoCords = "${weatherData.coord.lat}° lat. ${weatherData.coord.lon}° lon."
            fragment.tem = "${Configuration.convertTemperatureByLetter(weatherData.main.temp, "K", Configuration.getTemperatureUnit().name)}°${Configuration.getTemperatureUnit().name}"
            val zonedDateTime = getTimeForPlace(weatherData)
            fragment.time =  String.format("%02d:%02d:%02d %02d.%02d.%d", zonedDateTime?.hour, zonedDateTime?.minute, zonedDateTime?.second, zonedDateTime?.dayOfMonth, zonedDateTime?.monthValue, zonedDateTime?.year)
            fragment.pressure = "${weatherData.main.pressure} hPa"
            val apiManager = ApiManager()
            apiManager.setWeatherUriByCityName(weatherData.weather[0].icon)
            fragment.weatherImgUrl = apiManager.getWeatherUri()
            fragment.weatherKind =  weatherData.weather[0].main



            fragment.arguments = args

            return fragment
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onSaveInstanceState(outState: Bundle) {

        outState.putString("cityName", cityName)
        outState.putString("geoCords", geoCords)
        outState.putString("tem", tem)
        outState.putString("time", time)
        outState.putString("pressure", pressure)
        outState.putString("weatherImgUrl", weatherImgUrl)
        outState.putString("weatherKind", weatherKind)
        super.onSaveInstanceState(outState)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val weatherData = (requireActivity() as MainActivity).weatherData
        setWeatherData(weatherData!!)
        if (savedInstanceState != null) {


            requireView().findViewById<TextView>(R.id.city).text = savedInstanceState.getString("cityName", "")
            cityName = savedInstanceState.getString("cityName", "")

            requireView().findViewById<TextView>(R.id.geoCords).text = savedInstanceState.getString("geoCords", "")
            geoCords = savedInstanceState.getString("geoCords", "")

            requireView().findViewById<TextView>(R.id.temperature).text = savedInstanceState.getString("tem", "")
            tem = savedInstanceState.getString("tem", "")

            requireView().findViewById<TextView>(R.id.time).text = savedInstanceState.getString("time", "")
            time = savedInstanceState.getString("time", "")

            requireView().findViewById<TextView>(R.id.pressure).text = savedInstanceState.getString("pressure", "")
            pressure = savedInstanceState.getString("pressure", "")

            weatherImgUrl = savedInstanceState.getString("weatherImgUrl", "")
            weatherKind = savedInstanceState.getString("weatherKind", "")

            lifecycleScope.launch(Dispatchers.Main){
                val weatherIcon = ImageView(requireContext())
                Picasso.get()
                    .load(weatherImgUrl)
                    .into(weatherIcon)

                setIconLayout(weatherIcon)

                val weatherDataLayout = view.findViewById<LinearLayout>(R.id.weatherMainData)
                weatherDataLayout?.removeAllViews()
                val weatherKindTextView = TextView(requireContext())
                weatherKindTextView.text = weatherKind

                weatherKindTextView.textSize = resources.getDimension(R.dimen.weather_type_text_size)
                weatherKindTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                setWeatherPaneParams(weatherKindTextView)

                weatherDataLayout?.addView(weatherKindTextView)
                weatherDataLayout?.addView(weatherIcon)

            }




        }

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
        cityName = weatherData.name
        requireView().findViewById<TextView>(R.id.geoCords).text =
            "${weatherData?.coord?.lat}° lat. ${weatherData?.coord?.lon}° lon."
        geoCords = "${weatherData.coord.lat}° lat. ${weatherData.coord.lon}° lon."
        val temperature = requireView().findViewById<TextView>(R.id.temperature)
        temperature.text = "${Configuration.convertTemperatureByLetter(weatherData.main.temp, "K", Configuration.getTemperatureUnit().name)}°${Configuration.getTemperatureUnit().name}"
        tem = "${Configuration.convertTemperatureByLetter(weatherData.main.temp, "K", Configuration.getTemperatureUnit().name)}°${Configuration.getTemperatureUnit().name}"
        val zonedDateTime = getTimeForPlace(weatherData)


        requireView().findViewById<TextView>(R.id.time).text = String.format("%02d:%02d:%02d %02d.%02d.%d", zonedDateTime?.hour, zonedDateTime?.minute, zonedDateTime?.second, zonedDateTime?.dayOfMonth, zonedDateTime?.monthValue, zonedDateTime?.year)
        time = String.format("%02d:%02d:%02d %02d.%02d.%d", zonedDateTime?.hour, zonedDateTime?.minute, zonedDateTime?.second, zonedDateTime?.dayOfMonth, zonedDateTime?.monthValue, zonedDateTime?.year)
        requireView().findViewById<TextView>(R.id.pressure).text = "${weatherData?.main?.pressure} hPa"
        pressure = "${weatherData.main.pressure} hPa"
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

    private fun setWeatherPaneParams(
        weatherType: TextView,
    ) {
        weatherType.setShadowLayer(
            5.0F,
            5.0F,
            5.0F,
            ContextCompat.getColor(requireView().context, R.color.black)
        )
    }

    private fun setIconLayout(weatherIcon: ImageView) {
        val widthInPixels = resources.getDimensionPixelSize(R.dimen.weather_img_width)
        val heightInPixels = resources.getDimensionPixelSize(R.dimen.weather_img_height)

        val layoutParams = LayoutParams(
            widthInPixels,
            heightInPixels
        )

        weatherIcon.layoutParams = layoutParams
    }


}