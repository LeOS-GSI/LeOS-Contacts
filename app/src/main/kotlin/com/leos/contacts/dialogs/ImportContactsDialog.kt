package com.goodwy.contacts.dialogs

import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import com.goodwy.commons.extensions.*
import com.goodwy.commons.helpers.ContactsHelper
import com.goodwy.commons.helpers.SMT_PRIVATE
import com.goodwy.commons.helpers.ensureBackgroundThread
import com.goodwy.contacts.R
import com.goodwy.contacts.activities.SimpleActivity
import com.goodwy.contacts.extensions.config
import com.goodwy.contacts.extensions.showContactSourcePicker
import com.goodwy.contacts.helpers.VcfImporter
import com.goodwy.contacts.helpers.VcfImporter.ImportResult.IMPORT_FAIL
import kotlinx.android.synthetic.main.dialog_import_contacts.view.*

class ImportContactsDialog(val activity: SimpleActivity, val path: String, private val callback: (refreshView: Boolean) -> Unit) {
    private var targetContactSource = ""
    private var ignoreClicks = false

    init {
        val view = (activity.layoutInflater.inflate(R.layout.dialog_import_contacts, null) as ViewGroup).apply {
            targetContactSource = activity.config.lastUsedContactSource
            activity.getPublicContactSource(targetContactSource) {
                import_contacts_title.setText(it)
                if (it.isEmpty()) {
                    ContactsHelper(activity).getContactSources {
                        val localSource = it.firstOrNull { it.name == SMT_PRIVATE }
                        if (localSource != null) {
                            targetContactSource = localSource.name
                            activity.runOnUiThread {
                                import_contacts_title.setText(localSource.publicName)
                            }
                        }
                    }
                }
            }

            import_contacts_title.setOnClickListener {
                activity.showContactSourcePicker(targetContactSource) {
                    targetContactSource = if (it == activity.getString(R.string.phone_storage_hidden)) SMT_PRIVATE else it
                    activity.getPublicContactSource(it) {
                        val title = if (it == "") activity.getString(R.string.phone_storage) else it
                        import_contacts_title.setText(title)
                    }
                }
            }
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(view, this, R.string.import_contacts) { alertDialog ->
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        if (ignoreClicks) {
                            return@setOnClickListener
                        }

                        ignoreClicks = true
                        activity.toast(R.string.importing)
                        ensureBackgroundThread {
                            val result = VcfImporter(activity).importContacts(path, targetContactSource)
                            handleParseResult(result)
                            alertDialog.dismiss()
                        }
                    }
                }
            }
    }

    private fun handleParseResult(result: VcfImporter.ImportResult) {
        activity.toast(
            when (result) {
                VcfImporter.ImportResult.IMPORT_OK -> R.string.importing_successful
                VcfImporter.ImportResult.IMPORT_PARTIAL -> R.string.importing_some_entries_failed
                else -> R.string.importing_failed
            }
        )
        callback(result != IMPORT_FAIL)
    }
}
