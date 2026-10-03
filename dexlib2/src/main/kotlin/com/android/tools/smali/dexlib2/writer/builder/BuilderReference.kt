package com.android.tools.smali.dexlib2.writer.builder

import com.android.tools.smali.dexlib2.iface.reference.Reference

interface BuilderReference : Reference {
    var index: Int
}
