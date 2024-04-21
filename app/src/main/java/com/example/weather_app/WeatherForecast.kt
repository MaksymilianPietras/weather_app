package com.example.weather_app

import android.os.Parcel
import android.os.Parcelable


class WeatherForecast() : Parcelable{
    var city = City()
    var list: List<ForecastItem> = listOf()

    constructor(parcel: Parcel) : this() {

    }


    inner class City{
        var name: String = ""
        var timeZone: Int = 0
    }

    inner class ForecastItem{
        var main = Main()
        var weather: List<Weather> = listOf()
        var wind = Wind()
        var pressure: Int = 0
        var humidity: Int = 0
        var visibility: Int = 0
        var dt: Long = 0

    }

    inner class Wind{
        var speed: Double = 0.0
        var deg: Int = 0
    }

    inner class Main{
        var temp: Double = 0.0
        var temp_min: Double = 0.0
        var temp_max: Double = 0.0
        var pressure: Int = 0
        var humidity: Int = 0
        var feels_like: Double = 0.0
    }
    inner class Weather{
        var main: String = ""
        var icon: String = ""
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {

    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<WeatherForecast> {
        override fun createFromParcel(parcel: Parcel): WeatherForecast {
            return WeatherForecast(parcel)
        }

        override fun newArray(size: Int): Array<WeatherForecast?> {
            return arrayOfNulls(size)
        }
    }


}