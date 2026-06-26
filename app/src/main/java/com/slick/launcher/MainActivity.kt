package com.slick.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.animation.DecelerateInterpolator
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.slick.launcher.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var layoutManager: LinearLayoutManager

    // Flat list of headers + apps shared with the adapter
    private val allItems = mutableListOf<AppListItem>()

    // Maps each first-letter to its header position in allItems
    private val sectionIndex = mutableMapOf<Char, Int>()

    private var pendingRefresh = false

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyWindowInsets()

        // Launchers must never close on back press
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = Unit
        })

        setupRecyclerView()
        loadApps()
        setupAlphabetScrollbar()
        animateListIn()
        registerPackageReceiver()
    }

    override fun onResume() {
        super.onResume()
        if (pendingRefresh) {
            pendingRefresh = false
            refreshApps()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(packageReceiver) } catch (_: Exception) {}
    }

    // ── Window insets ──────────────────────────────────────────────────────────

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.recyclerView) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                view.paddingLeft,
                bars.top,
                view.paddingRight,
                bars.bottom
            )
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.alphabetScrollbar) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                view.paddingLeft,
                bars.top,
                view.paddingRight,
                bars.bottom
            )
            insets
        }
    }

    // ── App loading ────────────────────────────────────────────────────────────

    private fun loadApps() {
        allItems.clear()
        sectionIndex.clear()

        val pm = packageManager
        val launchIntent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }

        val apps = pm.queryIntentActivities(launchIntent, PackageManager.MATCH_DEFAULT_ONLY)
            .asSequence()
            .map { info ->
                AppInfo(
                    label = info.loadLabel(pm).toString(),
                    packageName = info.activityInfo.packageName,
                    icon = info.loadIcon(pm),
                    launchIntent = pm.getLaunchIntentForPackage(info.activityInfo.packageName)
                )
            }
            .filter { it.label.isNotBlank() && it.packageName != packageName }
            .sortedWith(
                compareBy(
                    // Non-letter names (e.g. "1Password") go at the end under '#'
                    { val c = it.label.first().uppercaseChar(); if (c.isLetter()) 0 else 1 },
                    { it.label.lowercase() }
                )
            )
            .toList()

        var currentLetter: Char? = null
        apps.forEach { app ->
            val fc = app.label.first().uppercaseChar()
            val letter = if (fc.isLetter()) fc else '#'
            if (letter != currentLetter) {
                currentLetter = letter
                sectionIndex[letter] = allItems.size
                allItems.add(AppListItem.Header(letter))
            }
            allItems.add(AppListItem.App(app))
        }
    }

    private fun refreshApps() {
        loadApps()
        (binding.recyclerView.adapter as? AppListAdapter)?.notifyDataSetChanged()
        setupAlphabetScrollbar()
    }

    // ── RecyclerView ───────────────────────────────────────────────────────────

    private fun setupRecyclerView() {
        layoutManager = LinearLayoutManager(this)
        binding.recyclerView.layoutManager = layoutManager
        binding.recyclerView.adapter = AppListAdapter(allItems) { appInfo ->
            appInfo.launchIntent
                ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ?.let { intent ->
                    runCatching { startActivity(intent) }
                }
        }

        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val firstVisible = layoutManager.findFirstVisibleItemPosition()
                binding.alphabetScrollbar.activeLetter = letterForPosition(firstVisible)
            }
        })
    }

    // ── Alphabet scrollbar ─────────────────────────────────────────────────────

    private fun setupAlphabetScrollbar() {
        // Letters sorted: A-Z first, '#' last
        binding.alphabetScrollbar.letters = sectionIndex.keys.sortedWith(
            compareBy { if (it.isLetter()) it else '{' }
        )

        binding.alphabetScrollbar.listener = object : AlphabetScrollbarView.OnLetterSelectedListener {
            override fun onLetterSelected(letter: Char, isDragging: Boolean) {
                val pos = sectionIndex[letter] ?: return
                layoutManager.scrollToPositionWithOffset(pos, 0)
            }
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private fun letterForPosition(position: Int): Char? {
        for (i in position downTo 0) {
            val item = allItems.getOrNull(i) ?: continue
            if (item is AppListItem.Header) return item.letter
        }
        return null
    }

    private fun animateListIn() {
        binding.recyclerView.alpha = 0f
        binding.recyclerView.translationY = 56f
        binding.recyclerView.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(480)
            .setStartDelay(60)
            .setInterpolator(DecelerateInterpolator(1.8f))
            .start()

        binding.alphabetScrollbar.alpha = 0f
        binding.alphabetScrollbar.animate()
            .alpha(1f)
            .setDuration(300)
            .setStartDelay(200)
            .start()
    }

    // ── Package change receiver ────────────────────────────────────────────────

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            pendingRefresh = true
        }
    }

    private fun registerPackageReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        registerReceiver(packageReceiver, filter)
    }
}
