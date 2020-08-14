package android.example.myfoodrecords.activities;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.example.myfoodrecords.R;
import android.example.myfoodrecords.RealmHelper;
import android.example.myfoodrecords.model.Food;
import android.example.myfoodrecords.model.PlaceModel;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

import io.realm.Realm;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap map;
    private Realm realm;
    private RealmHelper helper;
    private Food food;
    private PlaceModel placeModel = new PlaceModel();

    private Button saveButton;
    private boolean locationPermissionGranted;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 8;
    private static final String TAG = "MapsActivityTag";
    public static LatLng latLng;
    int requestCode;
    private int foodId = 0;
    private int placePrimaryKey = 0;

    public static final String INTENT_PUT = "placePrimaryKey";

    //TODO Carmera view looks blurry. Search for solution

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        setupRealm();
        setupUI();
    }

    private void setupRealm() {
        realm = Realm.getDefaultInstance();
        helper = new RealmHelper(realm);
        helper.selectPlaceFromDb();
    }

    private void setupUI() {
        saveButton = findViewById(R.id.map_save_button);
        requestCode = getIntent().getExtras().getInt("requestCode");
        foodId = getIntent().getExtras().getInt("foodId");
        if (requestCode == 0) {
            loadMapLocationFromRealm();
        }

        setupAutoComplete();
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        saveButtonClickEvent();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        getLocationPermission();
        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        map.addMarker(new MarkerOptions().position(sydney));
        map.moveCamera(CameraUpdateFactory.newLatLng(sydney));
        updateLocationUI();
        setMapClickListener();
        setMyLocationListener();
    }

    private void setupAutoComplete() {
        // Initialize the SDK
        Places.initialize(getApplicationContext(), getString(R.string.google_place_key));

        // Create a new PlacesClient instance
        PlacesClient placesClient = Places.createClient(this);

        // Initialize the AutocompleteSupportFragment.
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        // Specify the types of place data to return.
        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.TYPES, Place.Field.ADDRESS, Place.Field.RATING));

        // Set up a PlaceSelectionListener to handle the response.
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NotNull Place place) {
                LatLng selectedPlaceLagLng = place.getLatLng();
                addMarkerAndMoveCamera(selectedPlaceLagLng);
                if (place.getId() != null) {
                    placeModel.setPlaceId(place.getId());
                }
                if (place.getName() != null) {
                    placeModel.setPlaceName(place.getName());
                }
                if (place.getLatLng() != null) {
                    placeModel.setLatLng(place.getLatLng().toString());
                }
                if (place.getAddress() != null) {
                    placeModel.setAddress(place.getAddress());
                }
                if (place.getRating() != null) {
                    placeModel.setPlaceRating(place.getRating().floatValue());
                }
            }

            @Override
            public void onError(@NotNull Status status) {
                Log.i(TAG, "An error occurred: " + status);
            }
        });

    }

    //TODO Load Map from saved location called from DetailActivity
    private void loadMapLocationFromRealm() {
        saveButton.setVisibility(View.GONE);
        placeModel = realm.where(PlaceModel.class)
                .equalTo("id", foodId)
                .findFirst();
        if(placeModel != null) {
            placePrimaryKey = placeModel.getId();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        locationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    locationPermissionGranted = true;
                }
            }
        }
        updateLocationUI();
    }

    private void setMapClickListener() {
        map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                addMarkerAndMoveCamera(latLng);
            }
        });
    }

    private void setMyLocationListener() {
        map.setOnMyLocationClickListener(new GoogleMap.OnMyLocationClickListener() {
            @Override
            public void onMyLocationClick(@NonNull Location location) {
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                addMarkerAndMoveCamera(latLng);
            }
        });
    }

    private void addMarkerAndMoveCamera(LatLng latLng) {
        map.clear();
        map.addMarker(new MarkerOptions().position(latLng));
        MapsActivity.latLng = latLng;
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12));
    }


    private void getLocation() {

        if (requestCode != 0) {
            Intent intent = new Intent();
            switch (requestCode) {
                case EditorActivity.REQUEST_MAP:
                    //TODO save data to Place Realm
                    placeModel.setPrivate(false);
                    savePlace();
                    intent.putExtra(INTENT_PUT, placeModel.getId());
                    setResult(EditorActivity.RESULT_MAP, intent);
                    break;

                case EditorActivity.REQUEST_HOME:
                    //TODO save data to Place Realm
                    placeModel.setPrivate(true);
                    savePlace();
                    intent.putExtra(INTENT_PUT, placeModel.getId());
                    setResult(EditorActivity.RESULT_HOME, intent);
                    break;
            }
        }
    }

    private void saveButtonClickEvent() {
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getLocation();
            }
        });
    }

    private void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    // MyLocation button and Mylocation enable
    private void updateLocationUI() {
        if (map == null) {
            return;
        }
        try {
            if (locationPermissionGranted) {
                map.setMyLocationEnabled(true);
                map.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                map.setMyLocationEnabled(false);
                map.getUiSettings().setMyLocationButtonEnabled(false);
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    //TODO SavePlace
    private void savePlace() {
        helper.insertPlace(placeModel);
        if(foodId != 0) {
            helper.insertFood(food);
        }
    }

}