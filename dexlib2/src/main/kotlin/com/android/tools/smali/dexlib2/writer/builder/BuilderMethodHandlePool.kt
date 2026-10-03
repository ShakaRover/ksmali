package com.android.tools.smali.dexlib2.writer.builder

import com.android.tools.smali.dexlib2.MethodHandleType
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodHandleReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.writer.MethodHandleSection
import com.android.tools.smali.util.ExceptionWithContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

class BuilderMethodHandlePool(dexBuilder: DexBuilder) : BaseBuilderPool(dexBuilder),
    MethodHandleSection<BuilderMethodHandleReference, BuilderFieldReference, BuilderMethodReference> {
    private val internedItems: ConcurrentMap<MethodHandleReference, BuilderMethodHandleReference> =
        ConcurrentHashMap()

    fun internMethodHandle(methodHandleReference: MethodHandleReference): BuilderMethodHandleReference {
        var internedMethodHandle = internedItems[methodHandleReference]
        if (internedMethodHandle != null) {
            return internedMethodHandle
        }

        val memberReference: BuilderReference = when (methodHandleReference.methodHandleType) {
            MethodHandleType.STATIC_PUT,
            MethodHandleType.STATIC_GET,
            MethodHandleType.INSTANCE_PUT,
            MethodHandleType.INSTANCE_GET ->
                dexBuilder.internFieldReference(methodHandleReference.memberReference as FieldReference)
            MethodHandleType.INVOKE_STATIC,
            MethodHandleType.INVOKE_INSTANCE,
            MethodHandleType.INVOKE_CONSTRUCTOR,
            MethodHandleType.INVOKE_DIRECT,
            MethodHandleType.INVOKE_INTERFACE ->
                dexBuilder.internMethodReference(methodHandleReference.memberReference as MethodReference)
            else -> throw ExceptionWithContext("Invalid method handle type: %d",
                methodHandleReference.methodHandleType)
        }

        internedMethodHandle = BuilderMethodHandleReference(methodHandleReference.methodHandleType, memberReference)
        val prev = internedItems.putIfAbsent(internedMethodHandle, internedMethodHandle)
        return prev ?: internedMethodHandle
    }

    override fun getFieldReference(methodHandleReference: BuilderMethodHandleReference): BuilderFieldReference {
        return methodHandleReference.memberReference as BuilderFieldReference
    }

    override fun getMethodReference(methodHandleReference: BuilderMethodHandleReference): BuilderMethodReference {
        return methodHandleReference.memberReference as BuilderMethodReference
    }

    override fun getItemIndex(key: BuilderMethodHandleReference): Int {
        return key.index
    }

    override val items: Collection<MutableMap.MutableEntry<BuilderMethodHandleReference, Int>>
        get() = object : BuilderMapEntryCollection<BuilderMethodHandleReference>(internedItems.values) {
            override fun getValue(key: BuilderMethodHandleReference): Int {
                return key.index
            }

            override fun setValue(key: BuilderMethodHandleReference, value: Int): Int {
                val prev = key.index
                key.index = value
                return prev
            }
        }

    override val itemCount: Int
        get() = internedItems.size
}
