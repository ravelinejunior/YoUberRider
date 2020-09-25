package br.com.youberrider.ui.home;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import br.com.youberrider.R;
import br.com.youberrider.callback.IFirebaseDriverInfoListener;
import br.com.youberrider.callback.IFirebaseFailedListener;
import br.com.youberrider.model.AnimationModel;
import br.com.youberrider.model.DriverGeo;
import br.com.youberrider.model.DriverInfo;
import br.com.youberrider.model.GeoQueryModel;
import br.com.youberrider.remote.IGoogleAPI;
import br.com.youberrider.remote.RetrofitClient;
import br.com.youberrider.utils.Common;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;


public class HomeFragment extends Fragment implements OnMapReadyCallback, IFirebaseDriverInfoListener, IFirebaseFailedListener {


    private static final double LIMIT_RANGE = 10.0; //max km
    //Listeners
    IFirebaseDriverInfoListener iFirebaseDriverInfoListener;
    IFirebaseFailedListener iFirebaseFailedListener;
    private HomeViewModel homeViewModel;
    //location
    private GoogleMap mMap;
    private SupportMapFragment mapFragment;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private GeoFire geoFire;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    //firebase
    private FirebaseAuth auth = FirebaseAuth.getInstance();
    private DatabaseReference firebaseDatabase = FirebaseDatabase.getInstance().getReference();
    private DatabaseReference onlineRef;
    private DatabaseReference currentUserRef;
    //load driver
    private double distance = 1.0; //default em km
    private boolean firstTime = true;
    //para calcular a distancia entre
    private Location previousLocation;
    private Location currentLocation;

    private String cityName;

    //
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private IGoogleAPI iGoogleAPI;

    //Moving Marker
    private List<LatLng> polylineList = new ArrayList<>();
    private Handler handler;
    private int index;
    private int next;
    private LatLng start;
    private LatLng end;
    private float v;
    private double lat;
    private double lng;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        init();

        mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        return view;
    }

    @Override
    public void onStop() {
        compositeDisposable.clear();
        super.onStop();
    }

    private void init() {

        iGoogleAPI = RetrofitClient.getInstance().create(IGoogleAPI.class);

        iFirebaseDriverInfoListener = this;
        iFirebaseFailedListener = this;

        locationRequest = new LocationRequest();
        locationRequest.setSmallestDisplacement(50f);
        locationRequest.setInterval(15000);
        locationRequest.setFastestInterval(10000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);

                LatLng newPosition = new LatLng(locationResult.getLastLocation().getLatitude(),
                        locationResult.getLastLocation().getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newPosition, 18f));

                //se usuario mudou a localização, calcular e carregar motorista de novo
                if (firstTime) {
                    previousLocation = currentLocation = locationResult.getLastLocation();
                    firstTime = false;
                } else {
                    previousLocation = currentLocation;
                    currentLocation = locationResult.getLastLocation();
                }

                if (previousLocation.distanceTo(currentLocation) / 1000 <= LIMIT_RANGE)
                    loadAvailableDrivers();


            }
        };

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getContext());
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED
        ) {
            Snackbar.make(getView(), "Sem permissão de fusedLocationProviderClient.", Snackbar.LENGTH_LONG).show();
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());

        //carregar motoristas
        loadAvailableDrivers();

    }

    private void loadAvailableDrivers() {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Snackbar.make(getView(), "Sem permissão de load drivers.", Snackbar.LENGTH_LONG).show();
            return;
        }
        fusedLocationProviderClient.getLastLocation()
                .addOnFailureListener(e -> Snackbar.make(getView(), e.getMessage(), Snackbar.LENGTH_LONG).show())
                .addOnSuccessListener(location -> {
                    Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
                    List<Address> addressList;
                    try {
                        addressList = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                        cityName = addressList.get(0).getSubAdminArea(); // nome do estado

                        //QUERY
                        DatabaseReference driverLocationRef = firebaseDatabase.child(Common.DRIVERS_LOCATION_REFERENCE)
                                .child(cityName);

                        GeoFire geoFire = new GeoFire(driverLocationRef);
                        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(location.getLatitude(),
                                location.getLongitude()), distance);
                        geoQuery.removeAllListeners();

                        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
                            @Override
                            public void onKeyEntered(String key, GeoLocation location) {
                                Common.driversFound.add(new DriverGeo(key, location));
                            }

                            @Override
                            public void onKeyExited(String key) {

                            }

                            @Override
                            public void onKeyMoved(String key, GeoLocation location) {

                            }

                            @Override
                            public void onGeoQueryReady() {
                                if (distance <= LIMIT_RANGE) {
                                    distance++;
                                    loadAvailableDrivers(); // continue procurando numa nova distancia
                                } else {
                                    distance = 1.0; //reset
                                    addDriverMarker();
                                }
                            }

                            @Override
                            public void onGeoQueryError(DatabaseError error) {
                                Snackbar.make(getView(), error.getMessage(), Snackbar.LENGTH_LONG).show();
                            }
                        });

                        //listen to new driver and range it
                        driverLocationRef.addChildEventListener(new ChildEventListener() {
                            @Override
                            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                                GeoQueryModel geoQueryModel = snapshot.getValue(GeoQueryModel.class);
                                GeoLocation geoLocation = new GeoLocation(geoQueryModel.getL().get(0)
                                        , geoQueryModel.getL().get(1));
                                DriverGeo driverGeo = new DriverGeo(snapshot.getKey(), geoLocation);
                                Location newDriverLocation = new Location("");
                                newDriverLocation.setLatitude(geoLocation.latitude);
                                newDriverLocation.setLongitude(geoLocation.longitude);

                                float newDistance = location.distanceTo(newDriverLocation) / 1000; //in km
                                if (newDistance <= LIMIT_RANGE) {
                                    findDriverByKey(driverGeo); // se driver no alcance, add to map
                                }
                            }

                            @Override
                            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                                GeoQueryModel geoQueryModel = snapshot.getValue(GeoQueryModel.class);
                                GeoLocation geoLocation = new GeoLocation(geoQueryModel.getL().get(0)
                                        , geoQueryModel.getL().get(1));
                                DriverGeo driverGeo = new DriverGeo(snapshot.getKey(), geoLocation);
                                Location newDriverLocation = new Location("");
                                newDriverLocation.setLatitude(geoLocation.latitude);
                                newDriverLocation.setLongitude(geoLocation.longitude);

                                float newDistance = location.distanceTo(newDriverLocation) / 1000; //in km
                                if (newDistance <= LIMIT_RANGE) {
                                    findDriverByKey(driverGeo); // se driver no alcance, add to map
                                }
                            }

                            @Override
                            public void onChildRemoved(@NonNull DataSnapshot snapshot) {

                            }

                            @Override
                            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });

                    } catch (IOException e) {
                        e.printStackTrace();
                        Snackbar.make(getView(), e.getMessage(), Snackbar.LENGTH_LONG).show();
                    }
                });
    }

    private void addDriverMarker() {
        if (Common.driversFound.size() > 0) {
            Observable.fromIterable(Common.driversFound)
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(driverGeo -> {
                                //on next
                                findDriverByKey(driverGeo);
                            },
                            throwable -> {
                                Snackbar.make(getView(), "Sem permissão de fusedLocationProviderClient.", Snackbar.LENGTH_LONG).show();
                            },
                            () -> {
                            }
                    );
        } else {
            Snackbar.make(getView(), "Nenhum motorista encontrado!", Snackbar.LENGTH_LONG).show();
        }
    }

    private void findDriverByKey(DriverGeo driverGeo) {
        firebaseDatabase.child(Common.DRIVER_INFO_REFERENCE)
                .child(driverGeo.getKey())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.hasChildren()) {
                            driverGeo.setDriverInfo(snapshot.getValue(DriverInfo.class));
                            iFirebaseDriverInfoListener.onDriverInforLoadSuccess(driverGeo);
                        } else
                            iFirebaseFailedListener.onFirebaseLoadFailed("Erro ao obter chave de usuario. Chave "
                                    + driverGeo.getKey() + " não encontrada");
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        iFirebaseFailedListener.onFirebaseLoadFailed(error.getMessage());
                        Snackbar.make(getView(), error.getMessage(), Snackbar.LENGTH_LONG).show();
                    }
                });
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        //carregar estilo de mapa
        mMap.getUiSettings().setZoomControlsEnabled(true);

        //check permissions
        Dexter.withContext(getContext())
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {

                        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) !=
                                PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(),
                                Manifest.permission.ACCESS_COARSE_LOCATION) !=
                                PackageManager.PERMISSION_GRANTED) {
                            Toast.makeText(getContext(), "Entrou no modo check permission!", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        mMap.setMyLocationEnabled(true);
                        mMap.getUiSettings().setMyLocationButtonEnabled(true);

                        try {
                            mMap.setOnMyLocationButtonClickListener(() -> {
                                if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) !=
                                        PackageManager.PERMISSION_GRANTED &&
                                        ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) !=
                                                PackageManager.PERMISSION_GRANTED) {
                                    Toast.makeText(getContext(), "Entrou no modo check permission!", Toast.LENGTH_SHORT).show();
                                    return false;
                                }
                                fusedLocationProviderClient.getLastLocation().addOnFailureListener
                                        (e -> Toast.makeText(getContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show())
                                        .addOnSuccessListener(location -> {
                                            try {
                                                LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 18f));
                                            } catch (Exception e) {
                                                Snackbar.make(mapFragment.getView(), "Verifique sua conexão com a internet ou ligue o GPS", Snackbar.LENGTH_LONG).
                                                        setAnimationMode(Snackbar.ANIMATION_MODE_FADE).show();
                                            }
                                        });
                                return true;
                            });
                        } catch (Exception e) {
                            Snackbar.make(mapFragment.getView(), e.getMessage(), Snackbar.LENGTH_SHORT).
                                    setAnimationMode(Snackbar.ANIMATION_MODE_FADE).show();
                        }

                        //set layout button
                        View locationButton = ((View) mapFragment.getView().
                                findViewById(Integer.parseInt("1")).getParent()).findViewById(Integer.parseInt("2"));
                        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) locationButton.getLayoutParams();
                        //center bottom
                        params.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
                        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
                        params.addRule(RelativeLayout.ALIGN_PARENT_START, RelativeLayout.TRUE);
                        params.setMargins(50, 0, 0, 50);

                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
                        Toast.makeText(getContext(), "Permissão " + permissionDeniedResponse.getPermissionName() + " foi negada!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {

                    }
                }).check();


        try {
            boolean success = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(getContext(), R.raw.uber_maps_style));
            if (!success) {
                Snackbar.make(getView(), "Style parsing error", Snackbar.LENGTH_LONG).setAnimationMode(Snackbar.ANIMATION_MODE_FADE).show();
                Log.e("MapsError", "Style parsing error");
            }
        } catch (Resources.NotFoundException e) {
            Snackbar.make(getView(), e.getMessage(), Snackbar.LENGTH_LONG).setAnimationMode(Snackbar.ANIMATION_MODE_FADE).show();
            Log.e("MapsError", e.getMessage());
        }

    }

    @Override
    public void onDriverInforLoadSuccess(DriverGeo driverGeo) {
        //se ja existir marcador com essa key, não marcar de novo
        if (!Common.markerList.containsKey(driverGeo.getKey()))
            Common.markerList.put(driverGeo.getKey(), mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(driverGeo.getGeoLocation().latitude, driverGeo.getGeoLocation().longitude))
                    .flat(true)
                    .title(Common.buildName(driverGeo.getDriverInfo().getFirstName(),
                            driverGeo.getDriverInfo().getLastName()))
                    .snippet(driverGeo.getDriverInfo().getPhoneNumber())
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.car))));
        if (!TextUtils.isEmpty(cityName)) {
            DatabaseReference driverLocation = firebaseDatabase.child(Common.DRIVERS_LOCATION_REFERENCE)
                    .child(cityName)
                    .child(driverGeo.getKey());
            driverLocation.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (!snapshot.hasChildren()) {
                        if (Common.markerList.get(driverGeo.getKey()) != null)
                            Common.markerList.get(driverGeo.getKey()).remove(); // remover o marker

                        Common.markerList.remove(driverGeo.getKey());
                        Common.driverLocationSubscribe.remove(driverGeo.getKey()); //remover driver information
                        driverLocation.removeEventListener(this);
                    } else {
                        if (Common.markerList.get(driverGeo.getKey()) != null) {
                            GeoQueryModel geoQuery = snapshot.getValue(GeoQueryModel.class);
                            AnimationModel animationModel = new AnimationModel(false, geoQuery);
                            if (Common.driverLocationSubscribe.get(driverGeo.getKey()) != null) {
                                Marker currentMarker = Common.markerList.get(driverGeo.getKey());
                                AnimationModel oldPosition = Common.driverLocationSubscribe.get(driverGeo.getKey());

                                String from = new StringBuilder()
                                        .append(oldPosition.getGeoQuery().getL().get(0))
                                        .append(",")
                                        .append(oldPosition.getGeoQuery().getL().get(0))
                                        .toString();

                                String to = new StringBuilder()
                                        .append(animationModel.getGeoQuery().getL().get(0))
                                        .append(",")
                                        .append(animationModel.getGeoQuery().getL().get(0))
                                        .toString();

                                moveMarkerAnimation(driverGeo.getKey(), animationModel, currentMarker, from, to);

                            } else {
                                //localização inicial
                                Common.driverLocationSubscribe.put(driverGeo.getKey(), animationModel);
                            }
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Snackbar.make(getView(), error.getMessage(), Snackbar.LENGTH_LONG)
                            .setAnimationMode(Snackbar.ANIMATION_MODE_FADE).show();
                }
            });
        }
    }

    private void moveMarkerAnimation(String key, AnimationModel animationModel, Marker currentMarker, String from, String to) {
        if (!animationModel.isRun()) {
            //request API
            compositeDisposable.add(iGoogleAPI.getDirections("driving",
                    "less_driving",
                    from, to,
                    getString(R.string.google_api_key))
                    .subscribeOn(io.reactivex.schedulers.Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(returnResult -> {
                        Log.d("API_RETURN", returnResult);

                        try {
                            JSONObject jsonObject = new JSONObject(returnResult);
                            JSONArray jsonArray = jsonObject.getJSONArray("routes");
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject route = jsonArray.getJSONObject(i);
                                JSONObject poly = route.getJSONObject("overview_polyline");
                                String polyline = poly.getString("points");
                                polylineList = Common.decodePoly(polyline);
                            }

                            //moving
                            handler = new Handler();
                            index = 1;
                            next = 1;
                            Runnable runnable = new Runnable() {
                                @Override
                                public void run() {
                                    if (polylineList.size() > 1) {
                                        if (index < polylineList.size() - 2) {
                                            index++;
                                            next = index + 1;
                                            start = polylineList.get(index);
                                            end = polylineList.get(next);
                                        }

                                        ValueAnimator valueAnimator = ValueAnimator.ofInt(0,1);
                                        valueAnimator.setDuration(3000);
                                        valueAnimator.setInterpolator(new LinearInterpolator());
                                        valueAnimator.addUpdateListener(value -> {

                                            v = value.getAnimatedFraction();
                                            lat = v*end.latitude + (1-v) * start.latitude;
                                            lng = v*end.longitude+(1-v)*start.longitude;
                                            LatLng newPos = new LatLng(lat,lng);
                                            currentMarker.setPosition(newPos);
                                            currentMarker.setAnchor(0.5f,0.5f);
                                            currentMarker.setRotation(Common.getBearing(start,newPos));

                                        });

                                        valueAnimator.start();
                                        if(index<polylineList.size() - 2) //reach destination
                                            handler.postDelayed(this,1000);
                                        else if(index < polylineList.size() - 1){
                                            animationModel.setRun(false);
                                            Common.driverLocationSubscribe.put(key,animationModel); //Update data

                                        }
                                    }
                                }
                            };

                            //run handler
                            handler.postDelayed(runnable,1000);


                        } catch (Exception e) {
                            Snackbar.make(getView(), e.getMessage(), Snackbar.LENGTH_SHORT).show();
                        }
                    })

            );
        }
    }

    @Override
    public void onFirebaseLoadFailed(String message) {
        Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).setAnimationMode(Snackbar.ANIMATION_MODE_FADE).show();
    }
}