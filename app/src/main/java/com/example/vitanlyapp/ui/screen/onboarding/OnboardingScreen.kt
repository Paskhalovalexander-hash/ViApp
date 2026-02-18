package com.example.vitanlyapp.ui.screen.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vitanlyapp.ui.design.AppColorSchemes
import com.example.vitanlyapp.ui.design.DesignTokens
import androidx.core.view.WindowCompat
import kotlinx.coroutines.launch

/**
 * Экран онбординга для первоначальной настройки профиля пользователя.
 */
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scheme = AppColorSchemes.Classic
    val scope = rememberCoroutineScope()

    // Тёмные иконки строки состояния на светлом фоне онбординга
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
    }

    val pagerState = rememberPagerState(
        initialPage = state.stepIndex,
        pageCount = { state.totalSteps }
    )

    // Синхронизация pager с ViewModel
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            viewModel.goToStep(page)
        }
    }

    // Синхронизация ViewModel с pager
    LaunchedEffect(state.stepIndex) {
        if (pagerState.currentPage != state.stepIndex) {
            pagerState.animateScrollToPage(state.stepIndex)
        }
    }

    // Фон на весь экран (включая строку состояния) для читаемости иконок
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.screenBackground)
    ) {
        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .fillMaxSize()
                .padding(DesignTokens.screenPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Заголовок
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Настройка профиля",
                fontSize = 28.sp,
                fontFamily = DesignTokens.fontFamilyPlank,
                fontWeight = FontWeight.Normal,
                color = scheme.textColor
            )

            // Индикатор прогресса
            Spacer(modifier = Modifier.height(24.dp))
            StepIndicator(
                currentStep = state.stepIndex,
                totalSteps = state.totalSteps,
                scheme = scheme
            )

            // Контент страницы
            Spacer(modifier = Modifier.height(32.dp))
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                userScrollEnabled = false // Свайпы отключены, навигация только кнопками
            ) { page ->
                OnboardingPage(
                    step = OnboardingStep.entries[page],
                    state = state,
                    onBirthYearChange = viewModel::updateBirthYear,
                    onGenderChange = viewModel::updateGender,
                    onHeightChange = viewModel::updateHeight,
                    onWeightChange = viewModel::updateWeight,
                    onActivityChange = viewModel::updateActivityLevel,
                    onGoalChange = viewModel::updateGoal,
                    scheme = scheme
                )
            }

            // Кнопки навигации
            Spacer(modifier = Modifier.height(24.dp))
            NavigationButtons(
                isFirstStep = state.isFirstStep,
                isLastStep = state.isLastStep,
                isSaving = state.isSaving,
                onBack = {
                    scope.launch {
                        viewModel.previousStep()
                    }
                },
                onNext = {
                    scope.launch {
                        viewModel.nextStep()
                    }
                },
                onComplete = {
                    viewModel.saveProfile(onComplete)
                },
                scheme = scheme
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun StepIndicator(
    currentStep: Int,
    totalSteps: Int,
    scheme: com.example.vitanlyapp.ui.design.AppColorScheme
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalSteps) { index ->
            val isActive = index <= currentStep
            val targetColor = if (isActive) scheme.textColor else scheme.borderSoft
            val animatedColor by animateColorAsState(
                targetValue = targetColor,
                animationSpec = tween(300),
                label = "step_indicator_color"
            )

            Box(
                modifier = Modifier
                    .size(if (index == currentStep) 12.dp else 8.dp)
                    .clip(CircleShape)
                    .background(animatedColor)
            )
        }
    }
}

@Composable
private fun NavigationButtons(
    isFirstStep: Boolean,
    isLastStep: Boolean,
    isSaving: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onComplete: () -> Unit,
    scheme: com.example.vitanlyapp.ui.design.AppColorScheme
) {
    val buttonShape = RoundedCornerShape(16.dp)
    val gradientBorder = Brush.verticalGradient(
        colors = listOf(
            scheme.borderGradientTop,
            scheme.borderGradientBottom
        )
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Кнопка "Назад"
        if (!isFirstStep) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .clip(buttonShape)
                    .background(scheme.surfaceInput)
                    .border(1.dp, gradientBorder, buttonShape)
                    .clickable(enabled = !isSaving) { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад",
                        tint = scheme.textColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Назад",
                        fontSize = 18.sp,
                        fontFamily = DesignTokens.fontFamilyPlank,
                        fontWeight = FontWeight.Normal,
                        color = scheme.textColor
                    )
                }
            }
        }

        // Кнопка "Далее" или "Готово"
        Box(
            modifier = Modifier
                .weight(1f)
                .height(56.dp)
                .clip(buttonShape)
                .background(
                    if (isLastStep) scheme.shapeGradientAccent
                    else scheme.shapeGradientCream
                )
                .border(1.dp, gradientBorder, buttonShape)
                .clickable(enabled = !isSaving) {
                    if (isLastStep) onComplete() else onNext()
                },
            contentAlignment = Alignment.Center
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = scheme.textColor,
                    strokeWidth = 2.dp
                )
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isLastStep) "Готово" else "Далее",
                        fontSize = 18.sp,
                        fontFamily = DesignTokens.fontFamilyPlank,
                        fontWeight = FontWeight.Normal,
                        color = scheme.textColor
                    )
                    Icon(
                        imageVector = if (isLastStep) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = if (isLastStep) "Готово" else "Далее",
                        tint = scheme.textColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
