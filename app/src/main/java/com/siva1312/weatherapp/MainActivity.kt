package com.siva1312.weatherapp

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.*
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    lateinit var mainContainer: RelativeLayout
    lateinit var progressBar: ProgressBar
    lateinit var errorImage: ImageView
    lateinit var errorText: TextView
    lateinit var recyclerMain: RecyclerView
    lateinit var recyclerAdapter: HourRecyclerAdapter
    lateinit var layoutManager: RecyclerView.LayoutManager

    private val hourlyPredict = arrayListOf<HourlyPrediction>()
    lateinit var mFusedLocationClient: FusedLocationProviderClient

    private val api_key: String = "YOUR_API_KEY"
    private val PERMISSION_ID = 13
    var lat: String = ""
    var long: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerMain = findViewById(R.id.recyclerMain)
        mainContainer = findViewById(R.id.rlMainContainer)
        progressBar = findViewById(R.id.progressBar)
        errorImage = findViewById(R.id.imgError)
        errorText = findViewById(R.id.txtError)
        mainContainer.visibility = View.GONE
        progressBar.visibility = View.VISIBLE
        errorImage.visibility = View.GONE
        errorText.visibility = View.GONE
        layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        getLastLocation()
    }

    private fun getLastLocation() {
        if (checkPermission()) {
            if (isLocationEnabled()) {
                mFusedLocationClient.lastLocation.addOnCompleteListener(this) { task ->
                    var location: Location? = task.result
                    if (location == null) {
                        getNewLocationData()
                    } else {
                        lat = location.latitude.toString()
                        long = location.longitude.toString()
                        CoroutineScope(IO).launch {
                            getApiResponse2()
                            getApiResponse1()
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Turn on location", Toast.LENGTH_SHORT).show()
                val dialog = AlertDialog.Builder(this@MainActivity)
                dialog.setTitle("location is off")
                dialog.setMessage("Please Enable your Location Service")
                dialog.setPositiveButton("Open Settings") { text, listener ->
                    val settingsIntent =
                        Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS) //implicient intent to open location in settings
                    startActivity(settingsIntent)
                    this@MainActivity.finish() //closes the activity so when user re enters it its again created and the list is refreshed
                }
                dialog.setNegativeButton("cancel") { text, listener ->
                    ActivityCompat.finishAffinity(this@MainActivity) //closes the app(all its running instances is closed and app closes)
                }
                dialog.create()
                dialog.show()
            }
        } else {
            requestPermission()
        }
    }

    @SuppressLint("MissingPermission")
    private fun getNewLocationData() {
        var mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval = 0
        mLocationRequest.fastestInterval = 0
        mLocationRequest.numUpdates = 2

        mFusedLocationClient!!.requestLocationUpdates(
            mLocationRequest, mLocationCallback,
            Looper.myLooper()
        )
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(p0: LocationResult) {
            var mLastLocation: Location = p0.lastLocation
            lat = mLastLocation.latitude.toString()
            long = mLastLocation.longitude.toString()
            CoroutineScope(IO).launch {
                getApiResponse2()
                getApiResponse1()
            }
        }
    }

    private fun checkPermission(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ), PERMISSION_ID
        )
    }

    //this checks the permission result
    //used for debugging the code
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_ID) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("Debug", "You Have Permission")
            }
        }
    }

    private fun isLocationEnabled(): Boolean {
        var locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private suspend fun getApiResponse1() {
        if (checkInternet()) {
            var response1: String?
            try {
                response1 =
                    URL("https://api.openweathermap.org/data/2.5/weather?lat=$lat&lon=$long&units=metric&appid=$api_key").readText(
                        Charsets.UTF_8
                    )
                setValues(response1)
            } catch (e: Exception) {
                errorMessage()
            }

        } else {
            noInternet()
        }
    }

    private suspend fun getApiResponse2() {
        if (checkInternet()) {
            var response2: String?
            try {
                response2 = URL("https://api.openweathermap.org/data/2.5/onecall?lat=$lat&lon=$long&units=metric&appid=$api_key").readText(
                    Charsets.UTF_8
                )
                setRecyclerValues(response2)
            } catch (e: Exception) {
                errorMessage()

            }
        } else {
            noInternet()
        }

    }

    private suspend fun setValues(result1: String?) {
        try {
            val jsonObj = JSONObject(result1)
            val main = jsonObj.getJSONObject("main")
            val sys = jsonObj.getJSONObject("sys")
            val wind = jsonObj.getJSONObject("wind")
            val weather = jsonObj.getJSONArray("weather").getJSONObject(0)
            val updated: Long = jsonObj.getLong("dt")

            val temp = main.getString("temp") + "°C"
            val tempFeels = "Feels like:" + main.getString("feels_like") + "°C"
            val tempMin = main.getString("temp_min") + "°C"
            val tempMax = main.getString("temp_max") + "°C"
            val pressure = main.getString("pressure") + " mb"
            val humidity = main.getString("humidity") + " %"
            val sunrise: Long = sys.getLong("sunrise")
            val sunset: Long = sys.getLong("sunset")
            val windSpeed = wind.getString("speed") + " km/h"
            val weatherDescription = weather.getString("description")
            val icon = weather.getString("icon")

            val address =
                jsonObj.getString("name") + ", " + sys.getString("country")

            withContext(Main) {
                changeBackground(icon)

                //assigning data to views
                findViewById<TextView>(R.id.txtAddress).text = address
                findViewById<TextView>(R.id.txtDateTime).text = SimpleDateFormat(
                    "dd/MM/yyyy hh:mm a",
                    Locale.ENGLISH
                ).format(Date(updated * 1000))
                findViewById<TextView>(R.id.txtStatus).text = weatherDescription.capitalize()
                findViewById<TextView>(R.id.txtTemp).text = temp
                findViewById<TextView>(R.id.txtTempFeels).text = tempFeels
                findViewById<TextView>(R.id.txtMinTemp).text = tempMin
                findViewById<TextView>(R.id.txtMaxTemp).text = tempMax
                findViewById<TextView>(R.id.txtSunrise).text =
                    SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(Date(sunrise * 1000))
                findViewById<TextView>(R.id.txtSunset).text =
                    SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(Date(sunset * 1000))
                findViewById<TextView>(R.id.txtWind).text = windSpeed
                findViewById<TextView>(R.id.txtPressure).text = pressure
                findViewById<TextView>(R.id.txtHumidity).text = humidity

                val imageUri = "http://openweathermap.org/img/wn/$icon@4x.png"
                val imgWeather: ImageView = findViewById(R.id.imgWeather)
                Picasso.get().load(imageUri).error(R.drawable.ic_error)
                    .into(imgWeather)
                mainContainer.visibility = View.VISIBLE
                progressBar.visibility = View.GONE
                errorImage.visibility = View.GONE
                errorText.visibility = View.GONE

            }
        } catch (e: java.lang.Exception) {
            errorMessage()
        }
    }

    private suspend fun setRecyclerValues(result2: String?) {
        try {
            val jsonObj1 = JSONObject(result2)
            val hourly = jsonObj1.getJSONArray("hourly")
            for (i in 0 until 25) {
                val singleHour = hourly.getJSONObject(i)
                val dt = singleHour.getLong("dt")
                val predictTime =
                    SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(Date(dt * 1000))
                val predictIcon =
                    singleHour.getJSONArray("weather").getJSONObject(0).getString("icon")
                val predictTemp = singleHour.getString("temp") + "°C"
                val hourObj = HourlyPrediction(
                    predictTime, predictIcon, predictTemp
                )
                setRecyclerView(hourObj)
            }
        } catch (e: java.lang.Exception){
            errorMessage()
        }
        mainContainer.visibility = View.VISIBLE
        progressBar.visibility = View.GONE
        errorImage.visibility = View.GONE
        errorText.visibility = View.GONE

    }

    private suspend fun setRecyclerView(hourObj: HourlyPrediction) {
        withContext(Main) {
            hourlyPredict.add(hourObj)
            recyclerAdapter = HourRecyclerAdapter(hourlyPredict)
            recyclerMain.itemAnimator = DefaultItemAnimator()
            recyclerMain.adapter = recyclerAdapter
            recyclerMain.layoutManager = layoutManager
        }
    }

    private fun checkInternet(): Boolean{
        val cm =
            applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: NetworkInfo? = cm.activeNetworkInfo

        return activeNetwork?.isConnectedOrConnecting == true
    }

    private suspend fun noInternet() {
        withContext(Main) {
            val dialog = AlertDialog.Builder(this@MainActivity)
            dialog.setTitle("Error")
            dialog.setMessage("Internet Connection is not Found")
            dialog.setPositiveButton("Open Settings") { text, listener ->
                val settingsIntent =
                    Intent(Settings.ACTION_WIRELESS_SETTINGS) //implicient intent to open wireless in settings
                startActivity(settingsIntent)
                this@MainActivity.finish() //closes the activity so when user re enters it its again created and the list is refreshed
            }
            dialog.setNegativeButton("Exit") { text, listener ->
                ActivityCompat.finishAffinity(this@MainActivity) //closes the app(all its running instances is closed and app closes)
            }
            dialog.create()
            dialog.show()
        }
    }

    private suspend fun errorMessage(){
        withContext(Main) {
            mainContainer.visibility = View.GONE
            progressBar.visibility = View.GONE
            errorImage.visibility = View.VISIBLE
            errorText.visibility = View.VISIBLE
        }
    }

    //changes the background based on the weather using icon id
    private fun changeBackground(icon: String) {
        var background: Int
        if (icon == "01d" || icon == "02d" || icon == "03d" || icon == "04d") {
            background = R.drawable.bg_day_clear
        } else if (icon == "09d" || icon == "10d" || icon == "11d" || icon == "13d" || icon == "50d") {
            background = R.drawable.bg_day_cloudy
        } else if (icon == "01n" || icon == "02n" || icon == "03n" || icon == "04n") {
            background = R.drawable.bg_night_clear
        } else {
            background = R.drawable.bg_night_cloudy
        }
        findViewById<RelativeLayout>(R.id.rlMain).setBackgroundResource(background)
        checkApi(background)
    }

    //checks the api level and replaces status bar colour when true
    private fun checkApi(background: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor =
                ContextCompat.getColor(this@MainActivity, android.R.color.transparent)
            window.navigationBarColor =
                ContextCompat.getColor(this@MainActivity, android.R.color.transparent)
            window.setBackgroundDrawableResource(background)
        }
    }

}