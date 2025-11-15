package com.example.revd_up.presentation.views.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.launch

// --------------------------------------------------------------------------------
// DATA MODEL (Defined here since it's only used by OnboardingScreen)
// --------------------------------------------------------------------------------

data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: @Composable () -> Unit
)

// --------------------------------------------------------------------------------
// COMPOSABLES
// --------------------------------------------------------------------------------

@OptIn(ExperimentalPagerApi::class)
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pages = remember {
        listOf(
            OnboardingPage(
                "Welcome to REVD_UP!",
                "Track your fitness goals, log your workouts, and connect with friends.",
                icon = { Icon(rememberVectorPainter(Icons.Filled.Person), contentDescription = "Welcome Icon", modifier = Modifier.size(96.dp), tint = MaterialTheme.colorScheme.primary) }
            ),
            OnboardingPage(
                "Customize Your Journey",
                "Personalized workout plans and nutrition advice tailored just for you.",
                icon = { Icon(rememberVectorPainter(Icons.Filled.Lock), contentDescription = "Customize Icon", modifier = Modifier.size(96.dp), tint = MaterialTheme.colorScheme.secondary) }
            ),
            OnboardingPage(
                "Community Challenges",
                "Join competitive challenges and keep yourself motivated with our global community.",
                icon = { Icon(rememberVectorPainter(Icons.Filled.Person), contentDescription = "Community Icon", modifier = Modifier.size(96.dp), tint = MaterialTheme.colorScheme.tertiary) }
            )
        )
    }
    val pagerState = rememberPagerState()
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize()) {
        HorizontalPager(
            count = pages.size,
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { pageIndex ->
            OnboardingPageContent(page = pages[pageIndex])
        }

        // Indicator dots
        Row(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(pages.size) { iteration ->
                val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else Color.LightGray
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .background(color, shape = RoundedCornerShape(50))
                        .size(10.dp)
                )
            }
        }

        // Navigation buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (pagerState.currentPage > 0) {
                Button(onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } }) { Text("Previous") }
            } else { Spacer(modifier = Modifier.width(1.dp)) }

            if (pagerState.currentPage < pages.size - 1) {
                Button(onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } }) { Text("Next") }
            } else {
                Button(
                    onClick = onFinish,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) { Text("Get Started!") }
            }
        }
    }
}

@Composable
fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        page.icon()
        Spacer(modifier = Modifier.height(48.dp))
        Text(text = page.title, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = page.description, fontSize = 18.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f), modifier = Modifier.fillMaxWidth(0.8f))
    }
}