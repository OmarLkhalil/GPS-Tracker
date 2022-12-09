package com.omar.gps_tracker

import android.Manifest
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import com.omar.gps_tracker.base.BaseApplication

class MainActivity : BaseApplication() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if(isGPSAllowed()){
            getUserLocation()
        }
        if(!isGPSAllowed()){
            requestPermission()
        }
    }

    // Register the permissions callback, which handles the user's response to the
    // system permissions dialog. Save the return value, an instance of
    // ActivityResultLauncher. You can use either a val, as shown in this snippet,
    // or a lateinit var in your onAttach() or onCreate() method.

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // Permission is granted. Continue the action or workflow in your
                // app.
            } else {
                // Explain to the user that the feature is unavailable because the
                // feature requires a permission that the user has denied. At the
                // same time, respect the user's decision. Don't link to system
                // settings in an effort to convince the user to change their
                // decision.

                showMessage("we can't get the nearest driver to you," +
                        "to use this feature allow location permission")
            }
        }

    @RequiresApi(Build.VERSION_CODES.M)
    fun requestPermission(){
        Log.i("", "RequestPermission Started")
        if(shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION))
        {
            Log.i("", "Permission is granted")
            // Show Explanation to the user
            // Show dialog

            showMessage(message = "Please enable location permission, " +
            "to get you the nearest drivers"
            , posActionTitle = "yes",
            posAction = {
                dialogInterface, _ ->  dialogInterface.dismiss()
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
                ,
                negActionTitle = "No",
                negAction = {
                        dialogInterface, _ -> dialogInterface.dismiss()
                }
                )
        }
        else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            Log.i("", "Permission isn't granted")
        }
    }

    private fun isGPSAllowed(): Boolean{
        Log.i("", "isGPSAllowed")
        return ContextCompat.checkSelfPermission(this,
            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private val locationCallBack: LocationCallback = object: LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            for (location in result.locations){
                // update UI with location data
                Log.e("Location Updated",""+ location.latitude + location.longitude)
            }
        }
    }
    val locationRequest = LocationRequest.create().apply {
        interval = 10000
        fastestInterval = 5000
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }
    val REQUEST_LOCATION_CODE = 120
    private fun getUserLocation(){
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            fusedLocationClient.requestLocationUpdates(locationRequest,locationCallBack, Looper.getMainLooper())
            Log.i(  "", "Get user Location started") }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException){
                try {
                    exception.startResolutionForResult(this@MainActivity,REQUEST_LOCATION_CODE)
                }
                catch (sendEX: IntentSender.SendIntentException){
                }
            }
    }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == REQUEST_LOCATION_CODE){
            if(resultCode == RESULT_OK)
            {
                getUserLocation()
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        // it may crash your application if it still updating in the background
        fusedLocationClient.removeLocationUpdates(locationCallBack)
    }
}