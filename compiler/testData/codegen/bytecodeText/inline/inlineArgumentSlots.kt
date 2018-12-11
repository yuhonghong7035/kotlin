// WITH_RUNTIME

class Range<T>(val min: T, val max: T)

class Sprite

class A {
    private fun calcSpriteSizeRange(layoutSize: Int,
                                    sprites: Map<Long, Sprite>,
                                    minMargin: Int,
                                    range: Range<Int>)
            : Pair<Range<Int>, Int> {
        // Extract the number of sprites.
        val count = sprites.count()

        // Divide available height by number of sprites
        // yielding the maximum area available for each
        // sprite with its margin.
        var spriteBounds = layoutSize / count.toFloat()

        // Sanity check.
        require(spriteBounds * count <= layoutSize) {
            val result = spriteBounds * count <= layoutSize
            "Algorithm incorrect: $spriteBounds * " +
                    "$count == $result <= $layoutSize"
        }

        // This bounds value is a little bigger than the actual
        // target of a sprite + margin because the leading margin
        // required before the first sprite (n sprites requires
        // n + 1 margins) so we need to reduce this bound to
        // account for this missing margin value.
        val adjustedMargin = minMargin + (minMargin / count.toFloat())
        spriteBounds -= adjustedMargin

        // After removing the reduced margin, we finally have
        // the actual maximum allowable size for a sprite and
        // its optional label that will guarantee that all
        // sprites will be fully visible in the display area.
        var size = spriteBounds

        // Sanity check.
        require((size * count) + minMargin * (count + 1)
                        <= layoutSize) {
            val result = size * count + minMargin * (count + 1)
            "Algorithm incorrect: $size * $count + " +
                    "$minMargin * ($count + 1) == $result <= $layoutSize"
        }

        // Adjust this size to be no larger than a scaled
        // percent of the fixed dimension.
        size = kotlin.math.min(size, range.max.toFloat())

        // Sanity check.
        require(size > 0) {
            "Maximum palantir size ${size.toInt()} > 0."
        }

        // Adjust minimum size if it now exceeds the maximum size.
        val minSize =
            if (range.min > size) {
                size
            } else {
                range.min.toFloat()
            }

        // Safest way to determine the max margin is by
        // straight math (not using previous values).
        val margin = (layoutSize - (size * count)) / (count + 1)

        // Round down to ensure that total size is <= layoutSize
        val adjustedRange = Range(minSize.toInt(), size.toInt())
        val requiredSize =
            calcRequiredLayoutSize(count, adjustedRange.max, margin.toInt())
        require(requiredSize <= layoutSize) {
            "requiredSize <= layoutSize -> " +
                    "$requiredSize <= $layoutSize"
        }

        return Pair(adjustedRange, margin.toInt())
    }
}

fun calcRequiredLayoutSize(count: Int, max: Int, toInt: Int) = 0

// 2 ISTORE 10