/*
 * SPDX-License-Identifier: MPL-2.0
 * Copyright © 2023 Skyline Team and Contributors (https://github.com/skyline-emu/)
 */

package emu.skyline.preference

import android.content.Context
import android.util.AttributeSet
import androidx.preference.ListPreference
import androidx.preference.Preference.OnPreferenceChangeListener
import androidx.preference.Preference.SummaryProvider
import emu.skyline.di.getSettings

class RatingPreference @JvmOverloads constructor(context : Context, attrs : AttributeSet? = null, defStyleAttr : Int = androidx.preference.R.attr.preferenceStyle) : ListPreference(context, attrs, defStyleAttr) {
    val RATING_ORGANIZATIONS = arrayOf(
        "CERO",
        "GRACGCRB",
        "GSRMR",
        "ESRB",
        "ClassInd",
        "USK",
        "PEGI",
        "PEGIPortugal",
        "PEGIBBFC",
        "Russian",
        "ACB",
        "OFLC",
        "IARCGeneric"
    )

    init {
        entries = RATING_ORGANIZATIONS
        entryValues = RATING_ORGANIZATIONS.map { RATING_ORGANIZATIONS.indexOf(it).toString() }.toTypedArray()

        value = context.getSettings().ratingOrganization.toString()

        onPreferenceChangeListener = OnPreferenceChangeListener { _, newValue ->
            context.getSettings().ratingOrganization = (newValue as String).toInt()
            context.getSettings().refreshRequired = true
            true
        }

        summaryProvider = SummaryProvider<ListPreference> {
            RATING_ORGANIZATIONS[context.getSettings().ratingOrganization]
        }
    }
}