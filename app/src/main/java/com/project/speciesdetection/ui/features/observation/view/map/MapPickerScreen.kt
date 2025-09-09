package com.project.speciesdetection.ui.features.observation.view.map

import android.Manifest
import android.content.pm.PackageManager
import android.preference.PreferenceManager
import android.util.Log
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.google.android.gms.location.LocationServices
import com.project.speciesdetection.R
import com.project.speciesdetection.ui.composable.common.AppSearchBar
import com.project.speciesdetection.ui.features.observation.viewmodel.map.NewMapPickerViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapPickerScreen(
    navController: NavController,
    onLocationPicked: (Double, Double, String, String, String) -> Unit,
    viewModel: NewMapPickerViewModel = hiltViewModel()
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val mapView = remember { MapView(context) }
    val fusedLocationProviderClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var mapCenter by remember { mutableStateOf<GeoPoint?>(null) }

    val hasLocationPermission = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var expanded by remember { mutableStateOf(false) }


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
        } else {
            Toast.makeText(context, "Bạn cần cấp quyền vị trí để dùng bản đồ", Toast.LENGTH_LONG)
                .show()
        }
    }

    // Gọi khi mở màn
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
    }



    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.AutoMirrored.Default.KeyboardArrowLeft, null,
                            modifier = Modifier.clickable { navController.popBackStack() }
                        )
                        Column {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 10.dp)
                            ) {
                                item {
                                    Text(
                                        if (uiState.selectedAddress != "loading") uiState.selectedAddress else stringResource(R.string.location_init),
                                        //maxLines = 1,
                                        //overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                            if (uiState.selectedDisplayName.isNotEmpty())
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 10.dp)
                                ) {
                                    item {
                                        Text(
                                            uiState.selectedDisplayName,
                                            //maxLines = 1,
                                            //overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                        }
                    }


                }
            )
        }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    mapView.setTileSource(TileSourceFactory.MAPNIK)
                    mapView.setMultiTouchControls(true)
                    mapView.controller.setZoom(17.0)
                    mapView

                },
                update = {

                    mapCenter = it.mapCenter as? GeoPoint
                    /*Log.i("a", mapCenter.toString())*/
                    it.setMapListener(object : org.osmdroid.events.MapListener {
                        var isMovingProgrammatically = false
                        var lastMoveTime: Long = 0

                        override fun onScroll(event: org.osmdroid.events.ScrollEvent?): Boolean {
                            // Cơ chế debounce đơn giản để tránh gọi API quá nhiều
                            if (System.currentTimeMillis() - lastMoveTime > 1000) {
                                Log.i("a", it.mapCenter.toString())
                                lastMoveTime = System.currentTimeMillis()
                                (it.mapCenter as? GeoPoint)?.let { it1 ->
                                    viewModel.reverseGeocode(
                                        it1
                                    )
                                }

                            }
                            return true
                        }

                        override fun onZoom(event: org.osmdroid.events.ZoomEvent?): Boolean {
                            if (System.currentTimeMillis() - lastMoveTime > 1000) {
                                Log.i("a", it.mapCenter.toString())
                                (it.mapCenter as? GeoPoint)?.let { it1 ->
                                    viewModel.reverseGeocode(
                                        it1
                                    )
                                }
                            }
                            return true
                        }
                    })
                }
            )

            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Center marker",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(48.dp)
            )

            Row(modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp)) {
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
                                        mapView.controller.setZoom(17.0)
                                    }
                                }
                            } catch (e: SecurityException) {
                                Toast.makeText(
                                    context,
                                    "Không thể truy cập vị trí: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
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
                    .padding(innerPadding)
                    .padding(horizontal = 10.dp)
                    .padding(top = 10.dp)
                    .semantics { isTraversalGroup = true }
            ) {
                AppSearchBar(
                    query = searchQuery,
                    onQueryChanged = {
                        if (!expanded && searchQuery!="") expanded = true
                        viewModel.onQueryChanged(it) },
                    onClearQuery = { viewModel.onQueryChanged("") },
                    onSearchAction = {
                        viewModel.onQueryChanged(searchQuery)
                    },
                    hint = stringResource(R.string.enter_address),
                    modifier = Modifier
                        .fillMaxWidth()
                        /*.onFocusChanged { focusState ->
                            if (focusState.isFocused) expanded = true
                        }*/,
                    backgroundColor = MaterialTheme.colorScheme.surface
                )
                AnimatedVisibility(
                    expanded && searchQuery.isNotEmpty(),
                    modifier = Modifier
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                        .clip(RoundedCornerShape(10.dp, 10.dp, 10.dp, 10.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface)
                            .clip(RoundedCornerShape(10.dp, 10.dp, 10.dp, 10.dp))
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
                            LazyColumn {
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
                                                mapView.controller.setZoom(17.0)
                                                //onResultClick(resultText)

                                                keyboardController?.hide()
                                                focusManager.clearFocus()
                                                expanded = false
                                            }
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }


                /*

                SearchBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { traversalIndex = 0f },
                    inputField = {
                        // Customizable input field implementation

                        SearchBarDefaults.InputField(
                            query = searchQuery,
                            onQueryChange = {viewModel.onQueryChanged(it)},
                            onSearch = {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                                expanded = false
                            },
                            expanded = expanded,
                            onExpandedChange = { expanded = it },
                            placeholder = {Text("Nhap dia chi")},
                            leadingIcon = {
                                if (expanded)
                                    Icon(
                                        Icons.AutoMirrored.Default.ArrowBack, null,
                                        Modifier.clickable {
                                            expanded = false
                                            keyboardController?.hide()
                                            focusManager.clearFocus()
                                        })
                                else
                                    Icon(Icons.Default.Search, null) },

                            )
                    },
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                ) {
                    // Show search results in a lazy column for better performance

                }*/
            }

            Button(
                onClick = {
                    var tempAddress = "${
                        if (uiState.address["city"] == null)
                            removeLastWord(uiState.address["state"] ?: "")
                        else uiState.address["city"]
                    }" + ", ${uiState.address["country_code"]?.uppercase() ?: ""}"
                    Log.i("sa", tempAddress)
                    onLocationPicked(
                        uiState.selectedGeoPoint?.latitude ?: 0.0,
                        uiState.selectedGeoPoint?.longitude ?: 0.0,
                        uiState.selectedAddress,
                        uiState.selectedDisplayName,
                        tempAddress
                    )
                    navController.popBackStack()
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                enabled = uiState.selectedAddress != "Đang tìm vị trí của bạn..."
            ) {
                Text(stringResource(R.string.select_location))
            }
        }
    }


}

fun removeLastWord(input: String): String {
    // 1. Dùng trim() để loại bỏ các khoảng trắng thừa ở đầu và cuối chuỗi
    // 2. Tách chuỗi thành một danh sách các từ.
    //    Dùng Regex("\\s+") để xử lý trường hợp có nhiều khoảng trắng liền nhau.
    val words = input.trim().split(Regex("\\s+"))

    // 3. Nếu chuỗi chỉ có 1 từ hoặc là chuỗi rỗng, trả về chuỗi gốc
    if (words.size <= 1) {
        return input.trim() // Trả về chuỗi đã được trim
    }

    // 4. Bỏ từ cuối cùng trong danh sách và nối các từ còn lại bằng một khoảng trắng
    return words.dropLast(1).joinToString(" ")
}