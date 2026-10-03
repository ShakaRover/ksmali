/*
 * Copyright 2012, Google LLC
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *     * Neither the name of Google LLC nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


package com.android.tools.smali.dexlib2.analysis

import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.HiddenApiRestriction
import com.android.tools.smali.dexlib2.analysis.util.MemoizingSupplier
import com.android.tools.smali.dexlib2.analysis.util.TypeProtoUtils
import com.android.tools.smali.dexlib2.base.reference.BaseMethodReference
import com.android.tools.smali.dexlib2.iface.Annotation
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Field
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.MethodImplementation
import com.android.tools.smali.dexlib2.iface.MethodParameter
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.util.alignOffset
import com.android.tools.smali.dexlib2.util.isAligned
import com.android.tools.smali.dexlib2.util.methodSignaturesMatch
import com.android.tools.smali.util.ExceptionWithContext
import com.android.tools.smali.util.IteratorUtils
import com.android.tools.smali.util.SparseArray
import com.android.tools.smali.util.StringUtils
import java.util.Collections
import java.util.Comparator
import java.util.HashMap
import java.util.LinkedHashMap
import java.util.PriorityQueue
import java.util.function.Supplier

/**
 * A class "prototype". This contains things like the interfaces, the superclass, the vtable and the instance fields
 * and their offsets.
 */
open class ClassProto(override val classPath: ClassPath, override val type: String) : TypeProto {
    @JvmField
    protected var vtableFullyResolved = true

    @JvmField
    protected var interfacesFullyResolved = true

    protected var unresolvedInterfaces: MutableSet<String>? = null

    private val classDefSupplier: Supplier<ClassDef> =
        MemoizingSupplier.memoize(Supplier { classPath.getClassDef(type) })

    init {
        if (type[0] != 'L') {
            throw ExceptionWithContext("Cannot construct ClassProto for non reference type: %s", type)
        }
    }

    override fun toString(): String = type

    fun getClassDef(): ClassDef {
        return classDefSupplier.get()
    }

    /**
     * Returns true if this class is an interface.
     *
     * If this class is not defined, then this will throw an UnresolvedClassException
     *
     * @return True if this class is an interface
     */
    override fun isInterface(): Boolean {
        val classDef = getClassDef()
        return (classDef.accessFlags and AccessFlags.INTERFACE.value) != 0
    }

    /**
     * Returns the set of interfaces that this class implements as a Map<String, ClassDef>.
     *
     * The ClassDef value will be present only for the interfaces that this class directly implements (including any
     * interfaces transitively implemented), but not for any interfaces that are only implemented by a superclass of
     * this class
     *
     * For any interfaces that are only implemented by a superclass (or the class itself, if the class is an interface),
     * the value will be null.
     *
     * If any interface couldn't be resolved, then the interfacesFullyResolved field will be set to false upon return.
     *
     * @return the set of interfaces that this class implements as a Map<String, ClassDef>.
     */
    private fun getInterfaces(): LinkedHashMap<String, ClassDef?> {
        if (!classPath.isArt() || classPath.oatVersion < 72) {
            return preDefaultMethodInterfaceSupplier.get()
        } else {
            return postDefaultMethodInterfaceSupplier.get()
        }
    }

    /**
     * This calculates the interfaces in the order required for vtable generation for dalvik and pre-default method ART
     */
    private val preDefaultMethodInterfaceSupplier: Supplier<LinkedHashMap<String, ClassDef?>> =
        MemoizingSupplier.memoize(Supplier { preDefaultMethodInterfaces() })

    private fun preDefaultMethodInterfaces(): LinkedHashMap<String, ClassDef?> {
        val unresolvedInterfaces = HashSet<String>(0)
        val interfaces = LinkedHashMap<String, ClassDef?>()

        try {
            for (interfaceType in getClassDef().interfaces) {
                if (!interfaces.containsKey(interfaceType)) {
                    try {
                        val interfaceDef = classPath.getClassDef(interfaceType)
                        interfaces[interfaceType] = interfaceDef
                    } catch (ex: UnresolvedClassException) {
                        interfaces[interfaceType] = null
                        unresolvedInterfaces.add(interfaceType)
                        interfacesFullyResolved = false
                    }

                    val interfaceProto = classPath.getClass(interfaceType) as ClassProto
                    for (superInterface in interfaceProto.getInterfaces().keys) {
                        if (!interfaces.containsKey(superInterface)) {
                            interfaces[superInterface] =
                                interfaceProto.getInterfaces()[superInterface]
                        }
                    }
                    if (!interfaceProto.interfacesFullyResolved) {
                        unresolvedInterfaces.addAll(interfaceProto.getUnresolvedInterfacesOrEmpty())
                        interfacesFullyResolved = false
                    }
                }
            }
        } catch (ex: UnresolvedClassException) {
            interfaces[type] = null
            unresolvedInterfaces.add(type)
            interfacesFullyResolved = false
        }

        // now add self and super class interfaces, required for common super class lookup
        // we don't really need ClassDef's for that, so let's just use null

        if (isInterface() && !interfaces.containsKey(type)) {
            interfaces[type] = null
        }

        val superclass = this.superclass
        try {
            if (superclass != null) {
                val superclassProto = classPath.getClass(superclass) as ClassProto
                for (superclassInterface in superclassProto.getInterfaces().keys) {
                    if (!interfaces.containsKey(superclassInterface)) {
                        interfaces[superclassInterface] = null
                    }
                }
                if (!superclassProto.interfacesFullyResolved) {
                    unresolvedInterfaces.addAll(superclassProto.getUnresolvedInterfacesOrEmpty())
                    interfacesFullyResolved = false
                }
            }
        } catch (ex: UnresolvedClassException) {
            unresolvedInterfaces.add(superclass!!)
            interfacesFullyResolved = false
        }

        if (unresolvedInterfaces.size > 0) {
            this.unresolvedInterfaces = unresolvedInterfaces
        }

        return interfaces
    }

