package com.example.weather_app

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button


class FavouriteCityFragment : Fragment() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val cityBtn = view.findViewById<Button>(R.id.favouriteCityBtn)
        cityBtn.text = arguments?.getString("cityName")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_favourite_city, container, false)
    }

    companion object {

        fun newInstance(cityName: String): FavouriteCityFragment {
            val fragment = FavouriteCityFragment()
            fragment.arguments = Bundle().apply {putString("cityName", cityName)}
            return fragment
        }
    }
}