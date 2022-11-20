package com.example.arnavi;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;
import com.skt.tmap.TMapData;
import com.skt.tmap.TMapGpsManager;
import com.skt.tmap.TMapInfo;
import com.skt.tmap.TMapPoint;
import com.skt.tmap.TMapView;
import com.skt.tmap.overlay.TMapMarkerItem;
import com.skt.tmap.poi.TMapPOIItem;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    private SensorManager sensorManager;
    private final float[] accelerometerReading = new float[3];
    private final float[] magnetometerReading = new float[3];

    private final float[] rotationMatrix = new float[9];
    private final float[] mOrientationAngles = new float[3];

    // T Map View
    public TMapView tMapView;

    // T Map GPS
    public TMapGpsManager manager;

    public TMapData tmapdata;

    public TMapPOIItem startPoint;
    public TMapPOIItem endPoint;

    public TMapPoint start;
    public TMapPoint end;
    public Location currentLocation;
    public ArrayList<Location> pointList = new ArrayList<>();

    public double pointLat;
    public double pointLon;

    float currentOrientation;

    public boolean isPathGuide = false;

    public PreviewView previewView;

    public TextView orientationView;

    public ImageView arrow;

    public int nowIndex = 0;
    public int oldIndex = 0;

    public final int COUNT = 50;
    public int count = 0;
    public float angleSum = 0;
    public float[] angleList = new float[COUNT];

    public ConstraintLayout mainLayout;
    public ConstraintLayout loadingLayout;
    public ImageView locationImage;
    public EditText endPointText;
    public TextView logo;

    private int shortAnimationDuration;
    private boolean isStart = true;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String[] permissions = {
                Manifest.permission.ACTIVITY_RECOGNITION,
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
        };

        mainLayout = findViewById(R.id.mainLayout);
        loadingLayout = findViewById(R.id.loadingLayout);

        mainLayout.setVisibility(View.GONE);
        loadingLayout.setVisibility(View.VISIBLE);

        shortAnimationDuration = getResources().getInteger(
                android.R.integer.config_longAnimTime);

        ConstraintLayout constraintLayout = findViewById(R.id.constraintLayout);

        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        FrameLayout frameLayout = findViewById(R.id.container);
        previewView = findViewById(R.id.previewView);
        arrow = findViewById(R.id.arrow);

        orientationView = findViewById(R.id.orientationView);

        Bitmap current_icon = BitmapFactory.decodeResource(getResources(), R.drawable.current_icon);
        current_icon = Bitmap.createScaledBitmap(current_icon, 110, 110, true);
        Bitmap sight_icon = BitmapFactory.decodeResource(getResources(), R.drawable.sight_icon);
        sight_icon = Bitmap.createScaledBitmap(sight_icon, 350, 300, true);
        Bitmap finalCurrent_icon = current_icon;
        Bitmap finalSight_icon = sight_icon;

        manager = new TMapGpsManager(this);

        tmapdata = new TMapData();

        tMapView = new TMapView(this);
        tMapView.setSKTMapApiKey("l7xx8c266dda82a64d19921918f0225c8193");

        ConstraintLayout container = findViewById(R.id.Tmap);
        container.addView(tMapView);

        locationImage = findViewById(R.id.locationImage);
        locationImage.setOnClickListener(view -> {
            locationImage.setSelected(!locationImage.isSelected());
            setTracking(locationImage.isSelected());
        });

        endPointText = findViewById(R.id.endPointText);

        logo = findViewById(R.id.logo);

        tMapView.setOnMapReadyListener(() -> {
            //todo 맵 로딩 완료 후 구현

            tMapView.setSightImage(finalSight_icon);
            tMapView.setIcon(finalCurrent_icon);

            manager.setOnLocationChangeListener(locationListener);

            manager.setMinDistance(3);
            manager.setMinTime(300);

            manager.setProvider(TMapGpsManager.PROVIDER_GPS);
            manager.openGps();

            manager.setProvider(TMapGpsManager.PROVIDER_NETWORK);
            manager.openGps();

            tMapView.setTrackingMode(true);
        });

        ListView listView = findViewById(R.id.listView);

