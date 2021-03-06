package bo.young.myfoodrecords.activities;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.content.Intent;
import android.content.pm.PackageManager;

import bo.young.myfoodrecords.model.Food;
import bo.young.myfoodrecords.model.PlaceModel;
import bo.young.myfoodrecords.adapter.PrivatePlaceAdapter;

import android.example.myfoodrecords.BuildConfig;
import android.example.myfoodrecords.R;
import bo.young.myfoodrecords.utils.Constants;
import bo.young.myfoodrecords.utils.RealmHelper;

import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

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

    // git test
    private GoogleMap map;
    private Realm realm;
    private RealmHelper helper;
    private Food food;

    private PlaceModel placeModel = new PlaceModel();
    private PlaceModel newPlaceModel = new PlaceModel();

    private Marker marker;

    private AutocompleteSupportFragment autocompleteFragment;
    private Button saveButton;
    private boolean locationPermissionGranted;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 8;
    private static final String TAG = "MapsActivityTag";
    private static final String KEY_INSTANCE_PLACE_NAME = "keyPlaceName";
    private static final String KEY_INSTANCE_ADDRESS = "keyAddress";
    private static final String KEY_INSTANCE_PLACE_ID = "keyPlaceId";
    private static final String KEY_INSTANCE_PRIVATE = "keyPrivate";
    private static final String KEY_INSTANCE_ID = "keyId";
    private static final String KEY_INSTANCE_LAT = "keyLat";
    private static final String KEY_INSTANCE_LNG = "keyLng";
    private static final String KEY_INSTANCE_BUNDLE = "keyBundle";

    private int requestCode;
    private PlacesClient placesClient;

    private int foodId = 0;
    private int id = 0;
    private String mPlaceId;
    private String placeName;
    private String placeAddress;
    private double lat;
    private double lng;

    public static LatLng latLng;
    private boolean isPrivatePlace = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        latLng = new LatLng(43.765, -79.419);

        //TODO: git branch dev 1

        setupRealm();
        setupUI();
    }

    private void setupRealm() {
        realm = Realm.getDefaultInstance();
        helper = new RealmHelper(realm);
    }

    private void setupUI() {
        saveButton = findViewById(R.id.map_save_button);
        // Indicates from where the activity is come from
        requestCode = getIntent().getExtras().getInt(Constants.KEY_REQUEST_CODE);
        foodId = getIntent().getExtras().getInt(Constants.KEY_EDITOR_FOOD_ID);

        //If the user wants private place, set isPrivatePlace to true
        if (requestCode == PrivatePlaceActivity.REQUSET_PRIVATE_PLACE) {
            isPrivatePlace = true;
            id = getIntent().getExtras().getInt(Constants.PRIVATE_PLACE_KEY);
        }

        loadMapLocationFromRealm();

        // Initialize the SDK
        Places.initialize(getApplicationContext(), getString(R.string.google_place_key));

        placesClient = Places.createClient(this);

        setupAutoComplete();

        //If it is from DetailActivity, set saveButton and autocompleteFragment to be gone
        if (requestCode == Constants.DETAIL_REQUEST) {
            saveButton.setVisibility(View.GONE);
            autocompleteFragment.getView().setVisibility(View.GONE);
        }

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
        marker = map.addMarker(new MarkerOptions().position(latLng).title(placeName).snippet(placeAddress));
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13));

        //if the activity is not started from DetailActivity, set clickListeners on GoogleMap
        if (requestCode != Constants.DETAIL_REQUEST) {
            setMapClickListener();
        } else {
            //hide edit or save features and only shows the location with detail
            marker.showInfoWindow();
        }
        updateLocationUI();
    }

    /**
     * Autocomplete feature
     * User can search the place using keyboard input
     */
    private void setupAutoComplete() {
        // Initialize the AutocompleteSupportFragment.
        autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        // Specify the types of place data to return.
        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.TYPES, Place.Field.ADDRESS, Place.Field.RATING));

        // when place is selected, receives the data from Place Fields
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NotNull final Place place) {
                LatLng selectedPlaceLagLng = place.getLatLng();
                addMarkerAndMoveCamera(selectedPlaceLagLng);
                placeName = place.getName();
                placeAddress = place.getAddress();
                mPlaceId = place.getId();
                lat = place.getLatLng().latitude;
                lng = place.getLatLng().longitude;
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

        // types of place data
        List<Place.Field> placeField = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.TYPES, Place.Field.ADDRESS, Place.Field.RATING);
        final FetchPlaceRequest request = FetchPlaceRequest.newInstance(placeId, placeField);

        //when Poi is clicked, fetch the data and update
        placesClient.fetchPlace(request).addOnSuccessListener(new OnSuccessListener<FetchPlaceResponse>() {
            @Override
            public void onSuccess(FetchPlaceResponse response) {
                Place place = response.getPlace();
                placeName = place.getName();
                placeAddress = place.getAddress();
                mPlaceId = place.getId();
                lat = place.getLatLng().latitude;
                lng = place.getLatLng().longitude;

                marker = map.addMarker(new MarkerOptions().position(place.getLatLng()).title(placeName).snippet(placeAddress));
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

    /**
     * If the activity is started from PrivatePlaceActivity, select placeModel from Realm
     * else, look for Food from Realm.
     * Update the data accordingly
     */
    private void loadMapLocationFromRealm() {
        if (isPrivatePlace && id != 0) {
            placeModel = realm.where(PlaceModel.class)
                    .equalTo("id", id)
                    .findFirst();
        } else {
            food = realm.where(Food.class)
                    .equalTo("id", foodId)
                    .findFirst();
            if (food != null) {
                placeModel = food.getPlaceModel();
            }
        }
        if (placeModel != null) {
            placeName = placeModel.getPlaceName();
            placeAddress = placeModel.getAddress();
            id = placeModel.getId();
            mPlaceId = placeModel.getPlaceId();
            if (placeModel.getLat() != 0 && placeModel.getLng() != 0) {
                lat = placeModel.getLat();
                lng = placeModel.getLng();
                latLng = new LatLng(lat, lng);
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

    /**
     * Five different ClickListeners
     * Poi, Marker, Map, MyLocation
     */
    private void setMapClickListener() {
        map.setOnPoiClickListener(this);
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
                placeName = null;
                map.clear();
            }
        });
        map.setOnMyLocationClickListener(new GoogleMap.OnMyLocationClickListener() {
            @Override
            public void onMyLocationClick(@NonNull Location location) {
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                addMarkerAndMoveCamera(latLng);
            }
        });
    }

    /**
     * @param latLng Add Marker and move Camera to given latLng position
     */
    private void addMarkerAndMoveCamera(LatLng latLng) {
        map.clear();
        marker = map.addMarker(new MarkerOptions().position(latLng).title(placeName).snippet(placeAddress));
        lat = latLng.latitude;
        lng = latLng.longitude;
        MapsActivity.latLng = latLng;
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
    }

    /**
     * Saves the data and set the result according to where the activity has been started
     */
    private void getLocation() {
        if (requestCode != 0) {
            Intent intent = new Intent();
            switch (requestCode) {
                //From EditorActivity, by clicking "Select From Map"
                case Constants.REQUEST_MAP:
                    isPrivatePlace = false;
                    if (placeName == null) {
                        Toast.makeText(MapsActivity.this, "Please select the location", Toast.LENGTH_SHORT).show();
                        break;
                    }
                    id = 0;
                    savePlace();
                    //passes placeModel's ID data (Primary Key) using intent
                    intent.putExtra(Constants.KEY_PLACE_ID, newPlaceModel.getId());
                    setResult(Constants.RESULT_MAP, intent);
                    finish();
                    break;

                //From PrivatePlaceActivity, adding the private place details
                case PrivatePlaceActivity.REQUSET_PRIVATE_PLACE:
                    isPrivatePlace = true;
                    //as placeModel is the realm object, use newPlaceModel to avoid unwanted Realm transactions
                    if (placeModel == null) {
                        newPlaceModel.setId(0);
                    } else {
                        newPlaceModel.setId(placeModel.getId());
                    }
                    if (placeName == null) {
                        Toast.makeText(MapsActivity.this, getString(R.string.location_not_selected), Toast.LENGTH_SHORT).show();
                        break;
                    }
                    savePlace();
                    //passes placeModel's ID data (Primary Key) using intent
                    Intent intent2 = new Intent(MapsActivity.this, PlaceDetailActivity.class);
                    intent2.putExtra(Constants.KEY_PLACE_ID, newPlaceModel.getId());
                    startActivity(intent2);
                    finish();
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

    /**
     * MyLocation button and Mylocation enable
     */

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

    /**
     * saves the data in Realm database.
     * as placeModel is the realm object, use newPlaceModel to avoid unwanted Realm transactions.
     */
    private void savePlace() {
        if (placeName != null) {
            newPlaceModel.setPlaceName(placeName);
            newPlaceModel.setAddress(placeAddress);
            newPlaceModel.setPlaceId(mPlaceId);
            newPlaceModel.setId(id);
            newPlaceModel.setLng(lng);
            newPlaceModel.setLat(lat);
            newPlaceModel.setPrivate(isPrivatePlace);
            helper.insertPlace(newPlaceModel);
        }

    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Bundle bundle = new Bundle();
        bundle.putString(KEY_INSTANCE_PLACE_NAME, placeName);
        bundle.putString(KEY_INSTANCE_ADDRESS, placeAddress);
        bundle.putInt(KEY_INSTANCE_ID, id);
        bundle.putString(KEY_INSTANCE_PLACE_ID, mPlaceId);
        bundle.putDouble(KEY_INSTANCE_LAT, lat);
        bundle.putDouble(KEY_INSTANCE_LNG, lng);
        bundle.putBoolean(KEY_INSTANCE_PRIVATE, isPrivatePlace);
        outState.putBundle(KEY_INSTANCE_BUNDLE, bundle);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Bundle bundle = savedInstanceState.getBundle(KEY_INSTANCE_BUNDLE);
        placeName = bundle.getString(KEY_INSTANCE_PLACE_NAME);
        placeAddress = bundle.getString(KEY_INSTANCE_ADDRESS);
        id = bundle.getInt(KEY_INSTANCE_ID);
        mPlaceId = bundle.getString(KEY_INSTANCE_PLACE_ID);
        lat = bundle.getDouble(KEY_INSTANCE_LAT);
        lng = bundle.getDouble(KEY_INSTANCE_LNG);
        isPrivatePlace = bundle.getBoolean(KEY_INSTANCE_PRIVATE);
        if (lat != 0 && lng != 0) {
            latLng = new LatLng(lat, lng);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (realm != null) {
            realm.close();
        }
    }
}