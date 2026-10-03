/*
 * [The "BSD licence"]
 * Copyright (c) 2010 Ben Gruver (JesusFreke)
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.android.tools.smali.baksmali.Adaptors

import com.android.tools.smali.baksmali.BaksmaliOptions
import com.android.tools.smali.baksmali.formatter.BaksmaliFormatter
import com.android.tools.smali.baksmali.formatter.BaksmaliWriter
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.dexbacked.DexBackedClassDef
import com.android.tools.smali.dexlib2.iface.Annotation
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Field
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.MethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction21c
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.Reference
import java.io.IOException
import java.util.HashSet

class ClassDefinition(
    @JvmField val options: BaksmaliOptions,
    @JvmField val classDef: ClassDef
) {
    @JvmField
    protected var validationErrors = false

    private val fieldsSetInStaticConstructor: HashSet<String>
    val formatter: BaksmaliFormatter

    init {
        formatter = BaksmaliFormatter(if (options.implicitReferences) classDef.type else null)
        fieldsSetInStaticConstructor = findFieldsSetInStaticConstructor(classDef)
    }

    fun hadValidationErrors(): Boolean {
        return validationErrors
    }

    private fun findFieldsSetInStaticConstructor(classDef: ClassDef): HashSet<String> {
        val fieldsSetInStaticConstructor = HashSet<String>()

        for (method in classDef.directMethods) {
            if (method.name == "<clinit>") {
                val impl = method.implementation
                if (impl != null) {
                    for (instruction in impl.instructions) {
                        when (instruction.opcode) {
                            Opcode.SPUT, Opcode.SPUT_BOOLEAN, Opcode.SPUT_BYTE, Opcode.SPUT_CHAR,
                            Opcode.SPUT_OBJECT, Opcode.SPUT_SHORT, Opcode.SPUT_WIDE -> {
                                val ins = instruction as Instruction21c
                                val fieldRef = ins.reference as FieldReference
                                try {
                                    fieldRef.validateReference()
                                    if (fieldRef.definingClass == classDef.type) {
                                        fieldsSetInStaticConstructor.add(
                                            formatter.getShortFieldDescriptor(fieldRef))
                                    }
                                } catch (ex: Reference.InvalidReferenceException) {
                                    // Just ignore for now. We'll deal with it when processing the instruction
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
        return fieldsSetInStaticConstructor
    }

    @Throws(IOException::class)
    fun writeTo(writer: BaksmaliWriter) {
        writeClass(writer)
        writeSuper(writer)
        writeSourceFile(writer)
        writeInterfaces(writer)
        writeAnnotations(writer)
        val staticFields = writeStaticFields(writer)
        writeInstanceFields(writer, staticFields)
        val directMethods = writeDirectMethods(writer)
        writeVirtualMethods(writer, directMethods)
    }

    @Throws(IOException::class)
    private fun writeClass(writer: BaksmaliWriter) {
        writer.write(".class ")
        writeAccessFlags(writer)
        writer.writeType(classDef.type)
        writer.write('\n')
    }

    @Throws(IOException::class)
    private fun writeAccessFlags(writer: BaksmaliWriter) {
        for (accessFlag in AccessFlags.getAccessFlagsForClass(classDef.accessFlags)) {
            writer.write(accessFlag.toString())
            writer.write(' ')
        }
    }

    @Throws(IOException::class)
    private fun writeSuper(writer: BaksmaliWriter) {
        val superClass = classDef.superclass
        if (superClass != null) {
            writer.write(".super ")
            writer.writeType(superClass)
            writer.write('\n')
        }
    }

    @Throws(IOException::class)
    private fun writeSourceFile(writer: BaksmaliWriter) {
        val sourceFile = classDef.sourceFile
        if (sourceFile != null) {
            writer.write(".source ")
            writer.writeQuotedString(sourceFile)
            writer.write("\n")
        }
    }

    @Throws(IOException::class)
    private fun writeInterfaces(writer: BaksmaliWriter) {
        val interfaces = classDef.interfaces

        if (interfaces.size != 0) {
            writer.write('\n')
            writer.write("# interfaces\n")
            for (interfaceName in interfaces) {
                writer.write(".implements ")
                writer.writeType(interfaceName)
                writer.write('\n')
            }
        }
    }

    @Throws(IOException::class)
    private fun writeAnnotations(writer: BaksmaliWriter) {
        val classAnnotations = classDef.annotations
        if (classAnnotations.size != 0) {
            writer.write("\n\n")
            writer.write("# annotations\n")

            writeTo(writer, classAnnotations)
        }
    }

    @Throws(IOException::class)
    private fun writeStaticFields(writer: BaksmaliWriter): Set<String> {
        var wroteHeader = false
        val writtenFields = HashSet<String>()

        val staticFields: Iterable<Field> = if (classDef is DexBackedClassDef) {
            classDef.getStaticFields(false)
        } else {
            classDef.staticFields
        }

        for (field in staticFields) {
            if (!wroteHeader) {
                writer.write("\n\n")
                writer.write("# static fields")
                wroteHeader = true
            }
            writer.write('\n')

            var setInStaticConstructor: Boolean
            var fieldWriter = writer
            val fieldString = formatter.getShortFieldDescriptor(field)
            if (!writtenFields.add(fieldString)) {
                writer.write("# duplicate field ignored\n")
                fieldWriter = getCommentingWriter(writer)
                System.err.println(String.format("Ignoring duplicate field: %s->%s", classDef.type, fieldString))
                setInStaticConstructor = false
            } else {
                setInStaticConstructor = fieldsSetInStaticConstructor.contains(fieldString)
            }
            writeTo(fieldWriter, field, setInStaticConstructor)
        }
        return writtenFields
    }

    @Throws(IOException::class)
    private fun writeInstanceFields(writer: BaksmaliWriter, staticFields: Set<String>) {
        var wroteHeader = false
        val writtenFields = HashSet<String>()

        val instanceFields: Iterable<Field> = if (classDef is DexBackedClassDef) {
            classDef.getInstanceFields(false)
        } else {
            classDef.instanceFields
        }

        for (field in instanceFields) {
            if (!wroteHeader) {
                writer.write("\n\n")
                writer.write("# instance fields")
                wroteHeader = true
            }
            writer.write('\n')

            var fieldWriter = writer
            val fieldString = formatter.getShortFieldDescriptor(field)
            if (!writtenFields.add(fieldString)) {
                writer.write("# duplicate field ignored\n")
                fieldWriter = getCommentingWriter(writer)
                System.err.println(String.format("Ignoring duplicate field: %s->%s", classDef.type, fieldString))
            } else if (staticFields.contains(fieldString)) {
                System.err.println(String.format("Duplicate static+instance field found: %s->%s",
                    classDef.type, fieldString))
                System.err.println("You will need to rename one of these fields, including all references.")

                writer.write("# There is both a static and instance field with this signature.\n" +
                    "# You will need to rename one of these fields, including all references.\n")
            }
            writeTo(fieldWriter, field, false)
        }
    }

    @Throws(IOException::class)
    private fun writeDirectMethods(writer: BaksmaliWriter): Set<String> {
        var wroteHeader = false
        val writtenMethods = HashSet<String>()

        val directMethods: Iterable<Method> = if (classDef is DexBackedClassDef) {
            classDef.getDirectMethods(false)
        } else {
            classDef.directMethods
        }

        for (method in directMethods) {
            if (!wroteHeader) {
                writer.write("\n\n")
                writer.write("# direct methods")
                wroteHeader = true
            }
            writer.write('\n')

            // TODO: check for method validation errors
            val methodString = formatter.getShortMethodDescriptor(method)

            var methodWriter = writer
            if (!writtenMethods.add(methodString)) {
                writer.write("# duplicate method ignored\n")
                methodWriter = getCommentingWriter(writer)
            }

            val methodImpl = method.implementation
            if (methodImpl == null) {
                MethodDefinition.writeEmptyMethodTo(methodWriter, method, this)
            } else {
                val methodDefinition = MethodDefinition(this, method, methodImpl)
                methodDefinition.writeTo(methodWriter)
            }
        }
        return writtenMethods
    }

    @Throws(IOException::class)
    private fun writeVirtualMethods(writer: BaksmaliWriter, directMethods: Set<String>) {
        var wroteHeader = false
        val writtenMethods = HashSet<String>()

        val virtualMethods: Iterable<Method> = if (classDef is DexBackedClassDef) {
            classDef.getVirtualMethods(false)
        } else {
            classDef.virtualMethods
        }

        for (method in virtualMethods) {
            if (!wroteHeader) {
                writer.write("\n\n")
                writer.write("# virtual methods")
                wroteHeader = true
            }
            writer.write('\n')

            // TODO: check for method validation errors
            val methodString = formatter.getShortMethodDescriptor(method)

            var methodWriter = writer
            if (!writtenMethods.add(methodString)) {
                writer.write("# duplicate method ignored\n")
                methodWriter = getCommentingWriter(writer)
            } else if (directMethods.contains(methodString)) {
                writer.write("# There is both a direct and virtual method with this signature.\n" +
                    "# You will need to rename one of these methods, including all references.\n")
                System.err.println(String.format("Duplicate direct+virtual method found: %s->%s",
                    classDef.type, methodString))
                System.err.println("You will need to rename one of these methods, including all references.")
            }

            val methodImpl = method.implementation
            if (methodImpl == null) {
                MethodDefinition.writeEmptyMethodTo(methodWriter, method, this)
            } else {
                val methodDefinition = MethodDefinition(this, method, methodImpl)
                methodDefinition.writeTo(methodWriter)
            }
        }
    }

    fun getCommentingWriter(writer: BaksmaliWriter): BaksmaliWriter {
        return formatter.getWriter(CommentingIndentingWriter(writer.indentingWriter()))
    }
}
