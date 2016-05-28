/*
 *Copyright (c) 2010 Daniel Doubrovkine, All Rights Reserved
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 *
 * This library is licensed under the LGPL, version 2.1 or later, and
 * (from version 4.0 onward) the Apache Software License, version 2.0.
 * Commercial license arrangements are negotiable.
 */
package dorkbox.console.util.windows;

import com.sun.jna.FromNativeContext;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;

/**
 * Handle to an object.
 */
public
class HANDLE extends PointerType {
    /** Constant value representing an invalid HANDLE. */
    public static final HANDLE INVALID_HANDLE_VALUE = new HANDLE(Pointer.createConstant(Pointer.SIZE == 8 ? -1 : 0xFFFFFFFFL));

    private boolean immutable;

    public
    HANDLE() {
    }

    public
    HANDLE(Pointer p) {
        setPointer(p);
        immutable = true;
    }

    @Override
    public
    void setPointer(Pointer p) {
        if (immutable) {
            throw new UnsupportedOperationException("immutable reference");
        }

        super.setPointer(p);
    }

    /**
     * Override to the appropriate object for INVALID_HANDLE_VALUE.
     */
    @Override
    public
    Object fromNative(Object nativeValue, FromNativeContext context) {
        Object o = super.fromNative(nativeValue, context);

        if (INVALID_HANDLE_VALUE.equals(o)) {
            return INVALID_HANDLE_VALUE;
        }
        return o;
    }

    @Override
    public
    String toString() {
        return String.valueOf(getPointer());
    }
}