    /**
     * This calculates the interfaces in the order required for vtable generation for post-default method ART
     */
    private val postDefaultMethodInterfaceSupplier: Supplier<LinkedHashMap<String, ClassDef?>> =
        MemoizingSupplier.memoize(Supplier { postDefaultMethodInterfaces() })

    private fun postDefaultMethodInterfaces(): LinkedHashMap<String, ClassDef?> {
        val unresolvedInterfaces = HashSet<String>(0)
        val interfaces = LinkedHashMap<String, ClassDef?>()

        val superclass = this.superclass
        if (superclass != null) {
            val superclassProto = classPath.getClass(superclass) as ClassProto
            for (superclassInterface in superclassProto.getInterfaces().keys) {
                interfaces[superclassInterface] = null
            }
            if (!superclassProto.interfacesFullyResolved) {
                unresolvedInterfaces.addAll(superclassProto.getUnresolvedInterfacesOrEmpty())
                interfacesFullyResolved = false
            }
        }

        try {
            for (interfaceType in getClassDef().interfaces) {
                if (!interfaces.containsKey(interfaceType)) {
                    val interfaceProto = classPath.getClass(interfaceType) as ClassProto
                    try {
                        for ((key, value) in interfaceProto.getInterfaces()) {
                            if (!interfaces.containsKey(key)) {
                                interfaces[key] = value
                            }
                        }
                    } catch (ex: UnresolvedClassException) {
                        interfaces[interfaceType] = null
                        unresolvedInterfaces.add(interfaceType)
                        interfacesFullyResolved = false
                    }
                    if (!interfaceProto.interfacesFullyResolved) {
                        unresolvedInterfaces.addAll(interfaceProto.getUnresolvedInterfacesOrEmpty())
                        interfacesFullyResolved = false
                    }
                    try {
                        val interfaceDef = classPath.getClassDef(interfaceType)
                        interfaces[interfaceType] = interfaceDef
                    } catch (ex: UnresolvedClassException) {
                        interfaces[interfaceType] = null
                        unresolvedInterfaces.add(interfaceType)
                        interfacesFullyResolved = false
                    }
                }
            }
        } catch (ex: UnresolvedClassException) {
            interfaces[type] = null
            unresolvedInterfaces.add(type)
            interfacesFullyResolved = false
        }

        if (unresolvedInterfaces.size > 0) {
            this.unresolvedInterfaces = unresolvedInterfaces
        }

        return interfaces
    }

    private fun getUnresolvedInterfacesOrEmpty(): Set<String> {
        return unresolvedInterfaces ?: Collections.emptySet()
    }

    /**
     * Gets the interfaces directly implemented by this class, or the interfaces they transitively implement.
     *
     * This does not include any interfaces that are only implemented by a superclass
     *
     * @return An iterables of ClassDefs representing the directly or transitively implemented interfaces
     * @throws UnresolvedClassException if interfaces could not be fully resolved
     */
    private fun getDirectInterfaces(): Iterable<ClassDef> {
        val directInterfaces: Iterable<ClassDef> = getInterfaces().values.filterNotNull()

        if (!interfacesFullyResolved) {
            throw UnresolvedClassException(
                "Interfaces for class %s not fully resolved: %s", type,
                StringUtils.join(getUnresolvedInterfacesOrEmpty(), ",")
            )
        }

        return directInterfaces
    }

    /**
     * Checks if this class implements the given interface.
     *
     * If the interfaces of this class cannot be fully resolved then this
     * method will either return true or throw an UnresolvedClassException
     *
     * @param iface The interface to check for
     * @return true if this class implements the given interface, otherwise false
     * @throws UnresolvedClassException if the interfaces for this class could not be fully resolved, and the interface
     * is not one of the interfaces that were successfully resolved
     */
    override fun implementsInterface(iface: String): Boolean {
        if (getInterfaces().containsKey(iface)) {
            return true
        }
        if (!interfacesFullyResolved) {
            throw UnresolvedClassException("Interfaces for class %s not fully resolved", type)
        }
        return false
    }

    override val superclass: String?
        get() = getClassDef().superclass

