package org.simple.clinic.appupdate.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import org.simple.clinic.R

class AppUpdateDialog : DialogFragment() {

  companion object {

    private const val FRAGMENT_TAG = "app_update_alert"

    fun show(fragmentManager: FragmentManager) {
      val fragment = AppUpdateDialog().apply {
        isCancelable = false
      }

      fragment.show(fragmentManager, FRAGMENT_TAG)
    }
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    return AlertDialog.Builder(requireContext())
        .setTitle(getString(R.string.appupdatedialog_title))
        .setMessage(getString(R.string.appupdatedialog_body))
        .setPositiveButton(getString(R.string.appupdatedialog_positive_button_text)) { _, _ ->
          launchPlayStoreForUpdate()
        }
        .setNegativeButton(getString(R.string.appupdatedialog_negative_button_text), null)
        .create()
  }

  private fun launchPlayStoreForUpdate() {
    TODO("not implemented") //Implement later
  }

}
