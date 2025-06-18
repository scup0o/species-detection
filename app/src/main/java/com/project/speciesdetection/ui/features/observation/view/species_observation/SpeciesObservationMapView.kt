package com.project.speciesdetection.ui.features.observation.view.species_observation

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.preference.PreferenceManager
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.location.LocationServices
import com.project.speciesdetection.data.model.observation.Observation
import com.project.speciesdetection.ui.features.observation.viewmodel.map.NewMapPickerViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.util.GeoPoint as OsmGeoPoint
import org.osmdroid.views.overlay.Marker

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeciesObservationMapView(
    observationList: List<Observation>,
    onMarkerClick: (GeoPoint) -> Unit,
    viewModel: NewMapPickerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        Configuration.getInstance()
            .load(context, PreferenceManager.getDefaultSharedPreferences(context))
    }
    var expanded by remember { mutableStateOf(false) }
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()

    //val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    //val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    //var mapCenter by remember { mutableStateOf<GeoPoint?>(null) }

    val hasLocationPermission = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val fusedLocationProviderClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    //var expanded by remember { mutableStateOf(false) }


    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasLocationPermission.value = granted
        if (granted) {
            try {
                fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
                    location?.let {
                        val currentPoint = GeoPoint(it.latitude, it.longitude)
                        Log.i("c", currentPoint.toString())
                        mapView.controller.setCenter(currentPoint)
                        mapView.controller.setZoom(17.0)
                    }
                }
            } catch (e: SecurityException) {
                Toast.makeText(context, "Không thể truy cập vị trí: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Bạn cần cấp quyền vị trí để dùng bản đồ", Toast.LENGTH_LONG).show()
        }
    }

    /*// Gọi khi mở màn
    LaunchedEffect(Unit) {
        Configuration.getInstance()
            .load(context, PreferenceManager.getDefaultSharedPreferences(context))
        if (hasLocationPermission.value) {
            try {
                fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
                    location?.let {
                        val currentPoint = GeoPoint(it.latitude, it.longitude)
                        Log.i("c", currentPoint.toString())
                        mapView.controller.setCenter(currentPoint)
                        mapView.controller.setZoom(17.0)
                        mapCenter = currentPoint
                    }
                }
            } catch (e: SecurityException) {
                Toast.makeText(
                    context,
                    "Không thể truy cập vị trí: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
            //permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            try {
                fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
                    location?.let {
                        val currentPoint = GeoPoint(it.latitude, it.longitude)
                        Log.i("d", currentPoint.toString())
                        mapView.controller.setCenter(currentPoint)
                        mapView.controller.setZoom(17.0)
                        mapCenter = currentPoint
                    }
                }
            } catch (e: SecurityException) {
                Toast.makeText(
                    context,
                    "Không thể truy cập vị trí: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }*/



    Scaffold(
    ) {
        Box(Modifier.fillMaxSize()) {

            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {


                        mapView.layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                    mapView.setTileSource(TileSourceFactory.MAPNIK)
                    mapView.setMultiTouchControls(true)

                        // Lọc ra các Observation có location hợp lệ
                        val validObservations = observationList.filter { it.location != null }

                        if (validObservations.isNotEmpty()) {
                            // Center vào vị trí đầu tiên
                            val centerPoint = OsmGeoPoint(
                                validObservations.first().location!!.latitude,
                                validObservations.first().location!!.longitude
                            )
                            mapView.controller.setZoom(15.0)
                            mapView.controller.setCenter(centerPoint)

                            // Thêm marker cho từng Observation
                            validObservations.forEachIndexed { index, obs ->
                                val loc = obs.location!!
                                val point = OsmGeoPoint(loc.latitude, loc.longitude)
                                val marker = Marker(mapView).apply {
                                    position = point
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                    //title = obs.name

                                    snippet = "ID: ${obs.id}"
                                    setOnMarkerClickListener { _, _ ->
                                        onMarkerClick(point)
                                        true // sự kiện đã xử lý
                                    }
                                }
                                mapView.overlays.add(marker)
                            }
                        }
                    mapView

                },
                update = {

                }
            )
            Row(modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp)){
                IconButton(
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(0.8f),
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    onClick = {
                        if (hasLocationPermission.value) {
                            try {
                                fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
                                    location?.let {
                                        val currentPoint = GeoPoint(it.latitude, it.longitude)
                                        Log.i("c", currentPoint.toString())
                                        mapView.controller.setCenter(currentPoint)
                                        mapView.controller.setZoom(15.0)
                                    }
                                }
                            } catch (e: SecurityException) {
                                Toast.makeText(context, "Không thể truy cập vị trí: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                        else{
                            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    }
                ) {
                    Icon(
                        Icons.Default.Home, null,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
                    .padding(top = 10.dp)
                    .semantics { isTraversalGroup = true }
            ) {
                Surface(
                    shadowElevation = 30.dp,
                    tonalElevation = 0.dp,
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(
                        25
                    ),
                    modifier = Modifier.padding(start = 10.dp)
                ) {
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) expanded = true
                            },
                        value = searchQuery,
                        onValueChange = {
                            viewModel.onQueryChanged(it)
                        },
                        placeholder = {
                            Text(
                                "Nhap dia chi",
                                color = MaterialTheme.colorScheme.outline,
                                fontStyle = FontStyle.Italic
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, null)
                            /*if (expanded)
                                Icon(
                                    Icons.AutoMirrored.DefaulVit.ArrowBack, null,
                                    Modifier.clickable {
                                        expanded = false
                                        keyboardController?.hide()
                                        focusManager.clearFocus()
                                    })
                            else
                                Icon(Icons.Default.Search, null)*/
                        },
                        shape = RoundedCornerShape(25),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent
                        )
                    )
                }
                AnimatedVisibility(
                    expanded && searchQuery.isNotEmpty(),
                    modifier = Modifier.padding(start = 10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color.White)
                            .clip(RoundedCornerShape(0, 0, 10, 10))
                    ) {
                        if (uiState.isSearching) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 20.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        } else {
                            LazyColumn() {
                                items(count = searchResults.size) { index ->
                                    val resultText = searchResults[index]
                                    ListItem(
                                        headlineContent = { Text(resultText.name) },
                                        supportingContent = { Text(resultText.displayName) },
                                        //leadingContent = leadingContent,
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                        modifier = Modifier
                                            .clickable {
                                                val geoPoint = GeoPoint(
                                                    resultText.lat.toDouble(),
                                                    resultText.lon.toDouble()
                                                )
                                                mapView.controller.setCenter(geoPoint)  // Di chuyển bản đồ đến vị trí này
                                                mapView.controller.setZoom(15.0)
                                                //onResultClick(resultText)
                                                expanded = false
                                                keyboardController?.hide()
                                                focusManager.clearFocus()
                                            }
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

        }
    }


}