    /**
     * This is a helper method for getCommonSuperclass
     *
     * It checks if this class is an interface, and if so, if other implements it.
     *
     * If this class is undefined, we go ahead and check if it is listed in other's interfaces. If not, we throw an
     * UndefinedClassException
     *
     * If the interfaces of other cannot be fully resolved, we check the interfaces that can be resolved. If not found,
     * we throw an UndefinedClassException
     *
     * @param other The class to check the interfaces of
     * @return true if this class is an interface (or is undefined) other implements this class
     *
     */
    private fun checkInterface(other: ClassProto): Boolean {
        var isResolved = true
        var isInterface = true
        try {
            isInterface = isInterface()
        } catch (ex: UnresolvedClassException) {
            isResolved = false
            // if we don't know if this class is an interface or not,
            // we can still try to call other.implementsInterface(this)
        }
        if (isInterface) {
            try {
                if (other.implementsInterface(type)) {
                    return true
                }
            } catch (ex: UnresolvedClassException) {
                // There are 2 possibilities here, depending on whether we were able to resolve this class.
                // 1. If this class is resolved, then we know it is an interface class. The other class either
                //    isn't defined, or its interfaces couldn't be fully resolved.
                //    In this case, we throw an UnresolvedClassException
                // 2. If this class is not resolved, we had tried to call implementsInterface anyway. We don't
                //    know for sure if this class is an interface or not. We return false, and let processing
                //    continue in getCommonSuperclass
                if (isResolved) {
                    throw ex
                }
            }
        }
        return false
    }

    override fun getCommonSuperclass(other: TypeProto): TypeProto {
        // use the other type's more specific implementation
        if (other !is ClassProto) {
            return other.getCommonSuperclass(this)
        }

        if (this === other || type == other.type) {
            return this
        }

        if (type == "Ljava/lang/Object;") {
            return this
        }

        if (other.type == "Ljava/lang/Object;") {
            return other
        }

        var gotException = false
        try {
            if (checkInterface(other)) {
                return this
            }
        } catch (ex: UnresolvedClassException) {
            gotException = true
        }

        try {
            if (other.checkInterface(this)) {
                return other
            }
        } catch (ex: UnresolvedClassException) {
            gotException = true
        }
        if (gotException) {
            return classPath.getUnknownClass()
        }

        val thisChain = ArrayList<TypeProto>()
        thisChain.add(this)
        IteratorUtils.addAll(thisChain, TypeProtoUtils.getSuperclassChain(this).iterator())

        val otherChain = ArrayList<TypeProto>()
        otherChain.add(other)
        IteratorUtils.addAll(otherChain, TypeProtoUtils.getSuperclassChain(other).iterator())

        // reverse them, so that the first entry is either Ljava/lang/Object; or Ujava/lang/Object;
        Collections.reverse(thisChain)
        Collections.reverse(otherChain)

        var i = Math.min(thisChain.size, otherChain.size) - 1
        while (i >= 0) {
            val typeProto = thisChain[i]
            if (typeProto.type == otherChain[i].type) {
                return typeProto
            }
            i--
        }

        return classPath.getUnknownClass()
    }

    override fun getFieldByOffset(fieldOffset: Int): FieldReference? {
        if (getInstanceFields().size() == 0) {
            return null
        }
        return getInstanceFields().get(fieldOffset)
    }

    override fun getMethodByVtableIndex(vtableIndex: Int): Method? {
        val vtable = getVtable()
        if (vtableIndex < 0 || vtableIndex >= vtable.size) {
            return null
        }

        return vtable[vtableIndex]
    }

    override fun findMethodIndexInVtable(method: MethodReference): Int {
        return findMethodIndexInVtable(getVtable(), method)
    }

    private fun findMethodIndexInVtable(vtable: List<Method>, method: MethodReference): Int {
        for (i in vtable.indices) {
            val candidate = vtable[i]
            if (methodSignaturesMatch(candidate, method)) {
                if (!classPath.shouldCheckPackagePrivateAccess() ||
                    AnalyzedMethodUtil.canAccess(this, candidate, true, false, false)
                ) {
                    return i
                }
            }
        }
        return -1
    }

    private fun findMethodIndexInVtableReverse(vtable: List<Method>, method: MethodReference): Int {
        var i = vtable.size - 1
        while (i >= 0) {
            val candidate = vtable[i]
            if (methodSignaturesMatch(candidate, method)) {
                if (!classPath.shouldCheckPackagePrivateAccess() ||
                    AnalyzedMethodUtil.canAccess(this, candidate, true, false, false)
                ) {
                    return i
                }
            }
            i--
        }
        return -1
    }

    fun getInstanceFields(): SparseArray<FieldReference> {
        if (classPath.isArt()) {
            return artInstanceFieldsSupplier.get()
        } else {
            return dalvikInstanceFieldsSupplier.get()
        }
    }

    private val dalvikInstanceFieldsSupplier: Supplier<SparseArray<FieldReference>> =
        MemoizingSupplier.memoize(Supplier { computeDalvikInstanceFields() })

