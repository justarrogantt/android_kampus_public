# Правила R8 для минифицированных сборок (release, betaRelease).
#
# Рукописных правил для Retrofit и kotlinx.serialization здесь больше нет:
# Retrofit 2.11+ приносит META-INF/proguard/retrofit2.pro, а
# kotlinx-serialization-core 1.6+ — kotlinx-serialization-r8.pro. Оба набора
# шире того, что было написано руками (включая -if interface для R8 full mode,
# из-за отсутствия которых suspend-функции падали с
# ClassCastException: Class cannot be cast to ParameterizedType).
# Возвращать их вручную не нужно и вредно: рукописные копии устаревают молча.

# Стектрейсы от тестеров без этого нечитаемы даже с mapping.txt: R8 по
# умолчанию выбрасывает номера строк, и остаётся только имя метода.
# Имя исходного файла при этом обезличивается, класс восстанавливается по mapping.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