//        searchButton.setOnClickListener(view -> {
//            setTracking(true);
//            tMapView.removeAllTMapPOIItem();
//            tMapView.removeAllTMapOverlay();
//            listView.setVisibility(View.GONE);
//            inputMethodManager.hideSoftInputFromWindow(endPointText.getWindowToken(), 0);
//
//            if (start == null || end == null) {
//                return;
//            }
//
//            frameLayout.setVisibility(View.VISIBLE);
//
//            cameraProviderFuture = ProcessCameraProvider.getInstance(getApplicationContext());
//
//            cameraProviderFuture.addListener(() -> {
//                try {
//                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
//                    bindPreview(cameraProvider);
//                } catch (ExecutionException | InterruptedException e) {
//                    // No errors need to be handled for this Future.
//                    // This should never be reached.
//                }
//            }, ContextCompat.getMainExecutor(getApplicationContext()));
//
//            tmapdata.findPathDataWithType(TMapData.TMapPathType.PEDESTRIAN_PATH, start, end, tMapPolyLine -> {
//                tMapPolyLine.setLineWidth(3);
//                tMapPolyLine.setLineColor(Color.BLUE);
//                tMapPolyLine.setLineAlpha(255);
//
//                tMapPolyLine.setOutLineWidth(5);
//                tMapPolyLine.setOutLineColor(Color.RED);
//                tMapPolyLine.setOutLineAlpha(255);
//
//                tMapView.addTMapPolyLine(tMapPolyLine);
//                TMapInfo info = tMapView.getDisplayTMapInfo(tMapPolyLine.getLinePointList());
//                tMapView.setZoomLevel(info.getZoom());
//                tMapView.setCenterPoint(info.getPoint().getLatitude(), info.getPoint().getLongitude());
//
//                tMapView.setTMapPath(tMapPolyLine);
//            });
//
//            tmapdata.findPathDataAllType(TMapData.TMapPathType.PEDESTRIAN_PATH, start, end, document -> {
//                pointList.clear();
//                Element root = document.getDocumentElement();
//                NodeList nodeListPlacemark = root.getElementsByTagName("Placemark");
//                for( int i=0; i<nodeListPlacemark.getLength(); i++ ) {
//                    NodeList nodeListPlacemarkItem = nodeListPlacemark.item(i).getChildNodes();
//                    for( int j=0; j<nodeListPlacemarkItem.getLength(); j++ ) {
//                        if( nodeListPlacemarkItem.item(j).getNodeName().equals("Point") ) {
//                            NodeList nodeListPoint = nodeListPlacemarkItem.item(j).getChildNodes();
//                            for( int k=0; k<nodeListPoint.getLength(); k++) {
//                                if (nodeListPoint.item(k).getNodeName().equals("coordinates")) {
//                                    Log.d("tmap", nodeListPoint.item(k).getTextContent().trim());
//
//                                    String[] point = nodeListPoint.item(k).getTextContent().trim().split(",");
//                                    pointLat = Double.parseDouble(point[1]);
//                                    pointLon = Double.parseDouble(point[0]);
//
//                                    Log.d("tmap", "lat "+pointLat);
//                                    Log.d("tmap", "lon "+pointLon);
//
//                                    Location location = new Location("point");
//
//                                    location.setLatitude(pointLat);
//                                    location.setLongitude(pointLon);
//
//                                    pointList.add(location);
//                                }
//                            }
//                        }
//                    }
//                }
//
//                isPathGuide = true;
//            });
//        });


        endPointText.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                end = null;
            }
            return false;
        });

        endPointText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable editable) {
                String strData = endPointText.getText().toString();

                tMapView.removeAllTMapPOIItem();

                if(!strData.trim().isEmpty()) {
                    tmapdata.findAllPOI(strData, poiItemList -> {
                        if(poiItemList != null) {
//                            for (TMapPOIItem item : poiItemList) {
//                                Log.d("Poi Item",
//                                        "name:" + item.getPOIName() + " address:" + item.getPOIAddress()
//                                );
//                            }

                            if(endPointText.isFocused()) {
                                tMapView.addTMapPOIItem(poiItemList);
                            }

                            if (end == null) {
                                MyAdapter arrayAdapter = new MyAdapter(getApplicationContext(), R.layout.list_item, poiItemList);
                                runOnUiThread(() -> {
                                    listView.setVisibility(View.VISIBLE);
                                    listView.setAdapter(arrayAdapter);
                                    listView.setOnItemClickListener((adapterView, view, i, l) -> {
                                        endPoint = poiItemList.get(i);
                                        endPointText.setText(endPoint.getPOIName());
                                        endPointText.clearFocus();
                                        listView.setVisibility(View.GONE);

                                        setTracking(false);

                                        end = new TMapPoint(Double.parseDouble(endPoint.frontLat), Double.parseDouble(endPoint.frontLon));

                                        tMapView.removeAllTMapPOIItem();
                                        tMapView.setLocationPoint(end.getLatitude(), end.getLongitude());
                                        tMapView.setCenterPoint(end.getLatitude(), end.getLongitude());
                                        TMapMarkerItem endMarkerItem = new TMapMarkerItem();
                                        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.poi_dot);
                                        endMarkerItem.setId("endMarker");
                                        endMarkerItem.setIcon(bitmap);
                                        endMarkerItem.setTMapPoint(end);
                                        tMapView.addTMapMarkerItem(endMarkerItem);

                                        inputMethodManager.hideSoftInputFromWindow(endPointText.getWindowToken(), 0);
                                    });
                                });
                            }
                        }
                    });
                }
                else {
                    runOnUiThread(() -> listView.setVisibility(View.GONE));
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        SensorEventListener sensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                if (isPathGuide) {
                    if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                        System.arraycopy(sensorEvent.values, 0, accelerometerReading,
                                0, accelerometerReading.length);
                    } else if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                        System.arraycopy(sensorEvent.values, 0, magnetometerReading,
                                0, magnetometerReading.length);
                    }

                    updateOrientationAngles();
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        };

        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer != null) {
            sensorManager.registerListener(sensorEventListener, accelerometer,
                    SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        }
        Sensor magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (magneticField != null) {
            sensorManager.registerListener(sensorEventListener, magneticField,
                    SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

//        sensorManager.unregisterListener((SensorEventListener) this);
    }



    public void updateOrientationAngles() {
        // Update rotation matrix, which is needed to update orientation angles.
        SensorManager.getRotationMatrix(rotationMatrix, null,
                accelerometerReading, magnetometerReading);

        // "mRotationMatrix" now has up-to-date information.

        SensorManager.getOrientation(rotationMatrix, mOrientationAngles);

        // "mOrientationAngles" now has up-to-date information.
        currentOrientation = (float) (180/Math.PI)*mOrientationAngles[0];

        float arrowOrientation = getArrowOrientation();
        float angle = arrowOrientation;

        angleSum = angleSum + angle;
        angleList[count++] = angle;
        if (count == COUNT) {
            arrow.setRotation(angleSum/COUNT);
            count = 0;
            angleSum = 0;
        }
    }

    public float getArrowOrientation() {
        float orientation = 0;

        if (currentLocation.distanceTo(pointList.get(nowIndex)) <= 10.0F) {
            nowIndex++;
        }
        if (nowIndex == pointList.size()) {
            isPathGuide = false;
            Toast.makeText(this, "목적지에 도착하였습니다!", Toast.LENGTH_LONG).show();
        }

        if (nowIndex != oldIndex) {
            if (tMapView.getMarkerItemFromId("point") != null) {
                tMapView.removeTMapMarkerItem("point");
            }
            TMapMarkerItem tMapMarkerItem = new TMapMarkerItem();
            tMapMarkerItem.setId("point");
            tMapMarkerItem.setTMapPoint(pointList.get(nowIndex).getLatitude(), pointList.get(nowIndex).getLongitude());
            tMapMarkerItem.setIcon(BitmapFactory.decodeResource(getResources(), R.drawable.poi_dot));
            tMapView.addTMapMarkerItem(tMapMarkerItem);
        }

        float distance = currentLocation.distanceTo(pointList.get(nowIndex));

        float pointOrientation = currentLocation.bearingTo(pointList.get(nowIndex));

        orientation = pointOrientation - currentOrientation;

        orientationView.setText(nowIndex + ", " + distance + ", " + orientation);

        oldIndex = nowIndex;

        return orientation;
    }

    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder()
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        previewView.setScaleType(PreviewView.ScaleType.FILL_END);
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview);
    }


    private void setTracking(boolean isTracking) {
        if (isTracking) {
            manager.setOnLocationChangeListener(locationListener);

            manager.setMinDistance(3);
            manager.setMinTime(300);

            manager.setProvider(TMapGpsManager.PROVIDER_GPS);
            manager.openGps();

            manager.setProvider(TMapGpsManager.PROVIDER_NETWORK);
            manager.openGps();

            tMapView.setTrackingMode(true);
            tMapView.setSightVisible(true);
            tMapView.setCompassModeFix(true);
//            tMapView.setZoomLevel(16);
        } else {
            manager.closeGps();
            manager.setOnLocationChangeListener(null);

            tMapView.setTrackingMode(false);
            tMapView.setSightVisible(false);
            tMapView.setCompassModeFix(false);
        }
    }

    private TMapGpsManager.OnLocationChangedListener locationListener = new TMapGpsManager.OnLocationChangedListener() {
        @Override
        public void onLocationChange(Location location) {
            if (location != null) {
                currentLocation = location;
                tMapView.setLocationPoint(location.getLatitude(), location.getLongitude());

                if (isStart) {
                    isStart = false;
                    new Handler().postDelayed(() -> {
                        crossfade();
                    }, 1000);
                }
            }
        }
    };

    private void crossfade() {
        mainLayout.setAlpha(0f);
        mainLayout.setVisibility(View.VISIBLE);

        mainLayout.animate()
                .alpha(1f)
                .setDuration(shortAnimationDuration)
                .setListener(null);

        loadingLayout.animate()
                .alpha(0f)
                .setDuration(shortAnimationDuration)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        loadingLayout.setVisibility(View.GONE);
                    }
                });
    }
}


class MyAdapter extends BaseAdapter {

    Context context;
    int layoutId;
    ArrayList<TMapPOIItem> myDataArr;
    LayoutInflater Inflater;
    MyAdapter(Context _context, int _layout, ArrayList<TMapPOIItem> _myDataArr) {
        context = _context;
        layoutId = _layout;
        myDataArr = _myDataArr;
        Inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return myDataArr.size();
    }

    @Override
    public Object getItem(int i) {
        return myDataArr.get(i).name;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = Inflater.inflate(layoutId, viewGroup, false);
        }

        TextView nameView = view.findViewById(R.id.name);
        nameView.setText(myDataArr.get(i).getPOIName());

        TextView addressView = view.findViewById(R.id.address);
        addressView.setText(myDataArr.get(i).getPOIAddress());

        return view;
    }
}

