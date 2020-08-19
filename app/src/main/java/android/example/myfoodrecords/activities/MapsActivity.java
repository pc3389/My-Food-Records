package android.example.myfoodrecords.activities;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.example.myfoodrecords.adapter.PrivatePlaceAdapter;
import android.example.myfoodrecords.R;
import android.example.myfoodrecords.utils.RealmHelper;
import android.example.myfoodrecords.model.Food;
import android.example.myfoodrecords.model.PlaceModel;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PointOfInterest;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FetchPlaceResponse;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

import io.realm.Realm;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnPoiClickListener {

    private GoogleMap map;
    private Realm foodRealm;
    private RealmHelper foodHelper;
    private Food food;

    private PlaceModel placeModel = new PlaceModel();
    private PlaceModel newPlaceModel = new PlaceModel();

    private Button saveButton;
    private boolean locationPermissionGranted;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 8;
    public static final int RESULT_PRIVATE_PLACE = 3;
    private static final String TAG = "MapsActivityTag";
    public static final String PLACE_ID_KEY = "placePrimaryKey";

    public static LatLng latLng;
    private int requestCode;
    private int foodId = 0;
    private int placeId = 0;
    private PlacesClient placesClient;
    private boolean isPrivatePlace = false;

    private String placeAddress;
    private String placeName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        latLng = new LatLng(43, -79);

        setupRealm();
        setupUI();
    }

    private void setupRealm() {
        foodRealm = Realm.getDefaultInstance();
        foodHelper = new RealmHelper(foodRealm);
    }

    private void setupUI() {
        saveButton = findViewById(R.id.map_save_button);
        requestCode = getIntent().getExtras().getInt(EditorActivity.KEY_REQUEST_CODE);
        foodId = getIntent().getExtras().getInt(EditorActivity.KEY_EDITOR_FOOD_ID);
        if (requestCode == DetailActivity.DETAIL_REQUEST) {
            saveButton.setVisibility(View.GONE);
        }
        if (requestCode == PrivatePlaceActivity.REQUSET_PRIVATE_PLACE) {
            isPrivatePlace = true;
            placeId = getIntent().getExtras().getInt(PrivatePlaceAdapter.PRIVATE_PLACE_KEY);
        }

        loadMapLocationFromRealm();

        // Initialize the SDK
        Places.initialize(getApplicationContext(), getString(R.string.google_place_key));

        // Create a new PlacesClient instance
        placesClient = Places.createClient(this);

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

        getLocationPermission();
        map.setOnPoiClickListener(this);
        map.addMarker(new MarkerOptions().position(latLng).title(placeName).snippet(placeAddress));
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13));
        updateLocationUI();
        setMapClickListener();
        setMyLocationListener();
    }

    private void setupAutoComplete() {

        // Initialize the AutocompleteSupportFragment.
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        // Specify the types of place data to return.
        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.TYPES, Place.Field.ADDRESS, Place.Field.RATING));

        // Set up a PlaceSelectionListener to handle the response.
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NotNull final Place place) {
                LatLng selectedPlaceLagLng = place.getLatLng();
                addMarkerAndMoveCamera(selectedPlaceLagLng);

                placeName = place.getName();
                placeAddress = place.getAddress();
                newPlaceModel.setPlaceId(place.getId());
                newPlaceModel.setPlaceName(place.getName());
                newPlaceModel.setLat(place.getLatLng().latitude);
                newPlaceModel.setLng(place.getLatLng().longitude);
                newPlaceModel.setAddress(place.getAddress());
                if (place.getRating() != null) {
                    newPlaceModel.setPlaceRating(place.getRating().floatValue());
                }
            }

            @Override
            public void onError(@NotNull Status status) {
                Log.i(TAG, "An error occurred: " + status);
            }
        });
    }

    @Override
    public void onPoiClick(PointOfInterest poi) {
        map.clear();
        String placeId = poi.placeId;
        List<Place.Field> placeField = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.TYPES, Place.Field.ADDRESS, Place.Field.RATING);
        final FetchPlaceRequest request = FetchPlaceRequest.newInstance(placeId, placeField);
        placesClient.fetchPlace(request).addOnSuccessListener(new OnSuccessListener<FetchPlaceResponse>() {
            @Override
            public void onSuccess(FetchPlaceResponse response) {
                Place place = response.getPlace();
                placeName = place.getName();
                placeAddress = place.getAddress();

                newPlaceModel.setPlaceId(place.getId());
                newPlaceModel.setPlaceName(place.getName());
                newPlaceModel.setLat(place.getLatLng().latitude);
                newPlaceModel.setLng(place.getLatLng().longitude);
                newPlaceModel.setAddress(place.getAddress());
                if (place.getRating() != null) {
                    newPlaceModel.setPlaceRating(place.getRating().floatValue());
                }
                Marker marker = map.addMarker(new MarkerOptions().position(place.getLatLng()).title(placeName).snippet(placeAddress));
                marker.showInfoWindow();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                if (exception instanceof ApiException) {
                    final ApiException apiException = (ApiException) exception;
                    final int statusCode = apiException.getStatusCode();
                    Log.e(TAG, "Place not found: " + exception.getMessage() + statusCode);
                }
            }
        });
    }

    private void loadMapLocationFromRealm() {
        if (isPrivatePlace && placeId != 0) {
            placeModel = foodRealm.where(PlaceModel.class)
                    .equalTo("id", placeId)
                    .findFirst();
            if (placeModel != null) {
                latLng = new LatLng(placeModel.getLat(), placeModel.getLng());
                placeName = placeModel.getPlaceName();
                placeAddress = placeModel.getAddress();
            }

        } else {
            food = foodRealm.where(Food.class)
                    .equalTo("id", foodId)
                    .findFirst();
            if (food != null) {
                placeModel = food.getPlaceModel();
                if (placeModel != null) {
                    latLng = new LatLng(placeModel.getLat(), placeModel.getLng());
                    placeName = placeModel.getPlaceName();
                    placeAddress = placeModel.getAddress();
                }
            }
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
        map.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                addMarkerAndMoveCamera(latLng);
            }
        });
        map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                marker.showInfoWindow();
                return true;
            }
        });
        map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                map.clear();
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
        map.addMarker(new MarkerOptions().position(latLng).title(placeName).snippet(placeAddress));
        newPlaceModel.setLat(latLng.latitude);
        newPlaceModel.setLng(latLng.longitude);
        MapsActivity.latLng = latLng;
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
    }

    private void getLocation() {
        if (requestCode != 0) {
            Intent intent = new Intent();
            switch (requestCode) {
                case EditorActivity.REQUEST_MAP:
                    newPlaceModel.setPrivate(false);
                    savePlace();
                    intent.putExtra(PLACE_ID_KEY, newPlaceModel.getId());
                    setResult(EditorActivity.RESULT_MAP, intent);
                    break;

                case PrivatePlaceActivity.REQUSET_PRIVATE_PLACE:
                    newPlaceModel.setPrivate(true);
                    newPlaceModel.setId(placeId);
                    savePlace();
                    Intent intent2 = new Intent(MapsActivity.this, PlaceDetailActivity.class);
                    intent2.putExtra(PLACE_ID_KEY, newPlaceModel.getId());
                    startActivity(intent2);
                    break;
            }
            finish();
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

    private void savePlace() {
        if (newPlaceModel != null) {
            foodHelper.insertPlace(newPlaceModel);
        }
    }

}