    private fun computeDalvikInstanceFields(): SparseArray<FieldReference> {
        //This is a bit of an "involved" operation. We need to follow the same algorithm that dalvik uses to
        //arrange fields, so that we end up with the same field offsets (which is needed for deodexing).
        //See mydroid/dalvik/vm/oo/Class.c - computeFieldOffsets()

        val fields = getSortedInstanceFields(getClassDef())
        val fieldCount = fields.size
        //the "type" for each field in fields. 0=reference,1=wide,2=other
        val fieldTypes = ByteArray(fields.size)
        for (i in 0 until fieldCount) {
            fieldTypes[i] = getFieldType(fields[i])
        }

        //The first operation is to move all of the reference fields to the front. To do this, find the first
        //non-reference field, then find the last reference field, swap them and repeat
        var back = fields.size - 1
        var front = 0
        while (front < fieldCount) {
            if (fieldTypes[front] != REFERENCE) {
                while (back > front) {
                    if (fieldTypes[back] == REFERENCE) {
                        swap(fieldTypes, fields, front, back--)
                        break
                    }
                    back--
                }
            }

            if (fieldTypes[front] != REFERENCE) {
                break
            }
            front++
        }

        var startFieldOffset = 8
        val superclassType = superclass
        var superclass: ClassProto? = null
        if (superclassType != null) {
            superclass = classPath.getClass(superclassType) as ClassProto
            startFieldOffset = superclass.getNextFieldOffset()
        }

        val fieldIndexMod: Int
        if ((startFieldOffset % 8) == 0) {
            fieldIndexMod = 0
        } else {
            fieldIndexMod = 1
        }

        //next, we need to group all the wide fields after the reference fields. But the wide fields have to be
        //8-byte aligned. If we're on an odd field index, we need to insert a 32-bit field. If the next field
        //is already a 32-bit field, use that. Otherwise, find the first 32-bit field from the end and swap it in.
        //If there are no 32-bit fields, do nothing for now. We'll add padding when calculating the field offsets
        if (front < fieldCount && (front % 2) != fieldIndexMod) {
            if (fieldTypes[front] == WIDE) {
                //we need to swap in a 32-bit field, so the wide fields will be correctly aligned
                back = fieldCount - 1
                while (back > front) {
                    if (fieldTypes[back] == OTHER) {
                        swap(fieldTypes, fields, front++, back)
                        break
                    }
                    back--
                }
            } else {
                //there's already a 32-bit field here that we can use
                front++
            }
        }

        //do the swap thing for wide fields
        back = fieldCount - 1
        while (front < fieldCount) {
            if (fieldTypes[front] != WIDE) {
                while (back > front) {
                    if (fieldTypes[back] == WIDE) {
                        swap(fieldTypes, fields, front, back--)
                        break
                    }
                    back--
                }
            }

            if (fieldTypes[front] != WIDE) {
                break
            }
            front++
        }

        val superFields: SparseArray<FieldReference>
        if (superclass != null) {
            superFields = superclass.getInstanceFields()
        } else {
            superFields = SparseArray<FieldReference>()
        }
        val superFieldCount = superFields.size()

        //now the fields are in the correct order. Add them to the SparseArray and lookup, and calculate the offsets
        val totalFieldCount = superFieldCount + fieldCount
        val instanceFields = SparseArray<FieldReference>(totalFieldCount)

        var fieldOffset: Int

        if (superclass != null && superFieldCount > 0) {
            for (i in 0 until superFieldCount) {
                instanceFields.append(superFields.keyAt(i), superFields.valueAt(i))
            }

            fieldOffset = instanceFields.keyAt(superFieldCount - 1)

            val lastSuperField = superFields.valueAt(superFieldCount - 1)
            val fieldType = lastSuperField.type[0]
            if (fieldType == 'J' || fieldType == 'D') {
                fieldOffset += 8
            } else {
                fieldOffset += 4
            }
        } else {
            //the field values start at 8 bytes into the DataObject dalvik structure
            fieldOffset = 8
        }

        var gotDouble = false
        for (i in 0 until fieldCount) {
            val field = fields[i]

            //add padding to align the wide fields, if needed
            if (fieldTypes[i] == WIDE && !gotDouble) {
                if (fieldOffset % 8 != 0) {
                    assert(fieldOffset % 8 == 4)
                    fieldOffset += 4
                }
                gotDouble = true
            }

            instanceFields.append(fieldOffset, field)
            if (fieldTypes[i] == WIDE) {
                fieldOffset += 8
            } else {
                fieldOffset += 4
            }
        }

        return instanceFields
    }

    private fun getSortedInstanceFields(classDef: ClassDef): ArrayList<Field> {
        val fields = ArrayList(IteratorUtils.toList(classDef.instanceFields))
        Collections.sort(fields)
        return fields
    }

    private fun swap(fieldTypes: ByteArray, fields: MutableList<Field>, position1: Int, position2: Int) {
        val tempType = fieldTypes[position1]
        fieldTypes[position1] = fieldTypes[position2]
        fieldTypes[position2] = tempType

        val tempField = fields.set(position1, fields[position2])
        fields[position2] = tempField
    }

    private val artInstanceFieldsSupplier: Supplier<SparseArray<FieldReference>> =
        MemoizingSupplier.memoize(Supplier { computeArtInstanceFields() })

