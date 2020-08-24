package com.shameel.swipeableintrolibrary

import java.io.Serializable

object SwipeableIntroPage : Serializable {
    var titleText: String? = null
    var descriptionText: String? = null
    var bgColor = 0
    var contentIconRes = 0
    var bottomBarIconRes = 0


    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val that = o as SwipeableIntroPage
        if (bgColor != that.bgColor) return false
        if (contentIconRes != that.contentIconRes) return false
        if (bottomBarIconRes != that.bottomBarIconRes) return false
        if (if (titleText != null) titleText != that.titleText else that.titleText != null) return false
        return if (descriptionText != null) descriptionText == that.descriptionText else that.descriptionText == null
    }

    override fun hashCode(): Int {
        var result = if (titleText != null) titleText.hashCode() else 0
        result = 31 * result + if (descriptionText != null) descriptionText.hashCode() else 0
        result = 31 * result + bgColor
        result = 31 * result + contentIconRes
        result = 31 * result + bottomBarIconRes
        return result
    }

    override fun toString(): String {
        return "SwipeableIntroPage{" +
                "titleText='" + titleText + '\'' +
                ", descriptionText='" + descriptionText + '\'' +
                ", bgColor=" + bgColor +
                ", contentIconRes=" + contentIconRes +
                ", bottomBarIconRes=" + bottomBarIconRes +
                '}'
    }
}