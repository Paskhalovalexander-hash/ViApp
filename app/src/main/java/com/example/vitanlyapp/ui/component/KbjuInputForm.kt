package com.example.vitanlyapp.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.vitanlyapp.domain.model.KBJUData
import com.example.vitanlyapp.ui.design.AppColorScheme
import com.example.vitanlyapp.ui.design.DesignTokens
import com.example.vitanlyapp.ui.design.LocalAppColorScheme
import com.example.vitanlyapp.ui.design.shapedSurface

private const val MAX_VALUE = 9999
private const val MIN_VALUE = 0

@Composable
fun KbjuInputForm(
    data: KBJUData,
    onDataChange: (KBJUData) -> Unit,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAppColorScheme.current
    val sectionSpacing = if (compact) 10.dp else 20.dp
    val rowSpacing = if (compact) 6.dp else 10.dp
    val fieldSpacing = if (compact) 6.dp else 12.dp
    val fieldHeight = if (compact) 34.dp else 44.dp
    val cornerRadius = if (compact) 10.dp else 16.dp
    val labelSpacing = if (compact) 4.dp else 6.dp

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(sectionSpacing)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(fieldSpacing)) {
            Text(
                text = "Текущие значения",
                color = scheme.textColor,
                fontFamily = DesignTokens.fontFamilyPlank,
                fontWeight = FontWeight.Thin,
                fontSize = DesignTokens.fontSizeHeading
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(rowSpacing)
            ) {
                KbjuField(label = "К", value = data.currentCalories, modifier = Modifier.weight(1f), fieldHeight = fieldHeight, cornerRadius = cornerRadius, labelSpacing = labelSpacing, scheme = scheme) {
                    onDataChange(data.copy(currentCalories = it))
                }
                KbjuField(label = "Б", value = data.currentProtein, modifier = Modifier.weight(1f), fieldHeight = fieldHeight, cornerRadius = cornerRadius, labelSpacing = labelSpacing, scheme = scheme) {
                    onDataChange(data.copy(currentProtein = it))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(rowSpacing)
            ) {
                KbjuField(label = "Ж", value = data.currentFat, modifier = Modifier.weight(1f), fieldHeight = fieldHeight, cornerRadius = cornerRadius, labelSpacing = labelSpacing, scheme = scheme) {
                    onDataChange(data.copy(currentFat = it))
                }
                KbjuField(label = "У", value = data.currentCarbs, modifier = Modifier.weight(1f), fieldHeight = fieldHeight, cornerRadius = cornerRadius, labelSpacing = labelSpacing, scheme = scheme) {
                    onDataChange(data.copy(currentCarbs = it))
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(fieldSpacing)) {
            Text(
                text = "Максимум",
                color = scheme.textColor,
                fontFamily = DesignTokens.fontFamilyPlank,
                fontWeight = FontWeight.Thin,
                fontSize = DesignTokens.fontSizeHeading
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(rowSpacing)
            ) {
                KbjuField(label = "К", value = data.maxCalories, modifier = Modifier.weight(1f), fieldHeight = fieldHeight, cornerRadius = cornerRadius, labelSpacing = labelSpacing, scheme = scheme) {
                    onDataChange(data.copy(maxCalories = it))
                }
                KbjuField(label = "Б", value = data.maxProtein, modifier = Modifier.weight(1f), fieldHeight = fieldHeight, cornerRadius = cornerRadius, labelSpacing = labelSpacing, scheme = scheme) {
                    onDataChange(data.copy(maxProtein = it))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(rowSpacing)
            ) {
                KbjuField(label = "Ж", value = data.maxFat, modifier = Modifier.weight(1f), fieldHeight = fieldHeight, cornerRadius = cornerRadius, labelSpacing = labelSpacing, scheme = scheme) {
                    onDataChange(data.copy(maxFat = it))
                }
                KbjuField(label = "У", value = data.maxCarbs, modifier = Modifier.weight(1f), fieldHeight = fieldHeight, cornerRadius = cornerRadius, labelSpacing = labelSpacing, scheme = scheme) {
                    onDataChange(data.copy(maxCarbs = it))
                }
            }
        }
    }
}

@Composable
private fun KbjuField(
    label: String,
    value: Int,
    modifier: Modifier = Modifier,
    fieldHeight: Dp = 44.dp,
    cornerRadius: Dp = 16.dp,
    labelSpacing: Dp = 6.dp,
    scheme: AppColorScheme,
    onValueChange: (Int) -> Unit
) {
    val inputShape = RoundedCornerShape(cornerRadius)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(labelSpacing)
    ) {
        Text(
            text = label,
            color = scheme.textColor.copy(alpha = 0.7f),
            fontFamily = DesignTokens.fontFamilyPlank,
            fontWeight = FontWeight.Thin,
            fontSize = DesignTokens.fontSizeLabel
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(fieldHeight)
                .shapedSurface(
                    shape = inputShape,
                    elevation = DesignTokens.inputElevation,
                    backgroundColor = scheme.surfaceInput
                )
                .border(DesignTokens.tileBorderWidth, scheme.borderLight, inputShape),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = value.toString(),
                onValueChange = { s ->
                    val digits = s.filter { it.isDigit() }
                    val n = digits.toIntOrNull() ?: 0
                    onValueChange(n.coerceIn(MIN_VALUE, MAX_VALUE))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                textStyle = TextStyle(
                    color = scheme.textColor,
                    fontWeight = FontWeight.Thin,
                    fontSize = DesignTokens.fontSizeInput,
                    fontFamily = DesignTokens.fontFamilyPlank
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
    }
}