    private fun computeArtInstanceFields(): SparseArray<FieldReference> {
        // We need to follow the same algorithm that art uses to arrange fields, so that we end up with the
        // same field offsets, which is needed for deodexing.
        // See LinkFields() in art/runtime/class_linker.cc

        val gaps = PriorityQueue<FieldGap>()

        val linkedFields = SparseArray<FieldReference>()
        val fields = getSortedArtInstanceFields(getClassDef())

        var fieldOffset = 0
        val superclassType = superclass
        if (superclassType != null) {
            // TODO: what to do if superclass doesn't exist?
            val superclass = classPath.getClass(superclassType) as ClassProto
            val superFields = superclass.getInstanceFields()
            var field: FieldReference? = null
            var lastOffset = 0
            for (i in 0 until superFields.size()) {
                val offset = superFields.keyAt(i)
                field = superFields.valueAt(i)
                linkedFields.put(offset, field)
                lastOffset = offset
            }
            if (field != null) {
                fieldOffset = lastOffset + getFieldSize(field)
            }
        }

        for (field in fields) {
            val fieldSize = getFieldSize(field)

            if (!isAligned(fieldOffset, fieldSize)) {
                val oldOffset = fieldOffset
                fieldOffset = alignOffset(fieldOffset, fieldSize)
                addFieldGap(oldOffset, fieldOffset, gaps)
            }

            val gap = gaps.peek()
            if (gap != null && gap.size >= fieldSize) {
                gaps.poll()
                linkedFields.put(gap.offset, field)
                if (gap.size > fieldSize) {
                    addFieldGap(gap.offset + fieldSize, gap.offset + gap.size, gaps)
                }
            } else {
                linkedFields.append(fieldOffset, field)
                fieldOffset += fieldSize
            }
        }

        return linkedFields
    }

    private fun addFieldGap(gapStart: Int, gapEnd: Int, gaps: PriorityQueue<FieldGap>) {
        var offset = gapStart

        while (offset < gapEnd) {
            val remaining = gapEnd - offset

            if (remaining >= 4 && offset % 4 == 0) {
                gaps.add(FieldGap.newFieldGap(offset, 4, classPath.oatVersion))
                offset += 4
            } else if (remaining >= 2 && offset % 2 == 0) {
                gaps.add(FieldGap.newFieldGap(offset, 2, classPath.oatVersion))
                offset += 2
            } else {
                gaps.add(FieldGap.newFieldGap(offset, 1, classPath.oatVersion))
                offset += 1
            }
        }
    }

    private fun getSortedArtInstanceFields(classDef: ClassDef): ArrayList<Field> {
        val fields = ArrayList(IteratorUtils.toList(classDef.instanceFields))
        Collections.sort(fields, Comparator<Field> { field1, field2 ->
            var result = getFieldSortOrder(field1).compareTo(getFieldSortOrder(field2))
            if (result != 0) {
                return@Comparator result
            }

            result = field1.name.compareTo(field2.name)
            if (result != 0) {
                return@Comparator result
            }
            field1.type.compareTo(field2.type)
        })
        return fields
    }

    private fun getFieldSortOrder(field: FieldReference): Int {
        // The sort order is based on type size (except references are first), and then based on the
        // enum value of the primitive type for types of equal size. See: Primitive::Type enum
        // in art/runtime/primitive.h
        return when (field.type[0]) {
            /* reference */
            '[', 'L' -> 0
            /* 64 bit */
            'J' -> 1
            'D' -> 2
            /* 32 bit */
            'I' -> 3
            'F' -> 4
            /* 16 bit */
            'C' -> 5
            'S' -> 6
            /* 8 bit */
            'Z' -> 7
            'B' -> 8
            else -> throw ExceptionWithContext("Invalid field type: %s", field.type)
        }
    }

    private fun getFieldSize(field: FieldReference): Int {
        return getTypeSize(field.type[0])
    }

    private fun getNextFieldOffset(): Int {
        val instanceFields = getInstanceFields()
        if (instanceFields.size() == 0) {
            return if (classPath.isArt()) 0 else 8
        }

        val lastItemIndex = instanceFields.size() - 1
        val fieldOffset = instanceFields.keyAt(lastItemIndex)
        val lastField = instanceFields.valueAt(lastItemIndex)

        return if (classPath.isArt()) {
            fieldOffset + getTypeSize(lastField.type[0])
        } else {
            when (lastField.type[0]) {
                'J', 'D' -> fieldOffset + 8
                else -> fieldOffset + 4
            }
        }
    }

    fun getVtable(): List<Method> {
        if (!classPath.isArt() || classPath.oatVersion < 72) {
            return preDefaultMethodVtableSupplier.get()
        } else if (classPath.oatVersion < 87) {
            return buggyPostDefaultMethodVtableSupplier.get()
        } else {
            return postDefaultMethodVtableSupplier.get()
        }
    }

    //TODO: check the case when we have a package private method that overrides an interface method
    private val preDefaultMethodVtableSupplier: Supplier<List<Method>> =
        MemoizingSupplier.memoize(Supplier { computePreDefaultMethodVtable() })

