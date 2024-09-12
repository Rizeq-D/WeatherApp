package com.rizeq.weatherapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.*
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.rizeq.weatherapp.databinding.ActivityMainBinding
import com.rizeq.weatherapp.models.WeatherResponse
import com.rizeq.weatherapp.network.WeatherService
import com.rizeq.weatherapp.utils.Constants
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var mFusedLocationClient: FusedLocationProviderClient

    private var mProgressDialog: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (!isLocationEnabled()) {
            Toast.makeText(
                this,
                "Your location provider is turned off. Please turn it on.",
                Toast.LENGTH_SHORT
            ).show()

            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        } else {
            Dexter.withContext(this)
                .withPermissions(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
                .withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        report?.let {
                            if (report.areAllPermissionsGranted()) {
                                requestLocationData()
                            }

                            if (report.isAnyPermissionPermanentlyDenied) {
                                Toast.makeText(
                                    this@MainActivity,
                                    "You have denied location permission. Please allow it as it is mandatory.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        permissions: MutableList<PermissionRequest>?,
                        token: PermissionToken?
                    ) {
                        showRationalDialogForPermissions()
                    }
                }).check()
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
    private fun showRationalDialogForPermissions() {
        AlertDialog.Builder(this)
            .setMessage("It Looks like you have turned off permissions required for this feature. It can be enabled under Application Settings")
            .setPositiveButton("GO TO SETTINGS") { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationData() {
        val mLocationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 10000L
        ).build()

        mFusedLocationClient.requestLocationUpdates(
            mLocationRequest, mLocationCallback, Looper.myLooper()
        )
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val mLastLocation: Location? = locationResult.lastLocation
            val latitude = mLastLocation?.latitude
            val longitude = mLastLocation?.longitude

            Log.i("Current Latitude", "$latitude")
            Log.i("Current Longitude", "$longitude")

            if (latitude != null && longitude != null) {
                getLocationWeatherDetails(latitude, longitude)
            }
        }
    }

    private fun getLocationWeatherDetails(latitude: Double, longitude: Double) {

        if (Constants.isNetworkAvailable(this@MainActivity)) {

            showCustomProgressDialog()

            val retrofit: Retrofit = Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val service: WeatherService = retrofit.create(WeatherService::class.java)

            val listCall: Call<WeatherResponse> = service.getWeather(
                latitude, longitude, Constants.METRIC_UNIT, Constants.APP_ID
            )

            listCall.enqueue(object : Callback<WeatherResponse> {
                @SuppressLint("SetTextI18n")
                override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {

                    if (response.isSuccessful) {
                        hideProgressDialog()
                        val weatherList: WeatherResponse? = response.body()
                        if (weatherList != null)
                            setupUI(weatherList)
                        Log.i("WeatherResponse", "Weather data received: $weatherList")
                    } else {
                        when (response.code()) {
                            400 -> Log.e("WeatherResponse", "Error 400: Bad Request")
                            404 -> Log.e("WeatherResponse", "Error 404: Not Found")
                            else -> Log.e("WeatherResponse", "Error: Generic Error")
                        }
                    }
                }

                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                    Log.e("WeatherResponse", "API call failed: ${t.message}", t)
                    hideProgressDialog()
                }
            })
        } else {
            Toast.makeText(
                this@MainActivity,
                "No internet connection available.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    private fun showCustomProgressDialog() {
        mProgressDialog = Dialog(this)
        mProgressDialog!!.setContentView(R.layout.dialog_custom_progress)
        mProgressDialog!!.show()
    }

    private fun hideProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog!!.dismiss()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setupUI(weatherList: WeatherResponse) {

        for (z in weatherList.weather.indices) {
            Log.i("NAME", weatherList.weather[z].main)

            binding.tvMain.text = weatherList.weather[z].main
            binding.tvMainDescription.text = weatherList.weather[z].description
            binding.tvTemp.text =
                weatherList.main.temp.toString() + getUnit(application.resources.configuration.locales.toString())
            binding.tvHumidity.text = weatherList.main.humidity.toString() + " per cent"
            binding.tvMin.text = weatherList.main.tempMin.toString() + " min"
            binding.tvMax.text = weatherList.main.tempMax.toString() + " max"
            binding.tvSpeed.text = weatherList.wind.speed.toString()
            binding.tvName.text = weatherList.name
            binding.tvCountry.text = weatherList.sys.country
            binding.tvSunriseTime.text = unixTime(weatherList.sys.sunrise.toLong())
            binding.tvSunsetTime.text = unixTime(weatherList.sys.sunset.toLong())

            when (weatherList.weather[z].icon) {
                "01d" -> binding.ivMain.setImageResource(R.drawable.sunny)
                "02d" -> binding.ivMain.setImageResource(R.drawable.cloud)
                "03d" -> binding.ivMain.setImageResource(R.drawable.cloud)
                "04d" -> binding.ivMain.setImageResource(R.drawable.cloud)
                "04n" -> binding.ivMain.setImageResource(R.drawable.cloud)
                "10d" -> binding.ivMain.setImageResource(R.drawable.rain)
                "11d" -> binding.ivMain.setImageResource(R.drawable.storm)
                "13d" -> binding.ivMain.setImageResource(R.drawable.snowflake)
                "01n" -> binding.ivMain.setImageResource(R.drawable.cloud)
                "02n" -> binding.ivMain.setImageResource(R.drawable.cloud)
                "03n" -> binding.ivMain.setImageResource(R.drawable.cloud)
                "10n" -> binding.ivMain.setImageResource(R.drawable.cloud)
                "11n" -> binding.ivMain.setImageResource(R.drawable.rain)
                "13n" -> binding.ivMain.setImageResource(R.drawable.snowflake)
            }
        }
    }
    private fun getUnit(locale: String): String {
        return if (locale == "US" || locale == "LR" || locale == "MM") {
            "°F"
        } else {
            "°C"
        }
    }
    private fun unixTime(timex: Long): String? {
        val date = Date(timex * 1000L)
        @SuppressLint("SimpleDateFormat") val sdf =
            SimpleDateFormat("HH:mm:ss")
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(date)
    }
}

