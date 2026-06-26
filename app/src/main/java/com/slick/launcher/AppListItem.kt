package com.slick.launcher

sealed class AppListItem {
    data class Header(val letter: Char) : AppListItem()
    data class App(val appInfo: AppInfo) : AppListItem()
}
