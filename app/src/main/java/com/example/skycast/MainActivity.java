package com.example.skycast;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;


import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.skycast.databinding.ActivityMainBinding;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private final int PERMISSION_CODE = 1;
    String cityNames;
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/";
    private static final String API_KEY = "3dbdf1f7420b694cd16fc911077564cd";
    private static final String PREF_LOCATION_KEY = "pref_location_key";
    private ArrayList<String> savedLocations;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_CODE);
        } else {
            // Permission is granted, try to get the last known location
            Location location = getLocation();
            if (location != null) {
                cityNames = getCityName(location.getLongitude(), location.getLatitude());
                binding.MainCityName.setText(cityNames);
                getWeatherData(cityNames);
            } else {
                // Handle the case where location is not available
                Toast.makeText(this, "Location not available, please enter a city name", Toast.LENGTH_SHORT).show();
            }
        }
        binding.searchIcon.setOnClickListener(v -> {
            String city = binding.editCity.getText().toString().trim();
            if (city.isEmpty()) {
                Toast.makeText(this, "Please enter a city name", Toast.LENGTH_SHORT).show();
            } else {
                cityNames = city;
                getWeatherData(cityNames);

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        savePreferredLocation(cityNames);
                    }
                }, 1000);
            }
        });

        binding.menuIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSavedLocationsDialog();
            }
        });

        // Load saved locations from preferences
        savedLocations = getSavedLocations();


    }

    @Override
    public void onBackPressed() {
        String city = binding.editCity.getText().toString().trim();
        if (!city.isEmpty()) {
            savePreferredLocation(city);
        }
        super.onBackPressed();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission Granted
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
            } else {
                // Permission not granted, close the app or handle accordingly
                Toast.makeText(this, "Please provide the permission", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private Location getLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Handle location permissions are not granted
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_CODE);
            return null;
        }

        Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        return location;
    }

    private String getCityName(double longitude, double latitude) {
        Geocoder gcd1 = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = gcd1.getFromLocation(latitude, longitude, 10);
            if (addresses != null && !addresses.isEmpty()) {
                for (Address adr : addresses) {
                    if (adr != null) {
                        String city = adr.getLocality();
                        if (city != null && !city.isEmpty()) {
                            return city;
                        }
                    }
                }
            } else {
                Log.e("Location", "No address found for the given coordinates");
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("Location", "Error getting city name from coordinates: " + e.getMessage());
        }

        return "Not Found";
    }

    public void getWeatherData(String cityName) {
        String url = BASE_URL + "weather?q=" + cityName + "&appid=" + API_KEY;

        binding.MainCityName.setText(cityName);

        RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        binding.Loading.setVisibility(View.GONE);
                        binding.homeRl.setVisibility(View.VISIBLE);

                        try {
                            // Extract current weather information
                            JSONObject mainObj = response.getJSONObject("main");
                            double currentTemp = mainObj.getDouble("temp");

                            JSONArray weatherArray = response.getJSONArray("weather");
                            JSONObject weatherObj = weatherArray.getJSONObject(0);
                            String weatherDescription = weatherObj.getString("description");

                            JSONObject windObj = response.getJSONObject("wind");
                            double windSpeed = windObj.getDouble("speed");
                            // Extract sunrise and sunset times
                            JSONObject sysObj = response.getJSONObject("sys");
                            long sunriseTimestamp = sysObj.getLong("sunrise") * 1000; // Convert to milliseconds
                            long sunsetTimestamp = sysObj.getLong("sunset") * 1000;   // Convert to milliseconds

                            // Format sunrise and sunset times
                            String sunriseTime = formatTimestampToTime(sunriseTimestamp);
                            String sunsetTime = formatTimestampToTime(sunsetTimestamp);


                            double tempInCelsius1 = currentTemp - 273.15;
                            String temperatureFormatted0 = String.format(Locale.getDefault(), "%.0f°C", tempInCelsius1);
                            String windSpeedFormatted = String.format(Locale.getDefault(), "%.0f km/h", windSpeed);

                            binding.tvtemp.setText(temperatureFormatted0);
                            binding.TvCondition.setText(weatherDescription);
                            binding.Speed.setText(windSpeedFormatted);
                            binding.sunrise.setText(sunriseTime);
                            binding.sunset.setText(sunsetTime);


                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(MainActivity.this, "Error parsing weather data", Toast.LENGTH_SHORT).show();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                String errorMessage;
                if (error.networkResponse != null) {
                    // Check for a specific status code that indicates an issue with the city name
                    if (error.networkResponse.statusCode == 400) {
                        errorMessage = "Invalid city name. Please enter a valid city name.";
                    } else {
                        errorMessage = "Error fetching weather data. Please try again later.";
                    }
                } else {
                    errorMessage = "Network error. Please check your internet connection.";
                }
                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
        requestQueue.add(jsonObjectRequest);
    }

    // Helper method to format timestamp to "hh:mm aa"
    private String formatTimestampToTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm aa", Locale.getDefault());
        Date date = new Date(timestamp);
        return sdf.format(date);
    }

    private void savePreferredLocation(String location) {
        // Check if the location is already saved
        if (savedLocations.contains(location)) {
            Toast.makeText(MainActivity.this, "Location is already saved.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a confirmation dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Save Location");
        builder.setMessage("Do you want to save " + location + "?");

        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Add the location to the list
                savedLocations.add(location);

                // Save the updated list to SharedPreferences
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                SharedPreferences.Editor editor = preferences.edit();
                JSONArray jsonArray = new JSONArray(savedLocations);
                editor.putString(PREF_LOCATION_KEY, jsonArray.toString());
                editor.apply();

                Toast.makeText(MainActivity.this, "Location saved: " + location, Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.show();
    }

    // In your activity
    private void showSavedLocationsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Saved Locations");

        // Convert the list to an array for the dialog
        final CharSequence[] locationsArray = savedLocations.toArray(new CharSequence[savedLocations.size()]);

        builder.setItems(locationsArray, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                String selectedLocation = savedLocations.get(which);
                getWeatherData(selectedLocation);
            }
        });

        builder.show();
    }

    @NonNull
    private ArrayList<String> getSavedLocations() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String savedLocationsString = preferences.getString(PREF_LOCATION_KEY, "");

        ArrayList<String> savedLocations = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(savedLocationsString);

            // Get the latest 5 locations or all if less than 5
            int count = Math.min(jsonArray.length(), 5);
            for (int i = 0; i < count; i++) {
                savedLocations.add(jsonArray.getString(i));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return savedLocations;
    }


}