    private fun computePreDefaultMethodVtable(): List<Method> {
        val vtable = ArrayList<Method>()

        //copy the virtual methods from the superclass
        val superclassType: String?
        try {
            superclassType = superclass
        } catch (ex: UnresolvedClassException) {
            vtable.addAll((classPath.getClass("Ljava/lang/Object;") as ClassProto).getVtable())
            vtableFullyResolved = false
            return vtable
        }

        if (superclassType != null) {
            val superclass = classPath.getClass(superclassType) as ClassProto
            vtable.addAll(superclass.getVtable())

            // if the superclass's vtable wasn't fully resolved, then we can't know where the new methods added by this
            // class should start, so we just propagate what we can from the parent and hope for the best.
            if (!superclass.vtableFullyResolved) {
                vtableFullyResolved = false
                return vtable
            }
        }

        //iterate over the virtual methods in the current class, and only add them when we don't already have the
        //method (i.e. if it was implemented by the superclass)
        if (!isInterface()) {
            addToVtable(getClassDef().virtualMethods, vtable, true, true)

            // We use the current class for any vtable method references that we add, rather than the interface, so
            // we don't end up trying to call invoke-virtual using an interface, which will fail verification
            val interfaces = getDirectInterfaces()
            for (interfaceDef in interfaces) {
                val interfaceMethods = ArrayList<Method>()
                for (interfaceMethod in interfaceDef.virtualMethods) {
                    interfaceMethods.add(ReparentedMethod(interfaceMethod, type))
                }
                addToVtable(interfaceMethods, vtable, false, true)
            }
        }
        return vtable
    }

    /**
     * This is the vtable supplier for a version of art that had buggy vtable calculation logic. In some cases it can
     * produce multiple vtable entries for a given virtual method. This supplier duplicates this buggy logic in order to
     * generate an identical vtable
     */
    private val buggyPostDefaultMethodVtableSupplier: Supplier<List<Method>> =
        MemoizingSupplier.memoize(Supplier { computeBuggyPostDefaultMethodVtable() })

    private fun computeBuggyPostDefaultMethodVtable(): List<Method> {
        val vtable = ArrayList<Method>()

        //copy the virtual methods from the superclass
        val superclassType: String?
        try {
            superclassType = superclass
        } catch (ex: UnresolvedClassException) {
            vtable.addAll((classPath.getClass("Ljava/lang/Object;") as ClassProto).getVtable())
            vtableFullyResolved = false
            return vtable
        }

        if (superclassType != null) {
            val superclass = classPath.getClass(superclassType) as ClassProto
            vtable.addAll(superclass.getVtable())

            // if the superclass's vtable wasn't fully resolved, then we can't know where the new methods added by
            // this class should start, so we just propagate what we can from the parent and hope for the best.
            if (!superclass.vtableFullyResolved) {
                vtableFullyResolved = false
                return vtable
            }
        }

        //iterate over the virtual methods in the current class, and only add them when we don't already have the
        //method (i.e. if it was implemented by the superclass)
        if (!isInterface()) {
            addToVtable(getClassDef().virtualMethods, vtable, true, true)

            val interfaces = ArrayList(getInterfaces().keys)

            val defaultMethods = ArrayList<Method>()
            val defaultConflictMethods = ArrayList<Method>()
            val mirandaMethods = ArrayList<Method>()

            val methodOrder = HashMap<MethodReference, Int>()

            var i = interfaces.size - 1
            while (i >= 0) {
                val interfaceType = interfaces[i]
                val interfaceDef = classPath.getClassDef(interfaceType)

                for (interfaceMethod in interfaceDef.virtualMethods) {

                    val vtableIndex = findMethodIndexInVtableReverse(vtable, interfaceMethod)
                    var oldVtableMethod: Method? = null
                    if (vtableIndex >= 0) {
                        oldVtableMethod = vtable[vtableIndex]
                    }

                    for (j in vtable.indices) {
                        val candidate = vtable[j]
                        if (methodSignaturesMatch(candidate, interfaceMethod)) {
                            if (!classPath.shouldCheckPackagePrivateAccess() ||
                                AnalyzedMethodUtil.canAccess(this, candidate, true, false, false)
                            ) {
                                if (interfaceMethodOverrides(interfaceMethod, candidate)) {
                                    vtable[j] = interfaceMethod
                                }
                            }
                        }
                    }

                    if (vtableIndex >= 0) {
                        if (!isOverridableByDefaultMethod(vtable[vtableIndex])) {
                            continue
                        }
                    }

                    val defaultMethodIndex = findMethodIndexInVtable(defaultMethods, interfaceMethod)

                    if (defaultMethodIndex >= 0) {
                        if (!AccessFlags.ABSTRACT.isSet(interfaceMethod.accessFlags)) {
                            val existingInterface = classPath.getClass(
                                defaultMethods[defaultMethodIndex].definingClass
                            ) as ClassProto
                            if (!existingInterface.implementsInterface(interfaceMethod.definingClass)) {
                                val removedMethod = defaultMethods.removeAt(defaultMethodIndex)
                                defaultConflictMethods.add(removedMethod)
                            }
                        }
                        continue
                    }

                    val defaultConflictMethodIndex = findMethodIndexInVtable(
                        defaultConflictMethods, interfaceMethod
                    )
                    if (defaultConflictMethodIndex >= 0) {
                        // There's already a matching method in the conflict list, we don't need to do
                        // anything else
                        continue
                    }

                    val mirandaMethodIndex = findMethodIndexInVtable(mirandaMethods, interfaceMethod)

                    if (mirandaMethodIndex >= 0) {
                        if (!AccessFlags.ABSTRACT.isSet(interfaceMethod.accessFlags)) {

                            val existingInterface = classPath.getClass(
                                mirandaMethods[mirandaMethodIndex].definingClass
                            ) as ClassProto
                            if (!existingInterface.implementsInterface(interfaceMethod.definingClass)) {
                                val oldMethod = mirandaMethods.removeAt(mirandaMethodIndex)
                                val methodOrderValue = methodOrder[oldMethod]!!
                                methodOrder[interfaceMethod] = methodOrderValue
                                defaultMethods.add(interfaceMethod)
                            }
                        }
                        continue
                    }

                    if (!AccessFlags.ABSTRACT.isSet(interfaceMethod.accessFlags)) {
                        if (oldVtableMethod != null) {
                            if (!interfaceMethodOverrides(interfaceMethod, oldVtableMethod)) {
                                continue
                            }
                        }
                        defaultMethods.add(interfaceMethod)
                        methodOrder[interfaceMethod] = methodOrder.size
                    } else {
                        // TODO: do we need to check interfaceMethodOverrides here?
                        if (oldVtableMethod == null) {
                            mirandaMethods.add(interfaceMethod)
                            methodOrder[interfaceMethod] = methodOrder.size
                        }
                    }
                }
                i--
            }

            val comparator = Comparator<MethodReference> { o1, o2 ->
                methodOrder[o1]!!.compareTo(methodOrder[o2]!!)
            }

            // The methods should be in the same order within each list as they were iterated over.
            // They can be misordered if, e.g. a method was originally added to the default list, but then moved
            // to the conflict list.
            Collections.sort(mirandaMethods, comparator)
            Collections.sort(defaultMethods, comparator)
            Collections.sort(defaultConflictMethods, comparator)

            vtable.addAll(mirandaMethods)
            vtable.addAll(defaultMethods)
            vtable.addAll(defaultConflictMethods)
        }
        return vtable
    }

