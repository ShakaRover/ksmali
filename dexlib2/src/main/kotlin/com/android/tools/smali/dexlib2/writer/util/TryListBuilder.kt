/*
 * Copyright 2013, Google LLC
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

package com.android.tools.smali.dexlib2.writer.util

import com.android.tools.smali.dexlib2.base.BaseTryBlock
import com.android.tools.smali.dexlib2.iface.ExceptionHandler
import com.android.tools.smali.dexlib2.iface.TryBlock
import com.android.tools.smali.util.ExceptionWithContext
import com.android.tools.smali.util.IteratorUtils
import java.util.ArrayList

open class TryListBuilder<EH : ExceptionHandler> {
    // Linked list sentinels that don't represent an actual try block
    // Their values are never modified, only their links
    private val listStart: MutableTryBlock<EH>
    private val listEnd: MutableTryBlock<EH>

    init {
        listStart = MutableTryBlock<EH>(0, 0)
        listEnd = MutableTryBlock<EH>(0, 0)
        listStart.next = listEnd
        listEnd.prev = listStart
    }

    private fun getBoundingRanges(startAddress: Int, endAddress: Int): TryBounds<EH> {
        var startBlock: MutableTryBlock<EH>? = null

        var tryBlock = listStart.next!!
        while (tryBlock !== listEnd) {
            val currentStartAddress = tryBlock.startCodeAddress
            val currentEndAddress = tryBlock.endCodeAddress

            if (startAddress == currentStartAddress) {
                //|-----|
                //^------
                /*Bam. We hit the start of the range right on the head*/
                startBlock = tryBlock
                break
            } else if (startAddress > currentStartAddress && startAddress < currentEndAddress) {
                //|-----|
                //  ^----
                /*Almost. The start of the range being added is in the middle
                of an existing try range. We need to split the existing range
                at the start address of the range being added*/
                startBlock = tryBlock.split(startAddress)
                break
            } else if (startAddress < currentStartAddress) {
                if (endAddress <= currentStartAddress) {
                    //      |-----|
                    //^--^
                    /*Oops, totally too far! The new range doesn't overlap any existing
                    ones, so we just add it and return*/
                    val newStartBlock = MutableTryBlock<EH>(startAddress, endAddress)
                    tryBlock.prepend(newStartBlock)
                    return TryBounds(newStartBlock, newStartBlock)
                } else {
                    //   |-----|
                    //^---------
                    /*Oops, too far! We've passed the start of the range being added, but
                     the new range does overlap this one. We need to add a new range just
                     before this one*/
                    startBlock = MutableTryBlock<EH>(startAddress, currentStartAddress)
                    tryBlock.prepend(startBlock)
                    break
                }
            }

            tryBlock = tryBlock.next!!
        }

        //|-----|
        //        ^-----
        /*Either the list of tries is blank, or all the tries in the list
        end before the range being added starts. In either case, we just need
        to add a new range at the end of the list*/
        if (startBlock == null) {
            val newStartBlock = MutableTryBlock<EH>(startAddress, endAddress)
            listEnd.prepend(newStartBlock)
            return TryBounds(newStartBlock, newStartBlock)
        }

        tryBlock = startBlock
        while (tryBlock !== listEnd) {
            val currentStartAddress = tryBlock.startCodeAddress
            val currentEndAddress = tryBlock.endCodeAddress

            if (endAddress == currentEndAddress) {
                //|-----|
                //------^
                /*Bam! We hit the end right on the head... err, tail.*/
                return TryBounds(startBlock, tryBlock)
            } else if (endAddress > currentStartAddress && endAddress < currentEndAddress) {
                //|-----|
                //--^
                /*Almost. The range being added ends in the middle of an
                existing range. We need to split the existing range
                at the end of the range being added.*/
                tryBlock.split(endAddress)
                return TryBounds(startBlock, tryBlock)
            } else if (endAddress <= currentStartAddress) {
                //|-----|       |-----|
                //-----------^
                /*Oops, too far! The current range starts after the range being added
                ends. We need to create a new range that starts at the end of the
                previous range, and ends at the end of the range being added*/
                val endBlock = MutableTryBlock<EH>(tryBlock.prev!!.endCodeAddress, endAddress)
                tryBlock.prepend(endBlock)
                return TryBounds(startBlock, endBlock)
            }
            tryBlock = tryBlock.next!!
        }

        //|-----|
        //--------^
        /*The last range in the list ended before the end of the range being added.
        We need to add a new range that starts at the end of the last range in the
        list, and ends at the end of the range being added.*/
        val endBlock = MutableTryBlock<EH>(listEnd.prev!!.endCodeAddress, endAddress)
        listEnd.prepend(endBlock)
        return TryBounds(startBlock, endBlock)
    }

    fun addHandler(startAddress: Int, endAddress: Int, handler: EH) {
        val bounds = getBoundingRanges(startAddress, endAddress)

        val startBlock = bounds.start
        val endBlock = bounds.end

        var previousEnd = startAddress
        var tryBlock = startBlock

        /*Now we have the start and end ranges that exactly match the start and end
        of the range being added. We need to iterate over all the ranges from the start
        to end range inclusively, and append the handler to the end of each range's handler
        list. We also need to create a new range for any "holes" in the existing ranges*/
        do {
            //is there a hole? If so, add a new range to fill the hole
            if (tryBlock.startCodeAddress > previousEnd) {
                val newBlock = MutableTryBlock<EH>(previousEnd, tryBlock.startCodeAddress)
                tryBlock.prepend(newBlock)
                tryBlock = newBlock
            }

            tryBlock.addHandler(handler)
            previousEnd = tryBlock.endCodeAddress
            tryBlock = tryBlock.next!!
        } while (tryBlock.prev !== endBlock)
    }

    fun getTryBlocks(): List<TryBlock<EH>> {
        return IteratorUtils.toList(object : MutableIterator<TryBlock<EH>> {
            // The next TryBlock to return. This has already been merged, if needed.
            private var next: MutableTryBlock<EH>? = null

            init {
                next = listStart
                next = readNextItem()
            }

            /**
             * Read the item that comes after the current value of the next field.
             * @return The next item, or null if there is no next item
             */
            fun readNextItem(): MutableTryBlock<EH>? {
                // We can assume that next is not null, due to the way iteration happens
                val ret = next!!.next!!

                if (ret === listEnd) {
                    return null
                }

                while (ret.next !== listEnd) {
                    val retNext = ret.next!!
                    if (ret.endCodeAddress == retNext.startCodeAddress &&
                        ret.exceptionHandlers == retNext.exceptionHandlers) {
                        ret.mergeNext()
                    } else {
                        break
                    }
                }
                return ret
            }

            override fun hasNext(): Boolean {
                return next != null
            }

            override fun next(): TryBlock<EH> {
                if (!hasNext()) {
                    throw NoSuchElementException()
                }
                val ret = next!!
                next = readNextItem()
                // ret can't be null (ret=next and hasNext returned true)
                return ret
            }

            override fun remove() {
                throw UnsupportedOperationException()
            }
        })
    }

    class InvalidTryException : ExceptionWithContext {
        constructor(cause: Throwable) : super(cause)

        constructor(cause: Throwable, message: String, vararg formatArgs: Any?) : super(cause, message, *formatArgs)

        constructor(message: String, vararg formatArgs: Any?) : super(message, *formatArgs)
    }

    private class MutableTryBlock<EH : ExceptionHandler> : BaseTryBlock<EH> {
        var prev: MutableTryBlock<EH>? = null
        var next: MutableTryBlock<EH>? = null

        override var startCodeAddress: Int
        var endCodeAddress: Int
        override var exceptionHandlers: MutableList<EH> = ArrayList()

        constructor(startCodeAddress: Int, endCodeAddress: Int) {
            this.startCodeAddress = startCodeAddress
            this.endCodeAddress = endCodeAddress
        }

        constructor(startCodeAddress: Int, endCodeAddress: Int, exceptionHandlers: List<EH>) {
            this.startCodeAddress = startCodeAddress
            this.endCodeAddress = endCodeAddress
            this.exceptionHandlers = ArrayList(exceptionHandlers)
        }

        override val codeUnitCount: Int
            get() = endCodeAddress - startCodeAddress

        fun split(splitAddress: Int): MutableTryBlock<EH> {
            val newTryBlock = MutableTryBlock<EH>(splitAddress, endCodeAddress, exceptionHandlers)
            endCodeAddress = splitAddress
            append(newTryBlock)
            return newTryBlock
        }

        fun delete() {
            next!!.prev = prev
            prev!!.next = next
        }

        fun mergeNext() {
            //assert next.startCodeAddress == this.endCodeAddress;
            this.endCodeAddress = next!!.endCodeAddress
            next!!.delete()
        }

        fun append(tryBlock: MutableTryBlock<EH>) {
            next!!.prev = tryBlock
            tryBlock.next = next
            tryBlock.prev = this
            next = tryBlock
        }

        fun prepend(tryBlock: MutableTryBlock<EH>) {
            prev!!.next = tryBlock
            tryBlock.prev = prev
            tryBlock.next = this
            prev = tryBlock
        }

        fun addHandler(handler: EH) {
            for (existingHandler in exceptionHandlers) {
                val existingType = existingHandler.exceptionType
                val newType = handler.exceptionType

                if (existingType == null) {
                    if (newType == null) {
                        if (existingHandler.handlerCodeAddress != handler.handlerCodeAddress) {
                            throw InvalidTryException(
                                "Multiple overlapping catch all handlers with different handlers")
                        }
                        return
                    }
                } else if (existingType == newType) {
                    // dalvik doesn't reject cases when there are multiple catches with the same exception
                    // but different handlers. In practice, the first handler "wins". Since the later
                    // handler will never be used, we don't add it.
                    return
                }
            }

            exceptionHandlers.add(handler)
        }
    }

    private class TryBounds<EH : ExceptionHandler>(val start: MutableTryBlock<EH>, val end: MutableTryBlock<EH>)

    companion object {
        @JvmStatic
        fun <EH : ExceptionHandler> massageTryBlocks(tryBlocks: List<out TryBlock<out EH>>): List<TryBlock<EH>> {
            val tlb = TryListBuilder<EH>()

            for (tryBlock in tryBlocks) {
                val startAddress = tryBlock.startCodeAddress
                val endAddress = startAddress + tryBlock.codeUnitCount

                for (exceptionHandler in tryBlock.exceptionHandlers) {
                    tlb.addHandler(startAddress, endAddress, exceptionHandler)
                }
            }
            return tlb.getTryBlocks()
        }
    }
}
