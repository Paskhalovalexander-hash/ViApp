package com.example.vitanlyapp.ui.design

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp

/**
 * Общий модификатор для плашек/контейнеров (кроме плиток).
 * Тень рисуется по форме, контент обрезается по форме — без видимого прямоугольника.
 * Цвет и прозрачность задаются параметрами.
 */
fun Modifier.shapedSurface(
    shape: Shape,
    elevation: Dp,
    backgroundColor: Color
): Modifier = this
    .clip(shape)
    .shadow(elevation, shape)
    .background(backgroundColor, shape)