    private val postDefaultMethodVtableSupplier: Supplier<List<Method>> =
        MemoizingSupplier.memoize(Supplier { computePostDefaultMethodVtable() })

    private fun computePostDefaultMethodVtable(): List<Method> {
        val vtable = ArrayList<Method>()

        //copy the virtual methods from the superclass
        val superclassType: String?
        try {
            superclassType = superclass
        } catch (ex: UnresolvedClassException) {
            vtable.addAll((classPath.getClass("Ljava/lang/Object;") as ClassProto).getVtable())
            vtableFullyResolved = false
            return vtable
        }

        if (superclassType != null) {
            val superclass = classPath.getClass(superclassType) as ClassProto
            vtable.addAll(superclass.getVtable())

            // if the superclass's vtable wasn't fully resolved, then we can't know where the new methods added by
            // this class should start, so we just propagate what we can from the parent and hope for the best.
            if (!superclass.vtableFullyResolved) {
                vtableFullyResolved = false
                return vtable
            }
        }

        //iterate over the virtual methods in the current class, and only add them when we don't already have the
        //method (i.e. if it was implemented by the superclass)
        if (!isInterface()) {
            addToVtable(getClassDef().virtualMethods, vtable, true, true)

            val interfaces = IteratorUtils.toList(getDirectInterfaces())
            Collections.reverse(interfaces)

            val defaultMethods = ArrayList<Method>()
            val defaultConflictMethods = ArrayList<Method>()
            val mirandaMethods = ArrayList<Method>()

            val methodOrder = HashMap<MethodReference, Int>()

            for (interfaceDef in interfaces) {
                for (interfaceMethod in interfaceDef.virtualMethods) {

                    val vtableIndex = findMethodIndexInVtable(vtable, interfaceMethod)

                    if (vtableIndex >= 0) {
                        if (interfaceMethodOverrides(interfaceMethod, vtable[vtableIndex])) {
                            vtable[vtableIndex] = interfaceMethod
                        }
                    } else {
                        val defaultMethodIndex = findMethodIndexInVtable(defaultMethods, interfaceMethod)

                        if (defaultMethodIndex >= 0) {
                            if (!AccessFlags.ABSTRACT.isSet(interfaceMethod.accessFlags)) {
                                val existingInterface = classPath.getClass(
                                    defaultMethods[defaultMethodIndex].definingClass
                                ) as ClassProto
                                if (!existingInterface.implementsInterface(interfaceMethod.definingClass)) {
                                    val removedMethod = defaultMethods.removeAt(defaultMethodIndex)
                                    defaultConflictMethods.add(removedMethod)
                                }
                            }
                            continue
                        }

                        val defaultConflictMethodIndex = findMethodIndexInVtable(
                            defaultConflictMethods, interfaceMethod
                        )
                        if (defaultConflictMethodIndex >= 0) {
                            // There's already a matching method in the conflict list, we don't need to do
                            // anything else
                            continue
                        }

                        val mirandaMethodIndex = findMethodIndexInVtable(mirandaMethods, interfaceMethod)

                        if (mirandaMethodIndex >= 0) {
                            if (!AccessFlags.ABSTRACT.isSet(interfaceMethod.accessFlags)) {

                                val existingInterface = classPath.getClass(
                                    mirandaMethods[mirandaMethodIndex].definingClass
                                ) as ClassProto
                                if (!existingInterface.implementsInterface(interfaceMethod.definingClass)) {
                                    val oldMethod = mirandaMethods.removeAt(mirandaMethodIndex)
                                    val methodOrderValue = methodOrder[oldMethod]!!
                                    methodOrder[interfaceMethod] = methodOrderValue
                                    defaultMethods.add(interfaceMethod)
                                }
                            }
                            continue
                        }

                        if (!AccessFlags.ABSTRACT.isSet(interfaceMethod.accessFlags)) {
                            defaultMethods.add(interfaceMethod)
                            methodOrder[interfaceMethod] = methodOrder.size
                        } else {
                            mirandaMethods.add(interfaceMethod)
                            methodOrder[interfaceMethod] = methodOrder.size
                        }
                    }
                }
            }

            val comparator = Comparator<MethodReference> { o1, o2 ->
                methodOrder[o1]!!.compareTo(methodOrder[o2]!!)
            }

            // The methods should be in the same order within each list as they were iterated over.
            // They can be misordered if, e.g. a method was originally added to the default list, but then moved
            // to the conflict list.
            Collections.sort(defaultMethods, comparator)
            Collections.sort(defaultConflictMethods, comparator)
            Collections.sort(mirandaMethods, comparator)
            addToVtable(defaultMethods, vtable, false, false)
            addToVtable(defaultConflictMethods, vtable, false, false)
            addToVtable(mirandaMethods, vtable, false, false)
        }
        return vtable
    }

