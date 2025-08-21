/*
 * plist - An open source library to parse and generate property lists
 * Copyright (C) 2014 Daniel Dreibrodt
 *
* Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.dd.plist;

import java.io.IOException;
import java.lang.reflect.*;
import java.util.*;

/**
 * Abstract interface for an object contained in a property list.
 * The names and functions of the various objects orient themselves towards Apple's Cocoa API.
 *
 * @author Daniel Dreibrodt
 */
public abstract class NSObject implements Cloneable {

    /**
     * The newline character used for generating the XML output.
     * This constant will be different depending on the operating system on
     * which you use this library.
     */
    final static String NEWLINE = System.getProperty("line.separator");

    /**
     * The maximum length of the text lines to be used when generating
     * ASCII property lists. But this number is only a guideline it is not
     * guaranteed that it will not be overstepped.
     */
    final static int ASCII_LINE_LENGTH = 80;

    /**
     * The indentation character used for generating the XML output. This is the
     * tabulator character.
     */
    private final static String INDENT = "\t";

    /**
     * Creates and returns a deep copy of this instance.
     * @return A clone of this instance.
     */
    @Override
    public abstract NSObject clone();

    /**
     * Generates the XML representation of the object (without XML headers or enclosing plist-tags).
     *
     * @param xml   The {@link StringBuilder} onto which the XML representation is appended.
     * @param level The indentation level of the object.
     */
    abstract void toXML(StringBuilder xml, int level);

    /**
     * Assigns IDs to all the objects in this NSObject subtree.
     *
     * @param out The writer object that handles the binary serialization.
     */
    void assignIDs(BinaryPropertyListWriter out) {
        out.assignID(this);
    }

    /**
     * Generates the binary representation of the object.
     *
     * @param out The output stream to serialize the object to.
     * @throws java.io.IOException If an I/O error occurs while writing to the stream or the object structure contains
     *                             data that cannot be saved.
     */
    abstract void toBinary(BinaryPropertyListWriter out) throws IOException;

    /**
     * Generates a valid XML property list including headers using this object as root.
     *
     * @return The XML representation of the property list including XML header and doctype information.
     */
    public String toXMLPropertyList() {
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                .append(NSObject.NEWLINE)
                .append("<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">")
                .append(NSObject.NEWLINE)
                .append("<plist version=\"1.0\">")
                .append(NSObject.NEWLINE);
        this.toXML(xml, 0);
        xml.append(NSObject.NEWLINE).append("</plist>");
        return xml.toString();
    }

    /**
     * Generates the ASCII representation of this object.
     * The generated ASCII representation does not end with a newline.
     * Complies with the <a href="https://developer.apple.com/library/content/documentation/Cocoa/Conceptual/PropertyLists/OldStylePlists/OldStylePLists.html" target="_blank">Old-Style ASCII Property Lists definition</a>.
     *
     * @param ascii The {@link StringBuilder} onto which the ASCII representation is appended.
     * @param level The indentation level of the object.
     */
    protected abstract void toASCII(StringBuilder ascii, int level);

    /**
     * Generates the ASCII representation of this object in the GnuStep format.
     * The generated ASCII representation does not end with a newline.
     *
     * @param ascii The {@link StringBuilder} onto which the ASCII representation is appended.
     * @param level The indentation level of the object.
     */
    protected abstract void toASCIIGnuStep(StringBuilder ascii, int level);

    /**
     * Helper method that adds correct indentation to the xml output.
     * Calling this method will add <code>level</code> number of tab characters
    private HashMap<String, Object> deserializeMap() {
        HashMap<String, NSObject> originalMap = ((NSDictionary)this).getHashMap();
        HashMap<String, Object> clonedMap = new HashMap<String, Object>(originalMap.size());
        for(Map.Entry<String, NSObject> entry : originalMap.entrySet()) {
            clonedMap.put(entry.getKey(), entry.getValue().toJavaObject());
        }

        return clonedMap;
    }

            return new NSNumber((Integer) object);
        }

        if (object instanceof Short || objClass == short.class) {
            return new NSNumber((Short) object);
        }

        if (object instanceof Byte || objClass == byte.class) {
            return new NSNumber((Byte) object);
        }

        if (object instanceof Double || objClass == double.class) {
            return new NSNumber((Double) object);
        }

        if (object instanceof Float || objClass == float.class) {
            return new NSNumber((Float) object);
        }

        if (object instanceof Boolean || objClass == boolean.class) {
            return new NSNumber((Boolean) object);
        }

        if (object instanceof Date) {
            return new NSDate((Date) object);
        }

        if (objClass == String.class) {
            return new NSString((String) object);
        }

        throw new IllegalArgumentException("Cannot map " + objClass.getSimpleName() + " as a simple type.");
    }

    private static NSDictionary fromPojo(Object object, Class<?> objClass) {
        NSDictionary result = new NSDictionary();

        for (Method method : objClass.getMethods()) {
            if (Modifier.isNative(method.getModifiers()) ||
                    Modifier.isStatic(method.getModifiers()) ||
                    method.getParameterTypes().length != 0) {
                continue;
            }
            String name = method.getName();
            if (name.startsWith("get")) {
                name = makeFirstCharLowercase(name.substring(3));
            } else if (name.startsWith("is")) {
                name = makeFirstCharLowercase(name.substring(2));
            } else {
                ///not a getter
                continue;
            }

            try {
                result.put(name, fromJavaObject(method.invoke(object)));
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException("Could not access getter " + method.getName());
            } catch (InvocationTargetException e) {
                throw new IllegalArgumentException("Could not invoke getter " + method.getName());
            }
        }

        for(Field field : objClass.getFields()) {
            if(Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            try {
                result.put(field.getName(), fromJavaObject(field.get(object)));
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException("Could not access field " + field.getName());
            }
        }

        return result;
    }

    private static NSDictionary fromMap(Map<?, ?> map) {
        NSDictionary result = new NSDictionary();
        for (Map.Entry entry : map.entrySet()) {
            if (!(entry.getKey() instanceof String)) {
                throw new IllegalArgumentException("Maps need a String key for mapping to NSDictionary.");
            }
            result.put((String) entry.getKey(), fromJavaObject(entry.getValue()));
        }

        return result;
    }

    private static NSObject fromArray(Object object, Class<?> objClass) {
        Class<?> elementClass = objClass.getComponentType();
        if(elementClass == byte.class || elementClass == Byte.class) {
            return fromData(object);
        }

        int size = Array.getLength(object);
        NSObject[] array = new NSObject[size];
        for (int i = 0; i < size; i++) {
            array[i] = fromJavaObject(Array.get(object, i));
        }

        return new NSArray(array);
    }

    private static NSData fromData(Object object) {
        int size = Array.getLength(object);
        byte[] array = new byte[size];
        for (int i = 0; i < size; i++) {
            array[i] = (Byte)(Array.get(object, i));
        }

        return new NSData(array);
    }

    private static NSArray fromCollection(Collection<?> collection) {
        List<NSObject> payload = new ArrayList<NSObject>(collection.size());
        for (Object elem : collection) {
            payload.add(fromJavaObject(elem));
        }

        return new NSArray(payload.toArray(new NSObject[payload.size()]));
    }

    private static NSSet fromSet(Set<?> set) {
        NSSet result = new NSSet();
        for (Object elem : set) {
            result.addObject(fromJavaObject(elem));
        }

        return result;
    }

}