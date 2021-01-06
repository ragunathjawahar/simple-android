package org.simple.clinic.home.report

import android.annotation.SuppressLint
import android.content.Context
import android.os.Parcelable
import android.util.AttributeSet
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.FrameLayout
import io.reactivex.Observable
import kotlinx.android.synthetic.main.screen_report.view.*
import org.simple.clinic.di.injector
import org.simple.clinic.facility.Facility
import org.simple.clinic.mobius.MobiusDelegate
import org.simple.clinic.util.unsafeLazy
import org.simple.clinic.widgets.visibleOrGone
import javax.inject.Inject
import javax.inject.Provider

class ReportsScreen(context: Context, attrs: AttributeSet) : FrameLayout(context, attrs), ReportsUi {

  @Inject
  lateinit var effectHandler: ReportsEffectHandler

  @Inject
  lateinit var webViewDataProvider: WebViewDataProvider

  private val delegate by unsafeLazy {
    val uiRenderer = ReportsUiRenderer(this)

    MobiusDelegate.forView(
        events = Observable.never(),
        defaultModel = ReportsModel.create(),
        init = ReportsInit(),
        update = ReportsUpdate(),
        effectHandler = effectHandler.build(),
        modelUpdateListener = uiRenderer::render
    )
  }

  @SuppressLint("SetJavaScriptEnabled")
  override fun onFinishInflate() {
    super.onFinishInflate()

    if (isInEditMode) {
      return
    }

    context.injector<Injector>().inject(this)

    webView.settings.javaScriptEnabled = true
    webView.addJavascriptInterface(webViewDataProvider, "injectedObject")
    WebView.setWebContentsDebuggingEnabled(true)
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    delegate.start()
  }

  override fun onDetachedFromWindow() {
    delegate.stop()
    super.onDetachedFromWindow()
  }

  override fun onSaveInstanceState(): Parcelable? {
    return delegate.onSaveInstanceState(super.onSaveInstanceState())
  }

  override fun onRestoreInstanceState(state: Parcelable?) {
    super.onRestoreInstanceState(delegate.onRestoreInstanceState(state))
  }

  override fun showReport(html: String) {
    showWebview(true)
//    webView.loadDataWithBaseURL(null, html, "text/html", Charsets.UTF_8.name(), null)
    webView.loadUrl("file:///android_asset/data.html")
  }

  override fun showNoReportsAvailable() {
    showWebview(false)
    webView.loadUrl("about:blank")
  }

  private fun showWebview(isVisible: Boolean) {
    webView.visibleOrGone(isVisible)
    noReportView.visibleOrGone(isVisible.not())
  }

  interface Injector {
    fun inject(target: ReportsScreen)
  }

  class WebViewDataProvider @Inject constructor(
      private val currentFacilityProvider: Provider<Facility>
  ) {

    @JavascriptInterface
    fun currentFacility(): Int {
      return 15
    }
  }
}
