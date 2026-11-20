package com.example.ysukampus.ui.main.schedule

import androidx.compose.ui.graphics.Color

/**
 * Цвета типов занятий — перенесены один в один из CSS-переменных вебки
 * (`--lesson-lec`, `--lesson-lab`, `--lesson-prac`, `--danger`, `--warning`),
 * чтобы лекция и там и там была одинаково зелёной.
 */
object ScheduleColors {
    val Lecture = Color(0xFF059669)
    val Lab = Color(0xFF2563EB)
    val Practice = Color(0xFFF3BA11)
    val Danger = Color(0xFFDA1D1D)
    val Warning = Color(0xFFE87000)
    val Muted = Color(0xFF1E293B)
    val TextAccent = Color(0xFF2D5FA8)
}

/** Цвет плашки номера пары и строки типа занятия. */
fun LessonKind.color(): Color = when (this) {
    LessonKind.Lecture -> ScheduleColors.Lecture
    LessonKind.Lab -> ScheduleColors.Lab
    LessonKind.Practice -> ScheduleColors.Practice
    LessonKind.Exam, LessonKind.Retake -> ScheduleColors.Danger
    LessonKind.Credit -> ScheduleColors.Warning
    LessonKind.Other -> ScheduleColors.Muted
}
