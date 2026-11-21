package com.kingzcheung.xime.ui.keyboard

/** Same geometry for real text measurement, intrinsic flow measurement and rendering. */
internal fun belowCandidateWidth(primary: Int, secondary: Int, commentCap: Int): Int =
    maxOf(primary, minOf(secondary, commentCap))