    private fun addToVtable(
        localMethodsIn: Iterable<Method>,
        vtable: MutableList<Method>,
        replaceExisting: Boolean,
        sort: Boolean
    ) {
        var localMethods = localMethodsIn
        if (sort) {
            val methods = ArrayList(IteratorUtils.toList(localMethods))
            Collections.sort(methods)
            localMethods = methods
        }

        for (virtualMethod in localMethods) {
            val vtableIndex = findMethodIndexInVtable(vtable, virtualMethod)

            if (vtableIndex >= 0) {
                if (replaceExisting) {
                    vtable[vtableIndex] = virtualMethod
                }
            } else {
                // we didn't find an equivalent method, so add it as a new entry
                vtable.add(virtualMethod)
            }
        }
    }

    private fun isOverridableByDefaultMethod(method: Method): Boolean {
        val classProto = classPath.getClass(method.definingClass) as ClassProto
        return classProto.isInterface()
    }

    /**
     * Checks if the interface method overrides the virtual or interface method2
     * @param method A Method from an interface
     * @param method2 A Method from an interface or a class
     * @return true if the interface method overrides the virtual or interface method2
     */
    private fun interfaceMethodOverrides(method: Method, method2: Method): Boolean {
        val classProto = classPath.getClass(method2.definingClass) as ClassProto

        return if (classProto.isInterface()) {
            val targetClassProto = classPath.getClass(method.definingClass) as ClassProto
            targetClassProto.implementsInterface(method2.definingClass)
        } else {
            false
        }
    }

    internal class ReparentedMethod(private val method: Method, override val definingClass: String) :
        BaseMethodReference(), Method {
        override val name: String
            get() = method.name

        override val parameterTypes: List<CharSequence>
            get() = method.parameterTypes

        override val returnType: String
            get() = method.returnType

        override val parameters: List<MethodParameter>
            get() = method.parameters

        override val accessFlags: Int
            get() = method.accessFlags

        override val annotations: Set<Annotation>
            get() = method.annotations

        override val hiddenApiRestrictions: Set<HiddenApiRestriction>
            get() = method.hiddenApiRestrictions

        override val implementation: MethodImplementation?
            get() = method.implementation
    }

    private abstract class FieldGap(@JvmField val offset: Int, @JvmField val size: Int) : Comparable<FieldGap> {
        companion object {
            fun newFieldGap(offset: Int, size: Int, oatVersion: Int): FieldGap {
                return if (oatVersion >= 67) {
                    object : FieldGap(offset, size) {
                        override fun compareTo(o: FieldGap): Int {
                            val result = o.size.compareTo(size)
                            if (result != 0) {
                                return result
                            }
                            return offset.compareTo(o.offset)
                        }
                    }
                } else {
                    object : FieldGap(offset, size) {
                        override fun compareTo(o: FieldGap): Int {
                            val result = size.compareTo(o.size)
                            if (result != 0) {
                                return result
                            }
                            return o.offset.compareTo(offset)
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val REFERENCE: Byte = 0
        private const val WIDE: Byte = 1
        private const val OTHER: Byte = 2

        private fun getFieldType(field: FieldReference): Byte {
            return when (field.type[0]) {
                '[', 'L' -> 0 //REFERENCE
                'J', 'D' -> 1 //WIDE
                else -> 2 //OTHER
            }
        }

        private fun getTypeSize(type: Char): Int {
            return when (type) {
                'J', 'D' -> 8
                '[', 'L', 'I', 'F' -> 4
                'C', 'S' -> 2
                'B', 'Z' -> 1
                else -> throw ExceptionWithContext("Invalid type: %s", type)
            }
        }
    }
}
