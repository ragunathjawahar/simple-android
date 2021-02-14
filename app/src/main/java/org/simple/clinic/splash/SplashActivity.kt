package org.simple.clinic.splash

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.simple.clinic.setup.SetupActivity

class SplashActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val intent = Intent(this, SetupActivity::class.java)
    startActivity(intent)
    finish()
  }
}
