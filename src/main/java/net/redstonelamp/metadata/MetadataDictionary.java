/**
 * This file is part of RedstoneLamp.
 * <p>
 * RedstoneLamp is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * RedstoneLamp is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Lesser General Public License
 * along with RedstoneLamp.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.redstonelamp.metadata;


import net.redstonelamp.nio.BinaryBuffer;

import java.nio.ByteOrder;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Represents a Dictionary of MetadataElements.
 * This code is based off of the code here: https://github.com/NiclasOlofsson/MiNET/tree/master/src/MiNET/MiNET/Utils
 *
 * @author RedstoneLamp team
 */
public class MetadataDictionary extends Dictionary<Byte, MetadataElement>{
    private static Map<Byte, Class<? extends MetadataElement>> elements;
    private ConcurrentMap<Byte, MetadataElement> entries = new ConcurrentHashMap<>();

    public static void init(){
        elements = new HashMap<>();
        elements.put((byte) 0, MetadataByte.class);
        elements.put((byte) 1, MetadataShort.class);
        elements.put((byte) 2, MetadataInt.class);
        elements.put((byte) 4, MetadataString.class);
        elements.put((byte) 7, MetadataLong.class);
    }

    @Override
    public int size(){
        return entries.size();
    }

    @Override
    public boolean isEmpty(){
        return entries.isEmpty();
    }

    @Override
    public Enumeration<Byte> keys(){
        return new MetadataEnumeration(entries.keySet().toArray());
    }

    @Override
    public Enumeration<MetadataElement> elements(){
        return new MetadataEnumeration(entries.values().toArray());
    }

    @Override
    public MetadataElement get(Object key){
        return entries.get(key);
    }

    @Override
    public MetadataElement put(Byte key, MetadataElement value){
        return entries.put(key, value);
    }

    @Override
    public MetadataElement remove(Object key){
        return entries.remove(key);
    }

    public void fromBytes(byte[] bytes){
        BinaryBuffer bb = BinaryBuffer.wrapBytes(bytes, ByteOrder.LITTLE_ENDIAN);
        while(true){
            byte b = bb.getByte();
            if(b == -127){
                break;
            }

            byte type = (byte) ((b & 0xE0) >> 5);
            byte index = (byte) (b & 0x1F);

            MetadataElement element;
            if(index == 17){
                element = new MetadataLong(0);
            }else{
                element = getElementByType(type);
            }
            element.fromBytes(bb);
            element.setIndex(index);

            entries.put(index, element);
        }
    }

    private MetadataElement getElementByType(byte type){
        if(elements.containsKey(type)){
            try{
                return elements.get(type).newInstance();
            }catch(InstantiationException e){
                e.printStackTrace();
            }catch(IllegalAccessException e){
                e.printStackTrace();
            }
        }
        return null;
    }

    public byte[] toBytes(){
        BinaryBuffer bb = BinaryBuffer.newInstance(0, ByteOrder.LITTLE_ENDIAN);
        for(Byte key : entries.keySet()){
            MetadataElement element = entries.get(key);
            element.toBytes(bb, key);
        }
        return bb.toArray();
    }

    public static class MetadataEnumeration implements Enumeration{
        private List list;
        private int position = 0;

        public MetadataEnumeration(List elements){
            list = elements;
        }

        public MetadataEnumeration(Object[] elements){
            list = new ArrayList<>();
            for(Object element : elements){
                list.add(element);
            }
        }

        @Override
        public boolean hasMoreElements(){
            return position < list.size();
        }

        @Override
        public Object nextElement(){
            return list.get(position++);
        }
    }
}
