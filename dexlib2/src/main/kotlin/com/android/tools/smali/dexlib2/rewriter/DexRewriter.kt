/*
 * Copyright 2014, Google LLC
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

package com.android.tools.smali.dexlib2.rewriter

import com.android.tools.smali.dexlib2.iface.Annotation
import com.android.tools.smali.dexlib2.iface.AnnotationElement
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.DexFile
import com.android.tools.smali.dexlib2.iface.ExceptionHandler
import com.android.tools.smali.dexlib2.iface.Field
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.MethodImplementation
import com.android.tools.smali.dexlib2.iface.MethodParameter
import com.android.tools.smali.dexlib2.iface.TryBlock
import com.android.tools.smali.dexlib2.iface.debug.DebugItem
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.reference.CallSiteReference
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.value.EncodedValue

/**
 * Out-of-the box, this class does nothing except make a picture-perfect copy of a dex file.
 *
 * However, it provides many points where you can hook into this process and selectively modify
 * the dex file. For example, If you want to rename all instances (including definitions and references)
 * of the class Lorg/blah/MyBlah; to Lorg/blah/YourBlah;
 *
 * <pre>
 * {@code
 * DexRewriter rewriter = new DexRewriter(new RewriterModule() {
 *     public Rewriter<String> getTypeRewriter(Rewriters rewriters) {
 *         return new Rewriter<String>() {
 *             public String rewrite(String value) {
 *                 if (value.equals("Lorg/blah/MyBlah;")) {
 *                     return "Lorg/blah/YourBlah;";
 *                 }
 *                 return value;
 *             }
 *         };
 *     }
 * });
 * DexFile rewrittenDexFile = rewriter.rewriteDexFile(dexFile);
 * }
 * </pre>
 */
open class DexRewriter(module: RewriterModule) : Rewriters {
    override val dexFileRewriter: Rewriter<DexFile> = module.getDexFileRewriter(this)
    override val classDefRewriter: Rewriter<ClassDef> = module.getClassDefRewriter(this)
    override val fieldRewriter: Rewriter<Field> = module.getFieldRewriter(this)
    override val methodRewriter: Rewriter<Method> = module.getMethodRewriter(this)
    override val methodParameterRewriter: Rewriter<MethodParameter> =
        module.getMethodParameterRewriter(this)
    override val methodImplementationRewriter: Rewriter<MethodImplementation> =
        module.getMethodImplementationRewriter(this)
    override val instructionRewriter: Rewriter<Instruction> = module.getInstructionRewriter(this)
    override val tryBlockRewriter: Rewriter<TryBlock<out ExceptionHandler>> =
        module.getTryBlockRewriter(this)
    override val exceptionHandlerRewriter: Rewriter<ExceptionHandler> =
        module.getExceptionHandlerRewriter(this)
    override val debugItemRewriter: Rewriter<DebugItem> = module.getDebugItemRewriter(this)
    override val typeRewriter: Rewriter<String> = module.getTypeRewriter(this)
    override val fieldReferenceRewriter: Rewriter<FieldReference> =
        module.getFieldReferenceRewriter(this)
    override val methodReferenceRewriter: Rewriter<MethodReference> =
        module.getMethodReferenceRewriter(this)
    override val callSiteReferenceRewriter: Rewriter<CallSiteReference> =
        module.getCallSiteReferenceRewriter(this)
    override val annotationRewriter: Rewriter<Annotation> = module.getAnnotationRewriter(this)
    override val annotationElementRewriter: Rewriter<AnnotationElement> =
        module.getAnnotationElementRewriter(this)
    override val encodedValueRewriter: Rewriter<EncodedValue> = module.getEncodedValueRewriter(this)
}
