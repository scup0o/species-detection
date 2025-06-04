package com.project.speciesdetection.ui.features.encyclopedia_detail_screen.view

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.project.speciesdetection.R
import com.project.speciesdetection.core.navigation.AppScreen
import com.project.speciesdetection.core.theme.spacing
import com.project.speciesdetection.data.model.species.DisplayableSpecies
import com.project.speciesdetection.ui.composable.common.CustomActionButton
import com.project.speciesdetection.ui.composable.common.CustomChip
import com.project.speciesdetection.ui.composable.common.ErrorScreenPlaceholder
import com.project.speciesdetection.ui.composable.common.HyperlinkText
import com.project.speciesdetection.ui.composable.common.species.IUCNConservationStatusView
import com.project.speciesdetection.ui.composable.common.species.SpeciesClassification
import com.project.speciesdetection.ui.features.auth.viewmodel.AuthViewModel
import com.project.speciesdetection.ui.features.encyclopedia_detail_screen.viewmodel.EncyclopediaDetailViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun EncyclopediaDetailScreen(
    //species: DisplayableSpecies,
    observationImage : Uri? = null,
    navController: NavController,
    authViewModel: AuthViewModel,
    containerColor : Color = MaterialTheme.colorScheme.surfaceContainer,
    speciesDetailViewModel : EncyclopediaDetailViewModel = hiltViewModel()
) {
    val uiState by speciesDetailViewModel.uiState.collectAsStateWithLifecycle()
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val sourceList by speciesDetailViewModel.sourceList.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()

    val listState = rememberLazyListState()
    val listCurrentState by remember { derivedStateOf { listState.firstVisibleItemIndex } }
    val chipListState: LazyListState = rememberLazyListState()
    val indexList = listOf(
        stringResource(R.string.species_detail_summary) to 0,
        stringResource(R.string.species_detail_classification) to 1,
        stringResource(R.string.species_detail_physical) to 2,
        stringResource(R.string.species_detail_distribution) to 3,
        stringResource(R.string.species_detail_habitat) to 4,
        stringResource(R.string.species_detail_behavior) to 5,
        stringResource(R.string.species_detail_source)+" & "+stringResource(R.string.species_detail_more_info) to 6,
    )


    /*LaunchedEffect(listCurrentState) {
        Log.i("check chekc", listCurrentState.toString())
        if (chipListState.firstVisibleItemIndex.div(3)==0)
            coroutineScope.launch {
                chipListState.animateScrollToItem(index = listCurrentState)
            }

    }*/

    when (uiState){
        EncyclopediaDetailViewModel.UiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize().background(containerColor)){
                Row(modifier = Modifier.align(Alignment.Center)) {
                    CircularProgressIndicator()
                }
            }

        }

        is EncyclopediaDetailViewModel.UiState.Success ->{
            val species = (uiState as EncyclopediaDetailViewModel.UiState.Success).species
            val pagerState = rememberPagerState(pageCount = { species.imageURL.size })

            Scaffold(
                containerColor = containerColor,
                bottomBar = {
                    Column(
                        modifier = Modifier.padding(15.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (authState.currentUser != null) {
                            CustomActionButton(
                                onClick = {},
                                text =
                                    if (observationImage != null) stringResource(R.string.species_detail_record)
                                    else stringResource(R.string.species_detail_add),
                            )

                        } else {
                            CustomActionButton(
                                onClick = {
                                    navController.popBackStack(
                                        AppScreen.LoginScreen.route,
                                        inclusive = true,
                                        saveState = false
                                    )
                                    navController.navigate(AppScreen.LoginScreen.route) {
                                        launchSingleTop = true
                                    }
                                },
                                text =
                                    if (observationImage != null) stringResource(R.string.species_detail_login_to_record)
                                    else stringResource(R.string.species_detail_login_to_add),
                            )
                        }
                        CustomActionButton(
                            onClick = {},
                            text = stringResource(R.string.species_detail_view_observation),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            borderColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    }

                }
            ) { innerPadding ->
                LazyColumn(
                    modifier = Modifier.padding(innerPadding),
                    state = listState
                ) {
                    item {
                        Box(Modifier.fillMaxWidth()) {
                            HorizontalPager(pagerState) { page ->
                                GlideImage(
                                    model = species.imageURL[page], null,
                                    failure = placeholder(R.drawable.image_not_available),
                                    loading = placeholder(R.drawable.image_not_available),
                                    contentScale = ContentScale.FillWidth,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                )

                            }

                            Button(
                                onClick = { navController.popBackStack() },
                                contentPadding = PaddingValues(0.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White.copy(0.3f)
                                ),
                                modifier = Modifier.padding(5.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = "Back",
                                )
                            }

                            if (pagerState.pageCount>1){
                                LazyRow(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter),
                                    contentPadding = PaddingValues(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    items(pagerState.pageCount) { index ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(100))
                                                .size(
                                                    if (pagerState.currentPage == index) 12.dp
                                                    else 10.dp
                                                )
                                                .background(
                                                    if (pagerState.currentPage == index) Color.White
                                                    else Color.Transparent
                                                )
                                                .border(
                                                    width = if (pagerState.currentPage == index) 4.dp else 2.dp,
                                                    color = Color.White,
                                                    shape = RoundedCornerShape(100)
                                                )
                                                .clickable {
                                                    coroutineScope.launch {
                                                        pagerState.animateScrollToPage(index)
                                                    }

                                                }
                                        )
                                        Spacer(Modifier.width(5.dp))
                                    }
                                }
                            }


                            /*
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        val previousPage = (pagerState.currentPage - 1).coerceAtLeast(0)
                                        pagerState.animateScrollToPage(previousPage)
                                    }
                                },
                                modifier = Modifier.align(Alignment.CenterStart)
                            ) {
                                Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Previous Image")
                            }

                            // Next Button
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        val nextPage = (pagerState.currentPage + 1).coerceAtMost(pagerState.pageCount-1)
                                        pagerState.animateScrollToPage(nextPage)
                                    }
                                },
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Image")
                            }*/
                        }

                        Column( modifier = Modifier.padding(MaterialTheme.spacing.m)) {
                            Row(verticalAlignment = Alignment.CenterVertically){
                                Text(
                                    species.localizedName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    ", "+stringResource(R.string.species_family_description) + " ",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    species.localizedFamily,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(verticalAlignment = Alignment.Bottom){
                                Text(
                                    stringResource(R.string.species_detail_scientific_name)+": ",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline
                                    )

                                Text(
                                    species.getScientificName()!!,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }


                            Text(
                                species.localizedSummary.firstOrNull()?: stringResource(R.string.iucn_status_unknown),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top=10.dp)

                            )
                            Row {
                                sourceList.forEach{ source ->
                                    if (source.listIndex==0){
                                        Text(
                                            "[${source.orderAdded}]",
                                            color = MaterialTheme.colorScheme.tertiary,
                                            modifier = Modifier.clickable{
                                                coroutineScope.launch {
                                                    listState.scrollToItem(index = indexList.size-1, scrollOffset = 1000)
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            Text(
                                stringResource(R.string.species_detail_conservation_status),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = MaterialTheme.spacing.m, bottom = MaterialTheme.spacing.xs)
                            )

                            IUCNConservationStatusView(species.conservation)
                        }

                    }

                    stickyHeader {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.m),
                            state = chipListState
                        ) {
                            items(
                                indexList,
                                key = {it.second}){ section->
                                CustomChip(
                                    title = section.first,
                                    onClick = {
                                        coroutineScope.launch {
                                            listState.scrollToItem(index = section.second, scrollOffset = if (section.second==indexList.size-1) 1000 else 0)
                                        }
                                    },
                                    isSelected = listCurrentState == section.second
                                )
                            }

                        }
                    }

                    items(6) { index ->
                            Column(
                                modifier = Modifier.padding(horizontal = MaterialTheme.spacing.m)
                            ) {
                                Text(
                                    if(indexList[index+1].first==stringResource(R.string.species_detail_source)+" & "+stringResource(R.string.species_detail_more_info)) stringResource(R.string.species_detail_source) else indexList[index+1].first,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = MaterialTheme.spacing.m, bottom = MaterialTheme.spacing.xs)
                                )
                                if (indexList[index+1].first== stringResource(R.string.species_detail_classification)){
                                    SpeciesClassification(species)
                                }
                                else{
                                    if(indexList[index+1].first==stringResource(R.string.species_detail_source)+" & "+stringResource(R.string.species_detail_more_info)){
                                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            sourceList.forEachIndexed{ index, source ->
                                                Row{
                                                    HyperlinkText(
                                                        fullText = "[${index+1}] "+source.firstValue+" "+source.secondValue,
                                                        linkText = source.secondValue,
                                                        url = source.secondValue
                                                    )
                                                }

                                            }

                                            Text(
                                                stringResource(R.string.species_detail_more_info),
                                                modifier = Modifier.padding(top = MaterialTheme.spacing.m),
                                                fontWeight = FontWeight.Bold,
                                                )

                                            species.info.forEach { info ->
                                                when (info.key){
                                                    "wikipedia" -> {
                                                        HyperlinkText(
                                                            fullText = "Wikipedia: " + info.value,
                                                            linkText = info.value,
                                                            url = info.value,
                                                        )
                                                    }

                                                    "adw" -> {
                                                        HyperlinkText(
                                                            fullText = "ADW: " + info.value,
                                                            linkText = info.value,
                                                            url = info.value,
                                                        )
                                                    }
                                                    "worldlandtrust" -> {
                                                        HyperlinkText(
                                                            fullText = "World Land Trust: " + info.value,
                                                            linkText = info.value,
                                                            url = info.value,
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                    }
                                    else {
                                        Text(
                                            text = when (indexList[index + 1].first) {
                                                //"Summary" -> species.localizedSummary.firstOrNull() ?: ""
                                                //"Taxonomy" -> species.localizedPhysical.firstOrNull() ?: ""
                                                stringResource(R.string.species_detail_physical) -> species.localizedPhysical.firstOrNull()
                                                    ?: ""

                                                stringResource(R.string.species_detail_distribution) -> species.localizedDistribution.firstOrNull()
                                                    ?: ""

                                                stringResource(R.string.species_detail_habitat) -> species.localizedHabitat.firstOrNull()
                                                    ?: ""

                                                stringResource(R.string.species_detail_behavior) -> species.localizedBehavior.firstOrNull()
                                                    ?: ""
                                                //"Source" -> species.localizedBehavior.firstOrNull() ?: ""
                                                //"More Info" -> species.localizedBehavior.firstOrNull() ?: ""
                                                else -> "Đang cập nhật..."
                                            },
                                            style = MaterialTheme.typography.bodyMedium
                                        )


                                        Row {
                                            sourceList.forEach{ source ->
                                                if (source.listIndex==indexList[index+1].second){
                                                    Text(
                                                        "[${source.orderAdded}]",
                                                        color = MaterialTheme.colorScheme.tertiary,
                                                        modifier = Modifier.clickable{
                                                            coroutineScope.launch {
                                                                listState.scrollToItem(index = indexList.size-1, scrollOffset = 1000)
                                                            }
                                                        }
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
        }

        is EncyclopediaDetailViewModel.UiState.Error ->{
            ErrorScreenPlaceholder(
                onClick = {
                    speciesDetailViewModel.getSpeciesDetailed()
                }
            )
        }
    }
}